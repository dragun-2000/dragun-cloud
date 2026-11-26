# Plan: Đồng bộ giá & khuyến mãi Pancake → Debase

- Trạng thái: **Draft — chờ TL duyệt** (Risk Level: High — thay đổi cách tính tiền và tích hợp POS)
- Base branch: `dev/compression-image`
- Tài liệu tham chiếu: Pancake POS Open API — [Product](https://pos.pancake.vn/developers/api/en/product), [Promotion](https://pos.pancake.vn/developers/api/en/promotion), [Order](https://pos.pancake.vn/developers/api/en/order), [Webhook](https://pos.pancake.vn/developers/api/en/webhook)

---

## 1. Mục tiêu và nguyên tắc

**Mục tiêu:** giá niêm yết, khuyến mãi và giá bán trên web Debase luôn khớp với Pancake sau mỗi lần đồng bộ; đơn hàng đẩy sang Pancake có tổng tiền khớp với tổng khách đã trả trên web; mọi lần đồng bộ để lại dấu vết và báo cáo thay đổi.

**Nguồn sự thật (source of truth):**

| Dữ liệu | Chủ sở hữu | Debase |
|---|---|---|
| Giá niêm yết theo biến thể (`retail_price`) | Pancake | Chỉ đọc, sao chép khi sync |
| Tồn kho | Pancake | Chỉ đọc (sync + webhook) |
| Khuyến mãi giá sản phẩm (`promotion_advance`) | Pancake | Chỉ đọc, sao chép khi sync |
| Giá bán (sau giảm) | — | **Không lưu**, luôn tính bởi `PricingService` |
| Giá tại thời điểm đặt | `OrderItem` | Snapshot, không bao giờ tính lại |

**Bất biến (invariants) phải giữ:**

1. Không có chỗ nào trên Debase ghi vào giá hoặc % giảm của sản phẩm Pancake ngoài luồng sync.
2. Mọi giá hiển thị và thu tiền đều đi qua `PricingService` (một quy tắc tính, một quy tắc làm tròn).
3. Một lần sync hoặc **thành công toàn bộ**, hoặc **không ghi gì** (không để dữ liệu nửa cũ nửa mới).
4. Sync chạy lại nhiều lần với cùng dữ liệu Pancake cho cùng kết quả (idempotent, deterministic).
5. Đơn đã tạo không bị ảnh hưởng bởi sync sau đó.

**Ngoài phạm vi:** khuyến mãi theo tổng đơn, coupon/mã giảm giá, mua kèm, tặng quà, nhân điểm; webhook giá realtime (Pancake không có sự kiện này); đồng bộ chiều Debase → Pancake.

---

## 2. Hiện trạng và lỗi cần sửa

| # | Lỗi | Vị trí | Hậu quả |
|---|---|---|---|
| L1 | `product.price` lấy từ `variationPancakes.get(0)` sau khi gom từ `HashSet` | `ProductServiceImpl.syncProductPancake` (dòng 148, 157) | Giá ngẫu nhiên giữa các lần sync khi các biến thể có giá khác nhau; khách bị tính sai giá theo size/màu |
| L2 | Giá bán tính từ `product.finalPrice`, bỏ qua `variation.retailPrice` | `OrderPricingUtil.resolveSellingUnitPrice` | Như L1 |
| L3 | Không đồng bộ khuyến mãi; giảm giá nhập tay ở AM003/AM008 | `AM003Controller`, `ProductServiceImpl.updateProductsDiscount` | Giá web lệch giá khuyến mãi trên Pancake/cửa hàng |
| L4 | `Job01.updateDiscountIfNeeded` xoá giảm giá mọi sản phẩm mỗi phút khi voucher `DISCOUNT_PRICE` hết hạn | `Job01` | Mất giảm giá đặt tay |
| L5 | Sync không xử lý `is_removed` / `is_hidden` của biến thể | `syncProductPancake` | Biến thể đã xoá/ẩn trên Pancake vẫn bán được trên web |
| L6 | Sync ghi từng phần, không transaction toàn cục, không khoá chạy song song | `PancakeController.syncProducts`, `Job01.syncPancakeData` (2 bản code trùng) | Lỗi giữa chừng → dữ liệu nửa cũ nửa mới; admin bấm 2 lần chạy chồng |
| L7 | Sync không báo đã đổi gì | — | Không phát hiện giá sai trước khi khách thấy |
| L8 | `/api/v2/pancake/sync-products` và `/webhook` nằm ngoài các security chain (`/AM/**`, `/SA/**`, `/HO/**`, `/csv/**`, `/vnpay/**`) — **cần xác minh** | `AdminSecurityConfiguration`, `UserSecurityConfiguration` | Có thể bị gọi không cần đăng nhập |
| L9 | Webhook nuốt exception, không lưu lịch sử | `PancakePosService.updateStock` | Tồn kho lệch không ai biết |
| L10 | Đơn đẩy sang Pancake dồn giảm giá vào `discount` cấp đơn, `discount_each_product = 0` | `MainOrderRequest` | Báo cáo/hoàn trả từng dòng trên Pancake sai |

---

## 3. Việc cần xác minh trước khi code (Spike — làm đầu tiên)

| Mã | Việc | Cách làm | Kết quả cần có | Chặn |
|---|---|---|---|---|
| S1 | JSON thật của `promotion_advance` | Admin chạy lệnh GET (mục 9) với shop thật | File mẫu; giá trị thật của `order_sources` cho kênh web; các `type` đang dùng; `retail_price_after_discount` có khớp `level_info` không | Task P2 |
| S2 | Pancake có **tự áp khuyến mãi** khi tạo đơn qua API không | Tạo đơn test (sản phẩm đang có KM, không gửi discount), xem tổng Pancake tính | Có/không | Task P4 |
| S3 | `discount_each_product` là theo **đơn vị** hay **dòng** | Đơn test qty = 2, `discount_each_product = 10000` | Tổng giảm = 10.000 hay 20.000 | Task P4 |
| S4 | Biến thể có giá khác nhau trong cùng sản phẩm | Query đếm sản phẩm có `MIN(retail_price) <> MAX(retail_price)` trong `variations` | Danh sách cho kinh doanh xem trước khi deploy | Rollout |
| S5 | `/api/v2/pancake/**` có cần đăng nhập không | `POST` không cookie từ máy ngoài vào môi trường test | 401/403 hay 200 | Task P0-5 |
| S6 | `category_id` trong promotion khớp với dữ liệu danh mục nào trên Debase | Đối chiếu S1 với `VariationResponse.product.categories` | Cách map danh mục → sản phẩm | Task P2 |

Đơn test S2/S3 tạo trên shop test (hoặc huỷ ngay trên shop thật), ghi lại `custom_id` để dọn.

---

## 4. Thiết kế đích

### 4.1 Dữ liệu (ddl-auto=update tạo cột/bảng mới; tất cả nullable hoặc có default để deploy không gián đoạn)

**`variations`** (đã có `retail_price`): thêm
- `is_hidden` boolean, `is_removed` boolean (từ Pancake) → biến thể không bán khi một trong hai là true
- `price_synced_at` timestamp

**`products`** (bảng đọc cho trang danh sách — read model, không dùng để thu tiền): thêm
- `min_price`, `max_price` (giá niêm yết min/max của các biến thể còn bán)
- `min_selling_price` (giá bán thấp nhất sau KM), `max_discount_percent` (int), `on_sale` (boolean)
- Giữ `price`, `discount`, `discount_price`, `final_price` cho tương thích; sync ghi `price = min_price`, `final_price = min_selling_price`; đánh dấu `@Deprecated` trong code, không dùng cho thu tiền

**`promotion_rule`** (mới)

| Cột | Kiểu | Ghi chú |
|---|---|---|
| id | bigint PK | |
| source | varchar(20) | `PANCAKE` (để mở rộng sau) |
| pancake_promotion_id | varchar(64) | `promotion_advance.id` |
| pancake_item_id | varchar(64) | `items[].id`; unique cùng `pancake_promotion_id` |
| name | varchar(255) | |
| scope | varchar(20) | `VARIATION` / `PRODUCT` / `CATEGORY` |
| variation_id | varchar(64) | khớp `variations.variation_id` |
| product_pancake_id | varchar(64) | khớp `products.product_pancake_id` |
| category_id | varchar(64) | |
| kind | varchar(20) | `PERCENT` / `AMOUNT` / `FIXED_PRICE` |
| value | decimal(15,2) | % (0–100), số tiền VND, hoặc giá đồng giá |
| start_at, end_at | datetime (UTC) | |
| priority | int | từ `priority_level`, null → 0 |
| active | boolean | `is_activated` |
| sync_run_id | bigint | lần sync tạo/cập nhật |

**`pancake_sync_run`** (mới): id, started_at, finished_at, triggered_by (username hoặc `JOB01`), mode (`DRY_RUN`/`APPLY`), status (`RUNNING`/`SUCCESS`/`FAILED`), counts (product/variation/promotion: created/updated/removed/skipped), error.

**`pancake_sync_change`** (mới): sync_run_id, entity (`VARIATION`/`PROMOTION`), pancake_id, display_id, field, old_value, new_value, note (ví dụ lý do bỏ qua KM).

### 4.2 Luồng sync mới — `PancakeCatalogSyncService.run(mode, triggeredBy)`

Một service duy nhất, dùng chung cho nút admin và `Job01` (xoá bản code trùng ở `PancakeController`).

1. **Khoá:** tạo `pancake_sync_run` status `RUNNING`; nếu đã có run `RUNNING` chưa quá 30 phút → từ chối ("đang có lần đồng bộ khác").
2. **Tải toàn bộ (chưa ghi DB):**
   - Biến thể: `GET /shops/{id}/products/variations` lặp đến hết trang. Bất kỳ trang nào lỗi/timeout → run `FAILED`, **không ghi gì**.
   - Khuyến mãi: `GET /shops/{id}/promotion_advance?page_size=100` lặp đến `total_pages`. Lỗi → `FAILED`, không ghi gì.
3. **Kiểm tra dữ liệu:** `retail_price` null/âm, biến thể trùng id, sản phẩm không còn biến thể nào → ghi cảnh báo vào `pancake_sync_change`; nếu số biến thể giảm > 30% so với lần trước → `FAILED` (bảo vệ khỏi response rỗng/lỗi phía Pancake).
4. **Lọc khuyến mãi** (quy tắc mục 4.3) → danh sách rule hợp lệ + danh sách bỏ qua kèm lý do.
5. **Tính diff** với DB hiện tại: biến thể mới/đổi giá/đổi tồn/bị ẩn hoặc xoá; rule mới/đổi/hết.
6. `mode = DRY_RUN` → lưu diff, kết thúc `SUCCESS`, **không đổi dữ liệu bán hàng**.
7. `mode = APPLY` → **một transaction**:
   - upsert `variations` (giá, tồn, field màu/size/kiểu, `is_hidden`, `is_removed`, `price_synced_at`)
   - upsert `products` (thông tin chung; danh sách màu/size chỉ từ biến thể còn bán)
   - thay toàn bộ rule `source = PANCAKE` bằng tập rule mới (xoá rule không còn trên Pancake)
   - tính lại read model `products.min_price…on_sale` qua `PricingService`
   - lưu `pancake_sync_change`, cập nhật run `SUCCESS`
8. Trả về tóm tắt cho màn admin (mục 4.6).

### 4.3 Quy tắc chọn khuyến mãi áp lên giá web

Nhận một `items[]` thành rule khi **tất cả** đúng:

- `type ∈ {discount_by_product, fixed_prices}`
- `is_activated = true` và `end_time` chưa qua (rule chưa bắt đầu vẫn lưu, `PricingService` tự bỏ qua đến `start_at`)
- `customer_tags`, `warehouse_ids`, `post_ids` rỗng; `type_employee = "all"`; `is_discount_in_birthday_customer = false`
- `order_sources` rỗng hoặc chứa nguồn web (giá trị lấy từ S1, đưa vào config `pancake.promotion.web-order-source`)
- `discount_by_quantity` không phải theo bậc: `level_info` có đúng 1 phần tử, `from_quantity ≤ 1`, `to_quantity` null/0/rất lớn
- `limited_quantity = 0`
- `attachment_product_info` rỗng, `discount_info_by_attachment_product` null

Map giá trị:
- `discount_by_product`: `level_info[0].is_percent ? PERCENT : AMOUNT`, `value = level_info[0].discount`
- `fixed_prices`: `FIXED_PRICE`, `value = items[].fixed_prices`
- Scope theo field khác null: `variation_id` → `VARIATION`, `product_id` → `PRODUCT`, `category_id` → `CATEGORY` (map theo S6)

Không thoả → ghi `pancake_sync_change` loại "bỏ qua" với lý do (để admin biết KM nào không lên web).

### 4.4 `PricingService` — nơi duy nhất tính giá

```
PriceQuote quote(Variation v, Product p, Instant at)
  listPrice   = v.retailPrice (null/≤0 → không bán, ném lỗi rõ ràng; không fallback sang product.price)
  candidates  = rule active, start_at ≤ at < end_at, khớp VARIATION(v) ∪ PRODUCT(p) ∪ CATEGORY(p)
  rule        = max theo (priority, độ cụ thể VARIATION > PRODUCT > CATEGORY, giảm nhiều hơn cho khách)
  unitDiscount = PERCENT: round_half_up(listPrice × value / 100, 0)
                 AMOUNT : min(value, listPrice)
                 FIXED  : max(listPrice − value, 0)
  sellingPrice = listPrice − unitDiscount
  return {listPrice, unitDiscount, sellingPrice, percentForDisplay, ruleId}
```

- Tiền VND số nguyên, làm tròn **trên đơn vị**, sau đó mới nhân số lượng.
- Không cộng dồn nhiều khuyến mãi (Pancake `discount_by_product` cũng không cộng dồn theo mặc định — xác nhận lại ở S1/S2).
- Thay thế toàn bộ `OrderPricingUtil.resolve*`; các nơi gọi: `CartItem`, `CartServiceImpl.toOrderItem`, `OrderServiceImpl` (tạo đơn + subtotal), `MainOrderRequest`, `HomeController` (chi tiết + danh sách), mail xác nhận đơn.
- Thời điểm `at`: lúc tạo đơn dùng thời điểm tạo đơn; snapshot vào `OrderItem.price/finalPrice/discountPrice` như hiện tại.

### 4.5 Đẩy đơn sang Pancake (phụ thuộc S2, S3)

- `variation_info.retail_price = OrderItem.price` (giá niêm yết biến thể lúc đặt).
- Nếu **S2 = Pancake KHÔNG tự áp KM**: `discount_each_product = OrderItem.discountPrice` (đơn vị hoặc × qty tuỳ S3), `is_discount_percent = false`; `discount` cấp đơn = 0.
- Nếu **S2 = Pancake TỰ áp KM**: không gửi discount từ Debase; sau khi tạo đơn, đọc lại tổng Pancake trả về và so với tổng Debase — lệch thì gắn cờ đơn `PRICE_MISMATCH` + email admin.
- Trong cả hai trường hợp: lưu tổng Pancake trả về để đối soát.

### 4.6 Màn admin

- **AM003**: nút "Đồng bộ Pancake" → gọi `POST /AM/AM003/pancake-sync?mode=DRY_RUN` trước, hiện bảng thay đổi (giá cũ → mới, KM mới/hết, biến thể bị ẩn, KM bị bỏ qua kèm lý do); admin bấm "Áp dụng" → `mode=APPLY`. Có trang lịch sử các lần sync.
- **AM003 sửa sản phẩm**: ô giá và % giảm của sản phẩm Pancake chuyển chỉ đọc, ghi chú "Sửa trên Pancake rồi đồng bộ".
- **AM008**: chuyển thành màn **xem** giá/KM theo biến thể (giá niêm yết, KM đang áp, nguồn KM, giá bán, thời hạn). Bỏ chức năng cập nhật giảm giá (hoặc ẩn sau cờ cấu hình trong giai đoạn chuyển tiếp).

### 4.7 Website

- Trang chi tiết: JSON `variations` kèm `listPrice`, `sellingPrice`, `discountPercent` (từ `PricingService`); JS đổi giá/nhãn khi chọn màu/size; biến thể `is_hidden/is_removed` không hiện.
- Danh sách và thẻ sản phẩm: "từ {min_selling_price}", nhãn "Giảm đến {max_discount_percent}%".
- Trang SALE/BLACK FRIDAY: lọc `on_sale = true` (thay `discount > 0`). Read model được tính lại khi sync và bởi job nhẹ mỗi 5 phút **chỉ** cho rule vừa bắt đầu/kết thúc (không đụng dữ liệu bán hàng).

### 4.8 Bảo mật và độ tin cậy

- Bỏ `/api/v2/pancake/sync-products`; thay bằng `/AM/AM003/pancake-sync` (ROLE_ADMIN, CSRF).
- `/api/v2/pancake/webhook`: kiểm tra secret/IP Pancake (theo khả năng Pancake hỗ trợ), lưu `WebhookHistory`, log lỗi thay vì nuốt, bỏ qua sự kiện trùng.
- Gỡ `Job01.updateDiscountIfNeeded` và việc dùng voucher `DISCOUNT_PRICE` làm công tắc giảm giá.
- `Job01.syncPancakeData` (5h sáng) gọi `PancakeCatalogSyncService.run(APPLY, "JOB01")`; có cờ cấu hình để tắt.
- Timeout + retry có giới hạn khi gọi Pancake; không log `api_key`.

---

## 5. Kế hoạch triển khai (task theo thứ tự)

Mỗi task: viết test trước, thấy đỏ, rồi code. Ước lượng theo ngày-người.

### Phase 0 — chặn lỗi nguy hiểm (≈ 2 ngày)

| Task | Nội dung | File chính | Test |
|---|---|---|---|
| P0-1 | Spike S1–S6 | — | Kết quả ghi vào mục 3 tài liệu này |
| P0-2 | Gỡ `Job01.updateDiscountIfNeeded` | `Job01` | Job không còn đổi `products` |
| P0-3 | Sync tôn trọng `is_hidden/is_removed`; biến thể đó không vào giỏ/checkout | `Variation`, `VariationResponse`, `syncProductPancake`, `OrderServiceImpl`, `CartServiceImpl` | Unit: biến thể removed bị từ chối khi đặt |
| P0-4 | Giá deterministic: `product.price = min(retail_price)` của biến thể còn bán | `syncProductPancake`, `Product.toUpdate` | Unit: thứ tự input khác nhau → cùng kết quả |
| P0-5 | Chuyển endpoint sync vào `/AM/...` ROLE_ADMIN; webhook log lỗi + lưu lịch sử | `PancakeController`, `AM003Controller`, `AM003.js`, `PancakePosService` | Controller test 403 khi không phải ADMIN |

### Phase 1 — sync an toàn + giá theo biến thể (≈ 4 ngày)

| Task | Nội dung | Test |
|---|---|---|
| P1-1 | Bảng `pancake_sync_run`, `pancake_sync_change`; `PancakeCatalogSyncService` (khoá, tải hết rồi mới ghi, kiểm tra dữ liệu, diff, DRY_RUN/APPLY, 1 transaction) | Fixture JSON: lỗi trang 2 → không ghi; chạy 2 lần → không đổi; giảm >30% biến thể → FAILED |
| P1-2 | `PricingService` (chưa có rule) thay `OrderPricingUtil`; giá thu tiền = `variation.retailPrice` | Hai biến thể cùng sản phẩm, giá khác → giỏ/đơn tính đúng từng giá |
| P1-3 | Read model `products.min_price…`; trang danh sách "từ X" | Unit tính read model |
| P1-4 | Trang chi tiết đổi giá theo biến thể | Template test + kiểm tra thủ công 3 kích thước màn hình |
| P1-5 | Màn admin DRY_RUN → Áp dụng + lịch sử sync | Controller test, template test |

### Phase 2 — đồng bộ khuyến mãi (≈ 4 ngày)

| Task | Nội dung | Test |
|---|---|---|
| P2-1 | DTO `PromotionAdvanceResponse` + client phân trang | Fixture từ S1 parse đủ field |
| P2-2 | Bộ lọc + map rule (mục 4.3), báo cáo KM bị bỏ qua | Bảng test mỗi điều kiện loại trừ |
| P2-3 | Bảng `promotion_rule`; sync thay thế rule nguồn PANCAKE trong transaction | Rule bị xoá trên Pancake → biến mất trên Debase |
| P2-4 | `PricingService` áp rule (độ ưu tiên, %, số tiền, đồng giá, thời hạn) | Bảng test: trước/sau thời hạn, chồng rule, làm tròn |
| P2-5 | Read model `on_sale`, `max_discount_percent`; trang SALE lọc `on_sale`; job 5 phút cho rule bắt đầu/kết thúc | Unit + test lọc repository |
| P2-6 | AM003/AM008 chỉ đọc giá/KM sản phẩm Pancake | Controller test từ chối ghi |

### Phase 3 — đơn hàng và đối soát (≈ 2–3 ngày)

| Task | Nội dung | Test |
|---|---|---|
| P3-1 | `MainOrderRequest` gửi giá/giảm theo dòng theo kết quả S2/S3 | Snapshot JSON payload |
| P3-2 | So tổng Pancake trả về vs Debase khi tạo đơn; cờ `PRICE_MISMATCH` + email | Unit với response giả |
| P3-3 | Báo cáo đối soát ngày (dùng `Job003` đã kéo đơn mỗi giờ) | Unit |
| P3-4 | E2E: sync fixture → xem giá trang chi tiết → đặt hàng → payload đúng | E2E (nếu có môi trường test Pancake) hoặc integration test với Pancake giả |

**Tổng ≈ 12–13 ngày-người.** Có thể tách P0 thành MR riêng để deploy sớm.

---

## 6. Kiểm thử

- **Unit:** `PricingService` (bảng trường hợp), bộ lọc KM, map scope, read model, diff sync.
- **Integration với Pancake giả:** fixture JSON lấy từ S1 + biến thể thật (đã ẩn thông tin nhạy cảm) cho các ca: thành công, lỗi giữa chừng, response rỗng, biến thể bị ẩn, KM hết hạn, KM theo danh mục.
- **Đơn test trên Pancake** (S2/S3 và nghiệm thu P3): qty 1 và 2, có/không KM.
- **Thủ công:** trang chi tiết/danh sách/SALE ở PC (≥1200px), tablet và mobile (≤1199px dùng menu hamburger).
- **Hồi quy:** toàn bộ `mvn test` xanh; đơn cũ hiển thị giá như trước (đọc snapshot).

---

## 7. Triển khai và rollback

1. Deploy P0 (an toàn, không đổi giá trừ L1 — kèm danh sách S4 đã được kinh doanh duyệt).
2. Deploy P1+P2 với cờ `pricing.engine=LEGACY` (mặc định): code mới chạy nhưng thu tiền vẫn theo cách cũ.
3. Chạy sync **DRY_RUN** trên production, kinh doanh đối chiếu báo cáo thay đổi với Pancake.
4. Chạy sync **APPLY**, bật `pricing.engine=PANCAKE` ngoài giờ cao điểm; theo dõi 24–48h đơn mới (so tổng với Pancake).
5. **Rollback:** đặt lại `pricing.engine=LEGACY` (không cần deploy); bảng mới không ảnh hưởng luồng cũ; đơn đã tạo giữ snapshot.
6. Sau 2 tuần ổn định: xoá code legacy (`OrderPricingUtil` cũ, `updateProductsDiscount`, voucher `DISCOUNT_PRICE`).

---

## 8. Câu hỏi cần chốt

1. **Pancake là nguồn duy nhất cho giảm giá web?** (Plan này giả định CÓ → AM008 chỉ còn xem. Nếu cần KM riêng cho web, thêm `source = DEBASE` vào `promotion_rule` với quy tắc ưu tiên rõ ràng — không khuyến nghị.)
2. Khi nhiều KM cùng áp một biến thể: ưu tiên `priority_level` → độ cụ thể → giảm nhiều hơn cho khách. Đồng ý?
3. KM bị bỏ qua (theo bậc số lượng, theo nhóm khách…) có cần hiển thị cảnh báo đỏ trên màn admin không?
4. Thay đổi AM008 (cho phép 0%, số nguyên 0–99) và setting menu SALE/BLACK FRIDAY: giữ setting menu (độc lập), còn chức năng nhập giảm giá AM008 sẽ bị thay ở P2-6.
5. Ai duyệt danh sách S4 (sản phẩm đổi giá khi chuyển sang giá theo biến thể)?

---

## 9. Lệnh lấy mẫu dữ liệu (admin tự chạy, không dán API key vào chat/ticket)

```powershell
$base = "https://pos.pages.fm/api/v1/shops/<SHOP_ID>"; $key = "<API_KEY>"
$v = Invoke-RestMethod "$base/products/variations?api_key=$key&page_number=1"; $v.data | Select-Object -First 5 | ConvertTo-Json -Depth 8 | Out-File variations-sample.json -Encoding utf8
$p = Invoke-RestMethod "$base/promotion_advance?api_key=$key&page=1&page_size=50"; "pages=$($p.total_pages) total=$($p.total_entries)"; $p.data | ConvertTo-Json -Depth 8 | Out-File promotion-sample.json -Encoding utf8
```

```sql
-- S4: sản phẩm có biến thể khác giá
SELECT pancake_product_id, COUNT(*) AS so_bien_the, MIN(retail_price) AS gia_min, MAX(retail_price) AS gia_max
FROM variations
WHERE deleted = 0
GROUP BY pancake_product_id
HAVING MIN(retail_price) <> MAX(retail_price);
```
