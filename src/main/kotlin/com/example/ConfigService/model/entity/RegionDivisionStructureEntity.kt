package com.example.ConfigService.model.entity

import com.example.ConfigService.model.enumeration.UserStatus


data class RegionDivisionStructureEntity(
        val id: Int,
        val shortName: String,
        val longName: String,
        val regionFk: Int,
        val isDepartment: Boolean,
        val managerId: Int? = null,
        val managerName: String? = null,
        val managerLastName: String? = null,
        val managerMiddleName: String? = null,
        val managerEmail: String? = null,
        val managerCreatedDate: Long? = null,
        val managerStatus: UserStatus? = null,
        val managerPosition: String? = null,
        val managerManagerId: Int? = null,
        val managerManagerName: String? = null,
        val managerManagerLastName: String? = null,
        val managerManagerMiddleName: String? = null,
        val managerManagerPosition: String? = null,
        val managerDivisionId: Int? = null,
        val managerDivisionRegionId: Int? = null,
        val managerDivisionShortName: String? = null,
        val managerDivisionLongName: String? = null,
        val parentDivisionFk: Int? = null) {
}