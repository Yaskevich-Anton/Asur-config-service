package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.ZonedDateTime

@Schema(description = "Данные для получения значения измерения")
data class DimensionRequestBody(
    @Schema(description = "Время актуальности значений измерений")
    val actualDate: ZonedDateTime?
)
