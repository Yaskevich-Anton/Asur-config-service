package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Данные пользователя")
class UserShortDto(
    @Schema(description = "ID")
    val id: Int,

    @Schema(description = "Имя пользователя")
    var name: String,

    @Schema(description = "Фамилия пользователя")
    var lastName: String,

    @Schema(description = "Отчество пользователя")
    var middleName: String?,

    @Schema(description = "Должность пользователя")
    var position: String
) {
}