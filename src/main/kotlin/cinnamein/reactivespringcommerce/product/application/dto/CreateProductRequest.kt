package cinnamein.reactivespringcommerce.product.application.dto

data class CreateProductRequest(
    val name: String,
    val price: Long,
    val seller: String,
    val description: String = "",
    val options: List<ProductOptionRequest>,
    val images: List<ProductImageRequest>,
)