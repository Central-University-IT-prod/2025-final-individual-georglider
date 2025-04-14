package ru.georglider.prod.service.internal.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.georglider.prod.repository.advertiser.AdvertiserRepository
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Service
class AdvertiserMetricService (
    private val repository: AdvertiserRepository,
    meterRegistry: MeterRegistry
) {

    private val advertiserAmount = AtomicLong(0)
    private val needsCheck = AtomicBoolean(true)
    private val advertiserAmountGauge = Gauge
        .builder("advertiser_amount") { advertiserAmount.get() }
        .register(meterRegistry)

    fun refreshState() {
        needsCheck.set(true)
    }

    @Scheduled(fixedRate = 15000)
    fun refreshAdvertiserAmount() {
        if (needsCheck.getAndSet(false)) {
            repository.count().doOnNext {
                advertiserAmount.set(it)
            }.subscribe()
        }
    }

}