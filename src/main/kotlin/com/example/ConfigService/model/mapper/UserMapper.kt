package com.example.ConfigService.model.mapper

import com.example.ConfigService.model.*
import com.example.ConfigService.model.entity.UserEntity
import com.example.ConfigService.model.entity.UserPagedEntity
import com.example.ConfigService.model.enumeration.UserStatus
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Component
class UserMapper {

    fun toDto(entity: UserEntity) =
            UserDto(entity.id).apply {
                regionDto = RegionDto(entity.regionFk)
                name = entity.name
                lastName = entity.lastName
                middleName = entity.middleName
                position = entity.position
                manager = entity.managerUserFk?.let { UserDto(it) }
                regionDivisionDto = entity.regionDivFk?.let { RegionDivisionDto(it) }
            }

    fun toEntity(createUserDto: CreateUserDto, userId: Int, regionId: Int): UserEntity =
        UserEntity(
            id = userId,
            regionFk = regionId,
            name = createUserDto.name,
            lastName = createUserDto.lastName,
            middleName = createUserDto.middleName,
            status = UserStatus.DRAFT,
            email = createUserDto.email,
            position = createUserDto.position,
            managerUserFk = createUserDto.managerId,
            regionDivFk = createUserDto.divisionId)

    fun toAuthCreateUserDto(createUserDto: CreateUserDto, regionId: Int): AuthCreateUserDto =
        AuthCreateUserDto(createUserDto.login,
            regionId,
            createUserDto.name,
            createUserDto.lastName,
            createUserDto.middleName,
            UserStatus.DRAFT,
            createUserDto.email,
            createUserDto.position,
            createUserDto.reason,
            createUserDto.role
        )

    fun toPagedDto(entity: UserPagedEntity): UserPagedDto {
        val manager: UserShortDto? = if (entity.managerId == null) null
        else {
            UserShortDto(
                entity.managerId,
                entity.managerName!!,
                entity.managerLastName!!,
                entity.managerMiddleName,
                entity.managerPosition!!)
        }
        val division: RegionDivisionStructureDto? = if (entity.divisionId == null) null
        else {
            val divisionManager = getUserPagedDto(entity)
            RegionDivisionStructureDto(entity.divisionId,
                entity.divisionShortName!!,
                entity.divisionLongName!!,
                entity.divisionRegionId!!,
                leadUser = divisionManager)
        }

        val offset: Int = ZonedDateTime.now().offset.totalSeconds

        return UserPagedDto(entity.id,
            entity.name,
            entity.lastName,
            entity.middleName,
            entity.email,
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.createdDate), ZoneId.of("UTC"))
                .plusSeconds(offset.toLong()),
            entity.status,
            entity.position,
            manager,
            division)
    }

    private fun getUserPagedDto(entity: UserPagedEntity): UserPagedDto? {
        if (entity.divisionManagerId == null) return null
        else {
            val offset: Int = ZonedDateTime.now().offset.totalSeconds
            val createdDate =
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.divisionManagerCreatedDate!!), ZoneId.of("UTC"))
                    .plusSeconds(offset.toLong())
            return UserPagedDto(
                entity.divisionManagerId,
                entity.divisionManagerName!!,
                entity.divisionManagerLastName!!,
                entity.divisionManagerMiddleName,
                entity.divisionManagerEmail!!,
                createdDate,
                entity.divisionManagerStatus!!,
                entity.divisionManagerPosition!!
            )
        }
    }

    fun toUserResponse(userEntity: UserPagedEntity, authUserResponse: AuthUserResponse): UserResponse {
        val manager = if (userEntity.managerId == null) null
        else {
            val offset: Int = ZonedDateTime.now().offset.totalSeconds
            val createdDate =
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(userEntity.managerCreatedDate!!), ZoneId.of("UTC"))
                    .plusSeconds(offset.toLong())
            UserPagedDto(userEntity.managerId,
                userEntity.managerName!!,
                userEntity.managerLastName!!,
                userEntity.managerMiddleName,
                userEntity.managerEmail!!,
                createdDate,
                userEntity.status,
                userEntity.position)
        }
        val division = if(userEntity.divisionId == null) null
        else {
            RegionDivisionStructureDto(
                userEntity.divisionId,
                userEntity.divisionShortName!!,
                userEntity.divisionLongName!!,
                userEntity.divisionRegionId!!
            )
        }
        val role = RoleDto().apply {
            id = authUserResponse.role.id
            name = authUserResponse.role.name
            code = authUserResponse.role.code
        }
        return UserResponse(userEntity.id,
            authUserResponse.login,
            userEntity.name,
            userEntity.lastName,
            userEntity.middleName,
            userEntity.email,
            userEntity.position,
            userEntity.reason,
            role,
            authUserResponse.status,
            manager,
            division
        )
    }
}