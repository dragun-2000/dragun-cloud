package vn.co.cake.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "https://www.phodem.click",
                        "https://dragun.cloud",
                        "https://debase.vn",
                        "https://thiyen.vn",
                        "https://botnguhacmeden.thiyen.vn"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/external-images/**")
                .addResourceLocations("file:/var/www/html/external-images/")
                // Cache static file 30 ngày
                .setCachePeriod(2592000); // 30 * 24 * 60 * 60

        // Các static khác (JS, CSS, fonts, ...) lấy từ classpath
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}