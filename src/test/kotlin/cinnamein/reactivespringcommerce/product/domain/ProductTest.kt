package cinnamein.reactivespringcommerce.product.domain

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertNull

@DisplayName("Product 도메인 모델")
class ProductTest {

    @Nested
    @DisplayName("create — 상품 생성")
    inner class Create {

        @Test
        @DisplayName("유효한 값으로 상품을 생성하면 성공한다")
        fun `valid product creation`() {
            val product = Product.create(
                name = "캐시미어 코트",
                price = 298_000,
                seller = "W컨셉",
            )

            assertEquals("캐시미어 코트", product.name)
            assertEquals(298_000, product.price)
            assertEquals("W컨셉", product.seller)
            assertNull(product.id)
        }

        @Test
        @DisplayName("생성 시 상품명과 판매자의 앞뒤 공백을 제거한다")
        fun `trims name and seller`() {
            val product = Product.create(
                name = "  캐시미어 코트  ",
                price = 298_000,
                seller = "  W컨셉  ",
            )

            assertEquals("캐시미어 코트", product.name)
            assertEquals("W컨셉", product.seller)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   ", "\t", "\n"])
        @DisplayName("상품명이 공백이면 InvalidProductException이 발생한다")
        fun `blank name throws exception`(blankName: String) {
            assertThrows<InvalidProductException> {
                Product.create(name = blankName, price = 10_000, seller = "판매자")
            }
        }

        @ParameterizedTest
        @ValueSource(longs = [0, -1, -100, Long.MIN_VALUE])
        @DisplayName("가격이 0 이하이면 InvalidProductException이 발생한다")
        fun `zero or negative price throws exception`(invalidPrice: Long) {
            assertThrows<InvalidProductException> {
                Product.create(name = "상품", price = invalidPrice, seller = "판매자")
            }
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "   ", "\t"])
        @DisplayName("판매자가 공백이면 InvalidProductException이 발생한다")
        fun `blank seller throws exception`(blankSeller: String) {
            assertThrows<InvalidProductException> {
                Product.create(name = "상품", price = 10_000, seller = blankSeller)
            }
        }

        @Test
        @DisplayName("가격 경계값 — 1원은 허용된다")
        fun `minimum valid price is 1`() {
            val product = Product.create(name = "상품", price = 1, seller = "판매자")
            assertEquals(1, product.price)
        }

        @Test
        @DisplayName("가격 경계값 — Long 최대값도 허용된다")
        fun `maximum valid price is Long MAX`() {
            val product = Product.create(name = "상품", price = Long.MAX_VALUE, seller = "판매자")
            assertEquals(Long.MAX_VALUE, product.price)
        }
    }

    @Nested
    @DisplayName("reconstitute — DB 복원")
    inner class Reconstitute {

        @Test
        @DisplayName("DB에서 복원한 엔티티는 ID를 가진다")
        fun `reconstituted product has id`() {
            val product = Product.reconstitute(
                id = 1L,
                name = "상품",
                price = 10_000,
                seller = "판매자",
            )

            assertEquals(1L, product.id)
            assertEquals("상품", product.name)
        }

        @Test
        @DisplayName("reconstitute는 검증을 수행하지 않는다")
        fun `reconstitute skips validation`() {
            // DB에 이미 저장된 데이터이므로 검증 없이 복원 가능해야 함
            val product = Product.reconstitute(
                id = 1L,
                name = "상품",
                price = 10_000,
                seller = "판매자",
            )
            assertEquals(10_000, product.price)
        }
    }

    @Nested
    @DisplayName("update — 상품 수정")
    inner class Update {

        private fun createTestProduct(): Product = Product.reconstitute(
            id = 1L, name = "기존 상품", price = 10_000, seller = "기존 판매자"
        )

        @Test
        @DisplayName("유효한 값으로 수정하면 필드가 변경된다")
        fun `valid update changes fields`() {
            val product = createTestProduct()

            product.update(name = "변경된 상품", price = 20_000, seller = "변경된 판매자")

            assertEquals("변경된 상품", product.name)
            assertEquals(20_000, product.price)
            assertEquals("변경된 판매자", product.seller)
            assertEquals(1L, product.id) // ID는 변하지 않음
        }

        @Test
        @DisplayName("수정 시에도 앞뒤 공백을 제거한다")
        fun `update trims name and seller`() {
            val product = createTestProduct()

            product.update(name = "  새 이름  ", price = 15_000, seller = "  새 판매자  ")

            assertEquals("새 이름", product.name)
            assertEquals("새 판매자", product.seller)
        }

        @Test
        @DisplayName("수정 시 빈 상품명이면 예외가 발생한다")
        fun `update with blank name throws exception`() {
            val product = createTestProduct()

            assertThrows<InvalidProductException> {
                product.update(name = "", price = 20_000, seller = "판매자")
            }
            // 실패 시 기존 값이 유지되어야 함
            assertEquals("기존 상품", product.name)
        }

        @Test
        @DisplayName("수정 시 가격이 0이면 예외가 발생한다")
        fun `update with zero price throws exception`() {
            val product = createTestProduct()

            assertThrows<InvalidProductException> {
                product.update(name = "상품", price = 0, seller = "판매자")
            }
            assertEquals(10_000, product.price)
        }
    }
}