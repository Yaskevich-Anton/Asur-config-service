package com.example.ConfigService.service


import com.example.ConfigService.model.*
import com.example.ConfigService.model.DimensionConstants.Companion.CITIES_DIM_ID
import com.example.ConfigService.model.DimensionConstants.Companion.PERIOD_DIM_ID
import com.example.ConfigService.model.entity.FactDependentEntity
import com.example.ConfigService.model.entity.FactEntity
import com.example.ConfigService.model.entity.FactRelativesEntity
import com.example.ConfigService.model.entity.UserFactsEntity
import com.example.ConfigService.model.enumeration.GoalTypes
import com.example.ConfigService.model.enumeration.Period
import com.example.ConfigService.model.enumeration.Trend
import com.example.ConfigService.model.mapper.FactMapper
import com.example.ConfigService.repository.FactRelativesRepository
import com.example.ConfigService.repository.FactRepository
import com.example.ConfigService.repository.UserFactsRepository
import com.example.ConfigService.service.client.StoreRestClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet

@Service
class FactService(private val factRepository: FactRepository,
                  private val userFactsRepository: UserFactsRepository,
                  private val userDashboardService: UserDashboardService,
                  private val storeRestClient: StoreRestClient,
                  private val factMapper: FactMapper,
                  private val divisionService: RegionDivisionService,
                  private val factRelativesRepository: FactRelativesRepository,
                  private val dataSourceService: DataSourceService) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getFactByIdAndRegionId(factId: Int, regionId: Int) = factRepository.findFactByIdAndRegionId(factId, regionId)

    @Transactional
    fun pinFact(regionId: Int, factId: Int, pinFactDto: PinFactDto, currentUserId: Int) {
        val moduleId = pinFactDto.viewModuleId
        factRepository.findFactByIdAndRegionId(factId, regionId)
        val optionalUserFacts = userFactsRepository.findUserFactsByIdAndUserId(factId, currentUserId, moduleId)
        if (optionalUserFacts.isPresent) {
            val userFactEntity = optionalUserFacts.get()
            if (pinFactDto.before == null) {
                log.info("Показатель уже закреплен. Before не указан. Открепляем показатель и закрепляем в конец.")
                unPinFact(regionId, factId, currentUserId, moduleId)
                userFactsRepository.pin(currentUserId, factId, pinFactDto.viewModuleId)
                return
            }
            val optionalBeforeUserFact = userFactsRepository.findUserFactsByIdAndUserId(pinFactDto.before, currentUserId, moduleId)
            if (optionalBeforeUserFact.isEmpty) {
                log.info("Параметр указанный для вставки перед ним не закреплен. Открепляем и вставляем показатель последним.")
                unPinFact(regionId, factId, currentUserId, moduleId)
                userFactsRepository.pin(currentUserId, factId, pinFactDto.viewModuleId)
                return
            }
            log.info("Открепляем. Сдвигаем на одну позицию все показатели, начиная с указанного как before и закрепляем заново.")
            unPinFact(regionId, factId, currentUserId, moduleId)
            val beforeUserFact = optionalBeforeUserFact.get()
            val beforeOrderNum = if (beforeUserFact.orderNum > userFactEntity.orderNum) {
                //после открепления before был сдвинут на одну позицию
                beforeUserFact.orderNum - 1
            } else beforeUserFact.orderNum
            val maxOrderNum = userFactsRepository.findMaxOrderNumByUserIdAndModuleId(currentUserId, pinFactDto.viewModuleId)
            for(i in maxOrderNum downTo beforeOrderNum) {
                userFactsRepository.updateOrderNum(i, i + 1,  currentUserId, pinFactDto.viewModuleId)
            }
            userFactsRepository.pin(currentUserId, factId, userFactEntity.moduleFk, beforeOrderNum)
        } else {
            if (pinFactDto.before == null) {
                log.info("Если создаем новое закрепления без указания порядка, то добавляем в конец.")
                userFactsRepository.pin(currentUserId, factId, pinFactDto.viewModuleId)
                return
            }
            val optionalPinnedBefore = userFactsRepository.findUserFactsByIdAndUserId(pinFactDto.before, currentUserId, moduleId)
            if (optionalPinnedBefore.isPresent) {
                val pinnedBefore = optionalPinnedBefore.get()
                if (pinnedBefore.moduleFk != pinFactDto.viewModuleId) {
                    log.error("Показатель уже закреплен в другом модуле с ID: ${pinnedBefore.moduleFk}")
                    throw CoreException(ErrorCode.FACT_PINNED_TO_ANOTHER_MODULE, "")
                }
                log.info("Сдвигаем на одну позицию все показатели, начиная с указанного как before.")
                val maxOrderNum = userFactsRepository.findMaxOrderNumByUserIdAndModuleId(currentUserId, pinFactDto.viewModuleId )
                for(i in maxOrderNum downTo pinnedBefore.orderNum) {
                    userFactsRepository.updateOrderNum(i, i + 1, currentUserId, pinFactDto.viewModuleId)
                }
                userFactsRepository.pin(currentUserId, factId, pinFactDto.viewModuleId, pinnedBefore.orderNum)
            } else {
                log.info("Параметр указанный для вставки перед ним не закреплен. Вставляем показатель последним.")
                userFactsRepository.pin(currentUserId, factId, pinFactDto.viewModuleId)
            }
        }
    }

    @Transactional
    fun unPinFact(regionId: Int, factId: Int, currentUserId: Int, moduleId: Int? = null) {
        val userFact = userFactsRepository.findUserFactsByIdAndUserId(factId, currentUserId, moduleId).orElseThrow {
            log.warn("Не найден закрепленный показатель с ID = $factId для пользователя с ID = $currentUserId")
            throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "")
        }
        val position = userFact.orderNum
        val maxOrderNum = userFactsRepository.findMaxOrderNumByUserIdAndModuleId(currentUserId, userFact.moduleFk)
        userFactsRepository.delete(factId, currentUserId, moduleId)
        for (p in (position + 1)..maxOrderNum) {
            userFactsRepository.updateOrderNum(p, p - 1, currentUserId, userFact.moduleFk)
        }
    }

    fun getFacts(
            currentUserId: Int,
            regionId: Int,
            roles: String,
            text: String?,
            isPinned: Boolean?,
            moduleId: Int?,
            trend: Trend?,
            pageNum: Int,
            pageSize: Int
    ): List<FactShortDto> {
        val offset: Int = (pageNum - 1) * pageSize
        val limit: Int = pageSize
        if (trend == null) {
            return factRepository.findFacts(currentUserId, regionId, text, isPinned, moduleId, offset, limit)
        }
        val result: MutableList<FactShortDto> = mutableListOf()
        val facts = factRepository.findFacts(currentUserId, regionId, text, isPinned, moduleId, offset, limit)
        for (fact in facts) {
            val factInfo = getFactInfo(currentUserId, fact.id, regionId, roles, null, null,
            isShowDims = false,
            isShowDependent = false,
            isShowRelative = false)
            if (factInfo.highThreshold == null && factInfo.lowThreshold == null && factInfo.trendDelta == null) {
                continue
            }
            if (trend == factInfo.trend) {
                result.add(fact)
            }
        }
        return result
    }

    fun getFactsByModuleId(moduleId: Int): List<FactTrend> = factRepository.findFactsByModuleId(moduleId)

    fun getPinnedToDashboardFacts(userId: Int): List<FactShortDto> =
        factRepository.findPinnedToDashboardFacts(userId)

    fun getFactNames(regionId: Int, text: String?): List<FactName> =
        factRepository.findFactNames(regionId, text)

    fun getFactInfo(
        userId: Int,
        factId: Int,
        regionId: Int,
        roles: String,
        reportDate: LocalDate?,
        dataSourceId: Int?,
        isShowDims: Boolean,
        isShowDependent: Boolean,
        isShowRelative: Boolean
    ): FactInfoResponse {
        val factEntity = factRepository.findFactByIdAndRegionId(factId, regionId)
        val optionalSettings = userDashboardService.getDashboardSettings(userId, factId)
        val dimensionValues = getDimensionValues(factId, regionId, roles, optionalSettings)
        val now = ZonedDateTime.now()
        val period = Period.fromInt(factEntity.period)
        val firstPeriodDate = reportDate ?: getPeriodDate(period, now, roles, factEntity.planLoadDays)

        dimensionValues.add(DimensionValue(null, PERIOD_DIM_ID, firstPeriodDate.toString()))
        val factDataSourceId = dataSourceId ?: factEntity.dataSourceFk
        val factValues = storeRestClient.getFactValue(roles, dimensionValues, factId, regionId, factDataSourceId)
        var factValue: Double? = null
        var prevFactValue: Double? = null
        var trendDelta: Double? = null
        if (factValues.isNotEmpty() && factValues.first() != null) {
            //для расчета дельты
            //сдвинуть дату на один период назад и запросить на store еще одно значение
            factValue = factValues.first().value
            val prevPeriodFirstDate = getPrevPeriodFirstDate(period, firstPeriodDate.atStartOfDay(ZoneId.systemDefault()), 1)
            val prevDimensionValues: MutableList<DimensionValue> = mutableListOf()
            dimensionValues.forEach { dv ->
                //у даты в DimensionValue id == null
                val value = if (dv.id == null && dv.dimensionId == PERIOD_DIM_ID) {
                    prevPeriodFirstDate.toString()
                } else dv.value
                prevDimensionValues.add(DimensionValue(dv.id, dv.dimensionId, value))
            }
            val prevValues = storeRestClient.getFactValue(roles, prevDimensionValues, factId, regionId, factDataSourceId)
            if (prevValues.isNotEmpty()) {
                prevValues.first()?.let {
                    trendDelta = factValue - it.value
                    prevFactValue = it.value
                }
            }
        }

        val userFacts = userFactsRepository.findUserFactsByFactIdAndUserId(factId, userId)
        val isPinnedByDesktop = userFacts.any { uf: UserFactsEntity -> uf.moduleFk == null }
        val isPinnedByModule = userFacts.any { uf:UserFactsEntity -> uf.moduleFk != null }
        val trend: Trend? = getTrend(factEntity, factValue)
        val dataSource = dataSourceService.getDataSourceByIdAndFactId(factDataSourceId, factId)
        val division = factEntity.respUserDivisionId?.let { it:Int ->
            ShortRegionDivision(it, factEntity.respUserDivisionShortName!!, factEntity.respUserDivisionFullName!!)
        }
        val respUser = factEntity.respUserFk?.let { it:Int ->
            val department = division?.let { div: ShortRegionDivision ->
                val divisionsById = divisionService.getDivisionsByIdStructure(regionId)
                divisionService.getDepartmentByDivisionId(div.id, divisionsById)
            }
            RespUser(it, factEntity.respUserName!!, factEntity.respUserLastName!!, factEntity.respUserMiddleName, department, division)
        }
        val fact = factMapper.toFactInfo(factEntity,
            firstPeriodDate.atStartOfDay(ZoneId.systemDefault()),
            dimensionValues,
            factValue,
            prevFactValue,
            trendDelta,
            trend,
            isPinnedByDesktop,
            isPinnedByModule,
            dataSource,
            respUser)

        if (isShowDependent) {
            val dependentFacts: MutableList<FactInfoResponse> = mutableListOf()
            val relatives: List<FactRelativesEntity> = factRelativesRepository.findDependentFacts(factId)
            relatives.forEach { relativeFact ->
                var dependentReportDate = firstPeriodDate
                //отчетные периоды текущего показателя и показателей, от которых он зависит, могут не совпадать
                if (relativeFact.reportOffset != null && relativeFact.reportOffset > 0) {
                    //если отчетные периоды не совпадают, то сдвигаем дату
                    val reportOffset = relativeFact.reportOffset
                    dependentReportDate = getNextPeriodFirstDate(period, firstPeriodDate.atStartOfDay(ZoneId.systemDefault()), reportOffset)
                }
                val dependentFact = getFactInfo (userId, relativeFact.factPassportFk, regionId, roles, dependentReportDate, null, isShowDims, false, false)
                dependentFacts.add(dependentFact)
            }
            fact.dependent = dependentFacts
        }
        if (isShowRelative) {
            val relativeFacts: MutableList<RelativeFact> = mutableListOf()
            val relativesFromDb: List<FactRelativesEntity> = factRelativesRepository.findRelativeFacts(factId)
            relativesFromDb.forEach { relativeFactFromDb ->
                val reportOffset = relativeFactFromDb.reportOffset
                var reportDateWithOffset = firstPeriodDate
                if (reportOffset != null && reportOffset > 0) {
                    //если отчетные периоды не совпадают, то сдвигаем дату
                    reportDateWithOffset = getPrevPeriodFirstDate(period, firstPeriodDate.atStartOfDay(ZoneId.systemDefault()), reportOffset)
                }
                val relativeFact = getFactInfo(userId, relativeFactFromDb.factFk, regionId, roles, reportDateWithOffset, null, isShowDims, false, false)
                relativeFacts.add(RelativeFact(relativeFact.name, relativeFactFromDb.orderNum, relativeFactFromDb.reportOffset, relativeFact))
            }
            fact.relative = relativeFacts
        }
        return fact
    }

    private fun getTrend(factEntity: FactEntity, factValue: Double?): Trend? {
        if (factEntity.goalType == null || factEntity.lowThreshold == null || factEntity.highThreshold == null || factValue == null) {
            return null
        }
        return if (factEntity.goalType == GoalTypes.INCREASE.value) {
            if (factValue < factEntity.lowThreshold) Trend.NEGATIVE
            else if (factEntity.lowThreshold <= factValue && factValue <= factEntity.highThreshold) Trend.NEUTRAL
            else Trend.POSITIVE
        } else {
            if (factValue < factEntity.lowThreshold) Trend.POSITIVE
            else if (factEntity.lowThreshold <= factValue && factValue <= factEntity.highThreshold) Trend.NEUTRAL
            else Trend.NEGATIVE
        }
    }

    /**
     * Определение первого дня периода для текущей даты
     * planLoadDays - количество дней с начала периода, по истечению которых показатель должен быть расчитан
     */
    private fun getPeriodDate(period: Period, now: ZonedDateTime, roles: String, planLoadDays: Int): LocalDate {
        //получаем первую дату текущего периода
        val startDate = getStartDate(period, now)
        val planLoadDate = storeRestClient.getWorkDayForPeriod(roles, startDate, planLoadDays)
        val prevDate = dateMinusPeriods(period, startDate, 1)
        val prevStartDate = getStartDate(period, prevDate)
        return if (planLoadDate.isBefore(now.toLocalDate()) || planLoadDate.isEqual(now.toLocalDate())) {
            //вернуть первую дату прошлого периода
            prevStartDate.toLocalDate()
        } else {
            //вернуть первую дату позапрошлого периода
            val prevPrevDate = dateMinusPeriods(period, prevDate, 1)
            return getStartDate(period, prevPrevDate).toLocalDate()
        }
    }

    /**
     * Метод получения значений измерений из настроек. Если настроек нет, то по измерениям.
     */
    private fun getDimensionValues(factId: Int, regionId: Int, roles: String, optionalSettings: Optional<UserDashboards>): MutableList<DimensionValue> {
        val dimensionValues = mutableListOf<DimensionValue>()
        if (optionalSettings.isEmpty) {
            //настроек в бд нет. ищем измерения, связанные с показателем, получаем значения по каждому измерению и используем первое.
            val dimIds = factRepository.findDimIdsByFactId(factId)
            for (dimId in dimIds) {
                //исключаем отчетный период
                if (dimId == PERIOD_DIM_ID) {
                    continue
                }
                val dimValues = storeRestClient.getDifDimensionsValue(roles, regionId, dimId, null, null, ZonedDateTime.now())
                //если значение измерения город, то берем первый, если регион, то регион текущего пользователя.
            val dimValue = if (dimId == CITIES_DIM_ID) dimValues.first()
                else dimValues.first { dv -> dv.id == regionId }
                dimensionValues.add(DimensionValue(dimValue.id, dimId, dimValue.value))
            }
        } else {
            //настройки в бд сохранены. берем измерения и значения из настроек
            val settings = optionalSettings.get()
            settings.dimensionValues?.let { dvList ->
                dimensionValues.addAll(dvList.filter { dv -> dv.dimensionId != PERIOD_DIM_ID })
            }
        }
        return dimensionValues
    }

    /**
     * Метод отнимает от даты количество периодов(periodsCount) и возвращет первую дату полученного периода.
     */
    private fun getPrevPeriodFirstDate(period: Period, date: ZonedDateTime, periodsCount: Int): LocalDate {
        val prevPeriodDate = dateMinusPeriods(period, date, periodsCount)
        return getStartDate(period, prevPeriodDate).toLocalDate()
    }

    /**
     * Метод прибавляет к дате количество периодов(periodsCount) и возвращет первую дату полученного периода.
     */
    private fun getNextPeriodFirstDate(period: Period, date: ZonedDateTime, periodsCount: Int): LocalDate {
        val nextPeriodDate = datePlusPeriods(period, date, periodsCount)
        return getStartDate(period, nextPeriodDate).toLocalDate()
    }

    /**
     * Метод отнимает от даты количество периодов(periodsCount)
     */
    private fun dateMinusPeriods(period: Period, date: ZonedDateTime, periodsCount: Int): ZonedDateTime {
        return when(period) {
            Period.DAY -> date.minusDays(periodsCount.toLong() * 1)
            Period.WEEK -> date.minusDays(periodsCount.toLong() * 7)
            Period.MONTH -> date.minusMonths(periodsCount.toLong() * 1)
            Period.QUARTER -> date.minusMonths(periodsCount.toLong() * 3)
            Period.HALF_YEAR -> date.minusMonths(periodsCount.toLong() * 6)
            Period.YEAR -> date.minusYears(periodsCount.toLong() * 1)
        }
    }

    /**
     * Метод прибавляет к дате количество периодов(periodsCount)
     */
    private fun datePlusPeriods(period: Period, date: ZonedDateTime, periodsCount: Int): ZonedDateTime {
        return when(period) {
            Period.DAY -> date.plusDays(periodsCount.toLong() * 1)
            Period.WEEK -> date.plusDays(periodsCount.toLong() * 7)
            Period.MONTH -> date.plusMonths(periodsCount.toLong() * 1)
            Period.QUARTER -> date.plusMonths(periodsCount.toLong() * 3)
            Period.HALF_YEAR -> date.plusMonths(periodsCount.toLong() * 6)
            Period.YEAR -> date.plusYears(periodsCount.toLong() * 1)
        }
    }

    /**
     * Метод определения первого дня периода.
     * period - отчетный период
     */
    private fun getStartDate(period: Period, now: ZonedDateTime): ZonedDateTime {
        val startToday = now.with(ChronoField.NANO_OF_DAY, 0)
        val dayOfYear = now.dayOfYear
        val year = now.year
        return when (period) {
            Period.DAY -> startToday
            Period.WEEK -> startToday.minusDays(now.dayOfWeek.value.toLong() - 1)
            Period.MONTH -> startToday.minusDays(now.dayOfMonth.toLong() - 1)
            Period.QUARTER -> {
                val secondQuarterStart = LocalDate.of(year, 4, 1)
                val thirdQuarterStart = LocalDate.of(year, 7, 1)
                val fourthQuarterStart = LocalDate.of(year, 10, 1)
                val shift = if (dayOfYear < secondQuarterStart.dayOfYear) 0
                else if (secondQuarterStart.dayOfYear <= dayOfYear && dayOfYear < thirdQuarterStart.dayOfYear) {
                    secondQuarterStart.dayOfYear
                } else if (thirdQuarterStart.dayOfYear <= dayOfYear && dayOfYear < fourthQuarterStart.dayOfYear) {
                    thirdQuarterStart.dayOfYear
                } else {
                    fourthQuarterStart.dayOfYear
                }
                return startToday.minusDays((dayOfYear - shift - 1).toLong())
            }
            Period.HALF_YEAR -> {
                val halfYear = LocalDate.of(year, 7, 1)
                return if (dayOfYear < halfYear.dayOfYear) {
                    startToday.minusDays(dayOfYear.toLong() - 1)
                } else {
                    val shift = halfYear.dayOfYear
                    return startToday.minusDays((dayOfYear - shift - 1).toLong())
                }
            }
            Period.YEAR -> startToday.minusDays(dayOfYear.toLong() - 1)
        }
    }

    fun getRelativeFacts(regionId: Int, changes: FactChanges, roles: String): List<RelativeFactResp> {
        val result = LinkedHashSet<RelativeFactResp>()
        val queue = LinkedList<ChangedFact>(changes.changes)
        val allRelatives = factRelativesRepository.findAllByRegionId(regionId)

        //ключ - ID зависимого показателя, значение - список связанных показателей
        val relativesByDependentId = HashMap<Int, MutableList<FactDependentEntity>>()
        //ключ - ID связанного показателя, значение - список зависимых показателей
        val dependentsByRelativeId = HashMap<Int, MutableList<FactDependentEntity>>()
        allRelatives.forEach { r:FactDependentEntity->
            if (relativesByDependentId.containsKey(r.factPassportFk)) {
                relativesByDependentId[r.factPassportFk]!!.add(r)
            } else {
                relativesByDependentId[r.factPassportFk] = mutableListOf(r)
            }
            if (dependentsByRelativeId.containsKey(r.factFk)) {
                dependentsByRelativeId[r.factFk]!!.add(r)
            } else {
                dependentsByRelativeId[r.factFk] = mutableListOf(r)
            }
        }
        while (queue.isNotEmpty()) {
            val currentChangedFact = queue.poll()
            result.add(RelativeFactResp(currentChangedFact.factId, currentChangedFact.reportPeriod, getDimValues(currentChangedFact.reportPeriod.toString())))
            //получаем все строки, где текущий связанный для определения всех зависимых от текущего
            val dependents = dependentsByRelativeId[currentChangedFact.factId] ?: continue
            for(curDependent in dependents) {
                var isNeedToAddToResult = true
                val dependentId = curDependent.factPassportFk
                val offset = curDependent.reportOffset
                val dependentPeriod = Period.fromInt(curDependent.dependentsPeriod)
                val dependentShiftsPeriod = if (offset == 0) currentChangedFact.reportPeriod.toLocalDate()
                else getNextPeriodFirstDate(dependentPeriod, currentChangedFact.reportPeriod, offset)
                val dependent = RelativeFactResp(dependentId, dependentShiftsPeriod.atStartOfDay(ZoneId.systemDefault()), getDimValues(dependentShiftsPeriod.toString()))
                if (result.contains(dependent)) {
                    continue
                }
                //получаем все связанные показатели для текущего зависимого
                val relatives = relativesByDependentId[dependentId] ?: continue
                for (relative in relatives) {
                    val relativeId = relative.factFk
                    val relativeOffset = relative.reportOffset
                    val relativePeriod = Period.fromInt(curDependent.relativesPeriod)
                    val relShiftsPeriod = if (relativeOffset == 0) dependentShiftsPeriod
                    else getPrevPeriodFirstDate(relativePeriod, dependentShiftsPeriod.atStartOfDay(ZoneId.systemDefault()), relativeOffset)
                    val dimValues = getDimValues(relShiftsPeriod.toString())
                    val factValues = storeRestClient.getFactValue(roles, dimValues, relativeId, regionId, relative.dataSourceFk)
                    if (factValues.isEmpty()) {
                        isNeedToAddToResult = false
                        break
                    }
                }
                if (isNeedToAddToResult) {
                    queue.offer(ChangedFact(dependentId, dependentShiftsPeriod.atStartOfDay(ZoneId.systemDefault())))
                }
            }
        }

        return result.toList()
    }

    private fun getDimValues(date: String) = listOf(DimensionValue(dimensionId = PERIOD_DIM_ID, value = date))
}