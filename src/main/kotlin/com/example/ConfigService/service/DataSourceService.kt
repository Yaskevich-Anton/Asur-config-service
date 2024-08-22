package com.example.ConfigService.service

import com.example.ConfigService.model.DataSource
import com.example.ConfigService.model.RespUser
import com.example.ConfigService.model.ShortRegionDivision
import com.example.ConfigService.model.enumeration.SegmentType
import com.example.ConfigService.repository.DataSourceRepository
import org.springframework.stereotype.Service

@Service
class DataSourceService(private val dataSourceRepository: DataSourceRepository) {

    fun getDataSourceByIdAndFactId(id: Int, factId: Int): DataSource {
        val dataSource = dataSourceRepository.findDataSourceByIdAndFactId(id, factId)
        val respDivision = ShortRegionDivision(dataSource.respDivFk, dataSource.respDivShortName, dataSource.respDivLongName)
        val respUserDivision = dataSource.respUserDivId?.let {
            ShortRegionDivision(dataSource.respUserDivId, dataSource.respUserDivShortName!!, dataSource.respUserDivLongName!!)
        }
        val respUser = RespUser(dataSource.respUserFk, dataSource.respUserName, dataSource.respUserLastName, dataSource.respUserMiddleName, division = respUserDivision)
        return DataSource(dataSource.id,
            dataSource.regionFk,
            dataSource.name,
            dataSource.description,
            SegmentType.fromInt(dataSource.segment),
            respDivision,
            respUser)
    }
}