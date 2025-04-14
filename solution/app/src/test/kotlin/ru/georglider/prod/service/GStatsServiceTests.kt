package ru.georglider.prod.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.advertiser.campaign.stats.RecordType
import ru.georglider.prod.model.advertiser.campaign.stats.StatRecord
import ru.georglider.prod.repository.CampaignRepository
import ru.georglider.prod.repository.advertiser.AdvertiserAdvancedRepository
import ru.georglider.prod.repository.advertiser.AdvertiserRepository
import ru.georglider.prod.repository.advertiser.stats.AdvancedStatRecordRepository
import ru.georglider.prod.repository.advertiser.stats.StatRecordRepository
import ru.georglider.prod.service.internal.metrics.AdvertiserMetricService
import ru.georglider.prod.service.internal.metrics.CampaignMetricService
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Tag("test")
@ExtendWith(SpringExtension::class, MockitoExtension::class)
@ExtendWith(DatabaseSetupExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GStatsServiceTests {

    @Autowired private lateinit var advancedStatRecordRepository: AdvancedStatRecordRepository
    @Autowired private lateinit var statRecordRepository: StatRecordRepository
    private val timeService = mockk<TimeService>()
    private val campaignRepository = mockk<CampaignRepository>()
    private val metrics = mockk<CampaignMetricService>()
    private val moderationService = mockk<ModerationService>()
    private val campaignService = CampaignService(campaignRepository, metrics, moderationService)
    private val advertiserAdvancedRepository = mockk<AdvertiserAdvancedRepository>()
    private val advertiserRepository = mockk<AdvertiserRepository>()
    private val advertiserMetrics = mockk<AdvertiserMetricService>()
    private val advertiserService = AdvertiserService(advertiserRepository, advertiserAdvancedRepository, advertiserMetrics)

    @Test
    fun statsEmulationCheck() {
        val service = StatService(timeService, statRecordRepository, advancedStatRecordRepository, campaignService, advertiserService)
        every { timeService.get() } returns 30

        val advertiserId = UUID.randomUUID()
        val campaignIds = arrayOf(UUID.randomUUID(), UUID.randomUUID())

        val campaigns = listOf(
            Campaign(advertiserId, 500, 20, 5.0, 20.0, "Test1", "Test1", 28, 32, null, null, null, null, campaignIds[0]),
            Campaign(advertiserId, 200, 50, 7.0, 21.0, "Test2", "Test2", 25, 30, null, null, null, null, campaignIds[1])
        )

        campaigns.forEach { campaign ->
            every { campaignRepository.findByCampaignId(campaign.campaignId!!) } returns Mono.just(campaign)
        }
        every { campaignRepository.findAllByAdvertiserId(advertiserId) } returns campaigns.toFlux()

        val startingDay = 25

        val dateSpentImpressions = arrayOf(DoubleArray(33-startingDay) { 0.0 }, DoubleArray(33-startingDay) { 0.0 })
        val dateSpentClicks = arrayOf(DoubleArray(33-startingDay) { 0.0 }, DoubleArray(33-startingDay) { 0.0 })
        val dateEmulatedImpressions = arrayOf(IntArray(33-startingDay) { 0 }, IntArray(33-startingDay) { 0 })
        val dateEmulatedClicks = arrayOf(IntArray(33-startingDay) { 0 }, IntArray(33-startingDay) { 0 })

        fun emulateDay(id: Int, day: Int, impressions: Int, clicks: Int, impressionCost: Double, clickCost: Double) {
            every { timeService.get() } returns day
            repeat(impressions) { service.addStat(StatRecord(advertiserId, campaignIds[id], impressionCost, RecordType.IMPRESSION)).block() }
            repeat(clicks) { service.addStat(StatRecord(advertiserId, campaignIds[id], clickCost, RecordType.CLICK)).block() }

            dateSpentImpressions[id][day-startingDay] = impressionCost * impressions
            dateSpentClicks[id][day-startingDay] = clickCost * clicks
            dateEmulatedImpressions[id][day-startingDay] = impressions
            dateEmulatedClicks[id][day-startingDay] = clicks
        }

        emulateDay(0, 28, 6, 3, 5.0, 20.0)
        emulateDay(0, 29, 6, 3, 5.0, 20.0)
        emulateDay(0, 30, 10, 10, 5.0, 20.0)
        emulateDay(0, 32, 3, 4, 5.0, 20.0)

        emulateDay(1, 25, 10, 0, 7.0, 21.0)
        emulateDay(1, 27, 6, 3, 4.0, 16.0)
        emulateDay(1, 28, 2, 1, 7.0, 21.0)
        emulateDay(1, 29, 4, 1, 7.0, 21.0)
        emulateDay(1, 30, 10, 9, 7.0, 21.0)

        campaigns.forEachIndexed { index, _ ->
            val combinedStats = service.getCombinedCampaignStats(campaignIds[index]).block()

            val summDateSpentImpressions = dateSpentImpressions[index].reduce { acc, d -> acc + d }
            val summDateSpentClicks = dateSpentClicks[index].reduce { acc, d -> acc + d }
            val summDateEmulatedImpressions = dateEmulatedImpressions[index].reduce { acc, d -> acc + d }
            val summDateEmulatedClicks = dateEmulatedClicks[index].reduce { acc, d -> acc + d }

            assertNotNull(combinedStats)
            assertEquals(summDateSpentImpressions, combinedStats.spentImpressions)
            assertEquals(summDateSpentClicks, combinedStats.spentClicks)
            assertEquals(summDateEmulatedImpressions, combinedStats.impressionsCount)
            assertEquals(summDateEmulatedClicks, combinedStats.clicksCount)
            if (summDateEmulatedClicks > 0) {
                assertEquals(summDateEmulatedClicks.toDouble() / summDateEmulatedImpressions.toDouble() * 100, combinedStats.conversion)
            } else {
                assertEquals(0.0, combinedStats.conversion)
            }

            val dailyStats = service.getDailyCampaignStats(campaignIds[index]).block()
            assertNotNull(dailyStats)
            for (day in dailyStats) {
                val spentImpressions = dateSpentImpressions[index][day.date - startingDay]
                val spentClicks = dateSpentClicks[index][day.date - startingDay]
                val emulatedImpressions = dateEmulatedImpressions[index][day.date - startingDay]
                val emulatedClicks = dateEmulatedClicks[index][day.date - startingDay]

                assertEquals(spentImpressions, day.spentImpressions)
                assertEquals(spentClicks, day.spentClicks)
                assertEquals(emulatedImpressions, day.impressionsCount)
                assertEquals(emulatedClicks, day.clicksCount)
                if (emulatedImpressions > 0) {
                    assertEquals(emulatedClicks.toDouble() / emulatedImpressions.toDouble() * 100, day.conversion)
                } else {
                    assertEquals(0.0, day.conversion)
                }
            }
        }

        val combinedAllStats = service.getCombinedAdvertiserStats(advertiserId).block()

        val summDateSpentImpressionsAll = dateSpentImpressions[0].reduce { acc, d -> acc + d } + dateSpentImpressions[1].reduce { acc, d -> acc + d }
        val summDateSpentClicksAll = dateSpentClicks[0].reduce { acc, d -> acc + d } + dateSpentClicks[1].reduce { acc, d -> acc + d }
        val summDateEmulatedImpressionsAll = dateEmulatedImpressions[0].reduce { acc, d -> acc + d } + dateEmulatedImpressions[1].reduce { acc, d -> acc + d }
        val summDateEmulatedClicksAll = dateEmulatedClicks[0].reduce { acc, d -> acc + d } + dateEmulatedClicks[1].reduce { acc, d -> acc + d }

        assertNotNull(combinedAllStats)
        assertEquals(summDateSpentImpressionsAll, combinedAllStats.spentImpressions)
        assertEquals(summDateSpentClicksAll, combinedAllStats.spentClicks)
        assertEquals(summDateEmulatedImpressionsAll, combinedAllStats.impressionsCount)
        assertEquals(summDateEmulatedClicksAll, combinedAllStats.clicksCount)
        if (summDateEmulatedImpressionsAll > 0) {
            assertEquals(summDateEmulatedClicksAll.toDouble() / summDateEmulatedImpressionsAll.toDouble() * 100, combinedAllStats.conversion)
        } else {
            assertEquals(0.0, combinedAllStats.conversion)
        }

        val dailyAdvertiserStats = service.getDailyAdvertiserStats(advertiserId).block()!!
        for (day in dailyAdvertiserStats) {
            val spentImpressions = dateSpentImpressions[0][day.date - startingDay] + dateSpentImpressions[1][day.date - startingDay]
            val spentClicks = dateSpentClicks[0][day.date - startingDay] + dateSpentClicks[1][day.date - startingDay]
            val emulatedImpressions = dateEmulatedImpressions[0][day.date - startingDay] + dateEmulatedImpressions[1][day.date - startingDay]
            val emulatedClicks = dateEmulatedClicks[0][day.date - startingDay] + dateEmulatedClicks[1][day.date - startingDay]

            assertEquals(spentImpressions, day.spentImpressions)
            assertEquals(spentClicks, day.spentClicks)
            assertEquals(emulatedImpressions, day.impressionsCount)
            assertEquals(emulatedClicks, day.clicksCount)
            if (emulatedImpressions > 0) {
                assertEquals(emulatedClicks.toDouble() / emulatedImpressions.toDouble() * 100, day.conversion)
            } else {
                assertEquals(0.0, day.conversion)
            }
        }
    }

}