package vn.co.cake.service.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.zip.CRC32;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import lombok.Getter;
import lombok.ToString;

/**
 * Nén một file ảnh JPEG/PNG tại chỗ: giữ nguyên tên file và định dạng, chỉ giảm dung lượng.
 *
 * <p>Bất biến (an toàn dữ liệu):
 * <ul>
 *   <li>File &le; {@code targetBytes} hoặc đã mang dấu nén ({@link #MARKER}) không bao giờ bị đụng tới — nén lại
 *       ảnh đã nén chỉ làm giảm chất lượng.</li>
 *   <li>Định dạng ra = định dạng vào (nhận diện theo magic bytes, không theo đuôi file), nên URL/đuôi file luôn
 *       khớp nội dung. Định dạng khác (GIF, WebP, HEIC, ...) giữ nguyên.</li>
 *   <li>Kết quả chỉ được ghi khi đã giải mã lại thành công, đúng kích thước dự kiến và nhỏ hơn file gốc.</li>
 *   <li>Ghi qua file tạm rồi thay thế nguyên tử; file gốc thay đổi trong lúc nén thì bỏ qua. Mọi lỗi → giữ file
 *       gốc.</li>
 *   <li>Hướng xoay EXIF được áp vào pixel, nên ảnh hiển thị đúng chiều như ảnh gốc (kể cả ở client bỏ qua EXIF).
 *   </li>
 * </ul>
 * Lớp này không phụ thuộc Spring để test độc lập; điều phối (luồng, khoá, job) nằm ở
 * {@link ImageCompressionService}.
 */
public class ImageCompressor {

    /** Dấu nhận biết file đã được hệ thống nén (JPEG: segment COM, PNG: chunk tEXt). */
    public static final String MARKER = "dragun-image-compressed:v1";

    private static final byte[] MARKER_BYTES = MARKER.getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_TEXT_KEYWORD = "Comment".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};

    /** Chất lượng JPEG thử ở mỗi kích thước; khi đã chạm cạnh tối thiểu mới hạ thêm. */
    private static final float[] JPEG_QUALITIES = {0.85f, 0.80f};
    private static final float[] JPEG_QUALITIES_AT_FLOOR = {0.85f, 0.80f, 0.75f, 0.70f};
    private static final float[] PNG_QUALITIES = {-1f};
    private static final int MAX_SIZE_ATTEMPTS = 8;
    private static final int MAX_HEADER_SEGMENT = 64 * 1024 * 1024;

    public enum Format { JPEG, PNG, OTHER }

    public enum Outcome {
        /** Đã ghi đè bằng bản nén. */
        COMPRESSED,
        /** Dung lượng &le; ngưỡng — không cần nén. */
        SKIPPED_SMALL,
        /** Đã có dấu nén của hệ thống — không nén lại. */
        SKIPPED_ALREADY_COMPRESSED,
        /** Không phải JPEG/PNG. */
        SKIPPED_UNSUPPORTED,
        /** Nén không giảm đủ dung lượng — giữ file gốc. */
        SKIPPED_NO_GAIN,
        /** File gốc bị thay đổi trong lúc nén — giữ file mới đó. */
        SKIPPED_CHANGED,
        /** Lỗi đọc/giải mã/ghi — giữ file gốc. */
        FAILED
    }

    @Getter
    @ToString(exclude = "data")
    public static final class Result {
        private final Outcome outcome;
        private final Format format;
        private final long originalBytes;
        private final long finalBytes;
        private final int originalWidth;
        private final int originalHeight;
        private final int width;
        private final int height;
        private final boolean targetReached;
        private final String message;
        private final transient byte[] data;

        Result(Outcome outcome, Format format, long originalBytes, long finalBytes, int originalWidth,
               int originalHeight, int width, int height, boolean targetReached, String message, byte[] data) {
            this.outcome = outcome;
            this.format = format;
            this.originalBytes = originalBytes;
            this.finalBytes = finalBytes;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
            this.width = width;
            this.height = height;
            this.targetReached = targetReached;
            this.message = message;
            this.data = data;
        }

        static Result unchanged(Outcome outcome, Format format, long bytes, String message) {
            return new Result(outcome, format, bytes, bytes, 0, 0, 0, 0, false, message, null);
        }

        Result withOutcome(Outcome newOutcome, String newMessage) {
            if (newOutcome == Outcome.COMPRESSED) {
                return this;
            }
            return new Result(newOutcome, format, originalBytes, originalBytes, originalWidth, originalHeight,
                    originalWidth, originalHeight, false, newMessage, null);
        }
    }

    private final long targetBytes;
    private final int minLongEdge;
    private final long maxDecodePixels;
    private final double minSavingRatio;

    public ImageCompressor(long targetBytes, int minLongEdge, long maxDecodePixels, double minSavingRatio) {
        if (targetBytes <= 0 || minLongEdge <= 0 || maxDecodePixels <= 0 || minSavingRatio < 0 || minSavingRatio >= 1) {
            throw new IllegalArgumentException("Invalid image compression settings");
        }
        this.targetBytes = targetBytes;
        this.minLongEdge = minLongEdge;
        this.maxDecodePixels = maxDecodePixels;
        this.minSavingRatio = minSavingRatio;
    }

    /**
     * Nén file tại chỗ. Không bao giờ ném exception: mọi lỗi trả về {@link Outcome#FAILED} và file gốc giữ nguyên.
     *
     * @param backupDir thư mục sao lưu bản gốc trước khi ghi đè; {@code null} = không sao lưu
     */
    public Result compressInPlace(Path file, Path backupDir) {
        long originalSize;
        FileTime originalMtime;
        try {
            originalSize = Files.size(file);
            originalMtime = Files.getLastModifiedTime(file);
        } catch (IOException e) {
            return Result.unchanged(Outcome.FAILED, Format.OTHER, 0, "Không đọc được file: " + e.getMessage());
        }

        Result result = analyze(file, originalSize);
        if (result.getOutcome() != Outcome.COMPRESSED) {
            return result;
        }

        String fileName = file.getFileName().toString();
        Path tmp = file.resolveSibling("." + fileName + "." + UUID.randomUUID() + ".tmp");
        try {
            Files.write(tmp, result.getData(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            if (Files.size(tmp) != result.getData().length) {
                return result.withOutcome(Outcome.FAILED, "Ghi file tạm không đủ dữ liệu");
            }
            if (Files.size(file) != originalSize || !Files.getLastModifiedTime(file).equals(originalMtime)) {
                return result.withOutcome(Outcome.SKIPPED_CHANGED, "File gốc thay đổi trong lúc nén");
            }
            if (backupDir != null) {
                Files.createDirectories(backupDir);
                Path backup = backupDir.resolve(fileName);
                // Bản sao lưu đã có = bản gốc của lần chạy trước bị dừng giữa chừng; không ghi đè nó.
                if (!Files.exists(backup)) {
                    Files.copy(file, backup, StandardCopyOption.COPY_ATTRIBUTES);
                }
            }
            replace(tmp, file);
            return result;
        } catch (IOException e) {
            return result.withOutcome(Outcome.FAILED, "Ghi file nén lỗi, giữ file gốc: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return result.withOutcome(Outcome.FAILED, "Bị ngắt khi thay file");
        } finally {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // file tạm ẩn, lần chạy sau bỏ qua file bắt đầu bằng "."
            }
        }
    }

    /**
     * Kiểm tra nhanh (không giải mã) file có cần nén không. Trả về {@link Outcome#COMPRESSED} nghĩa là "sẽ nén",
     * các giá trị SKIPPED_* / FAILED như {@link #compressInPlace}.
     */
    public Outcome precheck(Path file) {
        try {
            if (Files.size(file) <= targetBytes) {
                return Outcome.SKIPPED_SMALL;
            }
            Format format = detectFormat(file);
            if (format == Format.OTHER) {
                return Outcome.SKIPPED_UNSUPPORTED;
            }
            return hasMarker(readHeader(file, format), format) ? Outcome.SKIPPED_ALREADY_COMPRESSED : Outcome.COMPRESSED;
        } catch (IOException e) {
            return Outcome.FAILED;
        }
    }

    /** File có dấu nén của hệ thống (bất kể dung lượng). */
    public boolean isMarked(Path file) {
        try {
            Format format = detectFormat(file);
            return format != Format.OTHER && hasMarker(readHeader(file, format), format);
        } catch (IOException e) {
            return false;
        }
    }

    /** Phân tích và nén trong bộ nhớ, không ghi file. */
    Result analyze(Path file, long originalSize) {
        Format format = Format.OTHER;
        try {
            format = detectFormat(file);
            if (originalSize <= targetBytes) {
                return Result.unchanged(Outcome.SKIPPED_SMALL, format, originalSize, "Dung lượng đã nhỏ hơn ngưỡng");
            }
            if (format == Format.OTHER) {
                return Result.unchanged(Outcome.SKIPPED_UNSUPPORTED, format, originalSize, "Không phải JPEG/PNG");
            }
            List<Segment> header = readHeader(file, format);
            if (hasMarker(header, format)) {
                return Result.unchanged(Outcome.SKIPPED_ALREADY_COMPRESSED, format, originalSize, "Đã nén trước đó");
            }
            return compress(file, format, header, originalSize);
        } catch (Exception | OutOfMemoryError e) {
            return Result.unchanged(Outcome.FAILED, format, originalSize,
                    "Không nén được, giữ file gốc: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private Result compress(Path file, Format format, List<Segment> header, long originalSize) throws IOException {
        Decoded decoded = decode(file, format);
        int orientation = format == Format.JPEG ? exifOrientation(header) : 1;
        BufferedImage base = applyOrientation(normalize(decoded.image, format == Format.PNG), orientation);
        boolean swapped = orientation >= 5;
        int originalWidth = swapped ? decoded.height : decoded.width;
        int originalHeight = swapped ? decoded.width : decoded.height;

        int baseLong = Math.max(base.getWidth(), base.getHeight());
        int floorLong = Math.min(minLongEdge, Math.max(originalWidth, originalHeight));
        if (baseLong < floorLong) {
            throw new IOException("Ảnh quá lớn để giải mã an toàn (" + originalWidth + "x" + originalHeight + ")");
        }

        long budget = targetBytes - markerOverhead(format);
        byte[] best = null;
        int bestWidth = 0;
        int bestHeight = 0;
        int currentLong = baseLong;
        for (int attempt = 0; attempt < MAX_SIZE_ATTEMPTS; attempt++) {
            BufferedImage scaled = currentLong == baseLong ? base : resize(base, currentLong);
            boolean atFloor = currentLong <= floorLong;
            float[] qualities = format == Format.PNG ? PNG_QUALITIES
                    : atFloor ? JPEG_QUALITIES_AT_FLOOR : JPEG_QUALITIES;
            long smallestHere = Long.MAX_VALUE;
            boolean reached = false;
            for (float quality : qualities) {
                byte[] encoded = encode(scaled, format, quality);
                smallestHere = Math.min(smallestHere, encoded.length);
                if (best == null || encoded.length < best.length) {
                    best = encoded;
                    bestWidth = scaled.getWidth();
                    bestHeight = scaled.getHeight();
                }
                if (encoded.length <= budget) {
                    reached = true;
                    break;
                }
            }
            if (reached || atFloor) {
                break;
            }
            // Dung lượng ~ tỉ lệ số pixel: ước lượng cạnh dài kế tiếp, luôn giảm ít nhất 10%.
            double factor = Math.min(Math.sqrt((double) budget / smallestHere) * 0.95, 0.9);
            currentLong = Math.max((int) Math.floor(currentLong * factor), floorLong);
        }

        byte[] output = withMarker(best, format);
        boolean targetReached = output.length <= targetBytes;
        long maxAllowed = (long) Math.floor(originalSize * (1 - minSavingRatio));
        if (!targetReached && output.length > maxAllowed) {
            return new Result(Outcome.SKIPPED_NO_GAIN, format, originalSize, originalSize, originalWidth,
                    originalHeight, originalWidth, originalHeight, false,
                    "Nén chỉ còn " + output.length + " bytes, không đủ tiết kiệm", null);
        }
        verify(output, format, bestWidth, bestHeight);
        return new Result(Outcome.COMPRESSED, format, originalSize, output.length, originalWidth, originalHeight,
                bestWidth, bestHeight, targetReached,
                targetReached ? "OK" : "Chưa xuống dưới ngưỡng ở cạnh tối thiểu " + floorLong + "px", output);
    }

    // ---------------------------------------------------------------- format / header

    static Format detectFormat(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return detectFormat(in);
        }
    }

    static Format detectFormat(InputStream in) throws IOException {
        byte[] head = new byte[8];
        int read = in.readNBytes(head, 0, head.length);
        if (read >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
            return Format.JPEG;
        }
        if (read == 8 && java.util.Arrays.equals(head, PNG_SIGNATURE)) {
            return Format.PNG;
        }
        return Format.OTHER;
    }

    /** Segment (JPEG) hoặc chunk (PNG) nằm trước dữ liệu ảnh. */
    static final class Segment {
        final int type;
        final byte[] data;

        Segment(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }

    static List<Segment> readHeader(Path file, Format format) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return readHeader(in, format);
        }
    }

    static List<Segment> readHeader(InputStream raw, Format format) throws IOException {
        DataInputStream in = new DataInputStream(new BufferedInputStream(raw));
        return format == Format.JPEG ? readJpegHeader(in) : readPngHeader(in);
    }

    private static List<Segment> readJpegHeader(DataInputStream in) throws IOException {
        List<Segment> segments = new ArrayList<>();
        in.readUnsignedShort(); // SOI
        try {
            while (true) {
                if (in.readUnsignedByte() != 0xFF) {
                    return segments;
                }
                int marker = in.readUnsignedByte();
                while (marker == 0xFF) {
                    marker = in.readUnsignedByte();
                }
                if (marker == 0xDA || marker == 0xD9) { // SOS / EOI: hết phần header
                    return segments;
                }
                if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                    continue;
                }
                int length = in.readUnsignedShort();
                if (length < 2) {
                    return segments;
                }
                byte[] data = new byte[length - 2];
                in.readFully(data);
                segments.add(new Segment(marker, data));
            }
        } catch (EOFException e) {
            return segments;
        }
    }

    private static List<Segment> readPngHeader(DataInputStream in) throws IOException {
        List<Segment> chunks = new ArrayList<>();
        in.readFully(new byte[8]);
        try {
            while (true) {
                int length = in.readInt();
                int type = in.readInt();
                if (type == pngType("IDAT") || type == pngType("IEND") || length < 0 || length > MAX_HEADER_SEGMENT) {
                    return chunks;
                }
                byte[] data = new byte[length];
                in.readFully(data);
                in.readInt(); // CRC
                chunks.add(new Segment(type, data));
            }
        } catch (EOFException e) {
            return chunks;
        }
    }

    static boolean hasMarker(List<Segment> header, Format format) {
        int wanted = format == Format.JPEG ? 0xFE : pngType("tEXt");
        for (Segment s : header) {
            if (s.type == wanted && indexOf(s.data, MARKER_BYTES) >= 0) {
                return true;
            }
        }
        return false;
    }

    /** Giá trị EXIF Orientation (1..8) trong APP1 của JPEG; mặc định 1. */
    static int exifOrientation(List<Segment> header) {
        for (Segment s : header) {
            if (s.type != 0xE1 || !startsWith(s.data, EXIF_HEADER)) {
                continue;
            }
            byte[] d = s.data;
            int tiff = EXIF_HEADER.length;
            if (d.length < tiff + 8) {
                return 1;
            }
            boolean little;
            if (d[tiff] == 'I' && d[tiff + 1] == 'I') {
                little = true;
            } else if (d[tiff] == 'M' && d[tiff + 1] == 'M') {
                little = false;
            } else {
                return 1;
            }
            if (u16(d, tiff + 2, little) != 42) {
                return 1;
            }
            long ifd = u32(d, tiff + 4, little);
            if (ifd < 8 || tiff + ifd + 2 > d.length) {
                return 1;
            }
            int p = (int) (tiff + ifd);
            int count = u16(d, p, little);
            for (int i = 0; i < count; i++) {
                int entry = p + 2 + i * 12;
                if (entry + 12 > d.length) {
                    break;
                }
                if (u16(d, entry, little) == 0x0112 && u16(d, entry + 2, little) == 3) {
                    int value = u16(d, entry + 8, little);
                    return value >= 1 && value <= 8 ? value : 1;
                }
            }
            return 1;
        }
        return 1;
    }

    // ---------------------------------------------------------------- decode / transform

    private static final class Decoded {
        final BufferedImage image;
        final int width;
        final int height;

        Decoded(BufferedImage image, int width, int height) {
            this.image = image;
            this.width = width;
            this.height = height;
        }
    }

    private Decoded decode(Path file, Format format) throws IOException {
        try (ImageInputStream iis = ImageIO.createImageInputStream(file.toFile())) {
            if (iis == null) {
                throw new IOException("Không mở được stream ảnh");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName(format == Format.JPEG ? "jpeg" : "png");
            if (!readers.hasNext()) {
                throw new IOException("Không có ImageReader cho " + format);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) {
                    throw new IOException("Kích thước ảnh không hợp lệ");
                }
                ImageReadParam param = reader.getDefaultReadParam();
                int step = 1;
                while (ceilDiv(width, step) * ceilDiv(height, step) > maxDecodePixels) {
                    step++;
                }
                if (step > 1) {
                    param.setSourceSubsampling(step, step, 0, 0);
                }
                BufferedImage image = reader.read(0, param);
                if (image == null) {
                    throw new IOException("Giải mã ảnh trả về rỗng");
                }
                return new Decoded(image, width, height);
            } finally {
                reader.dispose();
            }
        }
    }

    /** Đưa về sRGB 8-bit (INT_RGB, hoặc INT_ARGB khi PNG có kênh alpha); JPEG nền trắng. */
    static BufferedImage normalize(BufferedImage src, boolean keepAlpha) {
        boolean alpha = keepAlpha && src.getColorModel().hasAlpha();
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(),
                alpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        try {
            if (!alpha) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, dst.getWidth(), dst.getHeight());
            }
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return dst;
    }

    /** Xoay/lật pixel theo EXIF Orientation để ảnh ra không cần tag EXIF mà vẫn đúng chiều. */
    static BufferedImage applyOrientation(BufferedImage src, int orientation) {
        if (orientation < 2 || orientation > 8) {
            return src;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        boolean swap = orientation >= 5;
        int dw = swap ? h : w;
        BufferedImage dst = new BufferedImage(dw, swap ? w : h, src.getType());
        int[] s = ((DataBufferInt) src.getRaster().getDataBuffer()).getData();
        int[] d = ((DataBufferInt) dst.getRaster().getDataBuffer()).getData();
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                int nx;
                int ny;
                switch (orientation) {
                    case 2: nx = w - 1 - x; ny = y; break;
                    case 3: nx = w - 1 - x; ny = h - 1 - y; break;
                    case 4: nx = x; ny = h - 1 - y; break;
                    case 5: nx = y; ny = x; break;
                    case 6: nx = h - 1 - y; ny = x; break;
                    case 7: nx = h - 1 - y; ny = w - 1 - x; break;
                    default: nx = y; ny = w - 1 - x; break; // 8
                }
                d[ny * dw + nx] = s[row + x];
            }
        }
        return dst;
    }

    /** Thu nhỏ giữ tỉ lệ, chia đôi nhiều bước (bilinear) để ảnh mịn, không răng cưa. */
    static BufferedImage resize(BufferedImage src, int targetLongEdge) {
        int w = src.getWidth();
        int h = src.getHeight();
        double scale = (double) targetLongEdge / Math.max(w, h);
        if (scale >= 1) {
            return src;
        }
        int tw = Math.max(1, (int) Math.round(w * scale));
        int th = Math.max(1, (int) Math.round(h * scale));
        BufferedImage current = src;
        int cw = w;
        int ch = h;
        do {
            cw = Math.max(cw / 2, tw);
            ch = Math.max(ch / 2, th);
            BufferedImage next = new BufferedImage(cw, ch, src.getType());
            Graphics2D g = next.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
                g.drawImage(current, 0, 0, cw, ch, null);
            } finally {
                g.dispose();
            }
            current = next;
        } while (cw != tw || ch != th);
        return current;
    }

    // ---------------------------------------------------------------- encode / verify

    static byte[] encode(BufferedImage image, Format format, float quality) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (format == Format.PNG) {
            if (!ImageIO.write(image, "png", bos)) {
                throw new IOException("Không có ImageWriter PNG");
            }
            return bos.toByteArray();
        }
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("Không có ImageWriter JPEG");
        }
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(bos)) {
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return bos.toByteArray();
    }

    private static int markerOverhead(Format format) {
        return format == Format.JPEG ? 4 + MARKER_BYTES.length : 12 + PNG_TEXT_KEYWORD.length + 1 + MARKER_BYTES.length;
    }

    /** Chèn dấu nén: JPEG — segment COM sau APP0/JFIF; PNG — chunk tEXt ngay sau IHDR. */
    static byte[] withMarker(byte[] encoded, Format format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(encoded.length + 64);
        if (format == Format.JPEG) {
            int pos = 2;
            if (encoded.length > 6 && (encoded[2] & 0xFF) == 0xFF && (encoded[3] & 0xFF) == 0xE0) {
                pos = 4 + (((encoded[4] & 0xFF) << 8) | (encoded[5] & 0xFF));
            }
            out.write(encoded, 0, pos);
            int length = MARKER_BYTES.length + 2;
            out.write(0xFF);
            out.write(0xFE);
            out.write(length >> 8);
            out.write(length & 0xFF);
            out.write(MARKER_BYTES);
            out.write(encoded, pos, encoded.length - pos);
            return out.toByteArray();
        }
        int ihdrEnd = 8 + 4 + 4 + 13 + 4;
        out.write(encoded, 0, ihdrEnd);
        ByteArrayOutputStream chunk = new ByteArrayOutputStream();
        chunk.write(new byte[] {'t', 'E', 'X', 't'});
        chunk.write(PNG_TEXT_KEYWORD);
        chunk.write(0);
        chunk.write(MARKER_BYTES);
        byte[] typeAndData = chunk.toByteArray();
        writeInt(out, typeAndData.length - 4);
        out.write(typeAndData);
        CRC32 crc = new CRC32();
        crc.update(typeAndData);
        writeInt(out, (int) crc.getValue());
        out.write(encoded, ihdrEnd, encoded.length - ihdrEnd);
        return out.toByteArray();
    }

    private static void verify(byte[] data, Format format, int expectedWidth, int expectedHeight) throws IOException {
        BufferedImage check = ImageIO.read(new ByteArrayInputStream(data));
        if (check == null || check.getWidth() != expectedWidth || check.getHeight() != expectedHeight) {
            throw new IOException("Ảnh sau nén không giải mã lại được đúng kích thước");
        }
        if (detectFormat(new ByteArrayInputStream(data)) != format
                || !hasMarker(readHeader(new ByteArrayInputStream(data), format), format)) {
            throw new IOException("Ảnh sau nén sai định dạng hoặc thiếu dấu nén");
        }
    }

    /** Thay {@code target} bằng {@code source} (move nguyên tử nếu được), thử lại khi Windows đang khoá file. */
    static void replace(Path tmp, Path target) throws IOException, InterruptedException {
        // Windows: file đang được đọc (đang phục vụ HTTP) có thể khoá tạm thời → thử lại vài lần.
        for (int attempt = 0; ; attempt++) {
            try {
                try {
                    Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
                return;
            } catch (IOException e) {
                if (attempt >= 4) {
                    throw e;
                }
                Thread.sleep(200L * (attempt + 1));
            }
        }
    }

    // ---------------------------------------------------------------- byte helpers

    static int pngType(String type) {
        byte[] b = type.getBytes(StandardCharsets.US_ASCII);
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }

    private static void writeInt(ByteArrayOutputStream out, int v) {
        out.write(v >>> 24);
        out.write((v >>> 16) & 0xFF);
        out.write((v >>> 8) & 0xFF);
        out.write(v & 0xFF);
    }

    private static long ceilDiv(int value, int step) {
        return (value + step - 1L) / step;
    }

    private static int u16(byte[] d, int p, boolean little) {
        return little ? (d[p] & 0xFF) | ((d[p + 1] & 0xFF) << 8) : ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF);
    }

    private static long u32(byte[] d, int p, boolean little) {
        long a = d[p] & 0xFF;
        long b = d[p + 1] & 0xFF;
        long c = d[p + 2] & 0xFF;
        long e = d[p + 3] & 0xFF;
        return little ? a | (b << 8) | (c << 16) | (e << 24) : (a << 24) | (b << 16) | (c << 8) | e;
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static int indexOf(byte[] data, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= data.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (data[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
