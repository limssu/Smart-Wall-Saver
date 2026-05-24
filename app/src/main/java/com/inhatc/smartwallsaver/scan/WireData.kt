package com.inhatc.smartwallsaver.scan

/**
 * 자기장 센서 데이터와 ARCore 3D 공간 좌표를 결합한 데이터 클래스
 */
data class WireData(
    val x: Float,               // AR 공간의 X 좌표
    val y: Float,               // AR 공간의 Y 좌표
    val z: Float,               // AR 공간의 Z 좌표
    val intensity: Float,       // 자기장 세기 (uT 단위)
    val isWireDetected: Boolean // 전선 감지 여부 (임계치 초과 여부)
)