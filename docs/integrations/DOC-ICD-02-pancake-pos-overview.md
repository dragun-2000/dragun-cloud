# DOC-ICD-02 — Pancake POS — Tổng quan tích hợp

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-ICD-02 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | BA, SA, vận hành |
| **Liên quan** | DOC-ADD-01, DOC-UAT-02, UC-03, UC-04 |

---

## 1. Mục đích

Mô tả **vai trò nghiệp vụ** tích hợp Pancake POS với De Basé phục vụ nghiệm thu. Không mô tả sự cố hạ tầng lịch sử (xem `docs/archive/` nếu cần).

## 2. Vai trò

| Hướng | Nội dung nghiệp vụ |
|-------|-------------------|
| Debase → Pancake | Đồng bộ đơn hàng sau checkout (COD / sau VietQR xác nhận); đồng bộ thông tin SP/biến thể theo job hoặc thao tác admin |
| Pancake → Debase | Dữ liệu SP/tồn kho nguồn POS (qua sync) phản ánh lên website |

## 3. Điểm chạm quan sát được khi UAT

1. Sau **COD** thành công: đơn xuất hiện trên admin Debase; trên Pancake (nếu môi trường đã cấu hình API key) có đơn tương ứng trong thời gian chấp nhận được.  
2. Sau **VietQR** sync thành công: tương tự, đơn prepaid/đồng bộ theo thiết kế hiện hành.  
3. SP trên web có `productPancakeId` / variation liên kết — thêm giỏ dùng `variationId` từ POS.

## 4. Phạm vi nghiệm thu

- **Trong scope:** hành vi “đơn web tạo được và đồng bộ khi cấu hình đúng”.  
- **Ngoài scope:** tối ưu timeout Docker, WAF, phân tích NoHttpResponseException — tài liệu kỹ thuật nội bộ/archive.

## 5. Phụ thuộc cấu hình

- API key / shop id Pancake trên môi trường UAT.  
- Job đồng bộ (nếu bật) — ghi trạng thái môi trường trong biên bản DOC-UAT-06.

## 6. Trace

Checklist C1/C3/C4 ([DOC-UAT-05](../uat/DOC-UAT-05-acceptance-checklist.md)) xác nhận đơn phía Debase; đối soát Pancake ghi chú theo điều kiện môi trường.
