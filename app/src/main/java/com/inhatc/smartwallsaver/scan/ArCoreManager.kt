package com.inhatc.smartwallsaver.scan

import android.content.Context
import android.widget.Toast
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState

class ArCoreManager(private val context: Context) {

    var session: Session? = null
        private set

    //ARCore 세션을 수직 벽면 감지 및 자동 초점 모드로 초기화
    fun initSession() {
        try {
            session = Session(context)
            val config = Config(session).apply {
                planeFindingMode = Config.PlaneFindingMode.VERTICAL
                updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE

                // 자동 초점 기능 활성화
                // 초점 깨짐 및 거리(Depth) 측정 작동용
                focusMode = Config.FocusMode.AUTO
            }
            session?.configure(config)
        } catch (e: Exception) {
            val message = when (e) {
                is com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException -> "ARCore 라이브러리를 설치해 주세요."
                is com.google.ar.core.exceptions.UnavailableApkTooOldException -> "ARCore 앱을 업데이트해 주세요."
                is com.google.ar.core.exceptions.UnavailableSdkTooOldException -> "앱 타겟 SDK 버전을 확인해 주세요."
                is com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException -> "ARCore 기능을 지원하지 않는 기기입니다."
                else -> "AR 초기화 오류: ${e.message}"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun resume() {
        session?.resume()
    }

    fun pause() {
        session?.pause()
    }

    // 현재 프레임 업데이트
    fun updateFrame(): Frame? {
        return try {
            session?.update()
        } catch (e: Exception) {
            null
        }
    }

    // 지정한 화면 좌표(중앙)에서 레이저를 쏘아 수직 벽면과의 교차점을 반환(HitResult)
    fun performVerticalPlaneHitTest(frame: Frame, cx: Float, cy: Float): HitResult? {
        if (frame.camera.trackingState != TrackingState.TRACKING) return null

        val hitResults = frame.hitTest(cx, cy)
        for (hit in hitResults) {
            val trackable = hit.trackable
            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose) && trackable.type == Plane.Type.VERTICAL) {
                return hit
            }
        }
        return null
    }
}