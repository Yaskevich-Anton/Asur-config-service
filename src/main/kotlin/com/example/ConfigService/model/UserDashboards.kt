package com.example.ConfigService.model

data class UserDashboards(val id: Int,
    val userId: Int,
    val factId: Int,
    val dimensionValues: List<DimensionValue>? = null,
    val viewParams: String? = null) {
}