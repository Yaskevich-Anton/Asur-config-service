package com.example.ConfigService

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotlinProjectApplication

fun main(args: Array<String>) {
    runApplication<com.example.ConfigService.KotlinProjectApplication>(*args)
}
