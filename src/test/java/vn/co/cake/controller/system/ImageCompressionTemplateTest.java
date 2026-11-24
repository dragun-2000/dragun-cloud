package vn.co.cake.controller.system;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import vn.co.cake.config.ImageCompressionProperties;
import vn.co.cake.service.image.ProductImageCompressionService.ProductSummary;

/** Render thử template để bắt lỗi biểu thức Thymeleaf (không cần khởi động app/DB). */
class ImageCompressionTemplateTest {

    @Test
    void pageRenders_withRowsAndEmptyState() {
        ProductSummary pending = new ProductSummary(10L, "Áo khoác đen", "AK10", "/external-images/a.jpg", 3, 1, 1,
                5_000_000L, 2, 4_000_000L, 0, 2, null);
        String html = render(Arrays.asList(pending), true);
        assertTrue(html.contains("Áo khoác đen"));
        assertTrue(html.contains("/AM/system/image-compression/products/10/restore"));
        assertTrue(html.contains("2 ảnh gốc đang sao lưu"));
        assertTrue(html.contains("Check Sản Phẩm"), "trang nằm trong layout admin, có mục menu");
        assertTrue(html.contains("Cấu hình thanh toán"), "sidebar admin được dựng");

        String empty = render(Collections.emptyList(), false);
        assertTrue(empty.contains("Không có sản phẩm nào."));
    }

    private String render(List<ProductSummary> rows, boolean autoRefresh) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.addDialect(new nz.net.ultraq.thymeleaf.LayoutDialect());

        Map<String, Double> disk = new LinkedHashMap<>();
        disk.put("upload", 1234.5);
        disk.put("backup", -1.0);
        Map<String, Object> vars = new HashMap<>();
        vars.put("rows", rows);
        vars.put("q", "");
        vars.put("onlyPending", true);
        vars.put("page", 0);
        vars.put("totalPages", 2);
        vars.put("totalProducts", rows.size());
        vars.put("totalPendingMb", 3.8);
        vars.put("disk", disk);
        vars.put("props", new ImageCompressionProperties());
        vars.put("autoRefresh", autoRefresh);
        vars.put("message", "Đã xếp hàng nén 1 sản phẩm");
        vars.put("sideMenu", "/AM/system/image-compression");

        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(new MockHttpServletRequest(servletContext),
                new MockHttpServletResponse(), servletContext, Locale.forLanguageTag("vi"), vars);
        return engine.process("system/image-compression", context);
    }
}
