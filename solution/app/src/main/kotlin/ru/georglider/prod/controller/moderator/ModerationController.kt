package ru.georglider.prod.controller.moderator

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.georglider.prod.model.internal.PaginationDetails
import ru.georglider.prod.payload.request.moderator.DecisionRequest
import ru.georglider.prod.service.ModerationService

@RestController
@RequestMapping("/moderation")
class ModerationController (
    private val service: ModerationService
) {

    @PostMapping("/enable")
    fun enable() = service.set(true).map { "Successfully enabled moderation!" }

    @PostMapping("/disable")
    fun disable() = service.set(false).map { "Successfully disabled moderation!" }

    @GetMapping("/status")
    fun status(): String {
        if (service.isEnabled()) return "Moderation enabled!"
        return "Moderation is currently disabled!"
    }

    @GetMapping("/requests")
    fun requests(
        @Min(1, message = "Страница должна начинаться с 1")
        @RequestParam(value = "page", required = false, defaultValue = "1") page: Int,
        @Min(0, message = "Размер страницы должен быть больше 0")
        @RequestParam(value = "size", required = false, defaultValue = "10") size: Int,
    ) = service.findPaginated(PaginationDetails(page, size))

    @GetMapping("/request")
    fun request() = service.findOne()

    @PostMapping("/approve")
    fun approve(
        @RequestBody @Valid body: DecisionRequest
    ): Mono<String> {
        return service.approve(body.campaignId).defaultIfEmpty(true)
            .map { "Successfully approved ${body.campaignId}!" }
    }

    @PostMapping("/reject")
    fun reject(
        @RequestBody @Valid body: DecisionRequest
    ): Mono<String> {
        return service.reject(body.campaignId).defaultIfEmpty(true)
            .map { "Successfully rejected ${body.campaignId}!" }
    }
}