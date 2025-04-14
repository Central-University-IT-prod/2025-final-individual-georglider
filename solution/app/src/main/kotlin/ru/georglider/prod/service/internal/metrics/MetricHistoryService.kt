package ru.georglider.prod.service.internal.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.DependsOn
import org.springframework.stereotype.Service
import ru.georglider.prod.model.internal.MetricHistory
import ru.georglider.prod.repository.internal.MetricHistoryRepository

@Service
class MetricHistoryService (
    private val metricHistoryRepository: MetricHistoryRepository,
    private val meterRegistry: MeterRegistry
) {

    private final fun loadMetrics(name: String) = metricHistoryRepository.findAllByMetricName(name)
    fun updateMetric(history: MetricHistory) = metricHistoryRepository.upsert(history)

    @DependsOn("dbInitializer")
    @Bean("metricHistoryServiceInitializer")
    fun init(): String {
        loadMetrics("ad_impression_revenue_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        loadMetrics("ad_click_revenue_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        loadMetrics("ad_click_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        loadMetrics("ad_request_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        loadMetrics("ad_impression_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        loadMetrics("ad_repeat_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        loadMetrics("ad_unavailable_counter").doOnNext { pasteIntoPrometheus(it) }.subscribe()
        return ""
    }

    fun pasteIntoPrometheus(history: MetricHistory) {
        Counter.builder(history.metricName)
            .tags("date", history.date.toString())
            .register(meterRegistry)
            .increment(history.amount)
    }

}