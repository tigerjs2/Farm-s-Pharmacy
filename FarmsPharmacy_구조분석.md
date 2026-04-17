# 🌱 Farm's Pharmacy — 파일 구조 및 연결 관계 분석

---

## 📁 전체 파일 목록 한눈에 보기

| 파일 | 분류 | 한 줄 요약 |
|------|------|-----------|
| `LoginActivity.kt` | Activity | 이메일/비밀번호 로그인, 자동 로그인 처리 |
| `SignUpActivity.kt` | Activity | 회원가입, Firebase Auth + Firestore 저장 |
| `MainActivity.kt` | Activity | 앱 메인 화면, 하단 네비게이션 바 관리 |
| `ProfileActivity.kt` | Activity | 프로필 요약 화면 (이름 표시, 로그아웃) |
| `MyInfoActivity.kt` | Activity | 내 정보 수정 (이름, 프로필 사진, 비밀번호) |
| `HistoryActivity.kt` | Activity | 작물별 진단 기록 목록 화면 |
| `ResultActivity.kt` | Activity | 진단 결과 화면 (ViewPager2로 페이지 전환) |
| `HomeFragment.kt` | Fragment | 홈 탭 — 인사말, 프로필 이동 버튼 |
| `CameraFragment.kt` | Fragment | 카메라 탭 — 작물 선택 (촬영용) |
| `CheckFragment.kt` | Fragment | 기록 확인 탭 — 작물 선택 (히스토리용) |
| `PhotoFragment.kt` | Fragment | 실제 카메라 촬영 화면 (CameraX) |
| `ResultFragment.kt` | Fragment | 진단 결과 표시 (정상/질병/인식불가) |
| `DetailFragment.kt` | Fragment | 질병 상세 정보 (증상, 대처법) |
| `HistoryAdapter.kt` | Adapter | 히스토리 목록 RecyclerView 어댑터 |
| `HistoryItem.kt` | DataClass | 히스토리 아이템 데이터 모델 |

---

## 🔗 전체 화면 흐름도

```
앱 시작
  └─ LoginActivity
       ├─ (자동 로그인) ──────────────────────────────────────┐
       └─ 로그인 성공 → MainActivity                           │
                                                              │
MainActivity ◄─────────────────────────────────────────────┘
  ├─ [홈 탭]      → HomeFragment
  │                   └─ 프로필 아이콘 클릭 → ProfileActivity
  │                                              ├─ 내 정보 → MyInfoActivity
  │                                              └─ 로그아웃 → LoginActivity
  │
  ├─ [카메라 탭]  → CameraFragment
  │                   └─ 작물 버튼 클릭 → PhotoFragment
  │                                          └─ 촬영 → (저장) → ResultActivity
  │                                                               ├─ [0] ResultFragment
  │                                                               └─ [1] DetailFragment (DISEASE만)
  │
  └─ [기록 탭]    → CheckFragment
                      └─ 작물 버튼 클릭 → HistoryActivity
                                             └─ 썸네일 클릭 → ResultActivity
```

---

## 📄 각 파일 상세 설명

---

### 1. `LoginActivity.kt`
**역할:** 앱의 진입점. 이메일/비밀번호로 Firebase 로그인 처리.

**주요 기능:**
- `mAuth.currentUser != null` 이면 자동으로 `MainActivity`로 이동 (자동 로그인)
- 로그인 성공 시 `MainActivity` 시작
- "회원가입" 텍스트에 `ClickableSpan` 적용 → `SignUpActivity`로 이동
- 로고 텍스트("파머" 부분만 진한 초록) 투톤 스타일 적용

**연결:**
- → `MainActivity` (로그인 성공)
- → `SignUpActivity` (회원가입 클릭)

---

### 2. `SignUpActivity.kt`
**역할:** 신규 회원가입. Firebase Auth로 계정 생성 + Firestore에 유저 정보 저장.

**주요 기능:**
- 이메일/비밀번호/이름/비밀번호 확인 입력
- 비밀번호 6자 미만 / 불일치 유효성 검사
- `mAuth.createUserWithEmailAndPassword()` → 성공 시 Firestore `Users/{uid}` 문서 생성
- Firestore 저장 필드: `email`, `name`, `createdAt`, `farmLocation`, `profileImageUrl`

**연결:**
- → `MainActivity` (회원가입 성공)
- → `LoginActivity` (finish()로 뒤로가기)

---

### 3. `MainActivity.kt`
**역할:** 앱의 메인 컨테이너. 하단 네비게이션 바로 Fragment를 교체.

**주요 기능:**
- `fragmentContainer`에 Fragment를 `replace()`로 전환
- 탭 5개: 홈 / 카메라 / 캘린더(TODO) / 가이드(TODO) / 지도(TODO)
- 선택된 탭에 `nav_item_selected` 배경 drawable 적용

**연결:**
- → `HomeFragment` (홈 탭)
- → `CameraFragment` (카메라 탭)

---

### 4. `HomeFragment.kt`
**역할:** 홈 화면. Firebase에서 유저 이름 불러와 인사말 표시.

**주요 기능:**
- Firestore `Users/{uid}.name` 조회 → `"Hello, {이름}님!"` 표시
- `onResume()`에서도 이름 재조회 (다른 화면에서 이름 변경 후 돌아올 때 반영)
- 프로필 아이콘 클릭 → `ProfileActivity` 이동

**연결:**
- → `ProfileActivity` (프로필 아이콘 클릭)

---

### 5. `ProfileActivity.kt`
**역할:** 프로필 요약 화면. 이름 표시, 내 정보 이동, 로그아웃.

**주요 기능:**
- Firestore에서 이름 조회 → `tvProfileName`에 표시
- `onResume()`에서 이름 재조회
- "내 정보" 클릭 → `MyInfoActivity`
- "로그아웃" 클릭 → `mAuth.signOut()` + `LoginActivity` 이동 + `finishAffinity()`

**연결:**
- → `MyInfoActivity` (내 정보 클릭)
- → `LoginActivity` (로그아웃)

---

### 6. `MyInfoActivity.kt`
**역할:** 내 정보 수정 화면. 이름/프로필 사진/비밀번호 변경 가능.

**주요 기능:**
- Firestore에서 이름 + 프로필 이미지 URL 불러오기 (Glide로 이미지 표시)
- 이름 클릭 시 EditText 활성화 → 완료 시 Firestore 업데이트
- 프로필 사진: 갤러리에서 선택 → Firebase Storage 업로드 → Firestore URL 저장
- 비밀번호 변경: 현재 비밀번호로 재인증(`reauthenticate`) 후 `updatePassword()`

**사용 라이브러리:** Firebase Auth, Firestore, Storage, Glide

---

### 7. `CameraFragment.kt`
**역할:** 카메라 탭의 작물 선택 화면 (촬영 목적).

**주요 기능:**
- 작물 버튼 6개 (오이/딸기/파프리카/포도/고추/토마토)
- 버튼 클릭 시 `PhotoFragment.newInstance(cropName)`으로 전환 (`addToBackStack` 포함)
- "기록 확인" 버튼 → `CheckFragment`로 전환

**연결:**
- → `PhotoFragment` (작물 버튼 클릭)
- → `CheckFragment` (기록 확인 버튼)

---

### 8. `CheckFragment.kt`
**역할:** 기록 확인 탭의 작물 선택 화면 (히스토리 조회 목적).

**주요 기능:**
- 작물 버튼 6개 (오이/딸기/파프리카/포도/고추/토마토)
- 버튼 클릭 시 `HistoryActivity`로 이동하며 `EXTRA_CROP_NAME` 전달
- 카메라 버튼 → `CameraFragment`로 전환

> ⚠️ `CameraFragment`와 레이아웃이 유사하지만 **목적이 다름**  
> `CameraFragment` → 촬영하러 가는 길  
> `CheckFragment` → 기록 보러 가는 길

**연결:**
- → `HistoryActivity` (작물 버튼 클릭)
- → `CameraFragment` (카메라 버튼)

---

### 9. `PhotoFragment.kt`
**역할:** 실제 카메라 촬영 화면. CameraX로 사진 촬영 후 히스토리 저장.

**주요 기능:**
- CameraX `Preview` + `ImageCapture` 바인딩
- 카메라 권한 요청 (`ActivityResultContracts.RequestMultiplePermissions`)
- 촬영 후 MediaStore에 이미지 저장 (`Pictures/Farm-s-Pharmacy/`)
- `HistoryItem` 생성 후 SharedPreferences(`HistoryPrefs`)에 JSON 배열로 저장
- `ResultActivity`로 이동 (현재는 더미 데이터: `diagType=DISEASE`, `label=노균병`, `confidence=92`)

**저장 방식:**
```kotlin
prefs.getString("history_items", "[]")
// → Array<HistoryItem>으로 역직렬화
// → 새 항목 추가
// → 다시 JSON으로 직렬화해서 저장
```

> ⚠️ **TODO:** 실제 AI 모델 연동 시 더미 데이터(`노균병`, `92`) → API 응답값으로 교체 필요

**연결:**
- → `ResultActivity` (촬영 완료)

---

### 10. `ResultActivity.kt`
**역할:** 진단 결과 화면 컨테이너. ViewPager2로 세로 스와이프 페이지 구성.

**주요 기능:**
- Intent에서 `imageUri`, `cropName`, `diagType`, `label`, `confidence` 수신
- `diagType == "DISEASE"` → 2페이지 (ResultFragment + DetailFragment)
- `diagType != "DISEASE"` → 1페이지 (ResultFragment만) + 스와이프 비활성화

**연결:**
- [page 0] → `ResultFragment`
- [page 1] → `DetailFragment` (DISEASE일 때만)

---

### 11. `ResultFragment.kt`
**역할:** 진단 결과 표시. diagType에 따라 3가지 다른 UI 표시.

**주요 기능:**

| diagType | 표시 내용 | 색상 |
|----------|-----------|------|
| `DISEASE` | 질병명 + "이 의심돼요" | 노란색 |
| `NORMAL` | "정상" + "적인 잎입니다" | 파란색 |
| `UNKNOWN` | "인식하지 못했어요" | 회색 |

- 로고 투톤 스타일 (`SpannableString`)
- 촬영 이미지 표시 (`imageUri` → `setImageURI`)
- DISEASE일 때만 스와이프 안내(`detailButtonLayout`) 표시

> ⚠️ **TODO:** `getDummyBriefDesc()` → 실제 API 응답으로 교체 필요

---

### 12. `DetailFragment.kt`
**역할:** 질병 상세 정보 화면. 증상 목록과 대처법 표시.

**주요 기능:**
- "이런 증상을 보여요" / "이렇게 대처해요" 투톤 타이틀
- 증상 5개 + 대처법 3개를 동적으로 bullet 리스트 생성 (`makeBulletItem()`)
- ScrollView 터치 시 부모 ViewPager2의 스와이프 방지 (`requestDisallowInterceptTouchEvent`)
- Paperlogy Light 폰트 적용

> ⚠️ **TODO:** 현재 더미 데이터 → API 연동 시 `cropName` 기반으로 실제 데이터 조회 필요

---

### 13. `HistoryActivity.kt`
**역할:** 작물별 진단 기록 목록 화면. 필터/정렬 기능 포함.

**주요 기능:**
- `CheckFragment`에서 전달받은 `cropName`으로 해당 작물 기록만 필터링
- 질병 칩 필터 (ON/OFF 토글, 작물마다 다른 질병 목록)
- 최신순/오래된순 정렬 (`BottomSheetDialog`)
- RecyclerView `GridLayoutManager` (3열) + 날짜 헤더 (span=3)
- 썸네일 클릭 → `ResultActivity`로 이동

**데이터 로딩:**
```kotlin
SharedPreferences("HistoryPrefs") → "history_items" (JSON 배열)
→ 방어 코드로 깨진 데이터 자동 제거
→ cropName 필터링 후 표시
```

**작물별 질병 매핑:**
| 작물 | 질병 |
|------|------|
| 오이 | 노균병, 흰가루병, 기타 |
| 딸기 | 흰가루병, 기타 |
| 포도 | 노균병, 기타 |
| 고추 | 흰가루병, 기타 |
| 토마토 | 흰가루병, 잿빛곰팡이병, 기타 |
| 파프리카 | 흰가루병, 기타 |

**연결:**
- → `ResultActivity` (썸네일 클릭)

---

### 14. `HistoryAdapter.kt`
**역할:** 히스토리 RecyclerView 어댑터. 날짜 헤더 + 이미지 썸네일 혼합 표시.

**ViewType 2종:**
- `VIEW_TYPE_DATE (0)` → 날짜 헤더 (span 3 전체)
- `VIEW_TYPE_IMAGE (1)` → 이미지 썸네일

**동작 방식:**
```
[HistoryItem 리스트]
  └─ 날짜별로 그룹핑 (LinkedHashMap)
  └─ flatMap으로 [날짜문자열, item, item, 날짜문자열, item, ...] 형태로 변환
  └─ displayItems에 저장 → RecyclerView에 표시
```

- `imageUri` 있으면 `setImageURI()`, 없으면 `imageResId`로 폴백
- 아이템 클릭 시 `onItemClick` 콜백 실행 (`HistoryActivity`에서 `ResultActivity`로 이동)

---

### 15. `HistoryItem.kt`
**역할:** 히스토리 데이터 모델 (data class).

```kotlin
data class HistoryItem(
    val id: Int,           // System.currentTimeMillis().toInt() — 고유 ID 겸 정렬 기준
    val imageResId: Int,   // drawable 리소스 ID (갤러리 없을 때 폴백)
    val imageUri: String?, // 실제 촬영 이미지 URI (MediaStore)
    val diseaseName: String, // "노균병" / "정상" 등
    val confidence: Int,   // 신뢰도 (0~100)
    val date: String,      // "yyyy.MM.dd" 형식 — 날짜 헤더 그룹핑 기준
    val cropName: String,  // "오이" / "딸기" 등
    val diagType: String   // "DISEASE" / "NORMAL" / "UNKNOWN"
)
```

SharedPreferences에 **JSON 배열**로 직렬화/역직렬화되어 저장됨.

---

## 💾 데이터 흐름 — 히스토리 저장/조회

```
[PhotoFragment] 촬영 완료
  └─ HistoryItem 생성
  └─ SharedPreferences("HistoryPrefs")
       └─ key: "history_items"
       └─ value: JSON 배열 문자열 "[{...}, {...}]"

[HistoryActivity] 화면 열릴 때
  └─ "history_items" 읽기
  └─ 방어 코드: "[" 로 시작하지 않으면 삭제
  └─ Gson으로 List<HistoryItem> 역직렬화
  └─ cropName으로 필터링
  └─ HistoryAdapter에 전달
```

---

## 🔥 Firebase 사용 현황

| 기능 | 서비스 |
|------|--------|
| 로그인/회원가입 | Firebase Auth |
| 유저 정보 (이름, 위치, 이미지 URL) | Firestore `Users/{uid}` |
| 프로필 사진 | Firebase Storage `profile_images/{uid}.jpg` |
| 이미지 로딩 | Glide |

> 진단 이력(HistoryItem)은 Firebase **미사용** — 기기 로컬 SharedPreferences에만 저장

---

## ⚠️ 현재 TODO 목록

| 위치 | 내용 |
|------|------|
| `PhotoFragment.takePhoto()` | 더미 데이터(노균병, 92%) → 실제 AI API 연동 |
| `ResultFragment.getDummyBriefDesc()` | 더미 설명 → API 응답으로 교체 |
| `DetailFragment.bindDummyData()` | 더미 증상/대처법 → cropName 기반 API 연동 |
| `MainActivity` | 캘린더/가이드/지도 탭 구현 |
| HistoryItem 저장소 | SharedPreferences → Firebase Firestore로 마이그레이션 고려 |
