package com.example.ConfigService.model

data class DimensionValue(val id: Int? = null,
     val dimensionId: Int,
     val value: String,
     val parentValueId: Int? = null,)
