# Coding Conventions & Standards

This document establishes the official coding conventions, design patterns, and folder structures for the **Provincial Administrative Information Management and GIS Lookup System**.

All developers must adhere to these standards to ensure codebase consistency, maintainability, and clean code.

---

## 1. Naming Conventions

### 1.1. Java (Spring Boot Backend)

- **Packages:** All lowercase, singular, flat where possible within their module.
  - _Example:_ `com.website.gis.core.controller`, `com.website.gis.core.dto`, `com.website.gis.core.entity`, `com.website.gis.core.repository`, `com.website.gis.core.mapper`
  - _Rule:_ Package names are always lowercase, no exceptions — e.g. `com.website.gis.core.entity`, never `com.website.gis.core.Entity`.
  - _Rule:_ Administrative/core capabilities live under `com.website.gis.core.*` (Section 4.1 of `ARCHITECTURE SPECIFICATION.md`). Pluggable feature modules live under their own `com.website.gis.features.<module>.*` tree (Section 6 below) — never inside `core`.
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
package com.website.gis.core.exception;

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

Exceptions should convey specific HTTP semantics. Create a hierarchy under `com.website.gis.core.exception`:

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
package com.website.gis.core.exception;

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

- **Package Location:** Mappers for core administrative entities reside in `com.website.gis.core.mapper` (see `WardMapper.java` for the reference implementation). Mappers belonging to a pluggable feature module reside inside that module's own package, `com.website.gis.features.<module>.mapper` — **not** in a shared top-level location. This keeps each feature module self-contained, matching the "safely modified, omitted, or skipped" goal in `ARCHITECTURE SPECIFICATION.md` Section 4.1: deleting a feature module's package deletes its mapper along with it, with nothing left behind elsewhere.
- **Spring Integration:** Explicitly define the component model as Spring:
  ```java
  @Mapper(componentModel = "spring")
  ```
  _(This is also configured globally in `BE/pom.xml`, under the `maven-compiler-plugin` annotation processor args (`-Amapstruct.defaultComponentModel=spring`), as a default fallback.)_
- **Class Naming:** Use suffix `Mapper`.
  - _Example:_ `UserMapper.java`, `WardMapper.java`

### 3.2. Mapping Methods Pattern

Define clean interfaces for conversion. Avoid manual `.builder()` chains inside controllers or service classes:

```java
package com.website.gis.core.mapper;

import com.website.gis.core.entity.User;
import com.website.gis.core.dto.UserDto;
import com.website.gis.core.dto.UserCreateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", constant = "VIEWER")
    @Mapping(target = "password", ignore = true) // Handled by the caller using PasswordEncoder
    User toEntity(UserCreateRequest request);
}
```

**Injection:** this project currently has no dedicated `@Service` layer — controllers call repositories (and now mappers) directly. Inject mappers straight into the controller via constructor injection, the same way repositories already are:

```java
@RestController
@RequestMapping("/api/admin/users")
public class AdminController {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public AdminController(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
}
```

If a `@Service` layer is introduced later, mappers move with the logic they support — inject them into the service instead, following the same constructor-injection pattern.

### 3.3. What MapStruct Does *Not* Replace

Not every controller method is a pure mapping. Where a field's value depends on conditional business logic — e.g. `AdminController.updateUser()` only re-hashing `password` when the request actually supplies one, or `AuthController.getCurrentUser()` overriding `role` from the authenticated `Authentication`'s granted authorities — keep that logic explicit in the controller after calling the mapper, rather than forcing it into a `@Mapping` expression. MapStruct removes repetitive field-copying; it does not replace conditionals that only make sense to a human reading the endpoint's business rules.

---

## 4. React Query (TanStack Query) Key Conventions — Planned for Phase 2

> **Status:** not yet installed or used in `FE/`. No code in the repository calls `useQuery`/`useMutation` today. This section documents the convention to follow **once TanStack Query is adopted** (data-fetching for feature modules in Phase 2), so the pattern is agreed before the first hook is written — it is not a description of current frontend code.

To prevent bugs related to cache invalidation, typo errors, and inconsistent querying, the frontend will use a **Query Key Factory** pattern.

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
    ├── api/                # Core HTTP configurations
    │   └── axiosInstance.ts # Shared Axios instance with JWT interceptors
    │   # queryKeys.ts is planned here once TanStack Query is adopted (Section 4) — not present yet
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

When implementing a pluggable feature module (`ocop`, `science`, `agriculture`), the package/class naming must follow the same conventions above, applied consistently per module:

- Package: `com.website.gis.features.<module>` (lowercase, singular where natural — e.g. `features.ocop`, `features.science`, `features.agriculture`).
- Controller: `<Module>Controller` (e.g. `OcopController`, `ScienceController`, `AgricultureController`).
- Service: `<Module>Service`, Repository: `<Module>Repository`.
- Entity class name should match the domain noun, not the module name literally where they differ — e.g. module `ocop` → entity `OcopProduct` (per `DATA_MODEL.md` Section 4.1), module `science` → entity `ScienceUnit`, module `agriculture` → entity `AgricultureUnit`.
- DTOs follow the same `Request`/`Response`/`Dto` suffix rule as Section 1.1 (e.g. `OcopProductDto`, `ScienceUnitDto`, `AgricultureUnitDto`).
- Mappers live inside the module's own package, `com.website.gis.features.<module>.mapper` (per Section 3.1) — kept self-contained within the module, not in a shared top-level package, so the module stays independently removable.

## 7. Cross-References

- Compile-time toggling mechanics these conventions plug into: `ARCHITECTURE SPECIFICATION.md`.
- Table/column names these entities map to: `DATA_MODEL.md`.
- Endpoint shapes these controllers must expose: `API_CONTRACT.md`.
- Local environment setup to run and test code written under these conventions: `DEVELOPMENT_SETUP.md`.
