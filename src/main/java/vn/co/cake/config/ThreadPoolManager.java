package vn.co.cake.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolExecutorFactoryBean;

/**
 * ThreadPoolManager
 */
@Configuration
@EnableAsync
public class ThreadPoolManager {

    @Autowired
    private Environment env;

    @Bean
    public ThreadPoolExecutorFactoryBean threadPoolExecutor() {
        ThreadPoolExecutorFactoryBean threadPoolExecutorFactoryBean = new ThreadPoolExecutorFactoryBean();
        threadPoolExecutorFactoryBean.setCorePoolSize(env.getRequiredProperty("threadpoll.corePoolSize", Integer.class));
        threadPoolExecutorFactoryBean.setMaxPoolSize(env.getRequiredProperty("threadpoll.maxPoolSize", Integer.class));
        threadPoolExecutorFactoryBean.setKeepAliveSeconds(env.getRequiredProperty("threadpoll.keepAliveSeconds", Integer.class));
        threadPoolExecutorFactoryBean.setQueueCapacity(env.getRequiredProperty("threadpoll.queueCapacity", Integer.class));
        threadPoolExecutorFactoryBean.setAllowCoreThreadTimeOut(env.getRequiredProperty("threadpoll.allowCoreThreadTimeOut", Boolean.class));
        threadPoolExecutorFactoryBean.setWaitForTasksToCompleteOnShutdown(false);
        threadPoolExecutorFactoryBean.setKeepAliveSeconds(30);
        threadPoolExecutorFactoryBean.setDaemon(true);

        threadPoolExecutorFactoryBean.afterPropertiesSet();
        return threadPoolExecutorFactoryBean;
    }
}
