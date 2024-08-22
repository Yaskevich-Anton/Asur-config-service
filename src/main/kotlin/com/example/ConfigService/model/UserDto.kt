package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Пользователь")
class UserDto (
    @Schema(description = "ID")
    val id: Int) {

    @Schema(description = "Регион пользователя")
    var regionDto: RegionDto? = null

    @Schema(description = "Имя пользователя")
    var name: String? = null

    @Schema(description = "Фамилия пользователя")
    var lastName: String? = null

    @Schema(description = "Отчество пользователя")
    var middleName: String? = null

    @Schema(description = "Должность пользователя")
    var position: String? = null

    @Schema(description = "Менеджер пользователя")
    var manager: UserDto? = null

    @Schema(description = "Подразделение региона")
    var regionDivisionDto: RegionDivisionDto? = null
}
