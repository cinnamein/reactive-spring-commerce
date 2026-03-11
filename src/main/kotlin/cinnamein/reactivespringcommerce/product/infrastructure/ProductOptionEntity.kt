package cinnamein.reactivespringcommerce.product.infrastructure

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("product_options")
data class ProductOptionEntity(
    @Id val id: Long? = null,
    val productId: Long,
    val size: String,
    val color: String,
    val additionalPrice: Long = 0,
    val stockQuantity: Int = 0,
)