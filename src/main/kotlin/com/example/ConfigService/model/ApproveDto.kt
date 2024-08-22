package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные для согласования пользователей")
class ApproveDto (
    @Schema(description = "Список ID пользователей")
    @field:NotNull
    val userIds: List<Int>,

    @Schema(description = "Результат согласования пользователей")
    @field:NotNull(message = "Поле 'Результат согласования пользователей' не может быть пустым")
    val result: Boolean,

    @Schema(description = "Причина блокировки")
    val reason:String?
)
