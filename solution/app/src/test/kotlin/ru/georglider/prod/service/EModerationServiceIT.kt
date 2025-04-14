package ru.georglider.prod.service

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import ru.georglider.prod.payload.request.advertiser.BulkEntity
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.*
import ru.georglider.prod.payload.request.moderator.DecisionRequest
import java.util.*
import kotlin.test.assertEquals

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(DatabaseSetupExtension::class)
class EModerationServiceIT (
    @LocalServerPort private val port: Int
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    @Order(1)
    fun moderationStatusChangeTest() {
        When { post("/moderation/enable") } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        When { get("/moderation/status") } Then {
            body(equalTo("Moderation enabled!"))
        }

        When { post("/moderation/disable") } Then {
            body(equalTo("Successfully disabled moderation!"))
        }

        When { get("/moderation/status") } Then {
            body(equalTo("Moderation is currently disabled!"))
        }
    }

    @Test
    @Order(2)
    fun moderationApproveWrongBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{ \"campaign_id\": \"not-a-uuid\" }")
        } When { post("/moderation/approve") } Then {
            statusCode(400)
        }
    }

    @Test
    @Order(3)
    fun moderationApproveNotFoundExpect404() {
        Given {
            contentType(ContentType.JSON)
            body("{ \"campaign_id\": \"${UUID.randomUUID()}\" }")
        } When { post("/moderation/approve") } Then {
            statusCode(404)
        }
    }

    @Test
    @Order(4)
    fun moderationRejectWrongBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{ \"campaign_id\": \"not-a-uuid\" }")
        } When { post("/moderation/reject") } Then {
            statusCode(400)
        }
    }

    @Test
    @Order(5)
    fun moderationRejectNotFoundExpect404() {
        Given {
            contentType(ContentType.JSON)
            body("{ \"campaign_id\": \"${UUID.randomUUID()}\" }")
        } When { post("/moderation/reject") } Then {
            statusCode(404)
        }
    }


    @Test
    @Order(6)
    fun createApprovedCampaignTestWithFilterScenery() {
        val advertiserId = UUID.randomUUID()
        createAdvertiser(advertiserId)

        When {
            post("/moderation/enable")
        } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"Бесплатная доставка в Google еде!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(0))
        }

        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }
        var generatedId: UUID? = null
        When { get("/moderation/request") } Then {
            generatedId = UUID.fromString(extract().body().path("campaign_id"))
            body("ad_title", equalTo("Бесплатная доставка в Google еде!"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(generatedId!!))
        } When { post("/moderation/approve") } Then {
            body(equalTo("Successfully approved ${generatedId}!"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(generatedId!!))
        } When { post("/moderation/approve") } Then {
            statusCode(404)
        }

        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(1))
        }
    }

    @Test
    @Order(7)
    fun createRejectedCampaignTestWithFilterScenery() {
        val advertiserId = UUID.randomUUID()
        createAdvertiser(advertiserId)

        When {
            post("/moderation/enable")
        } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"BAD WORD!\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
        }

        var generatedId: UUID? = null
        When { get("/moderation/request") } Then {
            generatedId = UUID.fromString(extract().body().path("campaign_id"))
            body("ad_title", equalTo("BAD WORD!"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(generatedId!!))
        } When { post("/moderation/reject") } Then {
            statusCode(200)
            body(equalTo("Successfully rejected ${generatedId}!"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(generatedId!!))
        } When { post("/moderation/reject") } Then {
            statusCode(404)
        }

        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(0))
        }
    }


    @Test
    @Order(8)
    fun createCampaignAndRejectChangesTestWithFilterScenery() {
        val advertiserId = UUID.randomUUID()
        createAdvertiser(advertiserId)

        When {
            post("/moderation/enable")
        } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"GOOD WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(0))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }

        var moderationId: UUID? = null
        When { get("/moderation/request") } Then {
            moderationId = UUID.fromString(extract().body().path("campaign_id"))
            body("ad_title", equalTo("GOOD WORD"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(moderationId!!))
        } When { post("/moderation/approve") } Then {
            body(equalTo("Successfully approved ${moderationId}!"))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        var campaignId: UUID? = null

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            campaignId = UUID.fromString(extract().body().path("[0].campaign_id"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"BAD WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 21, \"end_date\": 31, \"targeting\": {\"gender\": \"ALL\", \"age_to\": 123 }}")
        } When {
            put("/advertisers/$advertiserId/campaigns/$campaignId")
        } Then {
            body("start_date", equalTo(21))
            // Сразу показываем, что текст не было изменен
            body("ad_title", equalTo("GOOD WORD"))
            statusCode(200)
        }

        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }
        // Проверяем кампанию, её текст не должен был смениться
        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            body("ad_title", equalTo("GOOD WORD"))
        }

        // Не позволяем изменения названия
        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(campaignId!!))
        } When { post("/moderation/reject") } Then {
            statusCode(200)
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            body("ad_title", equalTo("GOOD WORD"))
        }
    }

    @Test
    @Order(9)
    fun createCampaignAndApproveChangesTestWithFilterScenery() {
        val advertiserId = UUID.randomUUID()
        createAdvertiser(advertiserId)

        When {
            post("/moderation/enable")
        } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"GOOD WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(0))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }

        var moderationId: UUID? = null
        When { get("/moderation/request") } Then {
            moderationId = UUID.fromString(extract().body().path("campaign_id"))
            body("ad_title", equalTo("GOOD WORD"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(moderationId!!))
        } When { post("/moderation/approve") } Then {
            body(equalTo("Successfully approved ${moderationId}!"))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        var campaignId: UUID? = null

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            campaignId = UUID.fromString(extract().body().path("[0].campaign_id"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"OK WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 21, \"end_date\": 31, \"targeting\": {\"gender\": \"ALL\", \"age_to\": 123 }}")
        } When {
            put("/advertisers/$advertiserId/campaigns/$campaignId")
        } Then {
            body("start_date", equalTo(21))
            // Сразу показываем, что текст не было изменен
            body("ad_title", equalTo("GOOD WORD"))
            statusCode(200)
        }

        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }
        // Проверяем кампанию, её текст не должен был смениться
        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            body("ad_title", equalTo("GOOD WORD"))
        }

        // Позволяем изменения названия
        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(campaignId!!))
        } When { post("/moderation/approve") } Then {
            statusCode(200)
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            body("ad_title", equalTo("OK WORD"))
        }
    }

    @Test
    @Order(10)
    fun createCampaignAndApproveChangesWhileItIsDeletedTestWithFilterScenery() {
        val advertiserId = UUID.randomUUID()
        createAdvertiser(advertiserId)

        When {
            post("/moderation/enable")
        } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"PREVIOUS WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(0))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }

        var moderationId: UUID? = null
        When { get("/moderation/request") } Then {
            moderationId = UUID.fromString(extract().body().path("campaign_id"))
            body("ad_title", equalTo("PREVIOUS WORD"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(moderationId!!))
        } When { post("/moderation/approve") } Then {
            body(equalTo("Successfully approved ${moderationId}!"))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        var campaignId: UUID? = null

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            campaignId = UUID.fromString(extract().body().path("[0].campaign_id"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"OK WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 21, \"end_date\": 31, \"targeting\": {\"gender\": \"ALL\", \"age_to\": 123 }}")
        } When {
            put("/advertisers/$advertiserId/campaigns/$campaignId")
        } Then {
            body("start_date", equalTo(21))
            // Сразу показываем, что текст не было изменен
            body("ad_title", equalTo("PREVIOUS WORD"))
            statusCode(200)
        }

        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }
        // Проверяем кампанию, её текст не должен был смениться
        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            body("ad_title", equalTo("PREVIOUS WORD"))
        }
        // Удаляем кампанию
        When { delete("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            statusCode(204)
        }
        // Кампания должна удалить запрос о модерации
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        // Подтверждаем изменения (но объекта больше нет => 404)
        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(campaignId!!))
        } When { post("/moderation/approve") } Then {
            statusCode(404)
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            statusCode(404)
        }
    }

    @Test
    @Order(10)
    fun createCampaignAndNoTextChangeTestWithFilterScenery() {
        val advertiserId = UUID.randomUUID()
        createAdvertiser(advertiserId)

        When {
            post("/moderation/enable")
        } Then {
            body(equalTo("Successfully enabled moderation!"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 1000,\"clicks_limit\": 50,\"cost_per_impression\": 0.2,\"cost_per_click\": 3.5,\"ad_title\": \"PREVIOUS WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 20, \"end_date\": 30, \"targeting\": {\"gender\": \"ALL\", \"age_from\": 13, \"age_to\": null, \"location\": \"Moscow\" }}")
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            statusCode(201)
        }

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            body("size()", equalTo(0))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(1))
        }

        var moderationId: UUID? = null
        When { get("/moderation/request") } Then {
            moderationId = UUID.fromString(extract().body().path("campaign_id"))
            body("ad_title", equalTo("PREVIOUS WORD"))
        }

        Given {
            contentType(ContentType.JSON)
            body(DecisionRequest(moderationId!!))
        } When { post("/moderation/approve") } Then {
            body(equalTo("Successfully approved ${moderationId}!"))
        }
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }

        var campaignId: UUID? = null

        When { get("/advertisers/$advertiserId/campaigns") } Then {
            campaignId = UUID.fromString(extract().body().path("[0].campaign_id"))
        }

        Given {
            contentType(ContentType.JSON)
            body("{\"impressions_limit\": 50,\"clicks_limit\": 50,\"cost_per_impression\": 1.2,\"cost_per_click\": 3.5,\"ad_title\": \"PREVIOUS WORD\",\"ad_text\": \"Попробуйте заказать доставку от Google еды прямо сейчас\", \"start_date\": 21, \"end_date\": 31, \"targeting\": {\"gender\": \"ALL\", \"age_to\": 123 }}")
        } When {
            put("/advertisers/$advertiserId/campaigns/$campaignId")
        } Then {
            body("start_date", equalTo(21))
            // Сразу показываем, что текст не было изменен
            body("ad_title", equalTo("PREVIOUS WORD"))
            statusCode(200)
        }

        // Не должно было появиться нового запроса(т.к. текстовые поля не менялись)
        When { get("/moderation/requests") } Then {
            body("size()", equalTo(0))
        }
        // Проверяем кампанию, её текст не должен был смениться
        When { get("/advertisers/$advertiserId/campaigns/$campaignId") } Then {
            body("start_date", equalTo(21))
            body("ad_title", equalTo("PREVIOUS WORD"))
        }

        assertEquals(campaignId, moderationId)
    }


    @AfterEach
    fun after() {
        When {
            post("/moderation/disable")
        } Then {
            statusCode(200)
        }
    }


    private fun createAdvertiser(id: UUID) {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(id, "DEBUG ADVERTISER")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }
    }

}