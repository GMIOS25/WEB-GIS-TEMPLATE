# API Contract Specification

This document defines the API endpoints, data models (DTOs), authentication headers, error formats, and pagination standards for the **Provincial Administrative Information Management and GIS Lookup System**.

---

## 1. Authentication Standard (JWT)

Secure API endpoints require JSON Web Token (JWT) authentication.

- **Primary mechanism — HttpOnly cookie:** On successful login, the server sets the JWT via the `Set-Cookie` response header. The cookie is not readable by client-side JavaScript.
  - **Cookie name:** `gis_token` (default; configurable via `app.jwt.cookie-name`).
  - **Attributes:** `HttpOnly`, `Secure` (configurable via `app.jwt.cookie-secure`; should stay `true` in production, which requires HTTPS — may be set to `false` for local HTTP-only dev), `SameSite=Strict` (configurable via `app.jwt.cookie-same-site`).
  - The frontend must send requests with credentials included (e.g. Axios `withCredentials: true`) so the browser attaches the cookie automatically. This is what the web frontend uses — it does not read or store the token itself.
- **Fallback mechanism — `Authorization` header:** Still accepted by `JwtAuthenticationFilter` for clients that call the API directly and can't rely on cookies (Swagger UI, Postman, service-to-service calls). **Not used by the web frontend.**
  - **Format:** `Bearer <JWT_TOKEN>`
  - **Example:**
    ```http
    Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MT...
    ```
- **Error Behavior:** Missing, invalid, or expired tokens (via either mechanism) result in a `401 Unauthorized` response.

---

## 2. Error Response Standard

When an API request fails, the server returns a consistent error payload as specified in [CODING_CONVENTIONS.md](./CODING_CONVENTIONS.md#2-backend-exception-handling-structure):

```json
{
  "timestamp": "2026-07-08T14:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/admin/users",
  "details": {
    "username": "Username already exists"
  }
}
```

Common status codes:

- `400 Bad Request` — Input validation error, invalid request syntax.
- `401 Unauthorized` — Missing or invalid JWT token.
- `403 Forbidden` — User role does not have permission to access the endpoint.
- `404 Not Found` — Resource (e.g., Ward code) does not exist.
- `500 Internal Server Error` — Server issue.

---

## 3. Pagination Standard (Offset-based)

For list endpoints that support pagination, the server uses standard Spring Boot Data query variables and returns structured metadata.

### Request Query Parameters

- `page` (optional, integer): 0-indexed page index (default: `0`).
- `size` (optional, integer): Number of elements per page (default: `10`).
- `sort` (optional, string): Field name and direction (e.g. `name,asc`).

### Response Structure

```json
{
  "content": [
    // Array of DTO objects
  ],
  "pageable": {
    "sort": {
      "empty": false,
      "sorted": true,
      "unsorted": false
    },
    "offset": 0,
    "pageNumber": 0,
    "pageSize": 10,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 3,
  "totalElements": 25,
  "last": false,
  "size": 10,
  "number": 0,
  "sort": {
    "empty": false,
    "sorted": true,
    "unsorted": false
  },
  "numberOfElements": 10,
  "first": true,
  "empty": false
}
```

---

## 4. API Endpoint Roster

### 4.1. Authentication Module

#### `POST /api/auth/login`

- **Access:** Public
- **Request Body (`LoginRequest`):**
  ```json
  {
    "username": "admin",
    "password": "password123"
  }
  ```
- **Response Body (`LoginResponse`):**
  - Status `200 OK`
  - The JWT is **not** included in the response body — it is set via the `Set-Cookie` response header (see [§1. Authentication Standard](#1-authentication-standard-jwt)).
  ```json
  {
    "username": "admin",
    "fullName": "Quản trị viên Gia Lai",
    "role": "ADMIN"
  }
  ```

#### `GET /api/auth/me`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Behavior:** Returns the currently authenticated user, resolved from the JWT cookie (or `Authorization: Bearer` header, see §1) attached to the request. Since the JWT cookie is `HttpOnly` and unreadable by JS, the frontend calls this endpoint on app startup (`AuthContext`) to restore the session after a page reload, instead of reading a stored token.
- **Response Body (`LoginResponse`):**
  - Status `200 OK`
  ```json
  {
    "username": "admin",
    "fullName": "Quản trị viên Gia Lai",
    "role": "ADMIN"
  }
  ```
- **Error Behavior:** `401 Unauthorized` if no valid cookie/token is present.

#### `POST /api/auth/logout`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Behavior:** Clears the JWT cookie by re-issuing it with the same name/path/attributes and `Max-Age=0`. The JWT scheme itself remains **stateless** (no server-side session/token store), so there is nothing to invalidate server-side beyond expiring the cookie.
- **Response:** Status `200 OK`, empty body.
- **Note for implementers:** If a real token-invalidation requirement emerges later (e.g. "force logout a compromised account"), this will require introducing a token blocklist (e.g. a short-lived Redis set of invalidated JTIs) — that is a deliberate architectural addition, not something to improvise ad hoc inside this endpoint.

---

### 4.2. User Management Module (Admin Only)

#### `GET /api/admin/users`

- **Access:** Roles: `ADMIN`
- **Response Body (`List<UserDto>`):**
  - Status `200 OK`
  ```json
  [
    {
      "id": 1,
      "username": "admin",
      "fullName": "Quản trị viên Gia Lai",
      "role": "ADMIN"
    },
    {
      "id": 2,
      "username": "viewer",
      "fullName": "Người xem bản đồ",
      "role": "VIEWER"
    }
  ]
  ```

#### `POST /api/admin/users`

- **Access:** Roles: `ADMIN`
- **Request Body (`UserCreateRequest`):**
  ```json
  {
    "username": "new_viewer",
    "password": "securepassword123",
    "fullName": "Nguyễn Văn A"
  }
  ```
- **Response Body (`UserDto`):**
  - Status `201 Created`
  ```json
  {
    "id": 3,
    "username": "new_viewer",
    "fullName": "Nguyễn Văn A",
    "role": "VIEWER"
  }
  ```

#### `PUT /api/admin/users/{id}`

- **Access:** Roles: `ADMIN`
- **Behavior:** Updates a user's full name and optionally resets their password. Role is immutable via this endpoint to prevent unauthorized privilege escalation.
- **Request Body (`UserUpdateRequest`):**
  ```json
  {
    "fullName": "Nguyễn Văn B",
    "password": "optional_new_password"
  }
  ```
- **Response Body (`UserDto`):**
  - Status `200 OK`
  ```json
  {
    "id": 3,
    "username": "new_viewer",
    "fullName": "Nguyễn Văn B",
    "role": "VIEWER"
  }
  ```

#### `DELETE /api/admin/users/{id}`

- **Access:** Roles: `ADMIN`
- **Behavior & Guardrails:**
  - Deletes the specified user account.
  - **Self-deletion prevention:** Returns `400 Bad Request` ("You cannot delete your own account") if the authenticated admin attempts to delete themselves.
  - **Last admin protection:** Returns `400 Bad Request` ("Cannot delete the last remaining ADMIN account") if attempting to delete the only remaining `ADMIN` in the system.
- **Response:**
  - Status `200 OK` (e.g. `"User deleted successfully"`).


---

### 4.3. Administrative Unit & GIS Module

#### `GET /api/wards`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Query Parameters:**
  - `q` (optional, string): Name query filter (e.g. `Pleiku`).
- **Response Body (`List<WardDto>`):**
  - Status `200 OK`
  ```json
  [
    {
      "code": "21112",
      "name": "Ia Kring",
      "fullName": "Phường Ia Kring",
      "provinceName": "Tỉnh Gia Lai"
    }
  ]
  ```

#### `GET /api/wards/{code}`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response Body (`WardDetailDto`):**
  - Status `200 OK`
  - Note: `leaders` is populated from the `local_leaders` table (joined on `ward_code`), not an inline column on `wards` — see `DATA_MODEL.md` Section 3.7. A ward may have zero or more leaders (returns an empty array `[]` when no leaders exist).
  ```json
  {
    "code": "21112",
    "name": "Ia Kring",
    "fullName": "Phường Ia Kring",
    "provinceName": "Tỉnh Gia Lai",
    "areaKm2": 6.84,
    "leaders": [
      {
        "fullName": "Nguyễn Văn A",
        "position": "Chủ tịch UBND",
        "phoneNumber": "0905xxxxxx"
      }
    ]
  }
  ```

#### `GET /api/wards/{code}/geojson`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response Body:**
  - Status `200 OK`
  - Content-Type: `application/json`
  - Description: Returns a raw GeoJSON Feature coordinates geometry object representing the boundary of the ward.
  ```json
  {
    "type": "Feature",
    "geometry": {
      "type": "MultiPolygon",
      "coordinates": [[[[107.9812, 13.9723], [107.9854, 13.9745], ...]]]
    },
    "properties": {
      "code": "21112",
      "name": "Ia Kring",
      "fullName": "Phường Ia Kring",
      "areaKm2": 6.84
    }
  }
  ```

#### `GET /api/wards/geojson`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response Body:**
  - Status `200 OK`
  - Content-Type: `application/json`
  - Description: Returns a GeoJSON `FeatureCollection` containing all ward boundaries of Gia Lai Province.
  ```json
  {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "geometry": {
          "type": "MultiPolygon",
          "coordinates": [...]
        },
        "properties": {
          "code": "21112",
          "name": "Ia Kring",
          "fullName": "Phường Ia Kring",
          "areaKm2": 6.84
        }
      }
    ]
  }
  ```

#### `GET /api/wards/province/geojson`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response Body:**
  - Status `200 OK`
  - Content-Type: `application/json`
  - Description: Returns the boundary polygon of Gia Lai province (Province Code: **52**).
  ```json
  {
    "type": "Feature",
    "geometry": {
      "type": "Polygon",
      "coordinates": [...]
    },
    "properties": {
      "code": "52",
      "name": "Gia Lai",
      "fullName": "Tỉnh Gia Lai"
    }
  }
  ```

---

---

### 4.4. Shared Media & File Management (`/api/files`)

#### `POST /api/files`
- **Access:** `ADMIN` only
- **Content-Type:** `multipart/form-data`
- **Request Parameters:**
  - `file` (MultipartFile, required): Allowed formats JPEG, PNG (max 5MB, auto-resized to max 1600px width), PDF, DOCX (max 20MB). Magic bytes validated.
  - `folder` (string, optional): Sub-directory name (e.g. `ocop`, `science`, `agriculture`).
- **Response Body (`StoredFile`):**
  - Status `201 Created`
  ```json
  {
    "storedFileName": "ocop/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d.png",
    "originalFileName": "sample.png",
    "contentType": "image/png",
    "sizeBytes": 124560,
    "publicUrl": "/api/files/ocop/9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d.png"
  }
  ```

#### `GET /api/files/{fileName}`
- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response:** Raw file binary stream with appropriate `Content-Type`, `Content-Disposition: inline`, and `Cache-Control: private, max-age=604800`.

---

### 4.5. Feature Modules (`ocop`, `science`, `agriculture`)

These modules are implemented as independent pluggable feature extensions. Each controller is conditional upon `features.<module>.enabled=true` (returning `404 Not Found` when disabled).

#### `GET /api/{ocop|science|agriculture}`
- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Query Parameters:**
  - **Shared parameters:**
    - `page` (optional, integer): 0-indexed page index (default: `0`).
    - `size` (optional, integer): Number of elements per page (default: `10` for OCOP and Agriculture; `20` for Science).
    - `sort` (optional, string): Field name and direction (default: `id,desc` for OCOP and Agriculture; `id,asc` for Science).
    - `wardCode` (optional, string): Administrative commune/ward code filter (e.g. `21112`).
  - **Module-specific parameters:**
    - `q` (optional, string) — **OCOP only (`/api/ocop`)**: Search keyword for product name (case-insensitive substring match via `findByNameContainingIgnoreCase` or `findByWardCodeAndNameContainingIgnoreCase`).
    - > [!NOTE]
    - > `GET /api/science` and `GET /api/agriculture` currently do **not** support the `q` search parameter (they only filter by `wardCode`).
- **Response Body:** Paginated list of DTOs (Section 3).

#### `GET /api/{ocop|science|agriculture}/geojson`
- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response:** Status `200 OK`, `application/json`, `Cache-Control: private, max-age=3600`.
- **Response Body:** GeoJSON `FeatureCollection` with Point features `[longitude, latitude]` and minimal properties:
  ```json
  {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "geometry": {
          "type": "Point",
          "coordinates": [108.015, 13.985]
        },
        "properties": {
          "id": 1,
          "name": "Cà phê Robusta Pleiku",
          "productTypes": ["Đồ uống", "Nông sản"],
          "starRating": 4,
          "contactPhone": "0905123456",
          "locationAddress": "123 Đường Hùng Vương, TP Pleiku",
          "wardCode": "21112",
          "imageUrl": "/api/files/ocop/sample.jpg"
        }
      }
    ]
  }
  ```

#### `GET /api/{ocop|science|agriculture}/nearby`
- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Query Parameters:**
  - `lat` (required, double): Latitude in range `[-90.0, 90.0]`.
  - `lng` (required, double): Longitude in range `[-180.0, 180.0]`.
  - `radiusKm` (required, double): Search radius in kilometers (`radiusKm > 0`).
- **Validation:** Returns `400 Bad Request` if coordinates or radius are invalid/missing.
- **Response Body:** List of DTOs within the search radius:
  ```json
  [
    {
      "id": 1,
      "name": "Cà phê Robusta Pleiku",
      "productTypes": ["Đồ uống", "Nông sản"],
      "starRating": 4,
      "contactPhone": "0905123456",
      "locationAddress": "123 Đường Hùng Vương, TP Pleiku",
      "wardCode": "21112",
      "wardName": "Phường Ia Kring",
      "latitude": 13.985,
      "longitude": 108.015,
      "imageUrl": "/api/files/ocop/sample.jpg"
    }
  ]
  ```

#### `GET /api/{ocop|science|agriculture}/{id}`
- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Response Body:** Single item DTO (404 if not found).

#### `POST /api/{ocop|science|agriculture}`
- **Access:** `ADMIN` only
- **Request Body:** 
  - **For `ocop` (`OcopProductCreateRequest`):**
    - `name` (required, string, max 255)
    - `productTypes` (optional, array of strings)
    - `starRating` (optional, integer, min 1, max 5)
    - `contactPhone` (optional, string, max 13)
    - `locationAddress` (optional, string)
    - `wardCode` (required, string, max 20)
    - `latitude` (required, BigDecimal, -90.0 to 90.0)
    - `longitude` (required, BigDecimal, -180.0 to 180.0)
    - `imageUrl` (optional, string, max 500)
    ```json
    {
      "name": "Cà phê Robusta Pleiku",
      "productTypes": ["Đồ uống", "Nông sản"],
      "starRating": 4,
      "contactPhone": "0905123456",
      "locationAddress": "123 Đường Hùng Vương, TP Pleiku",
      "wardCode": "21112",
      "latitude": 13.985,
      "longitude": 108.015,
      "imageUrl": "/api/files/ocop/sample.jpg"
    }
    ```
  - **For `science` (`ScienceUnitCreateRequest`) & `agriculture` (`AgricultureUnitCreateRequest`):**
    - `name` (required, string, max 255)
    - `unitType` (optional, string, max 100)
    - `description` (optional, string)
    - `wardCode` (required, string, max 20)
    - `latitude` (required, BigDecimal, -90.0 to 90.0)
    - `longitude` (required, BigDecimal, -180.0 to 180.0)
    - `imageUrl` (optional, string, max 500)
    ```json
    {
      "name": "Trung tâm Ứng dụng Tiến bộ KH&CN",
      "unitType": "Trung tâm nghiên cứu",
      "description": "Nghiên cứu ứng dụng công nghệ sinh học",
      "wardCode": "21112",
      "latitude": 13.985,
      "longitude": 108.015,
      "imageUrl": "/api/files/science/sample.jpg"
    }
    ```
- **Response Body:** Created item DTO with status `201 Created`.

#### `PUT /api/{ocop|science|agriculture}/{id}`
- **Access:** `ADMIN` only

> [!WARNING]
> **Important Semantic Difference Between Modules:**
> - **OCOP (`PUT /api/ocop/{id}`) — Full Replacement Semantics (Strict PUT):**
>   - Required fields: `name` (`@NotBlank`), `wardCode` (`@NotBlank`), `latitude` (`@NotNull`), `longitude` (`@NotNull`).
>   - Optional fields: `productTypes`, `starRating` (1..5), `contactPhone`, `locationAddress`, `imageUrl`.
>   - **Overwrites all fields on the entity.** If an optional field is omitted or passed as `null` in the request body, it **will be cleared / set to `null`** in the database.
> - **Science & Agriculture (`PUT /api/science/{id}`, `PUT /api/agriculture/{id}`) — Partial Update Semantics (PATCH-like behavior):**
>   - All fields in `ScienceUnitUpdateRequest` / `AgricultureUnitUpdateRequest` are optional (`name`, `unitType`, `description`, `wardCode`, `latitude`, `longitude`, `imageUrl`).
>   - **Only updates non-null / non-empty fields.** Any field omitted or passed as `null` in the request body **retains its existing value** in the database.
> - **Client Integration Risk:** When calling `PUT /api/ocop/{id}`, frontend/API clients must supply the complete object state to avoid unintentionally erasing optional fields. Conversely, calls to `PUT /api/science/{id}` or `PUT /api/agriculture/{id}` can safely send only modified fields.

- **Request Body:**
  - **For `ocop` (`OcopProductUpdateRequest` — Full Replacement):**
    ```json
    {
      "name": "Cà phê Robusta Pleiku",
      "productTypes": ["Đồ uống", "Nông sản"],
      "starRating": 4,
      "contactPhone": "0905123456",
      "locationAddress": "123 Đường Hùng Vương, TP Pleiku",
      "wardCode": "21112",
      "latitude": 13.985,
      "longitude": 108.015,
      "imageUrl": "/api/files/ocop/sample.jpg"
    }
    ```
  - **For `science` (`ScienceUnitUpdateRequest`) & `agriculture` (`AgricultureUnitUpdateRequest`) — Partial Update:**
    ```json
    {
      "name": "Trung tâm Ứng dụng Tiến bộ KH&CN (Cập nhật)",
      "unitType": "Trung tâm nghiên cứu",
      "description": "Nghiên cứu ứng dụng công nghệ sinh học nông nghiệp",
      "wardCode": "21112",
      "latitude": 13.985,
      "longitude": 108.015,
      "imageUrl": "/api/files/science/sample.jpg"
    }
    ```
- **Response Body:** Updated item DTO with status `200 OK`.

#### `DELETE /api/{ocop|science|agriculture}/{id}`
- **Access:** `ADMIN` only
- **Response Body:** Status `200 OK`, `{ "message": "... deleted successfully" }`.


