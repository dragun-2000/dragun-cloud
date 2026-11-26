package vn.co.cake.storefront;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import vn.co.cake.storefront.entity.SaleMenuStyle;

/** Render the storefront header to check the SALE menu label and colour follow the admin setting. */
class SaleMenuHeaderTemplateTest {

    private static final Pattern SALE_LINK = Pattern.compile("<a[^>]*href=\"/products/group/SALE\"[^>]*>([^<]*)</a>");

    @Test
    void blackFriday_showsRedBlackFridayInDesktopAndMobileMenus() {
        String html = renderHeader(SaleMenuStyle.BLACK_FRIDAY, Locale.ENGLISH);

        Matcher links = SALE_LINK.matcher(html);
        int count = 0;
        while (links.find()) {
            count++;
            assertEquals("BLACK FRIDAY", links.group(1).trim());
            assertTrue(links.group(0).contains("is-black-friday"), links.group(0));
        }
        assertEquals(2, count, "desktop + mobile menu");
    }

    @Test
    void sale_keepsCurrentLabelAndNoHighlight() {
        String html = renderHeader(SaleMenuStyle.SALE, Locale.forLanguageTag("vi"));

        Matcher links = SALE_LINK.matcher(html);
        int count = 0;
        while (links.find()) {
            count++;
            assertEquals("GIẢM GIÁ", links.group(1).trim());
            assertFalse(links.group(0).contains("is-black-friday"), links.group(0));
        }
        assertEquals(2, count);
    }

    @Test
    void missingSetting_fallsBackToSale() {
        String html = renderHeader(null, Locale.ENGLISH);

        Matcher links = SALE_LINK.matcher(html);
        assertTrue(links.find());
        assertEquals("SALE", links.group(1).trim());
        assertFalse(html.contains("is-black-friday"));
    }

    private static String renderHeader(SaleMenuStyle style, Locale locale) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messages);

        Map<String, Object> vars = new HashMap<>();
        if (style != null) {
            vars.put("saleMenu", style);
        }
        vars.put("isLogin", false);
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(new MockHttpServletRequest(servletContext),
                new MockHttpServletResponse(), servletContext, locale, vars);
        return engine.process("header", Collections.singleton("header"), context);
    }
}
