# Implementation Plan - TSK-1: Entity check & Security (JWT) Configuration

This plan implements TSK-1 (Khai báo Entity & Cấu hình Security (JWT)) in the backend Spring Boot project.
The target is to:
1. Verify the existing entities (`User`, `Ward`, `Province`, `GisWard`, `LocalLeader`) and ensure there are no compile or database mapping errors.
2. Add JWT library dependencies (`io.jsonwebtoken`) to `pom.xml`.
3. Create `UserRepository` for DB authentication.
4. Implement JWT Token Utility (`JwtTokenProvider`) and authentication filter (`JwtAuthenticationFilter`).
5. Configure Spring Security (`SecurityConfig`) to secure all APIs by default, enable CORS for frontend access, configure stateless session management, and permit public access to authentication endpoints.

## User Review Required

> [!IMPORTANT]
> - We will add dependencies for **JJWT (Java JWT)** library versions `0.12.6` to `pom.xml`.
> - Spring Security config will be state-of-the-art for Spring Boot 3.5.x using the new lambda style (`http -> http...`).
> - We will configure CORS to allow requests from Vite's default dev server (`http://localhost:5173`) and common ports.

## Open Questions

> [!IMPORTANT]
> 1. **CORS Allowed Origins**: Do you want to allow all origins (`*`) during Phase 1 development, or restrict to Vite's default port (`http://localhost:5173`)?
> 2. **Authentication URL**: The task specifies `POST /api/auth/login` as the login API in TSK-2. We will permit access to `/api/auth/**` without credentials. Does this match your plan?
> 3. **Secret Storage**: We will define default JWT properties in `application.properties` (e.g., `app.jwt.secret` and `app.jwt.expiration`). Should we use a standard 256-bit base64-encoded secret key as default?

## Proposed Changes

### [Backend Dependencies]

#### [MODIFY] [pom.xml](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/pom.xml)
- Add JJWT dependencies (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) to allow generating and parsing JWT tokens.

---

### [Database & Entity Mapping]

#### [MODIFY] [application.properties](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/resources/application.properties)
- Add JWT properties:
  - `app.jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437` (A secure 256-bit hex secret key)
  - `app.jwt.expiration-ms=86400000` (1 day in milliseconds)

---

### [Repository & Security Layer]

#### [NEW] [UserRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/UserRepository.java)
- A standard JPA repository for the `User` entity to lookup users by username.

#### [NEW] [CustomUserDetailsService.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/security/CustomUserDetailsService.java)
- Implement Spring Security's `UserDetailsService` to fetch user authentication information from the database and translate it into a `UserDetails` object.

#### [NEW] [JwtTokenProvider.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/security/JwtTokenProvider.java)
- Class responsible for generating, parsing, and validating JWT tokens using the configured secret key.

#### [NEW] [JwtAuthenticationFilter.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/security/JwtAuthenticationFilter.java)
- A filter that intercepts requests, extracts the JWT from the `Authorization: Bearer <token>` header, validates the token, and sets the authenticated user in the security context.

#### [NEW] [SecurityConfig.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/config/SecurityConfig.java)
- Security configuration file to:
  - Configure Spring Security filter chain.
  - Define `BCryptPasswordEncoder` bean.
  - Define `AuthenticationManager` bean.
  - Enable CORS with allowed origins.
  - Configure stateless session management.
  - Exclude `/api/auth/**`, `/v3/api-docs/**`, and `/swagger-ui/**` from authentication checks.

---

## Verification Plan

### Automated Tests
- Run `mvn clean test` or build check to verify everything compiles and context loads properly.

### Manual Verification
- Start the application using Maven: `mvn spring-boot:run` on port `8080`.
- Call a protected endpoint (e.g. `GET http://localhost:8080/api/admin/users`) via curl/Postman without a token.
- Verify that it returns `401 Unauthorized` (confirming that Spring Security is active and blocking unauthorized requests).
