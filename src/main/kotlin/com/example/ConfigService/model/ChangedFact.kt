package com.example.ConfigService.model

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.ZonedDateTime

data class ChangedFact(val factId: Int,
                       @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone="Europe/UTC")
                       val reportPeriod: ZonedDateTime)
