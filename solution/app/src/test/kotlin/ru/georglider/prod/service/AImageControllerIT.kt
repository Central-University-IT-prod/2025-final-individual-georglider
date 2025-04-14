package ru.georglider.prod.service

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import ru.georglider.prod.utils.extensions.S3SetupExtension
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import ru.georglider.prod.utils.extensions.RedisSetupExtension
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.WritableRaster
import java.util.*


@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(S3SetupExtension::class, DatabaseSetupExtension::class, RedisSetupExtension::class)
class AImageControllerIT (
    @LocalServerPort private val port: Int
) {

    companion object {
        private val image: BufferedImage = BufferedImage(100, 100, BufferedImage.TYPE_BYTE_GRAY)
        private val raster: WritableRaster = image.raster
        private val buffer: DataBufferByte = raster.dataBuffer as DataBufferByte
        val data: ByteArray = buffer.data
    }

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun uploadRandomAdvertiserExpect404() {
        Given {
            contentType("image/png")
            body(data)
        } When {
            post("/advertisers/${UUID.randomUUID()}/campaigns/${UUID.randomUUID()}/image")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun uploadValidAdvertiserRandomCampaignExpect404() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(advertiserId, "FEMALE")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType("image/png")
            body(data)
        } When {
            post("/advertisers/${advertiserId}/campaigns/${UUID.randomUUID()}/image")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun uploadValidAdvertiserValidCampaignExpect201() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(advertiserId, "FEMALE")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        var campaignId: UUID? = null
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${advertiserId}/campaigns")
        } Then {
            campaignId = UUID.fromString(extract().body().path("campaign_id"))
            statusCode(201)
        }

        Given {
            contentType("image/png")
            body(data)
        } When {
            post("/advertisers/${advertiserId}/campaigns/${campaignId}/image")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun requestInvalidAdvertisementImageExpect400() {
        When { get("/ads/not-a-uuid/image") } Then {
            statusCode(400)
        }
    }

    @Test
    fun requestNotExistentAdvertisementImageExpect400() {
        When { get("/ads/${UUID.randomUUID()}/image") } Then {
            statusCode(404)
        }
    }

    @Test
    fun uploadInvalidType() {
        Given {
            contentType("video/mp4")
            body(data)
        } When {
            post("/advertisers/${UUID.randomUUID()}/campaigns/${UUID.randomUUID()}/image")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun advertisementImageShowDeleteMethods() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(advertiserId, "FEMALE")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        var campaignId: UUID? = null
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${advertiserId}/campaigns")
        } Then {
            campaignId = UUID.fromString(extract().body().path("campaign_id"))
            statusCode(201)
        }

        When { get("/ads/$campaignId/image") } Then {
            statusCode(404)
        }

        Given {
            contentType("image/png")
            body(data)
        } When {
            post("/advertisers/${advertiserId}/campaigns/${campaignId}/image")
        } Then {
            statusCode(201)
        }
        Given {
            contentType("video/mp4")
            body(data)
        } When {
            post("/advertisers/${advertiserId}/campaigns/${campaignId}/image")
        } Then {
            statusCode(400)
        }
        
        When { get("/ads/$campaignId/image") } Then {
            statusCode(200)
        }
        
        When {
            delete("/advertisers/${advertiserId}/campaigns/${campaignId}/image")
        } Then {
            statusCode(204)
        }

        When { get("/ads/$campaignId/image") } Then {
            statusCode(404)
        }
    }

    @Test
    fun advertisementImageDeleteNonExistentCampaignExpect404() {
        When {
            delete("/advertisers/${UUID.randomUUID()}/campaigns/${UUID.randomUUID()}/image")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun advertisementImageDeleteInvalidCampaignIdExpect400() {
        When {
            delete("/advertisers/${UUID.randomUUID()}/campaigns/not-a-uuid/image")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun advertisementImageDeleteInvalidAdvertiserIdExpect400() {
        When {
            delete("/advertisers/not-a-uuid/campaigns/${UUID.randomUUID()}/image")
        } Then {
            statusCode(400)
        }
    }

}