package ru.georglider.prod.controller

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.georglider.prod.payload.request.time.TimeSetRequest
import ru.georglider.prod.payload.response.time.TimeSetResponse
import ru.georglider.prod.service.TimeService

@RestController
@RequestMapping("/time/advance")
class TimeController (
    private val service: TimeService
) {

    @PostMapping
    fun setTime(@RequestBody @Valid request: TimeSetRequest): Mono<TimeSetResponse> {
        return service.set(request.date!!).map {
            TimeSetResponse(it)
        }
    }

}