
# Tiggle 백엔드 서버

‘티끌’ 같은 잔돈이 ‘태산’ 같은 변화를 만드는 금융 소셜 플랫폼

---

## ✨ 주요 기능

### 👤 인증/인가

* **통합 인증**: 이메일 + SMS(CoolSMS) 본인 확인
* **비밀번호 보안**: BCrypt 해시 저장
* **JWT 기반**: Access/Refresh 발급으로 **Stateless** 운영

```java
// SecurityConfig.java (발췌)
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/auth/join", "/api/auth/login").permitAll()
            .anyRequest().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
}
```

### ❤️ 기부

* **다양한 기부처 선택**, **투명한 내역 관리(DonationHistory)**
* **주간 정산**(대학/테마별 합산 송금), **랭킹/통계 제공**
* **캐시 업데이트**로 조회 성능 최적화

```java
// DonationService.java (발췌)
public interface DonationService {
    DonationStatus getUniversityDonationStatus(Long userId);
    DonationStatus getTotalDonationStatus();
    List<DonationRanking> getUniversityRanking();
    List<DonationRanking> getDepartmentRanking(Long userId);
    @Transactional void transferDonations();
    @Transactional(readOnly = true) void updateRankingCache();
}
```

### 💸 더치페이

* **간편 정산 요청/관리**, **상태 추적(PENDING/PAID)**
* **정산 이벤트 FCM 알림**

```java
public enum DutchpayShareStatus { PENDING, PAID }
```

### 🐷 돼지 저금통

* **목표 기반 저축**, **자투리(1,000원 미만) 자동 이체**
* **주간 자동 저축 스케줄러** 지원

```java
// WeeklyAutoSavingScheduler.java (발췌)
@Scheduled(cron = "0 0 18 ? * SUN", zone = "Asia/Seoul")
@Transactional
public void runWeeklyChangeSweep() { /* autoSaving 사용자 주간 이체 */ }
```

### 🔔 알림

* FCM으로 **더치페이/기부/목표 달성** 등 주요 이벤트 실시간 푸시

### 🎬 AI 콘텐츠(옵션)

* **TTS**(Google Cloud)로 공지/요약 음성 변환
* **Generative Video**로 기부/목표 달성 하이라이트 영상 생성

---

## ⏰ 스케줄러 (개요)

1. **WeeklyAutoSavingScheduler** — *일요일 18:00 KST*

   * 주계좌 잔액의 **천원 미만** 금액을 저금통으로 자동 이체
   * 주차 태그(`[W:yyyyMMdd]`)로 **멱등** 보장(중복 실행 방지)

2. **WeeklyUniversityDonationScheduler** — *토요일 06:56 KST*

   * 목표 달성 + 자동기부 ON 사용자 금액을 **대학 테마 계좌로 합산 송금**
   * `donation_ready=1→0` **슬롯 선점(UPDATE)** 로 중복 처리 방지
   * 처리 결과 **파일 로깅**, 실패 시 플래그 복구로 **다음 주기 재시도**

3. **DonationScheduler** — *일요일 20:00 KST*

   * 주중 모인 금액 **최종 송금**, **랭킹 캐시 업데이트**

```java
@Scheduled(cron = "0 0 20 * * SUN", zone = "Asia/Seoul")
public void runWeeklyDonation() {
    donationService.transferDonations();
    donationService.updateRankingCache();
}
```

---

## ❤️ 기부 기능 상세

### 1) 안전한 이체 파이프라인 (리액티브)

* DB 조회 → **잔액 확인(WebClient)** → **이체(WebClient)** → **성공 시에만 원장 기록**
* 이체 실패 시 **DB 미기록**으로 정합성 유지

```java
// DonationServiceImpl.createDonation() (요약)
return financialApiService.inquireDemandDepositAccountBalance(userKey, accNo)
    .flatMap(bal -> balEnough
        ? financialApiService.updateDemandDepositAccountTransfer(/*...*/)
        : Mono.error(DonationException.accountBalance(/*...*/)))
    .then(Mono.fromCallable(() -> donationHistoryRepository.save(/*...*/)));
```

> **주의**: WebClient는 비동기지만 **JPA는 블로킹**. 필요 시 `Schedulers.boundedElastic()` 오프로딩 또는 명령형 `@Transactional` 경계 내 처리 권장.

### 2) 랭킹/통계 집계 (JPQL + Projection)

* **필요 컬럼만** 조회해 트래픽/메모리 절감, DB에서 직접 집계

```java
@Query("""
  SELECT u.university.name AS name,
         SUM(d.amount)     AS amount,
         RANK() OVER (ORDER BY SUM(d.amount) DESC) AS rank
  FROM DonationHistory d
  JOIN d.user u
  GROUP BY u.university.name
  ORDER BY SUM(d.amount) DESC
""")
List<RankingProjection> getUniversityRanking();

public interface RankingProjection {
    Integer getRank(); String getName(); BigDecimal getAmount();
}
```

---

## 🛠 기술 스택

* **Backend**: Spring Boot 3.5.4, Java 17, Spring Security(JWT), Spring Data JPA, WebClient(WebFlux), MySQL 8.4, Redis 7, SpringDoc(OpenAPI)
* **DevOps**: GitHub Actions, Docker
* **External**: Firebase Admin SDK, CoolSMS, Google Cloud TTS/Generative, Jsoup

---

## 🚀 시작하기

### 사전 요구사항

* Java 17 / MySQL 8.4 / Redis 7

### 빌드 & 실행

```bash
./gradlew build
java -jar build/libs/tiggle-0.0.1-SNAPSHOT.jar
# 기본: http://43.203.36.96:80
```

---

## 📖 API 문서

* Swagger UI: **`http://43.203.36.96/swagger-ui/index.html`**

---

## 📁 패키지 구조

```
com.ssafy.tiggle
├── config           # Security/Swagger/Web 설정
├── constants        # 상수
├── controller       # REST API
├── domain           # 도메인 이벤트 등
├── dto              # Request/Response DTO
├── entity           # JPA 엔티티
├── exception        # 전역/커스텀 예외
├── repository       # JPA Repository
├── scheduler        # 스케줄러
├── security         # JWT/필터
├── service          # 비즈니스 로직
└── util             # 유틸
```

---

## 🔄 CI/CD (개요)

* `main` 푸시 시: **빌드 → Docker 이미지 푸시 → 서버 배포**

1. JDK 17/Gradle 세팅 → `./gradlew build`
2. Docker 이미지 빌드/푸시
3. 원격 서버에서 `docker compose pull && docker compose up -d`
