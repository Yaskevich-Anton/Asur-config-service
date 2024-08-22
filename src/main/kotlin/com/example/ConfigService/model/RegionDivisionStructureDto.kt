package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Структурное подразделение")
class RegionDivisionStructureDto(
    @Schema(description = "ID")
    val id: Int,

    @Schema(description = "Сокращенное название подразделения")
    val shortName: String,

    @Schema(description = "Полное название подразделения")
    val fullName: String,

    @Schema(description = "ID региона подразделения")
    val regionId: Int,

    @Schema(description = "Ведомство, в которое входит данное подразделение.")
    var department: String? = null,

    @Schema(description = "Руководитель подразделения")
    val leadUser: UserPagedDto? = null,

    @Schema(description = "Список подчиненных подразделений")
    val divisions: MutableList<RegionDivisionStructureDto> = mutableListOf()) {
}