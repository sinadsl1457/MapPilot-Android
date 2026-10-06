package com.example.simpletracking.domain.result

/**
 * Flow 전용 실패 신호.
 * - suspend 함수(한 번 호출) → AppResult로 반환
 * - Flow(스트림)           → 값은 그대로 흘리고, 실패하면 이 예외로 스트림을 종료
 * 어느 쪽이든 presentation이 보는 에러 타입은 DomainError 하나로 통일된다.
 */
class DomainException(val error: DomainError) : Exception(error.toString())
