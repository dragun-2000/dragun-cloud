# DOC-UC-01 — Use case cốt lõi

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-UC-01 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | BA, QA, Dev |
| **Liên quan** | DOC-BRD-01, DOC-ATP-01, DOC-UAT-04 |

---

## UC-01 — Duyệt và tìm sản phẩm

| | |
|--|--|
| **Actor** | Khách |
| **Trigger** | Vào trang chủ / menu / search |
| **BR** | BR-01…BR-04 |

**Main flow**

1. Khách chọn SALE / NEW IN / danh mục SHOP hoặc nhập từ khóa tìm kiếm.  
2. Hệ thống trả danh sách theo đúng rule lọc.  
3. Khách mở chi tiết một SP.

**Alt:** Không có kết quả → trang/list trống hoặc thông báo không có SP.

**AC:** Danh sách SALE không chứa SP `discount = 0`; NEW IN chỉ SP có category NEW IN.

---

## UC-02 — Thêm vào giỏ từ trang chi tiết

| | |
|--|--|
| **Actor** | Khách / Thành viên |
| **Trigger** | ADD TO BAG / BUY NOW |
| **BR** | BR-05 |

**Main flow**

1. Chọn màu/size/type đủ theo SP.  
2. Hệ thống resolve variation → thêm giỏ (session; DB nếu đã login).  
3. Cập nhật số túi trên header.

**Exception:** Thiếu lựa chọn bắt buộc → cảnh báo, không thêm.

**AC:** Dòng giỏ có đúng `variationId`, tên, giá, option hiển thị.

---

## UC-03 — Đặt hàng COD

| | |
|--|--|
| **Actor** | Khách / Thành viên |
| **BR** | BR-06 |

**Main flow**

1. Giỏ hợp lệ → checkout → chọn COD → gửi thông tin giao hàng.  
2. Hệ thống tạo đơn, xử lý giỏ theo policy COD.  
3. (Tuỳ cấu hình) đồng bộ Pancake / gửi email.

**AC:** Có đơn tra cứu được phía admin với đúng dòng hàng và phương thức COD.

---

## UC-04 — Thanh toán VietQR

| | |
|--|--|
| **Actor** | Khách; hệ thống VietQR (webhook) |
| **BR** | BR-07 |
| **ICD** | DOC-ICD-01 |

**Main flow**

1. Checkout chọn VietQR → hệ thống tạo phiên chờ / hiển thị QR.  
2. Khách chuyển khoản.  
3. VietQR lấy token → Transaction Sync.  
4. Debase xác nhận giao dịch và hoàn tất tạo/cập nhật đơn.

**Exception:** Token hết hạn / sai auth → sync thất bại, đơn chưa xác nhận.

**AC:** Sau sync thành công, đơn ở trạng thái thanh toán đúng; có dấu vết đối soát (AM010 nếu trong phạm vi).

---

## UC-05 — Gợi ý size trên PDP

| | |
|--|--|
| **Actor** | Khách / Thành viên |
| **BR** | BR-08, BR-10 |

**Main flow**

1. Mở modal Gợi ý size; prefill height/weight nếu login có dữ liệu.  
2. Submit → AI/rule trả recommended (+ backup lớn hơn nếu có).  
3. Có thể chat hỏi thêm trong modal kết quả.

**AC:** Size đề xuất nằm trong sizes SP; backup không nhỏ hơn recommended.

---

## UC-06 — Store Chat tư vấn sản phẩm

| | |
|--|--|
| **Actor** | Khách / Thành viên |
| **BR** | BR-11, BR-12, BR-13 |

**Main flow — exact**

1. Mở chat → hỏi loại món cụ thể (vd. áo phông).  
2. Hệ thống trả **chỉ** SP khớp loại + lời tư vấn.  
3. Khách Xem / chọn biến thể / Thêm giỏ.

**Alt — similar**

1. Không có SP exact → thông báo không có đúng loại.  
2. Hiển thị gợi ý tương tự + nhãn UI tương ứng.

**Alt — thiếu số đo**

1. Hỏi SP cần size nhưng chưa có height/weight (message/request/account).  
2. Bot hỏi bổ sung; không gắn size mặc định sai.

**AC:** “áo phông” không lẫn sơ mi trong kết quả exact; similar có nhãn; size chỉ khi đủ số đo hoặc sau khi khách cung cấp.
