package vn.co.cake.controller.AM011;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;

import vn.co.cake.common.BaseConst;
import vn.co.cake.enums.Authorities;
import vn.co.cake.payment.service.VietQrPilotAccessService;
import vn.co.cake.security.admin.AdminLoginInfo;
import vn.co.cake.storefront.dto.SaleMenuSettingsForm;
import vn.co.cake.storefront.entity.SaleMenuStyle;
import vn.co.cake.storefront.service.StorefrontSettingsService;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.service.VnpayPilotAccessService;

class AM011SaleMenuSaveTest {

    private StorefrontSettingsService storefrontSettingsService;
    private AM011Controller controller;
    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        storefrontSettingsService = mock(StorefrontSettingsService.class);
        controller = new AM011Controller(mock(VietQrPilotAccessService.class), mock(VnpayPilotAccessService.class),
                new VnpayProperties(), storefrontSettingsService);
        session = new MockHttpSession();
    }

    private void loginAs(Authorities authority) {
        AdminLoginInfo admin = mock(AdminLoginInfo.class);
        when(admin.getAuthority()).thenReturn(authority);
        when(admin.getUsername()).thenReturn("admin01");
        session.setAttribute(BaseConst.ADMIN_SESSION, admin);
    }

    private static SaleMenuSettingsForm form(String style) {
        SaleMenuSettingsForm form = new SaleMenuSettingsForm();
        form.setSaleMenuStyle(style);
        return form;
    }

    @Test
    void admin_savesBlackFriday() {
        loginAs(Authorities.ROLE_ADMIN);

        ResponseEntity<String> result = controller.saveSaleMenu(form("BLACK_FRIDAY"), session);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(storefrontSettingsService).saveSaleMenuStyle(SaleMenuStyle.BLACK_FRIDAY, "admin01");
    }

    @Test
    void notLoggedIn_isForbidden() {
        ResponseEntity<String> result = controller.saveSaleMenu(form("SALE"), session);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        verify(storefrontSettingsService, never()).saveSaleMenuStyle(any(), any());
    }

    @Test
    void staff_isForbidden() {
        loginAs(Authorities.ROLE_STAFF);

        ResponseEntity<String> result = controller.saveSaleMenu(form("SALE"), session);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        verify(storefrontSettingsService, never()).saveSaleMenuStyle(any(), any());
    }

    @Test
    void unknownStyle_isBadRequest() {
        loginAs(Authorities.ROLE_ADMIN);

        ResponseEntity<String> result = controller.saveSaleMenu(form("PURPLE"), session);

        assertEquals(HttpStatus.BAD_REQUEST, result.getStatusCode());
        verify(storefrontSettingsService, never()).saveSaleMenuStyle(any(), any());
    }
}
