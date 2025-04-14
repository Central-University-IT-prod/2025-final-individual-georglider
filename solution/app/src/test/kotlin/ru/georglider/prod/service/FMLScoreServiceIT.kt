package ru.georglider.prod.service

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import ru.georglider.prod.payload.request.advertiser.BulkEntity as AdBulkEntity
import ru.georglider.prod.payload.request.client.BulkEntity as ClientBulkEntity
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import ru.georglider.prod.payload.request.advertiser.MLScoreRequest
import java.util.*

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class)
class FMLScoreServiceIT (
    @LocalServerPort private val port: Int
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun getInvalidClientIdArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"client_id\": abc\", \"score\": 15 }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getInvalidAdvertiserIdArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"asjidoi21\", \"client_id\": b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"score\": 15 }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getInvalidScoreArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"client_id\": b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"score\": \"v\" }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }
    
    @Test
    fun getMissingClientIdArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"score\": 15 }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getMissingAdvertiserIdArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"client_id\": b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"score\": 15 }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getMissingScoreArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"client_id\": b2d45304-e821-43b1-9f34-97f89c0f1ae1\" }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getNullScoreArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"client_id\": b2d45304-e821-43b1-9f34-97f89c0f1ae1\", \"score\": null }")
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getNullClientIdArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(null, UUID.randomUUID(), 10))
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun getNullAdvertiserIdArgumentExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(UUID.randomUUID(), null, 10))
        } When {
            post("/ml-scores")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postValidEntityNotFoundExpect404() {
        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(UUID.randomUUID(), UUID.randomUUID(), 10))
        } When {
            post("/ml-scores")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun postValidEntityClientNotFoundExpect404() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(AdBulkEntity(advertiserId, "ABC")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(UUID.randomUUID(), advertiserId, 10))
        } When {
            post("/ml-scores")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun postValidEntityExpect200() {
        val advertiserId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(AdBulkEntity(advertiserId, "ABC")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        val clientId = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(ClientBulkEntity(clientId, "ABC", 10, "Moscow", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body(MLScoreRequest(clientId, advertiserId, 10))
        } When {
            post("/ml-scores")
        } Then {
            statusCode(200)
        }
    }

}