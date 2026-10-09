# DOC-UAT-03 — Danh mục chức năng

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-UAT-03 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | Ban nghiệm thu, BA, QA, Dev |
| **Liên quan** | DOC-BRD-01, DOC-UC-01, DOC-UAT-05 |

---

## 1. Mục đích

Liệt kê chức năng đưa vào nghiệm thu theo mã `FN-xxx`, map tới Business Rule / Use Case. Chi tiết thao tác xem [DOC-UAT-04](DOC-UAT-04-user-guide.md); assert xem [DOC-UAT-05](DOC-UAT-05-acceptance-checklist.md).

## 2. Catalog

| Mã | Tên chức năng | Actor | BR | UC | UAT bắt buộc |
|----|---------------|-------|----|----|--------------|
| FN-01 | Duyệt danh mục SALE | Khách | BR-01 | UC-01 | Có |
| FN-02 | Duyệt danh mục NEW IN | Khách | BR-02 | UC-01 | Có |
| FN-03 | Duyệt BEST SELLER / nhóm SHOP | Khách | BR-03 | UC-01 | Có |
| FN-04 | Tìm kiếm sản phẩm theo tên | Khách | BR-04 | UC-01 | Có |
| FN-05 | Chi tiết SP, chọn biến thể, thêm giỏ / mua ngay | Khách | BR-05 | UC-02 | Có |
| FN-06 | Đặt hàng COD | Khách / TV | BR-06 | UC-03 | Có |
| FN-07 | Thanh toán VietQR | Khách / TV | BR-07 | UC-04 | Có |
| FN-08 | Đăng ký / đăng nhập / hồ sơ (height, weight) | Thành viên | BR-08 | UC-05 | Có |
| FN-09 | Gợi ý size AI trên PDP | Khách / TV | BR-10 | UC-05 | Có |
| FN-10 | Store Chat stylist (tìm SP + size) | Khách / TV | BR-11…13 | UC-06 | Có |
| FN-11 | Admin quản lý sản phẩm | Admin | — | — | Có* |
| FN-12 | Admin quản lý / xem đơn hàng | Admin | — | UC-03/04 | Có* |
| FN-13 | Admin xem lịch sử giao dịch thanh toán (AM010) | Admin | BR-07 | UC-04 | Có* |

\* *Bắt buộc ở mức “thực hiện được thao tác nghiệp vụ chính”; không yêu cầu nghiệm thu toàn bộ màn hình admin phụ.*

## 3. Nhóm chức năng theo phân hệ

### 3.1 Storefront & danh mục

- Menu: SALE, NEW IN, BEST SELLER, OUTWEAR, TOP, BOTTOM, SHOES/BAGS, ACC.  
- Trang chủ block NEW IN và ALL PRODUCTS.  
- Search form header → `/products/search?name=`.

### 3.2 Giỏ & thanh toán

- `POST /add-to-cart` với `variationId`.  
- Checkout COD tạo đơn ngay; VietQR chờ webhook rồi xác nhận.  
- Chi tiết contract VietQR: [DOC-ICD-01](../integrations/DOC-ICD-01-vietqr-callback.md).

### 3.3 AI

| Chức năng | Endpoint / UI | Ghi chú |
|-----------|---------------|---------|
| Size advice | Modal PDP → `/api/size-advice`, chat `/api/size-advice/chat` | Bảng đo `description_size` |
| Store Chat | FAB góc phải → `/api/store-chat` | Exact loại SP trước; similar có nhãn |

## 4. Trạng thái sẵn sàng nghiệm thu

| Mã | Trạng thái đề xuất đợt này |
|----|----------------------------|
| FN-01 … FN-10 | Sẵn sàng UAT |
| FN-11 … FN-13 | Sẵn sàng UAT (phạm vi vận hành) |

Cập nhật cột trạng thái trên biên bản nếu có hạng mục hoãn.
