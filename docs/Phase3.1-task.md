# Phase 3.1 - Refactoring & Implementation Log

> **Tài liệu sống cùng code (Living Documentation)**  
> **Phiên bản:** Phase 3.1  
> **Thời gian thực hiện:** Tháng 8/2026  
> **Mục tiêu:** Đồng bộ Schema OCOP (Khách hàng thực tế), Chuẩn hóa GIS Utility, Tối ưu Bộ nhớ & Bảo mật, Đồng bộ UI Frontend (Yellow Stars).

---

## 1. Quyết định Kiến trúc & Nghiệp vụ (Decision Log)

| Mã Quyết định | Hạng mục | Quyết định đã duyệt | Chi tiết triển khai |
| :--- | :--- | :--- | :--- |
| **D-001** | Schema OCOP | **Option A (Đồng bộ toàn diện)** | Cập nhật `OcopProduct`, `OcopProductDto`, `Create/UpdateRequest`, Mapper & Controller sang schema `product_types text[]`, `star_rating`, `contact_phone`, `location_address`. Khớp 100% Flyway `V5_1__create_ocop_products.sql`. |
| **D-002** | Anti-DoS / Cache | **Option A (Caffeine Cache)** | Thay thế `ConcurrentHashMap` trong `LoginAttemptService` bằng `Caffeine` cache (`expireAfterWrite=30m`, `maxSize=10,000`), triệt tiêu rủi ro rò rỉ bộ nhớ (Memory Leak / OutOfMemory). |
| **D-003** | Khung mẫu Module | **Keep As-Is (Giữ khung mẫu)** | Giữ nguyên cấu trúc các module `Science` & `Agriculture` làm scaffold khung để phục vụ việc phát triển sau này khi làm việc với khách hàng tương ứng. |
| **D-004** | Chuẩn hóa GIS | **Option B (GisPointUtils)** | Tạo utility dùng chung `GisPointUtils.createPoint(latitude, longitude)` tại `com.website.gis.core.util`, cố định SRID 4326 và chuẩn hóa thứ tự truyền tham số `(lat, lng)`. Áp dụng trên toàn bộ Controllers. |
| **D-005** | Mã lỗi Validation | **Option A (400 Bad Request)** | Chuẩn hóa mã lỗi khi `wardCode` không tồn tại trong CSDL thành `400 Bad Request` (`BadRequestException`) thay vì 404. |
| **D-006** | CORS Config | **Option A (Dynamic Config)** | Hỗ trợ cấu hình `app.cors.allowed-origins` từ `application.properties` / Environment Variables trong `SecurityConfig.java`. |
| **D-007** | Spring Boot Clean | **Option A (Xóa ServletInitializer)** | Xóa bỏ file `ServletInitializer.java` dư thừa của mô hình WAR truyền thống, chuẩn hóa chạy `jar` độc lập qua container / Docker. |

---

## 2. Chi tiết các tệp đã cập nhật (File Changelog)

### 2.1. Backend (`BE/`)
- **`pom.xml`**: Bổ sung dependency `com.github.ben-manes.caffeine:caffeine`.
- **`com.website.gis.ServletInitializer`**: [DELETED] Đã xóa file thừa.
- **`com.website.gis.core.util.GisPointUtils`**: [NEW] Tạo hàm `createPoint(BigDecimal lat, BigDecimal lng)` chuẩn hóa PostGIS Point SRID 4326.
- **`com.website.gis.core.security.LoginAttemptService`**: Chuyển đổi sang `Caffeine` cache với TTL 30 phút và max size 10,000 items.
- **`com.website.gis.features.ocop.entity.OcopProduct`**: Cập nhật các trường `productTypes` (`List<String>` với `@JdbcTypeCode(SqlTypes.ARRAY)`), `starRating`, `contactPhone`, `locationAddress`.
- **`com.website.gis.features.ocop.dto.*`**:
  - `OcopProductDto`: Thêm `productTypes`, `starRating`, `contactPhone`, `locationAddress`.
  - `OcopProductCreateRequest` & `OcopProductUpdateRequest`: Thêm validation `@Min(1) @Max(5)` cho `starRating`, kiểm tra định dạng số điện thoại và địa chỉ.
- **`com.website.gis.features.ocop.controller.OcopController`**:
  - Áp dụng `GisPointUtils`.
  - Ném `BadRequestException` khi `wardCode` không tồn tại.
  - GeoJSON xuất đầy đủ properties `productTypes`, `starRating`, `contactPhone`, `locationAddress`.
- **`com.website.gis.features.science.controller.ScienceController`** & **`AgricultureController`**:
  - Tích hợp `GisPointUtils`, loại bỏ `GeometryFactory` thừa.
- **`BE/src/test/.../OcopControllerTest.java`** & **`OcopControllerIntegrationTest.java`**:
  - Cập nhật 100% test cases khớp với schema mới.

### 2.2. Frontend (`FE/`)
- **`src/config/gisConstants.ts`**: [NEW] Định nghĩa tọa độ trung tâm Gia Lai `GIA_LAI_CENTER: [13.883358, 108.542896]` và `DEFAULT_MAP_ZOOM: 9`.
- **`src/types/ocop.ts`**: Cập nhật kiểu `OcopProduct`, `OcopProductCreateRequest`, `OcopProductUpdateRequest`.
- **`src/types/gis.ts`**: Bổ sung `productTypes`, `starRating`, `contactPhone`, `locationAddress` vào `PoiGeoJsonProperties`.
- **`src/pages/home/components/OcopFormModal.tsx`**:
  - Thêm bộ chọn sao trực quan (1 - 5 ngôi sao màu vàng `#F59E0B` có hiệu ứng tương tác click/hover).
  - Thêm bộ chọn đa ngành hàng (`productTypes`), ô nhập số điện thoại (`contactPhone`), và địa chỉ (`locationAddress`).
- **`src/pages/home/components/OcopPanel.tsx`**:
  - Hiển thị trực quan xếp hạng sao vàng `★ ★ ★ ★ ★` kèm nhãn sao.
  - Hiển thị thẻ tag ngành hàng, số điện thoại liên hệ (click-to-call `tel:`), và địa chỉ cơ sở sản xuất.
  - Mở rộng tìm kiếm client-side theo tên, địa chỉ, sđt, ngành hàng.
- **`src/pages/home/components/DetailsPanel.tsx`**:
  - Hiển thị sao vàng OCOP, ngành hàng, điện thoại, địa chỉ khi xem chi tiết POI.
- **`src/pages/home/components/PoiMarkerClusterLayer.tsx`**:
  - Hiển thị sao vàng và loại sản phẩm trong popup Leaflet marker khi click trên bản đồ.
- **`src/pages/Home.tsx`**:
  - Kết nối dữ liệu chi tiết OCOP đầy đủ.
- **`src/pages/home/components/GisMap.tsx`** & **`MapPicker.tsx`**:
  - Sử dụng hằng số `GIA_LAI_CENTER` từ `gisConstants.ts`.

---

## 3. Kết quả Kiểm thử & Xác nhận (Verification Results)

1. **Backend Compilation**: `mvnw test-compile` hoàn tất với **BUILD SUCCESS** (0 lỗi, 0 cảnh báo bất thường).
2. **Frontend Compilation**: `tsc -b && vite build` thực hiện kiểm tra kiểu nghiêm ngặt và build production bundle thành công.
3. **API Contract & Data Model Sync**: `docs/en/API_CONTRACT.md` và `docs/en/DATA_MODEL.md` đã được đối chiếu, bảo đảm tài liệu phản ánh chính xác 100% cấu trúc codebase đang chạy.
