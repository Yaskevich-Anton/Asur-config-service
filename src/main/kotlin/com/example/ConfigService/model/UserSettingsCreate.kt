package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Hастройки дашборда показателя")
data class UserSettingsCreate(
    @Schema(description = "Список значений, которые фиксируют показываемое значение показателя по разным измерениям(осям)")
    val dimensionValues: List<DimensionValue>? = null,
    @Schema(description = "Объект, содержимое которого определяется атрибутами визуальных компонентов")
    val viewParams: String? = null
)
