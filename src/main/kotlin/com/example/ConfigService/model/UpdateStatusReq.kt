package com.example.ConfigService.model

import com.example.ConfigService.model.enumeration.UserStatus


data class UpdateStatusReq(val status: UserStatus, val generatePwd: Boolean, val regionId: Int)
