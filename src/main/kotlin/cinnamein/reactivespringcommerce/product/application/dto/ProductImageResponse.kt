package cinnamein.reactivespringcommerce.product.application.dto

data class ProductImageResponse(
    val url: String,
    val sortOrder: Int,
    val primaryImage: Boolean,
)