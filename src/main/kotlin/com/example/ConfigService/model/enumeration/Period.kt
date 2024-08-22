package com.example.ConfigService.model.enumeration

enum class Period(val value: Int) {
    DAY(1),
    WEEK(2),
    MONTH(3),
    QUARTER(4),
    HALF_YEAR(5),
    YEAR(6);

    companion object {
        fun fromInt(value: Int): Period =
            when (value) {
                1 -> DAY
                2 -> WEEK
                3 -> MONTH
                4 -> QUARTER
                5 -> HALF_YEAR
                6 -> YEAR
                else -> throw RuntimeException("Неверный период расчета показателя")
            }
    }
}