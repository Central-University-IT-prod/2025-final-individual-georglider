package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import ru.georglider.prod.model.client.Client
import ru.georglider.prod.payload.request.client.BulkEntity
import ru.georglider.prod.repository.client.ClientAdvancedRepository
import ru.georglider.prod.repository.client.ClientRepository
import ru.georglider.prod.service.internal.metrics.ClientMetricService
import java.util.*

@Service
class ClientService (
    private val advancedRepository: ClientAdvancedRepository,
    private val repository: ClientRepository,
    private val metrics: ClientMetricService
) {

    fun save(entities: List<BulkEntity>): Flux<Client> {
        return entities.map { Client(it) }.toFlux()
            .flatMap { c -> advancedRepository.upsert(c).map { c } }
            .doOnNext { metrics.refreshState() }
    }

    fun findById(id: UUID) = repository.findById(id)
    fun existsById(id: UUID) = repository.existsById(id)
    fun findAllById(ids: List<UUID>): Flux<Client> = repository.findAllById(ids)

}