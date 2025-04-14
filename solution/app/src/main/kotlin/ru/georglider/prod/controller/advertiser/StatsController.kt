package ru.georglider.prod.controller.advertiser

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.georglider.prod.service.StatService
import java.util.UUID

@RestController
@RequestMapping("/stats")
class StatsController (
    private val statService: StatService
) {
    
    @GetMapping("/campaigns/{campaignId}")
    fun getCampaignStats(@PathVariable campaignId: UUID) = statService.getCombinedCampaignStats(campaignId)

    @GetMapping("/campaigns/{campaignId}/daily")
    fun getDailyCampaignStats(@PathVariable campaignId: UUID) = statService.getDailyCampaignStats(campaignId)

    @GetMapping("/advertisers/{advertiserId}/campaigns")
    fun getAdvertiserStats(@PathVariable advertiserId: UUID) = statService.getCombinedAdvertiserStats(advertiserId)
    
    @GetMapping("/advertisers/{advertiserId}/campaigns/daily")
    fun getDailyAdvertiserStats(@PathVariable advertiserId: UUID) = statService.getDailyAdvertiserStats(advertiserId)

}