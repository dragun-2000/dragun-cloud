package vn.co.cake.service.image;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import vn.co.cake.config.ImageCompressionProperties;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.service.image.ImageCompressor.Outcome;
import vn.co.cake.service.image.ImageCompressor.Result;

/**
 * Nén ảnh theo từng sản phẩm (ảnh sản phẩm image..image9 + ảnh các biến thể), chọn từ màn quản trị.
 *
 * <p>Không sửa dữ liệu DB: ảnh được nén tại chỗ, giữ nguyên tên file nên mọi URL đang lưu vẫn đúng. Sao lưu (nếu
 * chọn) chỉ chứa ảnh gốc của đúng sản phẩm đó, trong {@code backup-dir/product-<id>/}; kiểm tra xong thì "Xoá sao
 * lưu" để giải phóng ổ đĩa, hoặc "Khôi phục" để trả lại ảnh gốc.
 */
@Slf4j
@Service
public class ProductImageCompressionService {

    static final String URL_SEGMENT = "/external-images/";
    /** Chừa lại khi kiểm tra dung lượng trống trước khi sao lưu. */
    static final long FREE_SPACE_MARGIN = 200L * 1024 * 1024;
    private static final int MAX_TASK_HISTORY = 2000;

    public enum TaskState { QUEUED, RUNNING, DONE, FAILED }

    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final ImageCompressionService images;
    private final ImageCompressionProperties properties;
    private final ThreadPoolExecutor executor;
    private final Map<Long, ProductTask> tasks = Collections.synchronizedMap(new LinkedHashMap<Long, ProductTask>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ProductTask> eldest) {
            return size() > MAX_TASK_HISTORY && !eldest.getValue().isActive();
        }
    });

    public ProductImageCompressionService(ProductRepository productRepository,
                                          VariationRepository variationRepository,
                                          ImageCompressionService images,
                                          ImageCompressionProperties properties) {
        this.productRepository = productRepository;
        this.variationRepository = variationRepository;
        this.images = images;
        this.properties = properties;
        this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(10_000),
                ImageCompressionService.daemon("image-compress-product"));
    }

    // ------------------------------------------------------------------ danh sách

    /**
     * Danh sách sản phẩm kèm thống kê ảnh, sắp theo dung lượng cần nén giảm dần.
     *
     * @param keyword     lọc theo tên/mã (không phân biệt hoa thường), rỗng = tất cả
     * @param onlyPending chỉ sản phẩm còn ảnh cần nén hoặc còn bản sao lưu
     */
    public List<ProductSummary> listProducts(String keyword, boolean onlyPending) {
        String kw = StringUtils.trimToEmpty(keyword).toLowerCase(Locale.ROOT);
        Map<String, List<Variation>> variationsByProduct = variationRepository.findAll().stream()
                .filter(v -> StringUtils.isNotBlank(v.getPancakeProductId()))
                .collect(Collectors.groupingBy(Variation::getPancakeProductId));

        List<ProductSummary> rows = new ArrayList<>();
        for (Product product : productRepository.findAllByDeletedIsFalseOrderByNameAsc()) {
            if (!kw.isEmpty() && !StringUtils.containsIgnoreCase(product.getName(), kw)
                    && !StringUtils.containsIgnoreCase(product.getCode(), kw)) {
                continue;
            }
            List<Variation> variations = variationsByProduct.getOrDefault(product.getProductPancakeId(),
                    Collections.emptyList());
            ProductSummary row = summarize(product, imagesOf(product, variations));
            if (!onlyPending || row.getPendingCount() > 0 || row.getBackupCount() > 0 || row.getTask() != null) {
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparingLong(ProductSummary::getPendingBytes).reversed()
                .thenComparing(ProductSummary::getProductId));
        return rows;
    }

    private ProductSummary summarize(Product product, List<ImageRef> refs) {
        long total = 0;
        long pendingBytes = 0;
        int local = 0;
        int pending = 0;
        int compressed = 0;
        int missing = 0;
        int external = 0;
        String firstUrl = null;
        for (ImageRef ref : refs) {
            if (firstUrl == null) {
                firstUrl = ref.getUrl();
            }
            if (ref.getPath() == null) {
                external++;
                continue;
            }
            if (!Files.isRegularFile(ref.getPath())) {
                missing++;
                continue;
            }
            local++;
            long size = sizeOf(ref.getPath());
            total += size;
            Outcome outcome = images.precheck(ref.getPath());
            if (outcome == Outcome.COMPRESSED) {
                pending++;
                pendingBytes += size;
            } else if (outcome == Outcome.SKIPPED_ALREADY_COMPRESSED
                    || (outcome == Outcome.SKIPPED_SMALL && images.isCompressed(ref.getPath()))) {
                compressed++;
            }
        }
        return new ProductSummary(product.getId(), product.getName(), product.getCode(), firstUrl, local, missing,
                external, total, pending, pendingBytes, compressed, countBackups(product.getId()),
                tasks.get(product.getId()));
    }

    // ------------------------------------------------------------------ nén

    /**
     * Xếp hàng nén cho các sản phẩm đã chọn; chạy tuần tự từng sản phẩm ở luồng nền.
     *
     * @return số sản phẩm được xếp hàng (sản phẩm đang chờ/đang chạy bị bỏ qua)
     */
    public int enqueue(List<Long> productIds, boolean backup) {
        if (productIds == null) {
            return 0;
        }
        int queued = 0;
        for (Long id : productIds.stream().distinct().collect(Collectors.toList())) {
            if (id == null) {
                continue;
            }
            ProductTask task;
            synchronized (tasks) {
                ProductTask existing = tasks.get(id);
                if (existing != null && existing.isActive()) {
                    continue;
                }
                task = new ProductTask(id, backup);
                tasks.remove(id); // đưa xuống cuối thứ tự LRU
                tasks.put(id, task);
            }
            try {
                executor.execute(() -> run(task));
                queued++;
            } catch (RejectedExecutionException e) {
                task.fail("Hàng đợi đầy, vui lòng thử lại sau");
            }
        }
        return queued;
    }

    private void run(ProductTask task) {
        try {
            compressProduct(task);
        } catch (RuntimeException e) {
            log.error("[ImageCompression] Lỗi khi nén ảnh sản phẩm {}", task.getProductId(), e);
            task.fail("Lỗi: " + e.getMessage());
        }
    }

    /** Nén đồng bộ ảnh của một sản phẩm (gọi từ luồng nền; tách riêng để test). */
    ProductTask compressProduct(ProductTask task) {
        task.start();
        if (!images.isEnabled()) {
            return task.fail("Nén ảnh đang tắt (image.compression.enabled=false)");
        }
        Product product = productRepository.findById(task.getProductId()).orElse(null);
        if (product == null) {
            return task.fail("Không tìm thấy sản phẩm");
        }
        List<Variation> variations = StringUtils.isBlank(product.getProductPancakeId()) ? Collections.emptyList()
                : variationRepository.findAllByPancakeProductId(product.getProductPancakeId());
        List<Path> files = imagesOf(product, variations).stream()
                .map(ImageRef::getPath)
                .filter(p -> p != null && Files.isRegularFile(p))
                .collect(Collectors.toList());

        Path backupDir = task.isBackup() ? productBackupDir(task.getProductId()).orElse(null) : null;
        if (task.isBackup() && backupDir == null) {
            return task.fail("Chưa cấu hình image.compression.backup-dir — bỏ chọn sao lưu để nén");
        }
        if (backupDir != null) {
            long need = files.stream().filter(p -> images.precheck(p) == Outcome.COMPRESSED).mapToLong(this::sizeOf).sum();
            long free = usableSpace(backupDir);
            if (free >= 0 && free < need + FREE_SPACE_MARGIN) {
                return task.fail(String.format("Không đủ dung lượng để sao lưu (cần %.1f MB, còn trống %.1f MB). "
                        + "Bỏ chọn sao lưu để nén không sao lưu.", mb(need + FREE_SPACE_MARGIN), mb(free)));
            }
        }

        for (Path file : files) {
            long before = sizeOf(file);
            Outcome pre = images.precheck(file);
            Result result = pre == Outcome.COMPRESSED ? images.compress(file, backupDir)
                    : Result.unchanged(pre, ImageCompressor.Format.OTHER, before, null);
            task.add(new FileResult(file.getFileName().toString(), result.getOutcome(), before,
                    result.getOutcome() == Outcome.COMPRESSED ? result.getFinalBytes() : before,
                    result.getWidth(), result.getHeight(), result.getMessage()));
        }
        return task.done();
    }

    // ------------------------------------------------------------------ sao lưu

    /** Trả ảnh gốc đã sao lưu về chỗ cũ (ghi đè bản nén) và xoá bản sao lưu. */
    public int restore(long productId) throws IOException {
        ensureNotActive(productId);
        Optional<Path> dir = productBackupDir(productId);
        if (!dir.isPresent() || !Files.isDirectory(dir.get())) {
            return 0;
        }
        Path uploadDir = images.uploadDir();
        int restored = 0;
        for (Path backup : listFiles(dir.get())) {
            Path target = uploadDir.resolve(backup.getFileName().toString()).normalize();
            if (!target.startsWith(uploadDir)) {
                continue;
            }
            try {
                ImageCompressor.replace(backup, target);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Bị ngắt khi khôi phục", e);
            }
            restored++;
        }
        deleteIfEmpty(dir.get());
        log.info("[ImageCompression] Khôi phục {} ảnh gốc của sản phẩm {}", restored, productId);
        return restored;
    }

    /** Xoá bản sao lưu ảnh gốc của sản phẩm (sau khi đã kiểm tra ảnh nén hiển thị ổn). */
    public int deleteBackup(long productId) throws IOException {
        ensureNotActive(productId);
        Optional<Path> dir = productBackupDir(productId);
        if (!dir.isPresent() || !Files.isDirectory(dir.get())) {
            return 0;
        }
        int deleted = 0;
        for (Path file : listFiles(dir.get())) {
            Files.deleteIfExists(file);
            deleted++;
        }
        deleteIfEmpty(dir.get());
        log.info("[ImageCompression] Xoá {} ảnh sao lưu của sản phẩm {}", deleted, productId);
        return deleted;
    }

    private void ensureNotActive(long productId) {
        ProductTask task = tasks.get(productId);
        if (task != null && task.isActive()) {
            throw new IllegalStateException("Sản phẩm đang được nén, vui lòng đợi xong");
        }
    }

    int countBackups(long productId) {
        Optional<Path> dir = productBackupDir(productId);
        if (!dir.isPresent() || !Files.isDirectory(dir.get())) {
            return 0;
        }
        try {
            return listFiles(dir.get()).size();
        } catch (IOException e) {
            return 0;
        }
    }

    Optional<Path> productBackupDir(long productId) {
        if (StringUtils.isBlank(properties.getBackupDir())) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(properties.getBackupDir()).toAbsolutePath().normalize().resolve("product-" + productId));
    }

    // ------------------------------------------------------------------ trạng thái / ổ đĩa

    public ProductTask task(long productId) {
        return tasks.get(productId);
    }

    public boolean hasActiveTasks() {
        synchronized (tasks) {
            return tasks.values().stream().anyMatch(ProductTask::isActive);
        }
    }

    /** Dung lượng trống (MB) của ổ chứa ảnh và ổ sao lưu; -1 nếu không xác định được. */
    public Map<String, Double> diskFreeMb() {
        Map<String, Double> map = new LinkedHashMap<>();
        map.put("upload", mb(usableSpace(images.uploadDir())));
        map.put("backup", productBackupDir(0).map(p -> mb(usableSpace(p))).orElse(-1.0));
        return map;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    // ------------------------------------------------------------------ ảnh của sản phẩm

    List<ImageRef> imagesOf(Product product, List<Variation> variations) {
        Map<String, ImageRef> refs = new LinkedHashMap<>();
        String[] productImages = {product.getImage(), product.getImage1(), product.getImage2(), product.getImage3(),
                product.getImage4(), product.getImage5(), product.getImage6(), product.getImage7(),
                product.getImage8(), product.getImage9()};
        for (int i = 0; i < productImages.length; i++) {
            addRef(refs, productImages[i], i == 0 ? "image" : "image" + i);
        }
        for (Variation v : variations) {
            addRef(refs, v.getImage(), "variation " + StringUtils.defaultString(v.getName(), String.valueOf(v.getId())));
        }
        return new ArrayList<>(refs.values());
    }

    private void addRef(Map<String, ImageRef> refs, String url, String source) {
        if (StringUtils.isBlank(url)) {
            return;
        }
        Path path = resolveLocal(url, images.uploadDir()).orElse(null);
        String key = path != null ? path.toString().toLowerCase(Locale.ROOT) : url;
        refs.putIfAbsent(key, new ImageRef(url, path, source));
    }

    /**
     * Đổi URL ảnh lưu trong DB ({@code /external-images/<tên>} hoặc URL đầy đủ chứa đoạn đó) thành file trong thư mục
     * upload. URL ngoài (CDN, Pancake, ...) hoặc trỏ ra ngoài thư mục upload → rỗng (không bao giờ bị đụng tới).
     */
    static Optional<Path> resolveLocal(String url, Path uploadDir) {
        if (StringUtils.isBlank(url)) {
            return Optional.empty();
        }
        int idx = url.indexOf(URL_SEGMENT);
        if (idx < 0) {
            return Optional.empty();
        }
        String name = url.substring(idx + URL_SEGMENT.length());
        int cut = StringUtils.indexOfAny(name, '?', '#');
        if (cut >= 0) {
            name = name.substring(0, cut);
        }
        if (name.contains("%")) {
            try {
                name = URLDecoder.decode(name.replace("+", "%2B"), StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
        if (name.isEmpty() || name.contains("/") || name.contains("\\") || name.contains(":")) {
            return Optional.empty();
        }
        Path base = uploadDir.toAbsolutePath().normalize();
        Path path = base.resolve(name).normalize();
        return path.startsWith(base) && !path.equals(base) ? Optional.of(path) : Optional.empty();
    }

    // ------------------------------------------------------------------ helpers

    private long sizeOf(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0;
        }
    }

    private static long usableSpace(Path path) {
        Path existing = path;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return -1;
        }
        try {
            return Files.getFileStore(existing).getUsableSpace();
        } catch (IOException e) {
            return -1;
        }
    }

    private static List<Path> listFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path p : stream) {
                if (Files.isRegularFile(p)) {
                    files.add(p);
                }
            }
        }
        return files;
    }

    private static void deleteIfEmpty(Path dir) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            if (!stream.iterator().hasNext()) {
                Files.deleteIfExists(dir);
            }
        } catch (IOException ignored) {
            // thư mục rỗng còn sót lại không ảnh hưởng gì
        }
    }

    static double mb(long bytes) {
        return bytes < 0 ? -1 : Math.round(bytes / 1024.0 / 1024.0 * 10) / 10.0;
    }

    // ------------------------------------------------------------------ DTO

    @Getter
    @AllArgsConstructor
    public static final class ImageRef {
        private final String url;
        /** {@code null} nếu là ảnh ngoài thư mục upload. */
        private final Path path;
        private final String source;
    }

    @Getter
    @AllArgsConstructor
    public static final class ProductSummary {
        private final long productId;
        private final String name;
        private final String code;
        private final String firstImageUrl;
        private final int localCount;
        private final int missingCount;
        private final int externalCount;
        private final long totalBytes;
        private final int pendingCount;
        private final long pendingBytes;
        private final int compressedCount;
        private final int backupCount;
        private final ProductTask task;

        public double getTotalMb() {
            return mb(totalBytes);
        }

        public double getPendingMb() {
            return mb(pendingBytes);
        }
    }

    @Getter
    @AllArgsConstructor
    public static final class FileResult {
        private final String fileName;
        private final Outcome outcome;
        private final long bytesBefore;
        private final long bytesAfter;
        private final int width;
        private final int height;
        private final String message;

        public long getKbBefore() {
            return bytesBefore / 1024;
        }

        public long getKbAfter() {
            return bytesAfter / 1024;
        }
    }

    /** Trạng thái nén một sản phẩm (giữ trong bộ nhớ, mất khi khởi động lại — ảnh đã nén thì vẫn còn dấu nén). */
    public static final class ProductTask {
        private final long productId;
        private final boolean backup;
        private final LocalDateTime queuedAt = LocalDateTime.now();
        private LocalDateTime finishedAt;
        private TaskState state = TaskState.QUEUED;
        private String message;
        private final List<FileResult> files = new ArrayList<>();

        ProductTask(long productId, boolean backup) {
            this.productId = productId;
            this.backup = backup;
        }

        public long getProductId() {
            return productId;
        }

        public boolean isBackup() {
            return backup;
        }

        public synchronized TaskState getState() {
            return state;
        }

        public synchronized boolean isActive() {
            return state == TaskState.QUEUED || state == TaskState.RUNNING;
        }

        public synchronized String getMessage() {
            return message;
        }

        public synchronized LocalDateTime getQueuedAt() {
            return queuedAt;
        }

        public synchronized LocalDateTime getFinishedAt() {
            return finishedAt;
        }

        public synchronized List<FileResult> getFiles() {
            return new ArrayList<>(files);
        }

        public synchronized long getSavedBytes() {
            return files.stream().mapToLong(f -> f.getBytesBefore() - f.getBytesAfter()).sum();
        }

        public synchronized double getSavedMb() {
            return mb(getSavedBytes());
        }

        public synchronized Map<Outcome, Long> getCounts() {
            Map<Outcome, Long> counts = new LinkedHashMap<>();
            Arrays.stream(Outcome.values()).forEach(o -> {
                long n = files.stream().filter(f -> f.getOutcome() == o).count();
                if (n > 0) {
                    counts.put(o, n);
                }
            });
            return counts;
        }

        synchronized void start() {
            state = TaskState.RUNNING;
        }

        synchronized void add(FileResult result) {
            files.add(result);
        }

        synchronized ProductTask done() {
            state = TaskState.DONE;
            finishedAt = LocalDateTime.now();
            long failed = files.stream().filter(f -> f.getOutcome() == Outcome.FAILED).count();
            message = failed > 0 ? failed + " ảnh lỗi, đã giữ nguyên ảnh gốc" : "Hoàn tất";
            return this;
        }

        synchronized ProductTask fail(String reason) {
            state = TaskState.FAILED;
            finishedAt = LocalDateTime.now();
            message = reason;
            return this;
        }
    }
}
