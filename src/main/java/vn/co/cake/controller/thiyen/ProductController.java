package vn.co.cake.controller.thiyen;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.co.cake.service.thiyen.ProductService;

import java.util.List;

@RestController
@RequestMapping("/api/thiyen/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService srv) {
        this.productService = srv;
    }

    @GetMapping("/list")
    public ResponseEntity<List<ProductDTO>> getProducts() {
        return ResponseEntity.ok(productService.getAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody ProductDTO productDTO) {
        productService.save(productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        productService.update(id, productDTO);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}