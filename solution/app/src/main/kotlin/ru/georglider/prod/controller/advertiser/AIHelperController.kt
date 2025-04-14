package ru.georglider.prod.controller.advertiser

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.payload.request.advertiser.AIGenerationRequest
import ru.georglider.prod.payload.response.advertiser.AIResponse
import ru.georglider.prod.service.AdvertiserService
import ru.georglider.prod.service.internal.LLMService
import java.util.*

@RestController
@RequestMapping("/advertisers/{advertiserId}/generation")
class AIHelperController (
    private val llmService: LLMService,
    private val advertiserService: AdvertiserService,
) {

    @PostMapping("/stream")
    fun generateStream(
        @PathVariable("advertiserId") advertiserId: UUID,
        @RequestBody @Valid body: AIGenerationRequest
    ): Flux<String> {
        return advertiserService.findById(advertiserId).switchIfEmpty {
            Mono.error(NotFoundException())
        }.flatMapMany { advertiser ->
            llmService.generate(advertiser.name, body.title)
        }
    }

    @PostMapping
    fun generate(
        @PathVariable("advertiserId") advertiserId: UUID,
        @RequestBody @Valid body: AIGenerationRequest
    ): Mono<AIResponse> {
        return advertiserService.findById(advertiserId).switchIfEmpty {
            Mono.error(NotFoundException())
        }.flatMap { advertiser ->
            llmService.generate(advertiser.name, body.title)
                .filter { it.isNotBlank() }
                .collectList()
                .map { it.joinToString("") }
                .map { it.replace("\"", "") }
        }.map {
            AIResponse(it)
        }
    }

}