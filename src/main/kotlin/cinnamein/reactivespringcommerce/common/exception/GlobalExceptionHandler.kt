package cinnamein.reactivespringcommerce.common.exception

import cinnamein.reactivespringcommerce.common.response.ApiResponse
import cinnamein.reactivespringcommerce.product.domain.exception.InvalidProductException
import cinnamein.reactivespringcommerce.product.domain.exception.ProductNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ProductNotFoundException::class)
    suspend fun handleNotFound(
        ex: ProductNotFoundException
    ): ResponseEntity<ApiResponse<Unit>> {
        log.warn("Product not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(404, ex.message ?: "리소스를 찾을 수 없습니다."))
    }

    @ExceptionHandler(InvalidProductException::class)
    suspend fun handleInvalidProduct(
        ex: InvalidProductException
    ): ResponseEntity<ApiResponse<Unit>> {
        log.warn("Invalid product: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, ex.message ?: "잘못된 상품 정보입니다."))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    suspend fun handleMessageNotReadable(
        ex: HttpMessageNotReadableException
    ): ResponseEntity<ApiResponse<Unit>> {
        log.warn("Malformed request body: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "잘못된 요청입니다. 허용되지 않는 값이 포함되어 있습니다."))
    }

    @ExceptionHandler(WebExchangeBindException::class)
    suspend fun handleValidation(
        ex: WebExchangeBindException
    ): ResponseEntity<ApiResponse<Unit>> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, message))
    }

    @ExceptionHandler(ServerWebInputException::class)
    suspend fun handleBadInput(
        ex: ServerWebInputException
    ): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(400, "잘못된 요청 형식입니다."))
    }

    @ExceptionHandler(Exception::class)
    suspend fun handleUnexpected(
        ex: Exception
    ): ResponseEntity<ApiResponse<Unit>> {
        log.error("Unexpected error", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다."))
    }
}