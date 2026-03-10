package cinnamein.reactivespringcommerce.product.presentation

import cinnamein.reactivespringcommerce.common.response.ApiResponse
import cinnamein.reactivespringcommerce.product.application.dto.CreateProductRequest
import cinnamein.reactivespringcommerce.product.application.dto.UpdateProductRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
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
        databaseClient.sql("DELETE FROM products").then().block()
        databaseClient.sql("ALTER SEQUENCE products_id_seq RESTART WITH 1").then().block()
    }

    private fun createProductRequest(
        name: String = "캐시미어 코트",
        price: Long = 298_000,
        seller: String = "W컨셉",
    ) = CreateProductRequest(name = name, price = price, seller = seller)

    private fun insertProduct(
        name: String = "캐시미어 코트",
        price: Long = 298_000,
        seller: String = "W컨셉",
    ): Long {
        val body = webTestClient
            .post().uri("/product")
            .bodyValue(createProductRequest(name, price, seller))
            .exchange()
            .expectStatus().isCreated
            .expectBody<ApiResponse<Map<String, Any>>>()
            .returnResult()
            .responseBody

        return (body?.data?.get("id") as Number).toLong()
    }

    // ──────────────────────────────────────────
    // POST /product
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("POST /product — 상품 등록")
    inner class CreateProduct {

        @Test
        @DisplayName("유효한 요청이면 201 Created와 함께 상품이 반환된다")
        fun `creates product successfully`() {
            webTestClient
                .post().uri("/product")
                .bodyValue(createProductRequest())
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo("생성 완료")
                .jsonPath("$.data.id").isNotEmpty
                .jsonPath("$.data.name").isEqualTo("캐시미어 코트")
                .jsonPath("$.data.price").isEqualTo(298_000)
                .jsonPath("$.data.seller").isEqualTo("W컨셉")
        }

        @Test
        @DisplayName("상품명이 비어있으면 400 Bad Request가 반환된다")
        fun `blank name returns 400`() {
            webTestClient
                .post().uri("/product")
                .bodyValue(createProductRequest(name = "  "))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
        }

        @Test
        @DisplayName("가격이 0이면 400 Bad Request가 반환된다")
        fun `zero price returns 400`() {
            webTestClient
                .post().uri("/product")
                .bodyValue(createProductRequest(price = 0))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
        }
    }

    // ──────────────────────────────────────────
    // GET /product/{id}
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /product/{id} — 상품 단건 조회")
    inner class GetProduct {

        @Test
        @DisplayName("존재하는 상품 ID로 조회하면 200과 함께 상품이 반환된다")
        fun `returns product when exists`() {
            val id = insertProduct()

            webTestClient
                .get().uri("/product/$id")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.id").isEqualTo(id.toInt())
                .jsonPath("$.data.name").isEqualTo("캐시미어 코트")
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 404가 반환된다")
        fun `returns 404 when not found`() {
            webTestClient
                .get().uri("/product/999")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
        }
    }

    // ──────────────────────────────────────────
    // GET /product
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("GET /product — 상품 전체 조회")
    inner class GetAllProducts {

        @Test
        @DisplayName("상품이 없으면 빈 리스트가 반환된다")
        fun `returns empty list when no products`() {
            webTestClient
                .get().uri("/product")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(0)
        }

        @Test
        @DisplayName("등록된 상품 수만큼 반환된다")
        fun `returns all products`() {
            insertProduct(name = "상품A")
            insertProduct(name = "상품B")
            insertProduct(name = "상품C")

            webTestClient
                .get().uri("/product")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(3)
        }
    }

    // ──────────────────────────────────────────
    // PUT /product/{id}
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("PUT /product/{id} — 상품 수정")
    inner class UpdateProduct {

        @Test
        @DisplayName("존재하는 상품을 유효한 값으로 수정하면 200과 변경된 상품이 반환된다")
        fun `updates product successfully`() {
            val id = insertProduct()
            val updateRequest = UpdateProductRequest(
                name = "캐시미어 롱코트",
                price = 328_000,
                seller = "W컨셉",
            )

            webTestClient
                .put().uri("/product/$id")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("캐시미어 롱코트")
                .jsonPath("$.data.price").isEqualTo(328_000)
        }

        @Test
        @DisplayName("존재하지 않는 상품을 수정하려 하면 404가 반환된다")
        fun `returns 404 when product not found`() {
            val updateRequest = UpdateProductRequest(
                name = "상품", price = 10_000, seller = "판매자"
            )

            webTestClient
                .put().uri("/product/999")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("유효하지 않은 값으로 수정하면 400이 반환된다")
        fun `returns 400 for invalid update`() {
            val id = insertProduct()
            val updateRequest = UpdateProductRequest(
                name = "", price = 10_000, seller = "판매자"
            )

            webTestClient
                .put().uri("/product/$id")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("수정 실패 시 기존 데이터가 유지된다")
        fun `failed update does not change original data`() {
            val id = insertProduct(name = "원래 상품", price = 10_000)
            val updateRequest = UpdateProductRequest(
                name = "", price = 20_000, seller = "판매자"
            )

            webTestClient
                .put().uri("/product/$id")
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isBadRequest

            webTestClient
                .get().uri("/product/$id")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.name").isEqualTo("원래 상품")
                .jsonPath("$.data.price").isEqualTo(10_000)
        }
    }

    // ──────────────────────────────────────────
    // DELETE /product/{id}
    // ──────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /product/{id} — 상품 삭제")
    inner class DeleteProduct {

        @Test
        @DisplayName("존재하는 상품을 삭제하면 200이 반환된다")
        fun `deletes product successfully`() {
            val id = insertProduct()

            webTestClient
                .delete().uri("/product/$id")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.message").isEqualTo("삭제 완료")
        }

        @Test
        @DisplayName("삭제 후 해당 상품을 조회하면 404가 반환된다")
        fun `deleted product is not found`() {
            val id = insertProduct()

            webTestClient
                .delete().uri("/product/$id")
                .exchange()
                .expectStatus().isOk

            webTestClient
                .get().uri("/product/$id")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("존재하지 않는 상품을 삭제하려 하면 404가 반환된다")
        fun `returns 404 when product not found`() {
            webTestClient
                .delete().uri("/product/999")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        @DisplayName("삭제 후 전체 목록에서도 제외된다")
        fun `deleted product is excluded from list`() {
            val id1 = insertProduct(name = "상품A")
            insertProduct(name = "상품B")

            webTestClient
                .delete().uri("/product/$id1")
                .exchange()
                .expectStatus().isOk

            webTestClient
                .get().uri("/product")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.data.length()").isEqualTo(1)
                .jsonPath("$.data[0].name").isEqualTo("상품B")
        }
    }
}