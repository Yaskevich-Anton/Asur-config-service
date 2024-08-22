package com.example.ConfigService.model.entity

class UserFactsEntity(val id: Int,
    val userFk: Int,
    val factFk: Int,
    val moduleFk: Int? = null,
    val orderNum: Int
    ) {
}