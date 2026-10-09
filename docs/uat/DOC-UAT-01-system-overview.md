# DOC-UAT-01 — Tổng quan hệ thống De Basé

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-UAT-01 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | Khách hàng, Ban nghiệm thu, Ban lãnh đạo dự án |
| **Liên quan** | DOC-UAT-02, DOC-ADD-01, DOC-BRD-01 |

---

## 1. Mục đích tài liệu

Mô tả ngắn gọn hệ thống De Basé phục vụ **nghiệm thu** và định hướng đọc các tài liệu chi tiết. Không thay thế đặc tả kỹ thuật nội bộ ([DOC-DEV-02](../dev/DOC-DEV-02-vietqr-detail-design.md)).

## 2. Giới thiệu sản phẩm

**De Basé** (website: https://debase.vn) là hệ thống thương mại điện tử thời trang, cho phép khách hàng duyệt sản phẩm, chọn biến thể (màu/size), thêm giỏ, đặt hàng và thanh toán; đồng thời hỗ trợ quản trị nội dung sản phẩm, đơn hàng và tích hợp đối tác.

Hệ thống phần mềm trong repo **dragun-cloud** gồm:

| Phân hệ | Vai trò |
|---------|---------|
| **Storefront** | Trải nghiệm mua hàng trên web (Thymeleaf) |
| **Admin** | Quản trị sản phẩm, đơn hàng, cấu hình thanh toán |
| **Thanh toán** | COD và chuyển khoản qua VietQR |
| **Tích hợp** | Đồng bộ với Pancake POS; email thông báo |
| **AI tư vấn** | Gợi ý size trên trang chi tiết; chatbot stylist toàn site |

## 3. Đối tượng người dùng

| Actor | Mô tả |
|-------|--------|
| Khách (guest) | Duyệt SP, giỏ session, đặt hàng theo luồng được phép |
| Thành viên (đã đăng nhập) | Profile, chiều cao/cân nặng phục vụ tư vấn size, giỏ đồng bộ |
| Admin / Staff | Quản lý SP, đơn, cấu hình VietQR, xem lịch sử thanh toán |

## 4. Module chức năng chính

1. **Danh mục & tìm kiếm** — SALE, NEW IN, BEST SELLER, nhóm SHOP (TOP, BOTTOM…).  
2. **Chi tiết sản phẩm** — chọn màu/size, thêm giỏ, gợi ý size AI.  
3. **Giỏ hàng & đặt hàng** — cập nhật số lượng, địa chỉ giao, voucher (nếu áp dụng).  
4. **Thanh toán** — COD hoặc VietQR (quét QR / chờ webhook xác nhận).  
5. **Tài khoản** — đăng ký, đăng nhập, hồ sơ.  
6. **Chat tư vấn (Store Chat)** — gợi ý SP đúng loại hỏi; size theo bảng đo khi có số đo.  
7. **Admin** — CRUD sản phẩm, theo dõi đơn, cấu hình thanh toán.

Chi tiết mã chức năng: [DOC-UAT-03](DOC-UAT-03-function-catalog.md). Quy tắc nghiệp vụ: [DOC-BRD-01](../business/DOC-BRD-01-business-rules.md).

## 5. Công nghệ (tóm tắt)

| Lớp | Công nghệ |
|-----|-----------|
| Ứng dụng | Java, Spring Boot, Thymeleaf |
| Dữ liệu | PostgreSQL, Redis (session/giỏ) |
| AI | OpenAI (GPT) qua cấu hình `openai.*` |
| Tích hợp | VietQR Partner API, Pancake POS |
| Triển khai | Docker (ứng dụng + dịch vụ phụ trợ) |

Kiến trúc xem [DOC-ADD-01](../system/DOC-ADD-01-architecture-overview.md).

## 6. Môi trường tham chiếu

| Môi trường | URL tham chiếu | Ghi chú |
|------------|---------------|---------|
| Production | https://debase.vn | Nghiệm thu trên môi trường thống nhất với KH |
| Nội bộ / staging | Theo biên bản từng đợt | Ghi rõ trong DOC-UAT-06 |

## 7. Tài liệu đọc tiếp (thứ tự đề xuất)

1. [DOC-UAT-02 — Phạm vi và tiêu chí](DOC-UAT-02-scope-and-criteria.md)  
2. [DOC-UAT-03 — Danh mục chức năng](DOC-UAT-03-function-catalog.md)  
3. [DOC-UAT-04 — Hướng dẫn sử dụng](DOC-UAT-04-user-guide.md)  
4. [DOC-UAT-05 — Checklist nghiệm thu](DOC-UAT-05-acceptance-checklist.md)  
5. [DOC-UAT-06 — Biên bản nghiệm thu](DOC-UAT-06-acceptance-minutes-template.md)  
