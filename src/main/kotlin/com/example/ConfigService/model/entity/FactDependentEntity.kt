package com.example.ConfigService.model.entity

data class FactDependentEntity(val id: Int,
    val factPassportFk: Int,
    val factFk: Int,
    val dataSourceFk: Int,
    val name: String,
    val orderNum: Int,
    val reportOffset: Int,
    val dependentDataSource: Int,
    val dependentsPeriod: Int,
    val relativesPeriod: Int
)
