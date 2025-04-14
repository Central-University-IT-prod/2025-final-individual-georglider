package ru.georglider.prod.service

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty
import ru.georglider.prod.exceptions.model.NotFoundException
import ru.georglider.prod.model.advertiser.campaign.stats.RecordType
import ru.georglider.prod.model.advertiser.campaign.stats.StatRecord
import ru.georglider.prod.payload.dto.advertiser.stats.DateStats
import ru.georglider.prod.payload.dto.advertiser.stats.Stats
import ru.georglider.prod.payload.dto.internal.DbDateStats
import ru.georglider.prod.repository.advertiser.stats.AdvancedStatRecordRepository
import ru.georglider.prod.repository.advertiser.stats.StatRecordRepository
import java.util.*

@Service
class StatService (
    private val timeService: TimeService,
    private val repository: StatRecordRepository,
    private val advancedRepository: AdvancedStatRecordRepository,
    private val campaignService: CampaignService,
    private val advertiserService: AdvertiserService
) {

    fun addStat(statRecord: StatRecord): Mono<StatRecord> {
        statRecord.date = timeService.get()
        return repository.save(statRecord)
    }

    fun getCombinedCampaignStats(campaignId: UUID): Mono<Stats> {
        return advancedRepository.retrieveStatsByCampaignId(campaignId).collectList().flatMap { list ->
            if (list.isNotEmpty()) return@flatMap Mono.just(list)
            campaignService.existsByCampaignId(campaignId)
                .map { list }
                .switchIfEmpty { Mono.error(NotFoundException()) }
        }.map { stats ->
            val (impressions, clicks) = stats.partition { it.type == RecordType.IMPRESSION }
            Stats(impressions, clicks)
        }
    }

    fun getCombinedAdvertiserStats(advertiserId: UUID): Mono<Stats> {
        return advancedRepository.retrieveStatsByAdvertiserId(advertiserId).collectList().flatMap { list ->
            if (list.isNotEmpty()) return@flatMap Mono.just(list)
            advertiserService.existsById(advertiserId)
                .mapNotNull { if (it) list else null }
                .switchIfEmpty { Mono.error(NotFoundException()) }
        }.map { stats ->
            val (impressions, clicks) = stats.partition { it.type == RecordType.IMPRESSION }
            Stats(impressions, clicks)
        }
    }

    fun getDailyAdvertiserStats(advertiserId: UUID): Mono<List<DateStats>> {
        return Mono.zip(
            campaignService.findAllByAdvertiserId(advertiserId)
                .flatMap { campaign -> Flux.range(campaign.startDate, campaign.endDate - campaign.startDate + 1) }
                .distinct().filter { it <= timeService.get() }.collectList(),
            advancedRepository.retrieveDailyStatsByAdvertiserId(advertiserId).collectList()
        ).flatMap { res ->
            if (res.t2.isNotEmpty()) return@flatMap Mono.just(Pair(res.t1, res.t2))
            advertiserService.existsById(advertiserId)
                .handle { it, sink -> if (it) sink.next(Pair(res.t1, res.t2)) else sink.error(NotFoundException()) }
                .switchIfEmpty { Mono.error(NotFoundException()) }
        }.map { dateStatsGenerator(it.first, it.second) }
    }

    fun getDailyCampaignStats(campaignId: UUID): Mono<List<DateStats>> {
        return Mono.zip(
            campaignService.findByCampaignId(campaignId)
                .switchIfEmpty { Mono.error(NotFoundException()) }
                .flatMapMany { campaign -> Flux.range(campaign.startDate, campaign.endDate - campaign.startDate + 1) }
                .filter { it <= timeService.get() }
                .collectList(),
            advancedRepository.retrieveDailyStatsByCampaignId(campaignId).collectList()
        ).map { dateStatsGenerator(it.t1, it.t2) }
    }

    private fun dateStatsGenerator(days: List<Int>, stats: List<DbDateStats>): List<DateStats> {
        return days.union(stats.map { it.date }).map { day ->
            val (impressions, clicks) = stats.filter { it.date == day }.partition { it.type == RecordType.IMPRESSION }
            DateStats(day, impressions, clicks)
        }
    }

}