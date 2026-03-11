package cinnamein.reactivespringcommerce.product.domain.model

enum class ProductStatus {
    DRAFT, // 임시 저장
    ON_SALE, // 판매 중
    SOLD_OUT, // 품절
    DISCONTINUED, // 판매 중지
    HIDDEN, // 숨김
}