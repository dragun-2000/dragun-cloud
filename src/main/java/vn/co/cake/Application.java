package vn.co.cake;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import vn.co.cake.ai.config.OpenAIConfig;

/**
 * FileMonitoringApplication
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(OpenAIConfig.class)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
