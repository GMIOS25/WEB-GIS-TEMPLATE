# BÁO CÁO TIẾN ĐỘ & KẾ HOẠCH CHI TIẾT - GIAI ĐOẠN 3: TÍCH HỢP BẢN ĐỒ GIS (GIS MAP INTEGRATION)

## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!IMPORTANT]
> **Trạng thái tài liệu (Cập nhật 2026-08-21 & 2026-09): Giai đoạn 3 đã HOÀN THÀNH trong môi trường phát triển cục bộ (Local Development).**
>
> Module OCOP đã hoàn thiện toàn diện nghiệp vụ thực tế (xếp hạng sao vàng, đa ngành hàng, hotline, địa chỉ cơ sở và hiển thị bản đồ). Hai module `science` và `agriculture` hoạt động ở vai trò Scaffold khung mẫu thử nghiệm tính độc lập của các lớp dữ liệu trên bản đồ. Bảng màu tuân thủ `docs/UI-UX/Design_rule.md` (OCOP: `#F97316` Cam, Science: `#64748B` Xám Slate, Agriculture: `#6B7280` Xám Cool).

---

## 1. Bảng Theo dõi Tiến độ Tasks (Phase 3 Live Task Tracker)

| Mã Task | Hạng mục công việc | Phạm vi (Scope) | Trạng thái | Ghi chú kỹ thuật |
| :--- | :--- | :--- | :--- | :--- |
| **TSK-17** | Endpoint GeoJSON cho OCOP / Science / Agriculture (`GET /api/{feature}/geojson`) | Backend | **HOÀN THÀNH** | Jackson `ObjectMapper` FeatureCollection, tọa độ `[lng, lat]`, cache header `private, max-age=3600`, 100% pass tests. |
| **TSK-18** | Truy vấn tìm kiếm bán kính (`GET /api/{feature}/nearby`) | Backend / DB | **HOÀN THÀNH** | PostGIS `ST_DWithin` trên `geography`, bổ sung Expression GiST index `idx_*_geog`, validate lat/lng/radius, 100% pass tests. |
| **TSK-19** | Lọc theo vùng hành chính (`GET /api/{feature}?wardCode=...`) | Backend | **HOÀN THÀNH** | Cả 3 controller hỗ trợ lọc theo xã kết hợp phân trang; riêng OCOP hỗ trợ tìm kiếm tên qua `?q=`, 100% pass tests. |
| **TSK-20** | Layer điểm OCOP trên bản đồ chính (có clustering & halo) | Frontend | **HOÀN THÀNH** | `PoiMarkerClusterLayer.tsx`, màu `#F97316`, halo viền trắng, badge `(N)`, popup thông tin + nút `[Xem chi tiết]`. |
| **TSK-21** | Layer điểm Science & Agriculture trên bản đồ chính | Frontend | **HOÀN THÀNH** | Tích hợp trong `PoiMarkerClusterLayer.tsx` với màu Slate `#64748B` và Cool Gray `#6B7280`, độc lập theo feature flag. |
| **TSK-22** | Sidebar điều khiển lớp dữ liệu (Single Source of Truth) + Legend | Frontend | **HOÀN THÀNH** | `SidebarDrawer.tsx` quản lý duy nhất layer state, có chấm màu Legend đối chiếu trực quan (🟠, 🔘, ⚪). |
| **TSK-23** | Interactive Map Coordinate Picker cho form thêm/sửa | Frontend | **HOÀN THÀNH** | `MapPicker.tsx` tích hợp vào `OcopFormModal.tsx`, `ScienceFormModal.tsx`, `AgricultureFormModal.tsx`. |
| **TSK-24** | UI Tìm kiếm bán kính & Lọc không gian | Frontend | **HOÀN THÀNH** | `RadiusSearchControl.tsx`, vẽ vòng tròn bán kính `Circle` trên Leaflet, highlight hiệu ứng pulse các điểm kết quả. |
| **TSK-25 (Local)** | Bảng thống kê số lượng tổng quan | Frontend | **HOÀN THÀNH** | `StatsBoard.tsx` tổng hợp số lượng đơn vị OCOP, Science, Agriculture theo thời gian thực trên bản đồ. |
| **TSK-25 (Export)** | Xuất báo cáo PDF/Excel (`xlsx`) | Backend/FE | **TẠM HOÃN (DEFERRED)** | Tạm hoãn trong Local Phase 3; sẽ triển khai khi có hạ tầng worker/object storage ở production. |

---

## 2. Chi tiết Kỹ thuật Triển khai

### 🔴 PHẦN 1: BACKEND (SPRING BOOT & POSTGIS)

#### **TSK-17: Endpoint GeoJSON (`GET /api/{feature}/geojson`)**
- Trả về `FeatureCollection` Point chuẩn GeoJSON RFC 7946: `geometry.coordinates = [longitude, latitude]`.
- Properties tối thiểu: `{ id, name, productType/unitType, wardCode, imageUrl }`. OCOP bổ sung `productTypes`, `starRating`, `contactPhone`, `locationAddress`.
- Cache-Control: `private, max-age=3600`.
- Tự động tắt (trả về 404) khi `features.<module>.enabled=false`.

#### **TSK-18: Radius Search & GiST Index (`GET /api/{feature}/nearby`)**
- Endpoint: `GET /api/{feature}/nearby?lat={lat}&lng={lng}&radiusKm={radiusKm}`.
- Migration Flyway bổ sung Expression GiST index tối ưu cho phép cast geography:
  - `ocop`: index `idx_ocop_products_geog` đã được tạo trực tiếp trong `V5_1__create_ocop_products.sql` (file `V5_1_1__insert_data_ocop.sql` nạp dữ liệu mẫu ban đầu).
  - `V5_2_1__add_geog_gist_index_science.sql`
  - `V5_3_1__add_geog_gist_index_agriculture.sql`
- Native query: `ST_DWithin(CAST(geom AS geography), ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)`.
- Input validation: `lat ∈ [-90, 90]`, `lng ∈ [-180, 180]`, `radiusKm > 0`.
- Response contract: `List<Dto>` đồng nhất với chuẩn Phase 2.

#### **TSK-19: Administrative Ward Filter**
- Cả 3 controller hỗ trợ `?wardCode=` kết hợp phân trang `Pageable`. Riêng OCOP hỗ trợ tìm kiếm tên sản phẩm qua `?q=`.

---

### 🔵 PHẦN 2: FRONTEND (REACT 19 + LEAFLET MARKERCLUSTER)

#### **TSK-20 & TSK-21: PoiMarkerClusterLayer**
- Sử dụng trực tiếp `leaflet.markercluster` qua `useMap()` React hook.
- Phân biệt màu sắc trực quan:
  - OCOP: Cam ấm `#F97316`
  - Science: Xám Slate `#64748B`
  - Agriculture: Xám Cool `#6B7280`
- Quản lý vòng đời chặt chẽ (Lifecycle Safety): `clusterGroup.clearLayers()` và `map.removeLayer(clusterGroup)` trong cleanup hook, đảm bảo 0 marker duplicate, 0 memory leak.
- Lazy Detail Loading: Popup hiển thị thông tin tóm tắt; click `[Xem chi tiết]` gọi API `GET /api/{feature}/{id}` để hiển thị đầy đủ trên `DetailsPanel.tsx`.

#### **TSK-22: Single Source of Truth Layer Control**
- Quản lý tập trung toàn bộ layers tại `Home.tsx` (`layers` state) và điều khiển duy nhất qua `SidebarDrawer.tsx`.
- Hiển thị chấm màu Legend bên cạnh tên lớp dữ liệu.

#### **TSK-23: Interactive Map Coordinate Picker**
- `MapPicker.tsx` cho phép click chọn tọa độ trực tiếp, đồng bộ 2 chiều với các ô nhập `latitude` và `longitude`.

#### **TSK-24: Spatial Radius Search Control**
- `RadiusSearchControl.tsx` cho phép chọn tâm điểm trên bản đồ, kéo bán kính (1 - 100 km), chọn chuyên đề và kích hoạt tìm kiếm.
- Vẽ vòng tròn bán kính đứt nét trên bản đồ và hiển thị hiệu ứng phát sáng (pulse) quanh các marker kết quả.

#### **TSK-25: Dashboard & Thống kê**
- `StatsBoard.tsx` tổng hợp số lượng xã/phường, diện tích và số lượng cơ sở chuyên đề.

---

## 3. Definition of Done — Giai đoạn 3

- [x] Cả 3 endpoint `/api/{feature}/geojson` trả về GeoJSON Point FeatureCollection hợp lệ.
- [x] Tìm kiếm bán kính `ST_DWithin` hoạt động chính xác với index GiST `idx_*_geog`.
- [x] Cả 3 layer điểm (OCOP, Science, Agriculture) hiển thị chuẩn màu theo `Design_rule.md`, clustering mượt mà khi zoom xa/gần.
- [x] Legend hiển thị đúng màu sắc đối chiếu trực quan.
- [x] Map picker hoạt động trơn tru trong modal thêm/sửa đơn vị cho cả 3 module.
- [x] Backend tests (`.\mvnw test`) và Frontend build (`pnpm run build` + `pnpm run lint`) hoàn tất thành công với 0 lỗi.
