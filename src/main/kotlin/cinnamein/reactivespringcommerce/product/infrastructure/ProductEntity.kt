package cinnamein.reactivespringcommerce.product.infrastructure

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("products")
data class ProductEntity(
    @Id
    val id: Long? = null,
    val name: String,
    val price: Long,
    val seller: String,
    val description: String = "",
    val status: String = "DRAFT",
)