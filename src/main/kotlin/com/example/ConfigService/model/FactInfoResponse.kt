package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.GoalTypes
import com.example.ConfigService.model.enumeration.Period
import com.example.ConfigService.model.enumeration.Trend
import java.time.ZonedDateTime

class FactInfoResponse(
    val factId: Int,
    val name: String,
    val regionId: Int,
    val measurementUnit: MeasurementUnit,
    val planLoadDate: ZonedDateTime,
    val factLoadDate: ZonedDateTime,
    val reportPeriod: ZonedDateTime,
    val dimensionValues: List<DimensionValue> = listOf(),
    val isPinned: Boolean,
    val isPinnedByModule: Boolean,
    val dataSource: DataSource
) {
    var goal: String? = null
    var goalType: GoalTypes? = null
    var highThreshold: Float? = null
    var lowThreshold: Float? = null
    var highThresholdPct: Float? = null
    var lowThresholdPct: Float? = null
    var normAct: ShortNormAct? = null
    var actualSince: ZonedDateTime? = null
    var period: Period? = null
    var respUser: RespUser? = null
    var trendDelta: Double? = null
    var trend: Trend? = null
    var reportValue: Double? = null
    var prevReportValue: Double? = null
    var module: ModuleShortDto? = null
    var relative: List<RelativeFact> = listOf()
    var dependent: List<FactInfoResponse> = listOf()
    var dimensions: List<DimensionValue> = listOf()

}
