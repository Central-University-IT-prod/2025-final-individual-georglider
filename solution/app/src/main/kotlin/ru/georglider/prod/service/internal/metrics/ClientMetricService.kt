package ru.georglider.prod.service.internal.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.georglider.prod.repository.client.ClientRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Service
class ClientMetricService (
    private val repository: ClientRepository,
    meterRegistry: MeterRegistry
) {

    private val amount = AtomicLong(0)
    private val needsCheck = AtomicBoolean(true)
    private val gauge = Gauge
        .builder("client_amount") { amount.get() }
        .register(meterRegistry)

    fun refreshState() {
        needsCheck.set(true)
    }

    @Scheduled(fixedRate = 15000)
    fun refreshClientAmount() {
        if (needsCheck.getAndSet(false)) {
            repository.count().doOnNext {
                amount.set(it)
            }.subscribe()
        }
    }

}