package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

class PinFactDto(
    @Schema(description = "ID модуля для закрепления.")
    @field:Min(1, message = "ID модуля для закрепления не может быть меньше 1.")
    val viewModuleId: Int? = null,

    @Schema(description = "ID закрепленного уже показателя, перед которым нужно расположить указанный.")
    @field:Min(1, message = "ID закрепленного уже показателя не может быть меньше 1.")
    val before: Int? = null,
) {
}