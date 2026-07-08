# API Contract Specification

This document defines the API endpoints, data models (DTOs), authentication headers, error formats, and pagination standards for the **Provincial Administrative Information Management and GIS Lookup System**.

---

## 1. Authentication Standard (JWT)

Secure API endpoints require JSON Web Token (JWT) authentication.

- **Header Name:** `Authorization`
- **Format:** `Bearer <JWT_TOKEN>`
- **Example:**
  ```http
  Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsImlhdCI6MT...
  ```
- **Error Behavior:** Missing, invalid, or expired tokens result in a `401 Unauthorized` response.

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
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "username": "admin",
    "fullName": "Quản trị viên Gia Lai",
    "role": "ADMIN"
  }
  ```

#### `POST /api/auth/logout`

- **Access:** Authenticated Users (`ADMIN`, `VIEWER`)
- **Behavior:** The JWT scheme in this system is **stateless** (no server-side session/token store). This endpoint is a convenience no-op for the client's flow — the client is responsible for discarding the token locally (e.g. clearing the auth context / storage).
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
- **Response:**
  - Status `204 No Content` (Success with no content payload) OR Status `200 OK` (plain success message).

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
  - Note: `leaders` is populated from the `local_leaders` table (joined on `ward_code`), not an inline column on `wards` — see `DATA_MODEL.md` Section 3.7. A ward may have zero or more leaders.
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

### 4.4. Convention for Future Feature Modules (`ocop`, `khcn`, `nonglam`)

These modules are not yet implemented (Core phase only covers auth + administrative units). When building them, follow the same shape established above rather than inventing a new one:

- **Base path:** `/api/{feature}` — e.g. `/api/ocop`, `/api/khcn`, `/api/nonglam`. Each is only registered when its `@ConditionalOnProperty` flag is enabled (`ARCHITECTURE SPECIFICATION.md` Section 4.2); when disabled, the path returns `404` because the controller bean simply doesn't exist.
- **List endpoint:** `GET /api/{feature}` — unlike `/api/wards` (unpaginated, since only 135 rows), feature module lists **should** use the pagination standard from Section 3, since the number of OCOP products/KHCN units/nông lâm zones can grow arbitrarily.
- **Detail endpoint:** `GET /api/{feature}/{id}`.
- **GeoJSON endpoint:** `GET /api/{feature}/geojson` — returns a `FeatureCollection`, same shape as `/api/wards/geojson`, for direct Leaflet layer consumption.
- **Write endpoints** (`POST`/`PUT`/`DELETE`) — restricted to `ADMIN` only, same role pattern as Section 4.2.
- **Geometry type differs by module:** `ocop`/`khcn` return `Point` geometries; `nonglam` returns `Polygon`/`MultiPolygon` — the frontend must render these via different Leaflet primitives (markers vs. `<GeoJSON>` polygon overlay), per `ARCHITECTURE SPECIFICATION.md` Section 6.4. Do not assume all feature modules are point-based.

See `DATA_MODEL.md` Section 4 for the corresponding table schema convention these endpoints will query.
