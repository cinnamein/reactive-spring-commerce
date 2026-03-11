package cinnamein.reactivespringcommerce.product.application.dto

data class UpdateProductRequest(
    val name: String,
    val price: Long,
    val seller: String,
    val description: String = "",
    val options: List<ProductOptionRequest>,
    val images: List<ProductImageRequest>,
)