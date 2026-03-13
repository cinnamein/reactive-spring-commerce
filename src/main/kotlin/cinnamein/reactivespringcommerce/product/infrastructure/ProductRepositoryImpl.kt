package cinnamein.reactivespringcommerce.product.infrastructure

import cinnamein.reactivespringcommerce.product.domain.model.Product
import cinnamein.reactivespringcommerce.product.domain.repository.ProductRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productRepo: R2dbcProductRepository,
    private val optionRepo: R2dbcProductOptionRepository,
    private val imageRepo: R2dbcProductImageRepository,
    private val databaseClient: DatabaseClient,
) : ProductRepository {

    override suspend fun save(product: Product): Product {
        val savedProduct = productRepo.save(ProductMapper.toProductEntity(product))
        val productId = savedProduct.id!!

        ProductMapper.toOptionEntities(productId, product.options)
            .forEach { optionRepo.save(it) }

        ProductMapper.toImageEntities(productId, product.images)
            .forEach { imageRepo.save(it) }

        return findById(productId)!!
    }

    override suspend fun findById(id: Long): Product? {
        val productEntity = productRepo.findById(id) ?: return null
        val options = optionRepo.findAllByProductId(id).toList()
        val images = imageRepo.findAllByProductId(id).toList()
        return ProductMapper.toDomain(productEntity, options, images)
    }

    /**
     * N+1 해결: 상품 전체 조회 시 Option/Image를 IN 쿼리로 한 번에 가져온 뒤
     * 메모리에서 productId 기준으로 그룹핑하여 Aggregate를 조립한다.
     *
     * 기존: 상품 N개 → 쿼리 1 + N + N = 2N+1회
     * 개선: 상품 N개 → 쿼리 3회 (products + options + images)
     */
    override suspend fun findAll(): List<Product> {
        val products = productRepo.findAll().toList()
        if (products.isEmpty()) return emptyList()

        val productIds = products.map { it.id!! }

        val allOptions = findOptionsByProductIds(productIds)
        val allImages = findImagesByProductIds(productIds)

        val optionsByProductId = allOptions.groupBy { it.productId }
        val imagesByProductId = allImages.groupBy { it.productId }

        return products.map { entity ->
            ProductMapper.toDomain(
                entity = entity,
                optionEntities = optionsByProductId[entity.id] ?: emptyList(),
                imageEntities = imagesByProductId[entity.id] ?: emptyList(),
            )
        }
    }

    override suspend fun update(product: Product): Product {
        val productId = product.id!!

        productRepo.save(ProductMapper.toProductEntity(product))

        optionRepo.deleteAllByProductId(productId)
        imageRepo.deleteAllByProductId(productId)

        ProductMapper.toOptionEntities(productId, product.options)
            .forEach { optionRepo.save(it) }

        ProductMapper.toImageEntities(productId, product.images)
            .forEach { imageRepo.save(it) }

        return findById(productId)!!
    }

    override suspend fun deleteById(id: Long) {
        productRepo.deleteById(id)
    }

    override suspend fun existsById(id: Long): Boolean {
        return productRepo.existsById(id)
    }

    // ── IN 쿼리로 일괄 조회 ──

    private suspend fun findOptionsByProductIds(productIds: List<Long>): List<ProductOptionEntity> {
        return databaseClient
            .sql("SELECT * FROM product_options WHERE product_id IN (:ids)")
            .bind("ids", productIds)
            .map { row, _ ->
                ProductOptionEntity(
                    id = row.get("id", java.lang.Long::class.java)?.toLong(),
                    productId = row.get("product_id", java.lang.Long::class.java)!!.toLong(),
                    size = row.get("size", String::class.java)!!,
                    color = row.get("color", String::class.java)!!,
                    additionalPrice = row.get("additional_price", java.lang.Long::class.java)?.toLong() ?: 0,
                    stockQuantity = row.get("stock_quantity", Integer::class.java)?.toInt() ?: 0,
                )
            }
            .all()
            .collectList()
            .awaitSingle()
    }

    private suspend fun findImagesByProductIds(productIds: List<Long>): List<ProductImageEntity> {
        return databaseClient
            .sql("SELECT * FROM product_images WHERE product_id IN (:ids)")
            .bind("ids", productIds)
            .map { row, _ ->
                ProductImageEntity(
                    id = row.get("id", java.lang.Long::class.java)?.toLong(),
                    productId = row.get("product_id", java.lang.Long::class.java)!!.toLong(),
                    url = row.get("url", String::class.java)!!,
                    sortOrder = row.get("sort_order", Integer::class.java)?.toInt() ?: 0,
                    primaryImage = row.get("primary_image", java.lang.Boolean::class.java)?.booleanValue() ?: false,
                )
            }
            .all()
            .collectList()
            .awaitSingle()
    }
}