package com.example.ConfigService.controller

import com.example.ConfigService.controller.HeadersConstants.Companion.REGION_ID_HEADER
import com.example.ConfigService.controller.HeadersConstants.Companion.ROLES_HEADER
import com.example.ConfigService.controller.HeadersConstants.Companion.USER_ID_HEADER
import com.example.ConfigService.model.*
import com.example.ConfigService.model.enumeration.ResponseType
import com.example.ConfigService.model.enumeration.Trend
import com.example.ConfigService.service.FactService
import com.example.ConfigService.service.UserDashboardService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import jakarta.validation.constraints.Min
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import ru.rieksp.coreservice.exception.CoreException
import ru.rieksp.coreservice.exception.ErrorCode
import java.time.LocalDate

@RestController
class FactController(private val factService: FactService, private val userDashboardService: UserDashboardService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Operation(summary = "Закрепление показателя на рабочем столе или в модуле")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Успешно закрплен"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Показатель не найден (отсутствует доступ)"
        )
    )
    @PutMapping("/*/v1/facts/{factId}/pin")
    fun pin(@RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
            @RequestHeader(name = USER_ID_HEADER, required = true) currentUserId: Int,
            @PathVariable @Min(1) factId: Int,
            @Validated @RequestBody(required = false) pinFactDto: PinFactDto = PinFactDto()): String {
        log.info("Запрос на закрепление модуля на рабочем столе. ID текущего пользователя: $currentUserId, " +
                "ID региона: $regionId, factId: $factId, ID модуля: ${pinFactDto.viewModuleId}, before: ${pinFactDto.before}")
        factService.pinFact(regionId, factId, pinFactDto, currentUserId)
        return ResponseType.OK.name
    }

    @Operation(summary = "Открепление показателя")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Успешно откреплен"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Показатель не найден (отсутствует доступ)"
        )
    )
    @PutMapping("/*/v1/facts/{factId}/unpin")
    fun unPin(@RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
              @RequestHeader(name = USER_ID_HEADER, required = true) currentUserId: Int,
              @PathVariable @Min(1) factId: Int,
              @Validated @RequestBody(required = false) unPinFactDto: UnPinFactDto = UnPinFactDto()): String {
        log.info("Запрос на открепление показателя. ID текущего пользователя: $currentUserId, " +
                "ID региона: $regionId, ID показателя: ${factId}, ID модуля: ${unPinFactDto.viewModuleId}")
        factService.unPinFact(regionId, factId, currentUserId, unPinFactDto.viewModuleId)
        return ResponseType.OK.name
    }

    @Operation(summary = "Поиск показателей")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Успешно найдены"
        )
    )
    @GetMapping("/*/v1/facts")
    fun getFacts(@RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
                 @RequestHeader(name = USER_ID_HEADER, required = true) currentUserId: Int,
                 @RequestHeader(name = ROLES_HEADER, required = true) roles: String,
                 @RequestParam(name = "text", required = false) text: String? = null,
                 @RequestParam(name = "pinned", required = false) isPinned: Boolean? = null,
                 @RequestParam(name = "moduleId", required = false) moduleId: Int? = null,
                 @RequestParam(name = "trend", required = false) trend: Trend? = null,
                 @RequestParam(name = "pageN", required = false) pageN: Int = 1,
                 @RequestParam(name = "pageSize", required = false) pageSize: Int = 20): List<FactShortDto> {
        log.info("Запрос на поиск показателей. ID текущего пользователя: $currentUserId, ID региона: $regionId, " +
                "роль пользователя: $roles, text = $text, pinned = $isPinned, moduleId = $moduleId, trend = $trend, " +
                "pageNum = $pageN, pageSize = $pageSize")
        return factService.getFacts(currentUserId, regionId, roles, text, isPinned, moduleId, trend, pageN, pageSize)
    }

    @Operation(summary = "Получение настроек дашборда показателя.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Настройки успешно получены"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Показатель не найден (отсутствует доступ)"
        )
    )
    @GetMapping("/*/v1/facts/{factId}/dashboard")
    fun getDashboardSettings(@RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
            @RequestHeader(name = USER_ID_HEADER, required = true) @Min(1) currentUserId: Int,
            @PathVariable @Min(1) factId: Int): UserDashboards {
        log.info("Запрос на получение настроек дашборда показателя. ID текущего пользователя: $currentUserId, " +
                "ID региона: $regionId, ID показателя: ${factId}")
        return userDashboardService.getDashboardSettings(currentUserId, factId).orElseThrow {
            val message ="Не найдены настройки рабочего стола для пользователя с ID = $currentUserId по ID показателя: $factId"
            log.warn(message)
            throw CoreException(ErrorCode.ENTITY_NOT_FOUND, message)
        }
    }

    @Operation(summary = "Запись настроек дашборда показателя")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Настройки успешно записаны"
        ),
        ApiResponse(
            responseCode = "400",
            description = "Показатель не найден (отсутствует доступ)"
        )
    )
    @PutMapping("/*/v1/facts/{factId}/dashboard")
    fun saveDashboardSettings(@RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
                              @RequestHeader(name = USER_ID_HEADER, required = true) @Min(1) currentUserId: Int,
                              @RequestBody userSettingsCreate: UserSettingsCreate,
                              @PathVariable @Min(1) factId: Int): String {
        log.info("Запрос на открепление показателя. ID текущего пользователя: $currentUserId, " +
                "ID региона: $regionId, ID показателя: ${factId}")
        userDashboardService.saveDashboardSettings(currentUserId, factId, userSettingsCreate)
        return ResponseType.OK.name
    }

    @Operation(summary = "Получить информацию о показателе")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Информация о показателе успешно получена"
        )
    )
    @GetMapping("/*/v1/facts/{factId}")
    fun getFactInfo(@PathVariable @Min(1) factId: Int,
            @RequestHeader(name = USER_ID_HEADER, required = true) @Min(1) currentUserId: Int,
            @RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
            @RequestHeader(name = ROLES_HEADER, required = true) roles: String,
            @RequestParam(name = "dimensions", required = false) dimensions: Boolean = false,
            @RequestParam(name = "dataSource", required = false) dataSourceId: Int? = null,
            @RequestParam(name = "reportDate", required = false) @DateTimeFormat(pattern="yyyy.MM.dd", iso = DateTimeFormat.ISO.DATE) reportDate: LocalDate? = null,
            @RequestParam(name = "relative", required = false) relative: Boolean = false,
            @RequestParam(name = "dependent", required = false) dependent: Boolean = false): FactInfoResponse {
        log.info("Запрос на получение информации о показателе. ID показателя: $factId. " +
                "ID текущего пользователя: $currentUserId, ID региона: $regionId, код роли пользователя: $roles, " +
                "ID источника данных: $dataSourceId, получить ли информацию об измерениях показателя: $dimensions, " +
                "дата отчетного периода для показа: $reportDate, выдавать ли информацию о связанных показателях: $relative, " +
                "выдавать ли информацию о зависимых показателях: $dependent")
        return factService.getFactInfo(currentUserId, factId, regionId, roles, reportDate, dataSourceId, dimensions, dependent, relative)
    }

    @Operation(summary = "Получить список названий показателей")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Список названий показателей успешно получен"
        )
    )
    @GetMapping("/methodolog/v1/fact-names")
    fun getFactNames(@RequestHeader(name = USER_ID_HEADER, required = true) @Min(1) currentUserId: Int,
                     @RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
                     @RequestParam(name = "text", required = false) text: String? = null): List<FactName> {
        log.info("Запрос на получение списка названий показателей. ID текущего пользователя: $currentUserId, " +
                "ID региона: $regionId, подстрока для поиска по имени или цели: $text")
        return factService.getFactNames(regionId, text,)
    }

    @Operation(summary = "Получить список зависимых показателей.")
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Список зависимых показателей успешно получен"
        )
    )
    @PostMapping("/internal/v1/graph")
    fun getRelativeFacts(@RequestHeader(name = USER_ID_HEADER, required = true) @Min(1) currentUserId: Int,
                         @RequestHeader(name = REGION_ID_HEADER, required = true) @Min(1) regionId: Int,
                         @RequestHeader(name = ROLES_HEADER, required = true) roles: String,
                         @Validated @RequestBody changes: FactChanges): List<RelativeFactResp> {
        log.info("Запрос на получение списка зависимых показателей. ID текущего пользователя: $currentUserId, " +
                "ID региона: $regionId, роль пользователя: $roles, данные об измененных показателях: $changes")
        return factService.getRelativeFacts(regionId, changes, roles)
    }
}