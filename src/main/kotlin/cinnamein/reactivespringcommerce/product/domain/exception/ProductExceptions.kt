package cinnamein.reactivespringcommerce.product.domain.exception

class InvalidProductException(message: String) : RuntimeException(message)

class ProductNotFoundException(id: Long) : RuntimeException("상품을 찾을 수 없습니다. id=$id")