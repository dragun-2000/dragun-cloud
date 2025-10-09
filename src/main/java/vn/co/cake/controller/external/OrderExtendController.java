package vn.co.cake.controller.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.controller.external.dto.OrderExtendDTO;
import vn.co.cake.service.external.OrderExtendService;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/extend/orders")
public class OrderExtendController {

    @Autowired
    private OrderExtendService orderExtendService;

    @PostMapping
    public OrderExtendDTO createOrder(@RequestBody OrderExtendDTO orderDTO) {
        log.info(">>> createOrder");
        return orderExtendService.createOrder(orderDTO);
    }

    @GetMapping
    public List<OrderExtendDTO> getAllOrders() {
        return orderExtendService.getAllOrders();
    }

    @GetMapping("/{id}")
    public Optional<OrderExtendDTO> getOrderById(@PathVariable Long id) {
        return orderExtendService.getOrderById(id);
    }

    @PutMapping("/{id}/status")
    public void updateOrderStatus(@PathVariable Long id, @RequestParam String status) {
        orderExtendService.updateOrderStatus(id, status);
    }

    @GetMapping("/customer/{id}")
    public List<OrderExtendDTO> getOrdersByCustomer(@PathVariable Long id) {
        return orderExtendService.getOrdersByCustomer(id);
    }
}
