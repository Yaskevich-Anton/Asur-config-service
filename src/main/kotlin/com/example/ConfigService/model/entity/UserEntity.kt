package com.example.ConfigService.model.entity

import com.example.ConfigService.model.enumeration.UserStatus


class UserEntity(
        val id: Int,
        val regionFk: Int,
        val name: String,
        val lastName: String,
        val middleName: String?,
        val status: UserStatus,
        val email: String,
        val createdDate: Long? = null,
        val position: String,
        val managerUserFk: Int?,
        val regionDivFk: Int?,
        val reason: String? = null
) {
}