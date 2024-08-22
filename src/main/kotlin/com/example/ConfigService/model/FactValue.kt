package com.example.ConfigService.model

data class FactValue(val valueId: Int,
    val value:Double,
    val dimensionValues: List<DimensionValue>,
) {
}