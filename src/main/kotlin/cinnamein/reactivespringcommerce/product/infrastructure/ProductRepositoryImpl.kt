package cinnamein.reactivespringcommerce.product.infrastructure

import cinnamein.reactivespringcommerce.product.domain.model.Product
import cinnamein.reactivespringcommerce.product.domain.repository.ProductRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productRepo: R2dbcProductRepository,
    private val optionRepo: R2dbcProductOptionRepository,
    private val imageRepo: R2dbcProductImageRepository,
) : ProductRepository {

    override suspend fun save(product: Product): Product {
        val savedProduct = productRepo.save(ProductMapper.toProductEntity(product))
        val productId = savedProduct.id!!

        ProductMapper.toOptionEntities(productId, product.options)
            .forEach { optionRepo.save(it) }

        ProductMapper.toImageEntities(productId, product.images)
            .forEach { imageRepo.save(it) }

        return findById(productId)!!
    }

    override suspend fun findById(id: Long): Product? {
        val productEntity = productRepo.findById(id) ?: return null
        val options = optionRepo.findAllByProductId(id).toList()
        val images = imageRepo.findAllByProductId(id).toList()
        return ProductMapper.toDomain(productEntity, options, images)
    }

    override suspend fun findAll(): List<Product> {
        return productRepo.findAll()
            .map { entity ->
                val options = optionRepo.findAllByProductId(entity.id!!).toList()
                val images = imageRepo.findAllByProductId(entity.id).toList()
                ProductMapper.toDomain(entity, options, images)
            }
            .toList()
    }

    override suspend fun update(product: Product): Product {
        val productId = product.id!!

        productRepo.save(ProductMapper.toProductEntity(product))

        optionRepo.deleteAllByProductId(productId)
        imageRepo.deleteAllByProductId(productId)

        ProductMapper.toOptionEntities(productId, product.options)
            .forEach { optionRepo.save(it) }

        ProductMapper.toImageEntities(productId, product.images)
            .forEach { imageRepo.save(it) }

        return findById(productId)!!
    }

    override suspend fun deleteById(id: Long) {
        productRepo.deleteById(id)
    }

    override suspend fun existsById(id: Long): Boolean {
        return productRepo.existsById(id)
    }
}