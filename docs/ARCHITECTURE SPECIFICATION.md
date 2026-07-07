# SYSTEM ARCHITECTURE & MODULARITY SPECIFICATION

This document outlines the system architecture design for the Provincial Administrative Information Management and GIS Lookup System, detailing how compile-time modularity (Feature Toggling) is implemented across the Frontend, Backend, and Database tiers.

---

## 1. Architectural Design Overview

The system follows a standard three-tier architecture split into:
1. **Presentation Layer (Frontend):** React (Vite) + Leaflet (Map rendering) + Tailwind CSS (Styling).
2. **Application Layer (Backend):** Spring Boot (Java 17) + Spring Security (JWT auth) + Hibernate Spatial.
3. **Database Layer (Storage):** PostgreSQL with PostGIS extensions + local file storage.

### 1.1. System Component Block Diagram

```mermaid
graph TD
    subgraph Client [Client Browser]
        Vite[Vite Build Engine]
        React[React Router / Sidebar Menu]
        Map[React Leaflet / GeoJSON Layers]
    end

    subgraph Server [Application Server - Spring Boot]
        CoreAPI[Administrative Core APIs]
        FeatureRegistry[Conditional Feature Beans]
        Security[Spring Security / JWT]
    end

    subgraph Database [Database Server - PostgreSQL]
        PostGIS[PostGIS Extension]
        CoreSchema[Administrative Core Tables]
        SchoolSchema[Optional: School Tables]
        HospitalSchema[Optional: Hospital Tables]
    end

    React -->|REST Calls| CoreAPI
    React -->|REST Calls| FeatureRegistry
    CoreAPI -->|SQL Spatial Queries| CoreSchema
    FeatureRegistry -->|SQL Queries| SchoolSchema
    FeatureRegistry -->|SQL Queries| HospitalSchema
```

---

## 2. Compile-Time Modularity (Feature Toggling)

To deliver bespoke packages for different clients (e.g., Client A only needs schools, Client B only needs hospitals) without maintaining separate codebases, the system utilizes a **Compile-time Modularity** pattern. Feature flags are set during the build stage, prompting compilers and dependency injection containers to exclude or ignore deactivated features.

```mermaid
sequenceDiagram
    participant Config as Build Environment Config
    participant FE as Vite Compiler (Frontend)
    participant BE as Spring Boot Compiler & DI (Backend)
    participant DB as Flyway Schema Migrator (Database)

    Config->>FE: Inject .env Variables (e.g., VITE_ENABLE_SCHOOLS=true)
    Config->>BE: Set active profiles or configurations (e.g., features.school.enabled=true)
    Config->>DB: Scan locations depending on active feature profiles

    Note over FE: Treeshakes/disables school routes & menus
    Note over BE: Only initializes School Controllers/Services/Repositories
    Note over DB: Only executes core + school migrations
```

---

## 3. Frontend Modularity Implementation (React + Vite)

Modularity in the frontend is controlled by environment variables injected at build time.

### 3.1. Environment Configuration (`.env`)
Each client deployment will have its own `.env` file containing feature switches:
```env
# Core Administrative Configurations
VITE_API_URL=http://localhost:8080/api
VITE_PROVINCE_CODE=52

# Feature Modularity Toggles
VITE_ENABLE_SCHOOLS=true
VITE_ENABLE_HOSPITALS=false
VITE_ENABLE_POLICE=false
VITE_ENABLE_OCOP=true
```

### 3.2. Dynamic Routing & Menu Filtering
The sidebar menu and router read environment variables to register paths:

```typescript
// src/config/features.ts
export const FEATURE_FLAGS = {
  schools: import.meta.env.VITE_ENABLE_SCHOOLS === 'true',
  hospitals: import.meta.env.VITE_ENABLE_HOSPITALS === 'true',
  police: import.meta.env.VITE_ENABLE_POLICE === 'true',
  ocop: import.meta.env.VITE_ENABLE_OCOP === 'true',
};

// src/router/index.tsx
import { RouteObject } from 'react-router-dom';
import { FEATURE_FLAGS } from '../config/features';

const baseRoutes: RouteObject[] = [
  { path: '/', element: <Dashboard /> },
  { path: '/admin-map', element: <AdministrativeMap /> },
];

const featureRoutes: RouteObject[] = [];

if (FEATURE_FLAGS.schools) {
  featureRoutes.push({
    path: '/schools',
    lazy: () => import('../pages/schools/SchoolManagement'), // Lazy loaded for code splitting
  });
}
if (FEATURE_FLAGS.hospitals) {
  featureRoutes.push({
    path: '/hospitals',
    lazy: () => import('../pages/hospitals/HospitalManagement'),
  });
}

export const routes = [...baseRoutes, ...featureRoutes];
```

### 3.3. Map Layer Control (Leaflet)
On the interactive GIS map, overlays are conditionally loaded:

```typescript
// src/components/map/GisMap.tsx
import React from 'react';
import { LayersControl } from 'react-leaflet';
import { FEATURE_FLAGS } from '../../config/features';
import { SchoolMarkers } from './SchoolMarkers';
import { HospitalMarkers } from './HospitalMarkers';

export const GisMap: React.FC = () => {
  return (
    <LayersControl position="topright">
      {FEATURE_FLAGS.schools && (
        <LayersControl.Overlay name="Schools">
          <SchoolMarkers />
        </LayersControl.Overlay>
      )}
      {FEATURE_FLAGS.hospitals && (
        <LayersControl.Overlay name="Hospitals">
          <HospitalMarkers />
        </LayersControl.Overlay>
      )}
    </LayersControl>
  );
};
```

---

## 4. Backend Modularity Implementation (Spring Boot)

At the Backend, feature toggles are driven by Spring Application configuration property keys and Spring Profiles, controlling the dependency injection (DI) lifecycle.

### 4.1. Package Structure
Core administrative capabilities are separated from feature packages. This structure allows feature directories to be safely modified, omitted, or skipped.

```
BE/src/main/java/com/website/gis/
├── core/                         # Core administrative packages
│   ├── controller/               # Administrative Unit Controllers
│   ├── entity/                   # Administrative Unit & User Entities
│   ├── repository/               # Basic JpaRepositories
│   └── security/                 # Spring Security & JWT components
└── features/                     # Pluggable features/modules
    ├── school/
    │   ├── SchoolController.java
    │   ├── SchoolService.java
    │   └── SchoolRepository.java
    └── hospital/
        ├── HospitalController.java
        ├── HospitalService.java
        └── HospitalRepository.java
```

### 4.2. Conditional Spring Bean Initialization
Controllers, services, and repositories for optional features use Spring Boot's `@ConditionalOnProperty` annotation. If disabled, Spring will not create these beans, meaning their REST endpoints are never registered:

```java
package com.website.gis.features.school;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
@ConditionalOnProperty(name = "features.school.enabled", havingValue = "true")
public class SchoolController {
    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }
    
    // Endpoints mapped here return 404 (Not Found) if disabled,
    // as Spring Boot does not load the controller bean at startup.
}
```

### 4.3. Application Settings Configuration (`application.yml`)
The main backend settings config:
```yaml
features:
  school:
    enabled: ${ENABLE_SCHOOLS:true}
  hospital:
    enabled: ${ENABLE_HOSPITALS:false}
  police:
    enabled: ${ENABLE_POLICE:false}
  ocop:
    enabled: ${ENABLE_OCOP:true}
```

---

## 5. Database Schema Modularity Strategy (Flyway)

To ensure client databases do not have ghost tables for features they did not request (e.g. creating the `schools` table for a client that only wants `hospitals`), Flyway migrations are partitioned by folder directories.

### 5.1. Flyway Directory Structure
```
BE/src/main/resources/db/migration/
├── core/
│   ├── V1__init_auth_schema.sql         # Base user authentication schema
│   └── V2__init_admin_units_schema.sql  # Administrative boundaries
├── school/
│   └── V3_1__create_school_table.sql    # Specific schema for schools
└── hospital/
    └── V3_2__create_hospital_table.sql  # Specific schema for hospitals
```

### 5.2. Dynamic Flyway Scan Locations Configuration
To merge active folders at run time based on active configurations, a configuration bean dynamically customizes the Flyway locations path list:

```java
package com.website.gis.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class DynamicFlywayConfig {

    @Value("${features.school.enabled:false}")
    private boolean schoolEnabled;

    @Value("${features.hospital.enabled:false}")
    private boolean hospitalEnabled;

    @Value("${features.police.enabled:false}")
    private boolean policeEnabled;

    @Value("${features.ocop.enabled:false}")
    private boolean ocopEnabled;

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> {
            List<String> locations = new ArrayList<>();
            // Core migrations must always execute
            locations.add("classpath:db/migration/core");

            // Conditionally append modular migrations based on active feature config
            if (schoolEnabled) {
                locations.add("classpath:db/migration/school");
            }
            if (hospitalEnabled) {
                locations.add("classpath:db/migration/hospital");
            }
            if (policeEnabled) {
                locations.add("classpath:db/migration/police");
            }
            if (ocopEnabled) {
                locations.add("classpath:db/migration/ocop");
            }

            configuration.locations(locations.toArray(new String[0]));
        };
    }
}
```

This ensures that only database tables matching the active modules are initialized in the target customer database. It avoids schema clutter, maintains table integrity, and keeps database sizes and structures exactly aligned with client purchase orders.
