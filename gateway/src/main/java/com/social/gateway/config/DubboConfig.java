package com.social.gateway.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo configuration for service proxying.
 * Enables communication with backend services via Dubbo RPC.
 */
@Configuration
public class DubboConfig {

    @Value("${spring.cloud.nacos.discovery.server-addr:127.0.0.1:8848}")
    private String nacosServerAddr;

    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig config = new ApplicationConfig();
        config.setName("gateway");
        config.setQosEnable(false);
        return config;
    }

    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig config = new RegistryConfig();
        config.setAddress("nacos");
        config.setRegister(false); // Gateway doesn't need to register as a service
        config.setParameters(java.util.Map.of("serverAddr", nacosServerAddr));
        return config;
    }

    @Bean
    public ConsumerConfig consumerConfig() {
        ConsumerConfig config = new ConsumerConfig();
        config.setTimeout(5000);
        config.setCheck(false);
        return config;
    }
}
