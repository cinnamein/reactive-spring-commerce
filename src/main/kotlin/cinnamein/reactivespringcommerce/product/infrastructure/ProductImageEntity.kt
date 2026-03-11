package cinnamein.reactivespringcommerce.product.infrastructure

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("product_images")
data class ProductImageEntity(
    @Id val id: Long? = null,
    val productId: Long,
    val url: String,
    val sortOrder: Int = 0,
    val primaryImage: Boolean = false,
)