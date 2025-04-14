package ru.georglider.prod.exceptions.handlers

import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.validation.method.MethodValidationException
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono
import ru.georglider.prod.payload.response.external.ExceptionResponse
import java.util.stream.Collectors

@RestControllerAdvice
@Order(-1000)
class ValidationException : ResponseEntityExceptionHandler() {

    override fun handleWebExchangeBindException(
        ex: WebExchangeBindException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Any>> {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ExceptionResponse(
                ex.bindingResult.fieldErrors.stream().map<String?> { err: FieldError -> err.defaultMessage }
                    .distinct()
                    .collect(Collectors.joining("; "))
            )
        ))
    }

    override fun handleMethodValidationException(
        ex: MethodValidationException,
        status: HttpStatus,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Any>> {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ExceptionResponse(
                ex.message ?: "Method validation error"
            )
        ))
    }

    override fun handleHandlerMethodValidationException(
        ex: HandlerMethodValidationException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Any>> {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ExceptionResponse(
                ex.reason ?: "Invalid body"
            )
        ))
    }

    override fun handleServerWebInputException(
        ex: ServerWebInputException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Any>> {
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
            ExceptionResponse(
                "Wrong JSON body"
            )
        ))
    }

}