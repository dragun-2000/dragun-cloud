# Plan tích hợp VNPAY-QR (môi trường TEST) — debase.vn

> Ngày: 2026-11-11 · Branch tham chiếu: `dev/vnpay` · Stack: Spring Boot 2.5.13, Java 11, PostgreSQL, Thymeleaf + jQuery
> Nguồn: tài liệu chính thống VNPAY (`/apis/docs/thanh-toan-pay/pay.html`, `/apis/docs/truy-van-hoan-tien/querydr&refund.html`) + email kết nối TEST VNPAY gửi đối tác `24J8016430`.

---

## 1. Tóm tắt thông tin kết nối đã có

| Mục | Giá trị | Ghi chú |
|---|---|---|
| Mã đối tác | `24J8016430` — HỘ KINH DOANH NGUYỄN TRUNG HIẾU 01 | |
| Website | https://debase.vn | |
| Dịch vụ | Thanh toán trực tuyến (QR VNPAY, thẻ ATM/tài khoản nội địa, thẻ quốc tế) | |
| `vnp_TmnCode` | `NTHTEST1` | ✅ đã cấu hình (dev) |
| `vnp_HashSecret` | Chỉ qua env `VNPAY_HASH_SECRET` (không commit vào repo) | ✅ đã clear khỏi properties; nạp local/server bằng env |
| Payment URL | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` | ✅ |
| Query/Refund API | `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` | ✅ |
| Return URL | `https://debase.vn/vnpay/return` | ✅ đã set (dev). IPN vẫn cần đăng ký riêng |
| SIT portal (kiểm thử IPN/Return) | `https://sandbox.vnpayment.vn/vsm-sit-client/login` | ❌ mật khẩu chưa có |
| Merchant view | `https://doitac-test.vnpaytest.vn/mcview/login` | ❌ tài khoản chưa có |

---

## 2. Phân tích tài liệu VNPAY (những điểm có hệ quả lên code)

### 2.1 Luồng chuẩn
```
Browser ──POST /api/payment/create (method=VNPAY)──▶ Server: tạo đơn AWAITING_PAYMENT + giữ kho + build URL ký HMAC
Browser ◀──── paymentUrl ──── Server
Browser ──redirect──▶ VNPAY sandbox (chọn QR / ATM / thẻ QT, nhập OTP)
VNPAY ──GET IPN (server-to-server, HTTPS)──▶ Server: verify chữ ký → cập nhật DB → trả {"RspCode","Message"}
VNPAY ──redirect browser──▶ Return URL: CHỈ verify chữ ký + hiển thị; KHÔNG cập nhật DB
```
**Quy tắc cứng:** chỉ IPN được cập nhật kết quả giao dịch. Return URL có thể đến trước/sau IPN hoặc không đến (user đóng tab).

### 2.2 Tham số tạo URL thanh toán (`vnp_Version=2.1.0`, `vnp_Command=pay`)
| Tham số | Bắt buộc | Quy tắc quan trọng |
|---|---|---|
| `vnp_TmnCode` | ✔ | `NTHTEST1` |
| `vnp_Amount` | ✔ | **VND × 100**, không thập phân (100.000đ → `10000000`) |
| `vnp_CurrCode` | ✔ | `VND` |
| `vnp_TxnRef` | ✔ | ≤100 ký tự, **duy nhất, không trùng trong ngày** |
| `vnp_OrderInfo` | ✔ | ≤255, **tiếng Việt không dấu, không ký tự đặc biệt** |
| `vnp_OrderType` | ✔ | mã danh mục hàng hoá; mặc định `other` |
| `vnp_Locale` | ✔ | `vn` / `en` |
| `vnp_ReturnUrl` | ✔ | 10–255 ký tự |
| `vnp_IpAddr` | ✔ | IP khách (lấy qua `X-Forwarded-For` — app đã bật `server.tomcat.remote-ip-header`) |
| `vnp_CreateDate` / `vnp_ExpireDate` | ✔ | `yyyyMMddHHmmss`, **GMT+7** (`Asia/Ho_Chi_Minh`, không phụ thuộc TZ của container) |
| `vnp_BankCode` | ✘ | `VNPAYQR` / `VNBANK` / `INTCARD`; bỏ trống → khách chọn trên VNPAY |
| `vnp_SecureHash` | ✔ | HMAC-SHA512 |

### 2.3 Thuật toán checksum (điểm dễ sai nhất)
1. Lấy mọi tham số `vnp_*` khác rỗng, **bỏ `vnp_SecureHash` và `vnp_SecureHashType`**.
2. Sắp xếp **tăng dần theo tên**.
3. Nối `urlencode(key)=urlencode(value)` bằng `&` — Java sample dùng `URLEncoder.encode(v, US_ASCII)` (khoảng trắng → `+`). **Chuỗi ký phải giống hệt query string gửi đi.**
4. `HmacSHA512(hashSecret, data)` → hex.
5. Khi verify IPN/Return: Spring đã decode param → phải **encode lại cùng cách** trước khi băm; so sánh không phân biệt hoa thường, dùng `MessageDigest.isEqual` (constant-time).

Query/Refund API dùng cách khác: chuỗi **nối bằng `|` theo thứ tự cố định** (không sort, không encode) — xem §2.6.

### 2.4 IPN — thứ tự kiểm tra & mã phản hồi
| Bước | Lỗi → `RspCode` |
|---|---|
| 1. Verify checksum | sai → `97` "Invalid Checksum" |
| 2. Tìm đơn theo `vnp_TxnRef` | không có → `01` "Order not found" |
| 3. So `vnp_Amount/100` với số tiền đã lưu | lệch → `04` "Invalid amount" |
| 4. Kiểm tra trạng thái đơn (idempotent) | đã xử lý → `02` "Order already confirmed" |
| 5. Cập nhật: `vnp_ResponseCode=00` **và** `vnp_TransactionStatus=00` → PAID; ngược lại → FAILED | — |
| 6. Trả `00` "Confirm Success" | exception → `99` "Unknown error" |

**Retry:** VNPAY dừng khi nhận `00` hoặc `02`; với `01/04/97/99` hoặc timeout sẽ gọi lại **tối đa 10 lần, cách 5 phút**. → IPN phải idempotent và trả nhanh. IPN URL **bắt buộc HTTPS** và public.

Lưu ý: giao dịch thất bại/khách huỷ cũng phải trả `00` sau khi ghi nhận FAILED (không phải lỗi merchant).

### 2.5 Mã kết quả cần map
- `vnp_ResponseCode`: `00` thành công · `07` trừ tiền thành công nhưng nghi ngờ · `09/10/12/13/51/65/75/79` lỗi phía khách/ngân hàng · `11` hết hạn chờ · `24` khách huỷ · `99` khác.
- `vnp_TransactionStatus`: `00` thành công · `01` chưa hoàn tất · `02` lỗi · `04` đảo (khách bị trừ tiền nhưng GD không thành công) · `05/06` đang hoàn tiền · `07` nghi ngờ gian lận · `09` hoàn bị từ chối.
- `07` (ResponseCode hoặc TransactionStatus) → **không giao hàng tự động**, đưa vào trạng thái cần admin xử lý (tái sử dụng `PAID_ISSUE`).

### 2.6 QueryDR / Refund (`POST /merchant_webapi/api/transaction`, JSON)
- `vnp_RequestId` duy nhất trong ngày; `vnp_TransactionDate` = **`vnp_CreateDate` của lần tạo thanh toán** → **phải lưu lại** khi tạo URL.
- Checksum querydr: `RequestId|Version|Command|TmnCode|TxnRef|TransactionDate|CreateDate|IpAddr|OrderInfo`.
- Checksum refund: `RequestId|Version|Command|TmnCode|TransactionType|TxnRef|Amount|TransactionNo|TransactionDate|CreateBy|CreateDate|IpAddr|OrderInfo` (`TransactionType` 02 toàn phần / 03 một phần).
- Mã phản hồi: `00` OK · `02` sai TmnCode · `03` sai định dạng · `91` không tìm thấy GD · `94` trùng request · `95` GD gốc thất bại (refund) · `97` sai checksum · `99` khác.
- ⚠ Tài liệu công thức checksum **response** querydr thiếu dấu `|` giữa `vnp_Amount` và `vnp_BankCode` → cần xác nhận với VNPAY (xem §6).

---

## 3. Nguyên tắc: VNPAY là module RIÊNG, không dùng lại code VietQR

Quyết định (2026-11-11): VNPAY không tái sử dụng service/entity/job của VietQR để sửa VietQR không ảnh hưởng VNPAY và ngược lại.

| Không dùng (thuộc VietQR) | Có dùng (domain chung, COD cũng dùng) |
|---|---|
| `VietQrCheckoutService`, `VietQrWebhookService`, `CheckoutPending`, `VietQrProperties`, `Job004`, `/api/payment/*`, `vietqr-checkout.js` | `OrderService` (tạo đơn), `InventoryReservationService` (giữ/hoàn kho), `CartService`, `PancakeSyncScheduler`, `OrderConfirmationMailScheduler`, `PaymentFulfillmentAlertScheduler`, `PaymentTransactionLogService` (màn log admin), bảng `payments` |

Điểm chạm duy nhất với code cũ:
- `payment-detail.js`: 4 dòng rẽ nhánh `paymentMethod === 'VNPAY'` → `startVnpayCheckout()`.
- `payment-detail.html`: thêm radio VNPAY + include `vnpay-checkout.js` (chỉ khi `vnpayEnabled`).
- `OrderConfirmationMailService`: nhận diện `VNPAY` là đã thanh toán online (nội dung mail VietQR giữ nguyên).
- Job004 (VietQR) vẫn quét mọi reservation HELD hết hạn → VNPAY đặt hạn giữ kho = hạn thanh toán + grace + 60 phút để Job004 không huỷ đơn VNPAY trước `VnpayExpireJob`.

## 4. Thiết kế đã triển khai (`src/main/java/vn/co/cake/vnpay/`)

| File | Vai trò |
|---|---|
| `VnpayConstants` | Mã lệnh, mã phản hồi, trạng thái, event log |
| `config/VnpayProperties` | `vnpay.*`; `isUsable()` = enabled + đủ TmnCode/HashSecret/ReturnUrl. Thiếu → ẩn VNPAY, app vẫn chạy |
| `config/VnpayModelAdvice` | Đưa `vnpayEnabled` vào trang thanh toán (không sửa `CartController`) |
| `support/VnpaySigner` | HMAC-SHA512: sort + encode US_ASCII, verify constant-time, checksum dạng `a|b|c` cho QueryDR |
| `support/VnpayFormats` | Ngày GMT+7, tiền ×100, OrderInfo không dấu |
| `entity/VnpayPayment` + `init/migration_add_vnpay_payment.sql` | Bảng `vnpay_payment` (txn_ref unique, lưu `vnp_create_date` cho QueryDR) |
| `repository/VnpayPaymentRepository` | `findByTxnRefForUpdate` (khoá dòng chống IPN song song) |
| `client/VnpayApiClient` | Tạo URL thanh toán; QueryDR + verify checksum response |
| `service/VnpayCheckoutService` | Tạo đơn `AWAITING_PAYMENT` + giữ kho + `vnpay_payment` + URL |
| `service/VnpayIpnService` | IPN: 97 → 01 → 04 → 02 → cập nhật → 00 (exception → 99, rollback) |
| `service/VnpayPaymentResultService` | Đã trả tiền → confirm kho, `PENDING_SYNC`, Pancake, mail; thất bại → `CANCELLED` + hoàn kho; `07`/hết kho → `PAYMENT_RECEIVED_UNFULFILLABLE` + alert |
| `service/VnpayExpireService` + `job/VnpayExpireJob` | Mỗi phút: PENDING quá hạn + grace → QueryDR → PAID / EXPIRED; QueryDR lỗi quá 30' → EXPIRED (IPN trễ vẫn được xử lý) |
| `api/VnpayCheckoutController` | `POST /api/vnpay/create`, `GET /api/vnpay/status/{txnRef}` |
| `api/VnpayCallbackController` | `GET /vnpay/ipn` (JSON RspCode), `GET /vnpay/return` (chỉ hiển thị) |
| `templates/vnpay-return.html`, `static/js/payment-detail/vnpay-checkout.js` | Trang kết quả (poll trạng thái server), redirect sang VNPAY |

Test: `src/test/java/vn/co/cake/vnpay/` — 21 case (chữ ký so với vector tính độc lập, định dạng, đủ nhánh RspCode IPN, IPN trễ sau khi hết hạn).

Biến môi trường (đã khai báo sẵn, giá trị còn thiếu để trống + comment cách lấy): `application-development.properties`, `application-production.properties`, `docker-compose.yml` — `VNPAY_ENABLED`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_PAY_URL`, `VNPAY_API_URL`, `VNPAY_RETURN_URL`, `VNPAY_ORDER_TYPE`, `VNPAY_EXPIRE_MINUTES`, `VNPAY_EXPIRE_GRACE_MINUTES`.

### Tình huống biên đã xử lý
1. IPN trễ sau khi đã EXPIRED → vẫn ghi nhận tiền, giữ lại kho nếu còn, không còn → PAID_ISSUE + alert; trả `00`.
2. Hết hạn → QueryDR trước khi huỷ (không huỷ nhầm đơn đã trả).
3. IPN trùng/song song → khoá dòng → `02`.
4. Sai số tiền → `04`, không cập nhật.
5. Mỗi lần đặt hàng sinh `vnp_TxnRef` mới (= mã đơn mới).
6. Ngày giờ format theo `Asia/Ho_Chi_Minh`, không phụ thuộc TZ container.

### Chưa làm (cần quyết định nghiệp vụ, §6 mục 10)
- Đơn > 1.500.000đ: rule cũ chỉ cho VietQR; nếu VietQR tắt, nút đặt hàng vẫn bị khoá dù có VNPAY.
- Refund từ admin; màn admin xem chi tiết `vnpay_payment` (hiện xem qua log thanh toán chung, `payment_method=VNPAY`).

---

## 5. Kế hoạch theo giai đoạn

| # | Việc | Kết quả nghiệm thu |
|---|---|---|
| P0 | Thu thập thông tin thiếu ở §6 | 🟡 một phần: Return URL đã có; HashSecret chỉ qua env (không commit); còn IPN đăng ký + credential SIT — xem checklist §6 |
| P1 ✅ | `VnpayProperties`, `VnpaySigner` + unit test (vector từ code mẫu Java VNPAY, encode khoảng trắng/ký tự đặc biệt, bỏ `vnp_SecureHashType`) | Test xanh; URL sinh ra mở được trang VNPAY sandbox (không lỗi "Sai chữ ký") |
| P2 ✅ (code) | Migration + entity `VnpayPayment` (bảng riêng `vnpay_payment`) | ☐ **Chưa chạy**: áp `init/migration_add_vnpay_payment.sql` lên DB test (compose đặt `ddl-auto=none`) |
| P3 ✅ (code) | `VnpayCheckoutService` + `POST /api/vnpay/create` + UI chọn VNPAY | Đặt hàng → redirect sang VNPAY, đơn ở `AWAITING_PAYMENT`, kho HELD |
| P4 ✅ (code) | `VnpayIpnService` + `/vnpay/ipn` + `VnpayPaymentResultService` (không đụng VietQrWebhookService) | Unit/integration test đủ 6 nhánh RspCode `00/01/02/04/97/99` + IPN thất bại (`24`); ☐ còn đăng ký IPN + Security permitAll |
| P5 ✅ (code) | `/vnpay/return` + trang kết quả | Return thành công/huỷ hiển thị đúng; không ghi DB |
| P6 ✅ (code) | QueryDR trong `VnpayExpireJob` riêng (không sửa Job004); log xem ở màn log thanh toán chung | Phiên hết hạn nhưng đã trả tiền được cứu, không bị huỷ; ☐ xác nhận checksum response QueryDR |
| P7 | Chạy kịch bản SIT trên `vsm-sit-client` | ☐ Toàn bộ case PASS, chụp evidence gửi VNPAY |
| P8 | (Tuỳ phạm vi) Refund toàn phần/một phần từ admin | ☐ Cần xác nhận phạm vi — xem §6 |

**Kịch bản kiểm thử SIT tối thiểu** (VNPAY chấm trên SIT portal, merchant phải trả đúng `RspCode`):
1. Giao dịch thành công → IPN `00`, đơn chuyển trạng thái, Return hiển thị thành công.
2. Giao dịch không thành công (khách huỷ `24`) → IPN `00`, đơn FAILED.
3. Gửi lại IPN đơn đã xác nhận → `02`.
4. Sai số tiền → `04`.
5. Sai chữ ký → `97`.
6. Mã đơn không tồn tại → `01`.
7. Return URL: verify chữ ký đúng/sai.

Thẻ test sandbox phổ biến của VNPAY (ngân hàng NCB, OTP `123456`) — lấy chính xác từ file zip/email kết nối hoặc trang `sandbox.vnpayment.vn/apis/vnpay-demo` trước khi test.

---

## 6. Checklist hoàn tất tích hợp TEST

Cập nhật: 2026-11-11 — sau khi nạp `vnpay.hash-secret` + `vnpay.return-url` trong `application-development.properties`.

### 6.1 Thông tin kết nối / ops

- [x] **`vnp_TmnCode`** = `NTHTEST1` (dev)
- [x] **`vnp_HashSecret`** — không commit vào repo; nạp qua env `VNPAY_HASH_SECRET` trên máy/server
- [x] **Payment URL** sandbox
- [x] **API URL** QueryDR/Refund sandbox
- [x] **Return URL** = `https://debase.vn/vnpay/return`
- [ ] **IPN URL đăng ký với VNPAY** = `https://debase.vn/vnpay/ipn` (v2.1.0 không truyền qua tham số; gửi KT VNPAY) — chặn P4/P7
- [ ] Xác nhận deploy/code trên `debase.vn` đã có endpoint `/vnpay/ipn` + `/vnpay/return` (HTTPS thật)
- [ ] **Mật khẩu SIT portal** `vsm-sit-client` (user `debase.1995@gmail.com`) — chặn P7
- [ ] **Tài khoản Merchant view** `doitac-test.vnpaytest.vn` — đối soát GD
- [ ] **Thẻ/TK test + OTP** (zip/email hoặc trang demo sandbox)
- [ ] **`vnp_OrderType` ngành thời trang** — đang dùng `other` (không chặn)
- [ ] **Danh sách IP VNPAY gọi IPN** (whitelist, tuỳ chọn)
- [ ] Xác nhận **HMAC-SHA512** cho TmnCode + công thức checksum **response QueryDR** (tài liệu thiếu `|`)
- [ ] Đối chiếu **file zip** VNPAY (code mẫu / version) với tài liệu web

### 6.2 Quyết định nghiệp vụ (chủ sản phẩm)

- [ ] a. Thời hạn thanh toán VNPAY (code default **15** phút + grace **5**)
- [ ] b. Chọn `VNPAYQR` / `VNBANK` / `INTCARD` trên web hay để khách chọn tại VNPAY (hiện **không gửi** `vnp_BankCode`)
- [ ] c. Giữ COD ≤ 1.500.000đ? Gate >1.5M có cho VNPAY khi VietQR tắt? (UI hiện vẫn khoá nếu chỉ còn VNPAY)
- [ ] d. VNPAY thay thế hay chạy song song VietQR
- [ ] e. Refund có trong phạm vi đợt này không (P8)
- [ ] f. Xử lý đơn `07` (nghi ngờ) — hiện `PAID_ISSUE` + alert

### 6.3 Việc kỹ thuật còn lại trước SIT

- [ ] Áp `init/migration_add_vnpay_payment.sql` lên DB test (volume cũ, `ddl-auto=none`)
- [ ] Thêm `VnpaySecurityConfiguration`: `permitAll` + CSRF ignore `/vnpay/ipn`, `/vnpay/return`
- [ ] Sửa gate checkout >1.5M tính thêm `vnpayEnabled` (+ copy UI)
- [ ] Return page: fallback khi poll `/api/vnpay/status` fail (hết session)
- [ ] Đồng bộ `docker-compose.yml` comment/`VNPAY_RETURN_URL` (không còn ghi [CÒN THIẾU] nếu dùng cùng debase.vn)
- [ ] Smoke: tạo đơn → redirect sandbox **không** lỗi “Sai chữ ký”
- [ ] Chạy checklist SIT §5 (7 case) + chụp evidence

### 6.4 Ngoài phạm vi đợt TEST (tuỳ quyết định §6.2.e)

- [ ] Refund toàn phần/một phần từ admin
- [ ] Màn admin chi tiết `vnpay_payment` (hiện chỉ log thanh toán chung)

> **Bảo mật:** `vnpay.hash-secret` để trống trong repo; chỉ nạp qua env. **Không** đưa secret/OTP/mật khẩu zip vào ticket hay remote công khai. Lưu ý: VietQR password / Pancake token / JWT secret vẫn đang hard-code sẵn từ trước (ngoài diff VNPAY) — nên chuyển env + rotate ở task riêng.

---

## 7. Rủi ro chính
| Rủi ro | Giảm thiểu |
|---|---|
| Sai encode khi ký → "Sai chữ ký" | Unit test với vector từ code mẫu; ký đúng chuỗi query gửi đi |
| IPN không tới (không public/HTTPS) | Có môi trường staging HTTPS trước P4; QueryDR làm đường dự phòng |
| Huỷ nhầm đơn đã trả tiền | QueryDR trước khi Job004 huỷ; nhánh PAID_ISSUE khi IPN trễ |
| Double-fulfil do IPN retry/song song | Khoá dòng + idempotent theo `txn_ref` |
| Lệch múi giờ container (UTC) | Format bằng `ZoneId.of("Asia/Ho_Chi_Minh")` |
