package cinnamein.reactivespringcommerce.product.domain.model

import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

@DisplayName("ProductOption Value Object")
class ProductOptionTest {

    @Test
    @DisplayName("유효한 값으로 생성하면 성공한다")
    fun `valid creation`() {
        val option = ProductOption(ProductSize.M, ProductColor.BLACK, 0, 10)
        assertEquals(ProductSize.M, option.size)
        assertEquals(ProductColor.BLACK, option.color)
    }

    @Test
    @DisplayName("추가 가격이 음수이면 예외가 발생한다")
    fun `negative additionalPrice throws exception`() {
        assertThrows<InvalidProductException> {
            ProductOption(ProductSize.M, ProductColor.BLACK, -1, 10)
        }
    }

    @Test
    @DisplayName("재고가 음수이면 예외가 발생한다")
    fun `negative stockQuantity throws exception`() {
        assertThrows<InvalidProductException> {
            ProductOption(ProductSize.M, ProductColor.BLACK, 0, -1)
        }
    }

    @Test
    @DisplayName("동일 값이면 동등하다 (Value Object)")
    fun `same values are equal`() {
        val a = ProductOption(ProductSize.M, ProductColor.BLACK, 0, 10)
        val b = ProductOption(ProductSize.M, ProductColor.BLACK, 0, 10)
        assertEquals(a, b)
    }
}