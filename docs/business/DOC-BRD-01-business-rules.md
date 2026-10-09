# DOC-BRD-01 — Quy tắc nghiệp vụ (Business Rules)

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-BRD-01 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | BA, SA, QA, Dev |
| **Liên quan** | DOC-UC-01, DOC-ATP-01, DOC-UAT-03 |

---

## 1. Mục đích

Tập trung **quy tắc nghiệp vụ** có thể kiểm chứng. Không mô tả UI chi tiết hay contract API.

## 2. Danh mục sản phẩm & lọc

| ID | Rule | Nguồn dữ liệu / hành vi hệ thống |
|----|------|----------------------------------|
| **BR-01** | Mục **SALE** chỉ gồm sản phẩm đang giảm giá | `discount > 0` (không phụ thuộc chuỗi category tên “SALE”) |
| **BR-02** | Mục **NEW IN** gồm SP được gán nhóm hàng mới | Field `categories` chứa chuỗi `NEW IN` |
| **BR-03** | Mục BEST SELLER / TOP / BOTTOM / OUTWEAR / SHOES_BAG / ACC | `categories` chứa đúng mã danh mục tương ứng |
| **BR-04** | Tìm kiếm theo tên | `name` chứa chuỗi tìm kiếm (không phân biệt hoa thường) |
| **BR-05** | Thêm giỏ bắt buộc biến thể hợp lệ | Phải có `variationId` khớp màu/size/type đã chọn (trừ phụ kiện cấu hình đặc biệt) |

## 3. Đơn hàng & thanh toán

| ID | Rule |
|----|------|
| **BR-06** | **COD:** tạo đơn và xử lý giỏ theo luồng COD hiện hành ngay khi checkout thành công |
| **BR-07** | **VietQR:** đơn/xác nhận thanh toán chuyển khoản hoàn tất sau khi Partner gọi Transaction Sync hợp lệ (sau bước cấp token); ghi nhận giao dịch để đối soát |
| **BR-08** | Thành viên có thể lưu **chiều cao (cm)** và **cân nặng (kg)** trên tài khoản để tái sử dụng tư vấn size |

## 4. AI — Gợi ý size (PDP)

| ID | Rule |
|----|------|
| **BR-10** | Tư vấn size dựa trên thông số khách + **bảng đo sản phẩm** (`products.description_size`) và danh sách size có sẵn; `backupSize` (form rộng) phải là size **lớn hơn** recommended theo thứ tự S→M→L→XL→XXL (không gợi ý nhỏ hơn) |

## 5. AI — Store Chat

| ID | Rule |
|----|------|
| **BR-11** | Khi khách nêu **loại món cụ thể** (vd. áo phông, sơ mi, jean): hệ thống lọc **exact** theo tên khớp loại đó; **không** trả toàn bộ danh mục TOP như kết quả đúng yêu cầu |
| **BR-12** | Chỉ khi **không có** SP exact: thông báo không có đúng loại; sau đó mới gợi ý **tương tự** (cùng nhóm gợi ý), vẫn loại SP lệch loại (vd. hỏi phông thì không đưa sơ mi như exact); gắn cờ/nhãn *gợi ý tương tự* |
| **BR-13** | Tư vấn size trong chat: ưu tiên số đo trong câu hỏi → request → profile login; thiếu số đo thì **hỏi thêm**, không gắn size mặc định sai (vd. XL); khi có số đo, size gợi ý lấy từ bảng đo / chart và phải thuộc `sizes` của SP |

## 6. Phi chức năng (tham chiếu)

| ID | Rule |
|----|------|
| **NFR-01** | Lỗi AI/quota: không làm sập trang; trả thông báo hoặc fallback có kiểm soát |
| **NFR-02** | Callback VietQR `/vqr/**` không áp CSRF như trang form thường (theo cấu hình bảo mật path) |

## 7. Trace

Checklist UAT map `BR-*` qua [DOC-ATP-01](../testing/DOC-ATP-01-acceptance-scenarios.md) và [DOC-UAT-05](../uat/DOC-UAT-05-acceptance-checklist.md).
