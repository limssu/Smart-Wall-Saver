package com.inhatc.smartwallsaver.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.ar.core.Anchor
import com.google.ar.core.Pose
import com.google.ar.core.TrackingState
import com.inhatc.smartwallsaver.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sqrt
import androidx.core.graphics.toColorInt
import java.util.Locale

class SafetyScanFragment : Fragment(), SensorEventListener, GLSurfaceView.Renderer {

    private lateinit var sensorManager: SensorManager
    private var magneticSensor: Sensor? = null
    private val filterAlpha = 0.15f
    private var magneticValues = FloatArray(3)

    @Volatile
    private var currentMagneticStrength = 0.0f

    private var pausedFrameCount = 0

    @Volatile
    private var isDrawingActive = false // 기본값은 '정지(Ready)' 상태
    private var lastAnchorTimestamp = 0L

    private lateinit var arCoreManager: ArCoreManager
    private lateinit var scanGridView: ScanGridView
    private lateinit var arSurfaceView: GLSurfaceView
    private val backgroundRenderer = CameraBackgroundRenderer()

    private var isCameraPermissionGranted = false
    private val detectedWireAnchors = mutableListOf<Anchor>()
    private var wireDetectionListener: WireDetectionListener? = null

    // UI 컴포넌트
    private lateinit var txtStatus: TextView
    private lateinit var txtMagneticStrength: TextView
    private lateinit var txtWarningBanner: TextView
    private lateinit var btnStartStop: Button
    private lateinit var btnClear: Button

    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val vertexIndex = FloatArray(4)
    private val resultVertex = FloatArray(4)

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        isCameraPermissionGranted = isGranted
        if (isGranted) arCoreManager.initSession()
        else Toast.makeText(context, "화면 출력을 위한 카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    fun setWireDetectionListener(listener: WireDetectionListener) {
        this.wireDetectionListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_safety_scan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arSurfaceView = view.findViewById(R.id.ar_surface_view)
        scanGridView = view.findViewById(R.id.scan_grid_view)
        txtStatus = view.findViewById(R.id.txt_status)
        txtMagneticStrength = view.findViewById(R.id.txt_magnetic_strength)
        txtWarningBanner = view.findViewById(R.id.txt_warning_banner)
        btnStartStop = view.findViewById(R.id.btn_start_stop)
        btnClear = view.findViewById(R.id.btn_clear)

        arCoreManager = ArCoreManager(requireContext())

        arSurfaceView.preserveEGLContextOnPause = true
        arSurfaceView.setEGLContextClientVersion(2)
        arSurfaceView.setRenderer(this)
        arSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        // 시작, 중지 버튼
        btnStartStop.setOnClickListener {
            isDrawingActive = !isDrawingActive
            if (isDrawingActive) {
                btnStartStop.text = "스캔 중지"
                btnStartStop.backgroundTintList = ColorStateList.valueOf("#E53935".toColorInt()) // 빨간색 변경
            } else {
                btnStartStop.text = "스캔 시작"
                btnStartStop.backgroundTintList = ColorStateList.valueOf("#FF823A".toColorInt()) // 주황색 원복
                txtWarningBanner.visibility = View.GONE // 중지 시 경고배너 X
            }
        }

        // 초기화 버튼
        btnClear.setOnClickListener {
            synchronized(detectedWireAnchors) {
                detectedWireAnchors.clear() // 탐지 데이터 초기화
            }
            scanGridView.updatePoints(emptyList()) // 즉시 화면 초기화
            txtWarningBanner.visibility = View.GONE
            Toast.makeText(context, "탐지된 전선과 화면이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
        }

        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (magneticSensor == null) {
            Toast.makeText(context, "이 기기는 자기장 센서를 지원하지 않습니다.", Toast.LENGTH_SHORT).show()
        }

        checkCameraPermission()
    }

    override fun onResume() {
        super.onResume()
        magneticSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        if (isCameraPermissionGranted && arCoreManager.session == null) {
            arCoreManager.initSession()
        }

        arCoreManager.resume()
        arSurfaceView.onResume()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        arCoreManager.pause()
        arSurfaceView.onPause()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues[0] = filterAlpha * event.values[0] + (1 - filterAlpha) * magneticValues[0]
            magneticValues[1] = filterAlpha * event.values[1] + (1 - filterAlpha) * magneticValues[1]
            magneticValues[2] = filterAlpha * event.values[2] + (1 - filterAlpha) * magneticValues[2]

            currentMagneticStrength = sqrt(
                (magneticValues[0] * magneticValues[0] +
                        magneticValues[1] * magneticValues[1] +
                        magneticValues[2] * magneticValues[2]).toDouble()
            ).toFloat()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        backgroundRenderer.init()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        arCoreManager.session?.setDisplayGeometry(0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val session = arCoreManager.session ?: return
        session.setCameraTextureName(backgroundRenderer.textureId)

        val currentFrame = arCoreManager.updateFrame() ?: return
        backgroundRenderer.draw(currentFrame)

        val strength = currentMagneticStrength
        val trackingState = currentFrame.camera.trackingState

        if (trackingState == TrackingState.TRACKING) {
            pausedFrameCount = 0
        } else if (trackingState == TrackingState.PAUSED) {
            pausedFrameCount++
        }

        if (trackingState != TrackingState.TRACKING && pausedFrameCount > MAX_PAUSED_GRACE_FRAMES) {
            activity?.runOnUiThread {
                txtMagneticStrength.text = "자기장 수치: %.1f uT".format(Locale.US, strength)
                txtStatus.text = "스캔 일시정지됨... 벽면에서 조금 떨어져 주변을 비춰주세요"
            }
            return
        }

        activity?.runOnUiThread {
            txtMagneticStrength.text = "자기장 수치: %.1f uT".format(Locale.US, strength)
            txtStatus.text = if (isDrawingActive) "벽면 전선 추적 중..." else "추적 일시 정지"
        }

        // 사용자의 선택에 따라 화면에 맵을 그릴 것인지 아닌지 체크
        if (isDrawingActive) {
            val view = view ?: return
            val centerX = view.width / 2f
            val centerY = view.height / 2f

            var finalPose: Pose? = null

            val hitResult = arCoreManager.performVerticalPlaneHitTest(currentFrame, centerX, centerY)
            if (hitResult != null) {
                finalPose = hitResult.hitPose
            } else {
                val cameraPose = currentFrame.camera.pose
                finalPose = cameraPose.compose(Pose.makeTranslation(0f, 0f, -0.05f))
            }

            if (finalPose != null) {
                val isWire = strength > WIRE_THRESHOLD_LIMIT

                if (isWire) {
                    activity?.runOnUiThread { txtWarningBanner.visibility = View.VISIBLE }

                    if (trackingState == TrackingState.TRACKING) {
                        // 0.3초당 한번씩 업데이트 되도록 고정
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastAnchorTimestamp > ANCHOR_THROTTLE_MS) {
                            val newAnchor = session.createAnchor(finalPose)
                            synchronized(detectedWireAnchors) {
                                if (detectedWireAnchors.isEmpty() || getDistance(detectedWireAnchors.last(), newAnchor) > 0.05f) {
                                    detectedWireAnchors.add(newAnchor)
                                    lastAnchorTimestamp = currentTime // 마지막 저장 시간 갱신
                                }
                            }
                        }
                    }
                } else {
                    activity?.runOnUiThread { txtWarningBanner.visibility = View.GONE }
                }

                val dataPoint = WireData(
                    x = finalPose.tx(), y = finalPose.ty(), z = finalPose.tz(),
                    intensity = strength, isWireDetected = isWire
                )
                wireDetectionListener?.onWirePointDetected(dataPoint)
            }
        }

        // 3D 매트릭스 및 그려진 화면 유지
        val camera = currentFrame.camera
        camera.getViewMatrix(viewMatrix, 0)
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f)
        android.opengl.Matrix.multiplyMM(vpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        val viewW = scanGridView.width
        val viewH = scanGridView.height

        val calculated2DPoints = mutableListOf<Pair<Float, Float>>()

        synchronized(detectedWireAnchors) {
            for (anchor in detectedWireAnchors) {
                if (anchor.trackingState == TrackingState.TRACKING) {
                    val pose = anchor.pose
                    val screenPos = project3DPointToScreen(pose.tx(), pose.ty(), pose.tz(), viewW, viewH)
                    if (screenPos != null) {
                        calculated2DPoints.add(screenPos)
                    }
                }
            }
        }

        scanGridView.updatePoints(calculated2DPoints)
    }

    private fun project3DPointToScreen(valX: Float, valY: Float, valZ: Float, viewW: Int, viewH: Int): Pair<Float, Float>? {
        vertexIndex[0] = valX
        vertexIndex[1] = valY
        vertexIndex[2] = valZ
        vertexIndex[3] = 1.0f

        android.opengl.Matrix.multiplyMV(resultVertex, 0, vpMatrix, 0, vertexIndex, 0)

        val w = resultVertex[3]
        if (w == 0f) return null

        val ndcX = resultVertex[0] / w
        val ndcY = resultVertex[1] / w

        val screenX = ((ndcX + 1.0f) * viewW / 2.0f)
        val screenY = ((1.0f - ndcY) * viewH / 2.0f)

        return Pair(screenX, screenY)
    }

    private fun getDistance(a: Anchor, b: Anchor): Float {
        val dx = a.pose.tx() - b.pose.tx()
        val dy = a.pose.ty() - b.pose.ty()
        val dz = a.pose.tz() - b.pose.tz()
        return sqrt((dx * dx + dy * dy + dz * dz).toDouble()).toFloat()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            isCameraPermissionGranted = true
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    companion object {
        private const val WIRE_THRESHOLD_LIMIT = 150.0f
        // 화면에 그린 줄이 유지되도록 하는 최소 시간
        private const val MAX_PAUSED_GRACE_FRAMES = 180

        // 센서 데이터 수집 속도 제한 (0.3초당 1번)
        private const val ANCHOR_THROTTLE_MS = 300L
    }
}