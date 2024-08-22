package com.example.ConfigService.model.enumeration

enum class GoalTypes(val value: Int) {
    INCREASE(1),
    DECLINE(2);
    companion object {
        fun fromInt(value: Int): GoalTypes =
            when (value) {
                1 -> INCREASE
                2 -> DECLINE
                else -> throw RuntimeException("Неизвестное направление тренда")
            }
    }
}