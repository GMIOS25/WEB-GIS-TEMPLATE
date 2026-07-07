# Kế hoạch triển khai TSK-2: Xây dựng REST APIs

Kế hoạch này chi tiết việc phát triển các REST API phục vụ xác thực người dùng, quản trị viên (ADMIN) quản lý tài khoản người dùng, và các API tra cứu thông tin địa giới hành chính, dữ liệu GeoJSON cho bản đồ tỉnh Gia Lai.

## Quy tắc thiết kế hệ thống
- **Xác thực:** Bắt buộc xác thực bằng JWT Bearer token cho toàn bộ các endpoints ngoại trừ `/api/auth/login` và API tài liệu Swagger/OpenAPI.
- **Phân quyền:** Chỉ tài khoản có vai trò `ADMIN` mới được phép thao tác trên `/api/admin/users/**`. Các tài khoản có vai trò `VIEWER` hoặc `ADMIN` đều có thể gọi các API tra cứu `/api/wards/**`.
- **Dữ liệu GeoJSON:** Sử dụng câu truy vấn Native SQL kết hợp hàm `ST_AsGeoJSON` từ PostGIS để lấy chuỗi GeoJSON tọa độ trực tiếp từ PostgreSQL, tránh lỗi serialize đối tượng hình học phức tạp của JTS Geometry.
- **Dữ liệu Dân số:** Bỏ qua việc truy vấn và cập nhật dữ liệu dân số theo thống nhất với người dùng.

---

## Thay đổi đề xuất

### 1. Thay đổi cấu hình Security
#### [MODIFY] [SecurityConfig.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/config/SecurityConfig.java)
- Cập nhật phân quyền truy cập:
  - Cho phép truy cập công khai đến `/api/auth/**` và swagger UI.
  - Hạn chế quyền truy cập vào `/api/admin/**` chỉ dành cho người dùng có quyền `ADMIN` (`hasRole("ADMIN")`).
  - Yêu cầu xác thực đối với tất cả các request khác (bao gồm `/api/wards/**`).

---

### 2. Định nghĩa các Lớp Data Transfer Object (DTO)
Tạo gói mới `com.website.gis.dto` để quản lý các request/response sạch sẽ:

#### [NEW] [LoginRequest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/LoginRequest.java)
- Trường: `username`, `password`.

#### [NEW] [LoginResponse.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/LoginResponse.java)
- Trường: `token`, `username`, `fullName`, `role`.

#### [NEW] [UserDto.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/UserDto.java)
- Trường: `id`, `username`, `fullName`, `role`. (Dùng để trả về thông tin user mà không lộ mật khẩu).

#### [NEW] [UserCreateRequest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/UserCreateRequest.java)
- Trường: `username`, `password`, `fullName` kèm validation cơ bản.

#### [NEW] [UserUpdateRequest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/UserUpdateRequest.java)
- Trường: `fullName`, `password` (mật khẩu mới, có thể để trống).

#### [NEW] [WardDto.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/WardDto.java)
- Trường: `code`, `name`, `fullName`, `provinceName`.

#### [NEW] [WardDetailDto.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/WardDetailDto.java)
- Trường: `code`, `name`, `fullName`, `provinceName`, `areaKm2`.

---

### 3. Tầng Repository
Tạo các JPA Repository cho Ward và GisWard:

#### [NEW] [WardRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/WardRepository.java)
- Hỗ trợ tìm kiếm theo tên xã: `findByNameContainingIgnoreCase(String name)`.

#### [NEW] [GisWardRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/GisWardRepository.java)
- Tìm kiếm GIS xã theo mã xã `ward_code`.
- Query lấy GeoJSON trực tiếp: `@Query(value = "SELECT ST_AsGeoJSON(geom) FROM gis_wards WHERE ward_code = :wardCode", nativeQuery = true)`.

---

### 4. Tầng Controller
Tạo các REST Controller xử lý HTTP requests:

#### [NEW] [AuthController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/AuthController.java)
- Endpoint: `POST /api/auth/login`.
- Thực hiện xác thực thông qua `AuthenticationManager`, tạo JWT Token và trả về `LoginResponse`.

#### [NEW] [AdminController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/AdminController.java)
- Endpoint: `GET /api/admin/users`: Liệt kê tất cả user.
- Endpoint: `POST /api/admin/users`: Thêm người dùng VIEWER mới (sử dụng `PasswordEncoder` để hash mật khẩu).
- Endpoint: `PUT /api/admin/users/{id}`: Cập nhật `fullName` và/hoặc mật khẩu mới.
- Endpoint: `DELETE /api/admin/users/{id}`: Xóa user theo id. Ngăn chặn việc tự xóa chính tài khoản đang đăng nhập.

#### [NEW] [WardController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/WardController.java)
- Endpoint: `GET /api/wards`: Lấy danh sách hoặc tìm kiếm xã/phường.
- Endpoint: `GET /api/wards/{code}`: Lấy thông tin chi tiết (bao gồm diện tích).
- Endpoint: `GET /api/wards/{code}/geojson`: Trả về GeoJSON String (media type: `application/json`).

---

## Kế hoạch kiểm thử & Xác thực

### Kiểm thử tự động (Unit Test)
- Sử dụng Spring MockMvc để kiểm thử:
  - `AuthControllerTest`: Đăng nhập thành công/thất bại.
  - `AdminControllerTest`: CRUD user và kiểm tra phân quyền (gọi API bằng token VIEWER trả về `403 Forbidden`).
  - `WardControllerTest`: Truy vấn dữ liệu địa giới và lấy GeoJSON.

### Kiểm thử thủ công bằng Bruno (HTTP Client)
- Tạo thư mục `Bruno/` chứa các file collection gọi thử nghiệm API:
  - `Login Admin` & `Login Viewer`.
  - Admin CRUD users (GET, POST, PUT, DELETE).
  - Tra cứu wards (Danh sách, chi tiết, GeoJSON).
