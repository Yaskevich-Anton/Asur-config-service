package com.example.ConfigService.service

import com.example.ConfigService.model.RegionDivisionStructureDto
import com.example.ConfigService.model.UserPagedDto
import com.example.ConfigService.model.entity.RegionDivisionEntity
import com.example.ConfigService.model.entity.RegionDivisionStructureEntity
import com.example.ConfigService.model.mapper.RegionDivisionsMapper
import com.example.ConfigService.repository.RegionDivisionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.rieksp.coreservice.service.UserService
import java.util.*
import kotlin.collections.HashMap

@Service
class RegionDivisionService(private val regionDivisionRepository: RegionDivisionRepository,
                            private val regionDivisionsMapper: RegionDivisionsMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    private lateinit var userService: UserService

    fun setUserService(userService: UserService) {
        this.userService = userService
    }

    fun setManager(regDivId: Int, managerId: Int, regionId: Int) {
        regionDivisionRepository.findRegionDivisionById(regDivId, regionId)
        userService.findUserById(managerId, regionId)
        regionDivisionRepository.setManager(regDivId, managerId)
    }

    fun getDivisionStructure(regionId: Int, isDepartment: Boolean?): List<RegionDivisionStructureDto> {
        val divisions: List<RegionDivisionStructureEntity> =
            regionDivisionRepository.findRegionDivisionsByRegionId(regionId, isDepartment)

        val divisionsByParentId: HashMap<Int?, MutableList<RegionDivisionStructureEntity>> = HashMap()
        for (division in divisions) {
            if (divisionsByParentId[division.parentDivisionFk] == null) {
                divisionsByParentId[division.parentDivisionFk] = mutableListOf(division)
            } else {
                divisionsByParentId[division.parentDivisionFk]?.add(division)
            }
        }
        val result: MutableList<RegionDivisionStructureDto> = mutableListOf()
        if (isDepartment == null) {
            val roots = divisionsByParentId[null] ?: return emptyList()
            for (root in roots) {
                val structureDto = regionDivisionsMapper.toTreeStructureDto(root, divisionsByParentId, root.longName)
                result.add(structureDto)
            }
            return result
        }
        val divisionsById = if (isDepartment) hashMapOf()
        else getDivisionsByIdStructure(regionId)
        divisions.forEach { div ->
            val departmentName = if (isDepartment) div.longName
            else {
                getDepartmentByDivisionId(div.id, divisionsById)
            }
            val structureDto = regionDivisionsMapper.toStructureDto(div, departmentName)
            result.add(structureDto)
        }

        return result
    }

    fun findRegionDivisionsByManagerId(managerId: Int): Optional<RegionDivisionEntity> {
        return regionDivisionRepository.findRegionDivisionsByManagerId(managerId)
    }

    fun findRegionDivisionsById(id: Int, regionId: Int) = regionDivisionRepository.findRegionDivisionById(id, regionId)

    fun getDepartmentsForUsers(regionId: Int, users: List<UserPagedDto>) {
        val divisionsById = getDivisionsByIdStructure(regionId)
        for (user in users) {
            if (user.division == null || divisionsById[user.division.id] == null) {
                continue
            }
            user.division.department = getDepartmentByDivisionId(user.division.id, divisionsById)
        }
    }

    fun getDivisionsByIdStructure(regionId: Int): HashMap<Int, RegionDivisionStructureEntity> {
        val divisions: List<RegionDivisionStructureEntity> =
            regionDivisionRepository.findRegionDivisionsByRegionId(regionId)

        val divisionsById = HashMap<Int, RegionDivisionStructureEntity>()
        divisions.forEach { division -> divisionsById[division.id] = division }
        return divisionsById
    }

    fun getDepartmentByDivisionId(divisionId: Int, divisionsById: HashMap<Int, RegionDivisionStructureEntity>): String? {
        if (divisionsById[divisionId] == null) {
            return null
        }
        val division = divisionsById[divisionId]!!
        var departmentName: String? = null
        if (division.isDepartment) departmentName = division.longName
        else {
            var currentDivision = division
            while (departmentName == null) {
                if (currentDivision.parentDivisionFk == null) {
                    departmentName = currentDivision.longName
                } else {
                    currentDivision = divisionsById[currentDivision.parentDivisionFk]!!
                    if (currentDivision.isDepartment) {
                        departmentName = currentDivision.longName
                    }
                }
            }
        }
        return departmentName
    }
}