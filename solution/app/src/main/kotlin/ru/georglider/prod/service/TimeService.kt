package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import ru.georglider.prod.exceptions.model.BadRequestException
import ru.georglider.prod.utils.WaitPostgresUtil
import java.util.concurrent.atomic.AtomicInteger

@Service
class TimeService (
    private val service: ConfigService
) {

    private var now = AtomicInteger(WaitPostgresUtil.initTime)

    fun set(date: Int): Mono<Int> {
        if (date < now.get()) throw BadRequestException("Invalid date")
        now.set(date)

        return service.upsert(1, date).map { date }
    }

    fun get(): Int = now.get()

}