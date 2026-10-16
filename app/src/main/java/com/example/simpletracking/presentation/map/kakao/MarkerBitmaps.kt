package com.example.simpletracking.presentation.map.kakao

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
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
