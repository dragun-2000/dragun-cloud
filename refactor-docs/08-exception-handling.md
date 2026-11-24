# 08 — Exception Handling (as implemented)

Baseline `a0515d8`. Paths relative to `src/main/java/vn/co/cake/`. All of this is part of the observable contract and must be preserved.

## 1. Key fact: `CommonServletException` is a **checked** exception

`exception/CommonServletException.java:8` — `extends ServletException` ✔. Consequences under Spring's default rollback rules (rollback only on `RuntimeException`/`Error`):

| Transaction | rollbackFor | Effect when `CommonServletException` is thrown |
|---|---|---|
| `OrderServiceImpl` (class) | none | **commits** partial writes (e.g. Order saved, items not) — COD/VietQR sold-out path |
| `CodCheckoutService.placeOrder` | none | commits; if an inner `rollbackFor=Exception` method threw, outer commit ⇒ `UnexpectedRollbackException` ⇒ 500 (UNKNOWN, Spring-semantics inference) |
| `VietQrCheckoutService.startCheckout` | none | QR/token HTTP failure commits draft order + stock HOLD + PENDING row |
| `CartServiceImpl.create` | none | empty `Cart` row kept when a variation/product is missing |
| `AccountServiceImpl` (class) | `Throwable` | rolls back |
| `InventoryReservationService.reserve/confirm/release`, `VnpayCheckoutService`, `VnpayIpnService`, `VietQrWebhookService` | `Exception` | rolls back |

**Refactor guard:** changing the exception hierarchy, adding/removing `rollbackFor`, or moving a throw across a transaction boundary changes data outcomes → CRITICAL.

## 2. `GlobalExceptionHandler` (`handler/GlobalExceptionHandler.java`, `@ControllerAdvice`)

| Exception | HTTP | Body / view |
|---|---|---|
| `CommonServletException` | **200** (no status set) | view `error-page` {message, url}; no API detection — also for JSON endpoints (e.g. `/add-to-cart`, AM003 `/settings`) |
| `ConversionFailedException` | 400 | plain text `ex.getMessage()` |
| `MethodArgumentNotValidException` | 400 | `CommonResponse{result:"Validation Failed", message: joined default messages}` |
| `HttpMessageNotReadableException` | 400 | JsonMappingException cause ⇒ `{result:"JSON parse error", message:"Invalid data in field: <path>"}`; else `{result:"Invalid Request", message: raw}` |
| `ObjectOptimisticLockingFailureException` (warn), `NullPointerException`, `IllegalArgumentException` (incl. `NumberFormatException`), `MultipartException` (heuristic message + optional "Chi tiết: " raw), `MissingServletRequestParameterException`, `Exception` | via `handleGenericException` (:211-231) | API request (Accept contains `application/json` **or** URI starts with `/api`) ⇒ **500** `CommonResponse{result:"Internal Server Error", message}`; otherwise view `error-page` with **HTTP 200** |

`CommonResponse` = {result, message}. `IllegalArgumentException` messages are exposed to the client. `request.getMethod()` is used for logging only (no reflection).
`vnpay/config/VnpayModelAdvice` handles no exceptions (model attributes only; swallows its own lookup errors with defaults).

## 3. Controller-local handling

| Controller | Pattern |
|---|---|
| SA001 / SA002 / AM001 / AM004 / AM007 | Catch `CommonServletException` (sometimes `Exception`) ⇒ `ResponseEntity<String>` 400/500 with Vietnamese text; SA001 register only passes through `PASSWORD_RE_ENTER_NOT_MATCH` / `EMAIL_ALREADY_EXISTS` messages. GET view endpoints let it reach the error page |
| AM003 delete/restore, AM007 delete | `void`; exceptions logged and swallowed; always 200 |
| AM003 register/update | `CommonServletException` ⇒ 400 text; `NumberFormatException` escapes ⇒ global IAE handler |
| AM006 | 403 Unauthorized, 404 not found, 409 wrong status / restock failure, 500 `"Đồng bộ thất bại: "+messageError` or `"Lỗi hệ thống: "+msg` |
| AM008 | 403 / 400 / 500 `"Lỗi hệ thống: ..."` JSON |
| AM009 | 400 `"Cập nhật thất bại: ..."` |
| AM011 | 403; `IllegalArgumentException` 400; other 500 |
| CartController | swallows at :130 (option rebuild), :298 (Pancake refresh, warn with full phone), :428/:444 (format), :524 (persist); `/order-history` any exception ⇒ view `login` |
| CustomerOrderCancelController | 403 no session; `CommonServletException` 400; other 500 |
| PaymentCheckoutController | 403 / 400 (`CommonServletException`) / 500 via `CommonResponse{result: reason phrase, message}` |
| VnpayCheckoutController | same; status endpoint maps `CommonServletException` ⇒ **404** |
| VnpayCallbackController `/ipn` | catches all ⇒ HTTP 200 `{RspCode:"99"}` |
| VietQrTransactionSyncController | 401 / 400 / 500 with own `{status, message}` |
| VietQrPartnerTokenController | bad credentials ⇒ 400 `{status:FAILED, message:UNAUTHORIZED}` |
| SizeAdviceController | validation 400 empty body; service errors swallowed into fallback (`/api/size-advice`) or 500 (`/chat`) |
| ImageCompressionController | redirects with flash messages; `IllegalStateException` when product task active |

## 4. Swallowed / silent failures (service & job level)

| Location | Behavior |
|---|---|
| `PancakePosService.updateStock` | all exceptions swallowed, log commented out (partial stock updates possible) |
| `PancakePosService.createOrder` | catches `HttpStatusCodeException`, `ResourceAccessException`, `Exception` ⇒ SYNC_FAIL; **payload build outside try** ⇒ NPE escapes |
| `PancakePosService.cancelOrderOnPancake` | any error ⇒ null (caller throws "Không hủy được…") |
| `PancakePosService.getAllProductPancake` | error ⇒ null ⇒ caller NPE on `.size()` (Job01 logs; endpoint 500) |
| Pancake failure dump + email | all exceptions swallowed |
| `PaymentTransactionLogService.save` | swallows its own exceptions inside caller tx (may leave tx rollback-only) |
| `CartServiceImpl.create` delete loop | per-item exceptions swallowed inside tx |
| `VnpayExpireService` QueryDR | exceptions swallowed ⇒ falls to give-up logic |
| `VnpayExpireJob`, Job002, Job003, Job004 per item | logged and continued |
| `OrderConfirmationMailService` | logged, returns false, no retry |
| `EmailService.sendEmail` result (forgot password) | ignored ⇒ always 200 |
| `OtpVerificationService.checkRateLimit` | exception ⇒ allow (fail-open) |
| `SizeAdviceServiceImpl.getSizeAdvice` | any exception ⇒ fallback size |
| `PixelConversionServiceImpl` | `printStackTrace` (dead code candidate) |
| `ImageCompressionService` | queue full ⇒ log and skip |

## 5. Security-layer error responses

- Login failure: handler sets 401 then `sendRedirect` ⇒ 302 to `…/SA/SA001/login?error` or `/AM/AM001?error`.
- Role/URL mismatch after login (customer): 401 + redirect to a doubled path `url-sa + "/SA/SA001/login"` (U-26).
- Access denied: `MissingCsrfTokenException` ⇒ forward `/error`; other ⇒ redirect to login page.
- `ContextService.getSecurityContext` may NPE on deserialization failure (UNKNOWN frequency).
