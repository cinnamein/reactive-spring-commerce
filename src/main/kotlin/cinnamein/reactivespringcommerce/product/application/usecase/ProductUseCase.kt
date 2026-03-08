package cinnamein.reactivespringcommerce.product.application.usecase

import cinnamein.reactivespringcommerce.product.application.dto.CreateProductRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductResponse
import cinnamein.reactivespringcommerce.product.application.dto.UpdateProductRequest
import cinnamein.reactivespringcommerce.product.domain.Product
import cinnamein.reactivespringcommerce.product.domain.ProductNotFoundException
import cinnamein.reactivespringcommerce.product.domain.ProductRepository
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

        product.update(
            name = request.name,
            price = request.price,
            seller = request.seller,
        )

        val updated = productRepository.update(product)
        return ProductResponse.from(updated)
    }

    suspend fun deleteProduct(id: Long) {
        if (!productRepository.existsById(id)) {
            throw ProductNotFoundException(id)
        }
        productRepository.deleteById(id)
    }
}