package com.inhatc.smartwallsaver.scan


// 전선 탐지 데이터를 실시간으로 전달하기 위한 인터페이스
interface WireDetectionListener {
    /**
     * 새로운 자기장 위치 데이터가 실시간으로 탐지되었을 때 호출
     * @param dataPoint 탐지된 벽면 좌표 및 자기장 수치 정보
     */
    fun onWirePointDetected(dataPoint: WireData)
}