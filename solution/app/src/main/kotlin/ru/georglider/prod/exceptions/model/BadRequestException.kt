package ru.georglider.prod.exceptions.model

class BadRequestException (msg: String? = null) : AppException(400, msg ?: "Bad Request")