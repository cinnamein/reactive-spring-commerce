package cinnamein.reactivespringcommerce.product.infrastructure

import cinnamein.reactivespringcommerce.product.domain.Product
import cinnamein.reactivespringcommerce.product.domain.ProductRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val r2dbcRepository: R2dbcProductRepository,
) : ProductRepository {

    override suspend fun save(product: Product): Product {
        val entity = ProductMapper.toEntity(product)
        val saved = r2dbcRepository.save(entity)
        return ProductMapper.toDomain(saved)
    }

    override suspend fun findById(id: Long): Product? {
        return r2dbcRepository.findById(id)?.let { ProductMapper.toDomain(it) }
    }

    override suspend fun findAll(): List<Product> {
        return r2dbcRepository.findAll()
            .map { ProductMapper.toDomain(it) }
            .toList()
    }

    override suspend fun update(product: Product): Product {
        val entity = ProductEntity(
            id = product.id,
            name = product.name,
            price = product.price,
            seller = product.seller,
        )
        val saved = r2dbcRepository.save(entity)
        return ProductMapper.toDomain(saved)
    }

    override suspend fun deleteById(id: Long) {
        r2dbcRepository.deleteById(id)
    }

    override suspend fun existsById(id: Long): Boolean {
        return r2dbcRepository.existsById(id)
    }
}