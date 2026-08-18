# KẾ HOẠCH CHI TIẾT - GIAI ĐOẠN 2: QUẢN LÝ ĐƠN VỊ TRỰC THUỘC (AFFILIATED UNIT MANAGEMENT)

## DỰ ÁN: HỆ THỐNG QUẢN LÝ VÀ TRA CỨU THÔNG TIN HÀNH CHÍNH TỈNH GIA LAI

---

> [!IMPORTANT]
> **Trạng thái tài liệu (Soạn ngày 2026-08-17): Đây là kế hoạch DỰ KIẾN (prospective), CHƯA triển khai.**
>
> Tài liệu này được soạn bởi AI (Claude) theo yêu cầu chủ dự án, dựa trên đối chiếu **7 tài liệu** đang có trong `docs/en/` + `docs/UI-UX/` với codebase thật tại commit `2f8eb24` (Giai đoạn 1 đã 100% xong, verify thực nghiệm bằng `./mvnw -B verify` + `docker compose up -d --build`). Phạm vi Giai đoạn 2 dưới đây **không phải suy đoán tuỳ tiện** — nó được rút thẳng từ bảng roadmap chính thức tại `docs/en/PROJECT_OVERVIEW.md` mục 2 ("Affiliated Unit Management"), đối chiếu chéo với `ARCHITECTURE SPECIFICATION.md`, `DATA_MODEL.md` mục 4, `API_CONTRACT.md` mục 4.4, và comment sẵn có trong chính `DynamicFlywayConfig.java` (đã tự ghi chú "Giai đoạn 2 - roadmap").
>
> **Agent đọc file này cần đọc kèm (không đọc thì sẽ implement sai):**
>
> - `docs/en/ARCHITECTURE SPECIFICATION.md` mục 2–6 — cơ chế Compile-time Modularity (feature flag → package → Flyway folder).
> - `docs/en/DATA_MODEL.md` mục 4 — quy ước schema cho bảng module mới.
> - `docs/en/API_CONTRACT.md` mục 4.4 — quy ước endpoint cho module mới.
> - `docs/en/CODING_CONVENTIONS.md` mục 1, 3, 5, 6 — naming, MapStruct, cấu trúc thư mục FE.
> - `docs/UI-UX/Design_rule.md` — bắt buộc cho mọi UI mới.
>
> **Việc PHẢI làm trước khi viết bất kỳ dòng code nào:** đọc mục 0 và mục 1 bên dưới. Có 1 việc sửa file bắt buộc (TSK-6) phải làm **trước tiên**, và 1 quyết định (mục 1.2) chủ dự án cần chốt trước khi agent code module thứ 3.

> [!TIP]
> **Triết lý Giai đoạn 2:** Giai đoạn 1 chứng minh nền tảng hành chính hoạt động đúng. Giai đoạn 2 chứng minh **kiến trúc modularity đã thiết kế trong Giai đoạn 1 thực sự "cắm là chạy"** — bằng cách xây module thật đầu tiên (OCOP) làm mẫu tham chiếu, rồi nhân bản cho 2 module còn lại. Giai đoạn 2 **CHƯA vẽ điểm/vùng lên bản đồ** (việc đó dời sang Giai đoạn 3 theo đúng bảng roadmap ở `PROJECT_OVERVIEW.md`) — Giai đoạn 2 chỉ xây "sổ đăng ký" (registry) CRUD + upload tài liệu/ảnh cho từng đơn vị trực thuộc.

---

## 0. Việc bắt buộc làm trước tiên: Dọn mâu thuẫn đặt tên module thứ 3

### TSK-6: Chuẩn hoá tên module `agriculture` xuyên suốt tài liệu — 🔴 Bắt buộc, làm đầu tiên

- **Vấn đề phát hiện được:** tài liệu hiện tại dùng **3 tên khác nhau** cho cùng một module (nông nghiệp):
  | Nguồn | Tên dùng |
  | :--- | :--- |
  | `.env.example`, `application.properties.example`, `DynamicFlywayConfig.java` (code THẬT, đã chạy, đã verify) | `agriculture` |
  | `Phase1-task.md` (hồ sơ nghiệm thu Giai đoạn 1) | `agriculture` |
  | `ARCHITECTURE SPECIFICATION.md` mục 4.3, 5.2 (code mẫu) | `agriculture` |
  | `ARCHITECTURE SPECIFICATION.md` mục 6.4 (đoạn văn xuôi) | `nonglam` |
  | `DATA_MODEL.md` mục 1, 4 (toàn bộ) | `nonglam` |
  | `CODING_CONVENTIONS.md` mục 6 | `nonglam` (`NonglamController`, `NongLamZone`) |
  | `architect_deploy.mermaid` (tên subdomain) | `nongnghiep` (`nongnghiep.gialai.gov.vn`) |
  | `DEPLOYMENT & FLEET STRATEGY.md` mục 6.3 | `ENABLE_AGRICULTURE` (tên biến env đã lỗi thời — xem cảnh báo ngay trong mục 4.1 của chính file đó) |

- **Quyết định (đã chọn, không phải để bàn thêm):** dùng **`agriculture`** làm tên canonical duy nhất. Lý do: đây là tên đã **THẬT SỰ CHẠY** trong code đã qua `./mvnw -B verify` và `docker compose up -d --build` thành công — đổi tên lúc này nghĩa là sửa lại hạ tầng đã verify, rủi ro cao hơn nhiều so với sửa vài dòng doc. `agriculture` cũng là tiếng Anh thuần, nhất quán với `ocop`/`science` (không ai đọc `nonglam` mà đoán ra "nông lâm" nếu không biết tiếng Việt).
- **Việc cần làm:**
  1. `DATA_MODEL.md`: thay toàn bộ `nonglam` → `agriculture`, `nonglam_zones` → `agriculture_zones`, `gis_nonglam_zones` → `gis_agriculture_zones` (mục 1, 4.2, 4.3, và dòng giới thiệu đầu file).
  2. `CODING_CONVENTIONS.md` mục 6: thay `nonglam` → `agriculture`, `NonglamController` → `AgricultureController`, `NongLamZone` → `AgricultureZone`, `NongLamZoneDto` → `AgricultureZoneDto`.
  3. `ARCHITECTURE SPECIFICATION.md` mục 6.4: sửa câu đang dùng `nonglam` thành `agriculture`, đồng thời thêm 1 câu "Naming note" giống hệt cấu trúc câu đã sửa `khcn` → `science` ngay phía trên nó trong cùng mục, để lịch sử đổi tên được ghi lại nhất quán (đừng xoá âm thầm).
  4. `DEPLOYMENT & FLEET STRATEGY.md` mục 6.3, bước 3: sửa `ENABLE_OCOP` / `ENABLE_SCIENCE` / `ENABLE_AGRICULTURE` thành `FEATURES_OCOP_ENABLED` / `FEATURES_SCIENCE_ENABLED` / `FEATURES_AGRICULTURE_ENABLED`, khớp với cảnh báo đã có sẵn ở mục 4.1 cùng file.
  5. **Không đổi** `architect_deploy.mermaid` (`nongnghiep.gialai.gov.vn`) — đó là tên miền hiển thị cho người dùng cuối, không phải tên kỹ thuật, không liên quan đến quy ước code.
- **Cách verify:** `grep -rn "nonglam\|Nonglam\|NongLam" docs/ BE/ FE/` chỉ còn ra kết quả trong chính đoạn "Naming note" giải thích lịch sử đổi tên (mục 4 ở trên), không còn chỗ nào dùng để đặc tả thật.

### Mục 1: Quyết định cần chủ dự án chốt (KHÔNG tự ý quyết định thay)

> [!WARNING]
> 2 điểm dưới đây agent **không tự suy diễn** — nếu code đụng tới, dừng lại và hỏi chủ dự án.

#### 1.1. "Tourist Spots" có phải module thứ 4 không?

`PROJECT_OVERVIEW.md` mục 2 (dòng Phase 2) liệt kê: *"OCOP production units, Sci-Tech units, Agricultural production units, **Tourist Spots**, etc."* — nhưng `.env.example`/`DynamicFlywayConfig.java`/`ARCHITECTURE SPECIFICATION.md` chỉ có scaffolding cho **3** feature flag (`ocop`, `science`, `agriculture`), không có `tourism`. Kế hoạch này **chỉ code 3 module đã có flag**. Nếu chủ dự án muốn thêm `tourism` làm module thứ 4, việc đó lặp lại đúng pattern TSK-8/TSK-10/TSK-11 bên dưới (thêm 1 dòng flag vào `.env.example` + `DynamicFlywayConfig.java` + `application.properties.example`, rồi làm y hệt 1 module điểm), nhưng cần xác nhận trước vì nó phát sinh thêm ~2-3 ngày công.

#### 1.2. "People's Committee" (UBND) có trùng với `local_leaders` không?

`PROJECT_OVERVIEW.md` mục 4.3 liệt kê "People's Committee" như một **loại tổ chức** (Organization Type) cần quản lý trong Module Quản lý Tổ chức Trực thuộc — nhưng dự án đã có sẵn bảng `local_leaders` (đóng nốt ở TSK-7 bên dưới) lưu **con người** (Chủ tịch/Phó Chủ tịch UBND) theo từng xã. Đây là 2 khái niệm khác nhau về bản chất:

- `local_leaders` = **cá nhân** lãnh đạo (tên người, chức vụ, SĐT) — đã có bảng, đã có entity, chỉ cần nối dây (TSK-7).
- "People's Committee" theo mục 4.3 = **tổ chức/địa điểm** (trụ sở UBND xã, có địa chỉ, ảnh, mô tả) — nếu cần, đây sẽ là 1 loại "affiliated organization" riêng, có thể cần một bảng mới (không phải `local_leaders`).

Kế hoạch này **giả định 2 khái niệm tách biệt** và **không** tạo bảng "People's Committee organization" trong Giai đoạn 2 (không nằm trong 3 feature flag đã scaffold) — chỉ đóng nốt `local_leaders` (TSK-7). Nếu chủ dự án cần UBND-như-một-tổ-chức-có-địa-điểm, đó là quyết định phạm vi cần bàn riêng, không tự suy diễn vào 3 module OCOP/Science/Agriculture.

---

## 2. Phạm vi Giai đoạn 2 (và KHÔNG thuộc phạm vi)

| Thuộc phạm vi Giai đoạn 2 | KHÔNG thuộc phạm vi (dời sang Giai đoạn 3 — xem `Phase3-task.md`) |
| :--- | :--- |
| CRUD "sổ đăng ký" cho OCOP / Science / Agriculture (tạo, sửa, xoá, xem danh sách có phân trang) | Vẽ điểm/vùng của 3 module này lên bản đồ chính (Leaflet marker/polygon layer) |
| Module Resource/Media dùng chung (upload ảnh, PDF/DOCX) | Cụm điểm (clustering) khi zoom xa, popup trên bản đồ |
| Nhập toạ độ Point (lat/lng) qua form nhập số tay cho OCOP/Science | Chọn vị trí bằng cách click lên bản đồ (map picker) |
| Nối dây `local_leaders` vào `GET /api/wards/{code}` (đóng nốt việc dở từ Giai đoạn 1) | Tìm kiếm bán kính (radius search), lọc theo vùng hành chính trên bản đồ |
| Áp dụng TanStack Query cho toàn bộ data-fetching mới | Dashboard/Analytics trực quan trên bản đồ, xuất báo cáo PDF/Excel |
| Sao lưu off-site (đã bị flag "Phase 2 hardening task" trong `DEPLOYMENT & FLEET STRATEGY.md` mục 5.3) | Tự động hoá fleet/multi-instance (Dokploy/Coolify) — vẫn đang bị deferred theo `ARCHITECTURE SPECIFICATION.md` mục 7 |

---

## 3. Danh sách Tasks

### 🟢 PHẦN 0: ĐÓNG NỐT VIỆC CÒN LẠI CỦA GIAI ĐOẠN 1

#### **TSK-7: Nối dây `local_leaders` vào `GET /api/wards/{code}`**

- **Vì sao:** `LocalLeader` entity, `LocalLeaderRepository`, `LeaderDto` đã tồn tại từ Giai đoạn 1 nhưng chưa dùng (`API_CONTRACT.md` mục 4.3 gọi đây là "Planned shape"). Đây là việc nhỏ, an toàn, không phụ thuộc feature flag (nằm trong `core`, không phải `features/`), nên làm trước để "dọn sạch" trước khi mở rộng.
- **Input xác nhận:** `Ward` entity **không** có quan hệ `@OneToMany` tới `LocalLeader` — `LocalLeaderRepository.findByWardCode(String)` là query độc lập, phải gọi riêng từ Controller (giống hệt cách `gisWardRepository.findByWardCode()` đang được gọi riêng trong `WardController.getWardDetail()` hiện tại).
- **Việc cần làm (theo đúng thứ tự):**
  1. `BE/src/main/java/com/website/gis/core/dto/WardDetailDto.java`: thêm field `private List<LeaderDto> leaders;` (nhớ thêm `import java.util.List;`).
  2. `BE/src/main/java/com/website/gis/core/dto/WardDto.java`: dòng `// private List<LeaderDto> leaders;` đang comment — **XOÁ hẳn dòng comment này** (không wire vào `WardDto`). Lý do: `API_CONTRACT.md` mục 4.3 chỉ định nghĩa `leaders` cho `WardDetailDto` (endpoint chi tiết 1 xã), không phải cho `WardDto` (endpoint danh sách 135 xã) — nhét mảng leaders vào response danh sách sẽ phình payload không cần thiết và không khớp API_CONTRACT.
  3. `BE/src/main/java/com/website/gis/core/mapper/WardMapper.java`: sửa chữ ký `toDetailDto` thành 3 tham số:
     ```java
     @Mapping(source = "ward.province.fullName", target = "provinceName")
     @Mapping(source = "gisWard.areaKm2", target = "areaKm2")
     @Mapping(source = "leaders", target = "leaders")
     WardDetailDto toDetailDto(Ward ward, GisWard gisWard, List<LocalLeader> leaders);
     ```
     (2 method `toLeaderDto`/`toLeaderDtos` đã có sẵn, không cần sửa — MapStruct tự dùng `toLeaderDtos` cho field `leaders` vì kiểu khớp `List<LocalLeader>` → `List<LeaderDto>`.)
  4. `BE/src/main/java/com/website/gis/core/controller/WardController.java`:
     - Inject thêm `LocalLeaderRepository localLeaderRepository` qua constructor (theo đúng pattern constructor injection đang dùng).
     - Trong `getWardDetail(String code)`, thêm dòng `List<LocalLeader> leaders = localLeaderRepository.findByWardCode(code);` trước khi gọi mapper, rồi đổi lời gọi thành `wardMapper.toDetailDto(ward, gisWard, leaders);`.
  5. `docs/en/API_CONTRACT.md` mục 4.3, endpoint `GET /api/wards/{code}`: xoá nhãn "Current response" / "Planned shape", gộp lại thành 1 response mẫu duy nhất (giờ đã đúng thực tế).
- **Lưu ý dữ liệu:** `DATA_MODEL.md` mục 3.7 ghi *"For now, skip this step as there is no data"* — bảng `local_leaders` hiện **rỗng** (0 dòng). Sau khi nối dây xong, `leaders` sẽ trả về mảng rỗng `[]` cho mọi xã cho tới khi có dữ liệu thật được nhập (nhập tay qua SQL trực tiếp, đúng triết lý tối giản đã nêu ở `Phase1-task.md` mục `[!TIP]` — không cần UI riêng cho việc này ở Giai đoạn 2).
- **Cách verify:** Viết/sửa `WardControllerTest.java` (test slice `@WebMvcTest` sẵn có) thêm case mock `localLeaderRepository.findByWardCode(...)` trả về danh sách 1-2 leader giả, assert JSON response có field `leaders` đúng shape. Gọi `GET /api/wards/21112` qua Swagger/Postman thật, xác nhận trả `"leaders": []` (không lỗi 500) vì bảng đang rỗng.

---

### 🔴 PHẦN 1: BACKEND (SPRING BOOT)

#### **TSK-8: Module OCOP (bản mẫu tham chiếu — làm ĐẦU TIÊN trong 3 module)**

> [!WARNING]
> **Thứ tự bắt buộc, làm sai thứ tự Flyway sẽ báo lỗi khởi động** (đã cảnh báo sẵn trong comment của `DynamicFlywayConfig.java`): phải tạo xong migration folder + file `V*.sql` **TRƯỚC KHI** bật `features.ocop.enabled=true`. Nếu bật flag trước khi thư mục `db/migration/ocop` tồn tại, Flyway ném lỗi `Unable to resolve location` ngay lúc Spring Boot khởi động.

- **Vì sao chọn OCOP làm module đầu tiên:** đơn giản nhất về mặt hình học (point-type, 1 bảng duy nhất — `DATA_MODEL.md` mục 4.1), đã có sẵn màu sắc/UX quy định trong `Design_rule.md` (duy nhất trong 3 module có ví dụ cụ thể), và là chương trình có thật, ưu tiên cao của tỉnh (không phải dữ liệu giả định).
- **Việc cần làm:**
  1. Tạo `BE/src/main/resources/db/migration/ocop/V1__create_ocop_products.sql` — theo đúng khuôn `DATA_MODEL.md` mục 4.1:
     ```sql
     CREATE TABLE ocop_products (
         id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
         name varchar(255) NOT NULL,
         product_type varchar(100),
         description text,
         ward_code varchar(20) NOT NULL,
         geom geometry(Point, 4326) NOT NULL,
         image_url varchar(500),
         PRIMARY KEY (id),
         CONSTRAINT ocop_products_ward_code_fkey FOREIGN KEY (ward_code) REFERENCES wards (code)
     );
     CREATE INDEX idx_ocop_products_ward_code ON public.ocop_products USING btree (ward_code);
     CREATE INDEX idx_ocop_products_geom ON public.ocop_products USING gist (geom);
     ```
     _Ghi chú:_ cột `geom` tạo sẵn ngay từ Giai đoạn 2 (dù chưa hiển thị bản đồ) để tránh phải `ALTER TABLE` rủi ro sau này — nhập toạ độ qua form tay ở TSK-13, không cần map picker (đó là việc của Giai đoạn 3).
  2. Package mới `com.website.gis.features.ocop` (theo đúng `ARCHITECTURE SPECIFICATION.md` mục 4.1 và `CODING_CONVENTIONS.md` mục 1.1/6) — **không** đặt trong `core`:
     - `entity/OcopProduct.java` — entity JPA ánh xạ bảng trên, dùng `org.locationtech.jts.geom.Point` cho cột `geom` (Hibernate Spatial đã có sẵn dependency, xem `WardMapper`/`GisWard` cho ví dụ cách project hiện xử lý kiểu geometry — thực ra `GisWard` lưu `geom` dưới dạng truy vấn native trả JSON, không map trực tiếp qua entity field kiểu JTS; giữ nhất quán bằng cách **không** map `geom` thành field entity kiểu phức tạp trong Giai đoạn 2 — thay vào đó lưu `geom` qua native `INSERT`/`UPDATE` dùng `ST_MakePoint(:lng, :lat)` giống cách `V4__import_gis_data_gialai.sql` đã làm, và **không** expose `geom` trong `OcopProductDto` của Giai đoạn 2 — chỉ cần `latitude`/`longitude` dạng `BigDecimal` trong DTO, service tự chuyển đổi. Điều này giữ Giai đoạn 2 đơn giản, không đòi hỏi agent phải xử lý JTS serialization chưa cần thiết).
     - `repository/OcopProductRepository.java` — `extends JpaRepository<OcopProduct, Integer>`, thêm method `Page<OcopProduct> findByWardCode(String wardCode, Pageable pageable)` cho lọc theo xã.
     - `dto/OcopProductDto.java`, `dto/OcopProductCreateRequest.java`, `dto/OcopProductUpdateRequest.java` — theo suffix convention `CODING_CONVENTIONS.md` mục 1.1. Field: `id`, `name`, `productType`, `description`, `wardCode`, `latitude`, `longitude`, `imageUrl`.
     - `mapper/OcopProductMapper.java` — `@Mapper(componentModel = "spring")`, đặt **trong** package `features.ocop.mapper` (không phải `core.mapper`) — đúng `CODING_CONVENTIONS.md` mục 3.1: *"deleting a feature module's package deletes its mapper along with it"*.
     - `controller/OcopController.java`:
       ```java
       @RestController
       @RequestMapping("/api/ocop")
       @ConditionalOnProperty(name = "features.ocop.enabled", havingValue = "true")
       public class OcopController {
           // GET  /api/ocop          -> Page<OcopProductDto>, phân trang theo API_CONTRACT.md mục 3
           // GET  /api/ocop/{id}     -> OcopProductDto, 404 nếu không có
           // POST /api/ocop          -> chỉ ADMIN (@PreAuthorize hoặc SecurityConfig matcher)
           // PUT  /api/ocop/{id}     -> chỉ ADMIN
           // DELETE /api/ocop/{id}   -> chỉ ADMIN
       }
       ```
       Không tạo `@Service` riêng (dự án hiện chưa có tầng Service — `CODING_CONVENTIONS.md` mục 3.2: *"controllers call repositories (and now mappers) directly"* — giữ nhất quán, không tự ý thêm tầng kiến trúc mới ở đây).
  3. `BE/src/main/java/com/website/gis/config/SecurityConfig.java`: thêm matcher `.requestMatchers(HttpMethod.POST, "/api/ocop/**").hasRole("ADMIN")`, tương tự cho `PUT`/`DELETE`; `GET /api/ocop/**` giữ trong nhóm `authenticated()` sẵn có (không cần dòng riêng vì rule mặc định `.requestMatchers("/api/**").authenticated()` đã áp dụng).
  4. `.env.example` (root) và `application.properties.example`: đổi `FEATURES_OCOP_ENABLED=false` / `features.ocop.enabled=false` thành `true` **chỉ trong môi trường dev cục bộ của agent khi test** — giữ nguyên `false` khi commit (Gia Lai chưa mua module này, đúng triết lý "database-per-customer" ở `ARCHITECTURE SPECIFICATION.md` mục 6).
  5. Viết `OcopControllerTest.java` (slice `@WebMvcTest`, mock repository — theo đúng pattern `WardControllerTest.java`/`AdminControllerTest.java` đã có) **và** 1 integration test dùng Testcontainers thật (theo pattern `AdminControllerIntegrationTest.java` mới thêm ở Giai đoạn 1) để xác nhận feature flag `false` → endpoint trả `404` (không phải `403`), và flag `true` → CRUD hoạt động đúng.
- **Cách verify:**
  - Với `features.ocop.enabled=false` (mặc định): `GET /api/ocop` phải trả `404 Not Found` (bean không được tạo — đúng như `ARCHITECTURE SPECIFICATION.md` mục 4.2 mô tả), **không phải** `403`.
  - Với `features.ocop.enabled=true`: đăng nhập `viewer`, gọi `POST /api/ocop` → `403`. Đăng nhập `admin` → tạo thành công, `GET /api/ocop` thấy sản phẩm vừa tạo, có phân trang đúng shape `API_CONTRACT.md` mục 3.
  - `./mvnw -B verify` chạy sạch với **cả 2 trường hợp** flag on/off (thêm profile test riêng nếu cần) — đây chính là bài học từ Bug Testcontainers/PostGIS đã gặp ở Giai đoạn 1: đừng chỉ tin code compile được, chạy test thật với DB thật.

#### **TSK-9: Module Resource/Media Management (dùng chung cho OCOP/Science/Agriculture)**

- **Vì sao làm riêng, làm chung:** `PROJECT_OVERVIEW.md` mục 4.4 mô tả đây là 1 module riêng ("Developed and integrated starting from Phase 2"), dùng chung cho mọi loại tổ chức trực thuộc — nếu code riêng cho từng module (OCOP tự upload, Science tự upload...) sẽ lặp code 3 lần. Đặt trong `core` (không phải `features/`) vì đây là hạ tầng dùng chung, không tắt/bật theo khách hàng.
- **Việc cần làm:**
  1. Thiết kế interface trước khi code, đúng yêu cầu *"Implemented via interface-driven code"* của `PROJECT_OVERVIEW.md` mục 4.4:
     ```java
     package com.website.gis.core.storage;

     public interface FileStorageService {
         StoredFile store(MultipartFile file, String subDirectory);
         Resource loadAsResource(String storedFileName);
         void delete(String storedFileName);
     }
     ```
     `StoredFile` là 1 record/DTO nhỏ chứa `storedFileName`, `originalFileName`, `contentType`, `sizeBytes`, `publicUrl`.
  2. `LocalFileStorageService implements FileStorageService` — lưu vào thư mục cấu hình qua property mới `app.storage.local-path` (mặc định `./data/uploads`, **không** commit thư mục dữ liệu thật vào git — thêm `data/` vào `.gitignore` root).
  3. Validate nghiêm ngặt trước khi lưu (không tin đuôi file client gửi lên): giới hạn kích thước (đề xuất 5MB ảnh / 20MB tài liệu — cần chủ dự án xác nhận số cụ thể), whitelist MIME type thật sự đọc từ nội dung file (không chỉ đọc `Content-Type` header, vì client có thể giả mạo) cho `image/jpeg`, `image/png`, `application/pdf`, và các định dạng Word (`.docx`).
  4. Tối ưu kích thước ảnh khi upload (`PROJECT_OVERVIEW.md` mục 4.4: *"Automatically optimize image sizes upon upload"*) — thêm dependency resize ảnh phía server (ví dụ `net.coobird:thumbnailator`, thư viện Java nhỏ gọn, license Apache 2.0 tương thích license hiện tại của dự án) vào `BE/pom.xml`; resize về chiều rộng tối đa hợp lý (đề xuất 1600px) trước khi lưu, giữ tỉ lệ khung hình.
  5. `controller/FileUploadController.java` (`core.controller`): `POST /api/files` (multipart, chỉ `ADMIN`, trả về `publicUrl` để FE gán vào field `imageUrl`/`documentUrl` của DTO tương ứng), `GET /api/files/{storedFileName}` (tải file, dùng cho cả `ADMIN` và `VIEWER` vì ảnh/tài liệu là dữ liệu công khai trong nội bộ hệ thống).
  6. Ghi migration `BE/src/main/resources/db/migration/core/V5__create_attachments.sql` (đây là bảng **core**, không phải feature — vì service dùng chung mọi module) nếu quyết định lưu metadata file vào DB thay vì chỉ dựa vào tên file trên đĩa; nếu không cần tra cứu/liệt kê file độc lập, có thể bỏ qua bảng riêng và chỉ lưu `imageUrl`/`documentUrl` dạng string trực tiếp trên từng entity (`OcopProduct.imageUrl` v.v.) — **khuyến nghị phương án đơn giản này trước**, chỉ thêm bảng `attachments` nếu về sau cần 1 tổ chức có nhiều hơn 1 ảnh/tài liệu.
- **Cách verify:** Unit test `LocalFileStorageService` (không cần Testcontainers, chỉ cần thư mục temp — dùng `@TempDir` của JUnit 5). Test upload file giả mạo đuôi (đổi tên `.exe` thành `.png`) bị từ chối đúng.

#### **TSK-10: Module Science (nhân bản từ OCOP)**

- **Việc cần làm:** lặp lại chính xác các bước ở TSK-8, đổi `ocop` → `science`, `OcopProduct` → `ScienceUnit`, bảng `science_units` (cùng shape cột với `ocop_products`, theo đúng câu *"science_units should follow the identical shape"* ở `DATA_MODEL.md` mục 4.1). Migration tại `db/migration/science/V1__create_science_units.sql`.
- **Khác biệt duy nhất so với OCOP:** không cần lặp lại việc thiết kế `FileStorageService` (TSK-9 đã xong, tái sử dụng interface có sẵn) — chỉ cần gọi `POST /api/files` từ FE giống OCOP.
- **Cách verify:** giống hệt TSK-8, đổi endpoint thành `/api/science`.

#### **TSK-11: Module Agriculture (nhân bản có điều chỉnh — KHÔNG có hình học ở Giai đoạn 2)**

> [!WARNING]
> Agriculture **khác về bản chất hình học** so với OCOP/Science: `DATA_MODEL.md` mục 4.2 xác định đây là **zone/polygon-type** (MultiPolygon), không phải point-type. Một `MultiPolygon` không thể nhập qua vài ô số như lat/lng — cần công cụ vẽ trên bản đồ, và việc đó thuộc Giai đoạn 3 (`Phase3-task.md` TSK-17). Vì vậy Agriculture ở Giai đoạn 2 **chỉ có bảng thuộc tính nghiệp vụ, KHÔNG có cột hình học** — đây là điều chỉnh có chủ đích so với việc áp y hệt khuôn OCOP, không phải thiếu sót.

- **Việc cần làm:**
  1. Migration `db/migration/agriculture/V1__create_agriculture_zones.sql` — **chỉ bảng nghiệp vụ**, chưa có bảng `gis_agriculture_zones`:
     ```sql
     CREATE TABLE agriculture_zones (
         id integer GENERATED ALWAYS AS IDENTITY NOT NULL,
         zone_name varchar(255) NOT NULL,
         zone_type varchar(100),
         description text,
         ward_code varchar(20) NOT NULL,
         image_url varchar(500),
         PRIMARY KEY (id),
         CONSTRAINT agriculture_zones_ward_code_fkey FOREIGN KEY (ward_code) REFERENCES wards (code)
     );
     CREATE INDEX idx_agriculture_zones_ward_code ON public.agriculture_zones USING btree (ward_code);
     ```
  2. Package `com.website.gis.features.agriculture` — entity `AgricultureZone`, repo, dto (`zoneName`, `zoneType`, `description`, `wardCode`, `imageUrl` — **không có `latitude`/`longitude`/`geom`**), mapper, `AgricultureController` với `@ConditionalOnProperty(name = "features.agriculture.enabled", ...)`, CRUD y hệt pattern TSK-8 nhưng không có toạ độ.
  3. Ghi rõ trong Javadoc đầu `AgricultureController.java` lý do không có hình học ở giai đoạn này + trỏ tới `Phase3-task.md` TSK-17 (nơi cột `geom` sẽ được thêm bằng migration **forward-only mới**, không sửa lại `V1` này — đúng nguyên tắc Flyway đã nêu ở `CODING_CONVENTIONS.md`/`DEPLOYMENT & FLEET STRATEGY.md` mục 5.2: *"Flyway migrations are never edited or deleted once applied"*).
- **Cách verify:** giống TSK-8 nhưng bỏ qua mọi bước liên quan toạ độ.

---

### 🔵 PHẦN 2: FRONTEND (REACT)

#### **TSK-12: Hạ tầng TanStack Query + Feature Flags**

- **Vì sao:** `PROJECT_OVERVIEW.md` mục 3.2 và `CODING_CONVENTIONS.md` mục 4 đã xác nhận: TanStack Query **chưa** được cài, và việc cài đặt được lên lịch chính thức cho Giai đoạn 2. Đây là việc nền tảng, làm trước mọi UI module.
- **Việc cần làm:**
  1. `pnpm add @tanstack/react-query` trong `FE/` — kiểm tra version tương thích với React 19 đã cài (`peerDependencies` của TanStack Query v5 hỗ trợ React 19; xác nhận lại bằng `pnpm build` sau khi cài, không giả định).
  2. Bọc `<QueryClientProvider>` trong `FE/src/main.tsx`, đặt **trong** `<AuthProvider>` hiện có hoặc bên ngoài đều được — ghi rõ lựa chọn trong code comment vì đây là quyết định one-way khó đổi sau khi nhiều hook đã dùng.
  3. Tạo `FE/src/api/queryKeys.ts` — đúng pattern Factory đã đặc tả sẵn ở `CODING_CONVENTIONS.md` mục 4.1 (copy y nguyên `wardKeys` mẫu, bổ sung `ocopKeys`, `scienceKeys`, `agricultureKeys` theo cùng shape `all/lists/list/detail`).
  4. Tạo `FE/src/config/features.ts` — đúng `ARCHITECTURE SPECIFICATION.md` mục 3.2:
     ```typescript
     export const FEATURE_FLAGS = {
       ocop: import.meta.env.VITE_ENABLE_OCOP === 'true',
       science: import.meta.env.VITE_ENABLE_SCIENCE === 'true',
       agriculture: import.meta.env.VITE_ENABLE_AGRICULTURE === 'true',
     };
     ```
     Tạo `FE/.env.example` (hiện **chưa tồn tại** — chỉ có `.env.example` ở root cho Docker/BE; FE cần file riêng cho biến `VITE_*` dùng lúc `pnpm build`) với 3 dòng trên đặt `false`.
  5. **Không** tự ý implement toàn bộ router lazy-loading mẫu ở `ARCHITECTURE SPECIFICATION.md` mục 3.2 (route `/ocop`, `/science` riêng) — dự án hiện dùng 1 trang `Home.tsx` với `activeView` state cục bộ (`SidebarDrawer` → `setActiveView`), **không** dùng React Router cho các view nội bộ (xem `Home.tsx`/`SidebarDrawer.tsx` hiện tại). Giữ nguyên pattern `activeView` đã có, chỉ thêm các case mới (`'ocop' | 'science' | 'agriculture'`) thay vì đổi sang React Router lazy loading — đổi kiến trúc routing không nằm trong phạm vi Giai đoạn 2 và sẽ phá vỡ `SidebarDrawer` hiện tại một cách không cần thiết.
- **Cách verify:** `pnpm build && pnpm lint` sạch (0 lỗi, giống chuẩn đã đạt ở Giai đoạn 1). Devtools React Query hiển thị đúng khi bật `import { ReactQueryDevtools } from '@tanstack/react-query-devtools'` ở môi trường dev.

#### **TSK-13: Giao diện Admin CRUD cho OCOP**

- **Việc cần làm:**
  - `FE/src/pages/home/components/OcopPanel.tsx` — bảng danh sách (phân trang, dùng `useQuery` + `ocopKeys.list(...)`), nút "Thêm mới" mở modal form (đặt tại `FE/src/pages/home/components/OcopFormModal.tsx`, code tay bằng Tailwind, **không** dùng Radix/Shadcn dialog — giữ đúng quyết định đã chốt ở Giai đoạn 1 `Phase1-task.md` TSK-4: *"modal CRUD user được code tay bằng Tailwind... để nhẹ hơn"*, áp dụng nhất quán cho mọi modal mới).
  - Form gồm: tên, loại sản phẩm, mô tả, dropdown chọn xã (tái dùng dữ liệu `GET /api/wards` đã có), 2 ô số `latitude`/`longitude` (validate trong khoảng toạ độ hợp lý của Gia Lai — tỉnh nằm trong khoảng vĩ độ ~12.7°–15.3°N, kinh độ ~107.3°–109.4°E — validate mềm, chỉ cảnh báo không chặn cứng vì agent không nên tự đặt biên chính xác tuyệt đối), widget upload ảnh gọi `POST /api/files` (TSK-9) trước, nhận `publicUrl` rồi mới `POST /api/ocop`.
  - Thêm nút điều hướng "OCOP" vào `SidebarDrawer.tsx`, **chỉ hiển thị khi `FEATURE_FLAGS.ocop === true`** (import từ `src/config/features.ts`) — đúng cơ chế Compile-time Modularity, không phải role-based như "Quản lý người dùng".
  - Áp dụng đúng bảng màu OCOP đã định nghĩa ở `Design_rule.md` mục 2 (`#F97316` light theme) cho mọi badge/icon liên quan tới OCOP trong UI quản trị này — dù bản đồ (nơi màu này chủ yếu dùng) thuộc Giai đoạn 3, dùng màu nhất quán ngay từ UI quản trị giúp người dùng liên kết thị giác sớm.
- **Cách verify:** `viewer` đăng nhập → không thấy mục "OCOP" trong sidebar (khi cờ FE bật) nếu quyết định ẩn hoàn toàn với non-admin — **cần xác nhận:** VIEWER có được xem danh sách OCOP (read-only) hay chỉ ADMIN mới thấy mục này? `PROJECT_OVERVIEW.md` không nói rõ; theo đúng vai trò đã định nghĩa ("VIEWER: Read-only map search and administrative boundary lookups"), đề xuất VIEWER **có thể xem** danh sách/bản đồ OCOP (đọc) nhưng không thấy nút Thêm/Sửa/Xoá — khớp với pattern quyền hạn ADMIN/VIEWER đã áp dụng nhất quán trong toàn dự án.

#### **TSK-14: Giao diện Admin CRUD cho Science**

- Nhân bản TSK-13, đổi `Ocop` → `Science`, dùng đúng field shape của `ScienceUnit` (TSK-10).

#### **TSK-15: Giao diện Admin CRUD cho Agriculture**

- Nhân bản TSK-13 nhưng **bỏ 2 ô `latitude`/`longitude`** (Agriculture Giai đoạn 2 không có toạ độ — xem cảnh báo ở TSK-11).

---

### 📦 PHẦN 3: VẬN HÀNH

#### **TSK-16: Sao lưu off-site (Off-VPS backup replication)**

- **Vì sao:** `DEPLOYMENT & FLEET STRATEGY.md` mục 5.3 tự flag rõ: *"Off-VPS copies of backups... are a Phase 2 hardening task — not yet in place; flag this as an open risk until it is."* Đây là nợ kỹ thuật đã được chính tài liệu Giai đoạn 1 xác nhận, không phải việc tự phát sinh thêm.
- **Việc cần làm:** mở rộng `scripts/backup-db.sh` (đã sửa đúng ở Giai đoạn 1 — xem lịch sử review trước) — sau dòng `pg_dump` thành công, đồng bộ file `.dump` vừa tạo sang 1 đích lưu trữ ngoài VPS. Đề xuất 2 phương án (cần chủ dự án chọn, không tự quyết vì liên quan chi phí/tài khoản bên thứ 3):
  1. `rclone` tới object storage tương thích S3 của nhà cung cấp trong nước (khớp yêu cầu chủ quyền dữ liệu ở `PROJECT_OVERVIEW.md` mục 1 — **không** dùng AWS S3/GCS trực tiếp nếu yêu cầu này áp dụng cho cả backup, cần xác nhận).
  2. `rsync` qua SSH tới 1 VPS phụ thứ hai.
- **Cách verify:** giả lập backup, xác nhận file xuất hiện ở đích ngoài VPS, và restore thử từ bản sao off-site (không chỉ từ bản local) để chắc chắn bản sao không hỏng.

---

## 4. Definition of Done — Giai đoạn 2

- [ ] TSK-6 xong: `grep -rn "nonglam\|Nonglam\|NongLam"` trong `docs/`/`BE/`/`FE/` chỉ còn trong đoạn giải thích lịch sử đổi tên.
- [ ] TSK-7 xong: `GET /api/wards/{code}` trả `leaders: []` (hoặc có dữ liệu nếu đã seed tay), `API_CONTRACT.md` không còn phân biệt "Current" / "Planned".
- [ ] TSK-8, TSK-10, TSK-11 xong: `./mvnw -B verify` pass với **cả 3 feature flag `false` lẫn `true`** (chạy test 2 lần, hoặc dùng `@ActiveProfiles` riêng cho từng trường hợp).
- [ ] TSK-9 xong: upload ảnh/PDF hoạt động, file giả mạo đuôi bị từ chối.
- [ ] TSK-12–15 xong: `pnpm build && pnpm lint` sạch; bật lần lượt từng `VITE_ENABLE_*` xác nhận đúng module hiện/ẩn trên Sidebar.
- [ ] TSK-16 xong: có bằng chứng restore thành công từ bản sao off-site.
- [ ] Tất cả 3 module OCOP/Science/Agriculture khi **tắt flag** (mặc định khi bàn giao, vì Gia Lai hiện chưa mua module nào) đều trả `404` đúng như thiết kế, không rò rỉ endpoint nào.
- [ ] Chạy lại `docker compose up -d --build` trên máy sạch một lần nữa (như đã làm cuối Giai đoạn 1) để xác nhận 3 Flyway feature-folder mới không phá vỡ luồng khởi động khi **toàn bộ 3 flag đều `false`** (trường hợp mặc định thật của Gia Lai).

## 5. Bàn giao sang Giai đoạn 3

Khi Definition of Done ở trên đạt 100%, entity + bảng dữ liệu của OCOP/Science đã có cột `geom`/toạ độ sẵn sàng (Agriculture thì chưa — xem TSK-17 đầu `Phase3-task.md`). Giai đoạn 3 sẽ **không** động vào CRUD registry đã xây ở đây — chỉ thêm lớp hiển thị bản đồ + spatial query lên trên. Xem `docs/Phase 3/Phase3-task.md`.
