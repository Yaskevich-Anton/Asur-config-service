package com.example.ConfigService.model.mapper

import com.example.ConfigService.model.*
import com.example.ConfigService.model.entity.FactEntity
import com.example.ConfigService.model.enumeration.GoalTypes
import com.example.ConfigService.model.enumeration.Period
import com.example.ConfigService.model.enumeration.Trend
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class FactMapper {

    fun toFactInfo(factEntity: FactEntity,
                   date: ZonedDateTime,
                   dimensionValues: List<DimensionValue>,
                   value: Double?,
                   prevValue: Double?,
                   trendDelta: Double? = null,
                   trend: Trend? = null,
                   isPinnedByDesktop: Boolean,
                   isPinnedByModule: Boolean,
                   dataSource: DataSource,
                   respUser: RespUser? = null): FactInfoResponse {
        val offset: Int = ZonedDateTime.now().offset.totalSeconds
        val actualSince = ZonedDateTime.ofInstant(Instant.ofEpochMilli(factEntity.actualSince), ZoneId.of("UTC"))
            .plusSeconds(offset.toLong())
        val factLoadDate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(factEntity.factLoadDate), ZoneId.of("UTC"))
            .plusSeconds(offset.toLong())
        val measurementUnit = MeasurementUnit(factEntity.measurementUnitId, factEntity.measurementUnitName, factEntity.measurementUnitShortName)
        val fact = FactInfoResponse(factEntity.id,
            factEntity.factName,
            factEntity.regionFk,
            measurementUnit,
            date,
            factLoadDate,
            date,
            dimensionValues,
            isPinnedByDesktop,
            isPinnedByModule,
            dataSource)

        respUser?.let { fact.respUser = it }
        trendDelta?.let { fact.trendDelta = it }
        trend?.let { fact.trend = it }
        fact.goalType = factEntity.goalType?.let { GoalTypes.fromInt(it) }
        fact.highThreshold = factEntity.highThreshold
        fact.lowThreshold = factEntity.lowThreshold
        fact.highThresholdPct = factEntity.highThresholdPct
        fact.lowThresholdPct = factEntity.lowThresholdPct
        if (factEntity.normActFk != null && factEntity.normActName != null) {
            fact.normAct = ShortNormAct(factEntity.normActFk, factEntity.normActName)
        }
        factEntity.goal?.let { fact.goal = it }
        fact.actualSince = actualSince
        fact.period = Period.fromInt(factEntity.period)
        value?.let { fact.reportValue = it }
        prevValue?.let { fact.prevReportValue = it }
        factEntity.moduleId?.let { fact.module = ModuleShortDto(it, factEntity.moduleName!!) }
        return fact
    }
}