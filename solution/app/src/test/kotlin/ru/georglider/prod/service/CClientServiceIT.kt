package ru.georglider.prod.service

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.core.IsEqual.equalTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import ru.georglider.prod.utils.extensions.DatabaseSetupExtension
import ru.georglider.prod.payload.request.client.BulkEntity
import java.util.*

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class)
class CClientServiceIT (
    @LocalServerPort private val port: Int
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun getInvalidArgumentExpect400() {
        When {
            get("/clients/4")
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
            post("/clients/bulk")
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
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postInvalidParamGenderExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", 12, "FL", "MALEE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postInvalidParamAgeExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", -1, "FL", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", 123, "FL", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postParamLocationLimitsTest() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", 15, "D".repeat(256), "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", 15, "D".repeat(300), "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "California", 15, "", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", 15, "D".repeat(255), "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            log().all()
            statusCode(201)
        }
    }

    @Test
    fun postParamLoginLimitsTest() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "D".repeat(256), 15, "California", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "D".repeat(300), 15, "California", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "", 15, "California", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "D".repeat(255), 15, "California", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            log().all()
            statusCode(201)
        }
    }

    @Test
    fun postInvalidParamIdExpect400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"client_id\": \"2c963f66afa6\",\"login\": \"string\",\"age\": 8,\"location\": \"string\",\"gender\": \"MALE\"}")
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postValidEntityWrongBodyExpect400() {
        Given {
            contentType(ContentType.JSON)
            body(BulkEntity(UUID.randomUUID(), "il", 12, "FL", "MALE"))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(400)
        }
    }

    @Test
    fun postValidEntityExpect201() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(UUID.randomUUID(), "il", 45, "FL", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun post2ValidEntitiesExpect201() {
        Given {
            contentType(ContentType.JSON)
            body(listOf(
                BulkEntity(UUID.randomUUID(), "weif", 15, "Moscow", "FEMALE"),
                BulkEntity(UUID.randomUUID(), "vne", 18, "Saint-Petersburg", "FEMALE"))
            )
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }
    }

    @Test
    fun changeEntityExpect201AndNameChange() {
        val id = UUID.randomUUID()
        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(id, "tochange", 22, "Vologda", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            statusCode(201)
        }

        Given {
            contentType(ContentType.JSON)
            body(listOf(BulkEntity(id, "Vladimir", 22, "Vologda", "MALE")))
        } When {
            post("/clients/bulk")
        } Then {
            log().all()
            statusCode(201)
            body("size()", equalTo(1))
            body("[0].client_id", equalTo(id.toString()))
            body("[0].login", equalTo("Vladimir"))
        }

        When {
            get("/clients/$id")
        } Then {
            statusCode(200)
            body("login", equalTo("Vladimir"))
        }
    }

    @Test
    fun findRandomValidIdExpect404() {
        When {
            get("/clients/${UUID.randomUUID()}")
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
                    BulkEntity(ids[0], "jerry", 20, "Saratov", "MALE"),
                    BulkEntity(ids[1], "helen", 22, "Vologda", "FEMALE"),
                    BulkEntity(ids[0], "jerry", 18, "Saratov", "MALE")
                )
            )
        } When {
            post("/clients/bulk")
        } Then {
            log().all()
            statusCode(201)
            body("size()", equalTo(2))
            body("location", hasItems("Saratov", "Vologda"))
        }
    }

}