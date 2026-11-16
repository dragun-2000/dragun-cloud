package vn.co.cake.service.image;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.config.ImageCompressionProperties;
import vn.co.cake.service.image.ImageCompressor.Outcome;
import vn.co.cake.service.image.ImageCompressor.Result;

/**
 * Điều phối nén ảnh: nén nền sau khi upload và nén một file theo yêu cầu (dùng cho nén theo sản phẩm).
 *
 * <ul>
 *   <li>Upload không bao giờ bị chặn hay chậm vì nén: file gốc được lưu và trả URL như cũ, việc nén chạy nền.</li>
 *   <li>Mỗi file chỉ có một tiến trình nén tại một thời điểm; số ảnh giải mã đồng thời bị giới hạn để bảo vệ heap.
 *   </li>
 *   <li>Ảnh không kịp nén (hàng đợi đầy, server tắt) vẫn hợp lệ — có thể nén sau ở màn quản trị.</li>
 * </ul>
 */
@Slf4j
@Service
public class ImageCompressionService {

    private final ImageCompressionProperties properties;
    private final ImageCompressor compressor;
    private final Semaphore decodePermits;
    private final Set<Path> inProgress = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor uploadExecutor;

    public ImageCompressionService(ImageCompressionProperties properties) {
        this.properties = properties;
        this.compressor = new ImageCompressor(properties.getTargetBytes(), properties.getMinLongEdge(),
                properties.getMaxDecodePixels(), properties.getMinSavingRatio());
        this.decodePermits = new Semaphore(Math.max(1, properties.getMaxConcurrent()));
        this.uploadExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(500), daemon("image-compress-upload"));
    }

    public Path uploadDir() {
        return Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public long targetBytes() {
        return properties.getTargetBytes();
    }

    /** Kiểm tra nhanh (không giải mã): {@link Outcome#COMPRESSED} = cần nén. */
    public Outcome precheck(Path file) {
        return compressor.precheck(file);
    }

    /** File đã được hệ thống nén (có dấu nén). */
    public boolean isCompressed(Path file) {
        return compressor.isMarked(file);
    }

    /** Xếp hàng nén nền cho file vừa upload. Không ném exception, không chặn request. */
    public void compressAfterUpload(Path file) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            uploadExecutor.execute(() -> compress(file, null));
        } catch (RejectedExecutionException e) {
            log.warn("[ImageCompression] Hàng đợi nén đầy, bỏ qua {} (có thể nén sau ở màn quản trị)", file.getFileName());
        }
    }

    /**
     * Nén một file tại chỗ (đồng bộ). Không ném exception; lỗi → {@link Outcome#FAILED}, file gốc giữ nguyên.
     *
     * @param backupDir thư mục sao lưu bản gốc, {@code null} = không sao lưu
     */
    public Result compress(Path file, Path backupDir) {
        Path key = file.toAbsolutePath().normalize();
        if (!inProgress.add(key)) {
            return Result.unchanged(Outcome.SKIPPED_CHANGED, ImageCompressor.Format.OTHER, 0, "Đang được nén ở luồng khác");
        }
        try {
            decodePermits.acquire();
            try {
                long started = System.currentTimeMillis();
                Result result = compressor.compressInPlace(file, backupDir);
                logResult(file, result, System.currentTimeMillis() - started);
                return result;
            } finally {
                decodePermits.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.unchanged(Outcome.FAILED, ImageCompressor.Format.OTHER, 0, "Bị ngắt");
        } finally {
            inProgress.remove(key);
        }
    }

    private void logResult(Path file, Result r, long millis) {
        switch (r.getOutcome()) {
            case COMPRESSED:
                log.info("[ImageCompression] {} {}x{} {}KB -> {}x{} {}KB ({} ms){}", file.getFileName(),
                        r.getOriginalWidth(), r.getOriginalHeight(), r.getOriginalBytes() / 1024, r.getWidth(),
                        r.getHeight(), r.getFinalBytes() / 1024, millis,
                        r.isTargetReached() ? "" : " - " + r.getMessage());
                break;
            case FAILED:
            case SKIPPED_NO_GAIN:
            case SKIPPED_CHANGED:
                log.warn("[ImageCompression] Giữ nguyên {}: {} - {}", file.getFileName(), r.getOutcome(), r.getMessage());
                break;
            default:
                log.debug("[ImageCompression] Bỏ qua {}: {}", file.getFileName(), r.getOutcome());
        }
    }

    @PreDestroy
    public void shutdown() {
        uploadExecutor.shutdownNow();
    }

    static ThreadFactory daemon(String name) {
        return runnable -> {
            Thread t = new Thread(runnable, name);
            t.setDaemon(true);
            return t;
        };
    }
}
