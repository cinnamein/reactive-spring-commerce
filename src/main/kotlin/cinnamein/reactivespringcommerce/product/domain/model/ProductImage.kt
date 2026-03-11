package cinnamein.reactivespringcommerce.product.domain.model

import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException

/**
 * 상품 이미지 (Value Object).
 */
data class ProductImage(
    val url: String,
    val sortOrder: Int,
    val primaryImage: Boolean,
) {
    init {
        if (url.isBlank()) throw InvalidProductException("이미지 URL은 공백일 수 없습니다.")
        if (sortOrder < 0) throw InvalidProductException("정렬 순서는 0 이상이어야 합니다.")
    }
}