# Local Development Setup Guide

This guide provides instructions on how to set up the local development environment for the **Provincial Administrative Information Management and GIS Lookup System**.

The project consists of:

1. **Backend (BE):** Spring Boot (Java 17) web application.
2. **Frontend (FE):** React + TypeScript (Vite) single-page application.
3. **Database:** PostgreSQL with PostGIS spatial extension.

---

## 1. Prerequisites

Ensure you have the following installed on your machine:

- **Java Development Kit (JDK) 17** (e.g., Eclipse Temurin or OpenJDK 17)
- **Node.js** (LTS version recommended)
- **pnpm** (Package manager for Frontend; install via `npm i -g pnpm`)
- **PostgreSQL 15+** with the **PostGIS** extension enabled
- **Maven** (optional, wrapper `./mvnw` is included in the BE project)

---

## 2. Database Setup & Seeding

The system stores administrative boundaries and spatial attributes in PostgreSQL. Follow the steps below to initialize the database.

### Step 2.1: Create Database & Enable PostGIS

Log into your PostgreSQL instance (via pgAdmin, DBeaver, or psql console) and execute:

```sql
-- 1. Create a database named 'gialai'
CREATE DATABASE gialai;

-- 2. Connect to the 'gialai' database and enable the PostGIS extension
CREATE EXTENSION postgis;
```

### Step 2.2: Automatic Schema Migration & Seeding

The system uses **Flyway** to automatically migrate the database schema and seed the initial data upon backend application startup. You do **not** need to manually import any SQL files.

When you start the Spring Boot application (described in [Section 3](#3-backend-setup-spring-boot)), Flyway will automatically run the migration scripts found in [BE/src/main/resources/db/migration/core](../../BE/src/main/resources/db/migration/core):

1. **`V1__create_schema_admin_units.sql`**
   - _Description:_ Creates core administrative schema tables (`provinces`, `wards`).
2. **`V2__import_data_admin_units.sql`**
   - _Description:_ Seeds national administrative unit dictionary data.
3. **`V3__create_gis_tables.sql`**
   - _Description:_ Creates the spatial GIS tables (`gis_provinces`, `gis_wards`) with PostGIS geography types.
4. **`V4__import_gis_data_gialai.sql`**
   - _Description:_ Imports coordinates, boundary borders (`MULTIPOLYGON`), and GIS spatial points specifically for Gia Lai province (Administrative Code: **52**).

> **Note** : Default administrator and viewer accounts are defined to be automatically created by the `DatabaseSeeder` upon the initial startup of the backend (no separate SQL execution required).

---

## 3. Backend Setup (Spring Boot)

The Backend code is located in the [/BE](../../BE) directory.

### Step 3.1: Configure Connection Settings

Open [BE/src/main/resources/application.properties](../../BE/src/main/resources/application.properties) and update the database credentials to match your local PostgreSQL configuration:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gialai
spring.datasource.username=YOUR_POSTGRES_USER
spring.datasource.password=YOUR_POSTGRES_PASSWORD
```

### Step 3.2: Build and Run the Application

From your terminal, navigate to the `BE` folder and execute the Spring Boot run task:

```bash
cd BE
./mvnw spring-boot:run
```

_(On Windows cmd/powershell, run `mvnw.cmd spring-boot:run`)_

Alternatively, open the `BE` folder in your IDE (IntelliJ IDEA or VS Code) and run the main entrypoint: `com.website.gis.GisApplication`.

The server will launch and listen at **`http://localhost:8080`**. You can verify that it is active by checking the health endpoint: `http://localhost:8080/actuator/health`.

---

## 4. Frontend Setup (React & Vite)

The Frontend code is located in the [/FE](../../FE) directory.

### Step 4.1: Environment Configuration

Create a `.env` file in the root of the `FE` directory (if it does not exist) and configure the Backend API URL:

```env
VITE_API_BASE_URL=http://localhost:8080
```

### Step 4.2: Install Dependencies & Run

Open a terminal, navigate to the `FE` folder, install the packages, and start the development server:

```bash
cd FE
pnpm install
pnpm dev
```

The Vite dev server will start (usually on **`http://localhost:5173`** or similar). Open the provided URL in your web browser to access the GIS portal.

---

## 5. Development Credentials

Use the following seeded accounts to log in during local testing:

| Username     | Password | Role     | Access Level / Privileges                                 |
| :----------- | :------- | :------- | :-------------------------------------------------------- |
| **`admin`**  | `123456` | `ADMIN`  | Manage user accounts (Create, Edit, Delete), View GIS map |
| **`viewer`** | `123456` | `VIEWER` | View-only GIS map & look up commune boundaries            |

---

## 6. Project Architecture References

- [Project Overview & Requirement Specs](../../docs/en/PROJECT_OVERVIEW.md)
- [Coding Conventions & Standards](../../docs/en/CODING_CONVENTIONS.md)
