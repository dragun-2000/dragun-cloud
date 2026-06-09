# Tài liệu so sánh thay đổi: old.json vs new.json

> **Ngày tạo:** 2026-06-05 14:28  
> **Nguồn TRƯỚC:** `docs/old.json`  
> **Nguồn SAU:** `docs/new.json`  
> **Sắp xếp theo:** `results.id` (tăng dần)  

---

## 1. Tổng quan

| Hạng mục | Giá trị |
|---|---|
| Tổng số bản ghi `results` | 70 |
| Số bản ghi có trong old.json | 70 |
| Số bản ghi có trong new.json | 70 |
| Số trường header thay đổi | 0 |
| Số bản ghi có thay đổi nghiệp vụ | 67 |
| Số bản ghi chỉ bổ sung trường mới (giá trị trống) | 3 |

### 1.1. Thống kê trường thay đổi (trong `results`)

| Trường | Số bản ghi thay đổi |
|---|---|
| `hachiojiEvaluationCode` | 70 |
| `id1` | 70 |
| `reportComment2` | 70 |
| `results` | 70 |
| `stressCode` | 70 |
| `reportComment1` | 69 |
| `isCorrection` | 67 |
| `markOutSource` | 19 |
| `outSourceCode` | 19 |

### 1.2. Ghi chú định dạng & màu sắc

- Giá trị **TRƯỚC** (old.json): <span style="color:#c62828"><del>gạch ngang màu đỏ</del></span>
- Giá trị **SAU** (new.json): <span style="color:#1b5e20;font-weight:bold">in đậm màu xanh</span> (các giá trị thay đổi)
- `*(không có)*`: trường không tồn tại trong file nguồn
- `*(trống)*`: trường tồn tại nhưng giá trị rỗng `""`

---

## 3. Bảng tóm tắt thay đổi theo `results.id`

Bảng dưới liệt kê **chỉ các trường có thay đổi giá trị nghiệp vụ** (loại trừ việc chỉ thêm trường mới với giá trị trống).

| results.id | sampleCode | testItemCode | Trường thay đổi | TRƯỚC (old.json) | SAU (new.json) |
|---:|---|---|---|---|---|
| 2870228 | 011 | Q339011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870229 | 011 | Q340011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870230 | 011 | Q341011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">73.4</span> |
| 2870231 | 011 | Q345011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">3.7</span> |
| 2870232 | 011 | Q346011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">0.7</span> |
| 2870233 | 011 | Q347011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">4.8</span> |
| 2870234 | 011 | Q348011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">17.4</span> |
| 2870235 | 011 | Q349011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870236 | 011 | Q350011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870237 | 011 | Q351011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870238 | 011 | Q352011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870239 | 011 | Q353011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870240 | 011 | Q354011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870241 | 011 | Q355011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">26</span> |
| 2870243 | 011 | H603011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">34.1</span> |
| 2870244 | 011 | W093011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">22.6</span> |
| 2870245 | 011 | W499011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">3220</span> |
| 2870246 | 011 | X268011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">160</span> |
| 2870247 | 011 | X743011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">30</span> |
| 2870248 | 011 | X769011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">760</span> |
| 2870249 | 011 | X963011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">210</span> |
| 2870250 | 011 | 0781011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">4400</span> |
| 2870251 | 011 | 0782011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">423</span> |
| 2870252 | 011 | 3940011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">13.3</span> |
| 2870253 | 011 | 3942011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">39.0</span> |
| 2870254 | 011 | 3982011 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">92.2</span> |
| 2870256 | 020 | 6576020 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">0.6</span> |
| 2870257 | 021 | 5059021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">4.6</span> |
| 2870258 | 021 | 2269021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">117</span> |
| 2870259 | 021 | 0821021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">0.33</span> |
| 2870260 | 021 | 0439021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">11</span> |
| 2870261 | 021 | 0438021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">16</span> |
| 2870262 | 021 | 0436021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">10</span> |
| 2870263 | 021 | 0433021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">76</span> |
| 2870264 | 021 | 0418021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">105</span> |
| 2870265 | 021 | 0417021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">3.7</span> |
| 2870266 | 021 | 0416021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">141</span> |
| 2870267 | 021 | 0408021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">9.1</span> |
| 2870268 | 021 | 0407021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">4.7</span> |
| 2870269 | 021 | 0406021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">0.76</span> |
| 2870270 | 021 | 0405021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">11.7</span> |
| 2870271 | 021 | 0401021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">48</span> |
| 2870272 | 021 | 0394021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">210</span> |
| 2870273 | 021 | 0388021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">0.3</span> |
| 2870274 | 021 | 0381021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">7.3</span> |
| 2870275 | 021 | 0203021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">79</span> |
| 2870276 | 021 | X436021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">67</span> |
| 2870277 | 021 | X437021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">196</span> |
| 2870278 | 021 | 0A51021 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">0.016</span> |
| 2870279 | 022 | R033022 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">20</span> |
| 2870280 | 03B | 039003B | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">85</span> |
| 2870281 | 541 | N751541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870282 | 541 | N750541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870283 | 541 | Y289541 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">5.5</span> |
| 2870284 | 541 | Y290541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870285 | 541 | Y291541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870286 | 541 | Y292541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">17</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870287 | 541 | Y293541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870288 | 541 | Y300541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870289 | 541 | Y311541 | <span style="color:#1b5e20;font-weight:bold">`hachiojiEvaluationCode`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">19</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
| 2870290 | 541 | Y317541 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">1.007</span> |
| 2870292 | 970 | L452970 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">1.01</span> |
| 2870293 | 970 | L451970 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">1.01</span> |
| 2870294 | 970 | L450970 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">99</span> |
| 2870295 | 970 | L449970 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">11.3</span> |
| 2870296 | 970 | 1385970 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">25.6</span> |
| 2870297 | 970 | 0910970 | <span style="color:#1b5e20;font-weight:bold">`isCorrection`</span> | <span style="color:#c62828"><del>false</del></span> | <span style="color:#1b5e20;font-weight:bold">true</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`reportComment1`</span> | <span style="color:#c62828"><del>04</del></span> | <span style="color:#1b5e20;font-weight:bold">*(trống)*</span> |
|  |  |  | <span style="color:#1b5e20;font-weight:bold">`results`</span> | <span style="color:#c62828"><del>*(không có)*</del></span> | <span style="color:#1b5e20;font-weight:bold">25.3</span> |
