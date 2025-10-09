package vn.co.cake.config;

import nz.net.ultraq.thymeleaf.LayoutDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.springsecurity5.dialect.SpringSecurityDialect;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.spring5.view.ThymeleafViewResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.nio.charset.StandardCharsets;

/**
 * ThymeleafResolver
 */
@Configuration
public class ThymeleafResolver {

    @Bean
    public TemplateEngine htmEngine() {
        TemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.addTemplateResolver(new StringTemplateResolver());
        templateEngine.addDialect(new LayoutDialect());
        return templateEngine;
    }

    @Bean
    public ThymeleafViewResolver viewResolver(SpringTemplateEngine templateEngine) {
        ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
        viewResolver.setContentType("application/json");
        viewResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        viewResolver.setTemplateEngine(templateEngine);
        return viewResolver;
    }

    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
}