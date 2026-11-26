package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Cart findFirstCartByAccountId(Long accountId);
}
