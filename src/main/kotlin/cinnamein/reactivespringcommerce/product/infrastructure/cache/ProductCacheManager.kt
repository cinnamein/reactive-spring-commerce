package cinnamein.reactivespringcommerce.product.infrastructure.cache

import cinnamein.reactivespringcommerce.product.application.dto.ProductResponse
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class ProductCacheManager(
    private val redisTemplate: ReactiveRedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val KEY_PREFIX = "product:"
        private const val LIST_KEY = "product:all"
        private val TTL = Duration.ofMinutes(10)
    }

    suspend fun getProduct(id: Long): ProductResponse? {
        return try {
            val json = redisTemplate.opsForValue()
                .get("$KEY_PREFIX$id")
                .awaitFirstOrNull() ?: return null
            objectMapper.readValue(json, ProductResponse::class.java)
        } catch (e: Exception) {
            log.warn("캐시 조회 실패 (id={}): {}", id, e.message)
            null
        }
    }

    suspend fun putProduct(response: ProductResponse) {
        try {
            val json = objectMapper.writeValueAsString(response)
            redisTemplate.opsForValue()
                .set("$KEY_PREFIX${response.id}", json, TTL)
                .awaitSingle()
        } catch (e: Exception) {
            log.warn("캐시 저장 실패 (id={}): {}", response.id, e.message)
        }
    }

    suspend fun evictProduct(id: Long) {
        try {
            redisTemplate.delete("$KEY_PREFIX$id").awaitSingle()
            redisTemplate.delete(LIST_KEY).awaitSingle()
        } catch (e: Exception) {
            log.warn("캐시 무효화 실패 (id={}): {}", id, e.message)
        }
    }

    suspend fun getProductList(): List<ProductResponse>? {
        return try {
            val json = redisTemplate.opsForValue()
                .get(LIST_KEY)
                .awaitFirstOrNull() ?: return null
            objectMapper.readValue(
                json,
                objectMapper.typeFactory.constructCollectionType(List::class.java, ProductResponse::class.java)
            )
        } catch (e: Exception) {
            log.warn("목록 캐시 조회 실패: {}", e.message)
            null
        }
    }

    suspend fun putProductList(responses: List<ProductResponse>) {
        try {
            val json = objectMapper.writeValueAsString(responses)
            redisTemplate.opsForValue()
                .set(LIST_KEY, json, TTL)
                .awaitSingle()
        } catch (e: Exception) {
            log.warn("목록 캐시 저장 실패: {}", e.message)
        }
    }

    suspend fun evictAll() {
        try {
            val keys = redisTemplate.keys("$KEY_PREFIX*").collectList().awaitSingle()
            if (keys.isNotEmpty()) {
                redisTemplate.delete(*keys.toTypedArray()).awaitSingle()
            }
        } catch (e: Exception) {
            log.warn("전체 캐시 무효화 실패: {}", e.message)
        }
    }
}