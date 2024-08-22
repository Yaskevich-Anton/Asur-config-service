package com.example.ConfigService.model.entity

data class DataSourceEntity(val id: Int,
    val regionFk: Int,
    val name: String,
    val description: String,
    val segment: Int,
    val respDivFk: Int,
    val respDivShortName: String,
    val respDivLongName: String,
    val respUserFk: Int,
    val respUserName: String,
    val respUserLastName: String,
    val respUserMiddleName: String? = null,
    val respUserDivId: Int? = null,
    val respUserDivShortName: String? = null,
    val respUserDivLongName: String? = null)
