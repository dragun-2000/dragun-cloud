package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Variation;

import java.util.List;

public interface VariationRepository extends JpaRepository<Variation, Long> {
    Variation findFirstByVariationId(String variationId);
    Variation findFirstByPancakeProductIdAndSize(String productPancakeId, String size);
    List<Variation> findAllByPancakeProductId(String pancakeProductId);
    List<Variation> findAllByVariationIdIn(List<String> variationIds);
    List<Variation> findAllByPancakeProductIdAndColor(String pancakeProductId, String color);
}
