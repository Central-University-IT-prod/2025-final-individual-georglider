package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.client.MLScore
import ru.georglider.prod.repository.client.AdvancedMLScoreRepository

@Service
class MLService (
    private val repository: AdvancedMLScoreRepository,
    private val advertiserService: AdvertiserService,
    private val clientService: ClientService
) {

    fun upsertScore(score: MLScore): Mono<Long> {
        return Mono.zip(
            advertiserService.existsById(score.advertiserId),
            clientService.existsById(score.clientId),
        ).map { it.t1 && it.t2 }.handle { it, sink -> if (it) sink.next(it) else sink.error(NotFoundException()) }.flatMap {
            repository.upsert(score)
        }
    }

}