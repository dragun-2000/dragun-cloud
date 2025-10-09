package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Cart;

public interface CartRepository extends JpaRepository<Cart, Long>, CustomCartRepository {
    Cart findFirstCartByCartId(Long id);
    Cart findFirstCartByAccountId(Long accountId);
}
