# DOC-ATP-01 — Kịch bản chấp nhận (Acceptance Test Scenarios)

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-ATP-01 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | QA, Ban nghiệm thu |
| **Liên quan** | DOC-BRD-01, DOC-UC-01, DOC-UAT-05 |

---

## 1. Mục đích

Mô tả **scenario kiểm thử chấp nhận** (`TC-xxx`) map tới BR/UC và dòng checklist DOC-UAT-05. Không thay checklist ký.

## 2. Ma trận trace

| TC | Tên ngắn | BR | UC | Checklist |
|----|----------|----|----|-----------|
| TC-01 | SALE chỉ SP giảm giá | BR-01 | UC-01 | A1 |
| TC-02 | NEW IN đúng category | BR-02 | UC-01 | A2 |
| TC-03 | Search theo tên | BR-04 | UC-01 | A5 |
| TC-04 | Thêm giỏ có variation | BR-05 | UC-02 | B2 |
| TC-05 | Đặt COD thành công | BR-06 | UC-03 | C1 |
| TC-06 | VietQR hiển thị QR | BR-07 | UC-04 | C2 |
| TC-07 | VietQR sync tạo/xác nhận đơn | BR-07 | UC-04 | C3 |
| TC-08 | Chat chip Sale | BR-01, BR-11 | UC-06 | E2 |
| TC-09 | Chat chip NEW IN | BR-02 | UC-06 | E3 |
| TC-10 | Size PDP backup không nhỏ hơn | BR-10 | UC-05 | B4 |
| TC-11 | Chat exact áo phông | BR-11 | UC-06 | E4 |
| TC-12 | Chat exact sơ mi | BR-11 | UC-06 | E5 |
| TC-13 | Chat similar khi hết exact | BR-12 | UC-06 | E6 |
| TC-14 | Chat size có số đo | BR-13 | UC-06 | E7 |
| TC-15 | Chat hỏi số đo khi thiếu | BR-13 | UC-06 | E8 |
| TC-16 | Chat thêm giỏ | BR-05 | UC-06 | E9 |
| TC-17 | Chat mở PDP | — | UC-06 | E10 |

## 3. Chi tiết scenario trọng điểm

### TC-01 — SALE

- **Given** có SP A `discount > 0`, SP B `discount = 0`.  
- **When** mở menu SALE.  
- **Then** thấy A, không thấy B như SP sale hợp lệ.

### TC-11 — Exact áo phông

- **Given** catalog có Logo Tee / Áo phông và có Áo sơ mi.  
- **When** chat: “áo phông” / “tee”.  
- **Then** card exact chỉ tee/phông; không có sơ mi; `similarSuggestion = false`.

### TC-13 — Similar

- **Given** không có SP tên khớp loại đang hỏi (hoặc môi trường test đã loại hết exact).  
- **When** hỏi đúng loại đó.  
- **Then** message chứa ý không có đúng loại; có nhãn “Gợi ý sản phẩm tương tự”; SP gợi ý không thuộc nhóm exclude của loại hỏi (vd. hỏi phông không đưa sơ mi như exact).

### TC-14 — Size trong chat

- **Given** hỏi “áo nỉ nam 1m50 58kg” (hoặc tương đương).  
- **Then** card có `recommendedSize` thuộc sizes SP; không mặc định sáng size lớn nhất/XL nếu chart chỉ ra size nhỏ hơn.

### TC-07 — VietQR sync

- **Given** cấu hình Partner đúng trên môi trường UAT.  
- **When** hoàn tất chuyển khoản test và VietQR gọi token + transaction-sync hợp lệ.  
- **Then** đơn được xác nhận; đối soát được trên admin/AM010 theo phạm vi.

## 4. Dữ liệu test đề xuất

| Nhóm | Yêu cầu dữ liệu |
|------|-----------------|
| SALE | ≥1 SP discount > 0; ≥1 SP discount = 0 để đối chứng |
| NEW IN | ≥1 SP `categories` chứa `NEW IN` |
| Chat exact | ≥1 tee/phông; ≥1 sơ mi |
| Size | SP có `description_size` và `sizes` rõ |
| VietQR | Tài khoản Partner + môi trường đồng bộ với đối tác |

## 5. Ghi nhận kết quả

Kết quả từng TC đánh dấu trên [DOC-UAT-05](../uat/DOC-UAT-05-acceptance-checklist.md); không nhân bản bảng ký tại đây.
