package com.example.ConfigService.model.mapper

import com.example.ConfigService.model.DimensionValue
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class DimensionValueMapper(val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun toJson(dimValue: DimensionValue): String {
        val objectMapper = ObjectMapper()
        objectMapper.registerModule(JavaTimeModule())
        try {
            return objectMapper.writeValueAsString(dimValue)
        } catch (jpe: JsonProcessingException) {
            log.error("Ошибка при преобразовании в json. dimValue = $dimValue")
            throw RuntimeException("Ошибка при преобразовании в json. dimValue = $dimValue")
        }
    }

    fun fromJson(value: String): List<DimensionValue> {
        objectMapper.registerKotlinModule()
        return try {
            objectMapper.readValue(value, object : TypeReference<List<DimensionValue>>() {})
        } catch (e: JsonProcessingException) {
            log.error("Ошибка при чтении DimensionsValue из БД: $value")
            throw RuntimeException("Ошибка при чтении DimensionsValue из БД", e)
        }
    }
}