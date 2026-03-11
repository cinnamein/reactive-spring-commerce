package cinnamein.reactivespringcommerce.product.application.dto

import cinnamein.reactivespringcommerce.product.domain.model.ProductColor
import cinnamein.reactivespringcommerce.product.domain.model.ProductSize

data class ProductOptionRequest(
    val size: ProductSize,
    val color: ProductColor,
    val additionalPrice: Long = 0,
    val stockQuantity: Int = 0,
)