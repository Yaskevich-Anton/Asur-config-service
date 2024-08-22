package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные для обновления статуса у группы пользователей")
data class InternalBatchUpdateStatus(
        @Schema(description = "Список ID пользователей")
    @field:NotNull(message = "Список ID пользователей не должен быть пустым.")
    val userIds: List<Int>,

        @Schema(description = "Статус пользователей")
    @field:NotNull(message = "Статус пользователей не должен быть пустым.")
    val status: UserStatus,

        @Schema(description = "ID региона")
    @field:NotNull(message = "ID региона не должен быть пустым.")
    @field:Min(value = 1, message = "ID региона не может быть меньше 1.")
    val regionId: Int,
) {}
