package com.example.ConfigService.service.client

import com.example.ConfigService.controller.HeadersConstants
import com.example.ConfigService.model.DimensionRequestBody
import com.example.ConfigService.model.DimensionValue
import com.example.ConfigService.model.FactDataReq
import com.example.ConfigService.model.FactValue
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
import java.time.LocalDate
import java.time.ZonedDateTime

@Service
class StoreRestClient(private val restTemplate: RestTemplate,
                      @Value("\${application.store.service.url}")
                      private val storeUrl: String) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun getWorkDayForPeriod(roleCode: String, startDay: ZonedDateTime, workDays: Int): LocalDate {
        log.info("Отправка запроса на получение рабочего дня для периода на сервис store. Код роли пользователя: $roleCode," +
                "начальная дата: $startDay, порядковый номер рабочего дня, начиная с первого рабочего дня, включая startDay: $workDays")
        val baseUrl = "$storeUrl/$roleCode/v1/calendar"
        val params: MultiValueMap<String, String> = createGetWorkDayParams(startDay, workDays)
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParams(params)
            .build()
            .toUriString()
        val responseEntity: ResponseEntity<LocalDate> = sendRequest(
            url = url, r = null, map = mapOf()
        ) { he -> restTemplate.exchange(url, HttpMethod.GET, he, LocalDate::class.java) }
        log.info("Ответ на запрос на получение рабочего дня для периода от сервиса store. " +
                "Дата успешно получена: ${responseEntity.body}")
        return responseEntity.body!!
    }

    fun getDifDimensionsValue(roleCode: String,
                                  regionId: Int,
                                  dimId: Int,
                                  parentId: Int?,
                                  filterText: String?,
                                  actualDate: ZonedDateTime?): List<DimensionValue> {
        log.info("Отправка запроса на получение различных значений измерений на сервис store. Код роли пользователя: $roleCode," +
                "ID региона: $regionId, ID измерения: $dimId, ID родителя измерения: $parentId, " +
                "фильтр для поиска по тексту: $filterText, время актуальности значений измерений: $actualDate")
        val baseUrl = "$storeUrl/$roleCode/v1/dimensions/$dimId/values"
        val params: MultiValueMap<String, String> = createGetDifDimensionsValueParams(parentId, filterText)
        val url = UriComponentsBuilder.fromHttpUrl(baseUrl)
            .queryParams(params)
            .build()
            .toUriString()
        val dto = DimensionRequestBody(actualDate)
        val responseEntity: ResponseEntity<List<DimensionValue>> = sendRequest(
            url = url, r = dto, map = mapOf(
                Pair(HeadersConstants.REGION_ID_HEADER, regionId),
                Pair(HeadersConstants.ROLES_HEADER, roleCode)
            )
        ) { he -> restTemplate.exchange(url, HttpMethod.GET, he, typeReference<List<DimensionValue>>()) }
        log.info("Ответ на запрос на получение различных значений измерений на сервис store. " +
                "Успешно получено ${responseEntity.body} значений.")
        return responseEntity.body!!
    }

    fun getFactValue(roles: String, dimensionValues: List<DimensionValue>, factId: Int, regionId: Int, factDataSourceId: Int): List<FactValue> {
        log.info("Отправка запроса на получение значений показателей для указанных ограничений по измерениям." +
                "ID региона: $regionId, значения измерений: $dimensionValues, роль пользователя: $roles")
        val url = "$storeUrl/$roles/v1/facts/$factId/getdata"
        val factDataReq = FactDataReq(dimensionValues = dimensionValues, dataSource = factDataSourceId)
        val responseEntity: ResponseEntity<List<FactValue>> = sendRequest(
            url = url, r = factDataReq, map = mapOf(
                Pair(HeadersConstants.REGION_ID_HEADER, regionId)
            )
        ) { he -> restTemplate.exchange(url, HttpMethod.POST, he, typeReference<List<FactValue>>()) }
        log.info("Ответ на запрос на получение значений показателей для указанных ограничений по измерениям: ${responseEntity.body!!}")
        return responseEntity.body!!
    }

    private fun createGetDifDimensionsValueParams(parentId: Int?, filterText: String?): MultiValueMap<String, String> {
        val params = LinkedMultiValueMap<String, String>()
        parentId?.let { params.add("parentId", it.toString()) }
        filterText?.let { params.add("text", it) }
        return params
    }

    private fun createGetWorkDayParams(startDate: ZonedDateTime, workDays: Int): MultiValueMap<String, String> {
        val localStartDate = startDate.toLocalDate()
        val params = LinkedMultiValueMap<String, String>()
        return params.also {
            it.add("startDay", localStartDate.toString())
            it.add("workDays", workDays.toString())
        }
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

    private inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

    private fun <T> checkResult(responseEntity: ResponseEntity<T>?) {
        if (responseEntity == null || responseEntity.statusCode != HttpStatus.OK || responseEntity.body == null) {
            val message = "Ошибка при получении ответа на запрос на сервисе аутентификации"
            log.error(message)
            throw CoreException(ErrorCode.INVALID_AUTH_RESPONSE, message)
        }
    }
}