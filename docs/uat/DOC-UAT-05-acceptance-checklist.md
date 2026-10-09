# DOC-UAT-05 — Checklist nghiệm thu

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-UAT-05 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | Ban nghiệm thu (ký xác nhận từng mục) |
| **Liên quan** | DOC-UAT-02, DOC-ATP-01, DOC-UAT-06 |

---

## Thông tin phiên UAT

| Trường | Điền khi nghiệm thu |
|--------|---------------------|
| Ngày giờ | |
| Môi trường / URL | |
| Người thực hiện (Bên A) | |
| Người chứng kiến (Bên B) | |
| Trình duyệt / thiết bị | |
| Phiên bản phần mềm / build | |

**Cột Đạt:** `Đ` = Đạt · `K` = Không đạt · `H` = Hoãn · `N/A` = Không áp dụng

---

## A. Danh mục & tìm kiếm

| STT | Hạng mục | Cách kiểm | Kỳ vọng | FN/TC | Đạt | Ghi chú |
|-----|----------|-----------|---------|-------|-----|---------|
| A1 | Menu SALE | Mở /products/group/SALE | Chỉ SP đang giảm giá | FN-01, TC-01 | | |
| A2 | Menu NEW IN | Mở NEW IN | SP thuộc nhóm NEW IN | FN-02, TC-02 | | |
| A3 | BEST SELLER | Mở BEST SELLER | Có danh sách / đúng nhóm | FN-03 | | |
| A4 | Nhóm TOP | Mở SHOP → TOP | Danh sách áo/top | FN-03 | | |
| A5 | Tìm kiếm | Search tên SP có trên site | Kết quả chứa tên | FN-04, TC-03 | | |

## B. Chi tiết sản phẩm & giỏ

| STT | Hạng mục | Cách kiểm | Kỳ vọng | FN/TC | Đạt | Ghi chú |
|-----|----------|-----------|---------|-------|-----|---------|
| B1 | Chọn biến thể | Chọn màu + size | UI phản hồi chọn | FN-05 | | |
| B2 | Thêm giỏ | ADD TO BAG | Bag count tăng; vào giỏ thấy dòng | FN-05, TC-04 | | |
| B3 | Thiếu chọn size | Không chọn size (nếu bắt buộc) | Cảnh báo / không thêm ẩu | FN-05 | | |
| B4 | Gợi ý size | Form cao/nặng → submit | Có size đề xuất hợp lý; backup không nhỏ hơn recommended | FN-09, TC-10 | | |

## C. Đặt hàng & thanh toán

| STT | Hạng mục | Cách kiểm | Kỳ vọng | FN/TC | Đạt | Ghi chú |
|-----|----------|-----------|---------|-------|-----|---------|
| C1 | COD | Checkout COD đủ thông tin | Đơn tạo thành công | FN-06, TC-05 | | |
| C2 | VietQR — hiển thị QR | Chọn VietQR | Có QR / hướng dẫn | FN-07, TC-06 | | |
| C3 | VietQR — xác nhận | Chuyển khoản test + webhook | Đơn xác nhận / trạng thái đúng | FN-07, TC-07 | | |
| C4 | Admin đơn | Tra đơn C1/C3 | Thấy đơn tương ứng | FN-12 | | |
| C5 | AM010 | Mở lịch sử TT (nếu có) | Thấy log giao dịch test | FN-13 | | |

## D. Tài khoản

| STT | Hạng mục | Cách kiểm | Kỳ vọng | FN/TC | Đạt | Ghi chú |
|-----|----------|-----------|---------|-------|-----|---------|
| D1 | Đăng ký / đăng nhập | Tạo user test | Vào được khu vực thành viên | FN-08 | | |
| D2 | Profile height/weight | Lưu số đo (nếu có UI) | Lưu được; chat/size dùng lại được | FN-08, BR-08 | | |

## E. Store Chat (AI)

| STT | Hạng mục | Cách kiểm | Kỳ vọng | FN/TC | Đạt | Ghi chú |
|-----|----------|-----------|---------|-------|-----|---------|
| E1 | Mở chat | Bấm FAB | Panel mở, có chip gợi ý | FN-10 | | |
| E2 | Chip SALE | Bấm Sale | SP discount > 0 | TC-08 | | |
| E3 | Chip NEW IN | Bấm Hàng mới | SP category NEW IN | TC-09 | | |
| E4 | Exact áo phông | Hỏi “áo phông” | Chỉ tee/phông; **không** sơ mi trong exact | TC-11 | | |
| E5 | Exact sơ mi | Hỏi “sơ mi” | Chỉ sơ mi | TC-12 | | |
| E6 | Similar | Case hết exact (hoặc data giả lập) | Báo không có + nhãn “Gợi ý sản phẩm tương tự” | TC-13 | | |
| E7 | Size trong chat | Hỏi SP + “1m65 55kg” | Có recommendedSize; không mặc định XL sai | TC-14 | | |
| E8 | Thiếu số đo | Hỏi SP không kèm đo, guest không profile | Bot hỏi chiều cao/cân nặng | TC-15 | | |
| E9 | Thêm giỏ từ card | Chọn màu/size → Thêm giỏ | Vào giỏ thành công | TC-16 | | |
| E10 | Xem chi tiết | Bấm Xem | Vào `/products?id=…` đúng SP | TC-17 | | |

## F. Tổng kết phiên

| Mục | Giá trị |
|-----|---------|
| Số mục Bắt buộc Đạt | … / … |
| Số Blocker mở | |
| Số Major mở | |
| Kết luận đề xuất | □ Đạt · □ Đạt có điều kiện · □ Không đạt · □ Hoãn |
| Chữ ký Bên giao | |
| Chữ ký Bên nhận | |

Chi tiết kịch bản: [DOC-ATP-01](../testing/DOC-ATP-01-acceptance-scenarios.md).  
Chuyển kết luận sang [DOC-UAT-06](DOC-UAT-06-acceptance-minutes-template.md).
