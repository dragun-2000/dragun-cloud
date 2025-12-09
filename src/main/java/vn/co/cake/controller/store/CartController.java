package vn.co.cake.controller.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.common.DateConst;
import vn.co.cake.controller.BaseController;
import vn.co.cake.controller.external.dto.OrderPancakeResponse;
import vn.co.cake.controller.external.dto.ShippingAddress;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.Province;
import vn.co.cake.entity.Voucher;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.entity.Variation;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.service.AccountService;
import vn.co.cake.service.CartService;
import vn.co.cake.service.OrderService;
import vn.co.cake.service.external.PancakePosService;
import vn.co.cake.utils.BigDecimalUtil;
import vn.co.cake.utils.DateUtil;
import vn.co.cake.utils.FunctionUtil;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Controller
@SessionAttributes("cartForm") 
public class CartController extends BaseController {
    private final CartService cartService;
    private final AccountService accountService;
    private final ProvinceRepository provinceRepository;
    private final VoucherRepository voucherRepository;
    private final OrderService orderService;
    private final PancakePosService pancakePosService;
    private final VariationRepository variationRepository;
    private final OrderRepository orderRepository;

    public CartController(CartService cartService,
                          AccountService accountService,
                          ProvinceRepository provinceRepository,
                          VoucherRepository voucherRepository,
                          OrderService orderService,
                          PancakePosService pancakePosService,
                          VariationRepository variationRepository,
                          OrderRepository orderRepository) {
        this.cartService = cartService;
        this.accountService = accountService;
        this.provinceRepository = provinceRepository;
        this.voucherRepository = voucherRepository;
        this.orderService = orderService;
        this.pancakePosService = pancakePosService;
        this.variationRepository = variationRepository;
        this.orderRepository = orderRepository;
    }

    @ModelAttribute("cartForm")
    public CartForm createCart() {
        return new CartForm();
    }

    @GetMapping("/cart-detail") 
    public String cartDetail(Model model, @ModelAttribute("cartForm") CartForm cartForm, HttpSession session) throws JsonProcessingException, CommonServletException {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (loginInfo != null) {
            Account account = accountService.getAccount(loginInfo.getId());
            model.addAttribute("account", account);
        }
        boolean isLogin = loginInfo != null;
        model.addAttribute("isLogin", isLogin);
        
        if ((Objects.isNull(cartForm) || CollectionUtils.isEmpty(cartForm.getOrderItems())) && loginInfo != null) {
            CartForm cartFormFromDb = cartService.findFirstByAccountId(loginInfo.getId());
            if (cartFormFromDb != null) {
                cartForm.setOrderItems(cartFormFromDb.getOrderItems());
            } else {
                cartForm.setOrderItems(new ArrayList<>());
            }
            Account account = accountService.getAccount(loginInfo.getId());
            model.addAttribute("account", account);
        }

        List<OrderItem> orderItems = new ArrayList<>();
        if (Objects.nonNull(cartForm) && !CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            orderItems = cartForm.getOrderItems();
            // Normalize option string to color[/type]/size based on variation data
            for (OrderItem item : orderItems) {
                try {
                    Variation v = variationRepository.findFirstByVariationId(item.getVariationId());
                    if (v != null) {
                        String color = v.getColor();
                        String type = v.getType();
                        String size = v.getSize();
                        List<String> parts = new ArrayList<>();
                        if (color != null && !color.trim().isEmpty()) parts.add(color.trim());
                        if (type != null && !type.trim().isEmpty()) parts.add(type.trim());
                        if (size != null && !size.trim().isEmpty()) parts.add(size.trim());
                        if (!parts.isEmpty()) {
                            item.setOption(String.join("/", parts));
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }

        model.addAttribute("totalPrice", this.getTotalPrice(orderItems, 0));
        model.addAttribute("totalPriceDisplay", BigDecimalUtil.formatMoney(this.getTotalPrice(orderItems, 0)));

        FunctionUtil.updateCartQuantity(model, cartForm);
        ObjectMapper objectMapper = new ObjectMapper();
        String orderItemsJson = objectMapper.writeValueAsString(orderItems);
        model.addAttribute("orderItems", orderItemsJson);
        return "cart-detail";
    }
    
    @PostMapping("/add-to-cart")
    public ResponseEntity<Void> addToCart(@ModelAttribute("cartForm") CartForm cartForm, HttpSession session,
                                          @RequestBody List<OrderItem> newOrderItems) throws CommonServletException {
        UserLoginInfo loginInfo = getLoginInfo(session);
        this.updateCartGuest(cartForm, newOrderItems);
        String variationId = newOrderItems.stream().map(OrderItem::getVariationId).findFirst().orElse(null);
        if (loginInfo != null) {
            cartService.create(cartForm, loginInfo.getId(), variationId);
        }
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/update-to-cart")
    public ResponseEntity<Void> updateToCart(@ModelAttribute("cartForm") CartForm cartForm,
                                          @RequestBody List<OrderItem> newOrderItems) {
        cartForm.setOrderItems(newOrderItems);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/payment/voucher")
    public ResponseEntity<Integer> updateToCart(@RequestParam(required = false) String code) {
        Voucher voucher = voucherRepository.findFirstByCodeAndDeletedIsFalse(code);
        if (voucher == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(voucher.getDiscountPercent());
    }

    @GetMapping("/payment-detail")
    public String paymentDetail(Model model, @ModelAttribute("cartForm") CartForm cartForm, HttpSession session) throws CommonServletException {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (loginInfo == null) {
            return "login";
        }
        Account account = accountService.getAccount(loginInfo.getId());
        model.addAttribute("account", account);
        model.addAttribute("isLogin", true);

        if (Objects.isNull(cartForm) || CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            CartForm cartFormFromDb = cartService.findFirstByAccountId(loginInfo.getId());
            if (cartFormFromDb != null) {
                cartForm.setOrderItems(cartFormFromDb.getOrderItems());
            } else {
                cartForm.setOrderItems(new ArrayList<>());
            }
            model.addAttribute("account", account);
        }
        
        model.addAttribute("orderDetail", this.init(account));
        
        List<Province> provinces = provinceRepository.findAll();
        model.addAttribute("provinces", provinces);

        List<OrderItem> orderItems = new ArrayList<>();
        if (Objects.nonNull(cartForm) && !CollectionUtils.isEmpty(cartForm.getOrderItems())) {
            orderItems = cartForm.getOrderItems();
        }
        String totalPrice = this.getTotalPrice(orderItems, 0);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("totalPriceDisplay", BigDecimalUtil.formatMoney(this.getTotalPrice(orderItems, 0)));

        Voucher shippingFee = voucherRepository.findFirstByCodeAndDeletedIsFalse("SHIPPING_FEE");
        int shippingFeeDefault = 0;
        if (shippingFee != null && Long.parseLong(totalPrice) < 1000000) {
            shippingFeeDefault = shippingFee.getShippingFee();
        }
        model.addAttribute("shippingFee", shippingFeeDefault);
        model.addAttribute("totalPriceDisplayFinal", BigDecimalUtil.formatMoney(this.getTotalPrice(orderItems, shippingFeeDefault)));

        FunctionUtil.updateCartQuantity(model, cartForm);
        return "payment-detail";
    }
    
    @GetMapping("/order-history")
    public String orderDetail(Model model, @ModelAttribute("cartForm") CartForm cartForm, HttpSession session) {
        try {
            UserLoginInfo loginInfo = getLoginInfo(session);
            if (Objects.isNull(loginInfo)) {
                return "login";
            }
            Account account = accountService.getAccount(loginInfo.getId());
            if (account == null) {
                return "login";
            }
            
            // Get phone number, use empty string if null to avoid NPE
            String phone = account.getPhone();
            if (phone == null || phone.trim().isEmpty()) {
                log.warn("Account {} has no phone number", account.getId());
                model.addAttribute("orderPancake", new ArrayList<>());
                model.addAttribute("account", account);
                model.addAttribute("isLogin", true);
                FunctionUtil.updateCartQuantity(model, cartForm);
                return "order-status";
            }
            
            log.info("Loading order history for account ID: {}, phone: {}", account.getId(), phone);
            
            List<OrderPancakeResponse> allOrders = new ArrayList<>();
            
            // 1. Query orders từ database theo phone
            List<Order> dbOrders = orderRepository.findByPhoneAndDeletedFalseOrderByCreatedDesc(phone);
            log.info("Found {} orders in database for phone: {}", dbOrders.size(), phone);
            
            // 2. Phân loại orders theo status
            List<Order> syncFailOrders = dbOrders.stream()
                .filter(order -> OrderStatus.SYNC_FAIL.getValue().equals(order.getStatus()))
                .collect(Collectors.toList());
            
            List<Order> newOrders = dbOrders.stream()
                .filter(order -> OrderStatus.NEW.getValue().equals(order.getStatus()))
                .collect(Collectors.toList());
            
            log.info("Orders breakdown - SYNC_FAIL: {}, NEW: {}", syncFailOrders.size(), newOrders.size());
            
            // 3. Map SYNC_FAIL orders → OrderPancakeResponse
            for (Order order : syncFailOrders) {
                OrderPancakeResponse response = mapOrderToOrderPancakeResponse(order);
                allOrders.add(response);
            }
            
            // 4. Lấy NEW orders từ Pancake POS API
            if (!newOrders.isEmpty()) {
                List<OrderPancakeResponse> pancakeOrders = pancakePosService.getAllOrderPancake(phone, 0, 1000);
                if (pancakeOrders != null && !pancakeOrders.isEmpty()) {
                    allOrders.addAll(pancakeOrders);
                    log.info("Retrieved {} orders from Pancake POS", pancakeOrders.size());
                }
            }
            
            // 5. Format và sort
            formatOrderPancakeResponses(allOrders);
            sortOrdersByDate(allOrders);
            
            log.info("Total orders to display: {}", allOrders.size());
            model.addAttribute("orderPancake", allOrders);
            model.addAttribute("account", account);
            model.addAttribute("isLogin", true);
            FunctionUtil.updateCartQuantity(model, cartForm);

        } catch (Exception e) {
            log.error("Error loading order history: {}", e.getMessage(), e);
            return "login";
        }
        return "order-status";
    }
    
    private OrderPancakeResponse mapOrderToOrderPancakeResponse(Order order) {
        OrderPancakeResponse response = new OrderPancakeResponse();
        response.setBill_full_name(order.getFullName() != null ? order.getFullName() : "N/A");
        
        // Format date
        if (order.getCreated() != null) {
            String orderDate = DateUtil.dateToString(order.getCreated(), DateConst.YYYY_MM_DD_T_HH_MM_SS);
            response.setInserted_at(orderDate);
        }
        
        // Format money
        if (order.getTotalAmount() != null) {
            String money = BigDecimalUtil.formatMoney(order.getTotalAmount()) + " VND";
            response.setMoney_to_collect(money);
        }
        
        // Set status name
        response.setStatus_name("Đang xử lý");
        
        // Create ShippingAddress
        ShippingAddress shippingAddress = new ShippingAddress();
        shippingAddress.setPhone_number(order.getPhone() != null ? order.getPhone() : "N/A");
        shippingAddress.setFull_address(order.getShippingAddress() != null ? order.getShippingAddress() : "N/A");
        shippingAddress.setFull_name(order.getFullName() != null ? order.getFullName() : "N/A");
        response.setShipping_address(shippingAddress);
        
        // tracking_link null cho SYNC_FAIL orders
        response.setTracking_link(null);
        
        return response;
    }
    
    private void formatOrderPancakeResponses(List<OrderPancakeResponse> orders) {
        if (orders == null) {
            return;
        }
        
        orders.forEach(orderPancakeResponse -> {
            if (orderPancakeResponse != null) {
                // Format date nếu chưa format
                if (orderPancakeResponse.getInserted_at() != null && 
                    orderPancakeResponse.getInserted_at().contains("T")) {
                    try {
                        String orderDate = DateUtil.stringToStringFormat(
                            orderPancakeResponse.getInserted_at(), 
                            DateConst.YYYY_MM_DD_T_HH_MM_SS
                        );
                        orderPancakeResponse.setInserted_at(orderDate);
                    } catch (Exception e) {
                        log.warn("Failed to format date: {}", orderPancakeResponse.getInserted_at());
                    }
                }
                
                // Format money nếu chưa format (chưa có "VND")
                if (orderPancakeResponse.getMoney_to_collect() != null && 
                    !orderPancakeResponse.getMoney_to_collect().contains("VND")) {
                    try {
                        // Try to parse as number
                        String moneyStr = orderPancakeResponse.getMoney_to_collect().replaceAll("[^0-9]", "");
                        if (!moneyStr.isEmpty()) {
                            BigDecimal money = new BigDecimal(moneyStr);
                            String formattedMoney = BigDecimalUtil.formatMoney(money) + " VND";
                            orderPancakeResponse.setMoney_to_collect(formattedMoney);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to format money: {}", orderPancakeResponse.getMoney_to_collect());
                    }
                }
            }
        });
    }
    
    private void sortOrdersByDate(List<OrderPancakeResponse> orders) {
        if (orders == null || orders.isEmpty()) {
            return;
        }
        
        orders.sort((o1, o2) -> {
            if (o1.getInserted_at() == null && o2.getInserted_at() == null) {
                return 0;
            }
            if (o1.getInserted_at() == null) {
                return 1;
            }
            if (o2.getInserted_at() == null) {
                return -1;
            }
            // Sort descending (newest first)
            return o2.getInserted_at().compareTo(o1.getInserted_at());
        });
    }
    
    private OrderDetailRequest init(Account account) {
        OrderDetailRequest request = new OrderDetailRequest();
        request.setFullName(account.getFullName());
        request.setEmail(account.getMailAddress());
        request.setPhone(account.getPhone());
        request.setAddress(account.getFloor());
        request.setProvince(account.getProvince());
        request.setDistrict(account.getDistrict());
        request.setWard(account.getWard());
        return request;
    }
    
    private void updateCartGuest(CartForm cartForm, List<OrderItem> newOrderItems) {
        if (CollectionUtils.isEmpty(newOrderItems)) return;
        if (cartForm == null) {
            cartForm = new CartForm();
        }

        List<OrderItem> orderItems = cartForm.getOrderItems();
        if (CollectionUtils.isEmpty(orderItems)) {
            cartForm.setOrderItems(newOrderItems);
        } else {
            String variationId = newOrderItems.stream().map(OrderItem::getVariationId).findFirst().orElse(null);
            
            List<OrderItem> orderItemDeletes = new ArrayList<>();
            orderItems.forEach(orderItem -> {
                if (Objects.equals(orderItem.getVariationId(), variationId)) {
                    orderItemDeletes.add(orderItem);
                }
            });
            
            orderItems.removeAll(orderItemDeletes);
            orderItems.addAll(newOrderItems);
        }
    }
    
    private String getTotalPrice(List<OrderItem> orderItems, long feeShipping) {
        if (CollectionUtils.isEmpty(orderItems)) return "0";
        int sum = orderItems.stream().mapToInt(orderItem -> {
            BigDecimal totalPrice = orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
            return totalPrice.intValue();
        }).sum();
        sum += feeShipping;
        return BigDecimal.valueOf(sum).toString();
    }
}
