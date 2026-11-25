package vn.co.cake.controller.AM002;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import vn.co.cake.controller.external.dto.response.OrderDetailResponse;
import vn.co.cake.entity.Order;

/**
 * "Ngày Đặt Hàng" trên AM002/AM006 phải là giờ Việt Nam, dù JVM chạy ở múi giờ nào
 * (server VN hay container Docker để mặc định UTC).
 */
class AM002OrderCreatedTimeTest {

    /** 03:04:05 UTC = 10:04:05 giờ Việt Nam. */
    private static final Instant ORDERED_AT = Instant.parse("2026-11-25T03:04:05Z");
    private static final String EXPECTED_VN = "2026-11-25 10:04:05";

    @ParameterizedTest
    @ValueSource(strings = {"Asia/Ho_Chi_Minh", "UTC"})
    void orderListShowsVietnamTime_whateverTheJvmTimezone(String jvmTimezone) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(jvmTimezone));
            Order order = new Order();
            order.setCode("DB0001");
            order.setCreated(Date.from(ORDERED_AT));
            List<OrderDetailResponse> orders = Collections.singletonList(OrderDetailResponse.forList(order));

            for (String screen : new String[] {"AM002/AM002", "AM006/AM006"}) {
                String html = renderOrderRows(screen, orders);
                assertTrue(html.contains(EXPECTED_VN),
                        screen + " với JVM " + jvmTimezone + " phải hiện " + EXPECTED_VN + ", nhưng ra:\n" + html);
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    private String renderOrderRows(String template, List<OrderDetailResponse> orders) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        Map<String, Object> vars = new HashMap<>();
        vars.put("orders", orders);
        vars.put("totalRecord", orders.size());
        MockServletContext servletContext = new MockServletContext();
        WebContext context = new WebContext(new MockHttpServletRequest(servletContext),
                new MockHttpServletResponse(), servletContext, Locale.forLanguageTag("vi"), vars);
        return engine.process(template, Collections.singleton("tbody"), context);
    }
}
