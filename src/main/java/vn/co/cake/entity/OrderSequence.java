package vn.co.cake.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "order_sequence")
public class OrderSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String date;

    private int maxOrder;
}
