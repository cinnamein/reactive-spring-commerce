package cinnamein.reactivespringcommerce.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val status: Int,
    val message: String,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    companion object {
        fun <T> ok(data: T, message: String = "성공"): ApiResponse<T> =
            ApiResponse(status = 200, message = message, data = data)

        fun <T> created(data: T, message: String = "생성 완료"): ApiResponse<T> =
            ApiResponse(status = 201, message = message, data = data)

        fun noContent(message: String = "삭제 완료"): ApiResponse<Unit> =
            ApiResponse(status = 200, message = message)

        fun error(status: Int, message: String): ApiResponse<Unit> =
            ApiResponse(status = status, message = message)
    }
}