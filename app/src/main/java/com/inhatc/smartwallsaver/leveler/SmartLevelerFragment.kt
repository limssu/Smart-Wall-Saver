package com.inhatc.smartwallsaver.leveler

import android.content.Context
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
import androidx.core.graphics.toColorInt

class SmartLevelerFragment : Fragment(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private val accelerometerReading = FloatArray(3)

    // 센서 노이즈 및 각도 댐핑을 위한 가중치
    private val alpha = 0.07f

    // 최종 계산된 각도가 튀는 것을 막아줄 필터링된 각도 저장 변수
    private var filteredAngle = 0.0

    // 모드 관리 변수 (true: 바닥 눕힘 기준, false: 벽면 세움 기준)
    private var isFlatMode = true

    // UI 컴포넌트 변수
    private lateinit var tvCurrentAngle: TextView
    private lateinit var tvLevelerStatus: TextView
    private lateinit var viewLevelerCircle: View
    private lateinit var viewInnerCircle: View
    private lateinit var levelerRootLayout: View

    // 추가된 모드 토글 뷰
    private lateinit var tvModeFlat: TextView
    private lateinit var tvModeStanding: TextView

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

        tvModeFlat = view.findViewById(R.id.tv_mode_flat)
        tvModeStanding = view.findViewById(R.id.tv_mode_standing)

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        //  자력계(MAGNETIC)는 사용하지 않으므로 가속도 센서만 깔끔하게 등록
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        initVibrator()
        initModeToggle()

        return view
    }

    private fun initModeToggle() {
        tvModeFlat.setOnClickListener {
            if (!isFlatMode) {
                isFlatMode = true
                isVibrated = false
                filteredAngle = 0.0 // 모드 변경 시 이전 각도 잔상이 남지 않도록 초기화
                updateToggleUi()
                calculateTiltAngle()
            }
        }

        tvModeStanding.setOnClickListener {
            if (isFlatMode) {
                isFlatMode = false
                isVibrated = false
                filteredAngle = 0.0 // 모드 변경 시 이전 각도 잔상이 남지 않도록 초기화
                updateToggleUi()
                calculateTiltAngle()
            }
        }
    }

    private fun updateToggleUi() {
        if (isFlatMode) {
            tvModeFlat.text = "● 바닥 모드"
            tvModeFlat.setTextColor("#FF823A".toColorInt())
            tvModeStanding.text = "○ 벽면 모드"
            tvModeStanding.setTextColor("#888888".toColorInt())
        } else {
            tvModeFlat.text = "○ 바닥 모드"
            tvModeFlat.setTextColor("#888888".toColorInt())
            tvModeStanding.text = "● 벽면 모드"
            tvModeStanding.setTextColor("#FF823A".toColorInt())
        }
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
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            applyLowPassFilter(event.values, accelerometerReading)
            // 오직 가속도 센서가 실제로 새로 측정되었을 때만 계산 수행
            calculateTiltAngle()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun applyLowPassFilter(input: FloatArray, output: FloatArray) {
        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    private fun calculateTiltAngle() {
        val ax = accelerometerReading[0]
        val ay = accelerometerReading[1]
        val az = accelerometerReading[2]

        val norm = kotlin.math.sqrt(ax * ax + ay * ay + az * az)
        if (norm == 0f) return

        // 1. 원시 가속도 센서 기반으로 즉시 측정된 원시 각도(rawAngle) 계산
        val rawAngle = if (isFlatMode) {
            val cosTilt = az / norm
            val safeCosTilt = cosTilt.coerceIn(-1.0f, 1.0f)
            Math.toDegrees(kotlin.math.acos(safeCosTilt).toDouble())
        } else {
            val cosTilt = ay / norm
            val safeCosTilt = cosTilt.coerceIn(-1.0f, 1.0f)
            Math.toDegrees(kotlin.math.acos(safeCosTilt).toDouble())
        }

        // 2. 삼각함수 연산 결과로 나온 최종 각도에 한 번 더 저주파 필터(LPF) 적용
        // 이 단계를 거쳐야 수식의 제곱 연산 때문에 발생하던 '벽면 모드 특유의 미세한 튐' 완벽하게 제거
        if (filteredAngle == 0.0) {
            filteredAngle = rawAngle
        } else {
            filteredAngle = filteredAngle + alpha * (rawAngle - filteredAngle)
        }

        val currentAngle = filteredAngle

        // 수평 판정 및 디자인 매핑 로직 
        if (currentAngle <= 0.15) {
            tvCurrentAngle.text = "0.0°"
            tvLevelerStatus.text = "수평이 맞습니다!"

            levelerRootLayout.setBackgroundColor("#121212".toColorInt())
            viewInnerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf("#1E1E1E".toColorInt())

            viewLevelerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf("#FF823A".toColorInt())
            tvCurrentAngle.setTextColor("#FF823A".toColorInt())
            tvLevelerStatus.setTextColor("#FF823A".toColorInt())

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
            tvCurrentAngle.text = String.format(Locale.getDefault(), "%.1f°", currentAngle)
            tvLevelerStatus.text = "수평을 맞추는 중..."

            levelerRootLayout.setBackgroundColor("#121212".toColorInt())
            viewInnerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf("#1E1E1E".toColorInt())

            viewLevelerCircle.backgroundTintList = android.content.res.ColorStateList.valueOf("#555555".toColorInt())
            tvCurrentAngle.setTextColor("#FFFFFF".toColorInt())
            tvLevelerStatus.setTextColor("#888888".toColorInt())

            isVibrated = false
        }
    }
}
