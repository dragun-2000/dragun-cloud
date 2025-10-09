package vn.co.cake.service.external;

import org.springframework.stereotype.Service;
import vn.co.cake.controller.external.dto.ShopExtendDTO;
import vn.co.cake.entity.external.ShopExtend;
import vn.co.cake.repository.ShopExtendRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ShopExtendService {

    private final ShopExtendRepository shopExtendRepository;

    public ShopExtendService(ShopExtendRepository shopExtendRepository) {
        this.shopExtendRepository = shopExtendRepository;
    }

    public List<ShopExtendDTO> getAllShops() {
        return shopExtendRepository.findAll()
            .stream()
            .map(ShopExtendService::toDTO)
            .collect(Collectors.toList());
    }

    public Optional<ShopExtendDTO> getShopById(Long id) {
        return shopExtendRepository.findById(id)
            .map(ShopExtendService::toDTO);
    }

    public ShopExtendDTO createShop(ShopExtendDTO shopExtendDTO) {
        ShopExtend shopExtend = toEntity(shopExtendDTO);
        ShopExtend savedShop = shopExtendRepository.save(shopExtend);
        return toDTO(savedShop);
    }

    public ShopExtendDTO updateShop(Long id, ShopExtendDTO shopExtendDTO) {
        ShopExtend shopExtend = toEntity(shopExtendDTO);
        if (shopExtendRepository.existsById(id)) {
            shopExtend.setId(id);
            ShopExtend updatedShop = shopExtendRepository.save(shopExtend);
            return toDTO(updatedShop);
        }
        return null;
    }

    public boolean deleteShop(Long id) {
        if (shopExtendRepository.existsById(id)) {
            shopExtendRepository.deleteById(id);
            return true;
        }
        return false;
    }
    
    public static ShopExtend toEntity(ShopExtendDTO shopExtendDTO) {
        ShopExtend shopExtend = new ShopExtend();
        shopExtend.setId(shopExtendDTO.getId());
        shopExtend.setCode(shopExtendDTO.getCode());
        shopExtend.setName(shopExtendDTO.getName());
        shopExtend.setAddress(shopExtendDTO.getAddress());
        shopExtend.setPhone(shopExtendDTO.getPhone());
        shopExtend.setImageUrl(shopExtendDTO.getImageUrl());
        shopExtend.setCategory(shopExtendDTO.getCategory());
        return shopExtend;
    }

    public static ShopExtendDTO toDTO(ShopExtend shopExtend) {
        return new ShopExtendDTO(
                shopExtend.getId(),
                shopExtend.getCode(),
                shopExtend.getName(),
                shopExtend.getAddress(),
                shopExtend.getPhone(),
                shopExtend.getImageUrl(),
                shopExtend.getCategory()
        );
    }
}
