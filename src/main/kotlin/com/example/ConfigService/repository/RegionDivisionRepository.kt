package com.example.ConfigService.repository

import com.example.ConfigService.model.entity.RegionDivisionEntity
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode
import java.util.*

@Repository
class RegionDivisionRepository(private val dsl: DSLContext) {
    fun findRegionDivisionById(regionDivisionId: Int, regionId: Int): RegionDivisionEntity {
        return dsl.selectFrom(REGION_DIVISIONS)
            .where(REGION_DIVISIONS.ID.eq(regionDivisionId).and(REGION_DIVISIONS.REGION_FK.eq(regionId)))
            .fetchOptional()
            .orElseThrow{
                throw CoreException(ErrorCode.ENTITY_NOT_FOUND,
                    "Не удалось найти подразделение по ID: $regionDivisionId")
            }
            .into(RegionDivisionEntity::class.java)
    }

    fun setManager(regDivId: Int, managerId: Int) {
        dsl.update(REGION_DIVISIONS)
            .set(REGION_DIVISIONS.LEAD_USER_FK, managerId)
            .where(REGION_DIVISIONS.ID.eq(regDivId))
            .execute()
    }

    fun findRegionDivisionsByRegionId(regionId: Int, isDepartment: Boolean? = null): List<RegionDivisionStructureEntity> {

        val managers = USERS.`as`("MANAGERS")
        val managerDivisions = REGION_DIVISIONS.`as`("MANAGER_DIVISIONS")
        var condition = REGION_DIVISIONS.REGION_FK.eq(regionId)
        isDepartment?.let { condition = condition.and(REGION_DIVISIONS.IS_DEPARTMENT.eq(it)) }

        return dsl.select(REGION_DIVISIONS.ID,
            REGION_DIVISIONS.SHORT_NAME,
            REGION_DIVISIONS.LONG_NAME,
            REGION_DIVISIONS.REGION_FK,
            REGION_DIVISIONS.IS_DEPARTMENT,
            REGION_DIVISIONS.LEAD_USER_FK.`as`("managerId"),
            USERS.NAME.`as`("managerName"),
            USERS.LAST_NAME.`as`("managerLastName"),
            USERS.MIDDLE_NAME.`as`("managerMiddleName"),
            USERS.EMAIL.`as`("managerEmail"),
            USERS.CREATED_DATE.`as`("managerCreatedDate"),
            USERS.STATUS.`as`("managerStatus"),
            USERS.POSITION.`as`("managerPosition"),
            USERS.MANAGER_USER_FK.`as`("managerManagerId"),
            managers.ID.`as`("managerManagerId"),
            managers.NAME.`as`("managerManagerName"),
            managers.LAST_NAME.`as`("managerManagerLastName"),
            managers.MIDDLE_NAME.`as`("managerManagerMiddleName"),
            managers.POSITION.`as`("managerManagerPosition"),
            managerDivisions.ID.`as`("managerDivisionId"),
            managerDivisions.REGION_FK.`as`("managerDivisionRegionId"),
            managerDivisions.SHORT_NAME.`as`("managerDivisionShortName"),
            managerDivisions.LONG_NAME.`as`("managerDivisionLongName"),
            REGION_DIVISIONS.PARENT_DIVISION_FK)
            .from(REGION_DIVISIONS)
            .leftJoin(USERS).on(REGION_DIVISIONS.LEAD_USER_FK.eq(USERS.ID).and(REGION_DIVISIONS.REGION_FK.eq(regionId)))
            .leftJoin(managers).on(USERS.MANAGER_USER_FK.eq(managers.ID))
            .leftJoin(managerDivisions).on(managerDivisions.ID.eq(USERS.REGION_DIV_FK))
            .where(condition)
            .fetchInto(RegionDivisionStructureEntity::class.java)
    }

    fun findRegionDivisionsByManagerId(managerId: Int): Optional<RegionDivisionEntity> {
        return dsl.selectFrom(REGION_DIVISIONS)
            .where(REGION_DIVISIONS.LEAD_USER_FK.eq(managerId))
            .fetchOptional { r -> r.into(RegionDivisionEntity::class.java) }
    }
}