# Walkthrough - Kết quả thực hiện TSK-2: REST APIs

Tôi đã hoàn thành việc xây dựng toàn bộ các endpoint RESTful phục vụ xác thực người dùng, phân quyền quản lý tài khoản cho Admin và tra cứu địa giới hành chính xã/phường phục vụ bản đồ GIS tỉnh Gia Lai.

---

## 🛠️ Các thay đổi đã thực hiện

### 1. Cấu hình bảo mật
- **[SecurityConfig.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/config/SecurityConfig.java)**:
  - Phân quyền endpoint `/api/admin/**` chỉ dành cho người dùng có vai trò `ADMIN` (`hasRole("ADMIN")`).
  - Đặt cấu hình bảo mật bắt buộc đăng nhập (JWT Token) đối với toàn bộ các endpoint tra cứu địa giới hành chính `/api/wards/**`.

### 2. Gói DTO (`com.website.gis.dto`)
Tạo mới các DTO để truyền nhận dữ liệu qua API:
- [LoginRequest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/LoginRequest.java) & [LoginResponse.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/LoginResponse.java)
- [UserDto.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/UserDto.java), [UserCreateRequest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/UserCreateRequest.java), [UserUpdateRequest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/UserUpdateRequest.java)
- [WardDto.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/WardDto.java) & [WardDetailDto.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/dto/WardDetailDto.java)

### 3. Tầng Repository
- **[WardRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/WardRepository.java)**: Tìm kiếm xã/phường không phân biệt hoa thường theo tên.
- **[GisWardRepository.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/repository/GisWardRepository.java)**: Truy vấn diện tích xã và lấy chuỗi tọa độ GeoJSON trực tiếp từ PostGIS bằng Native SQL Query `ST_AsGeoJSON(geom)`.

### 4. Tầng Controller
- **[AuthController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/AuthController.java)**: `POST /api/auth/login` - Trả về token JWT và thông tin người dùng.
- **[AdminController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/AdminController.java)**:
  - `GET /api/admin/users` - Danh sách người dùng.
  - `POST /api/admin/users` - Tạo người dùng VIEWER mới (mật khẩu được mã hóa bcrypt).
  - `PUT /api/admin/users/{id}` - Cập nhật tên và/hoặc mật khẩu mới.
  - `DELETE /api/admin/users/{id}` - Xóa tài khoản (ngăn chặn tự xóa tài khoản của chính mình).
- **[WardController.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/main/java/com/website/gis/controller/WardController.java)**:
  - `GET /api/wards` - Danh sách và tìm kiếm xã.
  - `GET /api/wards/{code}` - Thông tin chi tiết xã (kèm diện tích `areaKm2`).
  - `GET /api/wards/{code}/geojson` - Dữ liệu ranh giới địa lý GeoJSON trực tiếp.

### 5. Bộ sưu tập kiểm thử Bruno
Tạo thư mục **[Bruno](file:///d:/Work/WEB%20GIS%20TEMPLATE/Bruno)** ở thư mục gốc chứa các file `.bru` để chạy thử nghiệm các API trên môi trường local.

---

## 🧪 Kết quả kiểm thử (Unit Tests)

Tôi đã tạo 3 lớp kiểm thử MockMvc để xác thực logic phân quyền, nghiệp vụ CRUD và xử lý dữ liệu GeoJSON:
- [AuthControllerTest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/test/java/com/website/gis/controller/AuthControllerTest.java)
- [AdminControllerTest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/test/java/com/website/gis/controller/AdminControllerTest.java)
- [WardControllerTest.java](file:///d:/Work/WEB%20GIS%20TEMPLATE/BE/src/test/java/com/website/gis/controller/WardControllerTest.java)

### Kết quả chạy lệnh `.\mvnw.cmd test`:
- Tổng số **13/13** unit tests thuộc lớp WebMvcTest (chạy Mock độc lập không phụ thuộc vào cơ sở dữ liệu) đã **VƯỢT QUA THÀNH CÔNG (PASSED)**.
- Chỉ duy nhất integration test `GisApplicationTests.contextLoads` gặp lỗi do môi trường local hiện tại chưa chạy Docker Desktop để cung cấp môi trường Testcontainers (Lỗi: `Could not find a valid Docker environment`).
