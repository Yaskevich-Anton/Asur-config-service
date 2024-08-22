package com.example.ConfigService.service.client

import com.example.ConfigService.model.EmailRequestDto
import com.example.ConfigService.model.MailRequestDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

@Service
class NotificationRestClient(
    @Value("\${application.notification.service.url}")
    private val notificationUrl: String,
    private val restTemplate: RestTemplate) {

    private val log = LoggerFactory.getLogger(javaClass)
    fun sendNotification(mailRequestDto: MailRequestDto) {
        log.info("Запрос на отправку писем пользователям. {}", mailRequestDto)
        val httpEntity = HttpEntity<MailRequestDto>(mailRequestDto)
        val url = "$notificationUrl/internal/v1/send"
        sendRequest(url, httpEntity)
    }

    fun sendNotification(requestDto: EmailRequestDto) {
        log.info("Запрос на отправку писем пользователям. {}", requestDto)
        val httpEntity = HttpEntity<EmailRequestDto>(requestDto)
        val url = "$notificationUrl/internal/v1/send-emails"
        sendRequest(url, httpEntity)
    }

    private fun <T> sendRequest(url: String, httpEntity: HttpEntity<T>) {
        val responseEntity: ResponseEntity<Unit> = try {
            restTemplate.postForEntity(url, httpEntity, Unit::class.java)
        } catch (e: RestClientException) {
            log.error("Ошибка при отправке писем пользователям. url = $url, ${e.message}")
            throw RestClientException("${e.message}")
        }
        if (responseEntity == null || responseEntity.statusCode != HttpStatus.OK) {
            log.error("Ошибка при отправке писем пользователям")
            throw RuntimeException("Ошибка при отправке писем пользователям")
        }
        log.info("Запрос на отправку писем пользователям. Получен код ответа: {}", responseEntity.statusCode)
    }
}