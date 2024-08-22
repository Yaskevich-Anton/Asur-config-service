package ru.rieksp.coreservice.exception

class CoreException(val errorCode: ErrorCode, message: String, exception: Throwable? = null):
    RuntimeException(message, exception) {
}