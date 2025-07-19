# spring-batch-investment 프로젝트 분석

## 1. 프로젝트 개요

- **기능**: 금융감독원(FSS)의 금융상품 조회 API를 호출하여 예금 및 적금 상품 정보를 가져와 데이터베이스에 저장하고, 이를 Elasticsearch에 동기화하는 Spring Batch 애플리케이션입니다.
- **목표**: 정기적으로 금융 상품 데이터를 동기화하여 최신 정보를 유지하고, 검색 시스템에 반영합니다.

## 2. 기술 스택

- **언어**: Java 21
- **프레임워크**: Spring Boot 3.3.1, Spring Batch
- **데이터베이스**: MariaDB (운영), H2 (테스트)
- **데이터 접근**: Spring Data JPA, Spring Data JDBC
- **HTTP 클라이언트**: Spring WebFlux `WebClient` (비동기, 논블로킹)
- **의존성 관리**: Gradle
- **기타**: Lombok, ModelMapper, p6spy, Spotless (코드 포맷팅)

## 3. 프로젝트 구조 및 핵심 로직

### 3.1. 배치 작업 (Batch Job)

- **Job 이름**: `FINANCIAL_COMPANY_SYNC_JOB` (`BachConfig.java`)
- **실행 순서**:
    1. `financialSyncStep`: 금융 회사 정보 조회 및 저장
    2. `financialProductStatusUpdateStep`: 이전 동기화된 금융 상품의 상태를 `DELETED`로 초기화
    3. `financialProductSavingsSyncStep`: 예금 상품 정보 조회 및 저장 (API에 있는 상품은 `ACTIVE`로 업데이트)
    4. `financialProductInstallmentSavingsSyncStep`: 적금 상품 정보 조회 및 저장 (API에 있는 상품은 `ACTIVE`로 업데이트)
    5. `financialProductEsSyncStep`: RDB에 저장된 금융 상품 정보를 Elasticsearch에 동기화

### 3.2. 배치 단계 (Batch Step)

각 Step은 `ItemReader`, `ItemProcessor` (선택적), `ItemWriter`의 구조를 따릅니다.

- **`financialSyncStep`**
    - **Reader**: `FinancialCompanyItemReader`
        - `FssClient`를 사용하여 금융감독원 API에서 금융 회사 목록을 페이지별로 조회합니다.
    - **Writer**: `FinancialCompanyItemWriter`
        - 조회된 금융 회사 정보를 `FinancialCompanyRepository`를 통해 데이터베이스에 저장합니다.

- **`financialProductStatusUpdateStep`**
    - **Tasklet**: `FinancialProductStatusUpdateTasklet`
        - 현재 월에 해당하는 모든 금융 상품의 상태를 `DELETED`로 일괄 업데이트합니다. 이는 API에서 더 이상 제공되지 않는 상품을 식별하기 위함입니다.

- **`financialProductSavingsSyncStep` / `financialProductInstallmentSavingsSyncStep`**
    - **Reader**: `FinancialProductItemReader`
        - `FssClient`를 사용하여 예금 또는 적금 상품 목록을 페이지별로 조회합니다.
        - `FinancialProductType` (SAVINGS, INSTALLMENT_SAVINGS)으로 상품 종류를 구분합니다.
    - **Writer**: `FinancialProductItemWriter`
        - 조회된 금융 상품 및 옵션 정보를 `FinancialProductRepository`를 통해 데이터베이스에 저장합니다. 이때, API에서 조회된 상품은 `ACTIVE` 상태로 업데이트됩니다.

- **`financialProductEsSyncStep`**
    - **Reader**: `FinancialProductEsItemReader`
        - RDB에서 `FinancialProductEntity`를 페이지별로 조회합니다.
    - **Processor**: `FinancialProductEsItemProcessor`
        - `FinancialProductEntity`를 Elasticsearch에 저장할 `FinancialProductDocument`로 변환합니다.
    - **Writer**: `FinancialProductEsItemWriter`
        - 변환된 `FinancialProductDocument`를 `FinancialProductEsRepository`를 통해 Elasticsearch에 저장합니다.

### 3.3. 외부 API 연동 (`FssClient`)

- `WebClient`를 사용하여 금융감독원 API (`finlife.fss.or.kr`)와 통신합니다.
- **주요 기능**:
    - `getCompanies`: 금융 회사 정보 조회
    - `getFinancialProducts`: 예/적금 상품 정보 조회
- **에러 처리 및 재시도**:
    - `WebClientRequestException` (타임아웃 등) 발생 시 `FssUnavailableError`를 발생시킵니다.
    - `Retry.backoff`을 사용하여 `FssUnavailableError` 발생 시 최대 3회까지 재시도를 수행합니다. (초기 1초 backoff)
    - 4xx/5xx HTTP 에러 발생 시 `FssClientError`를 발생시킵니다.

### 3.4. 데이터베이스 및 엔티티

- **주요 엔티티**:
    - `FinancialCompanyEntity`: 금융 회사 정보
    - `FinancialProductEntity`: 금융 상품 정보 (새로운 `ProductStatus` enum 필드 포함)
    - `FinancialProductOptionEntity`: 금융 상품 옵션 (금리 등)
- **주요 Elasticsearch Document**:
    - `FinancialProductDocument`: 금융 상품 검색을 위한 Elasticsearch 문서 (새로운 `status` 필드 포함)
- **설정**:
    - `application.yml`에 `local`, `test`, `prod` 프로필별로 데이터베이스 연결 정보를 관리합니다.
    - `ddl-auto`는 `validate`로 설정되어 있어, 애플리케이션 실행 시 엔티티와 테이블 스키마가 일치하는지 검증합니다.

## 4. CI/CD (GitHub Actions)

- **워크플로우**: `.github/workflows/ci.yml`
- **트리거**: `releases-**` 브랜치에 push될 때 실행됩니다.
- **주요 작업**:
    1. **`build`**:
        - Java 21 환경을 설정하고 Gradle로 프로젝트를 빌드합니다.
        - 브랜치 이름에서 버전을 추출합니다.
        - Docker 이미지를 빌드하여 Docker Hub에 푸시합니다.
    2. **`update-helm-chart`**:
        - `build` 작업 완료 후 실행됩니다.
        - `hjj1991/helm-charts` 리포지토리의 `values.yaml` 파일을 업데이트하여 배포된 Docker 이미지 태그를 갱신합니다.

## 5. 실행 방법

### 5.1. 로컬 환경 실행

1. **환경 변수 설정**:
   - `FSS_AUTH_KEY`: 금융감독원 API 인증키를 설정합니다.
   - `GCP_PROJECT_ID`: Google Cloud Project ID를 설정합니다 (Elasticsearch 연동 시 필요).
2. **애플리케이션 실행**:
   - `local` 프로필을 활성화하여 실행합니다. (`spring.profiles.active=local`)
   - `job.name` 파라미터로 실행할 Job의 이름을 지정할 수 있습니다. (예: `FINANCIAL_COMPANY_SYNC_JOB`)

### 5.2. 테스트 실행

- `./gradlew test` 명령어를 사용하여 JUnit5 기반의 테스트를 실행합니다.
- 테스트 시에는 `test` 프로필이 활성화되며, H2 인메모리 데이터베이스를 사용합니다.

## 6. 주요 개선 및 고려사항

- **트랜잭션 관리**: 현재 `ResourcelessTransactionManager`를 사용하고 있어, 배치 처리 중 실패 시 롤백이 적용되지 않습니다. 데이터 정합성이 중요하다면 `DataSourceTransactionManager` 사용을 고려해야 합니다.
- **에러 처리**: API 호출 실패나 데이터 처리 중 에러 발생 시, 해당 배치를 실패 처리하고 알림을 보내는 등의 후속 조치 로직을 강화할 수 있습니다.
- **성능**: 대용량 데이터 처리 시 `chunk` 크기 조절, `PagingItemReader` 사용, 병렬 처리(multi-threaded step) 등을 통해 성능을 최적화할 수 있습니다.

## 7. 코딩 컨벤션

- **멤버 변수 접근**: 클래스 내부의 멤버 변수(필드)에 접근할 때는 `this` 키워드를 명시적으로 사용합니다. (예: `this.financialProductRepository.save(...)`)
