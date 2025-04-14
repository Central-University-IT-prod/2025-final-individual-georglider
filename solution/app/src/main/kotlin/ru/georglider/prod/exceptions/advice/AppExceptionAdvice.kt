package ru.georglider.prod.exceptions.advice

import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import ru.georglider.prod.exceptions.model.AppException
import ru.georglider.prod.payload.response.external.ExceptionResponse

@RestControllerAdvice
@Order(-5000)
class AppExceptionAdvice {

    @ExceptionHandler(AppException::class)
    fun handleAppException(ex: AppException): ResponseEntity<ExceptionResponse> {
        return ResponseEntity.status(ex.statusCode)
            .body(ExceptionResponse(ex.msg))
    }

}