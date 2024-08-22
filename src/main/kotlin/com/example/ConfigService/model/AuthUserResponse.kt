package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus


data class AuthUserResponse(val userId: Int,
                            val login: String,
                            val name: String,
                            val lastName: String,
                            val middleName: String?,
                            val email: String,
                            val role: RoleDto,
                            val status: UserStatus,
                            val isNeedChange: Boolean) {
}