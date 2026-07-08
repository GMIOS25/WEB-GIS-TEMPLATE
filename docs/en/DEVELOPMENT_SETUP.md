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

The system stores administrative boundaries and spatial attributes in PostgreSQL. Follow the steps below to initialize and seed the database manually.

### Step 2.1: Create Database & Enable PostGIS
Log into your PostgreSQL instance (via pgAdmin, DBeaver, or psql console) and execute:

```sql
-- 1. Create a database named 'gialai'
CREATE DATABASE gialai;

-- 2. Connect to the 'gialai' database and enable the PostGIS extension
CREATE EXTENSION postgis;
```

### Step 2.2: Import Database Schema & Seed Data
Navigate to the SQL data resources folder: [BE/src/main/resources/data](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/resources/data). Import the SQL files into the `gialai` database in the **exact order** specified below:

1. **`postgres_CreateSchema_CreateTables_vn_units.sql`**
   - *Description:* Creates core administrative schema tables (`provinces`, `districts`, `wards`).
2. **`postgres_ImportData_vn_units.sql`**
   - *Description:* Seeds national administrative unit dictionary data.
3. **`postgresql_CreateGISTables.sql`**
   - *Description:* Creates the spatial GIS tables (`gis_provinces`, `gis_districts`, `gis_wards`) with PostGIS geography types.
4. **`postgresql_ImportData_gis_2026-06-20__12_32_01.sql`**
   - *Description:* Imports coordinates, boundary borders (`MULTIPOLYGON`), and GIS spatial points specifically for Gia Lai province (Administrative Code: **52**).
5. **`DDL.sql`**
   - *Description:* Creates application authentication tables (`users`, `local_leaders`) and seeds default users.

---

## 3. Backend Setup (Spring Boot)

The Backend code is located in the [/BE](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE) directory.

### Step 3.1: Configure Connection Settings
Open [BE/src/main/resources/application.properties](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/resources/application.properties) and update the database credentials to match your local PostgreSQL configuration:

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
*(On Windows cmd/powershell, run `mvnw.cmd spring-boot:run`)*

Alternatively, open the `BE` folder in your IDE (IntelliJ IDEA or VS Code) and run the main entrypoint: `com.website.gis.GisApplication`.

The server will launch and listen at **`http://localhost:8080`**. You can verify that it is active by checking the health endpoint: `http://localhost:8080/actuator/health`.

---

## 4. Frontend Setup (React & Vite)

The Frontend code is located in the [/FE](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE) directory.

### Step 4.1: Environment Configuration
Create a `.env` file in the root of the `FE` directory (if it does not exist) and configure the Backend API URL:

```env
VITE_API_URL=http://localhost:8080
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

| Username | Password | Role | Access Level / Privileges |
| :--- | :--- | :--- | :--- |
| **`admin`** | `123456` | `ADMIN` | Manage user accounts (Create, Edit, Delete), View GIS map |
| **`viewer`** | `123456` | `VIEWER` | View-only GIS map & look up commune boundaries |

---

## 6. Project Architecture References
- [Project Overview & Requirement Specs](file:///d:/Work/WEB%20GIS%20TEMPLATE/docs/en/Project%20Overview.md)
- [Coding Conventions & Standards](file:///d:/Work/WEB%20GIS%20TEMPLATE/docs/en/CODING_CONVENTIONS.md)
