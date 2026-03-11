package cinnamein.reactivespringcommerce.product.domain.model

import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException

/**
 * Product Aggregate Root.
 *
 * Product는 Option과 Image의 생명주기를 관리한다.
 * 모든 하위 객체(Option, Image)는 반드시 Product를 통해서만 접근/변경된다.
 *
 * 불변식:
 * - 상품명은 공백일 수 없다
 * - 가격은 0보다 커야 한다
 * - 판매자는 공백일 수 없다
 * - 옵션은 최소 1개 이상이어야 한다
 * - 동일 size+color 조합의 옵션 중복 불가
 * - 대표 이미지는 정확히 1개여야 한다
 */
class Product private constructor(
    val id: Long? = null,
    name: String,
    price: Long,
    seller: String,
    description: String,
    status: ProductStatus,
    options: MutableList<ProductOption>,
    images: MutableList<ProductImage>,
) {
    var name: String = name
        private set

    var price: Long = price
        private set

    var seller: String = seller
        private set

    var description: String = description
        private set

    var status: ProductStatus = status
        private set

    private val _options: MutableList<ProductOption> = options
    val options: List<ProductOption> get() = _options.toList()

    private val _images: MutableList<ProductImage> = images
    val images: List<ProductImage> get() = _images.toList()

    companion object {
        fun create(
            name: String,
            price: Long,
            seller: String,
            description: String = "",
            options: List<ProductOption>,
            images: List<ProductImage>,
        ): Product {
            validateBase(name, price, seller)
            validateOptions(options)
            validateImages(images)

            return Product(
                name = name.trim(),
                price = price,
                seller = seller.trim(),
                description = description.trim(),
                status = ProductStatus.DRAFT,
                options = options.toMutableList(),
                images = images.toMutableList(),
            )
        }

        fun reconstitute(
            id: Long,
            name: String,
            price: Long,
            seller: String,
            description: String,
            status: ProductStatus,
            options: List<ProductOption>,
            images: List<ProductImage>,
        ): Product = Product(
            id = id,
            name = name,
            price = price,
            seller = seller,
            description = description,
            status = status,
            options = options.toMutableList(),
            images = images.toMutableList(),
        )

        private fun validateBase(name: String, price: Long, seller: String) {
            if (name.isBlank()) throw InvalidProductException("상품명은 공백일 수 없습니다.")
            if (price <= 0) throw InvalidProductException("가격은 0보다 커야 합니다. 입력값: $price")
            if (seller.isBlank()) throw InvalidProductException("판매자는 공백일 수 없습니다.")
        }

        private fun validateOptions(options: List<ProductOption>) {
            if (options.isEmpty()) throw InvalidProductException("옵션은 최소 1개 이상이어야 합니다.")
            val duplicates = options.groupBy { it.size to it.color }.filter { it.value.size > 1 }
            if (duplicates.isNotEmpty()) {
                val dup = duplicates.keys.first()
                throw InvalidProductException("동일한 사이즈/색상 조합의 옵션이 중복됩니다: ${dup.first} / ${dup.second}")
            }
        }

        private fun validateImages(images: List<ProductImage>) {
            val primaryCount = images.count { it.primaryImage }
            if (primaryCount != 1) {
                throw InvalidProductException("대표 이미지는 정확히 1개여야 합니다. 현재: ${primaryCount}개")
            }
        }
    }

    fun updateInfo(name: String, price: Long, seller: String, description: String) {
        validateBase(name, price, seller)
        this.name = name.trim()
        this.price = price
        this.seller = seller.trim()
        this.description = description.trim()
    }

    fun publish() {
        if (status != ProductStatus.DRAFT) {
            throw InvalidProductException("DRAFT 상태에서만 판매 개시할 수 있습니다. 현재: $status")
        }
        this.status = ProductStatus.ON_SALE
    }

    fun soldOut() {
        if (status != ProductStatus.ON_SALE) {
            throw InvalidProductException("ON_SALE 상태에서만 품절 처리할 수 있습니다. 현재: $status")
        }
        this.status = ProductStatus.SOLD_OUT
    }

    fun discontinue() {
        if (status != ProductStatus.ON_SALE && status != ProductStatus.SOLD_OUT) {
            throw InvalidProductException("ON_SALE 또는 SOLD_OUT 상태에서만 판매 중지할 수 있습니다. 현재: $status")
        }
        this.status = ProductStatus.DISCONTINUED
    }

    fun hide() {
        this.status = ProductStatus.HIDDEN
    }

    fun addOption(option: ProductOption) {
        if (_options.any { it.size == option.size && it.color == option.color }) {
            throw InvalidProductException("이미 존재하는 사이즈/색상 조합입니다: ${option.size} / ${option.color}")
        }
        _options.add(option)
    }

    fun removeOption(size: ProductSize, color: ProductColor) {
        if (_options.size <= 1) {
            throw InvalidProductException("옵션은 최소 1개 이상이어야 합니다.")
        }
        val removed = _options.removeAll { it.size == size && it.color == color }
        if (!removed) {
            throw InvalidProductException("존재하지 않는 옵션입니다: $size / $color")
        }
    }

    fun addImage(image: ProductImage) {
        if (image.primaryImage && _images.any { it.primaryImage }) {
            val updated = _images.map { it.copy(primaryImage = false) }
            _images.clear()
            _images.addAll(updated)
        }
        _images.add(image)
    }

    fun removeImage(imageUrl: String) {
        val target = _images.find { it.url == imageUrl }
            ?: throw InvalidProductException("존재하지 않는 이미지입니다: $imageUrl")

        if (target.primaryImage) {
            throw InvalidProductException("대표 이미지는 삭제할 수 없습니다. 다른 이미지를 대표로 지정한 후 삭제하세요.")
        }
        _images.remove(target)
    }
}