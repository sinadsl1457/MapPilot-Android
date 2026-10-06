package com.example.simpletracking.presentation.map.kakao

import android.content.Context
import android.graphics.Bitmap
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.simpletracking.R
import com.example.simpletracking.domain.model.GeoPoint
import com.example.simpletracking.domain.model.UserLocation
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.camera.CameraAnimation
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.Label
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.TransformMethod

/**
 * 카카오맵의 명령형 API(addLabel, moveCamera...)를 한곳에 모은 어댑터.
 * 카카오 SDK 타입(LatLng, Label...)은 이 패키지 밖으로 나가지 않는다.
 * → 나중에 네이버/구글 지도로 바꿔도 이 패키지만 교체하면 된다.
 */
internal class KakaoMapController(
    context: Context,
    private val map: KakaoMap,
) {
    private val labelManager = requireNotNull(map.labelManager) { "LabelManager is not ready" }

    private val myDotStyles = labelStyles("me_dot", context.bitmap(R.drawable.ic_marker_me))
    private val myHeadingStyles = labelStyles("me_heading", context.bitmap(R.drawable.ic_marker_me_heading))

    private var myLabel: Label? = null

    fun updateMyLocation(location: UserLocation?) {
        if (location == null) {
            myLabel?.remove()
            myLabel = null
            return
        }
        val position = location.point.toLatLng()
        val styles = if (location.bearing != null) myHeadingStyles else myDotStyles

        val label = myLabel ?: requireNotNull(labelManager.layer).addLabel(
            LabelOptions.from(position)
                .setStyles(styles)
                .setRank(Long.MAX_VALUE) // 다른 마커보다 항상 위에 그린다
                // 지도를 회전시켜도 화살표가 실제 방위(북쪽 기준)를 가리키게 한다
                .setTransform(TransformMethod.AbsoluteRotation)
        ).also { myLabel = it }

        if (label.styles != styles) label.changeStyles(styles)
        // 1초 간격 GPS 사이를 애니메이션으로 이어 마커가 순간이동하지 않게 한다
        label.moveTo(position, MARKER_MOVE_MILLIS)
        location.bearing?.let { label.rotateTo(Math.toRadians(it.toDouble()).toFloat(), MARKER_MOVE_MILLIS) }
    }

    fun moveCamera(target: GeoPoint, zoomLevel: Int) {
        map.moveCamera(
            CameraUpdateFactory.newCenterPosition(target.toLatLng(), zoomLevel),
            CameraAnimation.from(CAMERA_MOVE_MILLIS, true, true),
        )
    }

    // SDK가 @Nullable로 선언해 둬서 Kotlin에서는 LabelStyles?로 보인다. 실패하면 마커를 그릴 수 없으니 즉시 알린다.
    private fun labelStyles(id: String, icon: Bitmap): LabelStyles = requireNotNull(
        labelManager.addLabelStyles(LabelStyles.from(id, LabelStyle.from(icon).setAnchorPoint(0.5f, 0.5f)))
    ) { "Failed to register label style: $id" }

    private companion object {
        const val MARKER_MOVE_MILLIS = 800
        const val CAMERA_MOVE_MILLIS = 600
    }
}

internal fun GeoPoint.toLatLng(): LatLng = LatLng.from(latitude, longitude)

/** 카카오 라벨은 Bitmap만 받으므로 벡터 드로어블을 비트맵으로 그린다. */
private fun Context.bitmap(@DrawableRes id: Int) = requireNotNull(ContextCompat.getDrawable(this, id)).toBitmap()
