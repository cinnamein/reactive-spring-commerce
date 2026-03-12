package cinnamein.reactivespringcommerce.product.application.usecase

import cinnamein.reactivespringcommerce.product.application.dto.*
import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException
import cinnamein.reactivespringcommerce.product.domain.exception.ProductNotFoundException
import cinnamein.reactivespringcommerce.product.domain.model.*
import cinnamein.reactivespringcommerce.product.domain.repository.ProductRepository
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
        status: ProductStatus = ProductStatus.DRAFT,
    ) = Product.reconstitute(
        id = id,
        name = "캐시미어 코트",
        price = 298_000,
        seller = "W컨셉",
        description = "프리미엄 캐시미어",
        status = status,
        options = listOf(ProductOption(ProductSize.M, ProductColor.BLACK, 0, 10)),
        images = listOf(ProductImage("https://cdn.example.com/main.jpg", 0, true)),
    )

    private fun createRequest() = CreateProductRequest(
        name = "캐시미어 코트",
        price = 298_000,
        seller = "W컨셉",
        description = "프리미엄 캐시미어",
        options = listOf(ProductOptionRequest(ProductSize.L, ProductColor.GRAY, 0, 10)),
        images = listOf(ProductImageRequest("https://cdn.example.com/main.jpg", 0, true)),
    )

    @Nested
    @DisplayName("createProduct")
    inner class CreateProduct {

        @Test
        @DisplayName("유효한 요청이면 저장하고 응답을 반환한다")
        fun `creates and returns response`() = runTest {
            coEvery { productRepository.save(any()) } returns testProduct()

            val response = productUseCase.createProduct(createRequest())

            assertEquals(1L, response.id)
            assertEquals("DRAFT", response.status)
            coVerify(exactly = 1) { productRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("getProduct")
    inner class GetProduct {

        @Test
        @DisplayName("존재하는 ID면 응답을 반환한다")
        fun `returns response when exists`() = runTest {
            coEvery { productRepository.findById(1L) } returns testProduct()
            val response = productUseCase.getProduct(1L)
            assertEquals(1L, response.id)
        }

        @Test
        @DisplayName("존재하지 않으면 예외가 발생한다")
        fun `throws when not found`() = runTest {
            coEvery { productRepository.findById(999L) } returns null
            assertThrows<ProductNotFoundException> { productUseCase.getProduct(999L) }
        }
    }

    @Nested
    @DisplayName("publishProduct")
    inner class PublishProduct {

        @Test
        @DisplayName("DRAFT 상태면 ON_SALE로 전이된다")
        fun `publishes DRAFT product`() = runTest {
            val draft = testProduct(status = ProductStatus.DRAFT)
            val onSale = testProduct(status = ProductStatus.ON_SALE)

            coEvery { productRepository.findById(1L) } returns draft
            coEvery { productRepository.update(any()) } returns onSale

            val response = productUseCase.publishProduct(1L)
            assertEquals("ON_SALE", response.status)
        }

        @Test
        @DisplayName("ON_SALE 상태면 예외가 발생한다")
        fun `throws when already ON_SALE`() = runTest {
            coEvery { productRepository.findById(1L) } returns testProduct(status = ProductStatus.ON_SALE)
            assertThrows<InvalidProductException> { productUseCase.publishProduct(1L) }
            coVerify(exactly = 0) { productRepository.update(any()) }
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    inner class DeleteProduct {

        @Test
        @DisplayName("존재하면 삭제한다")
        fun `deletes when exists`() = runTest {
            coEvery { productRepository.existsById(1L) } returns true
            coEvery { productRepository.deleteById(1L) } returns Unit
            productUseCase.deleteProduct(1L)
            coVerify(exactly = 1) { productRepository.deleteById(1L) }
        }

        @Test
        @DisplayName("존재하지 않으면 예외가 발생한다")
        fun `throws when not found`() = runTest {
            coEvery { productRepository.existsById(999L) } returns false
            assertThrows<ProductNotFoundException> { productUseCase.deleteProduct(999L) }
        }
    }
}