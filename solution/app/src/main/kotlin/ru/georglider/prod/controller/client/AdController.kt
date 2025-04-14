package ru.georglider.prod.controller.client

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import ru.georglider.prod.payload.request.client.AdRequest
import ru.georglider.prod.payload.response.client.AdClickResponse
import ru.georglider.prod.payload.response.client.AdResponse
import ru.georglider.prod.service.AdService
import ru.georglider.prod.service.internal.metrics.AdMetricService
import java.util.*

@RestController
@RequestMapping("/ads")
class AdController (
    private val adService: AdService,
    private val metrics: AdMetricService
) {

    @GetMapping
    fun get(
        @RequestParam(value = "client_id", required = true) clientId: UUID
    ): Mono<AdResponse> {
        return Mono.just(clientId)
            .flatMap { adService.show(it) }
            .doOnError { exception -> metrics.incrementException(exception) }
    }

    @PostMapping("/{adId}/click")
    fun click(
        @PathVariable("adId") adId: UUID,
        @RequestBody @Valid body: AdRequest
    ): Mono<ResponseEntity<AdClickResponse>> {
        return Mono.just(adId).flatMap {
            adService.activate(it, body.clientId!!)
        }.map { ResponseEntity.status(204).body(AdClickResponse(it)) }
    }

}