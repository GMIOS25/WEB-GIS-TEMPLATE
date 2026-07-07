# KẾ HOẠCH TỐI GIẢN (CẬP NHẬT) - GIAI ĐOẠN 1: NỀN TẢNG HÀNH CHÍNH
## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!TIP]
> **Triết lý tối giản tối đa:**
> Hệ thống chỉ có hai vai trò: **ADMIN** (Quản trị hệ thống, quản lý tài khoản) và **VIEWER** (Xem và tra cứu bản đồ). Mọi tính năng chỉnh sửa dữ liệu hay thay đổi địa giới hành chính đều được lược bỏ (bạn sẽ sửa trực tiếp trong PostgreSQL khi cần thiết). Không sử dụng Nginx hay các cấu hình phức tạp.

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

## 2. Thiết lập Database (Chạy thủ công)
Chạy script này một lần trong PostgreSQL để tạo bảng quản lý người dùng và bảng tham chiếu lãnh đạo địa phương:

```sql
-- 1. Bảng lưu người dùng
CREATE TABLE users (
    id bigint PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    username varchar(50) UNIQUE NOT NULL,
    password varchar(100) NOT NULL, -- Mật khẩu mã hóa bcrypt
    full_name varchar(100),
    role varchar(20) NOT NULL CHECK (role IN ('ADMIN', 'VIEWER'))
);

-- 2. Bảng tham chiếu Lãnh đạo (Chỉ định nghĩa cấu trúc bảng để dùng sau này)
CREATE TABLE local_leaders (
    id integer PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    full_name varchar(255) NOT NULL,
    position varchar(100) NOT NULL,
    phone_number varchar(20),
    ward_code varchar(20) REFERENCES wards(code)
);

-- 3. Seed sẵn 2 tài khoản mẫu (Mật khẩu mặc định: 123456)
-- Hash bcrypt của '123456' là: $2a$10$tZ2E7H4H4f4V4jH7/Oa9NuF2g2pMh1B7F7G2K.Lz4j2w7n/wz9i1O
INSERT INTO users (username, password, full_name, role) VALUES 
('admin', '$2a$10$tZ2E7H4H4f4V4jH7/Oa9NuF2g2pMh1B7F7G2K.Lz4j2w7n/wz9i1O', 'Quản trị viên Gia Lai', 'ADMIN'),
('viewer', '$2a$10$tZ2E7H4H4f4V4jH7/Oa9NuF2g2pMh1B7F7G2K.Lz4j2w7n/wz9i1O', 'Người xem bản đồ', 'VIEWER');
```

---

## 3. Danh sách 5 Tasks phát triển cốt lõi

### 🔴 PHẦN 1: BACKEND (SPRING BOOT)

#### **TSK-1: Khai báo Entity & Cấu hình Security (JWT)**
- **Nội dung:** 
  - Tạo các JPA Entity `Ward`, `Province`, `GisWard`, `User`, `LocalLeader` ánh xạ chính xác với dữ liệu sẵn có và 2 bảng mới tạo.
  - Thiết lập Spring Security + Filter xác thực JWT. CORS config cho phép FE gọi trực tiếp.
- **Input:** Cơ sở dữ liệu hiện có.
- **Output:** Khung bảo mật Spring Boot chạy thành công ở local port `8080`.
- **Cách verify:** Gọi API bất kỳ khi chưa đăng nhập -> Trả về `401 Unauthorized`.

#### **TSK-2: Xây dựng REST APIs**
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

---

### 🔵 PHẦN 2: FRONTEND (REACT WEB MAP)

#### **TSK-3: Khởi tạo React & Màn hình Đăng nhập**
- **Nội dung:**
  - Khởi tạo React + Vite + Tailwind CSS + Shadcn UI.
  - Tạo Router điều hướng và Auth Context lưu trạng thái đăng nhập. Cấu hình Axios đính kèm Bearer Token tự động.
  - Thiết kế trang Đăng nhập đơn giản, sang trọng.
- **Input:** Khởi chạy project FE sạch.
- **Output:** Ứng dụng login được, chuyển hướng về trang chủ và lưu Token vào LocalStorage.
- **Cách verify:** Thử gõ bừa URL `/` khi chưa đăng nhập -> Tự động redirect về `/login`. Đăng nhập đúng `admin` hoặc `viewer` -> vào được bản đồ.

#### **TSK-4: Bản đồ GIS tương tác & Giao diện Quản trị**
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

---

### 📦 PHẦN 3: ĐÓNG GÓI & TRIỂN KHAI

#### **TSK-5: Đóng gói tích hợp & Triển khai Docker đơn giản**
- **Nội dung:**
  - Cấu hình copy file build tĩnh của React FE sang thư mục `src/main/resources/static` của BE khi build.
  - Viết `Dockerfile` để đóng gói toàn bộ ứng dụng JAR tích hợp này.
  - Viết `docker-compose.yml` gồm 2 dịch vụ duy nhất: `db` (Postgres + PostGIS) và `app` (Spring Boot chạy cổng `8080`).
- **Input:** Các file cấu hình Docker.
- **Output:** Toàn bộ hệ thống chạy chỉ bằng lệnh `docker-compose up -d`.
- **Cách verify:** Máy chủ triển khai sạch chạy lệnh và truy cập được vào cổng 8080 hiển thị đầy đủ giao diện hoạt động.
