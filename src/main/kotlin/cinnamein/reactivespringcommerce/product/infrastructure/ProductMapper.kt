package cinnamein.reactivespringcommerce.product.infrastructure

import cinnamein.reactivespringcommerce.product.domain.model.*

object ProductMapper {

    fun toProductEntity(product: Product): ProductEntity = ProductEntity(
        id = product.id,
        name = product.name,
        price = product.price,
        seller = product.seller,
        description = product.description,
        status = product.status.name,
    )

    fun toOptionEntities(productId: Long, options: List<ProductOption>): List<ProductOptionEntity> =
        options.map { option ->
            ProductOptionEntity(
                productId = productId,
                size = option.size.name,
                color = option.color.name,
                additionalPrice = option.additionalPrice,
                stockQuantity = option.stockQuantity,
            )
        }

    fun toImageEntities(productId: Long, images: List<ProductImage>): List<ProductImageEntity> =
        images.map { image ->
            ProductImageEntity(
                productId = productId,
                url = image.url,
                sortOrder = image.sortOrder,
                primaryImage = image.primaryImage,
            )
        }

    fun toDomain(
        entity: ProductEntity,
        optionEntities: List<ProductOptionEntity>,
        imageEntities: List<ProductImageEntity>,
    ): Product = Product.reconstitute(
        id = entity.id!!,
        name = entity.name,
        price = entity.price,
        seller = entity.seller,
        description = entity.description,
        status = ProductStatus.valueOf(entity.status),
        options = optionEntities.map { toOptionDomain(it) },
        images = imageEntities.map { toImageDomain(it) },
    )

    private fun toOptionDomain(entity: ProductOptionEntity): ProductOption = ProductOption(
        size = ProductSize.valueOf(entity.size),
        color = ProductColor.valueOf(entity.color),
        additionalPrice = entity.additionalPrice,
        stockQuantity = entity.stockQuantity,
    )

    private fun toImageDomain(entity: ProductImageEntity): ProductImage = ProductImage(
        url = entity.url,
        sortOrder = entity.sortOrder,
        primaryImage = entity.primaryImage,
    )
}