package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus

class AuthCreateUserDto(
        val login: String,
        val regionId: Int,
        val name: String,
        val lastName: String,
        val middleName: String,
        val status: UserStatus,
        val email: String,
        val position: String,
        val reason: String?,
        val role: String,
) {
}