package com.example.ConfigService.service

import com.example.ConfigService.model.UserDashboards
import com.example.ConfigService.model.UserSettingsCreate
import com.example.ConfigService.model.mapper.DimensionValueMapper
import com.example.ConfigService.repository.UserDashboardRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Collectors

@Service
class UserDashboardService(private val userDashBoardRepository: UserDashboardRepository,
                           private val dimensionValueMapper: DimensionValueMapper) {

    private val log = LoggerFactory.getLogger(javaClass)
    fun getDashboardSettings(userId: Int, factId: Int): Optional<UserDashboards> {
        val optionalSettings = userDashBoardRepository.findDashboardSettings(userId, factId)
        return if (optionalSettings.isPresent) {
            val settings = optionalSettings.get()
            val dimensionValues = settings.dimValues?.let { dimValues:String ->
                dimensionValueMapper.fromJson(dimValues)
            }
            val userDashboards = UserDashboards(settings.id, settings.userId, settings.factId, dimensionValues, settings.viewParams)
            Optional.of(userDashboards)
        } else {
            Optional.empty()
        }
    }

    fun saveDashboardSettings(userId: Int, factId: Int, userSettingsCreate: UserSettingsCreate) {
        val dimensions: List<String>? = userSettingsCreate.dimensionValues?.let {
            it.stream()
            .map { d -> dimensionValueMapper.toJson(d) }
            .collect(Collectors.toList())
        }
        val strDimensions: String? = dimensions?.let { dimensions.toString() }
        val optionalSettings = userDashBoardRepository.findDashboardSettings(userId, factId)
        if (optionalSettings.isPresent) {
            userDashBoardRepository.updateDashboardSettings(userId, factId, strDimensions, userSettingsCreate.viewParams)
        } else {
            userDashBoardRepository.saveDashboardSettings(userId, factId, strDimensions, userSettingsCreate.viewParams)
        }
    }
}