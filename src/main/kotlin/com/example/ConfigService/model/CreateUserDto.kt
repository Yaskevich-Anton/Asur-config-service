package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@Schema(description = "Данные для создания пользователя")
class CreateUserDto(
    @Schema(description = "Логин пользователя")
    @field:NotBlank(message = "Логин пользователя не должен быть пустым.")
    var login: String,

    @Schema(description = "Имя пользователя")
    @field:NotBlank(message = "Имя пользователя не должно быть пустым.")
    val name: String,

    @Schema(description = "Фамилия пользователя")
    @field:NotBlank(message = "Фамилия пользователя не должна быть пустой.")
    val lastName: String,

    @Schema(description = "Отчество пользователя")
    val middleName: String,

    @Schema(description = "Email пользователя")
    @field:NotBlank(message = "Email пользователя не должен быть пустым.")
    @field:Email
    var email: String,

    @Schema(description = "Должность пользователя")
    @field:NotBlank(message = "Должность пользователя не должна быть пустой.")
    val position: String,

    @Schema(description = "Причина блокировки")
    val reason: String? = null,

    @Schema(description = "Код роли пользователя")
    @field:NotBlank(message = "Код роли пользователя не должен быть пустым.")
    val role: String,

    @Schema(description = "ID руководителя")
    @field:Min(1, message = "Значение ID руководителя пользователя не может быть меньше 1.")
    val managerId: Int? = null,

    @Schema(description = "ID подразделения пользователя")
    @field:NotNull(message = "ID подразделения пользователя не должен быть пустым.")
    val divisionId: Int,
) {
}