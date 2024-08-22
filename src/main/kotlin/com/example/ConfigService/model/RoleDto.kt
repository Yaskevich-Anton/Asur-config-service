package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Роль пользователя")
class RoleDto() {
    @Schema(description = "ID")
    var id: Int? = null

    @Schema(description = "Название роли")
    var name: String? = null

    @Schema(description = "Код роли")
    var code: String? = null
}
