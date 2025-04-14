package ru.georglider.prod.controller

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ru.georglider.prod.model.client.MLScore
import ru.georglider.prod.payload.request.advertiser.MLScoreRequest
import ru.georglider.prod.service.MLService

@RestController
@RequestMapping("/ml-scores")
class MLController (
    private val service: MLService
) {

    @PostMapping
    fun mlUpsert(@RequestBody @Valid body: MLScoreRequest): Mono<ResponseEntity<Nothing>> {
        return service.upsertScore(MLScore(body))
            .map { ResponseEntity.ok(null) }
    }

}