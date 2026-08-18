# KẾ HOẠCH CHI TIẾT - GIAI ĐOẠN 3: TÍCH HỢP BẢN ĐỒ GIS (GIS MAP INTEGRATION)

## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!IMPORTANT]
> **Trạng thái tài liệu (Soạn ngày 2026-08-17, Cập nhật chuẩn hoá): Kế hoạch DỰ KIẾN, CHƯA triển khai. Phụ thuộc vào Giai đoạn 2 hoàn thành (`docs/Phase2-task.md`).**
>
> Cả 3 module (`ocop`, `science`, `agriculture`) đều đã được chuẩn hoá là dạng Điểm (**Point layer** — `geometry(Point, 4326)`). Bảng màu chính thức đã chốt trong `docs/UI-UX/Design_rule.md` (OCOP: `#F97316` Cam, Science: `#64748B` Xám Slate, Agriculture: `#6B7280` Xám Cool).

> [!TIP]
> **Triết lý Giai đoạn 3:** Giai đoạn 2 xây "sổ đăng ký" CRUD. Giai đoạn 3 **chỉ thêm lớp hiển thị bản đồ + clustering + popup + truy vấn không gian lên trên dữ liệu đã có** — không viết lại CRUD, tái sử dụng hạ tầng Point layer nhất quán cho cả 3 module.

---

## 1. Phạm vi Giai đoạn 3

| Thuộc phạm vi | Không thuộc phạm vi |
| :--- | :--- |
| Endpoint GeoJSON (Point FeatureCollection) cho cả 3 module | CRUD cơ bản của cả 3 module (thuộc Giai đoạn 2) |
| Vẽ marker layer lên bản đồ chính, hỗ trợ clustering (OCOP: Cam, Science/Agriculture: Xám) | Module Resource/Media (thuộc Giai đoạn 2) |
| Popup thông tin cơ sở/đơn vị khi click marker | Chuyển sang MinIO/S3 (giữ local storage đơn giản) |
| Tìm kiếm bán kính (`ST_DWithin`), lọc theo xã | Tự động hoá multi-instance fleet (Dokploy/Coolify) |
| Map picker chọn toạ độ bằng click trên modal form | |
| Dashboard/Analytics thống kê + xuất PDF/Excel | |

---

### 🔴 PHẦN 1: BACKEND (SPRING BOOT)

#### **TSK-17: Endpoint GeoJSON cho OCOP / Science / Agriculture**

- **Việc cần làm:** Theo đúng quy ước tại `API_CONTRACT.md` mục 4.4: `GET /api/{feature}/geojson` trả về `FeatureCollection` các Điểm (Point):
  - Cả 3 module (`ocop`, `science`, `agriculture`) đều có `geometry.type = "Point"`, toạ độ `[longitude, latitude]`.
  - Properties chứa: `id`, `name`, `unitType` (hoặc `productType`), `wardCode`, `imageUrl`.
  - Dựng GeoJSON bằng Jackson `ObjectMapper` (`ObjectNode`/`ArrayNode`) tương tự như cách `WardController.getAllWardsGeoJson()` đang làm.
  - Áp dụng cache header private và `@ConditionalOnProperty`.
- **Cách verify:** Gọi API `/api/{feature}/geojson` và tải dữ liệu lên [geojson.io] để kiểm tra toạ độ hiển thị đúng trong địa phận Gia Lai.

#### **TSK-18: Truy vấn tìm kiếm bán kính (Radius Search)**

- **Việc cần làm:** `GET /api/{feature}/nearby?lat={lat}&lng={lng}&radiusKm={radiusKm}`:
  - Sử dụng PostGIS `ST_DWithin` trên cột `geom` với cast `::geography` để tính khoảng cách chính xác theo mét:
    `ST_DWithin(geom::geography, ST_MakePoint(:lng, :lat)::geography, :radiusKm * 1000)`
  - Tận dụng index GiST `idx_{table}_geom` đã tạo từ Giai đoạn 2.
- **Cách verify:** Test truy vấn với toạ độ Pleiku, bán kính tăng dần và xác nhận số lượng bản ghi trả về tăng tương ứng.

#### **TSK-19: Lọc theo vùng hành chính (Administrative Area Filter)**

- **Việc cần làm:** Thêm query param `?wardCode=` vào endpoint danh sách `GET /api/{feature}` đã có từ Giai đoạn 2.
- **Cách verify:** `GET /api/ocop?wardCode=21112` chỉ trả về các sản phẩm thuộc đúng xã đó.

---

### 🔵 PHẦN 2: FRONTEND (REACT LEAFLET)

#### **TSK-20: Layer điểm OCOP trên bản đồ chính (có clustering)**

- **Việc cần làm:**
  - Thêm thư viện clustering cho React Leaflet (ví dụ `react-leaflet-cluster` hoặc `leaflet.markercluster`).
  - Tạo `FE/src/pages/home/components/OcopMarkers.tsx`, gọi `GET /api/ocop/geojson` qua TanStack Query.
  - Tuân thủ `Design_rule.md`:
    - Zoom xa: Gộp cụm điểm, hiển thị số lượng `(N)`.
    - Zoom gần: Rã cụm, hiển thị chấm tròn màu Cam ấm (`#F97316`), viền trắng (Halo effect).
    - Click điểm: Hiển thị popup với tên cơ sở, địa chỉ, SĐT, thuộc xã nào, nút `[Xem chi tiết]`.
  - Tích hợp vào `GisMap.tsx` bọc trong điều kiện `FEATURE_FLAGS.ocop`.

#### **TSK-21: Layer điểm Science & Agriculture trên bản đồ chính**

- **Việc cần làm:**
  - Tạo `ScienceMarkers.tsx` với màu xám Slate `#64748B` (`FEATURE_FLAGS.science`).
  - Tạo `AgricultureMarkers.tsx` với màu xám Cool Gray `#6B7280` (`FEATURE_FLAGS.agriculture`).
  - Cả 2 module đều dùng chung cơ chế clustering, unclustering, halo effect và popup như OCOP.

#### **TSK-22: Sidebar điều khiển lớp dữ liệu (Layer Control) + Legend**

- **Việc cần làm:**
  - Sidebar hiển thị checkbox bật/tắt từng layer chuyên đề khi module đó được kích hoạt qua feature flag.
  - Hiển thị chấm tròn màu trực quan ngay cạnh tên layer: OCOP (Cam `#F97316`), Science (Xám `#64748B`), Agriculture (Xám `#6B7280`).

#### **TSK-23: Map picker chọn toạ độ bằng click (nâng cấp form nhập liệu)**

- **Việc cần làm:**
  - Trong modal thêm/sửa của OCOP, Science, Agriculture: Tích hợp bản đồ nhỏ cho phép click chuột để ghim vị trí, tự động điền giá trị vào 2 ô số `latitude` và `longitude`.

#### **TSK-24: UI Tìm kiếm bán kính & Lọc không gian**

- **Việc cần làm:**
  - Thêm công cụ chọn bán kính (km) và tâm tìm kiếm trên bản đồ, gọi API `/api/{feature}/nearby` và highlight các điểm kết quả.

#### **TSK-25: Dashboard & Analytics**

- **Việc cần làm:**
  - Thống kê số lượng đơn vị theo từng module (OCOP, Science, Agriculture) phân bố theo xã/phường.
  - Hỗ trợ xuất dữ liệu ra Excel (`xlsx`) hoặc in báo cáo PDF.

---

## 2. Mô hình Vận hành & Triển khai 3 Deployment

- **1 VPS — 3 Stacks — 3 Database:**
  - Máy chủ VPS Viettel IDC chạy 3 container app độc lập: OCOP (`ocop.gialai.gov.vn`), Science (`khcn.gialai.gov.vn`), Agriculture (`nongnghiep.gialai.gov.vn`).
  - Mỗi container kết nối tới database riêng (`gialai_ocop`, `gialai_science`, `gialai_agriculture`).
  - Dùng chung ranh giới các xã (`wards`, `gis_wards`). Caddy xử lý reverse proxy và cấp phát chứng chỉ SSL tự động.

---

## 3. Definition of Done — Giai đoạn 3

- [ ] Cả 3 endpoint `/api/{feature}/geojson` trả về GeoJSON Point FeatureCollection hợp lệ.
- [ ] Tìm kiếm bán kính `ST_DWithin` hoạt động chính xác với index GiST.
- [ ] Cả 3 layer điểm (OCOP, Science, Agriculture) hiển thị chuẩn màu theo `Design_rule.md`, clustering mượt mà khi zoom xa/gần.
- [ ] Legend hiển thị đúng màu sắc đối chiếu trực quan.
- [ ] Map picker hoạt động trơn tru trong modal thêm/sửa đơn vị cho cả 3 module.
- [ ] `./mvnw -B verify` và `docker compose up -d --build` hoàn tất thành công khi bật đồng thời các module.
