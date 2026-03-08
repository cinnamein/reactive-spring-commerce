package cinnamein.reactivespringcommerce.product.domain

interface ProductRepository {
    suspend fun save(product: Product): Product
    suspend fun findById(id: Long): Product?
    suspend fun findAll(): List<Product>
    suspend fun update(product: Product): Product
    suspend fun deleteById(id: Long)
    suspend fun existsById(id: Long): Boolean
}