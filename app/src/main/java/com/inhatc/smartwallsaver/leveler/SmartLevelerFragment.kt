package com.inhatc.smartwallsaver.leveler

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.inhatc.smartwallsaver.R
import java.util.Locale

class SmartLevelerFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magneticField: Sensor? = null

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    // UI 컴포넌트 변수
    private lateinit var tvCurrentAngle: TextView
    private lateinit var tvLevelerStatus: TextView
    private lateinit var viewLevelerCircle: View
    private lateinit var viewInnerCircle: View
    private lateinit var levelerRootLayout: View

    private var vibrator: Vibrator? = null
    private var isVibrated = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_smart_leveler, container, false)

        // 위젯 바인딩
        levelerRootLayout = view.findViewById(R.id.leveler_root_layout)
        tvCurrentAngle = view.findViewById(R.id.tv_current_angle)
        tvLevelerStatus = view.findViewById(R.id.tv_leveler_status)
        viewLevelerCircle = view.findViewById(R.id.view_leveler_circle)
        viewInnerCircle = view.findViewById(R.id.view_inner_circle)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        initVibrator()

        return view
    }

    private fun initVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = requireActivity().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibrator = vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            vibrator = requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        magneticField?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.size)
        }

        calculateRollAngle()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun calculateRollAngle() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        val rollRadians = orientationAngles[2]
        val rollDegrees = Math.toDegrees(rollRadians.toDouble())
        val currentAngle = Math.abs(rollDegrees)

        // 🎯 수평 판정 오차 범위
        if (currentAngle <= 0.15) {
            tvCurrentAngle.text = "0.0°"
            tvLevelerStatus.text = "수평이 맞습니다!"

            // 🖤 배경은 언제나 시크한 검정색(#121212) 유지!
            levelerRootLayout.setBackgroundColor(Color.parseColor("#121212"))
            viewInnerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E"))

            // 🟠 [기획 핵심] 오직 바깥쪽 원형 띠와 텍스트들만 포인트 주황색(#FF823A)으로 강렬하게 점등!
            viewLevelerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FF823A"))
            tvCurrentAngle.setTextColor(Color.parseColor("#FF823A"))
            tvLevelerStatus.setTextColor(Color.parseColor("#FF823A"))

            // 일회성 진동 피드백
            if (!isVibrated) {
                vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(android.os.VibrationEffect.createOneShot(120, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(120)
                    }
                }
                isVibrated = true
            }
        } else {
            // ⚪ [평상시 기울어진 상태] 미니멀한 실버/그레이 톤 유지
            tvCurrentAngle.text = String.format(Locale.getDefault(), "%.1f°", currentAngle)
            tvLevelerStatus.text = "수평을 맞추는 중..."

            levelerRootLayout.setBackgroundColor(Color.parseColor("#121212")) // 배경 검정
            viewInnerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1E1E")) // 안쪽 원

            // 평소에는 원형 띠와 글씨가 은은한 그레이/화이트 톤
            viewLevelerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#555555")) // 비활성 회색 테두리
            tvCurrentAngle.setTextColor(Color.parseColor("#FFFFFF")) // 기본 흰색 각도
            tvLevelerStatus.setTextColor(Color.parseColor("#888888")) // 상태 메시지 회색

            isVibrated = false
        }
    }
}