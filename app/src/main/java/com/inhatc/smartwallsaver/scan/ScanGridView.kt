package com.inhatc.smartwallsaver.scan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.toColorInt

class ScanGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val wirePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val nodePaint = Paint().apply {
        color = "#FFEB3B".toColorInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // GL 스레드에서 계산이 끝난 순수 2D 화면 좌표들 보관
    @Volatile
    private var screenPoints = listOf<Pair<Float, Float>>()

    // 프래그먼트가 GL 스레드에서 연산한 2D 좌표 리스트 사용
    fun updatePoints(points: List<Pair<Float, Float>>) {
        this.screenPoints = points
        postInvalidate() // 비동기 스레드에서 UI를 강제 재드로잉하도록 유도
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val points = screenPoints
        if (points.size < 2) return

        // 메모리 할당 없이 탐색된 2D 포인트들을 실선으로 연결
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, wirePaint)
        }

        // 2. 꺾이는 지점마다 노란색 노드 점 추가
        for (point in points) {
            canvas.drawCircle(point.first, point.second, 10f, nodePaint)
        }
    }
}