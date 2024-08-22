package com.example.ConfigService.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Регион")
class RegionDto(
    @Schema(description = "ID")
    val id: Int) {

    @Schema(description = "Название региона")
    var name: String? = null
}
