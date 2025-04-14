package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.kotlin.core.publisher.toFlux
import ru.georglider.prod.model.advertiser.Advertiser
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import ru.georglider.prod.repository.advertiser.AdvertiserAdvancedRepository
import ru.georglider.prod.repository.advertiser.AdvertiserRepository
import ru.georglider.prod.service.internal.metrics.AdvertiserMetricService
import java.util.*

@Service
class AdvertiserService (
    private val repository: AdvertiserRepository,
    private val advancedRepository: AdvertiserAdvancedRepository,
    private val metrics: AdvertiserMetricService
) {

    fun save(entities: List<BulkEntity>): Flux<Advertiser> {
        return entities.map { Advertiser(it) }.toFlux()
            .flatMap { c -> advancedRepository.upsert(c).map { c } }
            .doOnNext { metrics.refreshState() }
    }

    fun findById(id: UUID) = repository.findById(id)
    fun existsById(id: UUID) = repository.existsById(id)
    fun findAllById(ids: List<UUID>): Flux<Advertiser> = repository.findAllById(ids)

}
