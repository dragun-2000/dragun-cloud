package vn.co.cake.storefront.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import vn.co.cake.storefront.entity.SaleMenuStyle;
import vn.co.cake.storefront.service.StorefrontSettingsService;

class SaleMenuModelAdviceTest {

    @Test
    void addsConfiguredStyle() {
        StorefrontSettingsService service = mock(StorefrontSettingsService.class);
        when(service.getSaleMenuStyle()).thenReturn(SaleMenuStyle.BLACK_FRIDAY);
        Model model = new ExtendedModelMap();

        new SaleMenuModelAdvice(service).addSaleMenu(model);

        assertEquals(SaleMenuStyle.BLACK_FRIDAY, model.getAttribute(SaleMenuModelAdvice.ATTRIBUTE));
    }

    @Test
    void lookupFailure_showsSale() {
        StorefrontSettingsService service = mock(StorefrontSettingsService.class);
        when(service.getSaleMenuStyle()).thenThrow(new IllegalStateException("boom"));
        Model model = new ExtendedModelMap();

        new SaleMenuModelAdvice(service).addSaleMenu(model);

        assertEquals(SaleMenuStyle.SALE, model.getAttribute(SaleMenuModelAdvice.ATTRIBUTE));
    }
}
