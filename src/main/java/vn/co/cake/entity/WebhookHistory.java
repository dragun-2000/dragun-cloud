package vn.co.cake.entity;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "webhook_history")
public class WebhookHistory extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String payload;
}
