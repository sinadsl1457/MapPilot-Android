package com.example.simpletracking.presentation.map.search

import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.util.GeoMath

/** 이 거리 안에 있는 결과끼리를 한 무리로 본다. */
const val RESULT_CLUSTER_RADIUS_METERS = 3_000.0

/**
 * 검색 결과 중 카메라를 맞출 지점들.
 * 결과가 여러 도시에 흩어져 있을 때(예: "상상마당" → 홍대·부천·춘천) 전부 맞추면 너무 멀리 줌아웃되므로,
 * 반경 안에 이웃이 가장 많은 결과를 골라 그 주변만 맞춘다. 개수가 같으면 검색 순위가 높은(앞쪽) 결과를 고른다.
 * 나머지 결과도 마커와 목록에는 그대로 남는다.
 */
fun densestCluster(points: List<GeoPoint>, radiusMeters: Double = RESULT_CLUSTER_RADIUS_METERS): List<GeoPoint> {
    if (points.size <= 1) return points
    fun neighborsOf(center: GeoPoint) = points.filter { GeoMath.distanceMeters(center, it) <= radiusMeters }
    // maxBy는 최댓값이 여럿이면 처음 것을 돌려준다 → 앞 순위 우선
    val center = points.maxBy { neighborsOf(it).size }
    return neighborsOf(center)
}
