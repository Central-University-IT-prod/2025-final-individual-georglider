package ru.georglider.prod.exceptions.model

import org.springframework.http.HttpStatusCode

open class AppException(
    val statusCode: Int,
    val msg: String
) : Throwable(msg) {
    constructor(statusCode: HttpStatusCode, msg: String) : this(statusCode.value(), msg)
}