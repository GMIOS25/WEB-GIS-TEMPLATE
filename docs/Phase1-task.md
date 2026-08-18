# KẾ HOẠCH TỐI GIẢN (CẬP NHẬT) - GIAI ĐOẠN 1: NỀN TẢNG HÀNH CHÍNH

## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!IMPORTANT]
> **Trạng thái tài liệu (CẬP NHẬT 2026-08-17): Giai đoạn 1 đã HOÀN THÀNH & VERIFY THỰC NGHIỆM 100% (5/5 Tasks: TSK-1 → TSK-5).**
>
> - **`./mvnw -B verify`**: 33/33 tests PASS (0 failures, 0 errors) với Docker Testcontainers PostGIS thật.
> - **`docker compose up -d --build` (TSK-5)**: Đã build multi-stage thành công từ đầu đến cuối trên môi trường sạch, khởi động trọn vẹn stack (`app`, `db`, `caddy`), Flyway tự động migrate V1→V4 (135 xã/phường), tài khoản mẫu `admin`/`viewer` được seed thành công, `/actuator/health` trả về `UP` và web app tải đầy đủ.
> - **Tối ưu & Khắc phục đi kèm**: Đã tạo `.dockerignore` (giảm build context từ 173MB/375s xuống 20KB/1s), cập nhật `SecurityConfig.java` mở quyền truy cập tài nguyên tĩnh frontend, và cấu hình `JWT_SECRET` chuẩn 64-byte.
>
> File này hiện là **hồ sơ nghiệm thu lịch sử hoàn tất cho Giai đoạn 1**. Mọi thông tin cập nhật nhất về kiến trúc và thiết kế hệ thống xem tại thư mục `docs/en/`:
>
> - Schema DB thật + quy ước bảng: `DATA_MODEL.md`
> - Kiến trúc modular, package `core/` vs `features/`: `ARCHITECTURE SPECIFICATION.md`
> - Hướng dẫn setup môi trường: `DEVELOPMENT_SETUP.md`
> - Vận hành, triển khai Docker/Caddy, backup, fleet: `DEPLOYMENT & FLEET STRATEGY.md`
> - Đặc tả toàn bộ REST API: `API_CONTRACT.md`

> [!TIP]
> **Triết lý tối giản tối đa:**
> Hệ thống chỉ có hai vai trò: **ADMIN** (Quản trị hệ thống, quản lý tài khoản) và **VIEWER** (Xem và tra cứu bản đồ). Mọi tính năng chỉnh sửa dữ liệu hay thay đổi địa giới hành chính đều được lược bỏ (bạn sẽ sửa trực tiếp trong PostgreSQL khi cần thiết). Không dùng Nginx (dùng Caddy — xem TSK-5 bên dưới) hay các cấu hình phức tạp khác.

---

## 1. Vai trò người dùng (Roles Matrix)

- **ADMIN**:
  - Đăng nhập hệ thống.
  - Quản lý tài khoản người dùng (Xem danh sách, Tạo mới tài khoản `VIEWER`, Sửa thông tin/đổi mật khẩu, Xóa tài khoản ngoại trừ chính mình và ADMIN cuối cùng).
  - Xem và tra cứu thông tin bản đồ tương tự như Viewer.
- **VIEWER**:
  - Đăng nhập hệ thống.
  - Xem bản đồ ranh giới hành chính Gia Lai.
  - Tra cứu thông tin chi tiết từng xã/phường (Diện tích, v.v.).
  - Tìm kiếm nhanh xã/phường trên bản đồ.

---

## 2. Thiết lập Database (Tự động hoá qua Flyway)

Toàn bộ cấu trúc cơ sở dữ liệu và dữ liệu ban đầu được tự động hoá hoàn toàn qua **Flyway migrations** (`BE/src/main/resources/db/migration/core/V1`→`V4`) và `DatabaseSeeder` khi Spring Boot khởi động:
- `V1`: Khởi tạo bảng danh mục hành chính (`provinces`, `wards`).
- `V2`: Nạp danh mục đơn vị hành chính chuẩn quốc gia.
- `V3`: Khởi tạo bảng dữ liệu không gian PostGIS (`gis_provinces`, `gis_wards`).
- `V4`: Nạp toạ độ, ranh giới địa giới hành chính (`MULTIPOLYGON`) 135 xã/phường tỉnh Gia Lai.
- `DatabaseSeeder`: Tự động nạp tài khoản mẫu `admin`/`viewer` (kích hoạt qua cấu hình `SEED_DEFAULT_ACCOUNTS=true`).

Chi tiết cài đặt môi trường xem tại [DEVELOPMENT_SETUP.md](file:///d:/Workspace/WEB%20GIS%20TEMPLATE/docs/en/DEVELOPMENT_SETUP.md).

---

## 3. Danh sách 5 Tasks cốt lõi đã hoàn thành (100% DONE)

### 🔴 PHẦN 1: BACKEND (SPRING BOOT)

#### **TSK-1: Khai báo Entity & Cấu hình Security (JWT)** — ✅ Hoàn thành

- **Nội dung:**
  - Khai báo các JPA Entity trong package `com.website.gis.core.entity`: `Ward`, `Province`, `GisWard`, `User`, `LocalLeader`.
  - Thiết lập Spring Security + Filter xác thực JWT. CORS config cho phép frontend kết nối.
- **Output:** Khung bảo mật Spring Boot chạy thành công ở local port `8080`.
- **Cách verify:** Gọi API bất kỳ khi chưa đăng nhập -> Trả về `401 Unauthorized`.

#### **TSK-2: Xây dựng REST APIs** — ✅ Hoàn thành

- **Nội dung:** Xây dựng hệ thống REST API theo chuẩn [API_CONTRACT.md](file:///d:/Workspace/WEB%20GIS%20TEMPLATE/docs/en/API_CONTRACT.md):
  - **Xác thực:** `POST /api/auth/login` (cấp cookie `HttpOnly`), `GET /api/auth/me` (khôi phục phiên), `POST /api/auth/logout` (xoá cookie).
  - **Quản lý User (Chỉ ADMIN):** `GET /api/admin/users`, `POST /api/admin/users`, `PUT /api/admin/users/{id}`, `DELETE /api/admin/users/{id}` (có chặn tự xoá và chặn xoá admin cuối cùng).
  - **Tra cứu bản đồ:** `GET /api/wards` (danh sách xã), `GET /api/wards/{code}` (chi tiết xã), `GET /api/wards/{code}/geojson` (GeoJSON xã), `GET /api/wards/geojson` (FeatureCollection 135 xã), `GET /api/wards/province/geojson` (ranh giới tỉnh).
- **Cách verify:** Đăng nhập tài khoản `viewer`, gọi API quản trị `/api/admin/users` -> Trả về `403 Forbidden`. Đăng nhập tài khoản `admin` -> thực hiện CRUD thành công.

---

### 🔵 PHẦN 2: FRONTEND (REACT WEB MAP)

#### **TSK-3: Khởi tạo React & Màn hình Đăng nhập** — ✅ Hoàn thành

- **Nội dung:**
  - Khởi tạo React + Vite + TypeScript + Tailwind CSS.
  - Xây dựng Router điều hướng và `AuthContext` quản lý phiên đăng nhập. Axios bật `withCredentials: true` để tự động đính kèm cookie `HttpOnly`.
  - Thiết kế trang Đăng nhập trực quan, sang trọng, tự động điều hướng về trang chủ khi đã đăng nhập.
- **Cách verify:** Truy cập `/` khi chưa đăng nhập -> Tự động chuyển hướng về `/login`. Đăng nhập thành công -> chuyển tiếp vào bản đồ.

#### **TSK-4: Bản đồ GIS tương tác & Giao diện Quản trị** — ✅ Hoàn thành

- **Nội dung:**
  - **Trang chính bản đồ (Main Web Map):**
    - Render bản đồ nền OpenStreetMap bằng `react-leaflet`.
    - Tải và hiển thị ranh giới GeoJSON toàn tỉnh và 135 xã/phường tỉnh Gia Lai.
    - Hỗ trợ hover highlight viền xã, click chọn xã để hiển thị thông tin chi tiết (diện tích, trực thuộc) ở Sidebar.
    - Thanh tìm kiếm nhanh xã/phường với hiệu ứng tự động di chuyển bản đồ (`flyTo`).
    - Panel thống kê nhanh tổng số xã và tổng diện tích.
  - **Giao diện quản lý Users (Chỉ ADMIN):**
    - Panel danh sách người dùng cho phép ADMIN tạo mới tài khoản Viewer, đặt lại mật khẩu hoặc xoá tài khoản qua Modal code tay với Tailwind.
- **Cách verify:** Đăng nhập vai trò `viewer` -> Sidebar không hiển thị mục "Quản lý người dùng". Đăng nhập vai trò `admin` -> hiển thị đầy đủ công cụ quản trị.

---

### 📦 PHẦN 3: ĐÓNG GÓI & TRIỂN KHAI

#### **TSK-5: Đóng gói tích hợp & Triển khai Docker** — ✅ Hoàn thành

- **Nội dung:**
  - `Dockerfile` multi-stage: Build frontend (Vite/React) -> Đóng gói tài nguyên tĩnh vào Spring Boot JAR -> Chạy trên Alpine JRE 17 gọn nhẹ với non-root user.
  - `docker-compose.yml`: Triển khai 3 service `app`, `db` (PostGIS), `caddy` (Reverse proxy tự động HTTPS).
  - `.dockerignore` tối ưu build context (giảm từ 173MB xuống 20KB).
  - `scripts/backup-db.sh`: Kịch bản sao lưu cơ sở dữ liệu tự động.
- **Kết quả verify thực nghiệm:**
  - `./mvnw -B verify`: 33/33 tests PASS trên DB Testcontainers PostGIS thật.
  - `docker compose up -d --build`: Toàn bộ stack khởi động thành công, tự động migrate dữ liệu, `/actuator/health` đạt `UP`.
