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
}