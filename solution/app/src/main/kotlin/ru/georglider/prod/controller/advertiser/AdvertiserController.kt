package ru.georglider.prod.controller.advertiser

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.payload.dto.advertiser.AdvertiserDTO
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import ru.georglider.prod.service.AdvertiserService
import java.util.*

@RestController
@RequestMapping("/advertisers")
class AdvertiserController (
    private val service: AdvertiserService
) {

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): Mono<ResponseEntity<AdvertiserDTO>> {
        return service.findById(id)
            .mapNotNull { AdvertiserDTO(it) }
            .mapNotNull { ResponseEntity.ok(it) }
            .switchIfEmpty { Mono.error(NotFoundException()) }
    }

    @PostMapping("/bulk")
    fun bulk(@RequestBody @Valid body: List<BulkEntity>): Mono<ResponseEntity<List<AdvertiserDTO>>> {
        return service.save(body).map { AdvertiserDTO(it) }.collectList().map { clients ->
            clients.distinctBy { it.advertiserId }
        }.flatMap { dtos ->
            if (body.size != dtos.size) return@flatMap service.findAllById(dtos.map { it.advertiserId })
                .map { AdvertiserDTO(it) }.collectList()
            Mono.just(dtos)
        }.map {
            ResponseEntity.status(HttpStatus.CREATED).body(it)
        }
    }

}