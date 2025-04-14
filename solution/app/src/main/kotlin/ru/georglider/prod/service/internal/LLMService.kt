package ru.georglider.prod.service.internal

import org.springframework.ai.mistralai.MistralAiChatModel
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.util.retry.Retry
import ru.georglider.prod.exceptions.model.BadRequestException
import ru.georglider.prod.exceptions.model.TooManyRequests
import java.time.Duration

@Service
class LLMService (
    private val chatModel: MistralAiChatModel
) {

    fun generate(companyName: String, title: String): Flux<String> {
        return this.chatModel.stream(
            """
                Представь, что ты работаешь менеджером интернет рекламной компании и клиент не знает, какой
                текст объявления ему можно сделать. Помоги ему создать ему небольшой текст для объявления, который будет
                находится под заголовком. Предлагай этот текст на основании имени компании, а также главного текста 
                рекламы. Имя компании - $companyName, главный текст - $title. Отправляй только
                итоговый результат, не должно быть никаких сообщений кроме самого текста для объявления. 
                Твой язык должен строго совпадать с языком текста!
            """
        ).retryWhen(
            Retry.fixedDelay(3, Duration.ofMillis(750))
                .filter { it is WebClientResponseException.TooManyRequests }
        ).onErrorMap {
            if (it is WebClientResponseException.TooManyRequests) {
                TooManyRequests()
            } else {
                BadRequestException()
            }
        }
    }

}