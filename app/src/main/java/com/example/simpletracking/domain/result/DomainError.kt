package com.example.simpletracking.domain.result

/**
 * 에러를 문자열이 아니라 타입으로 표현한다.
 * - UI가 종류별로 다르게 반응할 수 있다(재시도 버튼, 설정 안내 등).
 * - 사용자에게 보여줄 문구는 presentation이 정한다(다국어 대응).
 * sealed이므로 when에서 분기가 빠지면 컴파일 에러가 난다.
 */
sealed interface DomainError {
    data object Network : DomainError
    data object Unauthorized : DomainError
    data object QuotaExceeded : DomainError
    data class Server(val code: Int) : DomainError

    data object EmptyQuery : DomainError
    data object DestinationTooClose : DomainError
    data class RouteNotFound(val reason: String) : DomainError

    data object LocationPermissionDenied : DomainError

    data class Unknown(val cause: Throwable? = null) : DomainError
}
