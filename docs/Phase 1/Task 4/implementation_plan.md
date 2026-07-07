# Implementation Plan - Task 4: Bản đồ GIS tương tác & Giao diện Quản trị

This implementation plan details the steps to implement **Task 4 (Interactive GIS Map & User Administration)**. It spans both the Spring Boot backend (for optimized GeoJSON delivery) and the React frontend (for Leaflet map integration, search autocomplete, statistics dashboard, and Admin user CRUD dialogs).

---

## Decisions Made

The following key technical decisions have been made in alignment with project requirements:

1. **Population (Dân số) Data**:
   - The population requirement is completely removed. We will not use, calculate, or display population data on the map, sidebar, or statistics panels.
2. **GeoJSON Performance Optimization**:
   - Approved. We will add a new backend endpoint `GET /api/wards/geojson` which fetches all 135 ward boundaries of Gia Lai province in a single, highly optimized query from PostGIS.
3. **UI Library for Dialogs**:
   - Approved. We will code custom modal dialogs using standard Tailwind CSS and Lucide React. No external UI library (Shadcn UI or Radix) will be set up.
4. **Map Layers in Drawer**:
   - The "District" (Huyện) layer is removed. The Left Sidebar Drawer will only contain checkboxes for "Ranh giới cấp Tỉnh" (Province) and "Ranh giới cấp Xã" (Commune/Ward).

---

## Proposed Changes

### 🔴 Backend Changes (Spring Boot)

To support optimized data loading and ward statistics:

#### [MODIFY] [GisWardRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/GisWardRepository.java)
- Add a native query to fetch all ward codes, names, areas, and their corresponding GeoJSON boundary strings in a single query:
  ```java
  @Query(value = "SELECT gw.ward_code, w.name, w.full_name, gw.area_km2, ST_AsGeoJSON(gw.geom) " +
                 "FROM gis_wards gw JOIN wards w ON gw.ward_code = w.code", nativeQuery = true)
  List<Object[]> findAllWardsGeoJsonData();
  ```

#### [MODIFY] [WardController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/WardController.java)
- Add the `GET /api/wards/geojson` endpoint.
- Read from the database and assemble a standard GeoJSON `FeatureCollection` payload:
  ```json
  {
    "type": "FeatureCollection",
    "features": [
      {
        "type": "Feature",
        "geometry": { ... },
        "properties": {
          "code": "ward_code",
          "name": "name",
          "fullName": "fullName",
          "areaKm2": 12.34
        }
      }
    ]
  }
  ```

---

### 🔵 Frontend Changes (React)

#### [MODIFY] [package.json](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/package.json)
- Add frontend mapping dependencies:
  - `leaflet`
  - `react-leaflet`
  - `@types/leaflet` (devDependency)

#### [MODIFY] [main.tsx](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/src/main.tsx)
- Import standard Leaflet CSS globally to ensure map tiles align correctly:
  ```typescript
  import 'leaflet/dist/leaflet.css';
  ```

#### [MODIFY] [Home.tsx](file:///d:/Work/WEB%20GIS%20TEMPLATE/FE/src/pages/Home.tsx)
- **Drawer Controls**:
  - Remove "Ranh giới cấp Huyện" checkbox from the Left Sidebar drawer.
  - Keep toggles for "Ranh giới cấp Tỉnh" and "Ranh giới cấp Xã".
- **Map View Container Integration**:
  - Replace the placeholder with a responsive Leaflet Map using `<MapContainer>`, `<TileLayer>` (OpenStreetMap), and `<GeoJSON>`.
  - Center the map on Gia Lai coordinates `[13.8, 108.2]` with zoom level `9`.
  - Wire the Layer checkboxes to toggle visibility of the Ward boundaries and Province boundaries.
  - Implement hover effects on the ward boundaries (change border width, weight, and color to `#10b981`).
  - Implement a click listener on each ward feature: clicking a ward updates the state for the selected ward, opening the right Sidebar.
- **Search Autocomplete**:
  - Add a floating search bar on the top-left of the map.
  - Query `/api/wards` for autocomplete suggestions as the user types.
  - Selecting a ward zooms the map (`map.flyTo`) to the center of the ward's polygon and highlights it.
- **Right Sidebar Details Panel**:
  - Slide in a panel from the right when a ward is clicked/selected.
  - Display Ward Name, District/Province details, and Area (no population).
- **Summary Statistics Panel**:
  - Add a floating dashboard panel on the map showing:
    - Total number of Wards (135).
    - Total area (sum of all ward areas).
- **Admin User CRUD Management**:
  - Enhance the `/admin` view with functional forms/dialogs:
    - **Add User Modal**: Create a new viewer account (`POST /api/admin/users`).
    - **Edit/Reset Password Modal**: Change display name and password (`PUT /api/admin/users/{id}`).
    - **Delete Confirmation Dialog**: Delete viewer accounts with warning check (`DELETE /api/admin/users/{id}`).
    - Integrates standard Axios request handling with error toast alerts/feedbacks.

---

## Verification Plan

### Automated Checks
- Validate build using `pnpm run build` in the `FE/` directory.
- Verify backend compiles correctly using `./mvnw clean compile` in the `BE/` directory.

### Manual Verification
1. **Security & Role Restrictions**:
   - Login as `viewer`: verify the left sidebar drawer does NOT show the "Quản lý người dùng" button and the admin endpoint yields a 403 error.
   - Login as `admin`: verify access to the admin dashboard panel.
2. **User CRUD Operations (Admin)**:
   - Create a new Viewer account: verify the new user appears in the table.
   - Edit user profile/password: verify update success.
   - Delete a viewer account: verify deletion. Verify self-deletion is blocked.
3. **GIS Map & Interaction**:
   - Open Map view: verify OpenStreetMap tiles load smoothly.
   - Verify ward boundaries are rendered and highlight when hovered.
   - Click a ward: verify the right Sidebar opens showing details (area, name).
   - Search for a ward (e.g. "Pleiku" or any Gia Lai commune): select a result and verify the map flies to the coordinates and opens the details sidebar.
   - Verify the stats panel displays correct totals (135 wards and combined area).
