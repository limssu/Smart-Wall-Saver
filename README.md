# ⛑️Smart Wall-Saver (스마트 월-세이버)
> 스마트폰 센서를 활용한 셀프 인테리어 안심 가이드 서비스

## 📌 프로젝트 소개
'오늘의 집'과 같은 플랫폼을 통해 가구 구매는 매우 편리해졌습니다. 하지만 실제 시공 단계에서 사용자가 직접 타공을 진행할 때, 벽 내부 매설 전선을 인지하지 못해 발생하는 **합선 및 감전 사고의 위험**이 존재합니다. 또한 전문 장비가 없어 발생하는 **수평 미달 및 벽면 훼손** 문제와 단 한 번의 시공을 위해 고가의 전문 탐지 장비를 구매해야 하는 **경제적 부담**이 있습니다.

**Smart Wall-Saver**는 누구나 주머니 속 스마트폰의 내장 센서만을 활용하여 전문가급의 안전하고 정밀한 시공을 할 수 있도록 돕는 안드로이드 애플리케이션입니다.

---

## ⭐ 주요 기능
애플(Apple) '측정' 앱의 미니멀하고 직관적인 사용자 경험(UX)을 벤치마킹하여, 불필요한 메뉴를 없애고 **하단 Bottom Navigation 탭**을 통해 두 가지 핵심 기능을 명확히 분리하여 구현했습니다.

### 1. Safety Scan (전선 탐지 모드)
* **하드웨어 센서 퓨전 기술:** 내장 자기장 센서(`Sensor.TYPE_MAGNETIC_FIELD`)를 활용하여 벽면 내부의 미세 전자기장 변화 및 전류 임계값을 실시간으로 감지합니다.
* **ARCore 3D 공간 앵커링:** 디바이스의 이동 동선에 맞춰 전선이 매설된 것으로 추정되는 실시간 공간 좌표(x, y, z)를 연산하고 가상 앵커(Anchor)를 지정하여 벽면에 고정합니다.
* **2D 가상 캔버스 실선 매핑 (`ScanGridView`):** * GL 스레드에서 계산이 끝난 순수 2D 화면 좌표 리스트(`screenPoints`)를 비동기 스레드에서 안전하게 UI로 유도(`postInvalidate()`)합니다.
    * 메모리 할당 오버헤드 없이 탐색된 포인트들을 순차적으로 실선(`canvas.drawLine`)으로 연결하여 실제 전선 매립 경로를 '하나의 흐르는 선(Line)'으로 정밀 시각화합니다.
    * 전선관이 꺾이는 주요 지점마다 명확한 노란색 노드 점(`canvas.drawCircle`)을 렌더링하여 구조적 가독성을 극대화했습니다.
* **실시간 위협 알림:** 전자기장 임계값 초과 구역은 강렬한 적색(RED, `Color.RED`) 실선 스트로크로 드로잉되며, 동시에 하드웨어 햅틱 진동 피드백을 발생시켜 직관적인 감전·합선 사고 예방을 지원합니다

### 2. Smart Leveler (초정밀 융합 디지털 수평계)

* **듀얼 센서 모드 세분화 (바닥 모드 vs 벽면 모드):**
  * 기존의 스마트폰을 바닥에 평평하게 눕혀서만 측정해야 했던 단일 방식의 불편함을 개선했습니다.
  * **바닥 모드 (Flat Mode):** 가속도 센서의 Z축 데이터를 기반으로 중력 벡터를 연산하여, 바닥면에 눕힌 상태로 가구 등의 평평함을 측정합니다.
  * **벽면 모드 (Standing Mode):** 스마트폰을 벽면에 세워 액자나 선반 등을 설치할 때의 편의성을 위해 Y축 중력 벡터 연산으로 전환되는 토글 시스템을 구축했습니다.
* **이중 저주파 필터 알고리즘 (Double Low-Pass Filter):**
  * 하드웨어 센서 특유의 미세한 값 떨림(Jitter) 현상과 삼각함수 연산 시 소수점이 튀는 버그를 잡기 위해 2차 댐핑 필터 알고리즘을 도입했습니다.
  * 이를 통해 값이 흔들리지 않고 `0.1°` 단위까지 정밀하고 칼같이 멈추는 안정적인 데이터를 확보했습니다.
* **오렌지 링 UX & 단발성 햅틱 피드백:**
  * 완벽한 평형(`0.0°`)에 도달하는 순간, 화면 중심의 가이드 링이 강렬한 오렌지 테마 컬러(`#FF823A`)로 반전되어 시각적 즉시성을 제공합니다.
  * 무한 루프로 진동이 발생하는 버그를 방지하기 위해 단발성 예외 처리 플래그를 적용, 오차 범위 내 진입 시 딱 1번만 깔끔하게 `120ms` 햅틱 진동이 울리도록 구현하여 비시각적 환경에서도 직관적인 수평 확인을 지원합니다.

  
## 🎨 디자인 시스템 (Design System)

### 🔴 Color Palette

| 역할 |  컬러 코드 | 사용처 |
| :---: | :---: | --- |
| **Primary (대표색)** |  `#121212` | 앱 전체 메인 다크 배경, 프래그먼트 기본 베이스 |
| **Secondary (보조색)** | `#1E1E1E`| 하단 네비게이션 바, 수평계 원형 카드, 컴포넌트 배경 |
| **Point (포인트색)** |  `#FF823A`| 하단 탭 활성화 아이콘, 탐지 시작 버튼, 수평 일치 알림 |
| **Text Primary** | `#FFFFFF` | 실시간 수평 각도, 메인 타이틀, 강조 텍스트 |

---
### 3. UI 예시(초기)
<img width="1127" height="618" alt="스크린샷 2026-05-17 145121" src="https://github.com/user-attachments/assets/1ffa3e84-b4f8-41f8-b958-5209005b25a3" />

<img width="1100" height="625" alt="스크린샷 2026-05-17 145149" src="https://github.com/user-attachments/assets/ca9f06dc-8026-4a35-8afa-5ab6a0dcdd68" />

### 🎨 UI (최종)
<img width="1073" height="515" alt="image" src="https://github.com/user-attachments/assets/ee494b44-ef07-45e2-a5da-5e634ca31091" />

<img width="1048" height="522" alt="image" src="https://github.com/user-attachments/assets/e7464e9b-140a-4b74-b7e9-c1e07896fcf0" />

---

## 🛠️ 핵심 알고리즘 및 기술적 최적화

테스트 과정에서 발생한 기술적 병목 현상을 해결하기 위해 소프트웨어적으로 직접 설계하고 구현한 핵심 로직입니다.


### 1. 디지털 수평계(Smart Leveler) 노이즈 정제 및 예외 처리
* **핵심 기술:** 저역통과필터(Low-Pass Filter) 알고리즘 실장 및 초기 구동 딜레이 예외 처리

가속도·지자기 센서 특성상 손떨림이나 물리적 진동으로 인해 소수점 각도가 지속적으로 튀는 현상(Jitter)을 방지하기 위해 이중 LPF 알고리즘을 적용했습니다. 
또한, 앱 초기 구동 시 필터 연산 지연으로 인해 각도가 뒤늦게 차오르는 UX 병목을 조건문 분리를 통해 해결했습니다.

```kotlin
// 삼각함수 연산 결과로 나온 최종 각도에 한 번 더 저주파 필터(LPF) 적용
// 이 단계를 거쳐야 수식의 제곱 연산 때문에 발생하던 '벽면 모드 특유의 미세한 튐'을 완벽하게 제거
if (filteredAngle == 0.0) {
    filteredAngle = rawAngle // 초기 구동 시 실시간 데이터 즉각 반영 (반응성 확보)
} else {
    // alpha 값을 활용한 가중치 합성 필터링 수행 (안정성 확보)
    filteredAngle = filteredAngle + alpha * (rawAngle - filteredAngle)
}
```
### 2. ARCore 기반 3D 배선 매핑 실선 드로잉 및 메모리 최적화
**핵심 기술:** @volatile을 이용한 멀티스레드 동기화, onDraw() 런타임 메모리 힙 최적화

백그라운드에서 실시간으로 연산되는 ARCore의 GL 스레드 데이터와 화면을 렌더링하는 UI 메인 스레드 간의 데이터 정합성 불일치 문제를 해결하기 위해 메모리 가시성을 확보했습니다. 또한 프레임 드롭(화면 버벅임)을 막기 위해 커스텀 뷰의 드로잉 루프를 최적화했습니다.

```
// GL 스레드에서 계산이 끝난 순수 2D 화면 좌표들 보관 (메모리 가시성 확보)
@volatile
private var screenPoints = listOf<Pair<Float, Float>>()

// 프래그먼트가 GL 스레드에서 연산한 2D 좌표 리스트 사용
fun updatePoints(points: List<Pair<Float, Float>>) {
    this.screenPoints = points
    postInvalidate() // 비동기 스레드에서 UI를 메인 스레드로 강제 재드로잉하도록 안전하게 유도
}

override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val points = screenPoints
    if (points.size < 2) return

    // [최적화] 루프 내 신규 객체 생성 없이, 인덱스 참조만으로 실선 연결
    // 이를 통해 가비지 컬렉터(GC) 오버헤드와 프레임 드롭을 구조적으로 방지
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        canvas.drawLine(p1.first, p1.second, p2.first, p2.second, wirePaint)
    }

    // 2. 꺾이는 지점마다 노드 점 추가
    for (point in points) {
        canvas.drawCircle(point.first, point.second, 10f, nodePaint)
    }
}
```
---

## 🛠️ 개발 환경 및 활용 기술
* **OS**: Android 13.0 (API Level 33) 이상 타겟
* **IDE**: Android Studio Jellyfish
* **Language**: Kotlin
* **UI Architecture**: Single Activity, Multi-Fragments (BottomNavigationView 적용)
* **Sensors**: Hardware Magnetic Field, Gyroscope, Accelerometer Sensors, VibratorManager

---

## 📅 프로젝트 일정 및 진행 계획

교수님 피드백 및 학사 일정에 맞춘 'Smart Wall-Saver' 팀의 개발 및 발표 로드맵

### 📌 주요 일정
- [x] **05.26** :  06.02 대면 점검 시간 확정, 패키지 분업 구조 세팅 및 깃허브 원격 저장소 동기화 완료
- [x] **06.02** : **교수님 대면 중간 점검** 🔍
  - *주요 목표*: 프로젝트 중간 점검(`18:10분 예정`), 핵심 기능 중심의 프로토타입 대면 피드백 수행
  - **👍 주요 피드백 사항**:
     -  전반적인 기능 구현 상태 양호 및 긍정적 평가 (기본 전선 탐지 로직)
    -  *수평계 정밀 구현* 마무리 (완료 후 깃허브 최종 동기화 필수)
     - 🎬 **5분 발표 영상 제작** : 최종 발표 시 구동 시연 및 핵심 내용을 압축한 5분 내외의 발표 녹화 영상 자료 준비 필요
- [x] **06.09** : **보강 주간 (수업 없음)** 💻
  - *주요 목표*: 중간 점검 피드백 반영, 센서 데이터 보정 및 UI 정밀 고도화 기간, 앱 통합 빌드 및 최종 프로젝트 결과물 제작,최종 앱 구동 시연
- [x] **06.16** : **최종 프로젝트 발표 및 배포** 🚀
  - *주요 목표*: 깃허브 최종 커밋, 최종 발표 자료 및 최종 보고서 제출
