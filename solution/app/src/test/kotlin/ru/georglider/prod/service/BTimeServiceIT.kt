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
import ru.georglider.prod.payload.request.time.TimeSetRequest

@Tag("integration-test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DatabaseSetupExtension::class)
class BTimeServiceIT (
    @LocalServerPort private val port: Int
) {

    @BeforeEach
    fun beforeEach() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun setTime5Return5() {
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(5))
        } When {
            post("/time/advance")
        } Then {
            log().all()
            body("current_date", equalTo(5))
        }
    }

    @Test
    fun setTimeNullReturn400() {
        Given {
            contentType(ContentType.JSON)
            body("{\"current_date\": null}")
        } When {
            post("/time/advance")
        } Then {
            log().all()
            statusCode(400)
        }
    }

    @Test
    fun setTimeEmptyBodyReturn400() {
        Given {
            contentType(ContentType.JSON)
            body("{}")
        } When {
            post("/time/advance")
        } Then {
            log().all()
            statusCode(400)
        }
    }

    @Test
    fun setTimeSameReturn200() {
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(8))
        } When {
            post("/time/advance")
        } Then {
            log().all()
            statusCode(200)
        }

        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(8))
        } When {
            post("/time/advance")
        } Then {
            log().all()
            statusCode(200)
        }
    }

    @Test
    fun setTimeBackwardsReturn400() {
        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(9))
        } When {
            post("/time/advance")
        } Then {
            log().all()
            statusCode(200)
        }

        Given {
            contentType(ContentType.JSON)
            body(TimeSetRequest(4))
        } When {
            post("/time/advance")
        } Then {
            log().all()
            statusCode(400)
        }
    }

}