package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные пользователей для отправки на согласование")
data class UsersToRequestStatus(
    @Schema(description = "Список ID пользователей")
    @field:NotNull
    val userIds: List<Int>
)
