package cinnamein.reactivespringcommerce.product.presentation

import cinnamein.reactivespringcommerce.common.response.ApiResponse
import cinnamein.reactivespringcommerce.product.application.dto.CreateProductRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductImageRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductOptionRequest
import cinnamein.reactivespringcommerce.product.application.dto.UpdateProductRequest
import cinnamein.reactivespringcommerce.product.domain.model.ProductColor
import cinnamein.reactivespringcommerce.product.domain.model.ProductSize
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Testcontainers
@DisplayName("Product API 통합 테스트")
class ProductIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("testdb")
            withUsername("test")
            withPassword("test")
        }

        @DynamicPropertySource
        @JvmStatic
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.r2dbc.url") {
                "r2dbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/testdb"
            }
            registry.add("spring.r2dbc.username") { "test" }
            registry.add("spring.r2dbc.password") { "test" }
        }
    }

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Autowired
    private lateinit var databaseClient: DatabaseClient

    @BeforeEach
    fun cleanUp() {
        databaseClient.sql("DELETE FROM product_images").then().block()
        databaseClient.sql("DELETE FROM product_options").then().block()
        databaseClient.sql("DELETE FROM products").then().block()
        databaseClient.sql("ALTER SEQUENCE products_id_seq RESTART WITH 1").then().block()
    }

    private fun defaultRequest(
        name: String = "캐시미어 코트",
        price: Long = 298_000,
    ) = CreateProductRequest(
        name = name,
        price = price,
        seller = "W컨셉",
        description = "프리미엄 캐시미어",
        options = listOf(ProductOptionRequest(ProductSize.M, ProductColor.BLACK, 0, 10)),
        images = listOf(ProductImageRequest("https://cdn.example.com/main.jpg", 0, true)),
    )

    private fun insertProduct(name: String = "캐시미어 코트", price: Long = 298_000): Long {
        val body = webTestClient
            .post().uri("/product")
            .bodyValue(defaultRequest(name, price))
            .exchange()
            .expectStatus().isCreated
            .expectBody<ApiResponse<Map<String, Any>>>()
            .returnResult()
            .responseBody

        return (body?.data?.get("id") as Number).toLong()
    }

    @Nested
    @DisplayName("POST /product")
    inner class CreateProduct {

        @Test
        @DisplayName("유효한 요청이면 201과 DRAFT 상태로 생성된다")
        fun `creates product with DRAFT status`() {
            webTestClient
                .post().uri("/product")
                .bodyValue(defaultRequest())
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("DRAFT")
                .jsonPath("$.data.options.length()").isEqualTo(1)
                .jsonPath("$.data.options[0].size").isEqualTo("M")
                .jsonPath("$.data.options[0].color").isEqualTo("BLACK")
                .jsonPath("$.data.images.length()").isEqualTo(1)
                .jsonPath("$.data.images[0].primaryImage").isEqualTo(true)
        }

        @Test
        @DisplayName("옵션 없이 등록하면 400이 반환된다")
        fun `empty options returns 400`() {
            val request = CreateProductRequest(
                name = "상품", price = 10_000, seller = "판매자",
                options = emptyList(),
                images = listOf(ProductImageRequest("https://cdn.example.com/a.jpg", 0, true)),
            )
            webTestClient
                .post().uri("/product")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("GET /product, /product/{id}")
    inner class ReadProduct {

        @Test
        @DisplayName("단건 조회 시 옵션과 이미지가 포함된다")
        fun `get includes options and images`() {
            val id = insertProduct()
            webTestClient
                .get().uri("/product/$id")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.options[0].colorDisplayName").isEqualTo("검정")
        }

        @Test
        @DisplayName("존재하지 않는 ID면 404가 반환된다")
        fun `returns 404 when not found`() {
            webTestClient.get().uri("/product/999").exchange().expectStatus().isNotFound
        }

        @Test
        @DisplayName("전체 조회 시 등록된 수만큼 반환된다")
        fun `get all returns correct count`() {
            insertProduct("상품A")
            insertProduct("상품B")
            webTestClient
                .get().uri("/product")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("PUT /product/{id}")
    inner class UpdateProduct {

        @Test
        @DisplayName("수정하면 변경된 값이 반환된다")
        fun `updates successfully`() {
            val id = insertProduct()
            val request = UpdateProductRequest(
                name = "캐시미어 롱코트", price = 328_000, seller = "W컨셉", description = "변경됨",
                options = listOf(ProductOptionRequest(ProductSize.L, ProductColor.RED, 5000, 8)),
                images = listOf(ProductImageRequest("https://cdn.example.com/new.jpg", 0, true)),
            )
            webTestClient
                .put().uri("/product/$id")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("캐시미어 롱코트")
                .jsonPath("$.data.options[0].size").isEqualTo("L")
        }
    }

    @Nested
    @DisplayName("상태 전이 API")
    inner class StatusTransition {

        @Test
        @DisplayName("PATCH /publish → DRAFT에서 ON_SALE로 전이된다")
        fun `publish succeeds`() {
            val id = insertProduct()
            webTestClient
                .patch().uri("/product/$id/publish")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("ON_SALE")
        }

        @Test
        @DisplayName("PATCH /sold-out → ON_SALE에서 SOLD_OUT으로 전이된다")
        fun `sold-out succeeds after publish`() {
            val id = insertProduct()
            webTestClient.patch().uri("/product/$id/publish").exchange().expectStatus().isOk
            webTestClient
                .patch().uri("/product/$id/sold-out")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.status").isEqualTo("SOLD_OUT")
        }

        @Test
        @DisplayName("DRAFT에서 sold-out하면 400이 반환된다")
        fun `sold-out from DRAFT returns 400`() {
            val id = insertProduct()
            webTestClient
                .patch().uri("/product/$id/sold-out")
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("DELETE /product/{id}")
    inner class DeleteProduct {

        @Test
        @DisplayName("삭제 후 조회하면 404가 반환된다")
        fun `deleted product is not found`() {
            val id = insertProduct()
            webTestClient.delete().uri("/product/$id").exchange().expectStatus().isOk
            webTestClient.get().uri("/product/$id").exchange().expectStatus().isNotFound
        }

        @Test
        @DisplayName("존재하지 않는 ID 삭제하면 404가 반환된다")
        fun `delete non-existent returns 404`() {
            webTestClient.delete().uri("/product/999").exchange().expectStatus().isNotFound
        }
    }
}