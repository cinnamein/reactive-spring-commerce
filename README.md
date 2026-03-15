# Reactive Spring Commerce

> 이커머스 상품 관리 API — Kotlin + Spring WebFlux + Coroutines + R2DBC + DDD

비동기/논블로킹 기반의 상품 도메인 CRUD 및 상태 관리 API입니다.  
DDD(Domain-Driven Design) 아키텍처를 적용하여 도메인 불변식을 코드 수준에서 보장하고, Kotlin Coroutines 기반의 리액티브 스택으로 높은 동시성 환경에서의 안정적인 처리를 목표로 설계했습니다.

---

## 기술 스택

| 영역 | 기술 |
|------|------|
| Language | Kotlin 2.2 |
| Framework | Spring Boot 4.0, Spring WebFlux |
| Async | Kotlin Coroutines (suspend 기반) |
| Database | PostgreSQL 16 + R2DBC |
| Cache | Redis 7 (Reactive Lettuce) |
| Test | JUnit 6, MockK, TestContainers, k6 |
| Infra | Docker Compose, Gradle Kotlin DSL |

---

## WebFlux + Coroutines를 선택한 이유

이커머스 서비스는 상품 조회 트래픽이 전체의 80~90%를 차지합니다.  
기존 Spring MVC의 thread-per-request 모델에서는 동시 접속이 늘어날수록 스레드 풀이 고갈되어 응답 지연과 장애로 이어집니다.

Spring WebFlux는 Netty 기반의 이벤트 루프 모델을 사용하여 소수의 스레드로 수천 개의 동시 요청을 처리할 수 있습니다.  
여기에 Kotlin Coroutines를 결합하면 Mono/Flux 체이닝 없이 동기 코드처럼 읽히는 비동기 코드를 작성할 수 있어 유지보수성이 높아집니다.

```kotlin
// Mono/Flux 체이닝 (가독성 저하)
fun getProduct(id: Long): Mono<ProductResponse> =
    productRepository.findById(id)
        .switchIfEmpty(Mono.error(ProductNotFoundException(id)))
        .map { ProductResponse.from(it) }

// Coroutine suspend (동기 코드와 비슷한 가독성)
suspend fun getProduct(id: Long): ProductResponse {
    val product = productRepository.findById(id)
        ?: throw ProductNotFoundException(id)
    return ProductResponse.from(product)
}
```

R2DBC는 JDBC와 달리 데이터베이스 I/O까지 논블로킹으로 처리하여 WebFlux의 리액티브 파이프라인이 DB 호출에서 블로킹되지 않도록 보장합니다.

---

## 성능 최적화

### 부하 테스트 결과

k6를 사용하여 동시 접속 500명, 읽기 90% / 쓰기 10% 시나리오로 측정했습니다.

| 지표 | 최적화 전 | 최적화 후 | 개선 |
|------|-----------|-----------|------|
| 에러율 | 63.80% | **0.00%** | 완전 해소 |
| 총 처리량 (4분 20초) | 1,875건 | **65,554건** | **35배** |
| 처리량 (req/s) | 6.5 | **252** | **39배** |
| 평균 응답 시간 | 38,390ms | **1,008ms** | **97% 감소** |
| p95 응답 시간 | 60,000ms (타임아웃) | **4,676ms** | 정상 범위 |
| 단건 조회 평균 | 38,930ms | **927ms** | 97% 감소 |
| 전체 조회 평균 | 38,540ms | **1,006ms** | 97% 감소 |

- 테스트 환경: Docker Compose (MacBook Air M1 / PostgreSQL 16 + Redis 7 + App)
- 테스트 도구: k6 (grafana/k6 Docker 이미지)
- 시나리오: 단건 조회 60% + 전체 조회 30% + 등록 10%, 최대 동시접속 500

### 병목 분석 과정

초기 부하 테스트에서 **에러율 63.80%**, 대부분의 요청이 60초 타임아웃으로 실패했습니다.  
성공한 요청의 중간값은 4.74ms로, 앱 자체는 빠르지만 동시 접속이 올라가면 요청이 대기하다 타임아웃되는 패턴이었습니다.

**원인 1: R2DBC 커넥션 풀 고갈**  
기본 커넥션 풀 크기가 10개였습니다. 동시 500명이 요청하면 490개가 커넥션 대기 상태에 빠집니다.  
→ 커넥션 풀 최대 크기를 50으로 확장하여 해결했습니다.

**원인 2: findAll()의 N+1 쿼리**  
상품 전체 조회 시 각 상품마다 옵션/이미지를 개별 조회하여 상품 20개 기준 41개 쿼리(1 + 20 + 20)가 발생했습니다.  
→ IN 쿼리로 변경하여 상품 수와 무관하게 3개 쿼리(고정)로 줄였습니다.

**원인 3: 반복 조회의 DB 부하**  
동일 상품에 대한 반복 조회가 매번 DB를 거쳤습니다.  
→ Redis 캐싱을 적용하여 캐시 히트 시 DB를 거치지 않도록 했습니다.

### Redis 캐싱 전략

| 전략 | 설명 |
|------|------|
| **읽기** | 캐시 히트 시 DB 미조회, 미스 시 DB 조회 후 캐시 저장 |
| **쓰기** | DB 저장 후 해당 캐시 갱신 + 목록 캐시 무효화 |
| **TTL** | 10분 (데이터 일관성과 캐시 효율의 균형) |
| **장애 대응** | Redis 장애 시 DB fallback (모든 캐시 연산 try-catch) |

캐시는 성능 최적화 수단일 뿐 필수 의존성이 아닙니다. Redis가 다운되어도 애플리케이션은 DB에서 정상 조회되며, 경고 로그만 남깁니다.

---

## 아키텍처

### 계층 구조 (DDD)

```
presentation → application → domain ← infrastructure
     │              │           │            │
 Controller      UseCase     Aggregate    R2DBC 구현체
 (HTTP)       (오케스트레이션)  (불변식 보호)   (DB/Cache)
```

**domain 계층은 어떤 계층에도 의존하지 않습니다.**  
infrastructure가 domain의 Repository 인터페이스를 구현하며 (의존성 역전 원칙),  
이를 통해 DB를 교체하더라도 domain과 application 코드는 변경할 필요가 없습니다.

### 패키지 구조

```
src/main/kotlin/cinnamein/reactivespringcommerce/
├── common/
│   ├── response/ApiResponse.kt                  ← 공통 응답 포맷
│   └── exception/GlobalExceptionHandler.kt       ← 전역 예외 처리
├── config/RedisConfig.kt                         ← Redis 설정
└── product/
    ├── presentation/
    │   └── ProductController.kt                  ← REST API (9개 엔드포인트)
    ├── application/
    │   ├── dto/                                  ← Request/Response DTO (7개 파일)
    │   └── usecase/ProductUseCase.kt             ← 비즈니스 오케스트레이션
    ├── domain/
    │   ├── model/
    │   │   ├── Product.kt                        ← Aggregate Root
    │   │   ├── ProductOption.kt                  ← Value Object
    │   │   ├── ProductImage.kt                   ← Value Object
    │   │   ├── ProductStatus.kt                  ← 상태 Enum
    │   │   ├── ProductSize.kt                    ← 사이즈 Enum
    │   │   └── ProductColor.kt                   ← 색상 Enum
    │   ├── repository/ProductRepository.kt       ← Repository 인터페이스
    │   └── exception/ProductExceptions.kt        ← 도메인 예외
    └── infrastructure/
        ├── cache/ProductCacheManager.kt          ← Redis 캐시 관리
        ├── ProductEntity.kt
        ├── ProductOptionEntity.kt
        ├── ProductImageEntity.kt
        ├── R2dbcProductRepository.kt
        ├── R2dbcProductOptionRepository.kt
        ├── R2dbcProductImageRepository.kt
        ├── ProductMapper.kt                      ← Domain ↔ Entity 변환
        └── ProductRepositoryImpl.kt              ← Repository 구현체
```

---

## 도메인 설계

### Product Aggregate

Product는 Aggregate Root로서 Option과 Image의 생명주기를 관리합니다.  
모든 하위 객체는 반드시 Product를 통해서만 접근/변경할 수 있습니다.

```
Product (Aggregate Root)
├── name, price, seller, description
├── status: DRAFT → ON_SALE → SOLD_OUT → DISCONTINUED
│                                              ↑
│              어디서든 → HIDDEN                 │
├── options: List<ProductOption>  ← Value Object
│   └── size (Enum), color (Enum), additionalPrice, stockQuantity
└── images: List<ProductImage>   ← Value Object
    └── url, sortOrder, primaryImage
```

### 도메인 불변식

| 불변식 | 보장 위치 |
|--------|-----------|
| 상품명 공백 불가 | `Product.create()`, `updateInfo()` |
| 가격 > 0 | `Product.create()`, `updateInfo()` |
| 옵션 최소 1개 이상 | `Product.create()`, `removeOption()` |
| 동일 사이즈+색상 조합 중복 불가 | `Product.create()`, `addOption()` |
| 대표 이미지 정확히 1개 | `Product.create()`, `addImage()`, `removeImage()` |
| 상태 전이 규칙 | `publish()`, `soldOut()`, `discontinue()`, `hide()` |

이 불변식들은 모두 **도메인 엔티티 내부에서 보장**됩니다.  
Controller나 UseCase에서 검증하지 않으며, Product 생성자가 private이므로 반드시 `Product.create()` 또는 `Product.reconstitute()`를 통해서만 인스턴스를 만들 수 있습니다.

### 상태 전이

상태 전이는 도메인 메서드로만 가능하며, 허용되지 않는 전이 시 예외가 발생합니다.

```
DRAFT ──publish()──→ ON_SALE ──soldOut()──→ SOLD_OUT ──discontinue()──→ DISCONTINUED
                        │                                                    ↑
                        └─────────────discontinue()──────────────────────────┘

어떤 상태에서든 ──hide()──→ HIDDEN
```

---

## API

| Method | Path | 설명 |
|--------|------|------|
| `POST` | `/product` | 상품 등록 (DRAFT로 생성) |
| `GET` | `/product` | 전체 조회 (캐시 적용) |
| `GET` | `/product/{id}` | 단건 조회 (캐시 적용) |
| `PUT` | `/product/{id}` | 상품 수정 |
| `DELETE` | `/product/{id}` | 상품 삭제 |
| `PATCH` | `/product/{id}/publish` | DRAFT → ON_SALE |
| `PATCH` | `/product/{id}/sold-out` | ON_SALE → SOLD_OUT |
| `PATCH` | `/product/{id}/discontinue` | ON_SALE/SOLD_OUT → DISCONTINUED |
| `PATCH` | `/product/{id}/hide` | any → HIDDEN |

### 요청 예시

```bash
curl -X POST http://localhost:8080/product \
  -H "Content-Type: application/json" \
  -d '{
    "name": "캐시미어 코트",
    "price": 298000,
    "seller": "W컨셉",
    "description": "프리미엄 캐시미어 100%",
    "options": [
      {"size": "M", "color": "BLACK", "additionalPrice": 0, "stockQuantity": 10},
      {"size": "L", "color": "WHITE", "additionalPrice": 5000, "stockQuantity": 8}
    ],
    "images": [
      {"url": "https://cdn.example.com/main.jpg", "sortOrder": 0, "primaryImage": true}
    ]
  }'
```

### 응답 예시

```json
{
  "status": 201,
  "message": "생성 완료",
  "data": {
    "id": 1,
    "name": "캐시미어 코트",
    "price": 298000,
    "seller": "W컨셉",
    "description": "프리미엄 캐시미어 100%",
    "status": "DRAFT",
    "options": [
      {"size": "M", "color": "BLACK", "colorDisplayName": "검정", "additionalPrice": 0, "stockQuantity": 10},
      {"size": "L", "color": "WHITE", "colorDisplayName": "흰색", "additionalPrice": 5000, "stockQuantity": 8}
    ],
    "images": [
      {"url": "https://cdn.example.com/main.jpg", "sortOrder": 0, "primaryImage": true}
    ]
  },
  "timestamp": "2026-03-12T15:30:00"
}
```

---

## 테스트 전략

테스트 피라미드에 따라 3개 계층의 테스트를 작성했습니다.

```
        ┌─────────────────────────┐
        │   Integration Test      │  TestContainers + WebTestClient
        │   (API 전체 흐름)        │  실제 PostgreSQL로 검증
        ├─────────────────────────┤
        │   UseCase Test          │  MockK으로 Repository 모킹
        │   (비즈니스 흐름)        │  Coroutine 네이티브 테스트
        ├─────────────────────────┤
        │   Domain Unit Test      │  외부 의존성 없음
        │   (불변식, 상태 전이)     │  순수 Kotlin
        └─────────────────────────┘
```

| 계층 | 파일 | 검증 대상 |
|------|------|-----------|
| Domain | `ProductTest` | 생성, 상태 전이, 옵션/이미지 관리, 불변식 위반 |
| Domain | `ProductOptionTest` | Value Object 검증, 동등성 비교 |
| Domain | `ProductImageTest` | URL 공백, 정렬 순서 검증 |
| Application | `ProductUseCaseTest` | CRUD 흐름, 상태 전이, Repository 호출 검증 |
| Integration | `ProductIntegrationTest` | HTTP 상태 코드, JSON 응답, 데이터 정합성 |

| 기술 | 선택 이유 |
|------|----------|
| **MockK** | Kotlin Coroutine의 suspend 함수를 `coEvery`/`coVerify`로 네이티브 모킹 |
| **TestContainers** | H2가 아닌 실제 PostgreSQL 16으로 테스트하여 R2DBC 호환성까지 검증 |
| **WebTestClient** | WebFlux 전용 테스트 클라이언트로 논블로킹 환경에서 정확한 API 테스트 |

---

## 실행 방법

### 사전 요구사항

- Docker Desktop
- JDK 21

### 실행

```bash
# 앱 + PostgreSQL + Redis 동시 기동
docker compose up --build -d

# 정상 확인
curl http://localhost:8080/product
```

### 테스트

```bash
# 전체 테스트 (Docker Desktop 실행 필요)
./gradlew test

# Domain 테스트만 (Docker 불필요)
./gradlew test --tests "*ProductTest"

# 통합 테스트만 (Docker 필요 — TestContainers)
./gradlew test --tests "*IntegrationTest"
```

### 부하 테스트

```bash
# k6 설치 없이 Docker로 실행 (Mac)
docker run --rm -i grafana/k6 run \
  -e BASE_URL=http://host.docker.internal:8080 \
  - < k6/read-heavy-test.js
```

---

## 설계 결정 기록

| 결정 | 이유 |
|------|------|
| **price: Long** | 원화 기반 쇼핑몰은 정수 단위 저장이 표준. Double 부동소수점 오차 없음 |
| **private constructor + create()** | 생성 시 도메인 검증 강제. new로 우회 불가 |
| **reconstitute() 분리** | DB 복원 시 이미 검증된 데이터이므로 검증 skip. 생성과 복원의 의미 분리 |
| **size/color를 Enum으로** | 유효하지 않은 값이 컴파일 타임 또는 역직렬화 시점에서 즉시 차단 |
| **primaryImage 필드명** | Kotlin Boolean `is` prefix가 Jackson 직렬화 시 필드명이 변환되는 문제 회피 |
| **findAll() IN 쿼리** | N+1 쿼리(1+2N)를 3 쿼리(고정)로 변경. 상품 수 증가에도 쿼리 수 불변 |
| **R2DBC 커넥션 풀 50** | 기본값 10에서 확장. 동시 500명 부하 테스트에서 커넥션 고갈 방지 |
| **Options/Images Replace 전략** | R2DBC는 변경 감지를 지원하지 않아 삭제 후 재저장이 명확 |
| **ON DELETE CASCADE** | Product 삭제 시 하위 테이블 자동 정리 |
| **Redis 장애 시 DB fallback** | 캐시는 성능 최적화 수단이지 필수 의존성이 아님 |
| **Gradle Wrapper** | Dockerfile에서 gradle 이미지 태그 불일치 문제 방지 |
