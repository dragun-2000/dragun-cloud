package vn.co.cake.storefront.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.storefront.entity.SaleMenuStyle;
import vn.co.cake.storefront.service.StorefrontSettingsService;

/** Puts {@code saleMenu} on every page so header.html shows the SALE entry as configured on AM011. */
@Slf4j
@ControllerAdvice
public class SaleMenuModelAdvice {

    public static final String ATTRIBUTE = "saleMenu";

    private final StorefrontSettingsService settingsService;

    public SaleMenuModelAdvice(StorefrontSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @ModelAttribute
    public void addSaleMenu(Model model) {
        SaleMenuStyle style = SaleMenuStyle.SALE;
        try {
            style = settingsService.getSaleMenuStyle();
        } catch (RuntimeException ex) {
            log.warn("Sale menu setting lookup failed, showing SALE: {}", ex.getMessage());
        }
        model.addAttribute(ATTRIBUTE, style);
    }
}
