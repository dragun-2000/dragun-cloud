package vn.co.cake.controller.external;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.co.cake.controller.external.dto.ProductExtendDTO;
import vn.co.cake.service.aws.S3Service;
import vn.co.cake.service.external.ProductExtendService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/extend/products")
public class ProductExtendController {

    private final ProductExtendService productExtendService;
    private final S3Service s3Service;

    public ProductExtendController(ProductExtendService productExtendService, S3Service s3Service) {
        this.productExtendService = productExtendService;
        this.s3Service = s3Service;
    }

    @GetMapping
    public List<ProductExtendDTO> getAllProducts() {
        return productExtendService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductExtendDTO> getProductById(@PathVariable Long id) {
        Optional<ProductExtendDTO> productExtend = productExtendService.getProductById(id);
        return productExtend.map(ResponseEntity::ok)
                            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping("/create")
    public ProductExtendDTO createProduct(@RequestParam String name,
                                          @RequestParam double price,
                                          @RequestParam String description,
                                          @RequestParam Long shopId,
                                          @RequestParam String category,
                                          @RequestParam("image") MultipartFile image) throws IOException {
        String imageUrl = s3Service.uploadImageToS3(image);

        ProductExtendDTO newProduct = new ProductExtendDTO();
        newProduct.setName(name);
        newProduct.setPrice(price);
        newProduct.setShopId(shopId);
        newProduct.setDescription(description);
        newProduct.setCategory(category);
        newProduct.setImageUrl(imageUrl);

        return productExtendService.createProduct(newProduct);
    }

    @PutMapping("/update/{id}")
    public ProductExtendDTO updateProduct(@PathVariable Long id,
                                          @RequestParam String name,
                                          @RequestParam double price,
                                          @RequestParam Long shopId,
                                          @RequestParam String description,
                                          @RequestParam String category,
                                          @RequestParam(required = false, value = "image") MultipartFile image) throws IOException {
        String imageUrl = null;
        if (image != null) {
            imageUrl = s3Service.uploadImageToS3(image);
        }

        ProductExtendDTO updatedProduct = new ProductExtendDTO();
        updatedProduct.setId(id);
        updatedProduct.setName(name);
        updatedProduct.setPrice(price);
        updatedProduct.setShopId(shopId);
        updatedProduct.setDescription(description);
        updatedProduct.setCategory(category);
        if (imageUrl != null) {
            updatedProduct.setImageUrl(imageUrl); 
        }

        return productExtendService.updateProduct(id, updatedProduct);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        boolean isDeleted = productExtendService.deleteProduct(id);
        return isDeleted 
                ? ResponseEntity.noContent().build()
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
