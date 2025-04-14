package ru.georglider.prod.service

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import ru.georglider.prod.payload.request.time.TimeSetRequest
import java.util.*

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class)
class GStatsServiceIT (
    @LocalServerPort private val port: Int
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }
    
    @Test
    fun campaignDoesNotExistExpect404() {
        When {
            get("/stats/campaigns/${UUID.randomUUID()}")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun campaignIdInvalidExpect400() {
        When {
            get("/stats/campaigns/not-a-uuid")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun advertiserDoesNotExistExpect404() {
        When {
            get("/stats/advertisers/${UUID.randomUUID()}/campaigns")
        } Then {
            log().all()
            statusCode(404)
        }
    }

    @Test
    fun advertiserIdInvalidExpect400() {
        When {
            get("/stats/advertisers/not-a-uuid/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun campaignDoesNotExistDailyExpect404() {
        When {
            get("/stats/campaigns/${UUID.randomUUID()}/daily")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun campaignIdInvalidDailyExpect400() {
        When {
            get("/stats/campaigns/not-a-uuid/daily")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun advertiserDoesNotExistDailyExpect404() {
        When {
            get("/stats/advertisers/${UUID.randomUUID()}/campaigns/daily")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun advertiserIdInvalidDailyExpect400() {
        When {
            get("/stats/advertisers/not-a-uuid/campaigns/daily")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun validAdvertiserId() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(advertiserId, "CampaignStats")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        When {
            get("/stats/advertisers/${advertiserId}/campaigns")
        } Then {
            statusCode(200)
        }

        When {
            get("/stats/advertisers/${advertiserId}/campaigns/daily")
        } Then {
            statusCode(200)
        }
    }

    @Test
    fun validCampaignId() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(advertiserId, "CampaignStats")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        var campaignId: UUID? = null
        // Campaign lasts 5 days (78-82)
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 78, \"end_date\": 82, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            campaignId = UUID.fromString(extract().body().path("campaign_id"))
            statusCode(201)
        }

        // CHANGING TIME -> now we're able to see 3 days (78,79,80)
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(80))
        } When {
            post("/time/advance")
        } Then {
            body("current_date", equalTo(80))
        }

        When {
            get("/stats/advertisers/${advertiserId}/campaigns")
        } Then {
            statusCode(200)
        }

        When {
            get("/stats/advertisers/${advertiserId}/campaigns/daily")
        } Then {
            body("size()", equalTo(3))
            statusCode(200)
        }

        When {
            get("/stats/campaigns/${campaignId}")
        } Then {
            statusCode(200)
        }

        When {
            get("/stats/campaigns/${campaignId}/daily")
        } Then {
            body("size()", equalTo(3))
            statusCode(200)
        }


        // CHANGING TIME -> now we're able to see all 5 days
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(82))
        } When {
            post("/time/advance")
        } Then {
            body("current_date", equalTo(82))
        }

        When {
            get("/stats/advertisers/${advertiserId}/campaigns")
        } Then {
            statusCode(200)
        }

        When {
            get("/stats/advertisers/${advertiserId}/campaigns/daily")
        } Then {
            body("size()", equalTo(5))
            statusCode(200)
        }

        When {
            get("/stats/campaigns/${campaignId}")
        } Then {
            statusCode(200)
        }

        When {
            get("/stats/campaigns/${campaignId}/daily")
        } Then {
            body("size()", equalTo(5))
            statusCode(200)
        }
    }
    
    
}