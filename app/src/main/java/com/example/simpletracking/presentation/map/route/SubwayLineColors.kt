package com.example.simpletracking.presentation.map.route

import com.example.simpletracking.domain.model.TransitLeg

/** 수도권 전철 노선 공식 색(0xRRGGBB). 목록(TransitRouteList)과 지도 경로선(RouteUiState.overlay)이 같이 쓴다. */
object SubwayLineColors {
    /** 지하철 구간의 노선 색. 지하철이 아니거나 모르는 노선이면 null → 기본 지하철 색을 쓴다. */
    fun rgb(leg: TransitLeg): Int? =
        if (leg.kind == TransitLeg.Kind.SUBWAY) leg.lineNames.firstNotNullOfOrNull(::rgb) else null

    /** 카카오 응답의 노선 이름(예: "2호선", "수인분당선")으로 찾는다. */
    fun rgb(lineName: String): Int? {
        val name = lineName.replace(" ", "")
        // "인천1호선"이 "1호선"에 걸리지 않도록 이름 있는 노선부터 본다.
        NAMED_LINES.firstOrNull { (key, _) -> key in name }?.let { return it.second }
        val number = NUMBERED.find(name)?.groupValues?.get(1)?.toInt() ?: return null
        return NUMBERED_LINES[number]
    }

    private val NUMBERED = Regex("^(?:수도권|서울)?([1-9])호선")

    private val NUMBERED_LINES = mapOf(
        1 to 0x0052A4, 2 to 0x00A84D, 3 to 0xEF7C1C, 4 to 0x00A5DE, 5 to 0x996CAC,
        6 to 0xCD7C2F, 7 to 0x747F00, 8 to 0xE6186C, 9 to 0xBDB092,
    )

    private val NAMED_LINES = listOf(
        "인천1호선" to 0x7CA8D5,
        "인천2호선" to 0xED8B00,
        "수인분당" to 0xF5A200,
        "신분당" to 0xD4003B,
        "경의중앙" to 0x77C4A3,
        "공항철도" to 0x0090D2,
        "경춘" to 0x0C8E72,
        "경강" to 0x0054A6,
        "서해" to 0x81A914,
        "우이신설" to 0xB0CE18,
        "신림" to 0x6789CA,
        "김포골드" to 0xA17800,
        "에버라인" to 0x6FB245,
        "용인경전철" to 0x6FB245,
        "의정부" to 0xFDA600,
        "GTX-A" to 0x9A6292,
    )
}
