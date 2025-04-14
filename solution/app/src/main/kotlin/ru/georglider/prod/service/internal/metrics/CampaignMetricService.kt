package ru.georglider.prod.service.internal.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import ru.georglider.prod.repository.CampaignRepository
import ru.georglider.prod.service.TimeService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class CampaignMetricService (
    private val repository: CampaignRepository,
    private val timeService: TimeService,
    meterRegistry: MeterRegistry
) {

    private val campaignAmount = AtomicLong(0)
    private val activeCampaignAmount = AtomicLong(0)
    private val needsCheck = AtomicBoolean(true)
    private val lastTimeCheck = AtomicInteger(0)
    private val campaignAmountGauge = Gauge
        .builder("campaign_amount") { campaignAmount.get() }
        .register(meterRegistry)
    private val activeCampaignAmountGauge = Gauge
        .builder("campaign_amount_active") { activeCampaignAmount.get() }
        .register(meterRegistry)


    fun refreshState() {
        needsCheck.set(true)
    }

    @Scheduled(fixedRate = 15000)
    fun refreshCampaignAmount() {
        val time = timeService.get()
        val lastTimeCheck = lastTimeCheck.getAndSet(time)
        if (needsCheck.getAndSet(false) || lastTimeCheck != time) {
            repository.count().doOnNext {
                campaignAmount.set(it)
            }.subscribe()
            repository.countActive(time).doOnNext {
                activeCampaignAmount.set(it)
            }.subscribe()
        }
    }

}