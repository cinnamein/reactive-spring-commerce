package cinnamein.reactivespringcommerce.product.application.usecase

import cinnamein.reactivespringcommerce.product.application.dto.*
import cinnamein.reactivespringcommerce.product.domain.exception.ProductNotFoundException
import cinnamein.reactivespringcommerce.product.domain.model.Product
import cinnamein.reactivespringcommerce.product.domain.model.ProductImage
import cinnamein.reactivespringcommerce.product.domain.model.ProductOption
import cinnamein.reactivespringcommerce.product.domain.repository.ProductRepository
import cinnamein.reactivespringcommerce.product.infrastructure.cache.ProductCacheManager
import org.springframework.stereotype.Service

@Service
class ProductUseCase(
    private val productRepository: ProductRepository,
    private val cacheManager: ProductCacheManager,
) {

    suspend fun createProduct(request: CreateProductRequest): ProductResponse {
        val product = Product.create(
            name = request.name,
            price = request.price,
            seller = request.seller,
            description = request.description,
            options = request.options.map { it.toDomain() },
            images = request.images.map { it.toDomain() },
        )
        val saved = productRepository.save(product)
        val response = ProductResponse.from(saved)

        cacheManager.putProduct(response)
        cacheManager.evictAll()  // 목록 캐시 무효화

        return response
    }

    suspend fun getProduct(id: Long): ProductResponse {
        cacheManager.getProduct(id)?.let { return it }
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        val response = ProductResponse.from(product)

        cacheManager.putProduct(response)

        return response
    }

    suspend fun getAllProducts(): List<ProductResponse> {
        cacheManager.getProductList()?.let { return it }
        val responses = productRepository.findAll()
            .map { ProductResponse.from(it) }

        cacheManager.putProductList(responses)

        return responses
    }

    suspend fun updateProduct(id: Long, request: UpdateProductRequest): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)

        product.updateInfo(
            name = request.name,
            price = request.price,
            seller = request.seller,
            description = request.description,
        )

        val updated = Product.reconstitute(
            id = product.id!!,
            name = product.name,
            price = product.price,
            seller = product.seller,
            description = product.description,
            status = product.status,
            options = request.options.map { it.toDomain() },
            images = request.images.map { it.toDomain() },
        )

        val saved = productRepository.update(updated)
        val response = ProductResponse.from(saved)

        cacheManager.putProduct(response)
        cacheManager.evictAll()

        return response
    }

    suspend fun deleteProduct(id: Long) {
        if (!productRepository.existsById(id)) {
            throw ProductNotFoundException(id)
        }
        productRepository.deleteById(id)

        cacheManager.evictProduct(id)
    }

    suspend fun publishProduct(id: Long): ProductResponse {
        return updateStatusAndCache(id) { it.publish() }
    }

    suspend fun soldOutProduct(id: Long): ProductResponse {
        return updateStatusAndCache(id) { it.soldOut() }
    }

    suspend fun discontinueProduct(id: Long): ProductResponse {
        return updateStatusAndCache(id) { it.discontinue() }
    }

    suspend fun hideProduct(id: Long): ProductResponse {
        return updateStatusAndCache(id) { it.hide() }
    }

    private suspend fun updateStatusAndCache(id: Long, action: (Product) -> Unit): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        action(product)
        val saved = productRepository.update(product)
        val response = ProductResponse.from(saved)

        cacheManager.putProduct(response)
        cacheManager.evictAll()

        return response
    }

    private fun ProductOptionRequest.toDomain(): ProductOption {
        return ProductOption(
            size = size,
            color = color,
            additionalPrice = additionalPrice,
            stockQuantity = stockQuantity,
        )
    }

    private fun ProductImageRequest.toDomain(): ProductImage = ProductImage(
        url = url,
        sortOrder = sortOrder,
        primaryImage = primaryImage,
    )
}