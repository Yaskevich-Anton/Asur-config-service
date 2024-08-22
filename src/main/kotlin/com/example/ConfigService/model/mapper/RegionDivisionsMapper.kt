package com.example.ConfigService.model.mapper

import com.example.ConfigService.model.RegionDivisionStructureDto
import com.example.ConfigService.model.UserPagedDto
import com.example.ConfigService.model.UserShortDto
import com.example.ConfigService.model.entity.RegionDivisionStructureEntity
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class RegionDivisionsMapper {

    fun toTreeStructureDto(entity: RegionDivisionStructureEntity,
                           divisionsByParentId: HashMap<Int?, MutableList<RegionDivisionStructureEntity>>,
                           departmentName: String?): RegionDivisionStructureDto {
        val currentDepartmentName = if (entity.isDepartment) entity.longName
        else departmentName
        val dto = toStructureDto(entity, currentDepartmentName)
        divisionsByParentId[entity.id]?.let {
            it.forEach {
                e -> dto.divisions.add(toTreeStructureDto(e, divisionsByParentId, currentDepartmentName))
            }
        }
        return dto
    }

    fun toStructureDto(entity: RegionDivisionStructureEntity, departmentName: String?): RegionDivisionStructureDto {
        val manager: UserPagedDto? = if (entity.managerId == null) null
        else {
            val offset: Int = ZonedDateTime.now().offset.totalSeconds
            val managerManager = if (entity.managerManagerId == null) null
            else {
                UserShortDto(entity.managerManagerId,
                    entity.managerManagerName!!,
                    entity.managerManagerLastName!!,
                    entity.managerManagerMiddleName,
                    entity.managerManagerPosition!!)
            }
            val managerDivision: RegionDivisionStructureDto? = if (entity.managerDivisionId == null) null
            else {
                RegionDivisionStructureDto(entity.managerDivisionId,
                    entity.managerDivisionLongName!!,
                    entity.managerDivisionLongName,
                    entity.managerDivisionRegionId!!)
            }
            UserPagedDto(entity.managerId,
                entity.managerName!!,
                entity.managerLastName!!,
                entity.managerMiddleName,
                entity.managerEmail!!,
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.managerCreatedDate!!), ZoneId.of("UTC"))
                    .plusSeconds(offset.toLong()),
                entity.managerStatus!!,
                entity.managerPosition!!,
                managerManager,
                managerDivision)
        }
        val currentDepartmentName = if (entity.isDepartment) entity.longName
        else departmentName
        return RegionDivisionStructureDto(entity.id, entity.shortName, entity.longName, entity.regionFk, department = currentDepartmentName, leadUser = manager)
    }
}