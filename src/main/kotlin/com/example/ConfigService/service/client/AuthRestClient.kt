package com.example.ConfigService.service.client

import com.example.ConfigService.controller.HeadersConstants
import com.example.ConfigService.model.*
import com.example.ConfigService.model.enumeration.UserStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode

@Service
class AuthRestClient(
    private val restTemplate: RestTemplate,
    @Value("\${application.auth.service.url}")
    private val authUrl: String
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun sendCreateUser(authCreateUserDto: AuthCreateUserDto): Int {
        log.info("Отправка запроса на сохранение пользователя. {}", authCreateUserDto)
        val url = "$authUrl/internal/v1/users"
        val responseEntity: ResponseEntity<Int> = sendRequest(
            url = url, r = authCreateUserDto, map = mapOf()
        ) { he -> restTemplate.postForEntity(url, he, Int::class.java) }
        val userId = responseEntity.body!!
        log.info("Ответ на запрос на создание пользователя на сервис аутентификации. Получен ID пользователя: $userId")

        return userId
    }

    fun sendDeleteUser(userId: Int, regionId: Int, roles: String) {
        log.info("Отправка запроса на удаление пользователя. ID пользователя: $userId, ID региона текущего " +
                "пользователя: $regionId, роли текущего пользователя: $roles")
        val params = LinkedMultiValueMap<String, String>().also {
            it.add("regionId", regionId.toString())
            it.add("roles", roles)
        }
        val baseUrl = "$authUrl/internal/v1/users/$userId"
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParams(params)
            .build()
            .toUriString()
        val headers = HttpHeaders()
        val httpEntity = HttpEntity<Unit>(headers)
        val responseEntity: ResponseEntity<String> = try {
            restTemplate.exchange(url, HttpMethod.DELETE, httpEntity, String::class.java)
        } catch (e: RestClientException) {
            log.error("Ошибка при удалении пользователя на сервисе аутентификации, url = $url, ${e.message}")
            if (e.message?.contains(ErrorCode.TRY_TO_DELETE_DATA_MANAGER.name) == true) {
                throw CoreException(ErrorCode.TRY_TO_DELETE_DATA_MANAGER, "")
            } else {
                throw RestClientException("${e.message}")
            }
        }
        checkResult(responseEntity)
        log.info("Ответ на запрос на удаление пользователя от сервиса аутентификации. " +
                "Пользователь успешно удален ID: $userId")
    }

    fun sendGetUserById(userId: Int, regionId: Int, roles: String, currentUserId: Int): AuthUserResponse {
        log.info("Отправка запроса на получение данных пользователя по ID. ID пользователя: $userId, ID региона текущего " +
                "пользователя: $regionId, роли текущего пользователя: $roles, ID текущего пользователя: $currentUserId")
        val url = "$authUrl/$roles/v1/users/$userId"
        val responseEntity: ResponseEntity<AuthUserResponse> = sendRequest(
            url = url, r = null, map = mapOf(
                Pair(HeadersConstants.REGION_ID_HEADER, regionId),
                Pair(HeadersConstants.ROLES_HEADER, roles),
                Pair(HeadersConstants.USER_ID_HEADER, currentUserId)
            )
        ) { he -> restTemplate.exchange(url, HttpMethod.GET, he, AuthUserResponse::class.java) }
        log.info("Ответ на запрос на получение данных пользователя по ID от сервиса аутентификации. " +
                    "Пользователь успешно получен: ${responseEntity.body}")
        return responseEntity.body!!
    }

    fun sendUpdateUser(userId: Int, regionId: Int, roles: String, currentUserId: Int, updateDto: AuthUserUpdateDto) {
        log.info("Отправка запроса на редактирование пользователя. ID пользователя: $userId, ID региона текущего " +
                    "пользователя: $regionId, роли текущего пользователя: $roles, ID текущего пользователя: $currentUserId")
        val url = "$authUrl/$roles/v1/users/$userId"
        val responseEntity: ResponseEntity<String> = sendRequest(
            url, updateDto, mapOf(
                Pair(HeadersConstants.REGION_ID_HEADER, regionId),
                Pair(HeadersConstants.ROLES_HEADER, roles),
                Pair(HeadersConstants.USER_ID_HEADER, currentUserId)
            )
        ) { he -> restTemplate.exchange(url, HttpMethod.PUT, he, String::class.java) }
        log.info("Ответ на редактирование данных пользователя от сервиса аутентификации. " +
                    "Пользователь успешно получен: ${responseEntity.body}")
    }

    fun sendGetUserByIds(userIds: MutableList<Int>): List<AuthUserResponse> {
        log.info("Запрос на поиск пользователей по списку ID $userIds")
        val params: MultiValueMap<String, String> = createParams(userIds)
        val baseUrl = "$authUrl/internal/v1/users"
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParams(params)
            .build()
            .toUriString()
        val responseEntity: ResponseEntity<List<AuthUserResponse>> = sendRequest(
            url = url, r = null, map = mapOf()
        ) { he -> restTemplate.exchange(url, HttpMethod.GET, he, typeReference<List<AuthUserResponse>>()) }

        log.info("Получено пользователей: ${responseEntity.body!!.size}")
        return responseEntity.body!!
    }

    fun sendUpdateStatus(userId: Int, regionId: Int, updateStatusReq: UpdateStatusReq): String {
        log.info("Отправка запроса на обновление статуса пользователя. ID пользователя: $userId, ID региона текущего " +
                "пользователя: $regionId")
        val url = "$authUrl/internal/v1/users/$userId/status"
        val responseEntity: ResponseEntity<String> = sendRequest(
            url, updateStatusReq, mapOf()
        ) { he -> restTemplate.exchange(url, HttpMethod.PUT, he, String::class.java) }
        log.info("Полученная от сервиса аутентификации длина пароля: ${responseEntity.body!!.length}")

        return responseEntity.body!!
    }

    fun sendGetRegionAdmins(regionId: Int): List<AuthUserResponse> {
        log.info("Отправка запроса на получение списка администраторов региона, ID региона $regionId")
        val params = LinkedMultiValueMap<String, String>()
        params.add("regionId", regionId.toString())
        val baseUrl = "$authUrl/internal/v1/admin-users"
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParams(params)
            .build()
            .toUriString()
        val responseEntity: ResponseEntity<List<AuthUserResponse>> = sendRequest(
            url = url, r = null, map = mapOf()
        ) { he -> restTemplate.exchange(url, HttpMethod.GET, he, typeReference<List<AuthUserResponse>>()) }
        log.info("Получено пользователей-администраторов: ${responseEntity.body!!.size}")
        return responseEntity.body!!
    }

    fun sendGetRoles(role: String): List<RoleDto> {
        log.info("Отправка запроса на получение списка ролей.")
        val url = "$authUrl/$role/v1/roles"
        val responseEntity: ResponseEntity<List<RoleDto>> = sendRequest(
            url = url, r = null, map = mapOf()
        ) { he -> restTemplate.exchange(url, HttpMethod.GET, he, typeReference<List<RoleDto>>()) }
        log.info("Получено ролей: ${responseEntity.body!!.size}")
        return responseEntity.body!!
    }

    fun sendUpdateStatuses(userIds: List<Int>, regionId: Int, status: UserStatus): String {
        log.info("Отправка запроса на массовое обновление статуса пользователя. ID пользователей: $userIds, ID региона текущего " +
                "пользователя: $regionId, статус: $status")
        val url = "$authUrl/internal/v1/users/status"
        val dto = InternalBatchUpdateStatus(userIds, status, regionId)
        val responseEntity: ResponseEntity<String> = sendRequest(
            url, dto, mapOf()
        ) { he -> restTemplate.exchange(url, HttpMethod.PUT, he, String::class.java) }
        log.info("Полученная от сервиса аутентификации длина пароля: ${responseEntity.body!!.length}")

        return responseEntity.body!!
    }

    private fun <T, R> sendRequest(url: String, r: R? = null, map: Map<String, Any>, exchange: (h: HttpEntity<R>) -> ResponseEntity<T>): ResponseEntity<T> {
        val headers = HttpHeaders()
        val httpEntity = r?.let { HttpEntity<R>(it, headers) } ?: HttpEntity<R>(headers)
        map.forEach { (t, u) -> headers.set(t, u.toString()) }
        val responseEntity = try {
            exchange(httpEntity)
        } catch (e: RestClientException) {
            log.error("Ошибка при отправке запроса url = $url, ${e.message}")
            throw RestClientException("${e.message}")
        }
        checkResult(responseEntity)
        return responseEntity
    }

    private fun <T> checkResult(responseEntity: ResponseEntity<T>?) {
        if (responseEntity == null || responseEntity.statusCode != HttpStatus.OK || responseEntity.body == null) {
            val message = "Ошибка при получении ответа на запрос на сервисе аутентификации"
            log.error(message)
            throw CoreException(ErrorCode.INVALID_AUTH_RESPONSE, message)
        }
    }

    private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

    private fun createParams(userIds: MutableList<Int>): MultiValueMap<String, String> {
        val userIdsToString = userIds.toString()
        val ids = userIdsToString.substring(1, userIdsToString.length - 1)
        val params = LinkedMultiValueMap<String, String>()
        return params.also { it.add("ids", ids) }
    }
}