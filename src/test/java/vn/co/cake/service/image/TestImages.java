package vn.co.cake.service.image;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Random;

/** Ảnh tổng hợp cho test: nền xám chuyển sắc + chủ thể tối + hạt film (giống ảnh lookbook studio). */
final class TestImages {

    private TestImages() {
    }

    static BufferedImage grainyPhoto(int w, int h, int grain, long seed) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Random random = new Random(seed);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean subject = Math.abs(x - w / 2) < w / 5 && y > h / 10;
                int base = subject ? 25 : 90 + (int) (60.0 * y / h) - (int) (20.0 * Math.abs(x - w / 2) / w);
                int v = Math.max(0, Math.min(255, base + (int) (random.nextGaussian() * grain)));
                img.setRGB(x, y, (v << 16) | (v << 8) | v);
            }
        }
        return img;
    }

    /** JPEG ~2MB, 1600x2000 — lớn hơn ngưỡng 500KB. */
    static byte[] largeJpeg(long seed) throws IOException {
        return ImageCompressor.encode(grainyPhoto(1600, 2000, 18, seed), ImageCompressor.Format.JPEG, 0.97f);
    }

    /** JPEG nhỏ hơn ngưỡng. */
    static byte[] smallJpeg(long seed) throws IOException {
        return ImageCompressor.encode(grainyPhoto(400, 500, 10, seed), ImageCompressor.Format.JPEG, 0.85f);
    }
}
