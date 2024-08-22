package com.example.ConfigService.repository

import com.example.ConfigService.model.FactShortDto
import com.example.ConfigService.model.FactTrend
import com.example.ConfigService.model.entity.FactEntity
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import ru.rieksp.coreservice.exception.CoreException
import java.util.Optional

@Repository
class FactRepository(private val dsl: DSLContext) {

    private val log = LoggerFactory.getLogger(javaClass)
    fun findFactByIdAndRegionId(factId: Int, regionId: Int): FactEntity {
        return dsl.select(FACTS.ID,
            FACTS.REGION_FK,
            REGIONS.NAME.`as`("regionName"),
            FACTS.DATA_SOURCE_FK,
            D_FACT_NAMES.NAME.`as`("factName"),
            D_FACT_NAMES.GOAL.`as`("goal"),
            FACTS.MEASUREMENT_UNIT_FK.`as`("measurementUnitId"),
            D_MEASUREMENT_UNITS.NAME.`as`("measurementUnitName"),
            D_MEASUREMENT_UNITS.SHORT_NAME.`as`("measurementUnitShortName"),
            FACTS.GOAL_TYPE,
            FACTS.HIGH_THRESHOLD,
            FACTS.LOW_THRESHOLD,
            FACTS.HIGH_THRESHOLD_PCT,
            FACTS.LOW_THRESHOLD_PCT,
            FACTS.NORM_ACT_FK,
            D_NORM_ACTS.NAME.`as`("normActName"),
            FACTS.ACTUAL_SINCE,
            FACTS.PERIOD,
            FACTS.PLAN_LOAD_DAYS,
            FACTS.FACT_LOAD_DATE,
            FACTS.VIEW_MODULE_FK.`as`("moduleId"),
            VIEW_MODULES.NAME.`as`("moduleName"),
            FACTS.RESP_USER_FK,
            USERS.NAME.`as`("respUserName"),
            USERS.LAST_NAME.`as`("respUserLastName"),
            USERS.MIDDLE_NAME.`as`("respUserMiddleName"),
            USERS.REGION_DIV_FK.`as`("respUserDivisionId"),
            REGION_DIVISIONS.SHORT_NAME.`as`("respUserDivisionShortName"),
            REGION_DIVISIONS.LONG_NAME.`as`("respUserDivisionFullName"))
            .from(FACTS).join(REGIONS).on(FACTS.REGION_FK.eq(REGIONS.ID))
            .join(D_FACT_NAMES).on(FACTS.FACT_NAME_FK.eq(D_FACT_NAMES.ID))
            .join(D_MEASUREMENT_UNITS).on(FACTS.MEASUREMENT_UNIT_FK.eq(D_MEASUREMENT_UNITS.ID))
            .leftJoin(D_NORM_ACTS).on(FACTS.NORM_ACT_FK.eq(D_NORM_ACTS.ID))
            .leftJoin(VIEW_MODULES).on(VIEW_MODULES.ID.eq(FACTS.VIEW_MODULE_FK))
            .leftJoin(USERS).on(FACTS.RESP_USER_FK.eq(USERS.ID))
            .leftJoin(REGION_DIVISIONS).on(USERS.REGION_DIV_FK.eq(REGION_DIVISIONS.ID))
            .where(FACTS.ID.eq(factId).and(FACTS.REGION_FK.eq(regionId)))
            .fetchOptional()
            .orElseThrow {
                log.warn("Не найден показатель по ID = $factId и ID региона = $regionId")
                throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "")
            }
            .into(FactEntity::class.java)
    }

    fun findFactsByModuleId(moduleId: Int): List<FactTrend> {
        return dsl.select(FACTS.ID,
            FACTS.GOAL_TYPE,
            FACTS.HIGH_THRESHOLD,
            FACTS.LOW_THRESHOLD)
            .from(FACTS)
            .where(FACTS.VIEW_MODULE_FK.eq(moduleId))
            .fetch { r -> r.into(FactTrend::class.java) }
    }

    fun findFacts(currentUserId: Int, regionId: Int, text: String?, isPinned: Boolean?, moduleId: Int?, offset: Int, limit: Int): List<FactShortDto> {
        var condition = FACTS.REGION_FK.eq(regionId)
        text?.let { condition = condition.and(D_FACT_NAMES.NAME.likeIgnoreCase("%$it%")
            .or(D_FACT_NAMES.SHORT_NAME.likeIgnoreCase("%$it%"))) }
        if (isPinned == null) {
            moduleId?.let { condition = condition.and(FACTS.VIEW_MODULE_FK.eq(it)) }
            return dsl.select(FACTS.ID, D_FACT_NAMES.NAME)
                .from(FACTS).join(D_FACT_NAMES).on(FACTS.FACT_NAME_FK.eq(D_FACT_NAMES.ID))
                .where(condition)
                .orderBy(D_FACT_NAMES.NAME)
                .limit(limit)
                .offset(offset)
                .fetch { r -> r.into(FactShortDto::class.java) }
        }
        if (isPinned) {
            moduleId?.let { condition = condition.and(USER_FACTS.MODULE_FK.eq(moduleId)) }
            return dsl.select(FACTS.ID, D_FACT_NAMES.NAME)
                .from(FACTS).join(D_FACT_NAMES).on(FACTS.FACT_NAME_FK.eq(D_FACT_NAMES.ID))
                .join(USER_FACTS).on(USER_FACTS.USER_FK.eq(currentUserId).and(USER_FACTS.FACT_FK.eq(FACTS.ID)))
                .where(condition)
                .orderBy(USER_FACTS.ORDER_NUM)
                .limit(limit)
                .offset(offset)
                .fetch { r -> r.into(FactShortDto::class.java) }
        } else {
            moduleId?.let { condition = condition.and(FACTS.VIEW_MODULE_FK.eq(moduleId)) }
            return dsl.select(FACTS.ID, D_FACT_NAMES.NAME)
                .from(FACTS)
                .join(D_FACT_NAMES).on(FACTS.FACT_NAME_FK.eq(D_FACT_NAMES.ID))
                .where(condition)
                .orderBy(D_FACT_NAMES.NAME)
                .limit(limit)
                .offset(offset)
                .fetch { r -> r.into(FactShortDto::class.java) }
        }
    }

    fun findDimIdsByFactId(factId: Int): MutableList<Int> =
        dsl.select(FactDimensions.FACT_DIMENSIONS.DIMENSION_FK)
            .from(FactDimensions.FACT_DIMENSIONS)
            .where(FactDimensions.FACT_DIMENSIONS.FACT_FK.eq(factId))
            .fetch { r -> r.into(Int::class.java) }

    fun findPinnedToDashboardFacts(userId: Int): List<FactShortDto> {
        return dsl.select(FACTS.ID,
            D_FACT_NAMES.NAME)
            .from(FACTS)
            .join(D_FACT_NAMES).on(FACTS.FACT_NAME_FK.eq(D_FACT_NAMES.ID))
            .join(USER_FACTS).on(USER_FACTS.FACT_FK.eq(FACTS.ID)
                .and(USER_FACTS.USER_FK.eq(userId))
                .and(USER_FACTS.MODULE_FK.isNull))
            .orderBy(USER_FACTS.ORDER_NUM)
            .fetch { r -> r.into(FactShortDto::class.java) }
    }

    fun findRespUserByFactId(factId: Int): Optional<FactRespUserEntity> {
        return dsl.select(USERS.ID.`as`("id"),
            USERS.NAME.`as`("name"),
            USERS.LAST_NAME.`as`("lastName"),
            USERS.MIDDLE_NAME.`as`("middleName"),
            REGION_DIVISIONS.ID.`as`("divisionId"),
            REGION_DIVISIONS.SHORT_NAME.`as`("divisionShortName"),
            REGION_DIVISIONS.LONG_NAME.`as`("divisionFullName"))
            .from(USERS)
            .leftJoin(REGION_DIVISIONS).on(USERS.REGION_DIV_FK.eq(REGION_DIVISIONS.ID))
            .join(FACT_PASSPORTS).on(FACT_PASSPORTS.RESP_USER_FK.eq(USERS.ID).and(FACT_PASSPORTS.ID.eq(factId)))
            .fetchOptional { r -> r.into(FactRespUserEntity::class.java) }
    }

    fun findFactNames(regionId: Int, text: String?): List<FactName> {
        var condntion = D_FACT_NAMES.REGION_FK.eq(regionId)
        text?.let { condntion = condntion.and(D_FACT_NAMES.NAME.likeIgnoreCase("%$it%")
            .or(D_FACT_NAMES.SHORT_NAME.likeIgnoreCase("%$it%"))
            .or(D_FACT_NAMES.GOAL.likeIgnoreCase("%$it%"))) }
        return dsl.select(D_FACT_NAMES.ID,
            D_FACT_NAMES.NAME,
            D_FACT_NAMES.GOAL)
            .from(D_FACT_NAMES)
            .where(condntion)
            .orderBy(D_FACT_NAMES.NAME, D_FACT_NAMES.GOAL)
            .fetch { r -> r.into(FactName::class.java) }
    }
}