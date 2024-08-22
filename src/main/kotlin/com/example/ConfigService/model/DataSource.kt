package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.SegmentType

data class DataSource(val id: Int,
                      val regionFk: Int,
                      val name: String,
                      val description: String,
                      val segment: SegmentType,
                      val respDiv: ShortRegionDivision,
                      val respUser: RespUser )
