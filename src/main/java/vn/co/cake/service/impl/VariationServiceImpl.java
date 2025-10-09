package vn.co.cake.service.impl;

import org.springframework.stereotype.Service;
import vn.co.cake.entity.Variation;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.service.VariationService;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class VariationServiceImpl implements VariationService {

    private final VariationRepository variationRepository;

    public VariationServiceImpl(VariationRepository variationRepository) {
        this.variationRepository = variationRepository;
    }

    @Override
    public void update(Long variationId, String imageUrl) {
        Variation variation = variationRepository.findById(variationId)
                .orElseThrow(() -> new NoSuchElementException("Variation không tồn tại"));

        List<Variation> variations = variationRepository
                .findAllByPancakeProductIdAndColor(variation.getPancakeProductId(), variation.getColor());

        variations.forEach(v -> {
            v.setImage(imageUrl);
        });
        variationRepository.saveAll(variations);
    }
}
