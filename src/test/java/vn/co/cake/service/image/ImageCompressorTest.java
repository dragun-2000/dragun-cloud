package vn.co.cake.service.image;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import vn.co.cake.service.image.ImageCompressor.Format;
import vn.co.cake.service.image.ImageCompressor.Outcome;
import vn.co.cake.service.image.ImageCompressor.Result;

class ImageCompressorTest {

    private static final long TARGET = 500L * 1024;

    @TempDir
    Path dir;

    private final ImageCompressor compressor = new ImageCompressor(TARGET, 1200, 40_000_000L, 0.10);

    // ------------------------------------------------------------------ ảnh như ảnh ví dụ (1600x2000, film grain)

    @Test
    void grainyProductPhoto_overTarget_isCompressedBelowTarget_sameNameSameFormat() throws IOException {
        Path file = write("1700000000000_ao-khoac.jpg", jpeg(grainyPhoto(1600, 2000, 18, 1), 0.97f));
        long before = Files.size(file);
        assertTrue(before > TARGET, "ảnh test phải lớn hơn ngưỡng, thực tế " + before);

        Result r = compressor.compressInPlace(file, null);

        assertEquals(Outcome.COMPRESSED, r.getOutcome(), r.getMessage());
        assertTrue(r.isTargetReached());
        assertTrue(Files.exists(dir.resolve("1700000000000_ao-khoac.jpg")), "tên file không đổi");
        assertEquals(1, countFiles(), "không sinh file thừa");
        long after = Files.size(file);
        assertTrue(after <= TARGET, "sau nén phải <= 500KB, thực tế " + after);
        assertEquals(after, r.getFinalBytes());
        assertEquals(Format.JPEG, ImageCompressor.detectFormat(file));
        BufferedImage out = ImageIO.read(file.toFile());
        assertNotNull(out, "ảnh sau nén giải mã được");
        assertTrue(Math.max(out.getWidth(), out.getHeight()) >= 1200, "không thu nhỏ dưới cạnh tối thiểu");
        assertEquals(1600.0 / 2000.0, (double) out.getWidth() / out.getHeight(), 0.01, "giữ tỉ lệ khung hình");
        System.out.printf("grainy 1600x2000: %d KB -> %d KB, %dx%d%n", before / 1024, after / 1024,
                out.getWidth(), out.getHeight());
    }

    @Test
    void compressedFile_isNeverCompressedAgain() throws IOException {
        Path file = write("a.jpg", jpeg(grainyPhoto(1600, 2000, 18, 2), 0.97f));
        assertEquals(Outcome.COMPRESSED, compressor.compressInPlace(file, null).getOutcome());
        byte[] once = Files.readAllBytes(file);

        // Lần 2 với ngưỡng thấp hơn: vẫn không nén lại nhờ dấu nén.
        ImageCompressor stricter = new ImageCompressor(50 * 1024, 200, 40_000_000L, 0.10);
        Result again = stricter.compressInPlace(file, null);

        assertEquals(Outcome.SKIPPED_ALREADY_COMPRESSED, again.getOutcome());
        assertArrayEquals(once, Files.readAllBytes(file));
        assertEquals(Outcome.SKIPPED_ALREADY_COMPRESSED, stricter.precheck(file));
    }

    // ------------------------------------------------------------------ không đụng tới file

    @Test
    void fileAtOrBelowTarget_isUntouched() throws IOException {
        byte[] original = jpeg(grainyPhoto(400, 500, 10, 3), 0.85f);
        assertTrue(original.length <= TARGET);
        Path file = write("small.jpg", original);

        assertEquals(Outcome.SKIPPED_SMALL, compressor.compressInPlace(file, null).getOutcome());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void corruptJpeg_failsAndKeepsOriginalBytes() throws IOException {
        byte[] garbage = new byte[700 * 1024];
        new Random(4).nextBytes(garbage);
        garbage[0] = (byte) 0xFF;
        garbage[1] = (byte) 0xD8;
        garbage[2] = (byte) 0xFF;
        Path file = write("broken.jpg", garbage);

        Result r = compressor.compressInPlace(file, null);

        assertEquals(Outcome.FAILED, r.getOutcome());
        assertArrayEquals(garbage, Files.readAllBytes(file));
        assertEquals(1, countFiles(), "không để lại file tạm");
    }

    @Test
    void truncatedJpeg_keepsOriginalBytes() throws IOException {
        byte[] full = jpeg(grainyPhoto(1600, 2000, 18, 5), 0.97f);
        byte[] truncated = java.util.Arrays.copyOf(full, full.length / 2);
        Path file = write("truncated.jpg", truncated);

        Result r = compressor.compressInPlace(file, null);

        // Ảnh cắt cụt: hoặc lỗi (giữ gốc), hoặc giải mã được phần đầu — trường hợp nào file cũng phải còn hợp lệ.
        if (r.getOutcome() != Outcome.COMPRESSED) {
            assertArrayEquals(truncated, Files.readAllBytes(file));
        } else {
            assertNotNull(ImageIO.read(file.toFile()));
        }
    }

    @Test
    void unsupportedFormat_isUntouched() throws IOException {
        byte[] gif = new byte[600 * 1024];
        new Random(6).nextBytes(gif);
        System.arraycopy("GIF89a".getBytes(), 0, gif, 0, 6);
        Path file = write("anim.gif", gif);

        assertEquals(Outcome.SKIPPED_UNSUPPORTED, compressor.compressInPlace(file, null).getOutcome());
        assertArrayEquals(gif, Files.readAllBytes(file));
    }

    @Test
    void imageSmallerThanMinEdge_isNeverUpscaled() throws IOException {
        // 900x1100 nhiễu mạnh ở chất lượng 1.0: > ngưỡng nhưng cạnh dài < 1200.
        Path file = write("tiny-but-heavy.jpg", jpeg(grainyPhoto(900, 1100, 60, 7), 1.0f));
        assertTrue(Files.size(file) > TARGET);

        Result r = compressor.compressInPlace(file, null);

        if (r.getOutcome() == Outcome.COMPRESSED) {
            BufferedImage out = ImageIO.read(file.toFile());
            assertEquals(900, out.getWidth());
            assertEquals(1100, out.getHeight());
        }
    }

    // ------------------------------------------------------------------ PNG

    @Test
    void png_staysPng_andKeepsTransparency() throws IOException {
        BufferedImage img = new BufferedImage(1600, 1600, BufferedImage.TYPE_INT_ARGB);
        Random random = new Random(8);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int alpha = x < 100 ? 0 : 255;
                img.setRGB(x, y, (alpha << 24) | (random.nextInt(0xFFFFFF)));
            }
        }
        Path file = write("logo.png", png(img));
        assertTrue(Files.size(file) > TARGET);
        ImageCompressor small = new ImageCompressor(TARGET, 400, 40_000_000L, 0.10);

        Result r = small.compressInPlace(file, null);

        assertEquals(Outcome.COMPRESSED, r.getOutcome(), r.getMessage());
        assertEquals(Format.PNG, ImageCompressor.detectFormat(file));
        BufferedImage out = ImageIO.read(file.toFile());
        assertTrue(out.getColorModel().hasAlpha());
        assertEquals(0, out.getRGB(1, out.getHeight() / 2) >>> 24, "vùng trong suốt vẫn trong suốt");
        assertEquals(Outcome.SKIPPED_ALREADY_COMPRESSED, new ImageCompressor(1024, 10, 40_000_000L, 0.10).precheck(file));
    }

    // ------------------------------------------------------------------ EXIF orientation

    @Test
    void exifOrientation6_isAppliedToPixels() throws IOException {
        int w = 1800;
        int h = 1200;
        BufferedImage img = grainyPhoto(w, h, 40, 9);
        fill(img, 0, 0, 200, 200, 0xFF0000); // góc trên-trái đỏ
        byte[] jpeg = withExifOrientation(jpeg(img, 1.0f), 6);
        Path file = write("rotated.jpg", jpeg);
        ImageCompressor c = new ImageCompressor(200 * 1024, 300, 40_000_000L, 0.10);

        Result r = c.compressInPlace(file, null);

        assertEquals(Outcome.COMPRESSED, r.getOutcome(), r.getMessage());
        BufferedImage out = ImageIO.read(file.toFile());
        assertTrue(out.getHeight() > out.getWidth(), "ảnh ngang + orientation 6 hiển thị thành ảnh dọc");
        // Xoay 90° theo chiều kim đồng hồ: góc trên-trái cũ thành góc trên-phải.
        int px = out.getRGB(out.getWidth() - 3, 3);
        assertTrue(((px >> 16) & 0xFF) > 180 && ((px >> 8) & 0xFF) < 90, "góc đỏ nằm ở trên-phải");
    }

    @Test
    void orientationTransforms_mapCornersCorrectly() {
        BufferedImage src = new BufferedImage(3, 2, BufferedImage.TYPE_INT_RGB);
        src.setRGB(0, 0, 1); // trên-trái
        src.setRGB(2, 0, 2); // trên-phải
        assertEquals(1, ImageCompressor.applyOrientation(src, 6).getRGB(1, 0) & 0xFFFFFF);
        assertEquals(2, ImageCompressor.applyOrientation(src, 6).getRGB(1, 2) & 0xFFFFFF);
        assertEquals(1, ImageCompressor.applyOrientation(src, 8).getRGB(0, 2) & 0xFFFFFF);
        assertEquals(1, ImageCompressor.applyOrientation(src, 3).getRGB(2, 1) & 0xFFFFFF);
        assertEquals(1, ImageCompressor.applyOrientation(src, 2).getRGB(2, 0) & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ backup

    @Test
    void backupDir_receivesExactOriginal() throws IOException {
        byte[] original = jpeg(grainyPhoto(1600, 2000, 18, 10), 0.97f);
        Path file = write("p.jpg", original);
        Path backup = dir.resolve("backup");

        assertEquals(Outcome.COMPRESSED, compressor.compressInPlace(file, backup).getOutcome());

        assertArrayEquals(original, Files.readAllBytes(backup.resolve("p.jpg")));
        assertFalse(Files.readAllBytes(file).length >= original.length);
    }

    // ------------------------------------------------------------------ helpers

    private Path write(String name, byte[] data) throws IOException {
        Path file = dir.resolve(name);
        Files.write(file, data);
        return file;
    }

    private long countFiles() throws IOException {
        try (java.util.stream.Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile).count();
        }
    }

    /** Nền xám chuyển sắc + chủ thể tối + hạt film, gần với ảnh lookbook studio. */
    private static BufferedImage grainyPhoto(int w, int h, int grain, long seed) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(seed);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean subject = Math.abs(x - w / 2) < w / 5 && y > h / 10;
                int base = subject ? 25 : 90 + (int) (60.0 * y / h) - (int) (20.0 * Math.abs(x - w / 2) / w);
                int v = clamp(base + (int) (random.nextGaussian() * grain));
                img.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        return img;
    }

    private static void fill(BufferedImage img, int x0, int y0, int w, int h, int rgb) {
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    private static byte[] jpeg(BufferedImage img, float quality) throws IOException {
        return ImageCompressor.encode(img, Format.JPEG, quality);
    }

    private static byte[] png(BufferedImage img) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", bos);
        return bos.toByteArray();
    }

    /** Chèn APP1/EXIF tối thiểu (big-endian, IFD0 chỉ có tag Orientation) ngay sau SOI. */
    private static byte[] withExifOrientation(byte[] jpeg, int orientation) {
        byte[] tiff = {
                'M', 'M', 0, 42, 0, 0, 0, 8,          // header, IFD0 tại offset 8
                0, 1,                                 // 1 entry
                0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0, // Orientation SHORT
                0, 0, 0, 0                            // next IFD = 0
        };
        byte[] payload = new byte[6 + tiff.length];
        System.arraycopy(new byte[] {'E', 'x', 'i', 'f', 0, 0}, 0, payload, 0, 6);
        System.arraycopy(tiff, 0, payload, 6, tiff.length);
        int len = payload.length + 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(jpeg, 0, 2);
        out.write(0xFF);
        out.write(0xE1);
        out.write(len >> 8);
        out.write(len & 0xFF);
        out.write(payload, 0, payload.length);
        out.write(jpeg, 2, jpeg.length - 2);
        return out.toByteArray();
    }
}
