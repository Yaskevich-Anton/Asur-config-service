package com.example.ConfigService.model.entity

data class FactRelativesEntity(val id: Int,
    val factPassportFk: Int,
    val factFk: Int,
    val dataSourceFk: Int,
    val name: String,
    val orderNum: Int,
    val reportOffset: Int
)
