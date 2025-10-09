package vn.co.cake.service.external;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.co.cake.controller.external.dto.OrderExtendDTO;
import vn.co.cake.dto.GenericMailForm;
import vn.co.cake.entity.external.CustomerInfoExtend;
import vn.co.cake.entity.external.OrderExtend;
import vn.co.cake.entity.external.OrderItemExtend;
import vn.co.cake.helper.EmailService;
import vn.co.cake.repository.OrderExtendRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrderExtendService {

    @Autowired
    private OrderExtendRepository orderExtendRepository;
    private final EmailService emailService;

    public OrderExtendService(EmailService emailService) {
        this.emailService = emailService;
    }

    public OrderExtendDTO createOrder(OrderExtendDTO orderDTO) {
        OrderExtend order = new OrderExtend();
        // Map DTO to Entity
        order.setType(orderDTO.getType());
        order.setItems(orderDTO.getItems().stream().map(itemDTO -> {
            OrderItemExtend item = new OrderItemExtend();
            item.setProductId(itemDTO.getProductId());
            item.setQuantity(itemDTO.getQuantity());
            item.setPrice(itemDTO.getPrice());
            item.setSubtotal(itemDTO.getSubtotal());
            item.setImageUrl(itemDTO.getImageUrl());
            return item;
        }).collect(Collectors.toList()));
        order.setCustomerInfo(new CustomerInfoExtend(orderDTO.getCustomerInfo().getName(),
                                                    orderDTO.getCustomerInfo().getPhone(),
                                                    orderDTO.getCustomerInfo().getAddress()));
        order.setTotal(orderDTO.getTotal());
        order.setStatus("PENDING");
        order.setCreatedAt(new Date());
        order.setUpdatedAt(new Date());

        order = orderExtendRepository.save(order);

        orderDTO.setId(order.getId());
        
        GenericMailForm genericMailForm = GenericMailForm.builder()
                .url("https://www.phodem.click")
                .build();
        emailService.sendEmail("trinhhai28041994@gmail.com", genericMailForm);
        return orderDTO;
    }

    public List<OrderExtendDTO> getAllOrders() {
        List<OrderExtend> orders = orderExtendRepository.findAll();
        return orders.stream().map(order -> {
            OrderExtendDTO orderDTO = new OrderExtendDTO();
            orderDTO.setId(order.getId());
            orderDTO.setType(order.getType());
            orderDTO.setItems(order.getItems().stream().map(item -> {
                OrderExtendDTO.OrderItemExtendDTO itemDTO = new OrderExtendDTO.OrderItemExtendDTO();
                itemDTO.setProductId(item.getProductId());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setSubtotal(item.getSubtotal());
                itemDTO.setImageUrl(item.getImageUrl());
                return itemDTO;
            }).collect(Collectors.toList()));
            orderDTO.setTotal(order.getTotal());
            orderDTO.setStatus(order.getStatus());
            orderDTO.setCreatedAt(order.getCreatedAt().toString());
            orderDTO.setUpdatedAt(order.getUpdatedAt().toString());

            CustomerInfoExtend customerInfo = order.getCustomerInfo();
            orderDTO.setCustomerInfo(new OrderExtendDTO.CustomerInfoExtendDTO(
                                                        customerInfo.getName(), 
                                                        customerInfo.getPhone(), 
                                                        customerInfo.getAddress())
                                                        );
            return orderDTO;
        }).collect(Collectors.toList());
    }

    public Optional<OrderExtendDTO> getOrderById(Long id) {
        Optional<OrderExtend> order = orderExtendRepository.findById(id);
        return order.map(orderExtend -> {
            OrderExtendDTO orderDTO = new OrderExtendDTO();
            orderDTO.setId(orderExtend.getId());
            orderDTO.setType(orderExtend.getType());
            orderDTO.setItems(orderExtend.getItems().stream().map(item -> {
                OrderExtendDTO.OrderItemExtendDTO itemDTO = new OrderExtendDTO.OrderItemExtendDTO();
                itemDTO.setProductId(item.getProductId());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setSubtotal(item.getSubtotal());
                itemDTO.setImageUrl(item.getImageUrl());
                return itemDTO;
            }).collect(Collectors.toList()));
            orderDTO.setTotal(orderExtend.getTotal());
            orderDTO.setStatus(orderExtend.getStatus());
            orderDTO.setCreatedAt(orderExtend.getCreatedAt().toString());
            orderDTO.setUpdatedAt(orderExtend.getUpdatedAt().toString());
            CustomerInfoExtend customerInfo = orderExtend.getCustomerInfo();
            orderDTO.setCustomerInfo(new OrderExtendDTO.CustomerInfoExtendDTO(
                                                        customerInfo.getName(), 
                                                        customerInfo.getPhone(), 
                                                        customerInfo.getAddress())
                                                        );
            return orderDTO;
        });
    }

    public void updateOrderStatus(Long orderId, String status) {
        Optional<OrderExtend> order = orderExtendRepository.findById(orderId);
        order.ifPresent(o -> {
            o.setStatus(status);
            o.setUpdatedAt(new Date());
            orderExtendRepository.save(o);
        });
    }

    public List<OrderExtendDTO> getOrdersByCustomer(Long customerId) {
        // Example of filtering by customer info or other field
        List<OrderExtend> orders = orderExtendRepository.findByCustomerInfoName("John Doe");
        return orders.stream().map(order -> {
            OrderExtendDTO orderDTO = new OrderExtendDTO();
            orderDTO.setId(order.getId());
            orderDTO.setType(order.getType());
            orderDTO.setItems(order.getItems().stream().map(item -> {
                OrderExtendDTO.OrderItemExtendDTO itemDTO = new OrderExtendDTO.OrderItemExtendDTO();
                itemDTO.setProductId(item.getProductId());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setPrice(item.getPrice());
                itemDTO.setSubtotal(item.getSubtotal());
                itemDTO.setImageUrl(item.getImageUrl());
                return itemDTO;
            }).collect(Collectors.toList()));
            orderDTO.setTotal(order.getTotal());
            orderDTO.setStatus(order.getStatus());
            orderDTO.setCreatedAt(order.getCreatedAt().toString());
            orderDTO.setUpdatedAt(order.getUpdatedAt().toString());
            
            CustomerInfoExtend customerInfo = order.getCustomerInfo();
            orderDTO.setCustomerInfo(new OrderExtendDTO.CustomerInfoExtendDTO(
                                                        customerInfo.getName(), 
                                                        customerInfo.getPhone(), 
                                                        customerInfo.getAddress())
                                                        );
            return orderDTO;
        }).collect(Collectors.toList());
    }
}
