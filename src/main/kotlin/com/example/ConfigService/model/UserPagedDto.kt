package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.ZonedDateTime

@Schema(description = "Данные пользователя")
class UserPagedDto(
        @Schema(description = "ID")
    val id: Int,

        @Schema(description = "Имя пользователя")
    var name: String,

        @Schema(description = "Фамилия пользователя")
    var lastName: String,

        @Schema(description = "Отчество пользователя")
    var middleName: String?,

        @Schema(description = "Email пользователя")
    var email: String,

        @Schema(description = "Дата и время создания пользователя")
    val createdDate: ZonedDateTime,

        @Schema(description = "Статус пользователя")
    var status: UserStatus,

        @Schema(description = "Должность пользователя")
    val position: String,

        @Schema(description = "Руководитель пользователя")
    val manager: UserShortDto? = null,

        @Schema(description = "Подразделение пользователя")
    val division: RegionDivisionStructureDto? = null,

        @Schema(description = "Роль пользователя")
    val role: RoleDto? = null,
) {
}