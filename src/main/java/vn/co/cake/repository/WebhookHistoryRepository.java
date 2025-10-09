package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.WebhookHistory;

public interface WebhookHistoryRepository extends JpaRepository<WebhookHistory, Long> {
}
