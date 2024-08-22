package com.example.ConfigService.model

data class RespUser(val id: Int,
    val name: String,
    val lastName: String,
    val middleName: String? = null,
    val department: String? = null,
    val division: ShortRegionDivision? = null)
