package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus


class UserResponse(
        val userId: Int,
        val login: String,
        val name: String,
        val lastName: String,
        val middleName: String? = null,
        val email: String,
        val position: String,
        val reason: String? = null,
        val role: RoleDto,
        val status: UserStatus,
        val manager: UserPagedDto? = null,
        val division: RegionDivisionStructureDto? = null,
        var department: String? = null
) {
}