# DOC-UAT-02 — Phạm vi và tiêu chí nghiệm thu

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-UAT-02 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | Ban nghiệm thu, PM, BA |
| **Liên quan** | DOC-UAT-03, DOC-UAT-05, DOC-BRD-01 |

---

## 1. Mục đích

Xác định **phạm vi In/Out**, phụ thuộc bên thứ ba và **tiêu chí đạt / không đạt** cho đợt nghiệm thu hệ thống De Basé.

## 2. Trong phạm vi (In Scope)

| STT | Hạng mục | Tham chiếu |
|-----|----------|-----------|
| 1 | Storefront: menu SALE / NEW IN / BEST SELLER / SHOP, tìm kiếm, chi tiết SP | FN-01…FN-04, BR-01…BR-03 |
| 2 | Giỏ hàng guest & thành viên; thêm từ PDP và Store Chat | FN-05, UC-02 |
| 3 | Đặt hàng COD | FN-06, UC-03 |
| 4 | Thanh toán VietQR (tạo QR, chờ xác nhận, trạng thái đơn) | FN-07, UC-04, DOC-ICD-01 |
| 5 | Tài khoản: đăng ký / đăng nhập / cập nhật profile (gồm chiều cao, cân nặng nếu có) | FN-08 |
| 6 | AI Gợi ý size trên trang chi tiết sản phẩm | FN-09, UC-05, BR-10 |
| 7 | AI Store Chat: tư vấn SP đúng loại; similar khi hết exact; size khi có số đo | FN-10, UC-06, BR-11…BR-13 |
| 8 | Admin: quản lý sản phẩm, đơn hàng; xem log giao dịch thanh toán (AM010) | FN-11…FN-13 |
| 9 | Đồng bộ nghiệp vụ với Pancake (đơn/SP — mức hành vi quan sát được) | DOC-ICD-02 |

## 3. Ngoài phạm vi (Out of Scope)

| STT | Hạng mục | Ghi chú |
|-----|----------|---------|
| 1 | App mobile native | Không thuộc đợt này |
| 2 | Cổng thanh toán MoMo / VNPay | Đã thay bằng VietQR cho chuyển khoản |
| 3 | Đồng bộ MID/TID VietQR, xuất Excel AM010 | Theo design nội bộ — không bắt buộc UAT |
| 4 | Phân tích sự cố hạ tầng / Docker timeout lịch sử | Chỉ lưu `docs/archive/` |
| 5 | Chuẩn coding, detail design nội bộ | `docs/dev/` — không nghiệm thu với KH |
| 6 | Nội dung marketing / CMS ngoài admin hiện có | Không mở rộng trong đợt này |

## 4. Phụ thuộc bên thứ ba

| Hệ thống | Vai trò trong UAT | Rủi ro nếu gián đoạn |
|----------|-------------------|----------------------|
| **VietQR** | Token + Transaction Sync; API tạo QR | Không xác nhận thanh toán chuyển khoản |
| **Pancake POS** | Đồng bộ đơn / tồn kho nghiệp vụ | Đơn web OK nhưng POS chưa phản ánh |
| **OpenAI** | Size advice & Store Chat | Fallback rule/bảng size; hoặc thông báo lỗi thân thiện |
| **Email (SendGrid…)** | Xác nhận đơn / quên mật khẩu | Kiểm tra theo cấu hình môi trường |

Tiêu chí nghiệm thu **phải ghi rõ** môi trường và trạng thái kết nối đối tác tại thời điểm test (DOC-UAT-06).

## 5. Tiêu chí đạt (Acceptance Criteria tổng)

Hệ thống được coi là **Đạt** khi:

1. Mọi mục **Bắt buộc** trên [DOC-UAT-05](DOC-UAT-05-acceptance-checklist.md) đánh **Đạt**.  
2. Không còn lỗi **Blocker** (không mua được hàng, sai tiền nghiêm trọng, lộ dữ liệu thanh toán).  
3. Các rule nghiệp vụ cốt lõi trong [DOC-BRD-01](../business/DOC-BRD-01-business-rules.md) (SALE, NEW IN, chat exact/similar) được xác nhận bằng [DOC-ATP-01](../testing/DOC-ATP-01-acceptance-scenarios.md).  
4. Hai bên ký [DOC-UAT-06](DOC-UAT-06-acceptance-minutes-template.md) với kết luận **Nghiệm thu đạt** hoặc **Đạt có điều kiện** (kèm danh sách tồn đọng thời hạn).

## 6. Tiêu chí không đạt / tạm hoãn

| Mức | Định nghĩa |
|-----|------------|
| **Không đạt** | Có Blocker chưa khắc phục; hoặc > số mục Bắt buộc thất bại theo thỏa thuận đợt UAT |
| **Đạt có điều kiện** | Chỉ còn lỗi Minor/Cosmetic có kế hoạch sửa và thời hạn ghi trong biên bản |
| **Tạm hoãn** | Môi trường / đối tác bên thứ ba không sẵn sàng — ghi rõ nguyên nhân khách quan |

## 7. Phân loại mức độ lỗi (tham chiếu checklist)

| Mức | Ví dụ |
|-----|--------|
| Blocker | Không thêm giỏ; không tạo đơn; VietQR webhook không tạo đơn khi đã chuyển khoản thành công (trên môi trường đã cấu hình đúng) |
| Major | SALE hiển thị SP không giảm giá; chat “áo phông” trả lẫn sơ mi như exact |
| Minor | Copy UI sai chính tả; delay AI lâu nhưng có kết quả đúng |
| Cosmetic | Khoảng cách layout, icon |

## 8. Đầu vào / đầu ra nghiệm thu

**Đầu vào:** tài khoản test, dữ liệu SP mẫu (có `description_size`, có/không discount, category NEW IN), cấu hình VietQR sandbox/prod thống nhất.

**Đầu ra:** checklist đã điền (DOC-UAT-05), biên bản ký (DOC-UAT-06), danh sách bug (nếu có) kèm mức độ.
