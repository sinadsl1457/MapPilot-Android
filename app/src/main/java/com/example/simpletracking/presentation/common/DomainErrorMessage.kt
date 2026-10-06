package com.example.simpletracking.presentation.common

import androidx.annotation.StringRes
import com.example.simpletracking.R
import com.example.simpletracking.domain.result.DomainError

/**
 * 에러 → 사용자 문구 변환은 presentation의 책임이다.
 * 문자열 리소스(@StringRes)를 쓰면 values-en/strings.xml만 추가해 다국어를 지원할 수 있다.
 * sealed라서 DomainError에 새 타입을 추가하면 이 when이 컴파일 에러로 알려준다.
 */
@StringRes
fun DomainError.messageRes(): Int = when (this) {
    DomainError.Network -> R.string.error_network
    DomainError.Unauthorized -> R.string.error_unauthorized
    DomainError.QuotaExceeded -> R.string.error_quota_exceeded
    is DomainError.Server -> R.string.error_server
    DomainError.EmptyQuery -> R.string.error_empty_query
    DomainError.DestinationTooClose -> R.string.error_destination_too_close
    is DomainError.RouteNotFound -> R.string.error_route_not_found
    DomainError.LocationPermissionDenied -> R.string.error_location_permission_denied
    is DomainError.Unknown -> R.string.error_unknown
}
