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

## 2. Thiết lập Database

> [!WARNING]
> **Mục này đã lỗi thời — không còn chạy tay nữa.** Bản kế hoạch gốc (07/07) yêu cầu tự chạy SQL tay để tạo bảng `users`/`local_leaders`. Từ commit "implement Flyway for database migrations" (13/07), toàn bộ schema (bao gồm `provinces`, `wards`, `gis_provinces`, `gis_wards`, `users`, và cả bảng `administrative_units`/`administrative_regions` không có trong bản kế hoạch gốc) được Flyway tự động migrate khi Spring Boot khởi động, đọc từ `BE/src/main/resources/db/migration/core/V1`→`V4`. Tài khoản `admin`/`viewer` mẫu cũng được seed tự động bởi `DatabaseSeeder`, không cần `INSERT` tay.
>
> **Xem hướng dẫn setup chính xác hiện tại tại `docs/en/DEVELOPMENT_SETUP.md` mục 2.** Không dùng script SQL cũ trong mục này nữa.

---

## 3. Danh sách 5 Tasks phát triển cốt lõi (5/5 đã hoàn thành thật)

### 🔴 PHẦN 1: BACKEND (SPRING BOOT)

#### **TSK-1: Khai báo Entity & Cấu hình Security (JWT)** — ✅ Hoàn thành

- **Nội dung:**
  - Tạo các JPA Entity `Ward`, `Province`, `GisWard`, `User`, `LocalLeader` ánh xạ chính xác với dữ liệu sẵn có và 2 bảng mới tạo.
  - Thiết lập Spring Security + Filter xác thực JWT. CORS config cho phép FE gọi trực tiếp.
- **Input:** Cơ sở dữ liệu hiện có.
- **Output:** Khung bảo mật Spring Boot chạy thành công ở local port `8080`.
- **Cách verify:** Gọi API bất kỳ khi chưa đăng nhập -> Trả về `401 Unauthorized`.
- **Cập nhật thực tế:** các Entity/Repository/Security ở trên hiện nằm trong package `com.website.gis.core.*` (không phải package phẳng như bản kế hoạch gốc), theo đúng cấu trúc `core/` vs `features/` mô tả tại `ARCHITECTURE SPECIFICATION.md` mục 4.1 — chuẩn bị sẵn cho việc thêm module ở Giai đoạn 2.

#### **TSK-2: Xây dựng REST APIs** — ✅ Hoàn thành

- **Nội dung:** Viết các endpoint RESTful sau:
  - `POST /api/auth/login`: Nhận Username/Password, xác thực và trả về thông tin vai trò. ~~Trả về Token JWT~~ _(đã lỗi thời, xem ghi chú thực tế bên dưới)_.
  - **Nhóm quản lý User (Chỉ ADMIN được phép truy cập):**
    - `GET /api/admin/users`: Lấy danh sách tài khoản.
    - `POST /api/admin/users`: Tạo mới tài khoản VIEWER.
    - `PUT /api/admin/users/{id}`: Cập nhật thông tin (Tên hiển thị, Mật khẩu mới).
    - `DELETE /api/admin/users/{id}`: Xóa tài khoản người dùng.
  - **Nhóm tra cứu bản đồ (Public hoặc bắt buộc Đăng nhập tùy nhu cầu, khuyên dùng bắt buộc Đăng nhập):**
    - `GET /api/wards`: Lấy danh sách xã/phường (hỗ trợ tìm kiếm theo tên).
    - `GET /api/wards/{code}`: Xem chi tiết thông số xã/phường (diện tích).
    - `GET /api/wards/{code}/geojson`: Trả về dữ liệu tọa độ ranh giới địa giới hành chính xã/phường dưới dạng JSON chuẩn.
- **Input:** JPA Repositories và Spring Controllers.
- **Output:** Các endpoint hoạt động chính xác.
- **Cách verify:** Đăng nhập tài khoản `viewer`, gọi API tạo tài khoản `/api/admin/users` -> Trả về lỗi `403 Forbidden`. Đăng nhập tài khoản `admin` -> tạo thành công.
- **Cập nhật thực tế (TSK-4):** khi làm map, đã bổ sung thêm 2 endpoint gộp không có trong danh sách gốc để tối ưu hiệu năng tải toàn bộ 135 xã một lần: `GET /api/wards/geojson` (FeatureCollection toàn tỉnh) và `GET /api/wards/province/geojson` (đường viền tỉnh). Danh sách API đầy đủ và chính xác nhất hiện nay nằm ở `docs/en/API_CONTRACT.md`, không phải danh sách trong mục này.
- **Cập nhật thực tế (bảo mật JWT):** `POST /api/auth/login` không còn trả JWT trong response body — token được set qua cookie `HttpOnly` + `Secure` + `SameSite` (tên cookie mặc định `gis_token`, cấu hình qua `app.jwt.cookie-*`). Đồng thời đã bổ sung 2 endpoint không có trong danh sách gốc: `GET /api/auth/me` (lấy thông tin user hiện tại từ cookie, dùng để khôi phục phiên khi FE reload) và `POST /api/auth/logout` (xoá cookie bằng `Max-Age=0`). `JwtAuthenticationFilter` vẫn chấp nhận header `Authorization: Bearer` như phương án dự phòng cho công cụ gọi API trực tiếp (Swagger/Postman), nhưng FE web không dùng cách này nữa.

---

### 🔵 PHẦN 2: FRONTEND (REACT WEB MAP)

#### **TSK-3: Khởi tạo React & Màn hình Đăng nhập** — ✅ Hoàn thành

- **Nội dung:**
  - Khởi tạo React + Vite + Tailwind CSS + Shadcn UI.
  - Tạo Router điều hướng và Auth Context lưu trạng thái đăng nhập. Cấu hình Axios đính kèm Bearer Token tự động.
  - Thiết kế trang Đăng nhập đơn giản, sang trọng.
- **Input:** Khởi chạy project FE sạch.
- **Output:** Ứng dụng login được, chuyển hướng về trang chủ. ~~Lưu Token vào LocalStorage~~ _(đã lỗi thời, xem ghi chú thực tế bên dưới)_.
- **Cách verify:** Thử gõ bừa URL `/` khi chưa đăng nhập -> Tự động redirect về `/login`. Đăng nhập đúng `admin` hoặc `viewer` -> vào được bản đồ.
- **Cập nhật thực tế (bảo mật JWT):** FE không còn lưu token ở LocalStorage và Axios không còn tự gắn header `Authorization` như mô tả gốc. Token nằm trong cookie `HttpOnly` do BE set (JS không đọc được), Axios chỉ cần bật `withCredentials: true` để trình duyệt tự đính kèm cookie. `AuthContext` khôi phục phiên đăng nhập bằng cách gọi `GET /api/auth/me` mỗi khi app khởi động, thay vì đọc token đã lưu.

#### **TSK-4: Bản đồ GIS tương tác & Giao diện Quản trị** — ✅ Hoàn thành

- **Nội dung:**
  - **Trang chính bản đồ (Main Web Map):**
    - Render bản đồ nền OpenStreetMap bằng `react-leaflet`.
    - Gọi API tải và vẽ ranh giới GeoJSON các xã của tỉnh Gia Lai. Hover highlight viền xã; click chọn xã hiển thị chi tiết (Diện tích, thông báo tên xã) bên Sidebar phải.
    - Ô tìm kiếm nhanh xã/phường: Chọn xã từ kết quả tìm kiếm $\rightarrow$ Bản đồ tự bay đến (`flyTo`) và chọn xã đó.
    - Panel thông tin nhỏ thống kê nhanh: Tổng số xã, tổng diện tích.
  - **Giao diện quản lý Users (Chỉ hiển thị nút điều hướng cho ADMIN):**
    - Một trang/màn hình danh sách người dùng cho phép ADMIN tạo mới tài khoản Viewer hoặc reset mật khẩu, xóa tài khoản thông qua các Form/Dialog của Shadcn UI.
- **Input:** Thư viện map, API BE.
- **Output:** Giao diện trực quan, hoạt động hoàn hảo.
- **Cách verify:** Đăng nhập vai trò `viewer` -> Sidebar chính không hiển thị phần "Quản lý người dùng". Đăng nhập vai trò `admin` -> hiển thị và thực hiện CRUD thành công.
- **Ghi chú thực tế:** modal CRUD user được code tay bằng Tailwind (không dùng Radix UI/Shadcn dialog như dự kiến ban đầu) để nhẹ hơn. Layer "Huyện" trong Left Drawer đã được bỏ, chỉ giữ 2 toggle "Ranh giới cấp Tỉnh" và "Ranh giới cấp Xã".

---

### 📦 PHẦN 3: ĐÓNG GÓI & TRIỂN KHAI

#### **TSK-5: Đóng gói tích hợp & Triển khai Docker** — ✅ ĐÃ HOÀN THÀNH & VERIFY THỰC NGHIỆM (100% DONE)

- **Nội dung thực tế (khớp với đặc tả gốc trong `docs/en/DEPLOYMENT & FLEET STRATEGY.md`):**
  - `Dockerfile` (root repo) — multi-stage đúng như dự kiến: build FE (`node:20-alpine`, pin `pnpm@9` qua corepack) → build BE (`maven:3.9-eclipse-temurin-17`, copy `FE/dist` vào `src/main/resources/static` trước `mvnw package`, có thêm `chmod +x mvnw` phòng vệ) → runtime (`eclipse-temurin:17-jre-alpine`, user không phải root).
  - `docker-compose.yml` (root repo) — đúng 3 dịch vụ `app`/`db`/`caddy` như đặc tả, `app`/`db` không public port, chỉ `caddy` expose 80/443.
  - `Caddyfile` (root repo) — đúng domain placeholder `gis.gialai.gov.vn`, reverse proxy vào `app:8080`.
  - `.dockerignore` (root repo) — loại trừ `node_modules`, `target`, `.git` giúp tăng tốc độ build context (20KB thay vì 173MB) và chống xung đột nhị phân host/container.
  - `.env.example` và `.env` — đúng tên biến `FEATURES_OCOP_ENABLED`/`FEATURES_SCIENCE_ENABLED`/`FEATURES_AGRICULTURE_ENABLED`, `JWT_SECRET` hex 64-byte.
  - `scripts/backup-db.sh` — khớp với script mô tả ở Section 5.3 của tài liệu trên.
- **Output:** Toàn bộ pipeline build và đóng gói chạy thông suốt từ đầu đến cuối.
- **Kết quả verify thực nghiệm (Đạt 100% vào ngày 2026-08-17):**
  - Đã chạy `./mvnw -B verify` với Testcontainers PostGIS: **33/33 tests PASS (0 failures, 0 errors)**.
  - Đã chạy `docker compose up -d --build` thành công:
    - Multi-stage build FE (Vite) + BE (Maven JAR) không lỗi.
    - Cả 3 container `gialai-gis-app`, `gialai-gis-db`, `gialai-gis-proxy` đều `Up (healthy)`.
    - Flyway tự động migrate V1→V4 vào database, nhập đủ dữ liệu 135 xã/phường và bảng ranh giới không gian `gis_wards`.
    - `DatabaseSeeder` tự động seed tài khoản `admin` và `viewer`.
    - `GET /actuator/health` trả về `{"status":"UP"}`.
    - `GET /` trả về toàn bộ ứng dụng React Web GIS.
- **Chi tiết đặc tả vận hành đầy đủ** (backup, rollback, checklist VPS lần đầu, kế hoạch mở rộng nhiều khách hàng): xem `docs/en/DEPLOYMENT & FLEET STRATEGY.md`.
