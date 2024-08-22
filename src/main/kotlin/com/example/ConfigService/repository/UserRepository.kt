package com.example.ConfigService.repository

import com.example.ConfigService.model.entity.UserEntity
import com.example.ConfigService.model.entity.UserPagedEntity
import com.example.ConfigService.model.enumeration.UserStatus
import org.jooq.DSLContext
import org.jooq.TableField
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode
import java.lang.RuntimeException

@Repository
class UserRepository(private val dsl: DSLContext) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun findUserById(userId: Int, regionId: Int): UserEntity {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.eq(userId).and(USERS.REGION_FK.eq(regionId)))
            .fetchOptional()
            .orElseThrow {
                throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "Не удалось найти пользователя по ID: $userId " +
                        "и ID региона: $regionId")
            }
            .into(UserEntity::class.java)
    }

    fun saveUser(user: UserEntity) {
        try {
            dsl.insertInto(USERS)
                .set(USERS.ID, user.id)
                .set(USERS.REGION_FK, user.regionFk)
                .set(USERS.POSITION, user.position)
                .set(USERS.MANAGER_USER_FK, user.managerUserFk)
                .set(USERS.REGION_DIV_FK, user.regionDivFk)
                .set(USERS.STATUS, user.status.name)
                .set(USERS.NAME, user.name)
                .set(USERS.LAST_NAME, user.lastName)
                .set(USERS.MIDDLE_NAME, user.middleName)
                .set(USERS.EMAIL, user.email)
                .execute()
        } catch (ex: DuplicateKeyException) {
            throw CoreException(
                ErrorCode.ENTITY_ALREADY_EXIST,
                "Пользователь уже существует. ID = ${user.id}"
            )
        }
    }

    fun deleteManagerForEmployees(userId: Int) {
        dsl.update(USERS)
            .set(USERS.MANAGER_USER_FK, null as Int?)
            .where(USERS.MANAGER_USER_FK.eq(userId))
            .execute()
    }

    fun deleteUser(userId: Int): Int =
        try {
            dsl.delete(USERS)
                .where(USERS.ID.eq(userId))
                .execute()
        } catch (e: DataIntegrityViolationException) {
            throw RuntimeException("удаление невозможно, пользователь все еще " +
                    "является руководителем для существующих пользователей")
        }

    fun findUserCountByFilters(regionId: Int, text: String?, divisionId: Int?, managerId: Int?, status: UserStatus?, statusNotIn: List<UserStatus>): Int {
        var condition = USERS.REGION_FK.eq(regionId)
        divisionId?.let { condition = condition.and(USERS.REGION_DIV_FK.eq(it)) }
        managerId?.let { condition = condition.and(USERS.MANAGER_USER_FK.eq(it)) }
        text?.let {
            condition = condition.and(USERS.LAST_NAME.likeIgnoreCase("%$it%")
                .or(USERS.NAME.likeIgnoreCase("%$it%"))
                .or(USERS.MIDDLE_NAME.likeIgnoreCase("%$it%"))
                .or(USERS.POSITION.likeIgnoreCase("%$it%")))
        }
        status?.let { condition = condition.and(USERS.STATUS.eq(it.name)) }
        if (statusNotIn.isNotEmpty()) {
            condition = condition.and(USERS.STATUS.notIn(statusNotIn))
        }
        return dsl.select(USERS.ID).from(USERS)
            .where(condition)
            .count()
    }

    fun findUserByFilters(
            regionId: Int,
            divisionId: Int?,
            status: UserStatus?,
            statusNotIn: List<UserStatus>,
            managerId: Int?,
            text: String?,
            isRespUser: Boolean,
            isAuthorizeUser: Boolean,
            sortBy: String?,
            offset: Int,
            limit: Int
    ): List<UserPagedEntity> {
        val passports = FACT_PASSPORTS.`as`("PASSPORTS")
        var condition = USERS.REGION_FK.eq(regionId)
        divisionId?.let { condition = condition.and(USERS.REGION_DIV_FK.eq(it)) }
        managerId?.let { condition = condition.and(USERS.MANAGER_USER_FK.eq(it)) }
        status?.let { condition = condition.and(USERS.STATUS.eq(it.name)) }
        text?.let {
            condition = condition.and(USERS.LAST_NAME.likeIgnoreCase("%$it%")
                .or(USERS.NAME.likeIgnoreCase("%$it%"))
                .or(USERS.MIDDLE_NAME.likeIgnoreCase("%$it%"))
                .or(USERS.POSITION.likeIgnoreCase("%$it%")))
        }
        if (statusNotIn.isNotEmpty()) {
            condition = condition.and(USERS.STATUS.notIn(statusNotIn))
        }

        val managers = USERS.`as`("MANAGERS")
        val divisionManager = USERS.`as`("DIVISION_MANAGERS")

        val orderFields = mutableListOf(USERS.LAST_NAME.asc(), USERS.STATUS.asc(), USERS.CREATED_DATE.asc())
        orderFields.clear()
        if (sortBy.isNullOrBlank()) {
            orderFields.add(USERS.LAST_NAME.asc())
            orderFields.add(USERS.NAME.asc())
            orderFields.add(USERS.MIDDLE_NAME.asc())
        } else {
            val fieldNames = sortBy.split(",")
            fieldNames.forEach { part ->
                val order = part.last()
                val name = part.substring(0, part.length - 1)
                when(name) {
                    "name" -> orderFields.add(getSortedField(USERS.NAME, order))
                    "lastName" -> orderFields.add(getSortedField(USERS.LAST_NAME, order))
                    "middleName" -> orderFields.add(getSortedField(USERS.MIDDLE_NAME, order))
                    "status" -> orderFields.add(getSortedField(USERS.STATUS, order))
                    "position" -> orderFields.add(getSortedField(USERS.POSITION, order))
                    "email" -> orderFields.add(getSortedField(USERS.EMAIL, order))
                    "createdDate" -> orderFields.add(getSortedField(USERS.CREATED_DATE, order))
                    else -> log.warn("Неопознанное поле для сортировки: $name")
                }
            }
        }

        val tableResult = dsl.select(USERS.ID,
            USERS.NAME,
            USERS.LAST_NAME,
            USERS.MIDDLE_NAME,
            USERS.STATUS,
            USERS.POSITION,
            USERS.EMAIL,
            USERS.CREATED_DATE,
            managers.ID.`as`("managerId"),
            managers.NAME.`as`("managerName"),
            managers.LAST_NAME.`as`("managerLastName"),
            managers.MIDDLE_NAME.`as`("managerMiddleName"),
            managers.POSITION.`as`("managerPosition"),
            REGION_DIVISIONS.ID.`as`("divisionId"),
            REGION_DIVISIONS.REGION_FK.`as`("divisionRegionId"),
            REGION_DIVISIONS.SHORT_NAME.`as`("divisionShortName"),
            REGION_DIVISIONS.LONG_NAME.`as`("divisionLongName"),
            REGION_DIVISIONS.IS_DEPARTMENT.`as`("isDepartment"),
            divisionManager.ID.`as`("divisionManagerId"),
            divisionManager.NAME.`as`("divisionManagerName"),
            divisionManager.LAST_NAME.`as`("divisionManagerLastName"),
            divisionManager.MIDDLE_NAME.`as`("divisionManagerMiddleName"),
            divisionManager.EMAIL.`as`("divisionManagerEmail"),
            divisionManager.CREATED_DATE.`as`("divisionManagerCreatedDate"),
            divisionManager.STATUS.`as`("divisionManagerStatus"),
            divisionManager.POSITION.`as`("divisionManagerPosition"))
            .from(USERS).leftJoin(managers).on(USERS.MANAGER_USER_FK.eq(managers.ID))
            .leftJoin(REGION_DIVISIONS).on(USERS.REGION_DIV_FK.eq(REGION_DIVISIONS.ID))
            .leftJoin(divisionManager).on(REGION_DIVISIONS.LEAD_USER_FK.eq(divisionManager.ID))

        if (isRespUser) {
            tableResult.join(FACT_PASSPORTS).on(FACT_PASSPORTS.RESP_USER_FK.eq(USERS.ID))
        }
        if (isAuthorizeUser) {
            tableResult.join(passports).on(passports.AUTHORIZE_USER_FK.eq(USERS.ID))
        }

        return tableResult
            .where(condition)
            .orderBy(orderFields)
            .limit(limit)
            .offset(offset)
            .fetch { r -> r.into(UserPagedEntity::class.java) }
    }

    fun findManagers(regionId: Int): List<UserPagedEntity> {

        val managersIds = dsl.selectDistinct(REGION_DIVISIONS.LEAD_USER_FK.`as`("ID"))
            .from(REGION_DIVISIONS)
            .join(USERS).on(REGION_DIVISIONS.REGION_FK.eq(regionId)).and(USERS.ID.eq(REGION_DIVISIONS.LEAD_USER_FK))
            .and(USERS.STATUS.eq(UserStatus.ACTIVE.name))

        return dsl.select(USERS.ID,
            USERS.NAME,
            USERS.LAST_NAME,
            USERS.MIDDLE_NAME,
            USERS.STATUS,
            USERS.POSITION,
            USERS.EMAIL,
            USERS.CREATED_DATE,
            REGION_DIVISIONS.ID.`as`("divisionId"),
            REGION_DIVISIONS.REGION_FK.`as`("divisionRegionId"),
            REGION_DIVISIONS.SHORT_NAME.`as`("divisionShortName"),
            REGION_DIVISIONS.LONG_NAME.`as`("divisionLongName"))
            .from(managersIds).join(USERS).on(USERS.ID.`in`(managersIds.field("ID")))
            .join(REGION_DIVISIONS).on(REGION_DIVISIONS.LEAD_USER_FK.eq(USERS.ID))
            .fetch { r -> r.into(UserPagedEntity::class.java) }
    }

    fun updateStatusById(userId: Int, regionId: Int, status: UserStatus) {
        dsl.update(USERS)
            .set(USERS.STATUS, status.name)
            .where(USERS.ID.eq(userId).and(USERS.REGION_FK.eq(regionId)))
            .execute()
    }

    fun findUsersByIdAndRegionId(userIds: List<Int>, regionId: Int): List<UserEntity> {
        return dsl.selectFrom(USERS)
            .where(USERS.ID.`in`(userIds).and(USERS.REGION_FK.eq(regionId)))
            .fetch {r -> r.into(UserEntity::class.java)}
    }

    fun updateUsersStatusesAndReason(userIds: List<Int>, status: UserStatus, reason: String? = null) {
        dsl.update(USERS)
            .set(USERS.STATUS, status.name)
            .set(USERS.REASON, reason)
            .where(USERS.ID.`in`(userIds))
            .execute()
    }

    fun findUserByIdAndRegionId(userId: Int, regionId: Int): UserPagedEntity {
        val managers = USERS.`as`("MANAGERS")
        return dsl.select(USERS.ID,
            USERS.NAME,
            USERS.LAST_NAME,
            USERS.MIDDLE_NAME,
            USERS.STATUS,
            USERS.POSITION,
            USERS.EMAIL,
            USERS.REASON,
            USERS.CREATED_DATE,
            managers.ID.`as`("managerId"),
            managers.NAME.`as`("managerName"),
            managers.LAST_NAME.`as`("managerLastName"),
            managers.MIDDLE_NAME.`as`("managerMiddleName"),
            managers.STATUS.`as`("managerStatus"),
            managers.EMAIL.`as`("managerEmail"),
            managers.CREATED_DATE.`as`("managerCreatedDate"),
            managers.POSITION.`as`("managerPosition"),
            REGION_DIVISIONS.ID.`as`("divisionId"),
            REGION_DIVISIONS.REGION_FK.`as`("divisionRegionId"),
            REGION_DIVISIONS.SHORT_NAME.`as`("divisionShortName"),
            REGION_DIVISIONS.LONG_NAME.`as`("divisionLongName"))
            .from(USERS).leftJoin(managers).on(USERS.MANAGER_USER_FK.eq(managers.ID))
            .leftJoin(REGION_DIVISIONS).on(USERS.REGION_DIV_FK.eq(REGION_DIVISIONS.ID))
            .where(USERS.ID.eq(userId).and(USERS.REGION_FK.eq(regionId)))
            .fetchOptional()
            .orElseThrow {
                throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "Не удалось найти пользователя по ID: $userId " +
                        "и ID региона: $regionId")
            }
            .into(UserPagedEntity::class.java)
    }

    fun updateUser(userId: Int, regionId: Int, userUpdateDto: UserUpdateReqDto) {
        dsl.update(USERS)
            .set(USERS.REGION_FK, regionId)
            .set(USERS.NAME, userUpdateDto.name)
            .set(USERS.LAST_NAME, userUpdateDto.lastName)
            .set(USERS.MIDDLE_NAME, userUpdateDto.middleName)
            .set(USERS.EMAIL, userUpdateDto.email)
            .set(USERS.POSITION, userUpdateDto.position)
            .set(USERS.MANAGER_USER_FK, userUpdateDto.managerId)
            .set(USERS.REGION_DIV_FK, userUpdateDto.divisionId)
            .where(USERS.ID.eq(userId))
            .execute()
    }

    fun findUserByEmail(email: String): UserEntity {
        return dsl.selectFrom(USERS)
            .where(USERS.EMAIL.eq(email))
            .fetchOptional()
            .orElseThrow {
                throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "Не удалось найти пользователя по email: $email.")
            }
            .into(UserEntity::class.java)
    }

    fun findUsersByStatus(regionId: Int, status: UserStatus, limit: Int, offset: Int): List<UserEntity> {
        return dsl.selectFrom(USERS)
            .where(USERS.REGION_FK.eq(regionId).and(USERS.STATUS.eq(status.name)))
            .orderBy(USERS.LAST_NAME, USERS.NAME, USERS.MIDDLE_NAME)
            .limit(limit)
            .offset(offset)
            .fetchInto(UserEntity::class.java)
    }

    private fun <T> getSortedField(field: TableField<UsersRecord,T>, order: Char) =
        if (order == '-') {
            field.desc()
        } else {
            field.asc()
        }
}