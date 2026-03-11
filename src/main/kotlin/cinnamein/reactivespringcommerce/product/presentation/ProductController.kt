package cinnamein.reactivespringcommerce.product.presentation

import cinnamein.reactivespringcommerce.common.response.ApiResponse
import cinnamein.reactivespringcommerce.product.application.dto.CreateProductRequest
import cinnamein.reactivespringcommerce.product.application.dto.ProductResponse
import cinnamein.reactivespringcommerce.product.application.dto.UpdateProductRequest
import cinnamein.reactivespringcommerce.product.application.usecase.ProductUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/product")
class ProductController(
    private val productUseCase: ProductUseCase,
) {

    @PostMapping
    suspend fun createProduct(
        @RequestBody request: CreateProductRequest,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.createProduct(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.created(result))
    }

    @GetMapping("/{id}")
    suspend fun getProduct(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.getProduct(id)
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @GetMapping
    suspend fun getAllProducts(): ResponseEntity<ApiResponse<List<ProductResponse>>> {
        val result = productUseCase.getAllProducts()
        return ResponseEntity.ok(ApiResponse.ok(result))
    }

    @PutMapping("/{id}")
    suspend fun updateProduct(
        @PathVariable id: Long,
        @RequestBody request: UpdateProductRequest,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.updateProduct(id, request)
        return ResponseEntity.ok(ApiResponse.ok(result, "수정 완료"))
    }

    @DeleteMapping("/{id}")
    suspend fun deleteProduct(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Unit>> {
        productUseCase.deleteProduct(id)
        return ResponseEntity.ok(ApiResponse.noContent())
    }

    @PatchMapping("/{id}/publish")
    suspend fun publishProduct(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.publishProduct(id)
        return ResponseEntity.ok(ApiResponse.ok(result, "판매 개시"))
    }

    @PatchMapping("/{id}/sold-out")
    suspend fun soldOutProduct(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.soldOutProduct(id)
        return ResponseEntity.ok(ApiResponse.ok(result, "품절 처리"))
    }

    @PatchMapping("/{id}/discontinue")
    suspend fun discontinueProduct(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.discontinueProduct(id)
        return ResponseEntity.ok(ApiResponse.ok(result, "판매 중지"))
    }

    @PatchMapping("/{id}/hide")
    suspend fun hideProduct(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ProductResponse>> {
        val result = productUseCase.hideProduct(id)
        return ResponseEntity.ok(ApiResponse.ok(result, "숨김 처리"))
    }
}