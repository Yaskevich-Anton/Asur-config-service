package com.example.ConfigService.model

class FactTrend(val id: Int,
                val goalType: Int,
                val highThreshold: Float,
                val lowThreshold: Float) {
    var trend: Double? = null
}