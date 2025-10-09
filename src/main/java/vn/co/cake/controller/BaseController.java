package vn.co.cake.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import vn.co.cake.common.BaseConst;
import vn.co.cake.enums.Authorities;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.request.ProductSearchRequest;
import vn.co.cake.request.SearchRequest;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.utils.PageUtil;

import javax.servlet.http.HttpSession;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

/**
 * BaseController
 */
@Slf4j
public class BaseController {

    @Value("${cake.base.url}")
    protected String baseUrl;

    @Value("${cake.base.url-sa}")
    protected String baseUrlSa;

    protected static final String ACCOUNT_DATA = "accountData";

    // ID
    protected static final String ID = "ID";
    // attribute sort
    protected static final String SORT = "sort";
    // attribute sort property
    protected static final String SORT_PROPERTY = "property";
    // attribute direction
    protected static final String DIRECTION = "direction";
    // attribute page
    protected static final String PAGE = "page";
    // attribute size
    protected static final String SIZE = "size";
    // attribute size default
    protected static final int SIZE_DEFAULT = 10;
    // attribute page number
    protected static final String PAGE_NUMBERS = "pageNumbers";
    // attribute total page
    protected static final String TOTAL_PAGES = "totalPages";
    // attribute total record
    protected static final String TOTAL_RECORD = "totalRecord";
    // attribute search condition
    protected static final String SEARCH_CONDITION = "searchCondition";
    // attribute condition maps
    protected static final String CONDITION_MAPS = "conditionMaps";
    // attribute list url
    protected static final String LIST_URL = "listUrl";
    // attribute sort default
    protected static final String SORT_DEFAULT_FIRST = "lastUpdateDate";
    protected static final String SORT_DEFAULT = "id";
    // attribute logout
    protected static final String LOGOUT = "logout";
    // attribute reset password
    protected static final String RESET_PASSWORD = "resetPassword";

    protected static final String CHANGE_PASSWORD = "changePassword";
    // attribute key
    protected static final String KEY = "key";
    protected static final String ACCOUNT_ID = "accountId";
    // attribute hash
    protected static final String HASH = "hash";
    // attribute search
    protected static final String SEARCH = "search";

    protected static final String SETTING = "setting";

    protected static final String TRUE = "true";
    // attribute facility code default
    protected static final String PAGE_SIZE_MAP = "pageSizeMaps";
    // attribute sideMenu
    protected static final String SIDE_MENU = "sideMenu";

    protected static final String SUCCESS = "success";

    protected static final String FAILURE = "failure";

    // attribute request master convert group form
    protected static final String REQUEST_MASTER_CONVERT_FORM = "requestMasterConvertForm";
    protected static final String REQUEST_SFC_MASTER_FORM = "sfcMasterSearchRequest";
    // attribute pageable request
    protected static final String REQUEST_PAGEABLE = "requestPageable";
    // attribute from edit page
    protected static final String SESSION_FROM_EDIT_PAGE = "editPageSession";
    // attribute page number
    protected static final String PAGE_SIZE = "pageSize";
    
    protected AdminLoginInfo getLoginInfoAdmin(HttpSession session) {
        AdminLoginInfo adminLoginInfo = (AdminLoginInfo) session.getAttribute(BaseConst.ADMIN_SESSION);

        if (Objects.isNull(adminLoginInfo) || (!Authorities.ROLE_STAFF.equals(adminLoginInfo.getAuthority())
            && !Authorities.ROLE_ADMIN.equals(adminLoginInfo.getAuthority()))) {
        return null;
    }
        return adminLoginInfo;
    }

    protected UserLoginInfo getLoginInfo(HttpSession session) {
        return (UserLoginInfo) session.getAttribute(BaseConst.USER_SESSION);
    }

    /**
     * Set pagination attribute
     *
     * @param pageable    pageable
     * @param totalRecord total record of list
     * @param listUrl     base url of page
     * @param model       model of page
     * @param <T>         type of search condition
     */
    protected <T> void setPaginationAttribute(Pageable pageable,
                                              long totalRecord,
                                              String listUrl,
                                              Model model,
                                              Map<String, T> conditionMap) {
        setCurrentSort(pageable.getSort(), model);
        setCurrentSize(pageable.getPageSize(), model);
        setCurrentPage(pageable.getPageNumber(), model);
        setPageNumbers(totalRecord, pageable.getPageSize(), model);
        setBaseURL(listUrl, model);
        setConditionMap(conditionMap, model);
        setSearchCondition(conditionMap, model);
        setPageSizeMap(conditionMap, pageable.getSort(), model);
    }

    protected PageUtil getPageSize(HttpSession session) {
        Pageable pageable = (Pageable) session.getAttribute(REQUEST_PAGEABLE);
        int size = Optional.of(pageable.getPageSize()).orElse(SIZE_DEFAULT);
        if (Objects.nonNull(session.getAttribute(SESSION_FROM_EDIT_PAGE)) && Objects.nonNull(session.getAttribute(REQUEST_PAGEABLE))) {
            pageable = (Pageable) session.getAttribute(REQUEST_PAGEABLE);
        }
        return new PageUtil(size, pageable.getPageNumber(), pageable);
    }

    protected void removeSessionAttributes(HttpSession session, String sessionAttribute) {
        if (Objects.nonNull(session.getAttribute(sessionAttribute))) {
            session.removeAttribute(sessionAttribute);
        }
    }

    protected void addSideMenu(Model model, String sideMenu) {
        model.addAttribute(SIDE_MENU, sideMenu);
    }

    // set current page
    private void setCurrentPage(int page, Model model) {
        model.addAttribute(PAGE, page);
    }

    // set current size
    private void setCurrentSize(int size, Model model) {
        model.addAttribute(SIZE, size);
    }

    // set current sort
    private void setCurrentSort(Sort sort, Model model) {
        StringBuilder orderBy = new StringBuilder();
        Sort.Direction direction = Sort.Direction.ASC;

        if (sort.stream().findAny().isPresent()) {
            for (Sort.Order order : sort) {
                direction = order.getDirection();
                orderBy.append(order.getProperty());
                orderBy.append(",");
            }

            orderBy.append(direction);
        }

        model.addAttribute(DIRECTION, direction);
        model.addAttribute(SORT, orderBy);
    }

    // set page number
    private void setPageNumbers(long totalRecord, int limit, Model model) {
        long totalPages = totalRecord > 0 ? ((totalRecord - 1) / limit) + 1 : 0;

        if (totalPages > 0) {
            model.addAttribute(PAGE_NUMBERS, LongStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList()));
            model.addAttribute(TOTAL_PAGES, totalPages);
            model.addAttribute(TOTAL_RECORD, totalRecord);
        } else {
            model.addAttribute(PAGE_NUMBERS, new ArrayList<>());
            model.addAttribute(TOTAL_PAGES, totalPages);
            model.addAttribute(TOTAL_RECORD, totalPages);
        }
    }

    // set base url of page
    private void setBaseURL(String listUrl, Model model) {
        model.addAttribute(LIST_URL, listUrl);
    }

    // set search condition of request
    private <T> void setSearchCondition(T searchCondition, Model model) {
        model.addAttribute(SEARCH_CONDITION, searchCondition);
    }

    // set search condition of request
    private <T> void setSearchCondition(Map<String, T> conditionMaps, Model model) {
        if (conditionMaps != null) {
            conditionMaps.forEach(model::addAttribute);
        }
    }

    // set condition maps
    private <T> void setConditionMap(Map<String, T> conditionMaps, Model model) {
        StringBuilder condition = new StringBuilder();
        condition.append("?page={page}");
        condition.append("&sort={sort}");
        if (conditionMaps != null) {
            conditionMaps.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    condition.append("&");
                    condition.append(key);
                    condition.append("=");
                    condition.append(URLEncoder.encode((String) value, StandardCharsets.UTF_8));
                }
            });
        }
        model.addAttribute(CONDITION_MAPS, condition);
    }

    // set page size maps
    private <T> void setPageSizeMap(Map<String, T> conditionMaps, Sort sort, Model model) {
        StringBuilder condition = new StringBuilder();
        condition.append("?page=0");
        condition.append("&sort=");
        condition.append(SORT_DEFAULT_FIRST)
                .append(",")
                .append(SORT_DEFAULT);
        if (conditionMaps != null) {
            conditionMaps.forEach((key, value) -> {
                if (Objects.nonNull(value)) {
                    condition.append("&");
                    condition.append(key);
                    condition.append("=");
                    condition.append(URLEncoder.encode((String) value, StandardCharsets.UTF_8));
                }
            });
        }

        model.addAttribute(PAGE_SIZE_MAP, condition);
    }
    
    protected void setCurrentSearchRequestAndPageableSession(HttpSession session, SearchRequest request, Pageable pageable) {
         if (Objects.isNull(session.getAttribute(SESSION_FROM_EDIT_PAGE))) {
            session.setAttribute(REQUEST_MASTER_CONVERT_FORM, request);
            session.setAttribute(REQUEST_PAGEABLE, pageable);
        }
    }
    protected SearchRequest getSessionOrderForm(HttpSession session, String homePage) {
        if (Objects.nonNull(session.getAttribute(REQUEST_MASTER_CONVERT_FORM)) && StringUtils.isEmpty(homePage)) {
            return (SearchRequest) session.getAttribute(REQUEST_MASTER_CONVERT_FORM);
        } else {
            return new SearchRequest();
        }
    }
    
    protected void setCurrentSearchRequestAndPageableSession(HttpSession session, ProductSearchRequest request, Pageable pageable) {
         if (Objects.isNull(session.getAttribute(SESSION_FROM_EDIT_PAGE))) {
            session.setAttribute(REQUEST_MASTER_CONVERT_FORM, request);
            session.setAttribute(REQUEST_PAGEABLE, pageable);
        }
    }
    protected ProductSearchRequest getSessionProductForm(HttpSession session) {
        if (Objects.nonNull(session.getAttribute(REQUEST_MASTER_CONVERT_FORM))) {
            return (ProductSearchRequest) session.getAttribute(REQUEST_MASTER_CONVERT_FORM);
        } else {
            return new ProductSearchRequest();
        }
    }
}
