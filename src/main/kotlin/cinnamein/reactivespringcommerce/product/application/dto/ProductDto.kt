package cinnamein.reactivespringcommerce.product.application.dto

import cinnamein.reactivespringcommerce.product.domain.Product

data class CreateProductRequest(
    val name: String,
    val price: Long,
    val seller: String,
)

data class UpdateProductRequest(
    val name: String,
    val price: Long,
    val seller: String,
)

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val seller: String,
) {
    companion object {
        fun from(product: Product): ProductResponse = ProductResponse(
            id = product.id!!,
            name = product.name,
            price = product.price,
            seller = product.seller,
        )
    }
}