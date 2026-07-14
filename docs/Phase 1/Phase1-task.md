# KẾ HOẠCH TỐI GIẢN (CẬP NHẬT) - GIAI ĐOẠN 1: NỀN TẢNG HÀNH CHÍNH
## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!IMPORTANT]
> **Trạng thái tài liệu (cập nhật 2026-07-14): Giai đoạn 1 đã hoàn thành đầy đủ 5/5 task**, bao gồm cả TSK-5 (Docker) vừa được bổ sung. File này giờ mang tính **lịch sử/tham chiếu** cho những gì đã làm ở Giai đoạn 1, không còn là checklist "cần làm". Khi cần thông tin cập nhật nhất về kiến trúc, hãy xem các file trong `docs/en/` (đã được sửa nhiều sau khi file này viết xong ngày 07/07):
> - Schema DB thật + quy ước bảng tương lai: `DATA_MODEL.md`
> - Kiến trúc modular, package `core/` vs `features/`: `ARCHITECTURE SPECIFICATION.md`
> - Setup máy dev (không cần chạy SQL tay): `DEVELOPMENT_SETUP.md`
> - Vận hành/deploy thật (Docker, Caddy, backup, fleet): `DEPLOYMENT & FLEET STRATEGY.md`
> - API hiện có + quy ước API module tương lai: `API_CONTRACT.md`
>
> Tên module Khoa học Công nghệ đã được thống nhất là **`science`** (không dùng `khcn`) trong toàn bộ code và tài liệu — xem ghi chú tại `ARCHITECTURE SPECIFICATION.md` mục 6.4.

> [!TIP]
> **Triết lý tối giản tối đa:**
> Hệ thống chỉ có hai vai trò: **ADMIN** (Quản trị hệ thống, quản lý tài khoản) và **VIEWER** (Xem và tra cứu bản đồ). Mọi tính năng chỉnh sửa dữ liệu hay thay đổi địa giới hành chính đều được lược bỏ (bạn sẽ sửa trực tiếp trong PostgreSQL khi cần thiết). Không dùng Nginx (dùng Caddy — xem TSK-5 bên dưới) hay các cấu hình phức tạp khác.

---

## 1. Vai trò người dùng (Roles Matrix)
* **ADMIN**: 
  - Đăng nhập hệ thống.
  - Quản lý tài khoản người dùng khác (Xem danh sách, Tạo mới, Sửa thông tin/đổi mật khẩu, Xóa tài khoản `VIEWER`).
  - Xem và tra cứu thông tin bản đồ tương tự như Viewer.
* **VIEWER**:
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

## 3. Danh sách 5 Tasks phát triển cốt lõi

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
  - `POST /api/auth/login`: Nhận Username/Password, xác thực và trả về Token JWT cùng thông tin vai trò.
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

---

### 🔵 PHẦN 2: FRONTEND (REACT WEB MAP)

#### **TSK-3: Khởi tạo React & Màn hình Đăng nhập** — ✅ Hoàn thành
- **Nội dung:**
  - Khởi tạo React + Vite + Tailwind CSS + Shadcn UI.
  - Tạo Router điều hướng và Auth Context lưu trạng thái đăng nhập. Cấu hình Axios đính kèm Bearer Token tự động.
  - Thiết kế trang Đăng nhập đơn giản, sang trọng.
- **Input:** Khởi chạy project FE sạch.
- **Output:** Ứng dụng login được, chuyển hướng về trang chủ và lưu Token vào LocalStorage.
- **Cách verify:** Thử gõ bừa URL `/` khi chưa đăng nhập -> Tự động redirect về `/login`. Đăng nhập đúng `admin` hoặc `viewer` -> vào được bản đồ.

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
- **Ghi chú thực tế:** modal CRUD user được code tay bằng Tailwind (không dùng Radix UI/Shadcn dialog như dự kiến ban đầu) để nhẹ hơn. Layer "Huyện" trong Left Drawer đã được bỏ, chỉ giữ 2 toggle "Ranh giới cấp Tỉnh" và "Ranh giới cấp Xã". Xem chi tiết tại `docs/Phase 1/Task 4/walkthrough.md`.

---

### 📦 PHẦN 3: ĐÓNG GÓI & TRIỂN KHAI

#### **TSK-5: Đóng gói tích hợp & Triển khai Docker** — ✅ Hoàn thành (2026-07-14)
- **Nội dung (đã cập nhật so với bản gốc):**
  - Multi-stage `Dockerfile` ở root repo: build FE (`node:20-alpine` + pnpm) → build BE (`maven:3.9-eclipse-temurin-17`, copy `FE/dist` vào `src/main/resources/static` trước khi `mvnw package`) → runtime (`eclipse-temurin:17-jre-alpine`, chạy user không phải root).
  - `docker-compose.yml` gồm **3 dịch vụ** (khác với bản kế hoạch gốc chỉ dự tính 2 dịch vụ `db`+`app`): `app` (Spring Boot, không public port ra ngoài), `db` (`postgis/postgis:15-3.4-alpine`, không public port 5432 ra ngoài), và **`caddy`** (reverse proxy TLS tự động cấp chứng chỉ Let's Encrypt, expose port 80/443 — vẫn giữ đúng triết lý "không dùng Nginx" ở đầu file, chỉ là thay bằng Caddy thay vì bỏ hẳn reverse proxy).
  - `Caddyfile` cấu hình domain trỏ vào `app:8080`.
  - `.env.example` mẫu biến môi trường (DB credentials, JWT secret, feature flags `ENABLE_OCOP`/`ENABLE_SCIENCE`/`ENABLE_AGRICULTURE` — mặc định `false` cho Giai đoạn 1).
- **Input:** Các file cấu hình Docker (đã có sẵn tại root repo: `Dockerfile`, `docker-compose.yml`, `Caddyfile`, `.env.example`).
- **Output:** Toàn bộ hệ thống chạy chỉ bằng lệnh `docker compose up -d --build` sau khi `cp .env.example .env` và điền secret thật.
- **Cách verify:** Máy chủ triển khai sạch, sau khi build, kiểm tra `/actuator/health` trả về `UP` qua HTTPS (do Caddy cấp chứng chỉ tự động), giao diện FE hiển thị đầy đủ khi truy cập domain.
- **Chi tiết vận hành đầy đủ** (backup, rollback, checklist VPS lần đầu, kế hoạch mở rộng nhiều khách hàng): xem `docs/en/DEPLOYMENT & FLEET STRATEGY.md`.
