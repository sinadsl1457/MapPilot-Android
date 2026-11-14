package com.example.simpletracking.presentation.map.route

import androidx.compose.ui.graphics.Color
import com.example.simpletracking.domain.model.TransitLeg
import com.example.simpletracking.presentation.map.RouteLine

/** 길찾기 화면과 지도 경로 선이 같은 색을 쓰도록 한곳에 모은다. */
object RouteColors {
    val Car = Color(0xFF356DF3)
    val Walk = Color(0xFF8E949B)
    val Bus = Color(0xFF1FA463)
    val Subway = Color(0xFF6A5AE0)
    val Start = Car
    val End = Color(0xFFF2464B)

    fun line(kind: RouteLine.Kind): Color = when (kind) {
        RouteLine.Kind.CAR -> Car
        RouteLine.Kind.WALK -> Walk
        RouteLine.Kind.BUS -> Bus
        RouteLine.Kind.SUBWAY -> Subway
    }

    /** 구간 색. 지하철은 노선 색, 모르는 노선이면 종류 기본색. 지도 경로선과 같은 색이다. */
    fun leg(leg: TransitLeg): Color = SubwayLineColors.rgb(leg)?.let { Color(0xFF000000 or it.toLong()) } ?: leg(leg.kind)

    fun leg(kind: TransitLeg.Kind): Color = when (kind) {
        TransitLeg.Kind.WALK -> Walk
        TransitLeg.Kind.BUS -> Bus
        TransitLeg.Kind.SUBWAY -> Subway
    }
}
