package cinnamein.reactivespringcommerce.product.domain.model

import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException

/**
 * 상품 옵션 (Value Object).
 */

data class ProductOption(
    val size: ProductSize,
    val color: ProductColor,
    val additionalPrice: Long,
    val stockQuantity: Int,
) {
    init {
        if (additionalPrice < 0) throw InvalidProductException("추가 가격은 0 이상이어야 합니다.")
        if (stockQuantity < 0) throw InvalidProductException("재고 수량은 0 이상이어야 합니다.")
    }
}