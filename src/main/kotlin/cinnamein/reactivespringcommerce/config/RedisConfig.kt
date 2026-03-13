package cinnamein.reactivespringcommerce.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class RedisConfig {

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}