package ru.georglider.prod.service.internal.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component
import reactor.core.scheduler.Schedulers
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.internal.MetricHistory
import ru.georglider.prod.service.TimeService
import java.util.concurrent.atomic.AtomicInteger


@Component
class AdMetricService(
    private val timeService: TimeService,
    private val metricHistoryService: MetricHistoryService,
    private val meterRegistry: MeterRegistry
) {

    private val lastDate = AtomicInteger(0)

    private final fun refresh() {
        if (lastDate.get() != timeService.get()) {
            lastDate.set(timeService.get())
            incrementStat("ad_request_counter", 0.0)
            incrementStat("ad_unavailable_counter", 0.0)
            incrementStat("ad_impression_counter", 0.0)
            incrementStat("ad_repeat_counter", 0.0)
            incrementStat("ad_click_counter", 0.0)
            incrementStat("ad_click_revenue_counter", 0.0)
            incrementStat("ad_impression_revenue_counter", 0.0)
        }
    }

    private fun incrementRequestAmount() {
        refresh()
        incrementStat("ad_request_counter")
    }

    fun incrementException(exception: Throwable) {
        incrementRequestAmount()
        if (exception is NotFoundException) {
            incrementStat("ad_unavailable_counter")
        }
    }

    fun incrementSuccessAdRequests(profit: Double) {
        incrementRequestAmount()
        incrementStat("ad_impression_counter")
        incrementStat("ad_impression_revenue_counter", profit)
    }

    fun incrementSuccessAdRequests() {
        incrementRequestAmount()
        incrementStat("ad_repeat_counter")
    }

    fun incrementUniqueClick(profit: Double) {
        refresh()
        incrementStat("ad_click_counter")
        incrementStat("ad_click_revenue_counter", profit)
    }

    private fun incrementStat(name: String, amount: Double = 1.0) {
        metricHistoryService.updateMetric(
            MetricHistory(timeService.get(), amount, name)
        ).subscribeOn(Schedulers.boundedElastic()).subscribe()
        Counter.builder(name)
            .tag("date", timeService.get().toString())
            .register(meterRegistry)
            .increment(amount)
    }

}