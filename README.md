# ⛑️Smart Wall-Saver (스마트 월-세이버)
> 스마트폰 센서를 활용한 셀프 인테리어 안심 가이드 서비스

## 📌 프로젝트 소개
'오늘의 집'과 같은 플랫폼을 통해 가구 구매는 매우 편리해졌습니다. 하지만 실제 시공 단계에서 사용자가 직접 타공을 진행할 때, 벽 내부 매설 전선을 인지하지 못해 발생하는 **합선 및 감전 사고의 위험**이 존재합니다. 또한 전문 장비가 없어 발생하는 **수평 미달 및 벽면 훼손** 문제와 단 한 번의 시공을 위해 고가의 전문 탐지 장비를 구매해야 하는 **경제적 부담**이 있습니다.

**Smart Wall-Saver**는 누구나 주머니 속 스마트폰의 내장 센서만을 활용하여 전문가급의 안전하고 정밀한 시공을 할 수 있도록 돕는 안드로이드 애플리케이션입니다.

---

## 🎨 주요 기능 및 UI 구성 (User Interface)
애플(Apple) '측정' 앱의 미니멀하고 직관적인 사용자 경험(UX)을 벤치마킹하여, 불필요한 메뉴를 없애고 **하단 Bottom Navigation 탭**을 통해 두 가지 핵심 기능을 명확히 분리하여 구현했습니다.

### 1. Safety Scan (전선 탐지 모드)
* **자기장 센서(`Sensor.TYPE_MAGNETIC_FIELD`) 활용**
* **교수님 피드백 반영 (Grid Mapping)**: 단순히 수치만 보여주는 것이 아닌, 스마트폰을 벽면에 대고 움직이는 동선에 따라 **2D 격자(Grid) 히트맵**을 Canvas에 실시간으로 채워나갑니다.
* **위험 알림**: 전자기장 임계값 초과 구역은 강렬한 **적색(Red)**으로 맵핑되며, 화면을 보지 않고도 인지할 수 있도록 강한 햅틱 진동 피드백을 제공합니다.

### 2. Smart Leveler (정밀 수평계 모드)
* **가속도(`TYPE_ACCELEROMETER`) 및 자이로(`TYPE_GYROSCOPE`) 센서 활용**
* **0.1° 단위 정밀도**: 화면 중앙에 실시간 기울기 데이터를 대형 텍스트로 배치하여 공학적 신뢰성을 극대화합니다.
* **수평 정렬 가이드**: 완벽한 수평(`0.0°`)에 도달 시 화면 전체 테마 색상이 주황색으로 반전되며 한 번의 강력한 숏 진동(Haptic)으로 시공 타이밍을 알립니다.

### 3. UI 예시
<img width="1127" height="618" alt="스크린샷 2026-05-17 145121" src="https://github.com/user-attachments/assets/1ffa3e84-b4f8-41f8-b958-5209005b25a3" />

<img width="1100" height="625" alt="스크린샷 2026-05-17 145149" src="https://github.com/user-attachments/assets/ca9f06dc-8026-4a35-8afa-5ab6a0dcdd68" />

---

## 🛠️ 개발 환경 및 활용 기술
* **OS**: Android 13.0 (API Level 33) 이상 타겟
* **IDE**: Android Studio Jellyfish
* **Language**: Kotlin
* **UI Architecture**: Single Activity, Multi-Fragments (BottomNavigationView 적용)
* **Sensors**: Hardware Magnetic Field, Gyroscope, Accelerometer Sensors, VibratorManager
