package vn.co.cake.service.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import vn.co.cake.config.ImageCompressionProperties;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.service.image.ImageCompressor.Outcome;
import vn.co.cake.service.image.ProductImageCompressionService.ProductSummary;
import vn.co.cake.service.image.ProductImageCompressionService.ProductTask;
import vn.co.cake.service.image.ProductImageCompressionService.TaskState;

class ProductImageCompressionServiceTest {

    private static final long TARGET = 500L * 1024;

    @TempDir
    Path root;

    private Path uploadDir;
    private Path backupRoot;
    private ProductRepository productRepository;
    private VariationRepository variationRepository;
    private ImageCompressionService images;
    private ProductImageCompressionService service;

    private byte[] big1;
    private byte[] big2;
    private byte[] otherBig;
    private byte[] small;

    @BeforeEach
    void setUp() throws IOException {
        uploadDir = Files.createDirectories(root.resolve("external-images"));
        backupRoot = root.resolve("backup");
        ImageCompressionProperties props = new ImageCompressionProperties();
        props.setUploadDir(uploadDir.toString());
        props.setBackupDir(backupRoot.toString());
        props.setTargetBytes(TARGET);

        productRepository = mock(ProductRepository.class);
        variationRepository = mock(VariationRepository.class);
        images = new ImageCompressionService(props);
        service = new ProductImageCompressionService(productRepository, variationRepository, images, props);

        big1 = TestImages.largeJpeg(1);
        big2 = TestImages.largeJpeg(2);
        otherBig = TestImages.largeJpeg(3);
        small = TestImages.smallJpeg(4);
        write("1_main.jpg", big1);
        write("2_side.jpg", small);
        write("3_variation black.jpg", big2);
        write("9_other-product.jpg", otherBig);

        Product product = product(10L, "Áo khoác đen", "pk-10");
        product.setImage("/external-images/1_main.jpg");
        product.setImage1("https://debase.vn/external-images/2_side.jpg?v=3");
        product.setImage2("https://cdn.example.com/remote.jpg"); // ảnh ngoài: không đụng tới
        product.setImage3("/external-images/1_main.jpg");        // trùng ảnh: chỉ xử lý 1 lần
        product.setImage4("/external-images/not-exists.jpg");     // thiếu file
        Variation variation = new Variation();
        variation.setName("Đen / L");
        variation.setPancakeProductId("pk-10");
        variation.setImage("/external-images/3_variation%20black.jpg");

        Product other = product(20L, "Quần", "pk-20");
        other.setImage("/external-images/9_other-product.jpg");

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.findById(20L)).thenReturn(Optional.of(other));
        when(productRepository.findAllByDeletedIsFalseOrderByNameAsc()).thenReturn(Arrays.asList(product, other));
        when(variationRepository.findAllByPancakeProductId("pk-10")).thenReturn(Collections.singletonList(variation));
        when(variationRepository.findAllByPancakeProductId("pk-20")).thenReturn(Collections.emptyList());
        when(variationRepository.findAll()).thenReturn(Collections.singletonList(variation));
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
        images.shutdown();
    }

    @Test
    void resolveLocal_mapsOnlyFilesInsideUploadDir() {
        assertEquals(uploadDir.resolve("a.jpg"), ProductImageCompressionService.resolveLocal("/external-images/a.jpg", uploadDir).get());
        assertEquals(uploadDir.resolve("a b.jpg"), ProductImageCompressionService.resolveLocal("https://x.vn/external-images/a%20b.jpg?v=1", uploadDir).get());
        assertEquals(uploadDir.resolve("a b.jpg"), ProductImageCompressionService.resolveLocal("/external-images/a b.jpg", uploadDir).get());
        assertEquals(uploadDir.resolve("a+b.jpg"), ProductImageCompressionService.resolveLocal("/external-images/a+b.jpg", uploadDir).get());
        assertEquals(uploadDir.resolve("a+b c.jpg"), ProductImageCompressionService.resolveLocal("/external-images/a+b%20c.jpg", uploadDir).get());
        assertFalse(ProductImageCompressionService.resolveLocal("/external-images/../secret.txt", uploadDir).isPresent());
        assertFalse(ProductImageCompressionService.resolveLocal("/external-images/..%2Fsecret.txt", uploadDir).isPresent());
        assertFalse(ProductImageCompressionService.resolveLocal("/external-images/sub/a.jpg", uploadDir).isPresent());
        assertFalse(ProductImageCompressionService.resolveLocal("/external-images/", uploadDir).isPresent());
        assertFalse(ProductImageCompressionService.resolveLocal("https://cdn.example.com/a.jpg", uploadDir).isPresent());
        assertFalse(ProductImageCompressionService.resolveLocal(null, uploadDir).isPresent());
    }

    @Test
    void compressProduct_withBackup_compressesOnlyThatProductsLargeImages() throws IOException {
        ProductTask task = service.compressProduct(new ProductTask(10L, true));

        assertEquals(TaskState.DONE, task.getState(), task.getMessage());
        assertEquals(3, task.getFiles().size(), "main + side + variation (bỏ ảnh trùng, ảnh ngoài, ảnh thiếu file)");
        assertEquals(2, task.getCounts().get(Outcome.COMPRESSED));
        assertEquals(1, task.getCounts().get(Outcome.SKIPPED_SMALL));
        assertTrue(Files.size(file("1_main.jpg")) <= TARGET);
        assertTrue(Files.size(file("3_variation black.jpg")) <= TARGET);
        assertArrayEquals(small, Files.readAllBytes(file("2_side.jpg")), "ảnh nhỏ giữ nguyên");
        assertArrayEquals(otherBig, Files.readAllBytes(file("9_other-product.jpg")), "sản phẩm khác không bị đụng");
        Path productBackup = backupRoot.resolve("product-10");
        assertArrayEquals(big1, Files.readAllBytes(productBackup.resolve("1_main.jpg")));
        assertArrayEquals(big2, Files.readAllBytes(productBackup.resolve("3_variation black.jpg")));
        assertEquals(2, service.countBackups(10L));
        assertTrue(task.getSavedBytes() > 0);
    }

    @Test
    void compressProduct_twice_doesNotRecompress() throws IOException {
        service.compressProduct(new ProductTask(10L, false));
        byte[] once = Files.readAllBytes(file("1_main.jpg"));

        ProductTask second = service.compressProduct(new ProductTask(10L, false));

        assertEquals(TaskState.DONE, second.getState());
        assertFalse(second.getCounts().containsKey(Outcome.COMPRESSED));
        assertArrayEquals(once, Files.readAllBytes(file("1_main.jpg")));
    }

    @Test
    void compressProduct_withoutBackup_createsNoBackup() {
        ProductTask task = service.compressProduct(new ProductTask(10L, false));

        assertEquals(TaskState.DONE, task.getState());
        assertFalse(Files.exists(backupRoot.resolve("product-10")));
    }

    @Test
    void restore_putsOriginalsBack_andRemovesBackup() throws IOException {
        service.compressProduct(new ProductTask(10L, true));

        assertEquals(2, service.restore(10L));

        assertArrayEquals(big1, Files.readAllBytes(file("1_main.jpg")));
        assertArrayEquals(big2, Files.readAllBytes(file("3_variation black.jpg")));
        assertFalse(Files.exists(backupRoot.resolve("product-10")));
        assertEquals(0, service.countBackups(10L));
    }

    @Test
    void deleteBackup_keepsCompressedImages() throws IOException {
        service.compressProduct(new ProductTask(10L, true));
        byte[] compressed = Files.readAllBytes(file("1_main.jpg"));

        assertEquals(2, service.deleteBackup(10L));

        assertArrayEquals(compressed, Files.readAllBytes(file("1_main.jpg")));
        assertFalse(Files.exists(backupRoot.resolve("product-10")));
    }

    @Test
    void unknownProduct_fails_withoutTouchingFiles() throws IOException {
        when(productRepository.findById(anyLong())).thenReturn(Optional.empty());

        ProductTask task = service.compressProduct(new ProductTask(99L, true));

        assertEquals(TaskState.FAILED, task.getState());
        assertArrayEquals(big1, Files.readAllBytes(file("1_main.jpg")));
    }

    @Test
    void listProducts_showsPendingSortedByPendingSize_andFilters() {
        List<ProductSummary> rows = service.listProducts(null, true);

        assertEquals(2, rows.size());
        ProductSummary first = rows.get(0);
        assertEquals(10L, first.getProductId(), "sản phẩm có nhiều MB cần nén nhất lên đầu");
        assertEquals(3, first.getLocalCount());
        assertEquals(1, first.getMissingCount());
        assertEquals(1, first.getExternalCount());
        assertEquals(2, first.getPendingCount());

        assertEquals(1, service.listProducts("quần", true).size());
        assertEquals(1, service.listProducts("PK-20", true).size(), "lọc theo mã, không phân biệt hoa thường");

        service.enqueue(Collections.singletonList(20L), false);
        awaitDone(20L);
        List<ProductSummary> after = service.listProducts("quần", true);
        assertEquals(1, after.size(), "vẫn hiện vì có trạng thái vừa nén");
        assertEquals(0, after.get(0).getPendingCount());
        assertEquals(1, after.get(0).getCompressedCount());
    }

    @Test
    void enqueue_runsInBackground_andIgnoresDuplicates() throws Exception {
        assertEquals(1, service.enqueue(Arrays.asList(20L, 20L, null), false));

        awaitDone(20L);
        assertEquals(TaskState.DONE, service.task(20L).getState());
        assertTrue(Files.size(file("9_other-product.jpg")) <= TARGET);
    }

    @Test
    void restoreOrDelete_isRejectedWhileProductIsCompressing() {
        ProductTask running = new ProductTask(10L, true);
        running.start();
        try { // đưa task đang chạy vào bảng trạng thái
            java.lang.reflect.Field f = ProductImageCompressionService.class.getDeclaredField("tasks");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<Long, ProductTask> tasks = (java.util.Map<Long, ProductTask>) f.get(service);
            tasks.put(10L, running);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }

        try {
            service.restore(10L);
            throw new AssertionError("phải từ chối khôi phục khi đang nén");
        } catch (IllegalStateException | IOException expected) {
            // ok
        }
        assertEquals(0, service.enqueue(Collections.singletonList(10L), true), "không xếp hàng trùng sản phẩm đang chạy");
    }

    private void awaitDone(long productId) {
        long deadline = System.currentTimeMillis() + 60_000;
        while (service.task(productId).isActive() && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private Product product(long id, String name, String pancakeId) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setCode(pancakeId.toUpperCase());
        p.setProductPancakeId(pancakeId);
        return p;
    }

    private Path file(String name) {
        return uploadDir.resolve(name);
    }

    private void write(String name, byte[] data) throws IOException {
        Files.write(uploadDir.resolve(name), data);
    }
}
