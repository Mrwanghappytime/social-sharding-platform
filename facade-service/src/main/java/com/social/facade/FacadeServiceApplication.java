package com.social.facade;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDubbo
@ComponentScan(basePackages = {"com.social.facade", "com.social.common"})
public class FacadeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(FacadeServiceApplication.class, args);
    }
}
