# Walkthrough - TSK-1: Entity check & Security (JWT) Configuration

We have successfully completed Task 1 of Phase 1! The security framework is now established, and JWT verification filters have been configured.

## Changes Made

### [Backend Dependencies]
- Added JJWT (Java JWT) dependencies to [pom.xml](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/pom.xml) to handle token generation and validation.

### [Application Settings]
- Appended configuration properties for JWT (secret and expiration time) to [application.properties](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/resources/application.properties).

### [Repositories]
- Created [UserRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/UserRepository.java) to enable loading users by username from the database.

### [Security Infrastructure]
- Created [CustomUserDetailsService.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/security/CustomUserDetailsService.java) to translate database users into Spring Security user details.
- Created [JwtTokenProvider.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/security/JwtTokenProvider.java) to generate and validate JWT tokens using HMAC-SHA256 signature algorithm.
- Created [JwtAuthenticationFilter.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/security/JwtAuthenticationFilter.java) to intercept incoming requests and load user context if a valid Bearer token is provided.
- Created [SecurityConfig.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/config/SecurityConfig.java) which defines standard stateless API security policy (permits auth & openapi endpoints, disables CSRF, configures CORS, and adds an `AuthenticationEntryPoint` returning `401 Unauthorized` for failed logins).

---

## Verification Results

### Automated Unit Tests
- Created [SecurityConfigWebMvcTest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/test/java/com/website/gis/config/SecurityConfigWebMvcTest.java) to verify:
  1. Accessing a protected endpoint without credentials correctly blocks the request and returns `401 Unauthorized` (instead of 403).
  2. Accessing a public endpoint under `/api/auth/**` successfully bypasses security.
- Run `mvn test` specifically targeting the test. It runs and passes successfully:
  ```
  [INFO] Running com.website.gis.config.SecurityConfigWebMvcTest
  [INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 11.39 s -- in com.website.gis.config.SecurityConfigWebMvcTest
  [INFO] BUILD SUCCESS
  ```
