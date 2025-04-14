package ru.georglider.prod.service

import io.mockk.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.context.junit.jupiter.SpringExtension
import reactor.core.publisher.Mono
import ru.georglider.prod.controller.advertiser.CampaignsController
import ru.georglider.prod.exceptions.model.BadRequestException
import ru.georglider.prod.model.advertiser.campaign.Campaign
import ru.georglider.prod.model.common.Gender
import ru.georglider.prod.payload.dto.advertiser.campaign.CampaignDTO
import ru.georglider.prod.payload.dto.advertiser.campaign.GeneratedCampaignValuesHolder
import ru.georglider.prod.payload.dto.advertiser.campaign.Targeting
import ru.georglider.prod.payload.request.advertiser.campaign.CreateCampaignRequest
import ru.georglider.prod.payload.request.advertiser.campaign.EditCampaignRequest
import ru.georglider.prod.repository.CampaignRepository
import ru.georglider.prod.service.internal.metrics.CampaignMetricService
import java.util.*
import kotlin.test.assertEquals

@Tag("test")
@ExtendWith(SpringExtension::class, MockitoExtension::class)
class ECampaignServiceTests {

    private val timeService = mockk<TimeService>()
    private val metrics = mockk<CampaignMetricService>()

    private val campaignRepository = mockk<CampaignRepository>()
    private val advertiserService = mockk<AdvertiserService>()
    private val moderationService = mockk<ModerationService>()

    private val campaignService = CampaignService(campaignRepository, metrics, moderationService)
    private val campaignsController = CampaignsController(campaignService, advertiserService, timeService)

    @Test
    fun testInvalidDateCampaignCreationExpectBadRequest() {
        val advertiserId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)

        assertThrows<BadRequestException> {
            campaignsController.post(advertiserId, CreateCampaignRequest(
                1000, 200, 3.0, 5.0, "H1", "span",
                19, 22, Targeting()
            )).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignCreationAdvertiseNotFoundExpect404() {
        val advertiserId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(false)

        assertThrows<Exception> {
            campaignsController.post(advertiserId, CreateCampaignRequest(
                1000, 200, 3.0, 5.0, "H1", "span",
                30, 42, Targeting()
            )).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignCreationAdvertiseGood() {
        val advertiserId = UUID.randomUUID()
        every { moderationService.isEnabled() } returns false
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)
        every { campaignRepository.insert(any()) } returns Mono.just(
            GeneratedCampaignValuesHolder(UUID.randomUUID(), 3.0, 5.0, false)
        )

        assertDoesNotThrow {
            campaignsController.post(advertiserId, CreateCampaignRequest(
                1000, 200, 3.0, 5.0, "H1", "span",
                30, 42, Targeting()
            )).block()
        }
        verify { metrics.refreshState() }
    }

    @Test
    fun testCampaignCreationWithNullTargetingExpectBadRequest() {
        val advertiserId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)

        assertThrows<BadRequestException> {
            campaignsController.post(advertiserId, CreateCampaignRequest(
                1000, 200, 3.0, 5.0, "H1", "span",
                19, 22, null
            )).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignCreationWithInvalidDateRangeExpectBadRequest1() {
        val advertiserId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)

        assertThrows<BadRequestException> {
            campaignsController.post(advertiserId, CreateCampaignRequest(
                1000, 200, 3.0, 5.0, "H1", "span",
                23, 22
            )).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignCreationWithInvalidDateRangeExpectBadRequest2() {
        val advertiserId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)

        assertThrows<BadRequestException> {
            campaignsController.post(advertiserId, CreateCampaignRequest(
                1000, 200, 3.0, 5.0, "H1", "span",
                20, 19
            )).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testValidCampaignEditExpectSuccess() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { moderationService.isEnabled() } returns false
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)
        every { campaignRepository.update(Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)) } returns Mono.just(
            GeneratedCampaignValuesHolder(campaignId, 3.0, 5.5, false)
        )

        assertEquals(campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
            1000, 200, 3.0, 5.5, "Hello", "Test",
            18, 22, Targeting("MALE", null, null, null))
        ).block(), Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        ).map { CampaignDTO(it) }.block())
        verify { metrics.refreshState() }
    }

    @Test
    fun testValidCampaignEditAdvertiserNotFoundExpectException() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.empty()
        every { campaignRepository.update(Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)) } returns Mono.just(
            GeneratedCampaignValuesHolder(campaignId, 3.0, 5.5, false)
        )

        assertThrows<Exception> {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1000, 200, 3.0, 5.5, "Hello", "Test",
                18, 22, Targeting("MALE", null, null, null))
            ).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignEditWithInvalidClicksLimitExpectException() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )

        assertThrows<Exception> {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1000, 201, 3.0, 5.5, "Hello", "Test",
                18, 22, Targeting("MALE", null, null, null))
            ).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignEditWithInvalidImpressionsLimitExpectException() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )

        assertThrows<Exception> {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1001, 200, 3.0, 5.5, "Hello", "Test",
                18, 22, Targeting("MALE", null, null, null))
            ).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignEditWithInvalidAgeRangeExpectException1() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )

        assertThrows<Exception> {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1000, 200, 3.0, 5.5, "Hello", "Test",
                19, 22, Targeting("MALE", null, null, null))
            ).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignEditWithInvalidAgeRangeExpectException2() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )

        assertThrows<Exception> {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1000, 200, 3.0, 5.5, "Hello", "Test",
                19, 23, Targeting("MALE", null, null, null))
            ).block()
        }
        verify (exactly = 0) { metrics.refreshState() }
    }

    @Test
    fun testCampaignEditWithValidCPMExpectSuccess() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { moderationService.isEnabled() } returns false
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )
        every { campaignRepository.update(Campaign(advertiserId, 1000, 200, 5.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)) } returns Mono.just(
            GeneratedCampaignValuesHolder(campaignId, 5.0, 5.5, false)
        )

        assertDoesNotThrow {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1000, 200, 5.0, 5.5, "Hello", "Test",
                18, 22, Targeting("MALE", null, null, null))
            ).block()
        }
        verify { metrics.refreshState() }
    }

    @Test
    fun testCampaignEditWithValidCPCExpectSuccess() {
        val advertiserId = UUID.randomUUID()
        val campaignId = UUID.randomUUID()
        every { moderationService.isEnabled() } returns false
        every { metrics.refreshState() } just runs
        every { timeService.get() } returns 20
        every { advertiserService.existsById(advertiserId) } returns Mono.just(true)
        every { campaignService.findByAdvertiserIdAndCampaignId(advertiserId, campaignId) } returns Mono.just(
            Campaign(advertiserId, 1000, 200, 3.0, 5.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)
        )
        every { campaignRepository.update(Campaign(advertiserId, 1000, 200, 3.0, 6.5, "Hello", "Test", 18, 22, Gender.MALE, null, null, null, campaignId)) } returns Mono.just(
            GeneratedCampaignValuesHolder(campaignId, 3.0, 6.5, false)
        )

        assertDoesNotThrow {
            campaignsController.editCampaign(advertiserId, campaignId, EditCampaignRequest(
                1000, 200, 3.0, 6.5, "Hello", "Test",
                18, 22, Targeting("MALE", null, null, null))
            ).block()
        }
        verify { metrics.refreshState() }
    }

}