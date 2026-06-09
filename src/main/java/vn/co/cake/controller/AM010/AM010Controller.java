package vn.co.cake.controller.AM010;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.payment.dto.PaymentOrderLogSummary;
import vn.co.cake.payment.entity.PaymentTransactionLog;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.request.PaymentLogSearchRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.utils.PageUtil;

import javax.servlet.http.HttpSession;

import static vn.co.cake.common.BaseConst.REDIRECT;
import static vn.co.cake.common.StringConst.BOOKMARK_STAFF_KEY;

@Controller
public class AM010Controller extends BaseController {

    private static final String PAYMENT_LOG_SEARCH_SESSION = "paymentLogSearchForm";

    private final AccountService accountService;
    private final PaymentTransactionLogService paymentTransactionLogService;

    public AM010Controller(AccountService accountService,
                           PaymentTransactionLogService paymentTransactionLogService) {
        this.accountService = accountService;
        this.paymentTransactionLogService = paymentTransactionLogService;
    }

    @GetMapping(RequestPathConst.AM010)
    public String list(PaymentLogSearchRequest searchForm,
                       @PageableDefault(size = SIZE_DEFAULT, sort = "created", direction = Sort.Direction.DESC) Pageable pageable,
                       Model model, HttpSession session, String homePage) {
        String bookmarkUrl = accountService.getBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return REDIRECT.concat(baseUrl).concat(RequestPathConst.AM001_LOGIN);
        }
        if (StringUtils.isNotEmpty(bookmarkUrl) && !baseUrl.concat(RequestPathConst.AM010).equals(bookmarkUrl)) {
            accountService.deletedBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
            return REDIRECT.concat(bookmarkUrl);
        }
        if (Objects.isNull(session.getAttribute(SESSION_FROM_EDIT_PAGE))) {
            session.setAttribute(PAYMENT_LOG_SEARCH_SESSION, searchForm);
            session.setAttribute(REQUEST_PAGEABLE, pageable);
        }
        PageUtil pageUtil = getPageSize(session);
        searchForm = getSessionPaymentLogForm(session, homePage);
        Page<PaymentOrderLogSummary> orderLogs = paymentTransactionLogService.findOrderSummaryPage(searchForm, pageUtil);
        model.addAttribute("orderLogs", orderLogs.getContent());
        setPaginationAttribute(pageUtil, orderLogs.getTotalElements(), RequestPathConst.AM010, model, settingPaymentLogCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);
        addSideMenu(model, RequestPathConst.AM010);
        return ScreenPathConst.AM010_SCREEN;
    }

    @GetMapping(RequestPathConst.AM010_ORDER_LOGS)
    @ResponseBody
    public ResponseEntity<?> orderLogs(@RequestParam String groupKey, HttpSession session) {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        if (StringUtils.isBlank(groupKey)) {
            return ResponseEntity.badRequest().body("groupKey required");
        }
        List<PaymentTransactionLog> logs = paymentTransactionLogService.findLogsByGroupKey(groupKey.trim());
        List<Map<String, Object>> items = new ArrayList<>();
        for (PaymentTransactionLog logRow : logs) {
            items.add(toLogDetailMap(logRow));
        }
        Map<String, Object> body = new HashMap<>();
        body.put("groupKey", groupKey.trim());
        body.put("logs", items);
        return ResponseEntity.ok(body);
    }

    @GetMapping(RequestPathConst.AM010_DETAIL)
    @ResponseBody
    public ResponseEntity<?> detail(@PathVariable long id, HttpSession session) {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }
        PaymentTransactionLog log = paymentTransactionLogService.findById(id);
        if (log == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found");
        }
        return ResponseEntity.ok(toLogDetailMap(log));
    }

    private static Map<String, Object> toLogDetailMap(PaymentTransactionLog log) {
        Map<String, Object> body = new HashMap<>();
        body.put("id", log.getId());
        body.put("created", log.getCreated());
        body.put("paymentMethod", log.getPaymentMethod());
        body.put("eventType", log.getEventType());
        body.put("status", log.getStatus());
        body.put("orderCode", log.getOrderCode());
        body.put("vietqrOrderId", log.getVietqrOrderId());
        body.put("amount", log.getAmount());
        body.put("errorCode", log.getErrorCode());
        body.put("errorMessage", log.getErrorMessage());
        body.put("externalTxnId", log.getExternalTxnId());
        body.put("referenceNumber", log.getReferenceNumber());
        body.put("requestPayload", log.getRequestPayload());
        body.put("responsePayload", log.getResponsePayload());
        body.put("httpStatus", log.getHttpStatus());
        body.put("durationMs", log.getDurationMs());
        return body;
    }

    private PaymentLogSearchRequest getSessionPaymentLogForm(HttpSession session, String homePage) {
        if (Objects.nonNull(session.getAttribute(PAYMENT_LOG_SEARCH_SESSION)) && StringUtils.isEmpty(homePage)) {
            return (PaymentLogSearchRequest) session.getAttribute(PAYMENT_LOG_SEARCH_SESSION);
        }
        return new PaymentLogSearchRequest();
    }

    private Map<String, Object> settingPaymentLogCondition(PaymentLogSearchRequest searchForm) {
        Map<String, Object> condition = new HashMap<>();
        condition.put("orderCode", searchForm.getOrderCode());
        condition.put("vietqrOrderId", searchForm.getVietqrOrderId());
        condition.put("paymentMethod", searchForm.getPaymentMethod());
        condition.put("status", searchForm.getStatus());
        condition.put("eventType", searchForm.getEventType());
        condition.put("lastUpdateDateFrom", searchForm.getLastUpdateDateFrom());
        condition.put("lastUpdateDateTo", searchForm.getLastUpdateDateTo());
        return condition;
    }
}
