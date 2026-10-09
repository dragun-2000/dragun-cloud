# Tài liệu hệ thống De Basé (dragun-cloud)

| Mục | Giá trị |
|-----|---------|
| Dự án | De Basé — website thương mại điện tử thời trang |
| Repo | dragun-cloud |
| Phiên bản bộ tài liệu | 1.0 |
| Ngày ban hành | 2026-10-09 |

## Cách dùng theo đối tượng

| Đối tượng | Đọc trước |
|-----------|-----------|
| **Khách hàng / Ban nghiệm thu** | Thư mục [`uat/`](uat/) — bắt đầu từ [DOC-UAT-01](uat/DOC-UAT-01-system-overview.md) và [DOC-UAT-05](uat/DOC-UAT-05-acceptance-checklist.md) |
| **BA / SA / QA** | [`business/`](business/), [`testing/`](testing/), tham chiếu chéo `BR-` / `UC-` / `TC-` |
| **Đối tác tích hợp** | [`integrations/`](integrations/) |
| **Đội phát triển (nội bộ)** | [`dev/`](dev/) — **không** đưa vào gói gửi khách hàng |
| **Lưu trữ lịch sử** | [`archive/`](archive/) — **không** dùng cho nghiệm thu |

## Quy ước mã tài liệu

Định dạng file: `DOC-<NHÓM>-<STT>-<slug>.md`

| Nhóm | Prefix | Ý nghĩa |
|------|--------|---------|
| Nghiệm thu | `DOC-UAT-` | Gói User Acceptance / biên bản |
| Nghiệp vụ | `DOC-BRD-`, `DOC-UC-` | Business rules, use cases |
| Hệ thống | `DOC-ADD-`, `DOC-DM-` | Architecture, domain model |
| Tích hợp | `DOC-ICD-` | Interface / callback contract |
| Kiểm thử | `DOC-ATP-` | Acceptance test scenarios |
| Nội bộ Dev | `DOC-DEV-` | Coding rule, detail design |

### ID nghiệp vụ (traceability)

| Prefix | Dùng cho |
|--------|----------|
| `FN-` | Chức năng (function catalog) |
| `BR-` | Business rule |
| `UC-` | Use case |
| `TC-` | Test case / scenario |
| `NFR-` | Non-functional requirement |

**Nguyên tắc chống trùng:** mỗi thông tin chỉ sống ở một tài liệu. Tài liệu khác chỉ **tham chiếu ID**, không copy nguyên văn.

## Mục lục đầy đủ

### Gói nghiệm thu (`uat/`)

| Mã | Tài liệu |
|----|----------|
| DOC-UAT-01 | [Tổng quan hệ thống](uat/DOC-UAT-01-system-overview.md) |
| DOC-UAT-02 | [Phạm vi và tiêu chí nghiệm thu](uat/DOC-UAT-02-scope-and-criteria.md) |
| DOC-UAT-03 | [Danh mục chức năng](uat/DOC-UAT-03-function-catalog.md) |
| DOC-UAT-04 | [Hướng dẫn sử dụng (UAT)](uat/DOC-UAT-04-user-guide.md) |
| DOC-UAT-05 | [Checklist nghiệm thu](uat/DOC-UAT-05-acceptance-checklist.md) |
| DOC-UAT-06 | [Biên bản nghiệm thu (mẫu)](uat/DOC-UAT-06-acceptance-minutes-template.md) |

### Nghiệp vụ & kiểm thử

| Mã | Tài liệu |
|----|----------|
| DOC-BRD-01 | [Quy tắc nghiệp vụ](business/DOC-BRD-01-business-rules.md) |
| DOC-UC-01 | [Use case cốt lõi](business/DOC-UC-01-core-use-cases.md) |
| DOC-ATP-01 | [Kịch bản chấp nhận](testing/DOC-ATP-01-acceptance-scenarios.md) |

### Hệ thống & tích hợp

| Mã | Tài liệu |
|----|----------|
| DOC-ADD-01 | [Kiến trúc tổng quan](system/DOC-ADD-01-architecture-overview.md) |
| DOC-DM-01 | [Mô hình miền dữ liệu](system/DOC-DM-01-domain-model.md) |
| DOC-ICD-01 | [VietQR Callback API](integrations/DOC-ICD-01-vietqr-callback.md) |
| DOC-ICD-02 | [Pancake POS — tổng quan](integrations/DOC-ICD-02-pancake-pos-overview.md) |

### Nội bộ (`dev/`)

| Mã | Tài liệu |
|----|----------|
| DOC-DEV-01 | [Chuẩn coding](dev/DOC-DEV-01-coding-standards.md) |
| DOC-DEV-02 | [Detail design VietQR](dev/DOC-DEV-02-vietqr-detail-design.md) |

### Báo cáo đồ án (`thesis/`)

| Mã | Tài liệu |
|----|----------|
| DOC-SYS-01 | [Báo cáo Đồ án 1 — Chatbot AI (Trịnh Văn Hải)](thesis/DOC-SYS-01-bao-cao-do-an-chatbot-ai.md) |
