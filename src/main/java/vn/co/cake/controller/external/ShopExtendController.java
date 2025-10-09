package vn.co.cake.controller.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.controller.external.dto.ShopExtendDTO;
import vn.co.cake.service.external.ShopExtendService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/extend/shops")
public class ShopExtendController {

    private final ShopExtendService shopExtendService;

    public ShopExtendController(ShopExtendService shopExtendService) {
        this.shopExtendService = shopExtendService;
    }

    @GetMapping
    public List<ShopExtendDTO> getAllShops() {
        return shopExtendService.getAllShops();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShopExtendDTO> getShopById(@PathVariable Long id) {
        Optional<ShopExtendDTO> shopExtend = shopExtendService.getShopById(id);
        return shopExtend.map(ResponseEntity::ok)
                         .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    public ResponseEntity<ShopExtendDTO> createShop(@RequestBody ShopExtendDTO shopExtendDTO) {
        ShopExtendDTO createdShop = shopExtendService.createShop(shopExtendDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdShop);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShopExtendDTO> updateShop(@PathVariable Long id, @RequestBody ShopExtendDTO shopExtendDTO) {
        ShopExtendDTO updatedShop = shopExtendService.updateShop(id, shopExtendDTO);
        return updatedShop != null 
                ? ResponseEntity.ok(updatedShop)
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShop(@PathVariable Long id) {
        boolean isDeleted = shopExtendService.deleteShop(id);
        return isDeleted 
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
