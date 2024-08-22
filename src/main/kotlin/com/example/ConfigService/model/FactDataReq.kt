package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные для получения значений показателей для указанных ограничений по измерениям.")
data class FactDataReq(
    @Schema(description = "Флаг, определяющий показ значения из области предварительной загрузки для предварительного просмотра и проверки перед загрузкой в основное хранилище")
    val isPublished: Boolean = true,
    @Schema(description = "Список значений, которые фиксируют показываемое значение показателя по разным измерениям")
    val dimensionValues: List<DimensionValue> = listOf(),
    @Schema(description = "ID источника данных")
    @field:NotNull
    @field:Min(1)
    val dataSource: Int
)
