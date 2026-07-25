package vn.co.cake.repository;

import java.util.List;

import javax.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.co.cake.entity.Variation;

public interface VariationRepository extends JpaRepository<Variation, Long> {
    Variation findFirstByVariationId(String variationId);
    Variation findFirstByPancakeProductIdAndSize(String productPancakeId, String size);
    List<Variation> findAllByPancakeProductId(String pancakeProductId);
    List<Variation> findAllByVariationIdIn(List<String> variationIds);
    List<Variation> findAllByPancakeProductIdAndColor(String productPancakeId, String color);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Variation v where v.id in :ids order by v.id")
    List<Variation> findAllByIdInForUpdate(@Param("ids") List<Long> ids);
}
