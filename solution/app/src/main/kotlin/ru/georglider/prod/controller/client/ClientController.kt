package ru.georglider.prod.controller.client

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.BadRequestException
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.payload.dto.client.ClientDTO
import ru.georglider.prod.payload.request.client.BulkEntity
import ru.georglider.prod.service.ClientService
import java.util.*

@RestController
@RequestMapping("/clients")
class ClientController (
    private val service: ClientService
) {

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): Mono<ResponseEntity<ClientDTO>> {
        return service.findById(id)
            .mapNotNull { ClientDTO(it) }
            .mapNotNull { ResponseEntity.ok(it) }
            .switchIfEmpty { Mono.error(NotFoundException()) }
    }

    @PostMapping("/bulk")
    fun bulk(@RequestBody @Valid body: List<BulkEntity>): Mono<ResponseEntity<List<ClientDTO>>> {
        if (!body.all { it.isValid() }) throw BadRequestException()

        return service.save(body).map { ClientDTO(it) }.collectList().map { clients ->
            clients.distinctBy { it.clientId }
        }.flatMap { dtos ->
            if (body.size != dtos.size) return@flatMap service.findAllById(dtos.map { it.clientId })
                .map { ClientDTO(it) }.collectList()
            Mono.just(dtos)
        }.map {
            ResponseEntity.status(HttpStatus.CREATED).body(it)
        }
    }


}