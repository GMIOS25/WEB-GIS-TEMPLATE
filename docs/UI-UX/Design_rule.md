# Hướng dẫn Thiết kế UI/UX & Hệ thống Màu sắc (GIS Internal Project)

Tài liệu này định nghĩa cấu trúc giao diện, tư duy thiết kế, và hệ thống mã màu chủ đạo cho dự án **GIS nội bộ**. Agent/Developer cần tuân thủ nghiêm ngặt các quy tắc dưới đây để đảm bảo tính nhất quán và tối ưu hóa trải nghiệm người dùng (UX).

---

## 1. Phong cách Thiết kế Chủ đạo (Design Style)

- **Triết lý:** `Clean & Functional Minimalism` (Tối giản và Tập trung vào Công năng).
- **Nguyên tắc cốt lõi:** Bản đồ và dữ liệu là nhân vật chính. Giao diện xung quanh phải làm nền, tinh gọn, tránh gây nhiễu thị giác cho nhân viên vận hành.
- **Đặc tính UI:**
  - **Panels/Widgets:** Sử dụng thiết kế phẳng (Flat), bo góc nhẹ (`border-radius: 8px` đến `12px`).
  - **Layer-based UI:** Các bảng điều khiển đè lên bản đồ nên áp dụng hiệu ứng mờ nhẹ nền (`backdrop-filter: blur()`) để giữ ngữ cảnh không gian bên dưới.
  - **Độ tách biệt (Elevation):** Sử dụng đổ bóng mờ, đổ bóng nhẹ (`box-shadow`) để phân tách các lớp công cụ điều khiển ra khỏi bản đồ nền.

---

## 2. Hệ thống Chủ đề & Mã màu (Color Palette)

Hệ thống khuyến nghị mặc định sử dụng **Light Theme** (đặc thù ngành quản lý hành chính, giáo dục, văn phòng). Tuy nhiên cần cấu trúc mã nguồn sẵn sàng cho **Dark Theme** để mở rộng sau này.

### Quy tắc Phối màu Bản đồ (Map Layer Rule)

> **Nguyên tắc sống còn:** `Nền trung tính dịu mắt - Điểm nhấn rực rỡ`. Không trùng màu giữa UI hệ thống và dữ liệu chuyên đề trên bản đồ.

### Bảng mã màu chi tiết (Hex Codes)

| Thành phần hiển thị             | Light Theme (Khuyên dùng)           | Dark Theme (Dự phòng)       | Trạng thái / Ghi chú UX                                         |
| :------------------------------ | :---------------------------------- | :-------------------------- | :-------------------------------------------------------------- |
| **Màu nền UI hệ thống**         | `#F8FAFC` (Slate Light)             | `#0F172A` (Deep Slate)      | Tạo cảm giác công sở sạch sẽ, hiện đại.                         |
| **Màu chữ chính (Text)**        | `#1E293B` (Slate Dark)              | `#F1F5F9` (Slate White)     | Đảm bảo độ tương phản đọc text tốt.                             |
| **Style Bản đồ nền (Base Map)** | `CartoDB Positron` / `Mapbox Light` | `CartoDB Dark Matter`       | Tối giản, ẩn bớt các POI thương mại không liên quan.            |
| **Viền Ranh giới Phường/Xã**    | `#94A3B8` hoặc `#64748B`            | `#334155`                   | Nét mảnh (`1px - 1.5px`), dạng nét đứt (`dashed`).              |
| **Màu vùng Phường/Xã (Fill)**   | `Transparent` (Trong suốt)          | `Transparent` (Trong suốt)  | Chỉ đổi màu khi `Hover` hoặc `Select` (Xem chi tiết phía dưới). |
| **Màu Điểm Trường Học (Point)** | `#F97316` (Cam ấm)                  | `#FACC15` (Vàng Chanh Neon) | Dạng chấm tròn, có viền trắng (`Halo effect`) tách nền.         |

---

## 3. Quy trình Tương tác & Trải nghiệm Người dùng (UX Workflow)

Agent cần thiết lập logic xử lý bản đồ theo 3 kịch bản tương tác cốt lõi sau:

### Lớp dữ liệu Ranh giới Phường/Xã (Polygon Layer)

1. **Trạng thái mặc định (Default):** Chỉ hiển thị đường viền mảnh. Màu nền của vùng để trong suốt để thấy rõ đường sá bên dưới bản đồ nền.
2. **Trạng thái Di chuột (Hover):** Thay đổi màu nền vùng (Fill) sang xanh dương siêu nhạt `#E0F2FE` (Opacity 20-30%) để người dùng biết họ đang định vị ở xã nào.
3. **Trạng thái Click chọn (Selected):** Giữ màu nền chọn, đồng thời làm mờ nhẹ (`opacity`) các vùng xung quanh để làm nổi bật thực thể đang thao tác.

### Lớp dữ liệu (Ví dụ: Điểm Trường Học) (Point Layer)

- **Cơ chế Kích hoạt:** Chỉ hiển thị khi người dùng tích chọn (Toggle On) tại Sidebar quản lý lớp dữ liệu.
- **Logic Thu phóng (Zoom Visibility & Clustering):**
  - **Khi Zoom xa (Mức Tỉnh):** `BẮT BUỘC` gộp các điểm trường học gần nhau lại thành một vòng tròn lớn kèm số lượng (Ví dụ: `(50)` trường) để tránh nghẹt thở bản đồ.
  - **Khi Zoom gần (Mức Huyện/Xã):** Tự động rã gộp (Uncluster), hiển thị thành các chấm tròn màu Cam đơn lẻ tại vị trí tọa độ chính xác.

### Logic Hiển thị Hộp thông tin (Popup/Infowindow)

- Khi người dùng `Click` vào một Point Trường học:
  1. Thêm hiệu ứng mạch đập (Ripple effect) hoặc phóng to nhẹ điểm đó để định vị thị giác.
  2. Hiển thị Popup nhỏ ngay tại vị trí điểm với cấu trúc:
     - **Tiêu đề:** Tên trường học (Bold).
     - **Nội dung:** Địa chỉ chi tiết, Số điện thoại liên hệ, Thuộc Phường/Xã nào.
     - **Hành động:** Nút `[Xem chi tiết]` ở góc dưới để mở panel dữ liệu lớn (nếu cần).

---

## 4. Thiết kế Sidebar Điều khiển (Layer Control Panel)

- **Vị trí cố định:** Cạnh trái (Left Sidebar) hoặc Cạnh phải (Right Sidebar).
- **Cơ chế bật/tắt:** Dùng linh hoạt `Checkbox` hoặc `Switch Toggle`.
- **Bắt buộc có Legend trực quan:** Ngay bên cạnh nhãn "Trường học" phải có một icon nhỏ hoặc chấm tròn màu Cam đúng với màu hiển thị trên bản đồ để người dùng đối chiếu ngay lập tức mà không cần mở bảng chú giải riêng.
