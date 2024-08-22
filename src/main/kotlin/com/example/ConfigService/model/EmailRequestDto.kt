package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.MailTemplateType

data class EmailRequestDto(val emails: List<String>, val templateType: MailTemplateType, val params: String) {
}