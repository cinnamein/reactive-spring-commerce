package cinnamein.reactivespringcommerce.product.application.usecase

import cinnamein.reactivespringcommerce.product.application.dto.CreateProductRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductImageRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductOptionRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductResponse
import cinnamein.reactivespringcommerce.product.application.dto.UpdateProductRequest
import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException
import cinnamein.reactivespringcommerce.product.domain.exception.ProductNotFoundException
import cinnamein.reactivespringcommerce.product.domain.model.Product
import cinnamein.reactivespringcommerce.product.domain.model.ProductColor
import cinnamein.reactivespringcommerce.product.domain.model.ProductImage
import cinnamein.reactivespringcommerce.product.domain.model.ProductOption
import cinnamein.reactivespringcommerce.product.domain.model.ProductSize
import cinnamein.reactivespringcommerce.product.domain.repository.ProductRepository
import org.springframework.stereotype.Service

@Service
class ProductUseCase(
    private val productRepository: ProductRepository,
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
        return ProductResponse.from(saved)
    }

    suspend fun getProduct(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        return ProductResponse.from(product)
    }

    suspend fun getAllProducts(): List<ProductResponse> {
        return productRepository.findAll()
            .map { ProductResponse.from(it) }
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
        return ProductResponse.from(saved)
    }

    suspend fun deleteProduct(id: Long) {
        if (!productRepository.existsById(id)) {
            throw ProductNotFoundException(id)
        }
        productRepository.deleteById(id)
    }

    suspend fun publishProduct(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        product.publish()
        val saved = productRepository.update(product)
        return ProductResponse.from(saved)
    }

    suspend fun soldOutProduct(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        product.soldOut()
        val saved = productRepository.update(product)
        return ProductResponse.from(saved)
    }

    suspend fun discontinueProduct(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        product.discontinue()
        val saved = productRepository.update(product)
        return ProductResponse.from(saved)
    }

    suspend fun hideProduct(id: Long): ProductResponse {
        val product = productRepository.findById(id)
            ?: throw ProductNotFoundException(id)
        product.hide()
        val saved = productRepository.update(product)
        return ProductResponse.from(saved)
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