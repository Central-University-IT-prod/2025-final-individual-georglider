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
import ru.georglider.prod.payload.dto.advertiser.campaign.Targeting
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import ru.georglider.prod.payload.request.advertiser.campaign.CreateCampaignRequest
import java.util.*

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class)
class ECampaignServiceIT (
    @LocalServerPort private val port: Int
) {

    val ids = arrayOfNulls<UUID>(2)

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"

        if (ids[0] == null) {
            Given {
                contentType(ContentType.JSON)
                body(listOf(BulkEntity(UUID.randomUUID(), "DEBUG ADVERTISER")))
            } When {
                post("/advertisers/bulk")
            } Then {
                statusCode(201)
                ids[0] = UUID.fromString(extract().body().path("[0].advertiser_id"))
            }
        }
    }

    @Test
    fun createCampaignExpect404() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${UUID.randomUUID()}/campaigns")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun createCampaignExpect201() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun testTooLongLocationExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"${"L".repeat(256)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testInvalidAgeRangeExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 10, \"age_to\": 9, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testNegativeCostPerImpressionExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": -1.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testNegativeCostPerClickExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 2.0,\"cost_per_click\": -3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testInvalidCostPerImpressionExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(CreateCampaignRequest(51, 50, 0.2, -123.30,
                "Бесплатная доставка в Google еде!",
                "Попробуйте заказать доставку от Google еды прямо сейчас",
                20, 30, Targeting("ALL", 19, null, "London")
            ))
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testEmptyTitleExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(CreateCampaignRequest(51, 50, 0.2, 123.30,
                "",
                "Попробуйте заказать доставку от Google еды прямо сейчас",
                20, 30, Targeting("ALL", 19, null, "London")
            ))
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testTooLongTitleExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"D".repeat(256)}\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testNegativeLimitsExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": -3000,\"clicks_limit\": -2000,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testNegativeClicksLimitExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 23,\"clicks_limit\": -2000,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testNegativeImpressionsLimitExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": -23,\"clicks_limit\": 2000,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun testGoodCampaignCreationExpect201() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun testLongAdTitleAndTextExpect201() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(255)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun testSmallImpressionLimitExpect201() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(255)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun testCampaignChangeExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(255)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            post("/advertisers/${ids[0]}/campaigns")
        } Then {
            statusCode(201)
            ids[1] = UUID.fromString(extract().body().path("campaign_id"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(256)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            put("/advertisers/${ids[0]}/campaigns/${ids[1]}")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${""}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            put("/advertisers/${ids[0]}/campaigns/${ids[1]}")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(255)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            put("/advertisers/${ids[0]}/campaigns/${ids[1]}")
        } Then {
            statusCode(200)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": -50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(255)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            put("/advertisers/${ids[0]}/campaigns/${ids[1]}")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": -50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"${"L".repeat(255)}\",\"ad_text\": \"${"L".repeat(1024)}\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"${"L".repeat(255)}\" }}")
        } When {
            put("/advertisers/${ids[0]}/campaigns/${ids[1]}")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun campaignCreationDeletionTests() {
        val advertiserId = UUID.randomUUID()
        val adIds = arrayOfNulls<String>(3)

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(advertiserId, "DEBUG ADVERTISER 2")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"AD1\",\"ad_text\": \"The best 1st ad in the world!\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": 123, \"location\": \"Saint-Petersburg\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
            adIds[0] = extract().body().path("campaign_id")
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"AD2\",\"ad_text\": \"The best 2nd ad in the world!\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"MALE\", \"age_from\": 13, \"age_to\": 123, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
            log().all()
            adIds[1] = extract().body().path("campaign_id")
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"AD3\",\"ad_text\": \"The best 3rd ad in the world!\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"FEMALE\", \"age_from\": 13, \"age_to\": 123, \"location\": \"Vologda\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
            adIds[2] = extract().body().path("campaign_id")
        }

        When {
            get("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(200)
            body("size()", equalTo(3))
        }

        When {
            get("/advertisers/$advertiserId/campaigns?page=2&size=2")
        } Then {
            statusCode(200)
            body("size()", equalTo(1))
        }

        When {
            delete("/advertisers/${UUID.randomUUID()}/campaigns/${adIds[2]}")
        } Then {
            statusCode(404)
        }

        When {
            delete("/advertisers/$advertiserId/campaigns/${adIds[2]}")
        } Then {
            statusCode(204)
        }


        When {
            delete("/advertisers/$advertiserId/campaigns/${adIds[2]}")
        } Then {
            statusCode(404)
        }

        When {
            get("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(200)
            body("size()", equalTo(2))
        }

        When {
            get("/advertisers/$advertiserId/campaigns?page=2&size=2")
        } Then {
            statusCode(200)
            body("size()", equalTo(0))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"AD2\",\"ad_text\": \"The best 2nd ad in the world!\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"MALE\", \"age_from\": 13, \"age_to\": 18, \"location\": \"Vologda\" }}")
        } When {
            put("/advertisers/$advertiserId/campaigns/${adIds[1]}")
        } Then {
            statusCode(200)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"AD2\",\"ad_text\": \"The best 2nd ad in the world!\", \"start_date\": 45, \"end_date\": 30, \"targeting\": {\"gender\": \"MALE\", \"age_from\": 13, \"age_to\": 18, \"location\": \"Vologda\" }}")
        } When {
            put("/advertisers/$advertiserId/campaigns/${adIds[1]}")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"AD2\",\"ad_text\": \"The best 2nd ad in the world!\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"MALE\", \"age_from\": 13, \"age_to\": 18, \"location\": \"Vologda\" }}")
        } When {
            put("/advertisers/${UUID.randomUUID()}/campaigns/${adIds[1]}")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun testSearchCampaignsDoesNotExistExpect404() {
        When {
            get("/advertisers/${UUID.randomUUID()}/campaigns")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun testSearchSpecificCampaignDoesNotExistExpect404() {
        When {
            get("/advertisers/${UUID.randomUUID()}/campaigns/${UUID.randomUUID()}")
        } Then {
            statusCode(404)
        }
    }

}