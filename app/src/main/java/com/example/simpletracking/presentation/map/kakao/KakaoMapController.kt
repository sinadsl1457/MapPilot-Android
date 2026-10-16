package com.example.simpletracking.presentation.map.kakao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.presentation.map.MapFitLayout
import com.example.simpletracking.presentation.map.PlaceMarker
import com.example.simpletracking.presentation.map.RouteLine
import com.example.simpletracking.presentation.map.RouteOverlay
import com.example.simpletracking.presentation.map.route.RouteColors
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.CompetitionType
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelLayerOptions
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.OrderingType
import com.kakao.vectormap.label.TransformMethod
import com.kakao.vectormap.route.RouteLineLayer
import com.kakao.vectormap.route.RouteLineOptions
import com.kakao.vectormap.route.RouteLineSegment
import com.kakao.vectormap.route.RouteLineStyle
import com.kakao.vectormap.route.RouteLineStyles
import com.kakao.vectormap.route.RouteLineStylesSet
import kotlin.math.PI

/**
 * 카카오맵의 명령형 API(addLabel, moveCamera...)를 한곳에 모은 어댑터.
 * 카카오 SDK 타입(LatLng, Label...)은 이 패키지 밖으로 나가지 않는다.
 * → 나중에 네이버/구글 지도로 바꿔도 이 패키지만 교체하면 된다.
 */
internal class KakaoMapController(
    private val context: Context,
    private val map: KakaoMap,
    private val state: KakaoMapState,
) : MapCommands {
    private val density = context.resources.displayMetrics.density
    private val labelManager = requireNotNull(map.labelManager) { "LabelManager is not ready" }
    private val routeLineManager = requireNotNull(map.routeLineManager) { "RouteLineManager is not ready" }

    // 레이어 순서(아래 → 위): 경로 선 < 검색 결과 < 출발·도착 < 내 위치
    private val routeLayer: RouteLineLayer = requireNotNull(routeLineManager.addLayer(ROUTE_LAYER, 8_000))
    private val placeLayer: LabelLayer = addLabelLayer(PLACE_LAYER, zOrder = 9_000, clickable = true)
    private val endpointLayer: LabelLayer = addLabelLayer(ENDPOINT_LAYER, zOrder = 9_500)
    private val myLocationLayer: LabelLayer = addLabelLayer(MY_LOCATION_LAYER, zOrder = 10_000)

    private val myDotStyles = labelStyles("me_dot", context.bitmap(R.drawable.ic_marker_me), anchorY = 0.5f)
    private val myHeadingStyles = labelStyles("me_heading", context.bitmap(R.drawable.ic_marker_me_heading), anchorY = 0.5f)
    private val routeStartStyles = bubbleStyles("route_start", R.string.route_marker_start, RouteColors.Start.toArgb())
    private val routeEndStyles = bubbleStyles("route_end", R.string.route_marker_end, RouteColors.End.toArgb())
    private val routeStyles: Map<RouteLine.Kind, RouteLineStylesSet> = RouteLine.Kind.entries.associateWith { kind ->
        val width = if (kind == RouteLine.Kind.WALK) 5f else 8f
        requireNotNull(
            routeLineManager.addStylesSet(
                RouteLineStylesSet.from(
                    "route_${kind.name.lowercase()}",
                    RouteLineStyles.from(RouteLineStyle.from(width * density, RouteColors.line(kind).toArgb(), 2 * density, Color.WHITE)),
                )
            )
        ) { "Failed to register route style: $kind" }
    }
    private val placeStyleCache = mutableMapOf<Pair<Int, Boolean>, LabelStyles>()

    private var myLabel: Label? = null
    /** 마지막으로 적용한 회전값(라디안). 359° → 1°처럼 경계를 넘을 때 반대로 한 바퀴 돌지 않게 이어 붙인다. */
    private var lastRotation: Double? = null
    private var shownPlaceMarkers: List<PlaceMarker> = emptyList()
    private var shownSelectedPlaceId: String? = null

    init {
        map.setOnLabelClickListener { _, layer, label ->
            if (layer.layerId != PLACE_LAYER) return@setOnLabelClickListener false
            state.placeClicked(label.labelId)
            true
        }
        map.setOnCameraMoveEndListener { _, position, _ -> state.cameraDidStop(position.position.toGeoPoint()) }
        map.cameraPosition?.let { state.cameraDidStop(it.position.toGeoPoint()) }
    }

    override fun updateMyLocation(location: UserLocation?) {
        if (location == null) {
            myLabel?.remove()
            myLabel = null
            lastRotation = null
            return
        }
        val position = location.point.toLatLng()
        val styles = if (location.bearing != null) myHeadingStyles else myDotStyles

        val label = myLabel ?: myLocationLayer.addLabel(
            LabelOptions.from(position)
                .setStyles(styles)
                .setRank(Long.MAX_VALUE)
                // 지도를 회전시켜도 화살표가 실제 방위(북쪽 기준)를 가리키게 한다
                .setTransform(TransformMethod.AbsoluteRotation)
        )?.also { myLabel = it } ?: return

        if (label.styles != styles) label.changeStyles(styles)
        // 1초 간격 GPS 사이를 애니메이션으로 이어 마커가 순간이동하지 않게 한다
        label.moveTo(position, MARKER_MOVE_MILLIS)
        location.bearing?.let { bearing ->
            val rotation = continuousRotation(Math.toRadians(bearing.toDouble()))
            label.rotateTo(rotation.toFloat(), MARKER_MOVE_MILLIS)
            lastRotation = rotation
        }
    }

    override fun moveCamera(target: GeoPoint, zoomLevel: Int) {
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(target.toLatLng(), zoomLevel),
            CameraAnimation.from(CAMERA_MOVE_MILLIS, true, true),
        )
    }

    /** 지점이 모두 보이게 맞춘다. 위·아래 UI가 지도를 가리는 만큼 영역을 넓혀 잡는다(fitCorners). */
    override fun fitCamera(points: List<GeoPoint>, layout: MapFitLayout) {
        if (points.isEmpty()) return
        // 한 점이어도 화면 가운데가 아니라 UI에 가리지 않는 곳에 오도록 같은 계산을 쓴다(최소 범위가 있어 과하게 확대되지 않는다).
        val (southWest, northEast) = fitCorners(points, layout.insets)
        val corners = arrayOf(southWest.toLatLng(), northEast.toLatLng())
        map.moveCamera(
            CameraUpdateFactory.fitMapPoints(corners, 0, MAX_FIT_ZOOM),
            CameraAnimation.from(CAMERA_MOVE_MILLIS, true, true),
        )
    }

    override fun showPlaceMarkers(markers: List<PlaceMarker>, selectedId: String?) {
        // 같은 결과에서 선택만 바뀌었으면 바뀐 마커의 스타일만 고친다.
        if (markers == shownPlaceMarkers) {
            markers.filter { it.id == shownSelectedPlaceId || it.id == selectedId }.forEach { marker ->
                val label = placeLayer.getLabel(marker.id) ?: return@forEach
                val selected = marker.id == selectedId
                label.changeStyles(placeStyles(marker.number, selected))
                label.changeRank(placeRank(marker.number, selected))
            }
            shownSelectedPlaceId = selectedId
            return
        }

        placeLayer.removeAll()
        markers.forEach { marker ->
            val selected = marker.id == selectedId
            placeLayer.addLabel(
                LabelOptions.from(marker.id, marker.point.toLatLng())
                    .setStyles(placeStyles(marker.number, selected))
                    .setRank(placeRank(marker.number, selected))
                    .setClickable(true)
            )
        }
        shownPlaceMarkers = markers
        shownSelectedPlaceId = selectedId
    }

    override fun showRoute(overlay: RouteOverlay?) {
        routeLayer.removeAll()
        endpointLayer.removeAll()
        if (overlay == null) return

        overlay.lines.filter { it.points.size >= 2 }.forEachIndexed { index, line ->
            val stylesSet = routeStyles.getValue(line.kind)
            val segment = RouteLineSegment.from(line.points.map { it.toLatLng() }).setStyles(stylesSet.getStyles(0))
            routeLayer.addRouteLine(RouteLineOptions.from(segment).setStylesSet(stylesSet).setZOrder(index))
        }
        endpointLayer.addLabel(LabelOptions.from(overlay.start.toLatLng()).setStyles(routeStartStyles).setRank(1))
        endpointLayer.addLabel(LabelOptions.from(overlay.end.toLatLng()).setStyles(routeEndStyles).setRank(2))
    }

    private fun addLabelLayer(id: String, zOrder: Int, clickable: Boolean = false): LabelLayer = requireNotNull(
        labelManager.addLayer(
            LabelLayerOptions.from(id)
                // 결과끼리 겹쳐도 모두 보이도록 서로 경쟁(겹치면 숨김)하지 않는다.
                .setCompetitionType(CompetitionType.None)
                .setOrderingType(OrderingType.Rank)
                .setZOrder(zOrder)
                .setClickable(clickable)
        )
    ) { "Failed to add label layer: $id" }

    private fun placeStyles(number: Int, selected: Boolean): LabelStyles = placeStyleCache.getOrPut(number to selected) {
        val image = MarkerBitmaps.placePin(context, number, selected, RouteColors.Car.toArgb())
        labelStyles("place_${number}_$selected", image.bitmap, image.anchorY)
    }

    private fun bubbleStyles(id: String, textRes: Int, color: Int): LabelStyles {
        val image = MarkerBitmaps.routeBubble(context, context.getString(textRes), color)
        return labelStyles(id, image.bitmap, image.anchorY)
    }

    // SDK가 @Nullable로 선언해 둬서 Kotlin에서는 LabelStyles?로 보인다. 실패하면 마커를 그릴 수 없으니 즉시 알린다.
    private fun labelStyles(id: String, icon: Bitmap, anchorY: Float): LabelStyles = requireNotNull(
        labelManager.addLabelStyles(LabelStyles.from(id, LabelStyle.from(icon).setAnchorPoint(0.5f, anchorY)))
    ) { "Failed to register label style: $id" }

    private fun continuousRotation(target: Double): Double {
        val last = lastRotation ?: return target
        var delta = (target - last) % (2 * PI)
        if (delta > PI) delta -= 2 * PI
        if (delta < -PI) delta += 2 * PI
        return last + delta
    }

    private companion object {
        const val ROUTE_LAYER = "route"
        const val PLACE_LAYER = "places"
        const val ENDPOINT_LAYER = "route_endpoints"
        const val MY_LOCATION_LAYER = "my_location"
        const val MARKER_MOVE_MILLIS = 800
        const val CAMERA_MOVE_MILLIS = 300
        const val MAX_FIT_ZOOM = 17

        /** 선택된 마커가 다른 마커에 가려지지 않게 rank를 가장 높게 준다. 번호가 작을수록 위에 그린다. */
        fun placeRank(number: Int, selected: Boolean): Long = if (selected) 1_000L else 1_000L - number
    }
}

internal fun GeoPoint.toLatLng(): LatLng = LatLng.from(latitude, longitude)

private fun LatLng.toGeoPoint() = GeoPoint(latitude, longitude)

/** 카카오 라벨은 Bitmap만 받으므로 벡터 드로어블을 비트맵으로 그린다. */
private fun Context.bitmap(@DrawableRes id: Int) = requireNotNull(ContextCompat.getDrawable(this, id)).toBitmap()
