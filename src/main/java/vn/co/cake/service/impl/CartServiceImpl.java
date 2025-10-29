package vn.co.cake.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Cart;
import vn.co.cake.entity.CartItem;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.repository.CartItemRepository;
import vn.co.cake.repository.CartRepository;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.service.CartService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final AccountRepository accountRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           AccountRepository accountRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository, 
                           VariationRepository variationRepository) {
        this.cartRepository = cartRepository;
        this.accountRepository = accountRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.variationRepository = variationRepository;
    }

    @Override
    public CartForm findFirstByAccountId(Long accountId) {
        CartForm cartForm = new CartForm();
        List<OrderItem> orderItems = new ArrayList<>();
        Cart cart = cartRepository.findFirstCartByAccountId(accountId);
        if (Objects.nonNull(cart) && !CollectionUtils.isEmpty(cart.getCartItems())) {
            cart.getCartItems().forEach(cartItem -> {
                Product product = productRepository.findFirstByProductPancakeIdAndDeletedIsFalse(cartItem.getVariation().getPancakeProductId());
                OrderItem orderItem = this.toOrderItem(cartItem, product);
                orderItems.add(orderItem);
            });
        }
        cartForm.setOrderItems(orderItems);

        return cartForm;
    }

    @Override
    public void create(CartForm cartForm, Long accountId, String variationId) throws CommonServletException {
        Account account = accountRepository.findFirstByIdAndDeletedIsFalse(accountId);
        if (Objects.isNull(account)) return;

        List<OrderItem> orderItems = cartForm.getOrderItems();
        if (CollectionUtils.isEmpty(orderItems)) return;

        Cart cart = cartRepository.findFirstCartByAccountId(accountId);
        List<CartItem> newCartItems = new ArrayList<>();
        if (Objects.isNull(cart)) {
            cart = new Cart();
            cart.setAccount(account);
            cartRepository.save(cart);

            for (OrderItem orderItem : orderItems) {
                Variation variation = variationRepository.findFirstByVariationId(orderItem.getVariationId());
                Product product = productRepository.findFirstByProductPancakeIdAndDeletedIsFalse(variation.getPancakeProductId());
                CartItem cartItem = new CartItem(orderItem, cart, variation, product);
                newCartItems.add(cartItem);
            }

            if (!CollectionUtils.isEmpty(newCartItems)) {
                cartItemRepository.saveAll(newCartItems);
            }
            cartRepository.save(cart);
        } else {
            Set<CartItem> cartItemDeletedList = new HashSet<>();
            List<CartItem> cartItemExists = cart.getCartItems();
            Map<String, List<OrderItem>> orderItemMap = orderItems.stream().collect(Collectors.groupingBy(OrderItem::getVariationId));
            List<OrderItem> orderItemRequests = orderItemMap.get(variationId);
            for (OrderItem orderItem : orderItemRequests) {
                for (CartItem cartItem : cartItemExists) {
                    if (Objects.equals(orderItem.getVariationId(), cartItem.getVariation().getVariationId())) {
                        cartItemDeletedList.add(cartItem);
                    }
                }
                Variation variation = variationRepository.findFirstByVariationId(orderItem.getVariationId());
                Product product = productRepository.findFirstByProductPancakeIdAndDeletedIsFalse(variation.getPancakeProductId());
                CartItem cartItemNew = new CartItem(orderItem, cart, variation, product);
                newCartItems.add(cartItemNew);
            }

            if (!CollectionUtils.isEmpty(cartItemDeletedList)) {
                cartItemExists.removeAll(cartItemDeletedList);
                cartItemRepository.deleteAll(cartItemDeletedList);
            }

            cartItemExists.addAll(newCartItems);
            if (!CollectionUtils.isEmpty(cartItemExists)) {
                cartItemRepository.saveAll(cartItemExists);
            }
        }
    }

    @Override
    public List<OrderItem> findOtherProductItem(Long accountId, Long productId) {
        List<OrderItem> orderItems = new ArrayList<>();
        Account account = accountRepository.findFirstByIdAndDeletedIsFalse(accountId);
        if (Objects.isNull(account)) return orderItems;

        Cart cart = cartRepository.findFirstCartByAccountId(accountId);
        if (Objects.isNull(cart) || CollectionUtils.isEmpty(cart.getCartItems())) return orderItems;

        cart.getCartItems().forEach(cartItem -> {
            if (cartItem.getVariation().getId() != productId) {
                Product product = productRepository.findFirstByProductPancakeIdAndDeletedIsFalse(cartItem.getVariation().getPancakeProductId());
                OrderItem orderItem = this.toOrderItem(cartItem, product);
                orderItems.add(orderItem);
            }
        });

        return orderItems;
    }

    @Override
    public void deletedCartByAccount(Long accountId) {
        Cart cart = cartRepository.findFirstCartByAccountId(accountId);
        if (Objects.isNull(cart)) return;
        List<CartItem> cartItems = cartItemRepository.findAllByCart(cart);
        if (cartItems.isEmpty()) return;
        cartItemRepository.deleteAll(cartItems);
        cartRepository.delete(cart);
    }

    public OrderItem toOrderItem(CartItem cartItem, Product product) {
        OrderItem orderItem = new OrderItem();
        orderItem.setVariationId(cartItem.getVariation().getVariationId());
        orderItem.setName(cartItem.getVariation().getName());
        orderItem.setQuantity(cartItem.getQuantity());
        orderItem.setPrice(BigDecimal.valueOf(cartItem.getVariation().getRetailPrice()));
        // Guard against null product (e.g., product deleted or not found)
        String image = null;
        if (product != null) {
            image = product.getImage();
        }
        if (image == null || image.isEmpty()) {
            image = cartItem.getVariation().getImage();
        }
        orderItem.setImage(image);
        // Normalize option from variation fields: color[/type]/size when available
        Variation v = cartItem.getVariation();
        String color = v.getColor();
        String size = v.getSize();
        String type = v.getType();
        List<String> parts = new ArrayList<>();
        if (color != null && !color.trim().isEmpty()) parts.add(color.trim());
        if (type != null && !type.trim().isEmpty()) parts.add(type.trim());
        if (size != null && !size.trim().isEmpty()) parts.add(size.trim());
        if (!parts.isEmpty()) {
            orderItem.setOption(String.join("/", parts));
        } else {
            orderItem.setOption(cartItem.getOption());
        }
        return orderItem;
    }
}
