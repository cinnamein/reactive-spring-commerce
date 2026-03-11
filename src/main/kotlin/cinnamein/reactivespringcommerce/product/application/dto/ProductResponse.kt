package cinnamein.reactivespringcommerce.product.application.dto

import cinnamein.reactivespringcommerce.product.domain.model.Product

data class ProductResponse(
    val id: Long,
    val name: String,
    val price: Long,
    val seller: String,
    val description: String,
    val status: String,
    val options: List<ProductOptionResponse>,
    val images: List<ProductImageResponse>,
) {
    companion object {
        fun from(product: Product): ProductResponse = ProductResponse(
            id = product.id!!,
            name = product.name,
            price = product.price,
            seller = product.seller,
            description = product.description,
            status = product.status.name,
            options = product.options.map {
                ProductOptionResponse(it.size.name, it.color.name, it.color.displayName, it.additionalPrice, it.stockQuantity)
            },
            images = product.images.map {
                ProductImageResponse(it.url, it.sortOrder, it.primaryImage)
            },
        )
    }
}