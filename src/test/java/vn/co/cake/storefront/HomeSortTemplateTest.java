package vn.co.cake.storefront;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import vn.co.cake.enums.ProductSortOption;

/** Render the home page to check the ALL PRODUCTS sort control. */
class HomeSortTemplateTest {

    private static final Pattern FORM = Pattern.compile("<form\\b[^>]*class=\"products-toolbar[^\"]*\"[^>]*>");
    private static final Pattern OPTION = Pattern.compile("<option value=\"([A-Z_]+)\"(\\s+selected=\"selected\")?>([^<]*)</option>");

    @Test
    void sortControlListsEveryOptionAndMarksTheCurrentOne() {
        String html = renderIndex(ProductSortOption.PRICE_ASC, Locale.forLanguageTag("vi"));

        Matcher form = FORM.matcher(html);
        assertTrue(form.find(), "sort form rendered");
        assertTrue(form.group(0).contains("action=\"/\""), form.group(0));
        assertTrue(form.group(0).contains("method=\"get\""), form.group(0));
        assertTrue(html.contains("id=\"all-products\""));
        assertTrue(html.contains("Sắp xếp"));

        List<String> values = new ArrayList<>();
        String selected = null;
        Matcher options = OPTION.matcher(html);
        while (options.find()) {
            values.add(options.group(1));
            if (options.group(2) != null) {
                selected = options.group(1);
                assertEquals("Giá: thấp đến cao", options.group(3));
            }
        }
        assertEquals(List.of("NAME_ASC", "PRICE_DESC", "PRICE_ASC", "DISCOUNT_DESC"), values);
        assertEquals("PRICE_ASC", selected);
    }

    @Test
    void englishLabels() {
        String html = renderIndex(ProductSortOption.CUSTOMER_DEFAULT, Locale.ENGLISH);

        assertTrue(html.contains(">Sort by<"));
        assertTrue(Pattern.compile("<option value=\"DISCOUNT_DESC\"\\s+selected=\"selected\">Biggest discount</option>")
                .matcher(html).find());
        assertTrue(html.contains(">Price: high to low<"));
        assertTrue(html.contains(">Name: A - Z<"));
    }

    private static String renderIndex(ProductSortOption current, Locale locale) {
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

        Map<String, Object> product = new HashMap<>();
        product.put("id", 1L);
        product.put("image", "/img/p.jpg");
        product.put("name", "Dress");
        product.put("outStock", false);
        product.put("hasDiscount", false);
        product.put("priceDisplay", "100,000");
        product.put("discountPriceDisplay", "100,000");
        product.put("codeColors", Collections.emptyList());

        Map<String, Object> vars = new HashMap<>();
        vars.put("isLogin", false);
        vars.put("newInProducts", Collections.emptyList());
        vars.put("products", List.of(product));
        vars.put("sortOptions", ProductSortOption.CUSTOMER_OPTIONS);
        vars.put("currentSort", current);
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(new MockHttpServletRequest(servletContext),
                new MockHttpServletResponse(), servletContext, locale, vars);
        return engine.process("index", context);
    }
}
