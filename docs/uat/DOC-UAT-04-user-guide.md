# DOC-UAT-04 — Hướng dẫn sử dụng (phục vụ UAT)

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-UAT-04 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | Tester UAT, khách hàng tham gia nghiệm thu |
| **Liên quan** | DOC-UAT-03, DOC-UAT-05, DOC-UC-01 |

---

## 1. Mục đích

Hướng dẫn **thao tác** để kiểm thử chấp nhận. Không mô tả rule nghiệp vụ chi tiết (xem [DOC-BRD-01](../business/DOC-BRD-01-business-rules.md)).

## 2. Storefront — Duyệt và tìm sản phẩm

1. Mở trang chủ.  
2. Menu **SALE** → chỉ kỳ vọng SP đang giảm giá (BR-01).  
3. Menu **NEW IN** → SP thuộc nhóm hàng mới (BR-02).  
4. Menu **SHOP → TOP / BOTTOM /…** → đúng nhóm.  
5. **Search**: nhập tên (vd. `tee`, `jean`) → danh sách kết quả.

## 3. Chi tiết sản phẩm và giỏ hàng

1. Vào một SP còn hàng.  
2. Chọn **màu** / **size** / **kiểu** (nếu có).  
3. **ADD TO BAG** hoặc **BUY NOW**.  
4. Kiểm tra số trên icon túi tăng.  
5. Vào giỏ: sửa số lượng / xóa dòng nếu hỗ trợ trên màn hình.

**Gợi ý size (FN-09):**

1. Bấm **Gợi ý size**.  
2. Nhập giới tính, chiều cao, cân nặng (hoặc để sẵn nếu đã login có profile).  
3. Xem kết quả chat; có thể hỏi thêm trong khung chat size.

## 4. Đặt hàng COD

1. Giỏ có ít nhất 1 SP hợp lệ.  
2. Điền địa chỉ / SĐT theo form checkout.  
3. Chọn **COD**.  
4. Xác nhận → nhận thông báo thành công / mã đơn (theo UI môi trường).  
5. (Admin) Tra cứu đơn vừa tạo.

## 5. Thanh toán VietQR

1. Checkout chọn phương thức chuyển khoản / VietQR.  
2. Màn hình hiển thị mã QR / hướng dẫn chuyển khoản.  
3. Thực hiện chuyển khoản trên môi trường đã cấu hình với VietQR.  
4. Hệ thống nhận webhook → đơn được tạo/xác nhận (BR-07).  
5. (Admin) Kiểm tra đơn và/hoặc AM010 lịch sử giao dịch.

Chi tiết kỹ thuật đối tác: [DOC-ICD-01](../integrations/DOC-ICD-01-vietqr-callback.md).

## 6. Tài khoản

1. **Join us** / đăng ký → đăng nhập.  
2. Profile: cập nhật chiều cao, cân nặng nếu có form.  
3. Đăng xuất / đăng nhập lại kiểm tra session.

## 7. Store Chat (De Basé Stylist)

1. Bấm nút chat góc phải dưới.  
2. Thử chip **Hàng mới** / **Sale** / **Bán chạy**.  
3. Gõ ví dụ: `áo phông` → chỉ SP phông/tee (không sơ mi trong kết quả exact).  
4. Gõ `sơ mi` → chỉ sơ mi.  
5. Nếu không có exact: thông báo không có + nhãn **Gợi ý sản phẩm tương tự**.  
6. Có chiều cao/cân nặng (trong câu hoặc profile): card chọn sẵn size gợi ý; không có thì bot hỏi thêm.  
7. **Xem** → PDP; **Thêm giỏ** sau khi chọn màu/size.

## 8. Admin (mức vận hành UAT)

1. Đăng nhập khu vực quản trị.  
2. **Sản phẩm**: mở danh sách, mở chi tiết/sửa một SP (kiểm tra categories, discount, description size).  
3. **Đơn hàng**: tìm đơn test COD / VietQR.  
4. **AM010** (nếu có trên menu): xem log giao dịch thanh toán.

## 9. Lưu ý khi UAT

- Dùng dữ liệu test thống nhất (SP có bảng đo, SP có/không discount).  
- Ghi lại thời gian, trình duyệt, tài khoản test trên checklist.  
- Lỗi AI do hết quota OpenAI: ghi Minor/điều kiện — vẫn kiểm tra fallback nếu có.
