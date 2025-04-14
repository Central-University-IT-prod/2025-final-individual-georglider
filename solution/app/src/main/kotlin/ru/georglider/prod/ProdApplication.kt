package ru.georglider.prod

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling
import ru.georglider.prod.utils.MinioInitUtil
import ru.georglider.prod.utils.WaitPostgresUtil
import ru.georglider.prod.utils.WaitRedisUtil

@SpringBootApplication
@EnableScheduling
class ProdApplication

fun main(args: Array<String>) {
	MinioInitUtil.createBucket()
	WaitPostgresUtil.connectSocket()
	WaitRedisUtil.connectSocket()
	runApplication<ProdApplication>(*args)
}
