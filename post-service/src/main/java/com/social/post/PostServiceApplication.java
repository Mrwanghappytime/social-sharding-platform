package com.social.post;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableDubbo
@EntityScan("com.social.common.entity")
@EnableJpaRepositories("com.social.common.repository")
@ComponentScan(basePackages = {"com.social.post", "com.social.common"})
public class PostServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PostServiceApplication.class, args);
    }
}
