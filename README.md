# MapPilot (Android)

카카오맵 SDK로 만든 지도 앱입니다. 내 위치 따라가기, 장소 검색, 자동차·도보·대중교통 길찾기를 네이버 지도와 비슷한 흐름으로 구현했습니다.
같은 기능을 iOS 네이티브로도 만들었습니다 → [MapPilot-iOS](https://github.com/sinadsl1457/MapPilot-iOS)

| 지도·내 위치 | 장소 검색 | 길찾기 |
|---|---|---|
| <!-- docs/screenshots/map.png --> | <!-- docs/screenshots/search.png --> | <!-- docs/screenshots/route.png --> |

## 주요 기능

- **내 위치 따라가기** — 버튼을 누를 때마다 내 위치로 이동 → 따라가기 → 진행 방향으로 지도 회전 → 해제. 지도를 끌면 따라가기가 풀립니다.
- **나침반 방향 표시** — 멈춰 있을 때 폰이 향한 방향을 화살촉 또는 부채꼴로 보여 줍니다.
- **장소 검색** — 입력이 0.3초 멈추면 엔터 없이 결과를 미리 보여 주고, 입력한 단어를 이름에서 강조합니다. 카테고리(음식점·카페·편의점 등) 검색과 최근 검색어도 있습니다.
- **결과 목록 시트** — 목록을 끌어 올리면 검색창 바로 아래까지 펼쳐지고, 같은 손동작이 그대로 목록 스크롤로 이어집니다.
- **길찾기** — 출발지·도착지를 고르고 맞바꿀 수 있고, 최근 장소를 바로 고를 수 있습니다. 세 이동 수단을 동시에 조회해 탭마다 소요 시간을 보여 줍니다.
  - 대중교통은 경로 목록을 먼저 보여 주고, 고른 경로만 지도에 그립니다. 지하철 구간은 노선 공식 색(2호선 초록 등)으로 그립니다.
  - 자동차는 예상 택시비·통행료도 보여 줍니다.

## 기술적으로 신경 쓴 부분

| 문제 | 해결 |
|---|---|
| 글자마다 검색 API를 부르면 호출 한도를 쓰고, 늦게 온 이전 응답이 최신 결과를 덮어씀 | `debounce` + `mapLatest`로 마지막 입력만 요청하고 이전 요청은 취소. 공백만 바뀐 입력은 `distinctUntilChanged`로 다시 부르지 않음 |
| 검색 결과·경로에 카메라를 맞추면 위 검색창·아래 시트에 출발·도착이 가려짐 | 상단 UI와 시트가 실제로 가리는 높이를 재서 SDK 여백(`setPadding`)에 넣고 그 안쪽에 맞춤 |
| 검색 결과가 여러 도시에 흩어지면(예: "상상마당" → 홍대·부천·춘천) 너무 멀리 줌아웃됨 | 반경 안에 이웃이 가장 많은 결과 묶음에만 카메라를 맞춤(나머지는 목록·마커에 그대로) |
| 나침반 값이 떨리고 자북 기준이라 지도(진북)와 어긋남(서울 약 -9°) | 회전 벡터 센서 값에 sin·cos 저역 통과 필터, `GeomagneticField` 편각으로 진북 보정. 앱이 화면에 있을 때만 센서를 켬 |
| 시트를 손잡이로만 끌 수 있어 목록을 더 보기 불편함 | `NestedScrollConnection`으로 목록 스크롤과 시트 끌기를 한 손동작으로 이음 |
| 선언형 Compose와 명령형 카카오맵 SDK 연결, 지도 준비(1~2초) 전에 들어온 명령 | `KakaoMapState`가 다리 역할을 하며, 준비 전 명령은 보관했다가 지도가 붙는 순간 실행 |

## 구조

```
com.example.simpletracking/
├ domain/          순수 Kotlin. 모델, 저장소 인터페이스, UseCase(검색어 정리·거리 제한 같은 규칙)
├ data/            카카오 로컬·내비·길찾기 API(Retrofit), DTO → 도메인 변환, SharedPreferences 저장소, GPS
├ di/              Hilt 모듈(네트워크·저장소·위치)
└ presentation/    Jetpack Compose 화면 + ViewModel
   ├ map/          지도 화면, 내 위치·따라가기(MapViewModel)
   │  ├ kakao/     카카오맵 SDK를 감싸는 곳(SDK 타입은 이 패키지 밖으로 나가지 않음)
   │  ├ search/    장소 검색(PlaceSearchViewModel)
   │  └ route/     길찾기 결과·준비 화면·출발지 선택(ViewModel 3개)
   └ common/       하단 시트, 거리·시간 표시, 오류 문구
```

- 의존 방향은 `presentation → domain ← data`입니다. domain은 Android·SDK를 모릅니다.
- ViewModel은 화면 상태(`StateFlow`)와 일회성 이벤트(카메라 이동, 스낵바 — `Channel`)를 나눕니다.
- 화면(`MapScreen`)은 상태를 받아 그리기만 하고 입력은 콜백으로 올려 보내서, ViewModel 없이 가짜 상태로 그릴 수 있습니다.

## 기술 스택

Kotlin · Jetpack Compose (Material 3) · Coroutines/Flow · Hilt · Retrofit + kotlinx.serialization · OkHttp · Kakao Maps SDK v2 · Fused Location Provider · JUnit4 + kotlinx-coroutines-test

## 테스트

단위 테스트 147개(UseCase, 저장소, 매퍼, ViewModel). 저장소는 직접 만든 가짜 구현으로 바꿔 끼우고, 디바운스처럼 시간이 걸리는 동작은 가상 시간(`advanceTimeBy`)으로 확인합니다.

```bash
./gradlew testDebugUnitTest
```

## 실행 방법

필요 환경: Android Studio(JDK 17 이상 내장), minSdk 24 / targetSdk 36. 카카오맵 SDK가 ARM용만 제공해 ARM 기기·에뮬레이터에서 실행됩니다.

1. [카카오 디벨로퍼스](https://developers.kakao.com)에서 앱을 만들고, 플랫폼 → Android에 패키지 이름과 키 해시를 등록합니다.
2. 프로젝트 루트의 `local.properties`에 키를 넣습니다(git에 올라가지 않습니다).
   ```properties
   KAKAO_NATIVE_APP_KEY=네이티브 앱 키
   KAKAO_REST_API_KEY=REST API 키
   ```
3. Android Studio로 열고 실행합니다. 에뮬레이터는 Extended controls → Location의 가상 위치를 씁니다.
