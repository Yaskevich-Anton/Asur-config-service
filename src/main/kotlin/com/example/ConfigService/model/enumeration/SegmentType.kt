package com.example.ConfigService.model.enumeration

enum class SegmentType(val value: Int) {
    FEDERAL(1),
    REGIONAL(2),
    MUNICIPAL(3),
    DEPARTMENTAL(4);

    companion object {
        fun fromInt(value: Int): SegmentType =
            when (value) {
                1 -> FEDERAL
                2 -> REGIONAL
                3 -> MUNICIPAL
                4 -> DEPARTMENTAL
                else -> throw RuntimeException("Неверный сегмент источника данных показателя")
            }
    }
}