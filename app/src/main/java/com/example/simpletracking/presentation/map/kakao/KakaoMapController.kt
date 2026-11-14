package com.example.simpletracking.presentation.map.kakao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.example.simpletracking.presentation.map.FollowMode
import com.example.simpletracking.presentation.map.MapFitLayout
import com.example.simpletracking.presentation.map.MapGesture
import com.example.simpletracking.presentation.map.PlaceMarker
import com.example.simpletracking.presentation.map.RouteLine
import com.example.simpletracking.presentation.map.RouteOverlay
import com.example.simpletracking.presentation.map.route.RouteColors
import com.kakao.vectormap.GestureType
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
 *
 * - 나침반(정지 중 폰이 향한 방향): [CompassHeading] 값으로 따라가기 꺼짐이면 점의 화살촉을, 따라가기 중이면 점 아래 콘을 돌린다.
 * - 따라가기: SDK TrackingManager가 내 위치 라벨을 카메라로 따라간다. 사용자가 드래그하면 state에 알려 해제하게 한다.
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
    private val myArrowStyles = labelStyles("me_heading", context.bitmap(R.drawable.ic_marker_me_heading), anchorY = 0.5f)
    private val myHeadingDotStyles = labelStyles("me_heading_dot", MarkerBitmaps.myLocationHeadingDot(context, RouteColors.Car.toArgb()), anchorY = 0.5f)
    private val myConeStyles = labelStyles("me_cone", MarkerBitmaps.headingCone(context, RouteColors.Car.toArgb()), anchorY = 0.5f)
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
    /** 마지막으로 옮긴 내 위치. 다음 위치까지 거리로 애니메이션 여부를 정한다. */
    private var myPoint: GeoPoint? = null
    /** 마지막으로 적용한 회전값(라디안). 359° → 1°처럼 경계를 넘을 때 반대로 한 바퀴 돌지 않게 이어 붙인다. */
    private var lastRotation: Double? = null

    // 정지 중(bearing 없음) 폰이 향한 방향: 따라가기 꺼짐이면 점의 화살촉, 따라가기 중이면 점 아래 콘. 이동 중에는 화살표가 진행 방향을 보여 준다.
    private val compass = CompassHeading(context, ::onHeading)
    private var coneLabel: Label? = null
    private var lastConeRotation: Double? = null
    /** 나침반이 준 마지막 방향(도, 진북 기준). 아직 모르면 null. */
    private var heading: Double? = null
    private var isStationary = false
    /** 앱이 화면에 있을 때만 나침반을 켠다(KakaoMapView가 Activity resume/pause로 알려 준다). */
    private var isActive = true
    private var followMode = FollowMode.NONE
    private var shownPlaceMarkers: List<PlaceMarker> = emptyList()
    private var shownSelectedPlaceId: String? = null

    init {
        map.setOnLabelClickListener { _, layer, label ->
            if (layer.layerId != PLACE_LAYER) return@setOnLabelClickListener false
            state.placeClicked(label.labelId)
            true
        }
        map.setOnCameraMoveEndListener { _, position, _ -> state.cameraDidStop(position.position.toGeoPoint()) }
        // 코드로 옮긴 카메라(애니메이션·트래킹)는 Unknown으로 와서 무시된다.
        map.setOnCameraMoveStartListener { _, gesture ->
            when (gesture) {
                GestureType.Pan, GestureType.LongTapAndDrag -> state.userDidGesture(MapGesture.PAN)
                GestureType.Rotate -> state.userDidGesture(MapGesture.ROTATE)
                else -> Unit
            }
        }
        map.cameraPosition?.let { state.cameraDidStop(it.position.toGeoPoint()) }
    }

    /** KakaoMapView가 지도를 없앨 때. 센서와 리스너를 끊는다. */
    fun tearDown() {
        compass.stop()
        map.trackingManager?.stopTracking()
        map.setOnLabelClickListener(null)
        map.setOnCameraMoveEndListener(null)
        map.setOnCameraMoveStartListener(null)
    }

    /** Activity resume/pause. 백그라운드에서 센서를 계속 켜 두면 배터리를 쓴다. */
    fun setActive(active: Boolean) {
        isActive = active
        if (active && myLabel != null) compass.start() else compass.stop()
    }

    /**
     * 내 위치 점(방향이 있으면 화살표). null이면 지운다.
     * 처음엔 라벨을 만들고, 이후엔 위치·회전만 애니메이션으로 옮긴다. 100m 넘게 뛰면 바로 옮긴다.
     */
    override fun updateMyLocation(location: UserLocation?) {
        if (location == null) {
            map.trackingManager?.stopTracking()
            myLabel?.remove()
            myLabel = null
            myPoint = null
            lastRotation = null
            compass.stop()
            removeCone()
            return
        }
        if (isStationaryJitter(location)) return
        val position = location.point.toLatLng()
        val styles = myStyles(location.bearing)
        val isNewLabel = myLabel == null

        val label = myLabel ?: myLocationLayer.addLabel(
            LabelOptions.from(position)
                .setStyles(styles)
                .setRank(Long.MAX_VALUE)
                // 지도를 회전시켜도 화살표가 실제 방위(북쪽 기준)를 가리키게 한다
                .setTransform(TransformMethod.AbsoluteRotation)
        )?.also {
            myLabel = it
            // 위치를 받기 전에 따라가기를 켰으면 라벨이 생긴 지금 시작한다.
            syncTracking()
        } ?: return

        if (label.styles != styles) label.changeStyles(styles)
        // 위치 업데이트 사이를 애니메이션으로 이어 마커가 순간이동하지 않게 한다. 처음이거나 멀리 뛰었으면 바로 옮긴다.
        val jumped = myPoint?.let { distanceMeters(it, location.point) > MARKER_JUMP_METERS } ?: true
        val duration = if (jumped) 0 else MARKER_MOVE_MILLIS
        label.moveTo(position, duration)
        location.bearing?.let { bearing ->
            val rotation = continuousRotation(Math.toRadians(bearing.toDouble()), lastRotation)
            label.rotateTo(rotation.toFloat(), duration)
            lastRotation = rotation
        }
        myPoint = location.point

        val wasStationary = isStationary
        isStationary = location.bearing == null
        // 방금 멈췄거나 처음 그렸으면 점(화살촉)을 지금 방향으로 맞춘다. 나침반은 방향이 바뀔 때만 알려 준다.
        if (isStationary && (isNewLabel || !wasStationary)) rotateDotToHeading()
        compass.updateDeclination(location.point)
        if (isActive) compass.start()
        syncCone(duration)
    }

    /** 카메라가 내 위치를 따라간다. 회전 모드를 끄면 지도를 다시 북쪽이 위로 돌린다. */
    override fun setFollowMode(mode: FollowMode) {
        if (mode == followMode) return
        val wasRotating = followMode == FollowMode.FOLLOW_WITH_HEADING
        followMode = mode
        syncTracking()
        refreshStationaryMarker()
        rotateDotToHeading()
        syncCone(duration = 0)
        if (wasRotating && mode != FollowMode.FOLLOW_WITH_HEADING) {
            map.moveCamera(CameraUpdateFactory.rotateTo(0.0), CameraAnimation.from(CAMERA_MOVE_MILLIS, true, true))
        }
    }

    override fun moveCamera(target: GeoPoint, zoomLevel: Int, animated: Boolean) {
        val update = CameraUpdateFactory.newCenterPosition(target.toLatLng(), zoomLevel)
        if (animated) {
            map.moveCamera(update, CameraAnimation.from(CAMERA_MOVE_MILLIS, true, true))
        } else {
            map.moveCamera(update)
        }
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

    // region 따라가기·방향 콘

    /**
     * 정지 중(점 → 점) GPS 잡음으로 조금 흔들린 위치인가.
     * 매초 1초짜리 이동 애니메이션을 새로 걸면 지도가 쉬지 않고 다시 그려져(따라가기 중이면 카메라까지) 배터리를 쓴다.
     */
    private fun isStationaryJitter(location: UserLocation): Boolean {
        val last = myPoint ?: return false
        return myLabel != null && isStationary && location.bearing == null &&
            distanceMeters(last, location.point) < STATIONARY_JITTER_METERS
    }

    /**
     * 모드와 라벨 상태에 맞춰 SDK 트래킹을 켜고 끈다. 트래킹은 라벨의 이동 애니메이션을 매 프레임 따라가서
     * 카메라를 따로 움직이는 것보다 마커와 어긋나지 않는다. 회전 모드에서는 라벨의 회전값도 따라간다.
     */
    private fun syncTracking() {
        val tracking = map.trackingManager ?: return
        val label = myLabel
        // 한 번에 하나만 추적하고, 설정을 바꾸려면 먼저 멈춰야 한다.
        tracking.stopTracking()
        if (followMode == FollowMode.NONE || label == null) return
        tracking.setTrackingRotation(followMode == FollowMode.FOLLOW_WITH_HEADING)
        tracking.startTracking(label)
    }

    /**
     * 정지 중이면 점 라벨을 나침반 방향으로 돌린다. 이동 중에는 화살표가 진행 방향으로 돈다.
     * - 따라가기 꺼짐: 점의 화살촉이 폰이 향한 방향을 가리킨다.
     * - 회전 모드: 점은 동그라미라 돌려도 티가 나지 않고, 트래킹이 그 회전을 따라 지도를 돌려서 폰이 향한 방향이 위가 된다.
     */
    private fun rotateDotToHeading(duration: Int = 0) {
        val label = myLabel ?: return
        val heading = heading ?: return
        if (!isStationary) return
        val rotation = continuousRotation(Math.toRadians(heading), lastRotation)
        label.rotateTo(rotation.toFloat(), duration)
        lastRotation = rotation
    }

    /** 이동 중이면 화살표. 정지 중이면 따라가기 꺼짐 + 방향을 앎 → 화살촉 달린 점, 그 밖에는 점(따라가기 중엔 콘이 방향을 보여 준다). */
    private fun myStyles(bearing: Float?): LabelStyles = when {
        bearing != null -> myArrowStyles
        heading != null && followMode == FollowMode.NONE -> myHeadingDotStyles
        else -> myDotStyles
    }

    /** 정지 중 마커 모양을 지금 모드·나침반 상태에 맞춘다. 위치가 그대로여도 모드나 나침반이 바뀌면 불린다. */
    private fun refreshStationaryMarker() {
        val label = myLabel ?: return
        if (!isStationary) return
        val styles = myStyles(bearing = null)
        if (label.styles != styles) label.changeStyles(styles)
    }

    /** 나침반 값이 바뀔 때(초당 여러 번). 위치 이동과 달리 짧게 돌려서 손을 따라오게 한다. */
    private fun onHeading(degrees: Double) {
        heading = degrees
        refreshStationaryMarker()
        rotateDotToHeading(CONE_ROTATE_MILLIS)
        val cone = coneLabel ?: return syncCone(duration = 0)
        val rotation = continuousRotation(Math.toRadians(degrees), lastConeRotation)
        cone.rotateTo(rotation.toFloat(), CONE_ROTATE_MILLIS)
        lastConeRotation = rotation
    }

    /** 따라가기 중 정지해 있고 방향을 알면 콘을 내 위치로 옮기고(없으면 만들고), 아니면 지운다. */
    private fun syncCone(duration: Int) {
        val point = myPoint
        val heading = heading
        if (followMode == FollowMode.NONE || point == null || heading == null || !isStationary) return removeCone()

        val position = point.toLatLng()
        val cone = coneLabel
        if (cone != null) {
            cone.moveTo(position, duration)
            return
        }
        val rotation = Math.toRadians(heading)
        coneLabel = myLocationLayer.addLabel(
            LabelOptions.from(position)
                .setStyles(myConeStyles)
                // 점(Long.MAX_VALUE) 바로 아래에 깔린다
                .setRank(Long.MAX_VALUE - 1)
                .setTransform(TransformMethod.AbsoluteRotation)
        )?.also { it.rotateTo(rotation.toFloat(), 0) }
        lastConeRotation = rotation
    }

    private fun removeCone() {
        coneLabel?.remove()
        coneLabel = null
        lastConeRotation = null
    }

    // endregion

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

    private fun distanceMeters(a: GeoPoint, b: GeoPoint): Float {
        val result = FloatArray(1)
        Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
        return result[0]
    }

    /** 직전 회전값([last])에서 가까운 쪽으로 [target]을 이어 붙인다. 359° → 1°는 +2°만 돈다. */
    private fun continuousRotation(target: Double, last: Double?): Double {
        if (last == null) return target
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
        /** GPS 갱신 간격(1초)과 맞춘다. 짧으면 매초 잠깐 멈췄다 가고, 길면 다음 갱신이 중간에 끊는다. */
        const val MARKER_MOVE_MILLIS = 1_000
        /** 이보다 멀리 뛰면(GPS 재수신, 가상 위치 변경) 지도를 가로질러 미끄러지지 않게 바로 옮긴다. */
        const val MARKER_JUMP_METERS = 100f
        /** 정지 중 이보다 적게 움직이면 GPS 잡음으로 보고 마커를 옮기지 않는다. */
        const val STATIONARY_JITTER_METERS = 1f
        /** 나침반 값 사이를 잇는 회전 시간. 센서 갱신(약 60ms)보다 조금 길게 잡아 끊김 없이 돈다. */
        const val CONE_ROTATE_MILLIS = 150
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
