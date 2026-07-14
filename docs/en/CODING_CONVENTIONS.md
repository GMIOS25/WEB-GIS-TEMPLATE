# Coding Conventions & Standards

This document establishes the official coding conventions, design patterns, and folder structures for the **Provincial Administrative Information Management and GIS Lookup System**.

All developers must adhere to these standards to ensure codebase consistency, maintainability, and clean code.

---

## 1. Naming Conventions

### 1.1. Java (Spring Boot Backend)

- **Packages:** All lowercase, singular, flat where possible.
  - _Example:_ `com.website.gis.controller`, `com.website.gis.dto`, `com.website.gis.entity`, `com.website.gis.repository`
  - _Rule:_ Package names are always lowercase, no exceptions — e.g. `com.website.gis.entity`, never `com.website.gis.Entity`.
- **Classes & Interfaces:** `PascalCase`.
  - _Example:_ `GisWard`, `GisWardRepository`, `GlobalExceptionHandler`
- **Methods & Variables:** `camelCase`.
  - _Example:_ `getWards()`, `wardRepository`, `isLayerEnabled`
- **Constants:** `SCREAMING_SNAKE_CASE`.
  - _Example:_ `PROVINCE_CODE_GIA_LAI = 52`
- **Data Transfer Objects (DTOs):** Append purpose suffix (`Request`, `Response`, `Dto`).
  - _Example:_ `UserCreateRequest`, `LoginResponse`, `WardDetailDto`

### 1.2. TypeScript & React (Frontend)

- **Component Files:** `PascalCase` with `.tsx` extension.
  - _Example:_ `GisMap.tsx`, `SidebarDrawer.tsx`, `AdminPanel.tsx`
- **Utility / Service / Hook Files:** `camelCase` with `.ts` extension.
  - _Example:_ `axiosInstance.ts`, `useAuth.ts`
- **Variables & Functions:** `camelCase`.
  - _Example:_ `selectedWard`, `toggleLayer()`
- **Interfaces & Types:** `PascalCase`.
  - _Example:_ `GeoJsonData`, `GeoJsonFeature`
- **Constants:** `SCREAMING_SNAKE_CASE`.
  - _Example:_ `VITE_API_BASE_URL`

---

## 2. Backend Exception Handling Structure

To maintain clean controllers and return readable, consistent error payloads to the client, the project utilizes a centralized **Global Exception Handling** pattern via `@RestControllerAdvice`.

### 2.1. Standard Error Response Payload

All API errors return a consistent JSON structure using an `ErrorResponse` model:

```java
package com.website.gis.exception;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private Map<String, String> details; // For validation field errors
}
```

### 2.2. Custom Base Exceptions

Exceptions should convey specific HTTP semantics. Create a hierarchy under `com.website.gis.exception`:

- **`ResourceNotFoundException`** (Maps to `404 Not Found`):
  ```java
  public class ResourceNotFoundException extends RuntimeException {
      public ResourceNotFoundException(String message) {
          super(message);
      }
  }
  ```
- **`BadRequestException`** (Maps to `400 Bad Request`):
  ```java
  public class BadRequestException extends RuntimeException {
      public BadRequestException(String message) {
          super(message);
      }
  }
  ```

### 2.3. Global Exception Handler

Implement a centralized handler class to map exceptions to their respective HTTP status codes:

```java
package com.website.gis.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    // Handles @Valid DTO Validation Failures
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", request, details);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String message, HttpServletRequest request, Map<String, String> details) {
        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .details(details)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
```

---

## 3. MapStruct Mapping Conventions

MapStruct is used for automatic, compile-time mapping between Database Entities and Data Transfer Objects (DTOs).

### 3.1. General Rules

- **Package Location:** All mappers must reside in `com.website.gis.mapper`.
- **Spring Integration:** Explicitly define the component model as Spring:
  ```java
  @Mapper(componentModel = "spring")
  ```
  _(Note: This is also configured globally in [pom.xml](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/pom.xml#L169) as a default fallback)._
- **Class Naming:** Use suffix `Mapper`.
  - _Example:_ `UserMapper.java`, `WardMapper.java`

### 3.2. Mapping Methods Pattern

Define clean interfaces for conversion. Avoid manual loops inside service classes:

```java
package com.website.gis.mapper;

import com.website.gis.entity.User;
import com.website.gis.dto.UserDto;
import com.website.gis.dto.UserCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "VIEWER")
    @Mapping(target = "password", ignore = true) // Handled in Service class using Encoder
    User toEntity(UserCreateRequest request);
}
```

Inject mappers using constructor injection:

```java
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
}
```

---

## 4. React Query will be use in phase 2 (TanStack Query) Key Conventions

To prevent bugs related to cache invalidation, typo errors, and inconsistent querying, the frontend uses a **Query Key Factory** pattern.

### 4.1. Factory Strategy

Never use raw inline arrays (e.g., `['wards']`) inside components. Instead, co-locate a `queryKeys` constant object inside your API service file or inside a dedicated `src/api/queryKeys.ts` file:

```typescript
// src/api/queryKeys.ts

export const userKeys = {
  all: ["users"] as const,
  lists: () => [...userKeys.all, "list"] as const,
  list: (filters: string) => [...userKeys.lists(), { filters }] as const,
  details: () => [...userKeys.all, "detail"] as const,
  detail: (id: string | number) => [...userKeys.details(), id] as const,
};

export const wardKeys = {
  all: ["wards"] as const,
  lists: () => [...wardKeys.all, "list"] as const,
  geojson: () => [...wardKeys.all, "geojson"] as const,
  detail: (code: string) => [...wardKeys.all, "detail", code] as const,
};
```

### 4.2. Query Usage Example

Use key factories inside React Query hooks:

```typescript
import { useQuery } from "@tanstack/react-query";
import api from "./axiosInstance";
import { wardKeys } from "./queryKeys";

export const useWardGeoJson = () => {
  return useQuery({
    queryKey: wardKeys.geojson(),
    queryFn: async () => {
      const { data } = await api.get("/api/wards/geojson");
      return data;
    },
  });
};
```

### 4.3. Invalidation Usage Example

Invalidate queries predictably when mutations succeed:

```typescript
import { useMutation, useQueryClient } from "@tanstack/react-query";
import api from "./axiosInstance";
import { userKeys } from "./queryKeys";

export const useCreateUser = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (newUser: UserCreateRequest) =>
      api.post("/api/admin/users", newUser),
    onSuccess: () => {
      // Invalidates all user queries, forcing lists to refresh
      queryClient.invalidateQueries({ queryKey: userKeys.all });
    },
  });
};
```

---

## 5. Frontend Folder Structure

The React project is structured cleanly, prioritizing component co-location (grouping related parts next to pages that use them) rather than generic, globally nested folders.

```
FE/
├── public/                 # Static assets (favicons, manifest, etc.)
└── src/
    ├── api/                # Core HTTP configurations & TanStack query key configurations
    │   ├── axiosInstance.ts # Shared Axios instance with JWT interceptors
    │   └── queryKeys.ts     # Query key factories
    ├── assets/             # Shared static media, styles, and logo assets
    ├── context/            # App-wide React contexts
    │   └── AuthContext.tsx # Context for holding auth state, login, and logout functions
    ├── pages/              # Routed pages
    │   ├── Home.tsx        # Main GIS Map & Admin management wrapper page
    │   ├── Login.tsx       # Authentication Login screen
    │   └── home/           # Home-page specific resources and sub-components
    │       └── components/ # Component directory localized to the Home Page
    │           ├── AddUserModal.tsx
    │           ├── AdminPanel.tsx
    │           ├── DeleteUserModal.tsx
    │           ├── DetailsPanel.tsx
    │           ├── EditUserModal.tsx
    │           ├── GisMap.tsx
    │           ├── MapSearch.tsx
    │           ├── ProfileCard.tsx
    │           ├── SidebarDrawer.tsx
    │           └── StatsBoard.tsx
    ├── App.css             # Main stylesheet
    ├── App.tsx             # Entry routing and Context Provider configurations
    ├── index.css           # Global stylesheet containing Tailwind directives
    └── main.tsx            # Main application mounting entrypoint
```

### 5.1. Co-location Architecture Principle

- **Rule:** If a component is only used on a single page, place it in a sub-folder under `src/pages/[pageName]/components/` (like `src/pages/home/components/`).
- **Rule:** Only promote components to a global `src/components/` folder if they are used by **two or more** different pages (e.g., a shared `Button` or `Input`). This keeps the repository clean and prevents developer confusion during navigation.

---

## 6. Feature Module Naming (Backend `features/` packages)

When implementing a pluggable feature module (`ocop`, `science`, `nonglam`), the package/class naming must follow the same conventions above, applied consistently per module:

- Package: `com.website.gis.features.<module>` (lowercase, singular where natural — e.g. `features.ocop`, `features.science`, `features.nonglam`).
- Controller: `<Module>Controller` (e.g. `OcopController`, `ScienceController`, `NonglamController`).
- Service: `<Module>Service`, Repository: `<Module>Repository`.
- Entity class name should match the domain noun, not the module name literally where they differ — e.g. module `ocop` → entity `OcopProduct` (per `DATA_MODEL.md` Section 4.1), module `nonglam` → entity `NongLamZone`.
- DTOs follow the same `Request`/`Response`/`Dto` suffix rule as Section 1.1 (e.g. `OcopProductDto`, `NongLamZoneDto`).
- Mappers live in the shared `com.website.gis.mapper` package (per Section 3), not nested under `features/<module>/`, to keep mapper discovery consistent.

## 7. Cross-References

- Compile-time toggling mechanics these conventions plug into: `ARCHITECTURE SPECIFICATION.md`.
- Table/column names these entities map to: `DATA_MODEL.md`.
- Endpoint shapes these controllers must expose: `API_CONTRACT.md`.
- Local environment setup to run and test code written under these conventions: `DEVELOPMENT_SETUP.md`.
