package com.example.ConfigService.model.entity

class RegionDivisionEntity(
    val id: Int,
    val shortName: String,
    val longName: String,
    val regionFk: Int,
    val leadUserFk: Int?,
    val parentDivisionFk: Int?,
) {
}