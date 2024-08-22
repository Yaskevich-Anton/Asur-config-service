package ru.rieksp.coreservice.service

import com.example.ConfigService.model.*
import com.example.ConfigService.model.entity.UserEntity
import com.example.ConfigService.model.entity.UserPagedEntity
import com.example.ConfigService.model.enumeration.MailTemplateType
import com.example.ConfigService.model.enumeration.UserStatus
import com.example.ConfigService.model.mapper.UserMapper
import com.example.ConfigService.repository.UserRepository
import com.example.ConfigService.service.RegionDivisionService
import com.example.ConfigService.service.client.AuthRestClient
import com.example.ConfigService.service.client.NotificationRestClient
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.jooq.exception.MappingException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode
import java.util.stream.Collectors

@Service
class UserService(private val userRepository: UserRepository,
                  private val userMapper: UserMapper,
                  private val regionDivisionService: RegionDivisionService,
                  private val authRestClient: AuthRestClient,
                  private val notificationRestClient: NotificationRestClient,
                  private val objectMapper: ObjectMapper,) {

    private val log = LoggerFactory.getLogger(javaClass)


    @PostConstruct
    fun setUserService() {
        regionDivisionService.setUserService(this)
    }

    fun findUserById(userId: Int, regionId: Int): UserDto {
        val user: UserEntity = userRepository.findUserById(userId, regionId)
        return userMapper.toDto(user)
    }

    @Transactional
    fun createUser(createUserDto: CreateUserDto, regionId: Int): Int {
        regionDivisionService.findRegionDivisionsById(createUserDto.divisionId, regionId)
        val authCreateUserDto = userMapper.toAuthCreateUserDto(createUserDto, regionId)
        val userId = authRestClient.sendCreateUser(authCreateUserDto)
        val user:UserEntity = userMapper.toEntity(createUserDto, userId, regionId)
        userRepository.saveUser(user)
        return userId
    }

    @Transactional
    fun deleteUser(userId: Int, regionId: Int, roles: String) {
        val optionalManagedDivision = regionDivisionService.findRegionDivisionsByManagerId(userId)
        if (optionalManagedDivision.isPresent) {
            log.warn("Невозможно удалить пользователя(ID = $userId, " +
                    "так как он является руководителем подразделения(ID = ${optionalManagedDivision.get().id})")
            throw CoreException(ErrorCode.TRY_TO_DELETE_DIV_MANAGER, "")
        }
        userRepository.deleteManagerForEmployees(userId)
        userRepository.deleteUser(userId)
        authRestClient.sendDeleteUser(userId, regionId, roles)
    }

    fun getUserCountByFilters(
            regionId: Int,
            roleCode: String,
            text: String?,
            divisionId: Int?,
            managerId: Int?,
            status: UserStatus?,
            statusNotIn: List<UserStatus> = listOf(),
    ): Int {
        return userRepository.findUserCountByFilters(regionId, text, divisionId, managerId, status, statusNotIn)
    }

    fun findUsersByFilters(
        regionId: Int,
        divisionId: Int?,
        status: UserStatus?,
        managerId: Int?,
        text: String?,
        isRespUser: Boolean,
        isAuthorizeUser: Boolean,
        sortBy: String?,
        statusNotIn: List<UserStatus> = listOf(),
        pageN: Int,
        pageSize: Int
    ): List<UserPagedDto> {
        val offset: Int = (pageN - 1) * pageSize
        val limit: Int = pageSize
        val usersData = userRepository.findUserByFilters(regionId, divisionId, status, statusNotIn, managerId, text, isRespUser, isAuthorizeUser, sortBy, offset, limit)
        val users =  usersData.stream()
            .map { entity: UserPagedEntity -> userMapper.toPagedDto(entity) }
            .collect(Collectors.toList())
        regionDivisionService.getDepartmentsForUsers(regionId, users)
        return users
    }

    fun findManagers(regionId: Int): List<UserPagedDto> {
        val managersEntities:List<UserPagedEntity> = userRepository.findManagers(regionId)
        val result: MutableList<UserPagedDto> = mutableListOf()
        val userInResult: MutableSet<Int> = mutableSetOf()
        for (entity in managersEntities) {
            if (!userInResult.contains(entity.id)) {
                val dto = userMapper.toPagedDto(entity)
                result.add(dto)
                userInResult.add(entity.id)
            }
        }
        regionDivisionService.getDepartmentsForUsers(regionId, result)
        return result
    }

    @Transactional
    fun updateStatusByUserId(userId: Int, updateStatusDto: UpdateStatusDto) {
        userRepository.findUserById(userId, updateStatusDto.regionId)
        userRepository.updateStatusById(userId, updateStatusDto.regionId, updateStatusDto.status)
    }

    fun getUserById(userId: Int, currentUserId: Int, regionId: Int, roleCode: String): UserResponse {
        val userEntity = userRepository.findUserByIdAndRegionId(userId, regionId)
        val authUserResponse = authRestClient.sendGetUserById(userId, regionId, roleCode, currentUserId)
        val divisionsById = regionDivisionService.getDivisionsByIdStructure(regionId)
        val userResponse = userMapper.toUserResponse(userEntity, authUserResponse)
        userEntity.divisionId?.let { it:Int->
            userResponse.department = regionDivisionService.getDepartmentByDivisionId(it, divisionsById)
            userResponse.division?.let { div ->
                div.department = userResponse.department
            }
        }
        return userResponse
    }

    @Transactional
    fun updateUser(userId: Int, userUpdateDto: UserUpdateReqDto, regionId: Int, roleCodes: String, currentUserId: Int) {
        val userEntity = userRepository.findUserById(userId, regionId)
        if(userEntity.status != UserStatus.DRAFT) {
            throw CoreException(ErrorCode.USER_DRAFT_STATUS,
                "Редактировать можно только пользователей в статусе DRAFT(${UserStatus.DRAFT})")
        }
        val roles = authRestClient.sendGetRoles(roleCodes)
        val role = getRoleByCode(userUpdateDto.role, roles)
        val authUserUpdateDto = AuthUserUpdateDto(userUpdateDto.login,
            userUpdateDto.name,
            userUpdateDto.lastName,
            userUpdateDto.middleName,
            userUpdateDto.reason,
            userUpdateDto.email,
            userUpdateDto.position,
            userUpdateDto.managerId,
            role.id!!)
        authRestClient.sendUpdateUser(userId, regionId, roleCodes, currentUserId, authUserUpdateDto)
        userRepository.updateUser(userId, regionId, userUpdateDto)
    }

    fun getUserByEmail(email: String) = userRepository.findUserByEmail(email)

    @Transactional
    fun handlePswChangeApproveRequest(userIds: List<Int>, regionId: Int, isApprove: Boolean, reason: String?) {
        val foundUsers: List<UserEntity> = userRepository.findUsersByIdAndRegionId(userIds, regionId)
        if (isApprove) {
            userRepository.updateUsersStatusesAndReason(userIds, UserStatus.ACTIVE, reason)
            foundUsers.forEach { user -> onUserApproved(user, regionId, MailTemplateType.CHANGE_PWD_SUCCESS) }
        } else {
             userRepository.updateUsersStatusesAndReason(userIds, UserStatus.BLOCKED, reason)
            foundUsers.forEach { user -> onChangePswReqReject(user, regionId, reason)}
        }
    }

    @Transactional
    fun approveUsers(approveDto: ApproveDto, regionId: Int) {
        val foundUsers: List<UserEntity> = userRepository.findUsersByIdAndRegionId(approveDto.userIds, regionId)
        checkUsersCount(approveDto.userIds, foundUsers, regionId)
        handleApproveUsers(foundUsers, approveDto.userIds, regionId, approveDto.result, approveDto.reason)
    }

    @Transactional
    fun approveUser(userId: Int, userApprove: UserApprove, regionId: Int) {
        val user = userRepository.findUserById(userId, regionId)
        handleApproveUsers(listOf(user), listOf(userId), regionId, userApprove.result, userApprove.reason)
    }

    @Transactional
    fun toApproveUsers(usersToRequestStatus: UsersToRequestStatus, regionId: Int) {
        val foundUsers: List<UserEntity> = userRepository.findUsersByIdAndRegionId(usersToRequestStatus.userIds, regionId)
        checkUsersCount(usersToRequestStatus.userIds, foundUsers, regionId)
        authRestClient.sendUpdateStatuses(usersToRequestStatus.userIds, regionId, UserStatus.REQUEST)
        userRepository.updateUsersStatusesAndReason(usersToRequestStatus.userIds, UserStatus.REQUEST)
    }

    private fun handleApproveUsers(users: List<UserEntity>, userIds: List<Int>, regionId: Int, result: Boolean, reason: String?) {
        if (result) {
            userRepository.updateUsersStatusesAndReason(userIds, UserStatus.ACTIVE)
            users.forEach { user -> onUserApproved(user, regionId, MailTemplateType.USER_ACTIVATED) }
        } else {
            val regionAdmins: List<AuthUserResponse> = authRestClient.sendGetRegionAdmins(regionId)
            userRepository.updateUsersStatusesAndReason(userIds, UserStatus.BLOCKED, reason)
            if (regionAdmins.isEmpty()) {
                return
            }
            val regionAdminsEmails = regionAdmins.stream()
                .map { admin -> admin.email }
                .collect(Collectors.toList())
            users.forEach { user -> onUserBlocked(user, regionId, regionAdminsEmails, reason) }
        }
    }

    private fun onUserApproved(user: UserEntity, regionId: Int, templateType: MailTemplateType) {
        val password = authRestClient.sendUpdateStatus(user.id, regionId, UpdateStatusReq(UserStatus.ACTIVE, true, regionId))
        val params = HashMap<String, String>()
        params["login"] = user.email
        params["password"] = password
        params["userName"] = user.name
        params["middleName"] = user.middleName ?: ""
        sendNotification(listOf(user.email), templateType, params)
    }

    private fun onUserBlocked(user: UserEntity, regionId: Int, emails: List<String>, reason: String?) {
        authRestClient.sendUpdateStatus(user.id, regionId, UpdateStatusReq(UserStatus.BLOCKED, true, regionId))
        val params = HashMap<String, String>()
        params["login"] = user.email
        params["reason"] = reason ?: ""
        sendNotification(emails, MailTemplateType.USER_NOT_APPROVED, params)
    }

    private fun onChangePswReqReject(user: UserEntity, regionId: Int, reason: String?) {
        authRestClient.sendUpdateStatus(user.id, regionId, UpdateStatusReq(UserStatus.BLOCKED, false, regionId))
        val params = HashMap<String, String>()
        params["login"] = user.email
        params["reason"] = reason ?: ""
        params["recipientUserName"] = user.name
        params["recipientMiddleName"] = user.middleName ?: ""
        params["lastName"] = user.lastName
        params["userName"] = user.name
        params["middleName"] = user.middleName ?: ""
        sendNotification(listOf(user.email), MailTemplateType.CHANGE_PWD_BLOCKED, params)

        user.managerUserFk?.let { managerId ->
            val userManager = userRepository.findUserByIdAndRegionId(managerId, user.regionFk)
            val managerParams = HashMap<String, String>()
            managerParams["login"] = user.email
            managerParams["reason"] = reason ?: ""
            managerParams["recipientUserName"] = userManager.name
            managerParams["recipientMiddleName"] = userManager.middleName ?: ""
            managerParams["lastName"] = user.lastName
            managerParams["userName"] = user.name
            managerParams["middleName"] = user.middleName ?: ""
            sendNotification(listOf(userManager.email), MailTemplateType.CHANGE_PWD_BLOCKED, managerParams)
        }
    }

    private fun getParamsToString(params: Map<String, String>): String =
        try {
            objectMapper.writeValueAsString(params)
        } catch (e: JsonProcessingException) {
            log.error("Ошибка при предствалении списка параметров в строку. Параметры: $params")
            throw MappingException("Ошибка при предствалении списка параметров в строку. Параметры: $params")
        }

    private fun sendNotification(emails: List<String>, mailType: MailTemplateType, params: HashMap<String, String>) {
        val paramsToString: String = getParamsToString(params)
        val emailRequestDto = EmailRequestDto(emails, mailType, paramsToString)
        notificationRestClient.sendNotification(emailRequestDto)
    }

    private fun getRoleByCode(roleCode: String, roles: List<RoleDto>): RoleDto {
        for (role in roles) {
            if (roleCode == role.code) {
                return role
            }
        }
        log.error("Не удалось найти роль по коду: $roleCode")
        throw CoreException(ErrorCode.ENTITY_NOT_FOUND, "")
    }

    private fun checkUsersCount(userIds: List<Int>, foundUsers: List<UserEntity>, regionId: Int) {
        if (userIds.size > foundUsers.size) {
            val foundUsersIds: MutableSet<Int> = foundUsers.stream()
                .map { user -> user.id }
                .collect(Collectors.toSet())
            val sourceUsersIds = HashSet<Int>(userIds)
            sourceUsersIds.removeAll(foundUsersIds)
            val message = "Нет доступа или не удалось найти пользователей с ID: $sourceUsersIds, ID регона: $regionId"
            log.warn(message)
            throw CoreException(ErrorCode.ENTITY_NOT_FOUND, message)
        }
    }
}