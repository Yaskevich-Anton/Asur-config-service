package com.example.ConfigService.model.entity

data class FactRespUserEntity(val id: Int,
    val name: String,
    val lastName: String,
    val middleName: String? = null,
    val divisionId: Int? = null,
    val divisionShortName: String? = null,
    val divisionFullName: String? = null)
