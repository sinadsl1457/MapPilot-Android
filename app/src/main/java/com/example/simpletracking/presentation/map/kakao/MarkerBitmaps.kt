package com.example.simpletracking.presentation.map.kakao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import kotlin.math.ceil

/**
 * 검색 결과·길찾기 마커 이미지. 번호·글자가 들어가서 벡터 드로어블 대신 코드로 그린다.
 * 카카오 라벨은 Bitmap만 받는다.
 */
internal object MarkerBitmaps {

    /** [anchorY]: 꼭짓점(위치를 가리키는 점)의 세로 위치(0=위, 1=아래). 그림자 여백 때문에 1보다 조금 작다. */
    class MarkerImage(val bitmap: Bitmap, val anchorY: Float)

    private const val SHADOW_COLOR = 0x40000000

    /**
     * 정지 중이고 따라가기가 꺼져 있을 때. 내 위치 점(ic_marker_me와 같은 크기) 위에 폰이 향한 방향을 가리키는 작은 화살촉.
     * 36dp(화살촉이 후광 밖으로 나와서 점보다 크다). 위(북쪽)를 가리키게 그리고, 회전은 라벨이 맡는다.
     */
    fun myLocationHeadingDot(context: Context, tint: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = 36 * density
        val bitmap = createBitmap(ceil(size).toInt(), ceil(size).toInt())
        val canvas = Canvas(bitmap)
        val center = size / 2
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = withAlpha(tint, 0x33)
        canvas.drawCircle(center, center, 14 * density, paint)

        val pointer = Path().apply {
            moveTo(18 * density, 1.5f * density)
            lineTo(23.5f * density, 9.5f * density)
            lineTo(12.5f * density, 9.5f * density)
            close()
        }
        canvas.drawPath(pointer, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tint })
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.5f * density
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        canvas.drawPath(pointer, outline)

        paint.color = Color.WHITE
        canvas.drawCircle(center, center, 8 * density, paint)
        paint.color = tint
        canvas.drawCircle(center, center, 6 * density, paint)
        return bitmap
    }

    /**
     * 따라가기 중 정지해 있을 때 폰이 향한 방향. 80dp, 가운데에서 위(북쪽)로 퍼지는 부채꼴(70°). 회전은 라벨이 맡는다.
     * 가운데는 진하고 바깥으로 갈수록 투명해진다. 내 위치 점 아래에 깔린다.
     */
    fun headingCone(context: Context, tint: Int): Bitmap {
        val density = context.resources.displayMetrics.density
        val size = 80 * density
        val bitmap = createBitmap(ceil(size).toInt(), ceil(size).toInt())
        val canvas = Canvas(bitmap)
        val center = size / 2
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(center, center, center, withAlpha(tint, 0x80), withAlpha(tint, 0), Shader.TileMode.CLAMP)
        }
        // Canvas 각도는 오른쪽이 0°이고 시계방향이다. 위쪽(270°)을 가운데로 양옆 35°씩.
        canvas.drawArc(0f, 0f, size, size, 270f - CONE_SPREAD / 2, CONE_SPREAD, true, paint)
        return bitmap
    }

    private const val CONE_SPREAD = 70f

    private fun withAlpha(color: Int, alpha: Int): Int = (color and 0x00FFFFFF) or (alpha shl 24)

    /** 아래 꼭짓점이 장소 위치를 가리키는 번호 핀. 기본: 흰 바탕·파란 테두리, 선택: 파란 바탕·흰 번호·1.25배 */
    fun placePin(context: Context, number: Int, selected: Boolean, tint: Int): MarkerImage {
        val density = context.resources.displayMetrics.density
        val scale = if (selected) 1.25f else 1f
        val width = 28 * scale * density
        val height = 36 * scale * density
        val radius = width / 2
        val stroke = 2 * density
        val shadow = 2 * density
        val bitmap = createBitmap(ceil(width + shadow * 2).toInt(), ceil(height + shadow * 2).toInt())
        val canvas = Canvas(bitmap).apply { translate(shadow, shadow) }

        val inset = radius - stroke / 2
        // 원의 아래쪽 양옆(135°, 45°)에서 꼭짓점으로 이어지는 물방울 모양
        val path = Path().apply {
            arcTo(RectF(radius - inset, radius - inset, radius + inset, radius + inset), 135f, 270f)
            lineTo(radius, height - stroke)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (selected) tint else Color.WHITE
            setShadowLayer(shadow, 0f, density, SHADOW_COLOR)
        }
        canvas.drawPath(path, fill)
        val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeJoin = Paint.Join.ROUND
            color = if (selected) Color.WHITE else tint
        }
        canvas.drawPath(path, outline)

        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (selected) Color.WHITE else tint
            textSize = 13 * scale * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(number.toString(), radius, radius - (text.descent() + text.ascent()) / 2, text)
        return MarkerImage(bitmap, anchorY = (shadow + height - stroke) / bitmap.height)
    }

    /** 길찾기 출발·도착 마커. 글자가 들어간 말풍선이 아래 꼭짓점으로 위치를 가리킨다. */
    fun routeBubble(context: Context, label: String, color: Int): MarkerImage {
        val density = context.resources.displayMetrics.density
        val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 13 * density
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val bodyWidth = ceil(text.measureText(label)) + 20 * density
        val bodyHeight = 26 * density
        val tipHeight = 7 * density
        val tipHalfWidth = 6 * density
        val shadow = 2 * density
        val bitmap = createBitmap(ceil(bodyWidth + shadow * 2).toInt(), ceil(bodyHeight + tipHeight + shadow * 2).toInt())
        val canvas = Canvas(bitmap).apply { translate(shadow, shadow) }

        val path = Path().apply {
            addRoundRect(RectF(0f, 0f, bodyWidth, bodyHeight), bodyHeight / 2, bodyHeight / 2, Path.Direction.CW)
            moveTo(bodyWidth / 2 - tipHalfWidth, bodyHeight - density)
            lineTo(bodyWidth / 2, bodyHeight + tipHeight)
            lineTo(bodyWidth / 2 + tipHalfWidth, bodyHeight - density)
            close()
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            setShadowLayer(shadow, 0f, density, SHADOW_COLOR)
        }
        canvas.drawPath(path, fill)
        canvas.drawText(label, bodyWidth / 2, bodyHeight / 2 - (text.descent() + text.ascent()) / 2, text)
        return MarkerImage(bitmap, anchorY = (shadow + bodyHeight + tipHeight) / bitmap.height)
    }
}
