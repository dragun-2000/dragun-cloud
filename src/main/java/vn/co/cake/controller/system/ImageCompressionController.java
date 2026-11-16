package vn.co.cake.controller.system;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.config.ImageCompressionProperties;
import vn.co.cake.service.image.ProductImageCompressionService;
import vn.co.cake.service.image.ProductImageCompressionService.ProductSummary;
import vn.co.cake.service.image.ProductImageCompressionService.ProductTask;

/**
 * Màn quản trị nén ảnh theo từng sản phẩm. Chỉ ADMIN (AdminSecurityConfiguration: /AM/system/**).
 */
@Slf4j
@Controller
public class ImageCompressionController {

    static final String BASE = "/AM/system/image-compression";
    private static final int PAGE_SIZE = 50;

    private final ProductImageCompressionService productImages;
    private final ImageCompressionProperties properties;

    public ImageCompressionController(ProductImageCompressionService productImages,
                                      ImageCompressionProperties properties) {
        this.productImages = productImages;
        this.properties = properties;
    }

    @GetMapping(BASE)
    public String page(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "true") boolean onlyPending,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        List<ProductSummary> all = productImages.listProducts(q, onlyPending);
        int totalPages = Math.max(1, (all.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(page, 0), totalPages - 1);
        List<ProductSummary> rows = all.isEmpty() ? Collections.emptyList()
                : all.subList(current * PAGE_SIZE, Math.min(all.size(), (current + 1) * PAGE_SIZE));

        model.addAttribute("rows", rows);
        model.addAttribute("q", q);
        model.addAttribute("onlyPending", onlyPending);
        model.addAttribute("page", current);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", all.size());
        model.addAttribute("totalPendingMb", round(all.stream().mapToDouble(ProductSummary::getPendingMb).sum()));
        model.addAttribute("disk", productImages.diskFreeMb());
        model.addAttribute("props", properties);
        model.addAttribute("autoRefresh", productImages.hasActiveTasks());
        model.addAttribute("sideMenu", BASE);
        return "system/image-compression";
    }

    @PostMapping(BASE + "/compress")
    public String compress(@RequestParam(name = "productIds", required = false) List<Long> productIds,
                           @RequestParam(defaultValue = "false") boolean backup,
                           @RequestParam(required = false) String q,
                           @RequestParam(defaultValue = "true") boolean onlyPending,
                           @RequestParam(defaultValue = "0") int page,
                           RedirectAttributes redirect) {
        if (productIds == null || productIds.isEmpty()) {
            redirect.addFlashAttribute("error", "Chưa chọn sản phẩm nào");
        } else {
            int queued = productImages.enqueue(productIds, backup);
            redirect.addFlashAttribute("message", "Đã xếp hàng nén " + queued + " sản phẩm"
                    + (backup ? " (có sao lưu ảnh gốc)" : " (không sao lưu)"));
        }
        return back(redirect, q, onlyPending, page);
    }

    @PostMapping(BASE + "/products/{productId}/restore")
    public String restore(@PathVariable long productId,
                          @RequestParam(required = false) String q,
                          @RequestParam(defaultValue = "true") boolean onlyPending,
                          @RequestParam(defaultValue = "0") int page,
                          RedirectAttributes redirect) {
        try {
            int n = productImages.restore(productId);
            redirect.addFlashAttribute("message", "Đã khôi phục " + n + " ảnh gốc của sản phẩm #" + productId);
        } catch (IOException | IllegalStateException e) {
            log.error("[ImageCompression] Khôi phục sản phẩm {} lỗi", productId, e);
            redirect.addFlashAttribute("error", "Khôi phục lỗi: " + e.getMessage());
        }
        return back(redirect, q, onlyPending, page);
    }

    @PostMapping(BASE + "/products/{productId}/delete-backup")
    public String deleteBackup(@PathVariable long productId,
                               @RequestParam(required = false) String q,
                               @RequestParam(defaultValue = "true") boolean onlyPending,
                               @RequestParam(defaultValue = "0") int page,
                               RedirectAttributes redirect) {
        try {
            int n = productImages.deleteBackup(productId);
            redirect.addFlashAttribute("message", "Đã xoá " + n + " ảnh sao lưu của sản phẩm #" + productId);
        } catch (IOException | IllegalStateException e) {
            log.error("[ImageCompression] Xoá sao lưu sản phẩm {} lỗi", productId, e);
            redirect.addFlashAttribute("error", "Xoá sao lưu lỗi: " + e.getMessage());
        }
        return back(redirect, q, onlyPending, page);
    }

    @GetMapping(BASE + "/products/{productId}/status")
    @ResponseBody
    public Map<String, Object> status(@PathVariable long productId) {
        ProductTask task = productImages.task(productId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("productId", productId);
        if (task != null) {
            map.put("state", task.getState());
            map.put("message", task.getMessage());
            map.put("savedMb", task.getSavedMb());
            map.put("files", task.getFiles());
        }
        return map;
    }

    private static String back(RedirectAttributes redirect, String q, boolean onlyPending, int page) {
        redirect.addAttribute("q", q == null ? "" : q);
        redirect.addAttribute("onlyPending", onlyPending);
        redirect.addAttribute("page", page);
        return "redirect:" + BASE;
    }

    private static double round(double mb) {
        return Math.round(mb * 10) / 10.0;
    }
}
