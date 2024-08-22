package com.example.ConfigService.model.entity

import com.example.ConfigService.model.enumeration.UserStatus


class UserPagedEntity(
        val id: Int,
        val name: String,
        val lastName: String,
        val middleName: String? = null,
        val status: UserStatus,
        val position: String,
        val email: String,
        val createdDate: Long,
        val reason: String? = null,
        val isDepartment: Boolean? = null,
        val managerId: Int? = null,
        val managerName: String? = null,
        val managerLastName: String? = null,
        val managerMiddleName: String? = null,
        val managerStatus: UserStatus? = null,
        val managerEmail: String? = null,
        val managerCreatedDate: Long? = null,
        val managerPosition: String? = null,
        val divisionId: Int? = null,
        val divisionRegionId: Int? = null,
        val divisionShortName: String? = null,
        val divisionLongName: String? = null,
        val divisionManagerId: Int? = null,
        val divisionManagerName: String? = null,
        val divisionManagerLastName: String? = null,
        val divisionManagerMiddleName: String? = null,
        val divisionManagerEmail: String? = null,
        val divisionManagerCreatedDate: Long? = null,
        val divisionManagerStatus: UserStatus? = null,
        val divisionManagerPosition: String? = null,
) {
}