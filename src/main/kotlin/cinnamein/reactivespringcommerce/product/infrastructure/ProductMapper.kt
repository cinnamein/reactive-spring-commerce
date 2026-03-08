package cinnamein.reactivespringcommerce.product.infrastructure

import cinnamein.reactivespringcommerce.product.domain.Product

object ProductMapper {

    fun toEntity(product: Product): ProductEntity = ProductEntity(
        id = product.id,
        name = product.name,
        price = product.price,
        seller = product.seller,
    )

    fun toDomain(entity: ProductEntity): Product = Product.reconstitute(
        id = entity.id!!,
        name = entity.name,
        price = entity.price,
        seller = entity.seller,
    )
}