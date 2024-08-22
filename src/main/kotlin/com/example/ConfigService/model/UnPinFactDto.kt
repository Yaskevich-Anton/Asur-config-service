package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min

data class UnPinFactDto(
    @Schema(description = "ID модуля для закрепления.")
    @field:Min(1, message = "ID модуля для закрепления не может быть меньше 1.")
    val viewModuleId: Int? = null
)
