package cinnamein.reactivespringcommerce.product.application.dto

data class ProductImageRequest(
    val url: String,
    val sortOrder: Int = 0,
    val primaryImage: Boolean = false,
)