package cinnamein.reactivespringcommerce.product.domain.model

import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("ProductImage Value Object")
class ProductImageTest {

    @Test
    @DisplayName("URL이 공백이면 예외가 발생한다")
    fun `blank url throws exception`() {
        assertThrows<InvalidProductException> {
            ProductImage("  ", 0, true)
        }
    }

    @Test
    @DisplayName("정렬 순서가 음수이면 예외가 발생한다")
    fun `negative sortOrder throws exception`() {
        assertThrows<InvalidProductException> {
            ProductImage("https://cdn.example.com/a.jpg", -1, false)
        }
    }
}