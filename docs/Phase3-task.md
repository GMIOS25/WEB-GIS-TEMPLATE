# KẾ HOẠCH CHI TIẾT - GIAI ĐOẠN 3: TÍCH HỢP BẢN ĐỒ GIS (GIS MAP INTEGRATION)

## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!IMPORTANT]
> **Trạng thái tài liệu (Soạn ngày 2026-08-17): Kế hoạch DỰ KIẾN, CHƯA triển khai. Phụ thuộc cứng vào Giai đoạn 2 đã xong (`docs/Phase 2/Phase2-task.md`).**
>
> Phạm vi rút từ `docs/en/PROJECT_OVERVIEW.md` mục 2, dòng **Phase 3 — "GIS Map Integration"**, đối chiếu mục 4.6 ("Advanced GIS Map Module (Phase 3)"), `ARCHITECTURE SPECIFICATION.md` mục 3.3/6.4, `API_CONTRACT.md` mục 4.4, và `docs/UI-UX/Design_rule.md`.
>
> **KHÔNG bắt đầu Giai đoạn 3 nếu Giai đoạn 2 chưa đạt Definition of Done** (mục 4 của `Phase2-task.md`) — mọi task dưới đây giả định OCOP/Science/Agriculture đã có bảng CRUD hoạt động, endpoint `/api/{feature}` (list/detail/create/update/delete) đã chạy, và ít nhất OCOP + Science đã có cột `geom`/toạ độ trong DB từ Giai đoạn 2.

> [!TIP]
> **Triết lý Giai đoạn 3:** Giai đoạn 2 xây "sổ đăng ký". Giai đoạn 3 **chỉ thêm lớp hiển thị + truy vấn không gian lên trên dữ liệu đã có** — không viết lại CRUD, không đổi schema bảng nghiệp vụ đã tạo ở Giai đoạn 2 (chỉ **thêm** cột/bảng mới bằng migration forward-only, không sửa migration cũ).

---

## 1. Phạm vi Giai đoạn 3

| Thuộc phạm vi | Không thuộc phạm vi (đã xong ở Giai đoạn 2, hoặc dời xa hơn) |
| :--- | :--- |
| Thêm hình học (geometry) cho Agriculture | CRUD cơ bản của cả 3 module (đã xong Giai đoạn 2) |
| Endpoint GeoJSON cho cả 3 module | Module Resource/Media (đã xong Giai đoạn 2) |
| Vẽ marker/polygon lên bản đồ chính, có clustering | Tự động hoá fleet nhiều VPS thật sự (vẫn deferred — xem mục 8 bên dưới) |
| Tìm kiếm bán kính, lọc theo vùng hành chính | Chuyển sang MinIO/S3 cho file storage (chỉ là "future extension" theo `PROJECT_OVERVIEW.md` mục 3.4, không có tín hiệu nào yêu cầu làm ở Giai đoạn 3) |
| Dashboard/Analytics + xuất PDF/Excel | |
| Map picker chọn toạ độ bằng click (nâng cấp form nhập tay ở Giai đoạn 2) | |

---

### 🔴 PHẦN 1: BACKEND (SPRING BOOT)

#### **TSK-17: Thêm hình học cho Agriculture (migration forward-only mới, KHÔNG sửa `V1`)**

- **Vì sao cần làm trước tiên trong Giai đoạn 3:** Agriculture kết thúc Giai đoạn 2 mà **không có** cột hình học (quyết định có chủ đích, xem `Phase2-task.md` TSK-11). Mọi task GeoJSON/marker layer cho Agriculture đều phụ thuộc bảng này tồn tại trước.
- **Việc cần làm:** theo đúng khuôn "split" đã mô tả ở `DATA_MODEL.md` mục 4.2 (đã hợp lệ hoá ở Giai đoạn 2 vì lúc đó Agriculture **đã thật sự cần** listing phi không gian riêng — đúng điều kiện *"Pick the split only if a real non-spatial listing requirement exists"*):
  1. Tạo file **mới** `BE/src/main/resources/db/migration/agriculture/V2__create_gis_agriculture_zones.sql` (số `V2`, nối tiếp `V1` đã có từ Giai đoạn 2 — **tuyệt đối không sửa lại `V1__create_agriculture_zones.sql`**, đúng nguyên tắc Flyway forward-only đã nhắc ở `Phase2-task.md` TSK-11):
     ```sql
     CREATE TABLE gis_agriculture_zones (
         id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
         zone_id integer NOT NULL,
         area_km2 numeric(12, 5),
         geom geometry(MultiPolygon, 4326) NOT NULL,
         PRIMARY KEY (id),
         CONSTRAINT gis_agriculture_zones_zone_id_fkey FOREIGN KEY (zone_id) REFERENCES agriculture_zones (id)
     );
     CREATE INDEX idx_gis_agriculture_zones_geom ON public.gis_agriculture_zones USING gist (geom);
     ```
  2. Package `com.website.gis.features.agriculture`: thêm entity `GisAgricultureZone`, repository `GisAgricultureZoneRepository` (theo đúng pattern `GisWardRepository` đã có — dùng native query `ST_AsGeoJSON` thay vì map `geom` trực tiếp qua field entity kiểu JTS, giữ nhất quán kỹ thuật với cách `core` đang xử lý).
  3. Việc **nhập** polygon cho từng zone thuộc TSK-26 (map picker) — TSK-17 chỉ tạo hạ tầng lưu trữ, chưa có UI nhập liệu.
- **Cách verify:** `./mvnw -B verify` chạy sạch, xác nhận Flyway áp cả `V1` lẫn `V2` theo đúng thứ tự trên DB Testcontainers PostGIS thật (không phải giả lập).

#### **TSK-18: Endpoint GeoJSON cho OCOP / Science / Agriculture**

- **Việc cần làm:** theo đúng quy ước đã ghi sẵn ở `API_CONTRACT.md` mục 4.4: `GET /api/{feature}/geojson` trả `FeatureCollection`, cùng shape với `/api/wards/geojson` đã có (tái dùng cách dựng `ObjectNode`/`ArrayNode` qua Jackson `ObjectMapper` trong `WardController.getAllWardsGeoJson()` làm mẫu — **không** dựng chuỗi JSON bằng tay, bài học này đã ghi rõ trong chính comment của `WardController` hiện tại).
  - OCOP/Science: mỗi feature có `geometry.type = "Point"`.
  - Agriculture: mỗi feature có `geometry.type = "MultiPolygon"`, lấy từ `gis_agriculture_zones` (TSK-17), join `agriculture_zones` để lấy `properties` (tên, loại...).
  - Cache header: áp dụng `cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())` giống các endpoint geojson hiện có của `wards` — lý do cache private đã giải thích trong comment gốc (endpoint đứng sau `authenticated()`, không nên đánh dấu public dù rủi ro rò rỉ thấp).
  - `@ConditionalOnProperty` vẫn áp dụng — flag tắt thì endpoint 404, giống mọi endpoint khác của module.
- **Cách verify:** load thử response geojson của OCOP vào [geojson.io] (hoặc bất kỳ trình xem GeoJSON nào) để xác nhận toạ độ hợp lệ, nằm trong ranh giới Gia Lai — sai dấu lat/lng (đảo ngược) là lỗi rất dễ mắc khi chuyển đổi giữa `latitude, longitude` (thứ tự thường dùng khi nói) và GeoJSON (`[longitude, latitude]` — GeoJSON luôn kinh độ trước).

#### **TSK-19: Truy vấn tìm kiếm bán kính (Radius Search)**

- **Việc cần làm:** `GET /api/ocop/nearby?lat={lat}&lng={lng}&radiusKm={radiusKm}` (và tương tự cho `science`) dùng PostGIS `ST_DWithin` trên cột `geom` (đơn vị mét, cần `ST_Transform`/dùng `geography` cast cho tính khoảng cách chính xác trên mặt cầu thay vì mặt phẳng — ví dụ `ST_DWithin(geom::geography, ST_MakePoint(:lng, :lat)::geography, :radiusKm * 1000)`), có index GiST sẵn từ Giai đoạn 2 (`idx_ocop_products_geom`) nên hiệu năng tốt ngay từ đầu.
  - Với Agriculture (polygon), "bán kính" nghĩa là "vùng nào giao với vòng tròn bán kính đó" — dùng `ST_Intersects` thay vì `ST_DWithin`.
- **Cách verify:** test với toạ độ trung tâm Pleiku, bán kính nhỏ dần, xác nhận số kết quả giảm đơn điệu (không tăng khi bán kính giảm — bug logic kinh điển nếu lỡ đảo ngược điều kiện).

#### **TSK-20: Lọc theo vùng hành chính (Administrative Area Filter)**

- **Việc cần làm:** đã có sẵn `wardCode` trên mọi bảng module (`ward_code` FK) — chỉ cần thêm query param `?wardCode=` vào endpoint `GET /api/{feature}` đã có từ Giai đoạn 2 (không cần endpoint mới, không cần PostGIS — đây là lọc quan hệ thường, không phải spatial query). Việc này đơn giản hơn nhiều so với TSK-19, làm trước để có kết quả nhanh.
- **Cách verify:** `GET /api/ocop?wardCode=21112` chỉ trả sản phẩm thuộc đúng xã đó.

---

### 🔵 PHẦN 2: FRONTEND (REACT LEAFLET)

> [!WARNING]
> Trước khi code bất kỳ UI nào ở Phần 2, đọc kỹ toàn bộ `docs/UI-UX/Design_rule.md` — tài liệu này **bắt buộc**, không phải tham khảo. Lưu ý: `Design_rule.md` **chỉ định nghĩa màu cho OCOP** (`#F97316` light / `#FACC15` dark). Science và Agriculture **chưa có màu chính thức** — xem TSK-21b trước khi code Science/Agriculture để tránh tự chọn màu tuỳ tiện.

#### **TSK-21: Layer điểm OCOP trên bản đồ chính (có clustering)**

- **Việc cần làm:**
  - Thêm dependency clustering cho `react-leaflet` (ví dụ `react-leaflet-cluster` hoặc `leaflet.markercluster` — kiểm tra và chốt version tương thích với version `leaflet`/`react-leaflet` đang cài trong `FE/package.json` lúc thực thi, **không giả định sẵn version** vì có thể đã đổi từ lúc tài liệu này soạn tới lúc code).
  - Tạo `FE/src/pages/home/components/OcopMarkers.tsx` (đặt cùng cấp `GisMap.tsx`, theo đúng cấu trúc co-location `CODING_CONVENTIONS.md` mục 5.1), dùng `useQuery` + `ocopKeys.geojson()` (đã khai báo khung ở `Phase2-task.md` TSK-12) gọi `GET /api/ocop/geojson`.
  - **Bắt buộc đúng theo `Design_rule.md` mục 3:**
    - Zoom xa (mức tỉnh): gộp cụm, hiển thị số lượng trong vòng tròn (ví dụ `(50)`).
    - Zoom gần (mức huyện/xã): rã cụm, hiển thị chấm tròn cam đơn lẻ, có viền trắng (halo effect).
    - Click vào điểm: hiệu ứng ripple/phóng to nhẹ, mở popup đúng cấu trúc mục 3 (Tiêu đề bold = tên cơ sở, nội dung = địa chỉ/SĐT/thuộc xã nào, nút `[Xem chi tiết]`).
  - Import `GisMap.tsx`, thêm `<LayersControl.Overlay name="OCOP">` bọc `<OcopMarkers />`, chỉ render khi `FEATURE_FLAGS.ocop` — theo đúng mẫu đã có sẵn ở `ARCHITECTURE SPECIFICATION.md` mục 3.3.
- **Cách verify:** so trực quan với bảng màu `Design_rule.md` (dùng công cụ đo màu trên trình duyệt, không ước lượng bằng mắt). Zoom test thủ công qua các mức để xác nhận cluster/uncluster đúng ngưỡng.

#### **TSK-21b: [QUYẾT ĐỊNH CẦN CHỐT] Bổ sung màu cho Science và Agriculture vào `Design_rule.md`**

> [!WARNING]
> Đây là việc **cập nhật tài liệu thiết kế**, không phải tự quyết định trong code. Đề xuất dưới đây là gợi ý dựa trên nguyên tắc đã nêu ở `Design_rule.md` mục 2 (*"Không trùng màu giữa UI hệ thống và dữ liệu chuyên đề trên bản đồ"* + không trùng với OCOP `#F97316`/xanh ranh giới xã `#10b981`/`#059669`) — **cần người phụ trách thiết kế xác nhận trước khi code TSK-22/TSK-23**, không mặc định đúng như OCOP đã được chính thức hoá.

- **Đề xuất Science (point):** `#3B82F6` (xanh dương) light theme — tương phản tốt với cam OCOP và xanh lá ranh giới, gợi liên tưởng "công nghệ" theo quy ước màu phổ biến.
- **Đề xuất Agriculture (polygon fill):** `#CA8A04` (vàng đất/amber đậm), opacity `0.25`, viền `#92400E` — tránh trùng sắc lục của ward-selected-fill (`#a7f3d0`/`#059669`) và tránh trùng cam OCOP, gợi liên tưởng "đất nông nghiệp".
- **Việc cần làm:** thêm 2 dòng vào bảng màu mục 2 của `Design_rule.md` (theo đúng format bảng đang có), rồi mới code TSK-22/TSK-23 dùng đúng màu đã chốt trong tài liệu — **không** hard-code màu thẳng vào component mà không cập nhật tài liệu trước, để tránh lặp lại đúng kiểu lệch tài liệu/code đã gặp ở Giai đoạn 1/2.

#### **TSK-22: Layer điểm Science trên bản đồ chính**

- Nhân bản TSK-21, dùng màu đã chốt ở TSK-21b cho Science. Science không có yêu cầu clustering khác biệt trong `Design_rule.md` — áp dụng logic zoom/cluster giống hệt OCOP trừ màu sắc.

#### **TSK-23: Layer vùng Agriculture trên bản đồ chính (polygon, không phải marker)**

- **Khác biệt kỹ thuật quan trọng so với TSK-21/22:** đây là `<GeoJSON>` polygon overlay (giống cách `GisMap.tsx` hiện đang vẽ ranh giới xã), **không phải** marker/cluster. Copy cách `GisMap.tsx` xử lý style/hover/click cho polygon ranh giới xã hiện tại làm khung, đổi màu theo TSK-21b, đổi nguồn dữ liệu sang `GET /api/agriculture/geojson` (TSK-18).
- **Cách verify:** xác nhận polygon Agriculture và polygon ranh giới xã **không** style giống hệt nhau đến mức người dùng nhầm lẫn 2 lớp dữ liệu (đây chính là nguyên tắc sống còn ở `Design_rule.md` mục 2).

#### **TSK-24: Sidebar điều khiển lớp dữ liệu (Layer Control) + Legend**

- Theo đúng `Design_rule.md` mục 4: checkbox/switch bật-tắt từng layer, **bắt buộc** có chấm màu/icon nhỏ cạnh mỗi nhãn khớp đúng màu layer đó (không bắt người dùng mở bảng chú giải riêng). Chỉ hiển thị toggle cho layer mà `FEATURE_FLAGS.<module>` đang bật — layer bị tắt hoàn toàn ở Giai đoạn 2 (Gia Lai) thì Sidebar không hiện gì cả, không hiện dạng "disabled/xám mờ".

#### **TSK-25: Map picker chọn toạ độ bằng click (nâng cấp form Giai đoạn 2)**

- **Việc cần làm:** trong `OcopFormModal.tsx`/`ScienceFormModal.tsx` (đã tạo ở Giai đoạn 2 với 2 ô số lat/lng), thêm 1 bản đồ Leaflet thu nhỏ trong modal, click để đặt marker, tự động điền ngược lại 2 ô số (giữ 2 ô số làm phương án dự phòng/hiệu chỉnh tay, không xoá hẳn — đôi khi cần nhập toạ độ chính xác từ nguồn ngoài).
- **Với Agriculture:** cần công cụ vẽ polygon (ví dụ `react-leaflet-draw` hoặc tương đương) — đây là phần việc **mới hoàn toàn**, không phải nâng cấp từ Giai đoạn 2 (Agriculture Giai đoạn 2 không có form toạ độ nào để nâng cấp). Ưu tiên làm sau khi TSK-21/22 (điểm) đã ổn định, vì vẽ polygon phức tạp hơn đặt 1 điểm.

#### **TSK-26: UI Tìm kiếm bán kính**

- Ô nhập bán kính (km) + chọn tâm bằng click lên bản đồ hoặc dùng vị trí xã đang chọn làm tâm, gọi `GET /api/ocop/nearby` (TSK-19), highlight kết quả, tái dùng `MapSearch.tsx` hiện có làm tham chiếu về UX (flyTo khi chọn kết quả).

#### **TSK-27: Dashboard & Analytics**

- Theo `PROJECT_OVERVIEW.md` mục 4.5: đếm số đơn vị theo từng loại (OCOP/Science/Agriculture), phân bố theo xã, xuất PDF/Excel. Đặt tại `StatsBoard.tsx` hiện có (mở rộng, không tạo trang riêng) hoặc 1 trang Dashboard mới nếu nội dung quá nhiều cho panel nhỏ hiện tại — quyết định bố cục cụ thể để lúc code tuỳ theo khối lượng dữ liệu thực tế, không cố định trước.
- Xuất Excel: dùng `xlsx`/SheetJS (đã có sẵn trong danh sách thư viện được phép dùng cho artifact của dự án — kiểm tra lại `FE/package.json` xem đã cài chưa, nếu chưa thì thêm). Xuất PDF: cân nhắc render phía server (Spring Boot, dùng thư viện Java tạo PDF) thay vì phía client để đảm bảo định dạng nhất quán khi in — quyết định server-side vs client-side PDF cần chốt trước khi code, ảnh hưởng cả 2 phía BE/FE.

---

## 8. [QUYẾT ĐỊNH CẦN CHỐT] Có tính là "trigger" cho Fleet/PaaS hay không?

> [!WARNING]
> Đây là quyết định vận hành, **không** phải việc agent code tự quyết.

`architect_deploy.mermaid` (đã có trong repo) vẽ **3 deployment tách biệt** cho cùng khách hàng Gia Lai — `ocop.gialai.gov.vn`, `khcn.gialai.gov.vn`, `nongnghiep.gialai.gov.vn` — mỗi cái 1 container + 1 DB riêng (`AppOcop`→`DbOcop`, v.v.), y hệt cấu trúc "1 customer = 1 stack" mô tả ở `ARCHITECTURE SPECIFICATION.md` mục 6.1. Nhưng `ARCHITECTURE SPECIFICATION.md` mục 7.2 định nghĩa trigger để chuyển sang PaaS (Dokploy/Coolify) là *"khi có **khách hàng thứ hai**"* — còn 3 deployment trên vẫn là **cùng một khách hàng** (Gia Lai), chỉ tách theo phòng ban/module.

Về mặt vận hành, việc chạy đồng thời 3 stack độc lập **có độ phức tạp giống hệt** việc có 3 khách hàng riêng biệt (`DEPLOYMENT & FLEET STRATEGY.md` mục 1 mô tả mô hình hiện tại là "one Compose stack" — 3 lần lặp lại thủ công 3 stack riêng đã vượt quá mô hình đó). 2 hướng khả dĩ:

1. **Giữ đúng chữ nghĩa mục 7.2** (trigger = khách hàng thứ 2 thật sự, chưa xảy ra) → tiếp tục vận hành thủ công 3 `docker compose` stack riêng trên cùng 1 VPS (mỗi stack ở 1 thư mục khác nhau, 1 file `.env` khác nhau, bật đúng 1 feature flag mỗi nơi), theo đúng runbook thủ công đã có ở `DEPLOYMENT & FLEET STRATEGY.md` mục 5, nhân 3.
2. **Coi độ phức tạp vận hành thực tế là trigger** (bỏ qua định nghĩa "khách hàng" theo hợp đồng, tính theo số stack độc lập cùng chạy) → áp dụng Dokploy/Coolify ngay từ khi triển khai 3 subdomain này, theo đúng kế hoạch đã có sẵn ở `ARCHITECTURE SPECIFICATION.md` mục 7.3–7.4 và `DEPLOYMENT & FLEET STRATEGY.md` mục 6.2–6.3 (chọn 1 trong 2 công cụ, mỗi subdomain = 1 project trong PaaS).

Khuyến nghị nghiêng về hướng 2 (áp dụng PaaS ngay), vì độ phức tạp vận hành là thứ mục 7 ban đầu thực sự lo ngại — nhưng đây là **khuyến nghị**, không phải quyết định thay chủ dự án. Ghi quyết định cuối cùng lại vào `ARCHITECTURE SPECIFICATION.md` mục 7 (thêm 1 "Status note" mới, đúng phong cách các note trạng thái đã có trong tài liệu) trước khi agent triển khai hạ tầng thật cho 3 subdomain.

---

## 9. Definition of Done — Giai đoạn 3

- [ ] TSK-17: `gis_agriculture_zones` tồn tại, `V1`/`V2` của module `agriculture` cả hai đều áp dụng sạch trên DB Testcontainers thật.
- [ ] TSK-18: cả 3 endpoint `/api/{ocop,science,agriculture}/geojson` trả đúng shape, load được trong công cụ xem GeoJSON ngoài, toạ độ không bị đảo ngược.
- [ ] TSK-19/20: tìm kiếm bán kính trả kết quả đơn điệu theo bán kính; lọc theo xã chính xác.
- [ ] TSK-21b: `Design_rule.md` đã có màu chính thức cho Science/Agriculture **trước khi** TSK-22/23 code xong (không phải làm song song rồi đối chiếu ngược).
- [ ] TSK-21–24: cả 3 layer hiện/ẩn đúng theo feature flag, đúng màu đã chốt, clustering đúng ngưỡng zoom, legend khớp màu thật trên bản đồ.
- [ ] TSK-25: tạo mới 1 OCOP bằng cách click bản đồ, xác nhận toạ độ lưu DB khớp đúng điểm đã click (không lệch do đảo lat/lng).
- [ ] TSK-27: xuất thử 1 file Excel/PDF thật, mở file xác nhận số liệu khớp dữ liệu trong DB tại thời điểm xuất.
- [ ] Mục 8 đã có quyết định ghi lại bằng văn bản trong `ARCHITECTURE SPECIFICATION.md`, không còn bỏ ngỏ khi bắt đầu triển khai hạ tầng 3 subdomain thật.
- [ ] `./mvnw -B verify` + `docker compose up -d --build` chạy sạch trên máy sạch một lần cuối, với **toàn bộ 3 feature flag bật đồng thời** (kịch bản chưa từng test ở Giai đoạn 2, nơi mỗi lần chỉ test bật 1 flag) — xác nhận 3 module cùng hoạt động không xung đột route/bean/migration.
