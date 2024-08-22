package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные для согласования пользователя")
data class UserApprove(
    @Schema(description = "Результат согласования пользователя")
    @field:NotNull(message = "Поле 'Результат согласования пользователя' не может быть пустым")
    val result: Boolean,

    @Schema(description = "Причина блокировки")
    val reason:String? = null
) {}
