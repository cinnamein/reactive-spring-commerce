package cinnamein.reactivespringcommerce.product.application.usecase

import cinnamein.reactivespringcommerce.product.application.dto.CreateProductRequest
import cinnamein.reactivespringcommerce.product.application.dto.UpdateProductRequest
import cinnamein.reactivespringcommerce.product.domain.InvalidProductException
import cinnamein.reactivespringcommerce.product.domain.Product
import cinnamein.reactivespringcommerce.product.domain.ProductNotFoundException
import cinnamein.reactivespringcommerce.product.domain.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@DisplayName("ProductUseCase")
class ProductUseCaseTest {

    private lateinit var productRepository: ProductRepository
    private lateinit var productUseCase: ProductUseCase

    @BeforeEach
    fun setUp() {
        productRepository = mockk()
        productUseCase = ProductUseCase(productRepository)
    }

    private fun testProduct(
        id: Long = 1L,
        name: String = "캐시미어 코트",
        price: Long = 298_000,
        seller: String = "W컨셉",
    ): Product = Product.reconstitute(id = id, name = name, price = price, seller = seller)

    @Nested
    @DisplayName("createProduct — 상품 등록")
    inner class CreateProduct {

        @Test
        @DisplayName("유효한 요청이면 상품을 저장하고 응답을 반환한다")
        fun `creates product and returns response`() = runTest {
            val request = CreateProductRequest(name = "캐시미어 코트", price = 298_000, seller = "W컨셉")
            val saved = testProduct()

            coEvery { productRepository.save(any()) } returns saved

            val response = productUseCase.createProduct(request)

            assertEquals(1L, response.id)
            assertEquals("캐시미어 코트", response.name)
            assertEquals(298_000, response.price)
            assertEquals("W컨셉", response.seller)
            coVerify(exactly = 1) { productRepository.save(any()) }
        }

        @Test
        @DisplayName("가격이 0이면 Repository 호출 없이 도메인 예외가 발생한다")
        fun `invalid price does not call repository`() = runTest {
            val request = CreateProductRequest(name = "상품", price = 0, seller = "판매자")

            assertThrows<InvalidProductException> {
                productUseCase.createProduct(request)
            }
            coVerify(exactly = 0) { productRepository.save(any()) }
        }

        @Test
        @DisplayName("상품명이 비어있으면 Repository 호출 없이 도메인 예외가 발생한다")
        fun `blank name does not call repository`() = runTest {
            val request = CreateProductRequest(name = "  ", price = 10_000, seller = "판매자")

            assertThrows<InvalidProductException> {
                productUseCase.createProduct(request)
            }
            coVerify(exactly = 0) { productRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("getProduct — 상품 단건 조회")
    inner class GetProduct {

        @Test
        @DisplayName("존재하는 ID로 조회하면 상품 응답을 반환한다")
        fun `returns product when exists`() = runTest {
            val product = testProduct()
            coEvery { productRepository.findById(1L) } returns product

            val response = productUseCase.getProduct(1L)

            assertEquals(1L, response.id)
            assertEquals("캐시미어 코트", response.name)
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 ProductNotFoundException이 발생한다")
        fun `throws exception when not found`() = runTest {
            coEvery { productRepository.findById(999L) } returns null

            assertThrows<ProductNotFoundException> {
                productUseCase.getProduct(999L)
            }
        }
    }

    @Nested
    @DisplayName("getAllProducts — 상품 전체 조회")
    inner class GetAllProducts {

        @Test
        @DisplayName("상품이 있으면 전체 목록을 반환한다")
        fun `returns all products`() = runTest {
            val products = listOf(
                testProduct(id = 1L, name = "상품A"),
                testProduct(id = 2L, name = "상품B"),
            )
            coEvery { productRepository.findAll() } returns products

            val result = productUseCase.getAllProducts()

            assertEquals(2, result.size)
            assertEquals("상품A", result[0].name)
            assertEquals("상품B", result[1].name)
        }

        @Test
        @DisplayName("상품이 없으면 빈 리스트를 반환한다")
        fun `returns empty list when no products`() = runTest {
            coEvery { productRepository.findAll() } returns emptyList()

            val result = productUseCase.getAllProducts()

            assertEquals(0, result.size)
        }
    }

    @Nested
    @DisplayName("updateProduct — 상품 수정")
    inner class UpdateProduct {

        @Test
        @DisplayName("존재하는 상품을 수정하면 변경된 응답을 반환한다")
        fun `updates product and returns response`() = runTest {
            val existing = testProduct()
            val updated = testProduct(name = "캐시미어 롱코트", price = 328_000)
            val request = UpdateProductRequest(name = "캐시미어 롱코트", price = 328_000, seller = "W컨셉")

            coEvery { productRepository.findById(1L) } returns existing
            coEvery { productRepository.update(any()) } returns updated

            val response = productUseCase.updateProduct(1L, request)

            assertEquals("캐시미어 롱코트", response.name)
            assertEquals(328_000, response.price)
            coVerify(exactly = 1) { productRepository.update(any()) }
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하려 하면 ProductNotFoundException이 발생한다")
        fun `throws exception when product not found`() = runTest {
            val request = UpdateProductRequest(name = "상품", price = 10_000, seller = "판매자")
            coEvery { productRepository.findById(999L) } returns null

            assertThrows<ProductNotFoundException> {
                productUseCase.updateProduct(999L, request)
            }
            coVerify(exactly = 0) { productRepository.update(any()) }
        }

        @Test
        @DisplayName("유효하지 않은 값으로 수정하면 도메인 예외가 발생하고 저장되지 않는다")
        fun `invalid update does not save`() = runTest {
            val existing = testProduct()
            val request = UpdateProductRequest(name = "", price = 10_000, seller = "판매자")

            coEvery { productRepository.findById(1L) } returns existing

            assertThrows<InvalidProductException> {
                productUseCase.updateProduct(1L, request)
            }
            coVerify(exactly = 0) { productRepository.update(any()) }
        }
    }

    @Nested
    @DisplayName("deleteProduct — 상품 삭제")
    inner class DeleteProduct {

        @Test
        @DisplayName("존재하는 상품을 삭제하면 성공한다")
        fun `deletes product when exists`() = runTest {
            coEvery { productRepository.existsById(1L) } returns true
            coEvery { productRepository.deleteById(1L) } returns Unit

            productUseCase.deleteProduct(1L)

            coVerify(exactly = 1) { productRepository.deleteById(1L) }
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하려 하면 ProductNotFoundException이 발생한다")
        fun `throws exception when product not found`() = runTest {
            coEvery { productRepository.existsById(999L) } returns false

            assertThrows<ProductNotFoundException> {
                productUseCase.deleteProduct(999L)
            }
            coVerify(exactly = 0) { productRepository.deleteById(any()) }
        }
    }
}