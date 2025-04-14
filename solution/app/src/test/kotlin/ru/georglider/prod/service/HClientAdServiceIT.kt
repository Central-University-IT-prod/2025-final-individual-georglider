package ru.georglider.prod.service

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import ru.georglider.prod.payload.dto.advertiser.campaign.Targeting
import ru.georglider.prod.payload.request.advertiser.MLScoreRequest
import ru.georglider.prod.payload.request.advertiser.campaign.CampaignRequest
import ru.georglider.prod.payload.request.advertiser.campaign.CreateCampaignRequest
import ru.georglider.prod.payload.request.advertiser.campaign.EditCampaignRequest
import ru.georglider.prod.payload.request.client.AdRequest
import ru.georglider.prod.payload.request.time.TimeSetRequest
import ru.georglider.prod.utils.extensions.RedisSetupExtension
import java.util.*
import kotlin.test.assertEquals
import ru.georglider.prod.payload.request.advertiser.BulkEntity as AdBulkEntity
import ru.georglider.prod.payload.request.client.BulkEntity as ClientBulkEntity

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class, RedisSetupExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class HClientAdServiceIT (
    @LocalServerPort private val port: Int
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    @Order(1)
    fun targetingTest() {
        val advertiserId = UUID.randomUUID()

        Given {
            contentType(ContentType.JSON)
            body(listOf(AdBulkEntity(advertiserId, "Перекрёсток")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(90))
        } When { post("/time/advance") } Then {
            log().all()
            statusCode(200)
        }

        val campaignRequests = listOf(
            CreateCampaignRequest(50, 20, 5.0, 10.0, "Коляски для детей", "Коляски для детей", 90, 92, Targeting("FEMALE", 20, 40, null)),
            CreateCampaignRequest(50, 20, 2.0, 10.0, "Мыльные пузыри", "Мыльные пузыри", 90, 92, Targeting(null, null, 12, null)),
        )
        postCampaignRequests(campaignRequests, advertiserId)

        val userIds = arrayOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

        Given {
            contentType(ContentType.JSON)
            body(listOf(
                ClientBulkEntity(userIds[0], "Child", 8, "Moscow", "MALE"),
                ClientBulkEntity(userIds[1], "Mother", 30, "Saint-Petersburg", "FEMALE"),
                ClientBulkEntity(userIds[2], "Father", 30, "Kazan", "MALE")
            ))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }

        When { get("/ads?client_id=${userIds[0]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("Мыльные пузыри"))
        }
        When { get("/ads?client_id=${userIds[0]}") }.Then {
            body("ad_title", equalTo("Мыльные пузыри"))
        }

        When { get("/ads?client_id=${userIds[1]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("Коляски для детей"))
        }
        When { get("/ads?client_id=${userIds[1]}") }.Then {
            body("ad_title", equalTo("Коляски для детей"))
        }

        When { get("/ads?client_id=${userIds[2]}") }.Then {
            statusCode(404)
        }
    }

    @Test
    @Order(2)
    fun mlModelImportance() {
        val advertiserIds = arrayOf(UUID.randomUUID(), UUID.randomUUID())
        val userIds = arrayOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

        Given {
            contentType(ContentType.JSON)
            body(listOf(
                AdBulkEntity(advertiserIds[0], "Перекрёсток"),
                AdBulkEntity(advertiserIds[1], "Ашан"),
            ))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(94))
        } When { post("/time/advance") } Then {
            statusCode(200)
        }

        val campaignRequests = listOf(
            CreateCampaignRequest(50, 20, 8.0, 10.0, "8 ОТ ПЕРЕКРЁСТКА", "8 ОТ ПЕРЕКРЁСТКА", 94, 96, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "3 ОТ ПЕРЕКРЁСТКА", "3 ОТ ПЕРЕКРЁСТКА", 94, 96, Targeting(null, null, null, null)),
        )
        postCampaignRequests(campaignRequests, advertiserIds[0])
        postCampaignRequests(listOf(
            CreateCampaignRequest(50, 20, 5.0, 10.0, "АШАН 20%", "АШАН", 94, 96, Targeting(null, null, null, null)),
        ), advertiserIds[1])

        Given {
            contentType(ContentType.JSON)
            body(listOf(
                ClientBulkEntity(userIds[0], "Child", 8, "Moscow", "MALE"),
                ClientBulkEntity(userIds[1], "Mother", 30, "Saint-Petersburg", "FEMALE"),
                ClientBulkEntity(userIds[2], "Father", 30, "Kazan", "MALE")
            ))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }

        When { get("/ads?client_id=${userIds[0]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("8 ОТ ПЕРЕКРЁСТКА"))
        }
        When { get("/ads?client_id=${userIds[0]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("АШАН 20%"))
        }
        When { get("/ads?client_id=${userIds[0]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("3 ОТ ПЕРЕКРЁСТКА"))
        }

        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(userIds[1], advertiserIds[1], 15))
        } When { post("/ml-scores") } Then {
            statusCode(200)
        }
        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(userIds[2], advertiserIds[0], 20))
        } When { post("/ml-scores") } Then {
            statusCode(200)
        }

        When { get("/ads?client_id=${userIds[1]}") }.Then {
            log().all()
            statusCode(200)
            body("ad_title", equalTo("АШАН 20%"))
        }
        When { get("/ads?client_id=${userIds[2]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("8 ОТ ПЕРЕКРЁСТКА"))
        }

    }

    @Test
    @Order(0)
    fun clickInvalidAdIdExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(UUID.randomUUID()))
        } When {
            post("/ads/not-a-uuid/click")
        } Then {
            statusCode(400)
        }
    }

    @Test
    @Order(0)
    fun clickNonExistentAdIdExpect404() {
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(UUID.randomUUID()))
        } When {
            post("/ads/${UUID.randomUUID()}/click")
        } Then {
            statusCode(404)
        }
    }

    @Test
    @Order(0)
    fun clickNonExistentAdIdNoBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{}")
        } When {
            post("/ads/${UUID.randomUUID()}/click")
        } Then {
            statusCode(400)
        }
    }

    @Test
    @Order(0)
    fun clickNonExistentAdIdInvalidBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{ \"client_id\": \"not-a-uuid\" }")
        } When {
            post("/ads/${UUID.randomUUID()}/click")
        } Then {
            log().all()
            statusCode(400)
        }
    }

    @Test
    @Order(3)
    fun clickExistentAdTests() {
        val advertiserId = UUID.randomUUID()

        Given {
            contentType(ContentType.JSON)
            body(listOf(AdBulkEntity(advertiserId, "Перекрёсток")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        var adId: UUID? = null
        Given {
            contentType(ContentType.JSON)
            body(CreateCampaignRequest(50, 20, 5.0, 10.0, "Упаковка для подарков", "Упаковка для подарка со скидкой 20%", 96, 97, Targeting(null, null, null, "TheNorthPole")))
        } When {
            post("/advertisers/$advertiserId/campaigns")
        } Then {
            adId = UUID.fromString(extract().body().path("campaign_id"))
            statusCode(201)
        }

        // Несуществующий пользователь
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(UUID.randomUUID()))
        } When {
            post("/ads/${adId}/click")
        } Then {
            statusCode(403)
        }

        val userIds = arrayOf(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())
        Given {
            contentType(ContentType.JSON)
            body(listOf(
                ClientBulkEntity(userIds[0], "Ded Moroz", 90, "TheNorthPole", "MALE"),
                ClientBulkEntity(userIds[1], "Snegurochka", 24, "TheNorthPole", "FEMALE"),
                ClientBulkEntity(userIds[2], "Elf", 18, "TheNorthPole", "MALE"),
            ))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }

        // Пользователь ещё не смотрел рекламу
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(userIds[0]))
        } When {
            post("/ads/${adId}/click")
        } Then {
            statusCode(403)
        }

        // Меняем дату для того, чтобы существовала только одна реклама
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(97))
        } When { post("/time/advance") } Then {
            statusCode(200)
        }

        // Дед мороз смотрит рекламу
        When { get("/ads?client_id=${userIds[0]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("Упаковка для подарков"))
        }

        // Дед мороз переходит по рекламе
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(userIds[0]))
        } When {
            post("/ads/${adId}/click")
        } Then {
            statusCode(204)
        }

        // Меняем прайс (Дед Мороз вытратил 15 рублей (5 на просмотр и 10 на клик)
        Given {
            contentType(ContentType.JSON)
            body(EditCampaignRequest(50, 20, 7.0, 20.0, "Упаковка для подарков", "Упаковка для подарка со скидкой 20%", 96, 97, Targeting(null, null, null, "TheNorthPole")))
        } When {
            put("/advertisers/$advertiserId/campaigns/$adId")
        } Then {
            adId = UUID.fromString(extract().body().path("campaign_id"))
            statusCode(200)
        }

        // Снегурочка смотрит рекламу (7 рублей)
        When { get("/ads?client_id=${userIds[1]}") }.Then {
            statusCode(200)
            body("ad_title", equalTo("Упаковка для подарков"))
        }

        // Пока снегурочка сидела в браузере, наступил следующий день
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(98))
        } When { post("/time/advance") } Then {
            statusCode(200)
        }

        // Снегурочка пытается зайти на рекламу (20 рублей)
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(userIds[1]))
        } When {
            post("/ads/${adId}/click")
        } Then {
            statusCode(204)
        }

        // Эльф смотрит рекламу
        When { get("/ads?client_id=${userIds[2]}") }.Then {
            statusCode(404)
        }

        // Эльф пытается зайти на рекламу
        Given {
            contentType(ContentType.JSON)
            body(AdRequest(userIds[2]))
        } When {
            post("/ads/${adId}/click")
        } Then {
            statusCode(403)
        }

        // Смотрим статистику рекламы
        When { get("/stats/campaigns/$adId") }.Then {
            body("impressions_count", equalTo(2))
            body("clicks_count", equalTo(2))
            body("spent_impressions", equalTo((5+7).toFloat()))
            body("spent_clicks", equalTo((10+20).toFloat()))
            body("conversion", equalTo(100.0F))
            body("spent_total", equalTo((10+20+5+7).toFloat()))
            statusCode(200)
        }

        When { get("/stats/advertisers/$advertiserId/campaigns") }.Then {
            body("impressions_count", equalTo(2))
            body("clicks_count", equalTo(2))
            body("spent_impressions", equalTo((5+7).toFloat()))
            body("spent_clicks", equalTo((10+20).toFloat()))
            body("conversion", equalTo(100.0F))
            body("spent_total", equalTo((10+20+5+7).toFloat()))
            statusCode(200)
        }

        When { get("/stats/campaigns/$adId/daily") }.Then {
            log().all()
            // День 97 - второй, так как кампания началась 96 днём
            body("[1].impressions_count", equalTo(2))
            body("[1].clicks_count", equalTo(1))
            body("[1].spent_impressions", equalTo((5+7).toFloat()))
            body("[1].spent_clicks", equalTo(10.0F))
            body("[1].conversion", equalTo(50.0F))
            body("[1].spent_total", equalTo((10+5+7).toFloat()))

            body("[2].impressions_count", equalTo(0))
            body("[2].clicks_count", equalTo(1))
            body("[2].spent_impressions", equalTo(0.0F))
            body("[2].spent_clicks", equalTo(20.0F))
            body("[2].conversion", equalTo(0.0F))
            body("[2].spent_total", equalTo(20.0F))
            statusCode(200)
        }

        When { get("/stats/advertisers/$advertiserId/campaigns/daily") }.Then {
            body("[1].impressions_count", equalTo(2))
            body("[1].clicks_count", equalTo(1))
            body("[1].spent_impressions", equalTo((5+7).toFloat()))
            body("[1].spent_clicks", equalTo(10.0F))
            body("[1].conversion", equalTo(50.0F))
            body("[1].spent_total", equalTo((10+5+7).toFloat()))

            body("[2].impressions_count", equalTo(0))
            body("[2].clicks_count", equalTo(1))
            body("[2].spent_impressions", equalTo(0.0F))
            body("[2].spent_clicks", equalTo(20.0F))
            body("[2].conversion", equalTo(0.0F))
            body("[2].spent_total", equalTo(20.0F))
            statusCode(200)
        }
    }
    
    @Test
    @Order(4)
    fun fulfillmentAmtTest() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(98))
        } When { post("/time/advance") } Then {
            statusCode(200)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(AdBulkEntity(advertiserId, "Перекрёсток")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        val campaignRequests = listOf(
            CreateCampaignRequest(5, 5, 800.0, 10.0, "GOLD", "8 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "SILVER#1", "3 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "SILVER#2", "3 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "SILVER#3", "3 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "SILVER#4", "3 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "SILVER#5", "3 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
            CreateCampaignRequest(50, 20, 3.0, 10.0, "SILVER#6", "3 ОТ ПЕРЕКРЁСТКА", 98, 98, Targeting(null, null, null, null)),
        )
        postCampaignRequests(campaignRequests, advertiserId)

        val userIds = arrayOfNulls<UUID>(80)
        val userList = mutableListOf<ClientBulkEntity>()
        for (i in 0..79) {
            userIds[i] = UUID.randomUUID()
            userList.add(i, ClientBulkEntity(userIds[i], "USER$i", 20, "Arkhangelsk", "MALE"))
        }

        Given {
            contentType(ContentType.JSON)
            body(userList)
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }

        val shownMap = hashMapOf<String, Int>()
        for (userId in userIds) {
            When { get("/ads?client_id=${userId}") }.Then {
                val title = extract().body().path<String>("ad_title") ?: "null"
                shownMap[title] = shownMap.getOrDefault(title, 0) + 1
            }
        }

        assertEquals(80, shownMap.values.reduce { acc, i -> acc + i })
    }

    private fun postCampaignRequests(requests: List<CampaignRequest>, advertiserId: UUID) {
        for (request in requests) {
            Given {
                contentType(ContentType.JSON)
                body(request)
            } When {
                post("/advertisers/$advertiserId/campaigns")
            } Then {
                statusCode(201)
            }
        }
    }

//    @Test
//    fun revenueTestOneAdvertiser(): Unit {
//        val advertiserId = UUID.randomUUID()
//
//        Given {
//            contentType(ContentType.JSON)
//            body(listOf(AdBulkEntity(advertiserId, "VK")))
//        } When {
//            post("/advertisers/bulk")
//        } Then {
//            statusCode(201)
//        }
//
//        val campaignRequests = listOf(
//            CreateCampaignRequest(50, 20, 2.0, 8.0, "VK 50:20:2:8", "", 0, 100, Targeting()),
//            CreateCampaignRequest(45, 25, 11.2, 9.5, "VK 45, 25, 11.2, 9.5", "", 0, 100, Targeting()),
//            CreateCampaignRequest(65, 35, 10.8, 8.2, "VK 65, 35, 10.8, 8.2", "", 0, 100, Targeting()),
//            CreateCampaignRequest(110, 55, 8.7, 6.5, "VK 110, 55, 8.7, 6.5", "", 0, 100, Targeting()),
//            CreateCampaignRequest(140, 60, 9.3, 7.1, "VK 140, 60, 9.3, 7.1", "", 0, 100, Targeting()),
//            CreateCampaignRequest(90, 40, 10.1, 7.9, "VK 90, 40, 10.1, 7.9", "", 0, 100, Targeting()),
//            CreateCampaignRequest(75, 50, 7.5, 5.0, "VK 75, 50, 7.5, 5.0", "", 0, 100, Targeting()),
//            CreateCampaignRequest(95, 65, 6.5, 5.3, "VK 95, 65, 6.5, 5.3", "", 0, 100, Targeting()),
//            CreateCampaignRequest(170, 80, 5.4, 6.0, "VK 170, 80, 5.4, 6.0", "", 0, 100, Targeting()),
//            CreateCampaignRequest(240, 100, 5.9, 5.6, "VK 240, 100, 5.9, 5.6", "", 0, 100, Targeting()),
//            CreateCampaignRequest(220, 95, 4.8, 5.1, "VK 220, 95, 4.8, 5.1", "", 0, 100, Targeting()),
//            CreateCampaignRequest(260, 120, 4.3, 4.6, "VK 260, 120, 4.3, 4.6", "", 0, 100, Targeting()),
//            CreateCampaignRequest(150, 75, 6.1, 5.7, "VK 150, 75, 6.1, 5.7", "", 0, 100, Targeting()),
//            CreateCampaignRequest(180, 90, 5.0, 4.9, "VK 180, 90, 5.0, 4.9", "", 0, 100, Targeting()),
//            CreateCampaignRequest(200, 110, 4.6, 4.3, "VK 200, 110, 4.6, 4.3", "", 0, 100, Targeting()),
//            CreateCampaignRequest(130, 70, 6.8, 5.2, "VK 130, 70, 6.8, 5.2", "", 0, 100, Targeting()),
//            CreateCampaignRequest(300, 140, 3.5, 3.5, "VK 300, 140, 3.5, 3.5", "", 0, 100, Targeting()),
//            CreateCampaignRequest(320, 160, 3.0, 3.0, "VK 320, 160, 3.0, 3.0", "", 0, 100, Targeting()),
//            CreateCampaignRequest(280, 130, 3.8, 3.2, "VK 280, 130, 3.8, 3.2", "", 0, 100, Targeting()),
//            CreateCampaignRequest(420, 200, 2.5, 2.5, "VK 420, 200, 2.5, 2.5", "", 0, 100, Targeting()),
//            CreateCampaignRequest(400, 180, 3.1, 3.3, "VK 400, 180, 3.1, 3.3", "", 0, 100, Targeting()),
//            CreateCampaignRequest(460, 220, 2.2, 2.2, "VK 460, 220, 2.2, 2.2", "", 0, 100, Targeting()),
//            CreateCampaignRequest(360, 160, 3.4, 2.8, "VK 360, 160, 3.4, 2.8", "", 0, 100, Targeting()),
//            CreateCampaignRequest(350, 150, 3.2, 2.6, "VK 350, 150, 3.2, 2.6", "", 0, 100, Targeting()),
//            CreateCampaignRequest(250, 18, 1.8, 1.0 , "VK 500, 250, 1.8, 1.8", "", 0, 100, Targeting()),
//            CreateCampaignRequest(320, 140, 2.7, 3.1, "VK 320, 140, 2.7, 3.1", "", 0, 100, Targeting()),
//            CreateCampaignRequest(390, 180, 3.0, 2.4, "VK 390, 180, 3.0, 2.4", "", 0, 100, Targeting()),
//            CreateCampaignRequest(440, 210, 2.4, 2.7, "VK 440, 210, 2.4, 2.7", "", 0, 100, Targeting()),
//            CreateCampaignRequest(570, 290, 1.5, 1.5, "VK 570, 290, 1.5, 1.5", "", 0, 100, Targeting()),
//            CreateCampaignRequest(650, 340, 1.2, 1.2, "VK 650, 340, 1.2, 1.2", "", 0, 100, Targeting())
//        )
//
//        for (request in campaignRequests) {
//            Given {
//                contentType(ContentType.JSON)
//                body(request)
//            } When {
//                post("/advertisers/$advertiserId/campaigns")
//            } Then {
//                statusCode(201)
//            }
//        }
//
//        val userIds = arrayOfNulls<UUID>(152)
//        var click = 0
//        for (i in 0..151) {
//            userIds[i] = UUID.randomUUID()
//            Given {
//                contentType(ContentType.JSON)
//                body(listOf(ClientBulkEntity(userIds[i], "Login $i",21,"Moscow","MALE")))
//            } When {
//                post("/clients/bulk")
//            } Then { statusCode(201) }
//        }
//
//        for (i in 1..4) {
//            for (userId in 0..151) {
//                When {
//                    get("/ads?client_id=${userIds[userId]}")
//                } Then {
//                    click += 1
//                    if (click % 15 == 0 && extract().statusCode() != 404) {
//                        val adId = extract().body().path<String>("ad_id")
//                        Given {
//                            contentType(ContentType.JSON)
//                            body(AdRequest(userIds[userId]))
//                        } When {
//                            post("/ads/$adId/click")
//                        } Then {
//                            statusCode(204)
//                        }
//                    }
//                }
//            }
//        }
//
//
//        for (i in 1..28) {
//            When {
//                get("/ads?client_id=${userIds[44]}")
//            } Then {}
//        }
//
//
//        When {
//            get("/stats/advertisers/$advertiserId/campaigns")
//        } Then {
//            log().all()
//            statusCode(200)
//        }
//
//        When {
//            get("/advertisers/$advertiserId/campaigns")
//        } Then {
//            for (el in extract().body().path<List<HashMap<String, String>>>("")) {
//                When {
//                    get("/stats/campaigns/${el["campaign_id"]}")
//                } Then {
//                    log().all()
//                }
//            }
//            log().all()
//            statusCode(200)
//        }
//    }


}