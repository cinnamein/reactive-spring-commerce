package cinnamein.reactivespringcommerce.product.application.dto

data class ProductOptionResponse(
    val size: String,
    val color: String,
    val colorDisplayName: String,
    val additionalPrice: Long,
    val stockQuantity: Int,
)