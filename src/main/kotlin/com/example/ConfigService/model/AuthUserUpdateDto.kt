package com.example.ConfigService.model

class AuthUserUpdateDto(
    val login: String,
    val name: String,
    val lastName: String,
    val middleName: String,
    var reason: String?,
    val email: String,
    val position: String,
    val managerId: Int?,
    val roleId: Int,
) {
}