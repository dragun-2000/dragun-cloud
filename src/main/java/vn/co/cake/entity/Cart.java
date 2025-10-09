package vn.co.cake.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.Data;
import org.springframework.util.CollectionUtils;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "carts")
public class Cart extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long cartId;

    // Quan hệ N-1: Nhiều giỏ hàng thuộc về một người dùng
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL)
    private List<CartItem> cartItems;
    
    public Cart() {
        this.account = new Account();
        this.cartItems = new ArrayList<>();
    }

    public long getTotal() {
        return 0;
    }
}
