package vn.co.cake.controller.thiyen;

import vn.co.cake.entity.thiyen.ProductGallery;
import vn.co.cake.entity.thiyen.ProductVariant;
import vn.co.cake.entity.thiyen.YProduct;

import java.util.List;
import java.util.stream.Collectors;

public class ProductMapper {

    public static ProductDTO toDTO(YProduct product, List<ProductGallery> galleryList, List<ProductVariant> variantList) {
        if (product == null) return null;

        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setPrice(product.getPrice());
        dto.setOldPrice(product.getOldPrice());
        dto.setBulkPrice(product.getBulkPrice());
        dto.setQuantity(product.getQuantity());
        dto.setBulkQuantity(product.getBulkQuantity());
        dto.setDiscount(product.getDiscount());
        dto.setReviewCount(product.getReviewCount());
        dto.setShortDesc(product.getShortDesc());
        dto.setBenefits(product.getBenefits());
        dto.setTargetUsers(product.getTargetUsers());
        dto.setUsage(product.getUsage());
        dto.setManufacturer(product.getManufacturer());
        dto.setIngredients(product.getIngredients());
        dto.setDetailedUsage(product.getDetailedUsage());
        dto.setSpecifications(product.getSpecifications());
        dto.setTechnology(product.getTechnology());
        dto.setStorage(product.getStorage());
        dto.setImage(product.getImage());
        dto.setCategory(product.getCategory());
        dto.setIsNew(product.getIsNew());

        if (product.getGallery() != null) {
            dto.setGallery(
                galleryList.stream()
                .map(pg -> {
                    ProductGalleryDTO pgDto = new ProductGalleryDTO();
                    pgDto.setImageUrl(pg.getImageUrl());
                    pgDto.setPosition(pg.getPosition());
                    return pgDto;
                }).collect(Collectors.toList())
            );
        }

        if (product.getVariants() != null) {
            dto.setVariants(
                variantList.stream()
                .map(pv -> {
                    ProductVariantDTO pvDto = new ProductVariantDTO();
                    pvDto.setLabel(pv.getLabel());
                    pvDto.setValue(pv.getValue());
                    return pvDto;
                }).collect(Collectors.toList())
            );
        }

        return dto;
    }

    public static YProduct toEntity(ProductDTO dto) {
        if (dto == null) return null;

        YProduct product = new YProduct();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        product.setOldPrice(dto.getOldPrice());
        product.setBulkPrice(dto.getBulkPrice());
        product.setQuantity(dto.getQuantity());
        product.setBulkQuantity(dto.getBulkQuantity());
        product.setDiscount(dto.getDiscount());
        product.setReviewCount(dto.getReviewCount());
        product.setShortDesc(dto.getShortDesc());
        product.setBenefits(dto.getBenefits());
        product.setTargetUsers(dto.getTargetUsers());
        product.setUsage(dto.getUsage());
        product.setManufacturer(dto.getManufacturer());
        product.setIngredients(dto.getIngredients());
        product.setDetailedUsage(dto.getDetailedUsage());
        product.setSpecifications(dto.getSpecifications());
        product.setTechnology(dto.getTechnology());
        product.setStorage(dto.getStorage());
        product.setImage(dto.getImage());
        product.setCategory(dto.getCategory());
        product.setIsNew(dto.getIsNew());

        return product;
    }
}