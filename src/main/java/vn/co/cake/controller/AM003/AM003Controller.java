package vn.co.cake.controller.AM003;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.co.cake.common.RequestPathConst;
import vn.co.cake.common.ScreenPathConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.dto.ShippingDto;
import vn.co.cake.entity.Category;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.entity.Voucher;
import vn.co.cake.enums.Colors;
import vn.co.cake.enums.Sizes;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.CategoryRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.request.ProductCreateRequest;
import vn.co.cake.request.ProductSearchRequest;
import vn.co.cake.response.ProductResponse;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.ProductService;
import vn.co.cake.service.VariationService;
import vn.co.cake.service.aws.S3Service;
import vn.co.cake.utils.BigDecimalUtil;
import vn.co.cake.utils.PageUtil;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static vn.co.cake.common.BaseConst.REDIRECT;
import static vn.co.cake.common.StringConst.BOOKMARK_STAFF_KEY;

@Controller
@Slf4j
public class AM003Controller extends BaseController {
    private final AccountService accountService;
    private final ProductService productService;
    private final S3Service s3Service;
    private final CategoryRepository categoryRepository;
    private final VariationRepository variationRepository;
    private final VoucherRepository voucherRepository;
    private final VariationService variationService;

    public AM003Controller(AccountService accountService,
                           ProductService productService,
                           S3Service s3Service,
                           CategoryRepository categoryRepository,
                           VariationRepository variationRepository,
                           VoucherRepository voucherRepository,
                           VariationService variationService) {
        this.accountService = accountService;
        this.productService = productService;
        this.s3Service = s3Service;
        this.categoryRepository = categoryRepository;
        this.variationRepository = variationRepository;
        this.voucherRepository = voucherRepository;
        this.variationService = variationService;
    }

    /**
     * list master convert groups
     *
     * @param pageable paging
     * @return listMasterConvertGroups screen
     * */
    @GetMapping(RequestPathConst.AM003)
    public String listProducts(ProductSearchRequest searchForm,
                               @PageableDefault(
                                            size = SIZE_DEFAULT,
                                            sort = {SORT_DEFAULT},
                                            direction = Sort.Direction.DESC
                                          ) Pageable pageable,
                               Model model, HttpSession session) {

        String bookmarkUrl = accountService.getBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
        log.info("Get bookmark url form redis= {}", bookmarkUrl);

        AdminLoginInfo adminLoginInfo = getLoginInfoAdmin(session);
        if (adminLoginInfo == null) {
            return REDIRECT.concat(baseUrl).concat(RequestPathConst.AM001_LOGIN);
        }

        if (StringUtils.isNotEmpty(bookmarkUrl) && !baseUrl.concat(RequestPathConst.AM002).equals(bookmarkUrl)) {
            accountService.deletedBookmarkUrlLasted(BOOKMARK_STAFF_KEY);
            return REDIRECT.concat(bookmarkUrl);
        }
        
        setCurrentSearchRequestAndPageableSession(session, searchForm, pageable);
        PageUtil pageUtil = getPageSize(session);
        searchForm = getSessionProductForm(session);
        Page<Product> productResponses = productService.getAllByCondition(searchForm, pageUtil, true);
        List<Category> categories = categoryRepository.findAllByDeletedIsFalse();
        model.addAttribute("products", productResponses.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("colors", Colors.getValue());
        model.addAttribute("sizes", Sizes.getValue());
        setPaginationAttribute(pageUtil, productResponses.getTotalElements(), RequestPathConst.AM003, model, settingCondition(searchForm));
        model.addAttribute(SEARCH_CONDITION, searchForm);
        
        Voucher shippingFee = voucherRepository.findFirstByCodeAndDeletedIsFalse("SHIPPING_FEE");
        if (shippingFee == null) {
            shippingFee = new Voucher();
            shippingFee.setName("SHIPPING_FEE");
            shippingFee.setShippingFee(0);
        }
        model.addAttribute("shippingFee", shippingFee);
        
        Voucher discountPrice = voucherRepository.findFirstByCodeAndDeletedIsFalse("DISCOUNT_PRICE");
        if (discountPrice == null) {
            discountPrice = new Voucher();
            discountPrice.setName("DISCOUNT_PRICE");
            discountPrice.setDiscountPercent(0);
        }
        model.addAttribute("discountPrice", discountPrice);

        addSideMenu(model, RequestPathConst.AM003);
        removeSessionAttributes(session, SESSION_FROM_EDIT_PAGE);
        return ScreenPathConst.AM003_SCREEN;
    }
    
    @GetMapping(RequestPathConst.AM003_01)
    public String createProductView(Model model, HttpSession session, HttpServletResponse response) throws Exception {
        if (getLoginInfoAdmin(session) == null) {
            response.sendRedirect(baseUrl);
        }
        List<Category> categories = categoryRepository.findAllByDeletedIsFalse();
        model.addAttribute("categories", categories);
        model.addAttribute("product", new ProductResponse());
        model.addAttribute("colorsInit1", Colors.getValue().subList(0, 4));
        model.addAttribute("colorsInit2", Colors.getValue().subList(5, 9));
        model.addAttribute("sizesInit", Sizes.getValue());

        List<Product> products = productService.getAllOrderByNameAsc();
        model.addAttribute("relatedProducts", products);
        model.addAttribute("products", products);
        addSideMenu(model, RequestPathConst.AM003);
        return ScreenPathConst.AM003_01_SCREEN;
    }

    /**
     * Registration Product
     *
     * @return screen url
     * */
    @PostMapping(RequestPathConst.AM003_01_REGISTER)
    public ResponseEntity<String> createProduct(@RequestParam String name,
                                                @RequestParam String code,
                                                @RequestParam(required = false) String subCode,
                                                @RequestParam(required = false) String description,
                                                @RequestParam(required = false) String descriptionSize,
                                                @RequestParam(required = false) String colorBase,
                                                @RequestParam(required = false) String sizeBase,
                                                @RequestParam String price,
                                                @RequestParam(required = false) String discount,
                                                @RequestParam(required = false) Long stockQuantity,
                                                @RequestParam List<String> category,
                                                @RequestParam(required = false) String relatedProduct1,
                                                @RequestParam(required = false) String relatedProduct2,
                                                @RequestParam(required = false) String relatedProduct3,
                                                @RequestParam(required = false) String relatedProduct4,
                                                @RequestParam(value = "file") MultipartFile file,
                                                @RequestParam(value = "file1", required = false) MultipartFile file1,
                                                @RequestParam(value = "file2", required = false) MultipartFile file2,
                                                @RequestParam(value = "file3", required = false) MultipartFile file3,
                                                @RequestParam(value = "file4", required = false) MultipartFile file4,
                                                @RequestParam(value = "file5", required = false) MultipartFile file5,
                                                @RequestParam(value = "file6", required = false) MultipartFile file6,
                                                @RequestParam(value = "file7", required = false) MultipartFile file7,
                                                @RequestParam(value = "file8", required = false) MultipartFile file8,
                                                @RequestParam(value = "file9", required = false) MultipartFile file9,
                                                HttpServletResponse response,
                                                HttpSession session) {      
        String message = "Đăng Kí Sản Phẩm Đã Hoàn Thành";
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect("/SA/SA001/login");
            }

            if (StringUtils.isBlank(discount)) {
                discount = BigDecimal.ZERO.toString();
            }

            String priceCustom = BigDecimalUtil.formatDouble(price);
            String discountCustom = BigDecimalUtil.formatDouble(discount);
            String errorMessage = this.validateProduct(priceCustom, discountCustom, stockQuantity);
            if (StringUtils.isNotEmpty(errorMessage)) {
                return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
            }
            
            ProductCreateRequest request = new ProductCreateRequest();
            request.setName(name);
            request.setCode(code);
            request.setSubCode(subCode);
            request.setDescription(description);
            request.setDescriptionSize(descriptionSize);
            request.setColor(colorBase);
            request.setSizes(sizeBase);
            request.setPrice(BigDecimal.valueOf(Long.parseLong(priceCustom)));
            request.setDiscount(BigDecimal.ZERO);
            request.setStockQuantity(stockQuantity);
            if (StringUtils.isNotBlank(discountCustom)) {
                request.setDiscount(BigDecimal.valueOf(Long.parseLong(discountCustom)));
            }
            request.setCategory(String.join(",", category));
            if (StringUtils.isNotEmpty(relatedProduct1)) {
                request.setRelatedProduct1(relatedProduct1);
            }
            if (StringUtils.isNotEmpty(relatedProduct2)) {
                request.setRelatedProduct2(relatedProduct2);
            }
            if (StringUtils.isNotEmpty(relatedProduct3)) {
                request.setRelatedProduct3(relatedProduct3);
            }
            if (StringUtils.isNotEmpty(relatedProduct4)) {
                request.setRelatedProduct4(relatedProduct4);
            }

            if (!file.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file);
                request.setImageUrl(imageUrl);
            }
            if (!file1.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file1);
                request.setImage1Url(imageUrl);
            }
            if (!file2.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file2);
                request.setImage2Url(imageUrl);
            }
            if (!file3.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file3);
                request.setImage3Url(imageUrl);
            }
            if (!file4.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file4);
                request.setImage4Url(imageUrl);
            }
            if (!file5.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file5);
                request.setImage5Url(imageUrl);
            }
            if (!file6.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file6);
                request.setImage6Url(imageUrl);
            }
            if (!file7.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file7);
                request.setImage7Url(imageUrl);
            }
            if (!file8.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file8);
                request.setImage8Url(imageUrl);
            }
            if (!file9.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file9);
                request.setImage9Url(imageUrl);
            }

            productService.create(request);
        } catch (IOException | CommonServletException e) {
            log.error("AM003_REGISTER error: {}", e.getMessage());
            message = e.getMessage();
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }
    
    @GetMapping(RequestPathConst.AM003_02)
    public String updateProductView(@PathVariable("id") Long id,  Model model, HttpSession session,
                                    HttpServletResponse response) throws IOException, CommonServletException {
        if (getLoginInfoAdmin(session) == null) {
            response.sendRedirect(baseUrl);
        }
        Product product = productService.detail(id);
        List<Variation> variations = variationRepository.findAllByPancakeProductId(product.getProductPancakeId());
        model.addAttribute("variations", variations);
        model.addAttribute("totalRecord", variations.size());

        ProductResponse productResponse = new ProductResponse(product);
        List<Category> categories = categoryRepository.findAllByDeletedIsFalse();
        model.addAttribute("categories", categories);
        model.addAttribute("product", productResponse);
        model.addAttribute("colorsInit1", Colors.getValue().subList(0, 4));
        model.addAttribute("colorsInit2", Colors.getValue().subList(5, 9));
        model.addAttribute("sizesInit", Sizes.getValue());
        model.addAttribute("relatedProduct1", productResponse.getRelatedProduct1());
        model.addAttribute("relatedProduct2", productResponse.getRelatedProduct2());
        model.addAttribute("relatedProduct3", productResponse.getRelatedProduct3());
        model.addAttribute("relatedProduct4", productResponse.getRelatedProduct4());

        List<Product> products = productService.getAllOrderByNameAsc();
        List<Product> productList = products.stream().filter(item -> item.getId() != (product.getId())).collect(Collectors.toList());
        model.addAttribute("products", productList);
        
        addSideMenu(model, RequestPathConst.AM003);
        return ScreenPathConst.AM003_02_SCREEN;
    }

    /**
     * Update Product
     *
     * @return screen url
     * */
    @PostMapping(RequestPathConst.AM003_02_UPDATE)
    public ResponseEntity<String> updateProduct(@RequestParam Long id,
                                                @RequestParam String name,
                                                @RequestParam String code,
                                                @RequestParam(required = false) String subCode,
                                                @RequestParam(required = false) String description,
                                                @RequestParam(required = false) String descriptionSize,
                                                @RequestParam(required = false) String colorBase,
                                                @RequestParam(required = false) String sizeBase,
                                                @RequestParam String price,
                                                @RequestParam(required = false) String discount,
                                                @RequestParam(required = false) Long stockQuantity,
                                                @RequestParam List<String> category,
                                                @RequestParam(required = false) String relatedProduct1,
                                                @RequestParam(required = false) String relatedProduct2,
                                                @RequestParam(required = false) String relatedProduct3,
                                                @RequestParam(required = false) String relatedProduct4,
                                                @RequestParam(value = "file", required = false) MultipartFile file,
                                                @RequestParam(value = "file1", required = false) MultipartFile file1,
                                                @RequestParam(value = "file2", required = false) MultipartFile file2,
                                                @RequestParam(value = "file3", required = false) MultipartFile file3,
                                                @RequestParam(value = "file4", required = false) MultipartFile file4,
                                                @RequestParam(value = "file5", required = false) MultipartFile file5,
                                                @RequestParam(value = "file6", required = false) MultipartFile file6,
                                                @RequestParam(value = "file7", required = false) MultipartFile file7,
                                                @RequestParam(value = "file8", required = false) MultipartFile file8,
                                                @RequestParam(value = "file9", required = false) MultipartFile file9,
                                                HttpServletResponse response,
                                                HttpSession session) {
        String message = "Cập Nhập Sản Phẩm Đã Hoàn Thành";
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect("/SA/SA001/login");
            }

            if (StringUtils.isBlank(discount)) {
                discount = BigDecimal.ZERO.toString();
            }

            String priceCustom = BigDecimalUtil.formatDouble(price);
            String discountCustom = BigDecimalUtil.formatDouble(discount);

            String errorMessage = this.validateProduct(priceCustom, discountCustom, stockQuantity);
            if (StringUtils.isNotEmpty(errorMessage)) {
                return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
            }

            ProductCreateRequest request = new ProductCreateRequest();
            request.setId(id);
            request.setCode(code);
            request.setSubCode(subCode);
            request.setName(name);
            request.setDescription(description);
            request.setDescriptionSize(descriptionSize);
            request.setColor(colorBase);
            request.setSizes(sizeBase);
            request.setPrice(BigDecimal.valueOf(Long.parseLong(priceCustom)));
            request.setDiscount(BigDecimal.ZERO);
            request.setStockQuantity(stockQuantity);
            if (StringUtils.isNotBlank(discount)) {
                request.setDiscount(BigDecimal.valueOf(Long.parseLong(discountCustom)));
            }
            request.setCategory(String.join(",", category));
            if (StringUtils.isNotEmpty(relatedProduct1)) {
                request.setRelatedProduct1(relatedProduct1);
            }
            if (StringUtils.isNotEmpty(relatedProduct2)) {
                request.setRelatedProduct2(relatedProduct2);
            }
            if (StringUtils.isNotEmpty(relatedProduct3)) {
                request.setRelatedProduct3(relatedProduct3);
            }
            if (StringUtils.isNotEmpty(relatedProduct4)) {
                request.setRelatedProduct4(relatedProduct4);
            }

            if (!file.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file);
                request.setImageUrl(imageUrl);
            }
            if (!file1.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file1);
                request.setImage1Url(imageUrl);
            }
            if (!file2.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file2);
                request.setImage2Url(imageUrl);
            }
            if (!file3.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file3);
                request.setImage3Url(imageUrl);
            }
            if (!file4.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file4);
                request.setImage4Url(imageUrl);
            }
            if (!file5.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file5);
                request.setImage5Url(imageUrl);
            }
            if (!file6.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file6);
                request.setImage6Url(imageUrl);
            }
            if (!file7.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file7);
                request.setImage7Url(imageUrl);
            }
            if (!file8.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file8);
                request.setImage8Url(imageUrl);
            }
            if (!file9.isEmpty()) {
                String imageUrl = s3Service.uploadImageToS3(file9);
                request.setImage9Url(imageUrl);
            }

            productService.update(request);
        } catch (CommonServletException | IOException e) {
            log.error("AM003_UPDATE error: {}", e.getMessage());
            message = "Cập Nhập Sản Phẩm Đã Thất Bại!";
            return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(message, HttpStatus.CREATED);
    }

    /**
     * Delete Product
     *
     * @param id ProductId
     * @return screen url
     * */
    @PostMapping(RequestPathConst.AM003_DELETED)
    public void deleted(@RequestBody Long id, RedirectAttributes redirAttrs, HttpSession session,
                        HttpServletResponse response) {
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect("");
            }
            productService.deleted(id);
            redirAttrs.addFlashAttribute(SUCCESS, "Đã xóa.");
        } catch (CommonServletException | IOException e) {
            log.error(e.getMessage());
        }
    }

    @PostMapping(RequestPathConst.AM003_RESTORE)
    public void restore(@RequestBody Long id, RedirectAttributes redirAttrs, HttpSession session,
                        HttpServletResponse response) {
        try {
            if (getLoginInfoAdmin(session) == null) {
                response.sendRedirect("");
            }
            productService.restore(id);
            redirAttrs.addFlashAttribute(SUCCESS, "Đã thêm lại sản phẩm.");
        } catch (CommonServletException | IOException e) {
            log.error(e.getMessage());
        }
    }
    
    @PostMapping(RequestPathConst.AM003_SETTINGS)
    public ResponseEntity<String> settingShipping(@RequestBody ShippingDto shippingDto) throws CommonServletException {
        if (!BigDecimalUtil.shippingFeeValid(shippingDto.getShippingFee())) {
            throw new CommonServletException("Nhập giá shipping từ 0 ~ 100000 %");
        }
        Voucher shippingFee = voucherRepository.findFirstByCodeAndDeletedIsFalse("SHIPPING_FEE");
        if (shippingFee == null) {
            shippingFee = new Voucher();
        }
        shippingFee.setName(shippingDto.getName());
        shippingFee.setCode("SHIPPING_FEE");
        shippingFee.setShippingFee(shippingDto.getShippingFee());
        voucherRepository.save(shippingFee);
        
        return new ResponseEntity<>("Cập nhập cài đặt thành công", HttpStatus.OK);
    }
    
    @PostMapping(RequestPathConst.AM003_SETTING_DISCOUNT)
    public ResponseEntity<String> settingDiscount(@RequestBody ShippingDto discountDto) throws CommonServletException {
//        if (!BigDecimalUtil.discountValid(discountDto.getDiscountPercent())) {
//            throw new CommonServletException("Nhập mã giảm giá từ 0 ~ 99 %");
//        }
        
        Voucher discountPrice = voucherRepository.findFirstByCodeAndDeletedIsFalse("DISCOUNT_PRICE");
        if (discountPrice == null) {
            discountPrice = new Voucher();
        }
        discountPrice.setName(discountDto.getName());
        discountPrice.setCode("DISCOUNT_PRICE");
        discountPrice.setDiscountPercent(discountDto.getDiscountPercent());
        discountPrice.setStartDate(discountDto.getStartDate());
        discountPrice.setEndDate(discountDto.getEndDate());
        voucherRepository.save(discountPrice);
        
//        productService.updateProductDiscountPrice(discountDto);
        return new ResponseEntity<>("Cập nhập cài đặt thành công", HttpStatus.OK);
    }

    @PutMapping("/variations/{variationId}")
    public ResponseEntity<?> updateVariationImage(@PathVariable Long variationId, @RequestParam("image") MultipartFile imageFile) {

        try {
            String imageUrl = s3Service.uploadImageToS3(imageFile);
            variationService.update(variationId, imageUrl);
            return ResponseEntity.ok("Cập nhật ảnh thành công.");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Không tìm thấy variation với ID: " + variationId);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private String validateProduct(String price, String discount, Long stockQuantity) {
        String message = StringUtils.EMPTY;
        if (!BigDecimalUtil.priceValid(price)) {
            message = "Nhập giá sản phẩm từ 0 ~ 999999999 VND";
        }
        
        if (!BigDecimalUtil.discountValid(discount)) {
            message = "Nhập mã giảm giá từ 0 ~ 99 %";
        }

        return message;
    }
    
    private Map<String, Object> settingCondition(ProductSearchRequest searchForm) {
        Map<String, Object> conditionMaps = new HashMap<>();
        return conditionMaps;
    }
}
