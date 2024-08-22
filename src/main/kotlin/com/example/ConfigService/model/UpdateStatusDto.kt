package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

@Schema(description = "Статус пользователя")
class UpdateStatusDto(
        @Schema(description = "Статус пользователя")
    @field:NotNull(message = "Статус пользователя не может быть пустым")
    val status: UserStatus,

        @Schema(description = "ID региона пользователя")
    @field:NotNull(message = "ID региона пользователя не может быть пустым")
    @field:Min(1, message = "ID региона пользователя не может меньше единицы")
    val regionId: Int
) {
}