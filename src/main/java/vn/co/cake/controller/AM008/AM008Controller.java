package vn.co.cake.controller.AM008;

import lombok.extern.slf4j.Slf4j;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.entity.Product;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.ProductDiscountUpdateRequest;
import vn.co.cake.request.ProductSearchRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.ProductService;
import vn.co.cake.utils.PageUtil;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static vn.co.cake.common.BaseConst.REDIRECT;
import static vn.co.cake.common.StringConst.BOOKMARK_STAFF_KEY;

@Controller
@Slf4j
public class AM008Controller extends BaseController {

    private final AccountService accountService;
    private final ProductService productService;

    public AM008Controller(AccountService accountService, ProductService productService) {
        this.accountService = accountService;
        this.productService = productService;
    }

    @GetMapping(RequestPathConst.AM008)
    public String listProducts(ProductSearchRequest searchForm,
                               @PageableDefault(
                                       size = SIZE_DEFAULT,
                                       sort = {SORT_DEFAULT_FIRST, SORT_DEFAULT},
                                       direction = Sort.Direction.DESC
                               ) Pageable pageable,
                               Model model, HttpSession session, String homePage) {

        String bookmarkUrl = accountService.getBookmarkUrlLasted(BOOKMARK_STAFF_KEY);

        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return REDIRECT.concat(baseUrl).concat(RequestPathConst.AM001_LOGIN);
        }

        if (StringUtils.isNotEmpty(bookmarkUrl) && !baseUrl.concat(RequestPathConst.AM008).equals(bookmarkUrl)) {
            accountService.deletedBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
            return REDIRECT.concat(bookmarkUrl);
        }

        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        searchForm = getSessionProductForm(session);
        Page<Product> products = productService.getAllByCondition(searchForm, pageUtil, true);
        model.addAttribute("products", products.getContent());
        setPaginationAttribute(pageUtil, products.getTotalElements(), RequestPathConst.AM008, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);

        addSideMenu(model, RequestPathConst.AM008);
        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);
        return ScreenPathConst.AM008_SCREEN;
    }

    @PostMapping(RequestPathConst.AM008_UPDATE_DISCOUNT)
    public ResponseEntity<?> updateDiscount(@RequestBody ProductDiscountUpdateRequest request, HttpSession session) {
        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try {
            productService.updateProductsDiscount(request.getProductIds(), request.getDiscount());
            return ResponseEntity.ok(Map.of("success", true, "message", "Cập nhật giảm giá thành công"));
        } catch (CommonServletException e) {
            log.error("AM008: Error updating discount: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("AM008: Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    private Map<String, Object> settingCondition(ProductSearchRequest searchForm) {
        Map<String, Object> conditionMaps = new HashMap<>();
        if (StringUtils.isNotEmpty(searchForm.getName())) {
            conditionMaps.put("name", searchForm.getName());
        }
        if (StringUtils.isNotEmpty(searchForm.getCode())) {
            conditionMaps.put("code", searchForm.getCode());
        }
        if (StringUtils.isNotEmpty(searchForm.getSubCode())) {
            conditionMaps.put("subCode", searchForm.getSubCode());
        }
        return conditionMaps;
    }
}

