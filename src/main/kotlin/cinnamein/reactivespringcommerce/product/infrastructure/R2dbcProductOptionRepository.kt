package cinnamein.reactivespringcommerce.product.infrastructure

import kotlinx.coroutines.flow.Flow
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface R2dbcProductOptionRepository : CoroutineCrudRepository<ProductOptionEntity, Long> {
    fun findAllByProductId(productId: Long): Flow<ProductOptionEntity>
    suspend fun deleteAllByProductId(productId: Long)
}