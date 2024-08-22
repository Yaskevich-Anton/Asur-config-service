package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.MailTemplateType


data class MailRequestDto(val toUsers: String, val templateType: MailTemplateType, val params: String) {
}