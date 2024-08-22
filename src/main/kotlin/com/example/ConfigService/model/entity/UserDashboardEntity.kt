package com.example.ConfigService.model.entity

data class UserDashboardEntity(val id: Int,
    val userId: Int,
    val factId: Int,
    val dimValues: String? = null,
    val viewParams: String? = null)