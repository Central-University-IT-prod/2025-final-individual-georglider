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
import java.util.*

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class)
class DAdvertiserServiceIT (
    @LocalServerPort private val port: Int,
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun getInvalidArgumentExpect400() {
        When {
            get("/advertisers/4")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postEmptyWrongBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(BulkEntity())
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postEmptyBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity()))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postInvalidParamIdExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"advertiser_id\": \"2c963f66afa6\",\"name\": \"string\"}")
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postParamNameLimitsTest() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "A".repeat(256))))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            log().all()
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "A".repeat(255))))
        } When {
            post("/advertisers/bulk")
        } Then {
            log().all()
            statusCode(201)
        }
    }

    @Test
    fun postValidEntityWrongBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(BulkEntity(UUID.randomUUID(), "il"))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postValidEntityExpect201() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "MALE")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun post2ValidEntitiesExpect201() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(
                BulkEntity(UUID.randomUUID(), "FEMALE"),
                BulkEntity(UUID.randomUUID(), "Saint-Petersburg"))
            )
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun changeEntityExpect201AndNameChange() {
        val id = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(id, "tochange")))
        } When {
            post("/advertisers/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(id, "Vladimir")))
        } When {
            post("/advertisers/bulk")
        } Then {
            log().all()
            statusCode(201)
            body("size()", equalTo(1))
            body("[0].advertiser_id", equalTo(id.toString()))
            body("[0].name", equalTo("Vladimir"))
        }

        When {
            get("/advertisers/$id")
        } Then {
            statusCode(200)
            body("name", equalTo("Vladimir"))
        }
    }

    @Test
    fun findRandomValidIdExpect404() {
        When {
            get("/advertisers/${UUID.randomUUID()}")
        } Then {
            statusCode(404)
        }
    }

    @Test
    fun create2EntitiesPass3Expect201And2Items() {
        val ids = listOf(UUID.randomUUID(), UUID.randomUUID())
        Given {
            contentType(ContentType.JSON)
            body(
                listOf(
                    BulkEntity(ids[0], "jerry"),
                    BulkEntity(ids[1], "helen"),
                    BulkEntity(ids[0], "vitaly")
                )
            )
        } When {
            post("/advertisers/bulk")
        } Then {
            log().all()
            statusCode(201)
            body("size()", equalTo(2))
        }
    }

}