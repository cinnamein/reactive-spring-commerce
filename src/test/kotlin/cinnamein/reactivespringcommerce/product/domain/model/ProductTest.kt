package cinnamein.reactivespringcommerce.product.domain.model

import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("Product 도메인 모델")
class ProductTest {

    private fun defaultOptions() = listOf(
        ProductOption(ProductSize.M, ProductColor.BLACK, 0, 10),
    )

    private fun defaultImages() = listOf(
        ProductImage("https://cdn.example.com/main.jpg", 0, true),
    )

    private fun createTestProduct() = Product.create(
        name = "캐시미어 코트",
        price = 298_000,
        seller = "W컨셉",
        description = "프리미엄 캐시미어",
        options = defaultOptions(),
        images = defaultImages(),
    )

    private fun reconstitutedProduct(
        status: ProductStatus = ProductStatus.DRAFT,
    ) = Product.reconstitute(
        id = 1L,
        name = "캐시미어 코트",
        price = 298_000,
        seller = "W컨셉",
        description = "프리미엄 캐시미어",
        status = status,
        options = defaultOptions(),
        images = defaultImages(),
    )

    @Nested
    @DisplayName("create — 상품 생성")
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 상품을 생성하면 DRAFT 상태로 생성된다")
        fun `valid creation returns DRAFT product`() {
            val product = createTestProduct()

            assertEquals("캐시미어 코트", product.name)
            assertEquals(298_000, product.price)
            assertEquals(ProductStatus.DRAFT, product.status)
            assertEquals(1, product.options.size)
            assertEquals(1, product.images.size)
            assertNull(product.id)
        }

        @Test
        @DisplayName("상품명이 공백이면 예외가 발생한다")
        fun `blank name throws exception`() {
            assertThrows<InvalidProductException> {
                Product.create("  ", 10_000, "판매자", options = defaultOptions(), images = defaultImages())
            }
        }

        @Test
        @DisplayName("가격이 0이면 예외가 발생한다")
        fun `zero price throws exception`() {
            assertThrows<InvalidProductException> {
                Product.create("상품", 0, "판매자", options = defaultOptions(), images = defaultImages())
            }
        }

        @Test
        @DisplayName("옵션이 비어있으면 예외가 발생한다")
        fun `empty options throws exception`() {
            assertThrows<InvalidProductException> {
                Product.create("상품", 10_000, "판매자", options = emptyList(), images = defaultImages())
            }
        }

        @Test
        @DisplayName("대표 이미지가 없으면 예외가 발생한다")
        fun `no primary image throws exception`() {
            val images = listOf(ProductImage("https://cdn.example.com/a.jpg", 0, false))
            assertThrows<InvalidProductException> {
                Product.create("상품", 10_000, "판매자", options = defaultOptions(), images = images)
            }
        }

        @Test
        @DisplayName("대표 이미지가 2개이면 예외가 발생한다")
        fun `two primary images throws exception`() {
            val images = listOf(
                ProductImage("https://cdn.example.com/a.jpg", 0, true),
                ProductImage("https://cdn.example.com/b.jpg", 1, true),
            )
            assertThrows<InvalidProductException> {
                Product.create("상품", 10_000, "판매자", options = defaultOptions(), images = images)
            }
        }

        @Test
        @DisplayName("동일 size+color 옵션이 중복되면 예외가 발생한다")
        fun `duplicate size-color combination throws exception`() {
            val options = listOf(
                ProductOption(ProductSize.M, ProductColor.BLACK, 0, 10),
                ProductOption(ProductSize.M, ProductColor.BLACK, 1000, 5),
            )
            assertThrows<InvalidProductException> {
                Product.create("상품", 10_000, "판매자", options = options, images = defaultImages())
            }
        }
    }

    @Nested
    @DisplayName("상태 전이")
    inner class StatusTransition {

        @Test
        @DisplayName("DRAFT → ON_SALE 전이가 성공한다")
        fun `publish from DRAFT succeeds`() {
            val product = reconstitutedProduct(ProductStatus.DRAFT)
            product.publish()
            assertEquals(ProductStatus.ON_SALE, product.status)
        }

        @Test
        @DisplayName("ON_SALE 상태에서 publish하면 예외가 발생한다")
        fun `publish from ON_SALE throws exception`() {
            val product = reconstitutedProduct(ProductStatus.ON_SALE)
            assertThrows<InvalidProductException> { product.publish() }
        }

        @Test
        @DisplayName("ON_SALE → SOLD_OUT 전이가 성공한다")
        fun `soldOut from ON_SALE succeeds`() {
            val product = reconstitutedProduct(ProductStatus.ON_SALE)
            product.soldOut()
            assertEquals(ProductStatus.SOLD_OUT, product.status)
        }

        @Test
        @DisplayName("ON_SALE → DISCONTINUED 전이가 성공한다")
        fun `discontinue from ON_SALE succeeds`() {
            val product = reconstitutedProduct(ProductStatus.ON_SALE)
            product.discontinue()
            assertEquals(ProductStatus.DISCONTINUED, product.status)
        }

        @Test
        @DisplayName("SOLD_OUT → DISCONTINUED 전이가 성공한다")
        fun `discontinue from SOLD_OUT succeeds`() {
            val product = reconstitutedProduct(ProductStatus.SOLD_OUT)
            product.discontinue()
            assertEquals(ProductStatus.DISCONTINUED, product.status)
        }

        @Test
        @DisplayName("DRAFT에서 discontinue하면 예외가 발생한다")
        fun `discontinue from DRAFT throws exception`() {
            val product = reconstitutedProduct(ProductStatus.DRAFT)
            assertThrows<InvalidProductException> { product.discontinue() }
        }

        @Test
        @DisplayName("어떤 상태에서든 hide할 수 있다")
        fun `hide from any status succeeds`() {
            ProductStatus.entries.forEach { status ->
                val product = reconstitutedProduct(status)
                product.hide()
                assertEquals(ProductStatus.HIDDEN, product.status)
            }
        }
    }

    @Nested
    @DisplayName("옵션 관리")
    inner class OptionManagement {

        @Test
        @DisplayName("새 옵션을 추가할 수 있다")
        fun `addOption succeeds`() {
            val product = reconstitutedProduct()
            product.addOption(ProductOption(ProductSize.L, ProductColor.WHITE, 5000, 5))
            assertEquals(2, product.options.size)
        }

        @Test
        @DisplayName("동일 size+color 옵션을 추가하면 예외가 발생한다")
        fun `addOption with duplicate combination throws exception`() {
            val product = reconstitutedProduct()
            assertThrows<InvalidProductException> {
                product.addOption(ProductOption(ProductSize.M, ProductColor.BLACK, 1000, 5))
            }
        }

        @Test
        @DisplayName("옵션을 삭제할 수 있다")
        fun `removeOption succeeds when multiple options exist`() {
            val product = reconstitutedProduct()
            product.addOption(ProductOption(ProductSize.L, ProductColor.WHITE, 0, 5))
            product.removeOption(ProductSize.M, ProductColor.BLACK)
            assertEquals(1, product.options.size)
            assertEquals(ProductSize.L, product.options[0].size)
        }

        @Test
        @DisplayName("마지막 옵션을 삭제하면 예외가 발생한다")
        fun `removeOption on last option throws exception`() {
            val product = reconstitutedProduct()
            assertThrows<InvalidProductException> {
                product.removeOption(ProductSize.M, ProductColor.BLACK)
            }
        }
    }

    @Nested
    @DisplayName("이미지 관리")
    inner class ImageManagement {

        @Test
        @DisplayName("이미지를 추가할 수 있다")
        fun `addImage succeeds`() {
            val product = reconstitutedProduct()
            product.addImage(ProductImage("https://cdn.example.com/detail.jpg", 1, false))
            assertEquals(2, product.images.size)
        }

        @Test
        @DisplayName("새 대표 이미지 추가 시 기존 대표가 해제된다")
        fun `addImage with primaryImage replaces existing primary`() {
            val product = reconstitutedProduct()
            product.addImage(ProductImage("https://cdn.example.com/new-main.jpg", 0, true))

            val primaries = product.images.filter { it.primaryImage }
            assertEquals(1, primaries.size)
            assertEquals("https://cdn.example.com/new-main.jpg", primaries[0].url)
        }

        @Test
        @DisplayName("대표 이미지를 삭제하면 예외가 발생한다")
        fun `removeImage on primary throws exception`() {
            val product = reconstitutedProduct()
            assertThrows<InvalidProductException> {
                product.removeImage("https://cdn.example.com/main.jpg")
            }
        }
    }
}