# login-practice
연습 환경


로컬: C:\Users\User\Downloads\login-practice
Git: GitHub Desktop 사용, 로컬 커밋 진행 중
연습용 별도 프로젝트. 팀 repo(Team-IZ/Backend)와 무관


기술 스택 (실제 확인된 버전)

Spring Boot 4.1.0   ← Initializr에 3.x가 없어서 4.x 사용
Spring Framework 7.0.8
Spring Security 7.1.0
Hibernate ORM 7.4.1
H2 2.4.240 (인메모리)
jjwt 0.12.6
Lombok 1.18.46
Java 17 (Boot 4의 최소 요구사항)
포트 8081


Boot 4 주의점: spring-boot-starter-web이 아니라 **spring-boot-starter-webmvc**로 들어옴(모듈화). H2 콘솔도 spring-boot-h2console 별도 모듈. 블로그(대부분 3.x 기준)와 문법이 다를 수 있음. import jakarta.*는 그대로.



구현 범위

ERD에서 roles, app_user 2개 테이블만 잘라내서 연습. refresh_token, one_time_token, permission, role_permission, commit_email은 범위 밖. app_user.org_id도 제외.

프로젝트 구조

com.example.loginpractice
├─ LoginPracticeApplication   앱 시작 + 역할 3개 초기 생성(CommandLineRunner)
├─ config
│  ├─ SecurityConfig          BCrypt 빈, 필터체인, STATELESS
│  ├─ JwtUtil                 토큰 생성/검증 (createToken, getEmail, getRole, validate)
│  └─ JwtAuthFilter           Bearer 토큰 → SecurityContext 등록
├─ controller
│  ├─ AuthController          POST /api/auth/signup, /api/auth/login
│  └─ MeController            GET /api/me
├─ dto
│  ├─ SignupRequest           email, password, name, phone
│  └─ LoginRequest            email, password
├─ entity
│  ├─ Role                    @Table("roles") ← role은 SQL 예약어
│  └─ AppUser                 @ManyToOne(LAZY) → Role
├─ repository
│  ├─ RoleRepository          findByCode
│  └─ AppUserRepository       findByEmail, existsByEmail
└─ service
└─ AuthService             signup, login


IZ-Get 인증 API 명세서


버전: v0 (연습본)
작성일: 2026-07-15
Base URL: http://localhost:8081
인증 방식: JWT Bearer Token




목차


공통 사항
POST /api/auth/signup — 회원가입
POST /api/auth/login — 로그인
GET /api/me — 내 정보 조회
JWT 사양
권한 정책
데이터 모델



1. 공통 사항

요청

항목값Content-Typeapplication/json인코딩UTF-8인증 헤더Authorization: Bearer {accessToken}

응답 상태 코드

코드의미200성공403인증 필요 / 토큰 무효405허용되지 않은 HTTP 메서드500서버 오류 (현재 모든 비즈니스 예외 포함)

인증이 필요 없는 경로

/api/auth/**
/h2-console/**

그 외 모든 경로는 유효한 JWT 필요.


2. POST /api/auth/signup — 회원가입

신규 사용자를 생성한다. 역할은 서버가 TRAINEE로 자동 부여한다.

Request

httpPOST /api/auth/signup
Content-Type: application/json

{
"email": "hong@example.com",
"password": "1234",
"name": "홍길동",
"phone": "010-1234-5678"
}

필드타입필수제약비고emailstring✅최대 255자, 유일소문자로 변환되어 저장passwordstring✅—BCrypt 해싱 후 저장namestring✅최대 100자phonestring❌최대 30자


클라이언트는 역할(role)을 지정할 수 없다. 권한 상승 방지.



Response — 200 OK

json{
"userId": "cd975d04-ea0b-427a-a314-2c471e9da2fb"
}

필드타입설명userIdstring (UUID)생성된 사용자 식별자

오류

상황현재 응답예외이메일 중복500IllegalArgumentException: 이미 사용 중인 이메일입니다TRAINEE 역할 미존재500IllegalStateException: 역할이 없습니다

처리 흐름

1. email.toLowerCase()
2. existsByEmail(email)        → 중복이면 예외
3. findByCode("TRAINEE")       → 역할 조회
4. passwordEncoder.encode()    → BCrypt 해싱
5. save()                      → user_id 반환

예시

powershellInvoke-RestMethod -Uri "http://localhost:8081/api/auth/signup" `
  -Method Post -ContentType "application/json" `
-Body '{"email":"hong@example.com","password":"1234","name":"Hong"}'


3. POST /api/auth/login — 로그인

이메일·비밀번호를 검증하고 JWT를 발급한다.

Request

httpPOST /api/auth/login
Content-Type: application/json

{
"email": "hong@example.com",
"password": "1234"
}

필드타입필수비고emailstring✅대소문자 무관 (서버에서 소문자 변환 후 조회)passwordstring✅

Response — 200 OK

json{
"accessToken": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJob25nQGV4YW1wbGUuY29tIiwicm9sZSI6IlRSQUlORUUiLCJpYXQiOjE3ODQwOTQ1MjIsImV4cCI6MTc4NDA5ODEyMn0.L7eqkSQuorFujgHKV-CfO4LW4LpK6gjiCwi5b0Qk08g"
}

필드타입설명accessTokenstringJWT. 유효기간 1시간


사용자 정보(name, role 등)는 응답에 포함되지 않는다. 토큰 payload를 디코딩하거나 GET /api/me를 호출해야 한다.



오류

상황현재 응답메시지존재하지 않는 이메일500이메일 또는 비밀번호가 올바르지 않습니다비밀번호 불일치500이메일 또는 비밀번호가 올바르지 않습니다


두 경우 메시지가 동일하다. 어떤 이메일이 가입되어 있는지 알아내는 계정 열거(account enumeration) 공격을 막기 위함.



처리 흐름

1. email.toLowerCase()
2. findByEmail(email)                          → 없으면 예외
3. passwordEncoder.matches(입력, 저장된 해시)   → 불일치면 예외
4. jwtUtil.createToken(email, role.code)


BCrypt는 복호화가 불가능하다. matches()는 입력값을 같은 방식으로 해싱해 비교한다.



예시

powershell$r = Invoke-RestMethod -Uri "http://localhost:8081/api/auth/login" `
  -Method Post -ContentType "application/json" `
-Body '{"email":"hong@example.com","password":"1234"}'
$token = $r.accessToken


4. GET /api/me — 내 정보 조회

토큰에 담긴 인증 주체 정보를 반환한다. 인증 동작 확인용.

Request

httpGET /api/me
Authorization: Bearer eyJhbGciOiJIUzM4NCJ9...

Response — 200 OK

json{
"email": "hong@example.com",
"authorities": [
{ "authority": "ROLE_TRAINEE" }
]
}

필드타입설명emailstring토큰의 sub 클레임authoritiesarrayROLE_ 접두사가 붙은 권한 목록

Response — 403 Forbidden

토큰이 없거나, 만료됐거나, 서명이 유효하지 않은 경우.


DB 조회가 발생하지 않는다. 모든 정보를 토큰에서 추출한다.



예시

powershellInvoke-RestMethod -Uri "http://localhost:8081/api/me" `
-Headers @{Authorization="Bearer $token"}


5. JWT 사양

항목값라이브러리jjwt 0.12.6알고리즘HS384 (secret 길이에 따라 자동 선택)유효기간3,600,000ms = 1시간전달 방식Authorization: Bearer {token}Refresh Token없음 (만료 시 재로그인)

Payload

json{
"sub": "hong@example.com",
"role": "TRAINEE",
"iat": 1784094522,
"exp": 1784098122
}

클레임설명sub사용자 이메일role역할 코드 (TRAINEE / MANAGER / SUPER_ADMIN)iat발급 시각 (Unix epoch, 초)exp만료 시각 (Unix epoch, 초)

보안 특성


JWT는 암호화가 아니라 서명이다. payload는 secret 없이 누구나 디코딩할 수 있다 (Base64URL)
따라서 비밀번호 등 민감정보를 넣지 않는다
위조는 secret 없이 불가능하다. payload를 변조하면 서명 검증에서 탈락한다



6. 권한 정책

필터 체인

JwtAuthFilter → UsernamePasswordAuthenticationFilter → ... → Controller

JwtAuthFilter 동작

1. Authorization 헤더 확인
2. "Bearer " 접두사 제거 (7글자)
3. 서명 검증 → 실패 시 인증 없이 통과 (뒤에서 403)
4. sub, role 추출
5. SecurityContext에 ROLE_{role} 권한으로 등록

Security 설정

항목값이유CSRFdisableAPI 서버, 세션 미사용frameOptionsdisableH2 콘솔이 iframe 사용SessionSTATELESS서버가 상태를 보관하지 않음

접근 제어

경로권한/api/auth/**permitAll/h2-console/**permitAll그 외authenticated


역할별 세분화(@PreAuthorize("hasRole('SUPER_ADMIN')"))는 미적용. 토큰에 role이 담겨 있어 적용 준비는 되어 있음.




7. 데이터 모델

roles

컬럼타입제약role_idSMALLINTPK, IDENTITYcodeVARCHAR(12)NOT NULL, UNIQUEnameVARCHAR(30)NOT NULL

초기 데이터 — 앱 시작 시 CommandLineRunner가 생성 (없을 때만)

role_idcodename1TRAINEE교육생2MANAGER매니저3SUPER_ADMIN슈퍼어드민


테이블명이 role이 아니라 roles인 이유: ROLE은 SQL 예약어.



app_user

컬럼타입제약user_idUUIDPK, 자동 생성emailVARCHAR(255)NOT NULL, UNIQUEpassword_hashVARCHAR(255)NOT NULLnameVARCHAR(100)NOT NULLphoneVARCHAR(30)NULLrole_idSMALLINTNOT NULL, FK → roles

관계

app_user (N) ──── (1) roles
@ManyToOne(fetch = LAZY)
@JoinColumn(name = "role_id")


LAZY 로딩이므로 user.getRole() 접근 시 @Transactional 범위 안이어야 한다.



ERD 대비 차이

항목비고org_id제외 (연습 범위 밖)email CITEXTH2에 미지원 → 애플리케이션에서 소문자 정규화role_id SMALLSERIALH2에서 IDENTITY 전략 + Java Shortrefresh_token 등제외


부록: 실행 환경

Spring Boot 4.1.0 / Spring Framework 7.0.8 / Spring Security 7.1.0
Hibernate ORM 7.4.1 / H2 2.4.240 (인메모리) / Java 17
포트: 8081
DB: jdbc:h2:mem:testdb (ddl-auto: create-drop → 재시작 시 초기화)
H2 콘솔: http://localhost:8081/h2-console (User: sa, 비밀번호 없음)