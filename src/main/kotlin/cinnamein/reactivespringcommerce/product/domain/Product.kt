package cinnamein.reactivespringcommerce.product.domain

class Product private constructor(
    val id: Long? = null,
    name: String,
    price: Long,
    seller: String,
) {
    var name: String = name
        private set

    var price = price
        private set

    var seller = seller
        private set

    companion object {
        fun create(name: String, price: Long, seller: String): Product {
            validate(name, price, seller)
            return Product(
                name = name.trim(),
                price = price,
                seller = seller.trim(),
            )
        }

        private fun validate(name: String, price: Long, seller: String) {
            require(name.isNotBlank()) {
                throw InvalidProductException("상품명은 공백일 수 없습니다.")
            }
            require(price > 0) {
                throw InvalidProductException("가격은 0보다 커야 합니다. 입력값: $price")
            }
            require(seller.isNotBlank()) {
                throw InvalidProductException("판매자는 공백일 수 없습니다.")
            }
        }

        fun reconstitute(
            id: Long,
            name: String,
            price: Long,
            seller: String,
        ): Product = Product(
            id = id,
            name = name,
            price = price,
            seller = seller,
        )
    }

    fun update(name: String, price: Long, seller: String): Product {
        validate(name, price, seller)
        this.name = name.trim()
        this.price = price
        this.seller = seller.trim()
        return this
    }
}