package vn.co.cake.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import vn.co.cake.common.DateConst;
import vn.co.cake.constants.OrderConstants;
import vn.co.cake.entity.*;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.*;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.request.SearchRequest;
import vn.co.cake.service.OrderService;
import vn.co.cake.utils.DateUtil;
import vn.co.cake.utils.StringUtil;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderSequenceRepository orderSequenceRepository;
    private final ProductRepository productRepository;
    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final VoucherRepository voucherRepository;
    private final VariationRepository variationRepository;

    public OrderServiceImpl(OrderRepository orderRepository,
                            AccountRepository accountRepository,
                            CartRepository cartRepository,
                            OrderItemRepository orderItemRepository,
                            OrderSequenceRepository orderSequenceRepository,
                            ProductRepository productRepository,
                            ProvinceRepository provinceRepository,
                            DistrictRepository districtRepository,
                            WardRepository wardRepository,
                            VoucherRepository voucherRepository,
                            VariationRepository variationRepository) {
        this.orderRepository = orderRepository;
        this.accountRepository = accountRepository;
        this.cartRepository = cartRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderSequenceRepository = orderSequenceRepository;
        this.productRepository = productRepository;
        this.provinceRepository = provinceRepository;
        this.districtRepository = districtRepository;
        this.wardRepository = wardRepository;
        this.voucherRepository = voucherRepository;
        this.variationRepository = variationRepository;
    }
    @Override
    public Page<Order> findAllByCondition(SearchRequest searchForm, Pageable pageable) {
        return orderRepository.findAllByCondition(searchForm, pageable);
    }

    @Override
    public Page<Order> findAllSyncFailOrders(SearchRequest searchRequest, Pageable pageable) {
        return orderRepository.findAllSyncFailOrders(searchRequest, pageable);
    }
    
    @Override
    public Order create(Long accountId, List<vn.co.cake.dto.OrderItem> newOrderItems, OrderDetailRequest request) throws CommonServletException {
        Account account = accountRepository.findByIdAndDeletedFalse(accountId);
        if (Objects.isNull(account)) {
            throw new CommonServletException("please login before order");
        }

        if (CollectionUtils.isEmpty(newOrderItems)) {
            throw new CommonServletException("Order failed!");
        }

        List<vn.co.cake.dto.OrderItem> itemOrders = newOrderItems.stream().filter(orderItem -> orderItem.getQuantity() > 0).collect(Collectors.toList());
        List<String> variationIds = itemOrders.stream().map(vn.co.cake.dto.OrderItem::getVariationId).collect(Collectors.toList());

        List<Variation> variations = variationRepository.findAllByVariationIdIn(variationIds);
        Map<String, Variation> variationMap = variations.stream().collect(Collectors.toMap(Variation::getVariationId, variation -> variation));
        if (CollectionUtils.isEmpty(itemOrders)) {
            throw new CommonServletException("Order failed!");
        }

        long discountPrice = 0;
        Voucher shippingFee = voucherRepository.findFirstByCodeAndDeletedIsFalse(OrderConstants.VOUCHER_SHIPPING_FEE);
        int fee = 0;
        BigDecimal totalPriceOrder = this.getTotalPriceOrder(itemOrders, discountPrice);
        if (shippingFee != null && totalPriceOrder.longValue() < OrderConstants.FREE_SHIPPING_THRESHOLD) {
            fee = shippingFee.getShippingFee();
        }

        Order order = new Order();
        order.setCode(this.getCodeMaxOrder());
        order.setAccount(account);
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus(OrderStatus.NEW.getValue());
        order.setShippingAddress(this.getAddressShipping(request));
        order.setTotalAmount(this.getTotalPriceOrder(itemOrders, discountPrice));
        order.setShippingFee(BigDecimal.valueOf(fee));

        order.setFullName(request.getFullName());
        order.setEmail(request.getEmail());
        order.setNote(request.getNote());
        order.setPhone(request.getPhone());
        order.setVoucher(request.getVoucher());
        orderRepository.save(order);

        List<OrderItem> orderItems = new ArrayList<>();
        for (vn.co.cake.dto.OrderItem cartItemRequest : itemOrders) {
            Variation variation = variationMap.get(cartItemRequest.getVariationId());
            if (Objects.isNull(variation)) {
                throw new CommonServletException("Sản phẩm order không tồn tại!");
            }

            if (variation.getRemainQuantity() < cartItemRequest.getQuantity()) {
                newOrderItems.remove(cartItemRequest);
                throw new CommonServletException(String.format("Sản phẩm [%s] đã bán hết! Chúng tôi sẽ xóa khỏi gi hàng của bạn", variation.getName()));
            }

            Product product = productRepository.findFirstByProductPancakeId(variation.getPancakeProductId());
            if (Objects.isNull(product)) {
                throw new CommonServletException("Sản phẩm không tồn tại trong hệ thống!");
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setVariation(variation);
            orderItem.setPrice(BigDecimal.valueOf(variation.getRetailPrice()));
            orderItem.setFinalPrice(product.getFinalPrice());
            orderItem.setDiscountPrice(product.getDiscountPrice());
            orderItem.setQuantity(cartItemRequest.getQuantity());
            orderItem.setOption(cartItemRequest.getOption());
            orderItems.add(orderItem);
        }
        orderItemRepository.saveAll(orderItems);
        order.setOrderItems(orderItems);
        
        return order;
    }

    @Override
    public Order detail(String code) {
        Order order = orderRepository.findFirstByCode(code);
        if (order != null) {
            // Tải orderItems trong session (tránh LazyInitializationException khi build OrderDetailResponse ở controller).
            order.getOrderItems().size();
        }
        return order;
    }

    @Override
    public Order detail(Long accountId) {
        Order order = orderRepository.findFirstByAccountIdOrderByCreatedDesc(accountId);
        if (order != null) {
            order.getOrderItems().size();
        }
        return order;
    }

    @Override
    public void delete(String code) throws CommonServletException {
        Order order = orderRepository.findFirstByCode(code);
        if (Objects.isNull(order)) {
            throw new CommonServletException("Order not found with code: " + code);
        }
        order.setDeleted(true);
        orderRepository.save(order);
    }

    private BigDecimal getTotalPriceOrder(List<vn.co.cake.dto.OrderItem> orderItems, long discountPrice) {
        long sum = orderItems.stream().mapToLong(orderItem -> orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity())).longValue()).sum();
        sum = sum - discountPrice;
        return BigDecimal.valueOf(sum);
    }

    private String getCodeMaxOrder() {
        int maxOrder = 1;
        String dateOrder = DateUtil.dateToString(DateUtil.now(), DateConst.YYMMDD);
        OrderSequence orderSequence = orderSequenceRepository.findFirstByDate(dateOrder);
        if (Objects.isNull(orderSequence)) {
            orderSequence = new OrderSequence();
            orderSequence.setMaxOrder(maxOrder);
            orderSequence.setDate(dateOrder);
            orderSequenceRepository.save(orderSequence);
        } else {
            maxOrder = orderSequence.getMaxOrder() + 1;
            orderSequence.setMaxOrder(maxOrder);
            orderSequenceRepository.save(orderSequence);

        }
        BigDecimal bigDecimal = new BigDecimal(maxOrder);
        String leadingZeros = StringUtil.formatWithLeadingZeros(bigDecimal);
        return dateOrder + leadingZeros;
    }
    
    private String getAddressShipping(OrderDetailRequest request) {
        Province province = provinceRepository.findFirstByCode(request.getProvince());
        District district = districtRepository.findFirstByCode(request.getDistrict());
        Ward ward = wardRepository.findFirstByCode(request.getWard());
        
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(request.getAddress())) {
            builder.append(request.getAddress()).append(StringUtils.SPACE);
        }
        if (Objects.nonNull(ward)) {
            builder.append(ward.getPathWithType());
        } else {
            if (Objects.nonNull(district)) {
                builder.append(district.getPathWithType());
            } else if (Objects.nonNull(province)) {
                builder.append(province.getNameWithType());
            }
        }

        return builder.toString();
    }
}
