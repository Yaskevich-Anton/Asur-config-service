package com.example.ConfigService.model

import java.time.ZonedDateTime

data class RelativeFactResp(val factId: Int, val reportPeriod: ZonedDateTime, val dimensionValues: List<DimensionValue>)
