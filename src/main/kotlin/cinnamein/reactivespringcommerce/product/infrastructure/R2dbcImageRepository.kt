package cinnamein.reactivespringcommerce.product.infrastructure

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface R2dbcProductImageRepository : CoroutineCrudRepository<ProductImageEntity, Long> {
    fun findAllByProductId(productId: Long): Flow<ProductImageEntity>
    suspend fun deleteAllByProductId(productId: Long)
}