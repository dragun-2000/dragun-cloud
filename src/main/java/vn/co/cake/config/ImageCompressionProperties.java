package vn.co.cake.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Cấu hình nén ảnh upload ({@code image.compression.*}).
 *
 * <p>Nguyên tắc: nén chỉ làm giảm dung lượng file đã lưu. Tên file, đường dẫn và định dạng (JPEG/PNG)
 * không đổi; mọi lỗi trong quá trình nén đều giữ nguyên file gốc.
 */
@Data
@Component
@ConfigurationProperties(prefix = "image.compression")
public class ImageCompressionProperties {

    /** Tắt toàn bộ việc nén (upload vẫn lưu file gốc như cũ). */
    private boolean enabled = true;

    /** Thư mục chứa ảnh upload (phải trùng với resource handler /external-images/**). */
    private String uploadDir = "c:/data/external-images/";

    /** Ảnh có dung lượng <= ngưỡng này không bị nén. Ảnh lớn hơn được nén để xuống dưới ngưỡng. */
    private long targetBytes = 500L * 1024;

    /**
     * Cạnh dài tối thiểu (px) khi phải thu nhỏ để đạt {@link #targetBytes}. Không bao giờ thu nhỏ dưới mức này
     * (ảnh gốc nhỏ hơn thì giữ nguyên kích thước gốc) — tránh ảnh sản phẩm bị vỡ khi zoom.
     */
    private int minLongEdge = 1200;

    /**
     * Giới hạn số pixel giải mã một lần (bảo vệ heap). Ảnh lớn hơn được đọc có lấy mẫu (subsampling) — chỉ áp
     * dụng khi đằng nào cũng phải thu nhỏ để đạt dung lượng mục tiêu.
     */
    private long maxDecodePixels = 40_000_000L;

    /** Khi không đạt được {@link #targetBytes}: chỉ ghi đè nếu tiết kiệm ít nhất tỉ lệ này. */
    private double minSavingRatio = 0.10;

    /** Số ảnh nén song song tối đa (mỗi ảnh lớn có thể chiếm vài trăm MB heap khi giải mã). */
    private int maxConcurrent = 1;

    /**
     * Thư mục sao lưu ảnh gốc khi chạy nén lại ảnh cũ. Để trống = không sao lưu.
     * Sau khi kiểm tra ảnh hiển thị ổn, có thể xoá thư mục này để giải phóng ổ đĩa.
     */
    private String backupDir = "c:/data/external-images-backup/";
}
