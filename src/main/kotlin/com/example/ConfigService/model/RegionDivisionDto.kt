package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Структурные подразделения региона")
class RegionDivisionDto (
    @Schema(description = "ID")
    val id: Int) {

    @Schema(description = "Сокращенное название подразделения")
    val shortName: String? = null

    @Schema(description = "Полное название подразделения")
    val longName: String? = null

    @Schema(description = "Регион подразделения")
    val regionDto: RegionDto? = null

    @Schema(description = "Руководитель подразделения")
    val leadUser: UserDto? = null

    @Schema(description = "Вышестоящее подразделение")
    val parentDivision: RegionDivisionDto? = null
}