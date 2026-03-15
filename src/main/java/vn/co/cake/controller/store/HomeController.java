package vn.co.cake.controller.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.entity.Voucher;
import vn.co.cake.enums.Categories;
import vn.co.cake.enums.Colors;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.request.ProductSearchRequest;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.CartService;
import vn.co.cake.service.ProductService;
import vn.co.cake.utils.FunctionUtil;
import vn.co.cake.utils.PageUtil;
import vn.co.cake.response.ProductResponse;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.websocket.server.PathParam;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@SessionAttributes("cartForm")
public class HomeController extends BaseController {
	
	private final ProductService productService;
	private final CartService cartService;
    private final AccountService accountService;
    private final VariationRepository variationRepository;
    private final VoucherRepository voucherRepository;

	public HomeController(ProductService productService,
                          CartService cartService,
                          AccountService accountService,
                          VariationRepository variationRepository, VoucherRepository voucherRepository) {
		this.productService = productService;
        this.cartService = cartService;
        this.accountService = accountService;
        this.variationRepository = variationRepository;
        this.voucherRepository = voucherRepository;
    }

    @ModelAttribute("cartForm")
    public CartForm createCart() {
        return new CartForm();
    }
	
	@GetMapping(RequestPathConst.HOME)
    public String listProducts(ProductSearchRequest searchForm,
                               @ModelAttribute("cartForm") CartForm cartForm,
							   @PageableDefault(
                                            size = 8,
                                            sort = {SORT_DEFAULT},
                                            direction = Sort.Direction.DESC
                                          ) Pageable pageable,
							   Model model, HttpSession session,
                               HttpServletResponse response) throws CommonServletException {
							   
        UserLoginInfo loginInfo = getLoginInfo(session);
        boolean isLogin = false;
        if (loginInfo != null) {
            cartForm = this.getCartFormDefault(loginInfo, cartForm);
            isLogin = true;
        }
        model.addAttribute("isLogin", isLogin);
        FunctionUtil.updateCartQuantity(model, cartForm);
        
        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        searchForm = getSessionProductForm(session);
        
        Page<Product> products = productService.getAllByCondition(searchForm, pageUtil, false);
        List<ProductResponse> responses = products.stream()
                                                .sorted(Comparator.comparing(Product::getUpdated).reversed())
                                                .map(ProductResponse::new)
                                                .collect(Collectors.toList());
        model.addAttribute("products", responses);

        String categoryRoot = searchForm.getCategory();
        
        searchForm.setCategory(Categories.NEW_IN.getText());
        Page<Product> newInProductPage = productService.getAllByCondition(searchForm, pageUtil, false);
        List<ProductResponse> newInProducts = newInProductPage.stream()
                                                .sorted(Comparator.comparing(Product::getUpdated).reversed())
                                                .map(ProductResponse::new)
                                                .collect(Collectors.toList());
        model.addAttribute("newInProducts", newInProducts);
        
        searchForm.setCategory(categoryRoot);
        long totalRecord = responses.size();
        setPaginationAttribute(pageUtil, totalRecord, RequestPathConst.AM003, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);
        model.addAttribute("currentPage", pageable.getPageNumber());
        model.addAttribute("totalPages", products.getTotalElements());
        
        Voucher discountPrice = voucherRepository.findFirstByCodeAndDeletedIsFalse("DISCOUNT_PRICE");
        boolean isDiscount = discountPrice != null && discountPrice.getDiscountPercent() > 0;
        model.addAttribute("isDiscount", isDiscount);
        
        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);

        // ✅ Cho phép cache trang HTML trong 1 ngày
        response.setHeader("Cache-Control", "public, max-age=86400");

        return ScreenPathConst.INDEX_SCREEN;
    }
    
    @GetMapping("/products/group/{category}")
    public String listProductByCategory(
                                @PathVariable(required = false) String category,
							    @PageableDefault(
                                            size = 12,
                                            sort = {SORT_DEFAULT},
                                            direction = Sort.Direction.DESC
                                          ) Pageable pageable,
                                @ModelAttribute("cartForm") CartForm cartForm,    
							    Model model, HttpSession session) throws CommonServletException {
        
        UserLoginInfo loginInfo = getLoginInfo(session);
        boolean isLogin = false;
        if (loginInfo != null) {
            cartForm = this.getCartFormDefault(loginInfo, cartForm);
            isLogin = true;
        }
        model.addAttribute("isLogin", isLogin);
        FunctionUtil.updateCartQuantity(model, cartForm);
        
        ProductSearchRequest searchForm = new ProductSearchRequest();
        searchForm.setCategory(category);
        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        Page<Product> products = productService.getAllByCondition(searchForm, pageUtil, false);
        List<ProductResponse> responses = products.stream().map(ProductResponse::new).collect(Collectors.toList());
        setPaginationAttribute(pageUtil, products.getTotalElements(), "/products/group/" + category, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);
        model.addAttribute("products", responses);
        model.addAttribute("category", category);
        
        Voucher discountPrice = voucherRepository.findFirstByCodeAndDeletedIsFalse("DISCOUNT_PRICE");
        boolean isDiscount = discountPrice != null && discountPrice.getDiscountPercent() > 0;
        model.addAttribute("isDiscount", isDiscount);

        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);
        return "product-category";
    }

    @GetMapping("/products/search")
    public String listProductByProductName(@RequestParam String name,
                                            @PageableDefault(
                                                    size = 12,
                                                    sort = {SORT_DEFAULT},
                                                    direction = Sort.Direction.DESC
                                            ) Pageable pageable,
                                            @ModelAttribute("cartForm") CartForm cartForm,    
                                            Model model, HttpSession session) throws CommonServletException {
        UserLoginInfo loginInfo = getLoginInfo(session);
        boolean isLogin = false;
        if (loginInfo != null) {
            cartForm = this.getCartFormDefault(loginInfo, cartForm);
            isLogin = true;
        }
        model.addAttribute("isLogin", isLogin);
        FunctionUtil.updateCartQuantity(model, cartForm);

        ProductSearchRequest searchForm = new ProductSearchRequest();
        searchForm.setName(name);
        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        Page<Product> products = productService.getAllByCondition(searchForm, pageUtil, false);
        List<ProductResponse> responses = products.stream().map(ProductResponse::new).collect(Collectors.toList());
        model.addAttribute("products", responses);
        
        Voucher discountPrice = voucherRepository.findFirstByCodeAndDeletedIsFalse("DISCOUNT_PRICE");
        boolean isDiscount = discountPrice != null && discountPrice.getDiscountPercent() > 0;
        model.addAttribute("isDiscount", isDiscount);
        
        setPaginationAttribute(pageUtil, products.getTotalElements(), "/products/search?name=" + name, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);
        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);
        return "product-category";
    }
	
	@GetMapping("/products")
	public String productDetail(@PathParam("id") Long id, Model model, @ModelAttribute("cartForm") CartForm cartForm,
	                            HttpSession session) throws CommonServletException, JsonProcessingException {
        UserLoginInfo loginInfo = getLoginInfo(session);
        boolean isLogin = false;
        if (loginInfo != null) {
            cartForm = this.getCartFormDefault(loginInfo, cartForm);
            isLogin = true;
        }
        model.addAttribute("isLogin", isLogin);
        FunctionUtil.updateCartQuantity(model, cartForm);

        Product product = productService.detail(id);

        String sizes = product.getSizes();
        List<String> sizeModel = new ArrayList<>(); 
        if (StringUtils.isNotEmpty(sizes)) {
            sizeModel = Arrays.asList(sizes.split(","));
        }
        model.addAttribute("sizes", sizeModel);
        
        String colors = product.getColors();
        List<String> colorModel = new ArrayList<>(); 
        if (StringUtils.isNotEmpty(colors)) {
            colorModel = Arrays.asList(colors.split(","));
        }
        model.addAttribute("colors", colorModel);
        
        List<String> backgroundColor = new ArrayList<>();
        colorModel.forEach(color -> {
            String codeByName = Colors.getCodeByName(color);
            if (StringUtils.isNotBlank(codeByName)) {
                backgroundColor.add(codeByName);
            }
        });
        model.addAttribute("codeColors", backgroundColor);
        
        ProductResponse response = new ProductResponse(product);
        model.addAttribute("product", response);
		model.addAttribute("cart", cartForm);

        String descriptions = "";
        if (StringUtils.isNotBlank(response.getDescription())) {
            descriptions = response.getDescription().replace("\n", "<br>").replace("\\n", "<br>");
        }
		model.addAttribute("descriptions", descriptions);

        String descriptionSizes = "";
        if (StringUtils.isNotBlank(response.getDescriptionSize())) {
            descriptionSizes = response.getDescriptionSize().replace("\n", "<br>").replace("\\n", "<br>");
        }
		model.addAttribute("descriptionSizes", descriptionSizes);

        String productInformation = "";
        if (StringUtils.isNotBlank(response.getProductInformation())) {
            productInformation = response.getProductInformation().replace("\n", "<br>").replace("\\n", "<br>");
        }
        model.addAttribute("productInformation", productInformation);

        // Thêm thông tin account để fill form size advice
        Integer userHeight = null;
        Integer userWeight = null;
        if (loginInfo != null) {
            Account account = accountService.getAccount(loginInfo.getId());
            if (account != null) {
                userHeight = account.getHeight();
                userWeight = account.getWeight();
            }
        }
        model.addAttribute("userHeight", userHeight);
        model.addAttribute("userWeight", userWeight);

        List<OrderItem> orderItems = new ArrayList<>();
        if (!CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            orderItems.addAll(cartForm.getOrderItems());
        }
        ObjectMapper objectMapper = new ObjectMapper();
        String orderItemsJson = objectMapper.writeValueAsString(orderItems);
        model.addAttribute("orderItems", orderItemsJson);

        model.addAttribute("productLinkeds", this.getLinkedProducts(product));

        List<Variation> variations = variationRepository.findAllByPancakeProductId(product.getProductPancakeId());
        String variationsJson = objectMapper.writeValueAsString(variations);
        model.addAttribute("variations", variationsJson);

        // Provide distinct types (Kiểu) only when present (non-null, non-empty)
        Set<String> typeSet = new LinkedHashSet<>();
        for (Variation v : variations) {
            if (v.getType() != null) {
                String t = v.getType().trim();
                if (!t.isEmpty()) {
                    typeSet.add(t);
                }
            }
        }
        if (!typeSet.isEmpty()) {
            model.addAttribute("types", new ArrayList<>(typeSet));
        } else {
            model.addAttribute("types", null);
        }

        boolean accessory = false;
        if (variations.size() == 1) {
            Variation variation = variations.get(0);
            // accessory when no selectable options
            if (StringUtils.isBlank(variation.getColor()) && StringUtils.isBlank(variation.getSize()) && StringUtils.isBlank(variation.getType())) {
                accessory = true;
            }
        }
		model.addAttribute("accessory", accessory);

		return "product-detail";
	}
	
	@GetMapping(RequestPathConst.PRIVATE_POLICY)
    public String privatePolicy() {
        return ScreenPathConst.PRIVATE_POLICY_SCREEN;
    }

	private Map<String, Object> settingCondition(ProductSearchRequest searchForm) {
        Map<String, Object> conditionMaps = new HashMap<>();
        conditionMaps.put("category", searchForm.getCategory());
        return conditionMaps;
    }

    private CartForm getCartFormDefault(UserLoginInfo userLoginInfo, CartForm cartForm) throws CommonServletException {
        Account account = accountService.getAccount(userLoginInfo.getId());
        CartForm cartFormFromDb = cartService.findFirstByAccountId(userLoginInfo.getId());

        if (Objects.isNull(cartForm)) {
            cartForm = new CartForm();
        }

        if (CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            if (cartFormFromDb != null) {
                cartForm.setOrderItems(cartFormFromDb.getOrderItems());
            } else {
                cartForm.setOrderItems(new ArrayList<>());
            }
        } else if (account.isFirstLogin()) {
            account.setFirstLogin(false);
            accountService.save(account);
            List<OrderItem> existsOrderItems = cartFormFromDb != null ? cartFormFromDb.getOrderItems() : new ArrayList<>();
            List<OrderItem> orderItems = cartForm.getOrderItems();
            List<OrderItem> orderItemsDeleted = new ArrayList<>();
            for (OrderItem orderItem : orderItems) {
                for (OrderItem existsOrderItem : existsOrderItems) {
                    if (Objects.equals(orderItem.getVariationId(), existsOrderItem.getVariationId())) {
                        orderItemsDeleted.add(existsOrderItem);
                    }
                }
            }
            existsOrderItems.removeAll(orderItemsDeleted);
            existsOrderItems.addAll(orderItems);

            cartForm.setOrderItems(existsOrderItems);


            return cartForm;
        }
        return cartForm;
    }
    
    private List<ProductResponse> getLinkedProducts(Product product) {
        List<Product> productLinkeds = new ArrayList<>();
        List<String> productCodes = new ArrayList<>();
        if (StringUtils.isNotEmpty(product.getRelatedProduct1())) {
            productCodes.add(product.getRelatedProduct1());
        }
        if (StringUtils.isNotEmpty(product.getRelatedProduct2())) {
            productCodes.add(product.getRelatedProduct2());
        }
        if (StringUtils.isNotEmpty(product.getRelatedProduct3())) {
            productCodes.add(product.getRelatedProduct3());
        }
        if (StringUtils.isNotEmpty(product.getRelatedProduct4())) {
            productCodes.add(product.getRelatedProduct4());
        }
        
        if (!CollectionUtils.isEmpty(productCodes)) {
            productLinkeds = productService.getLinkedProducts(productCodes);
        }

        return productLinkeds.stream().map(ProductResponse::new).collect(Collectors.toList());
    }
}
