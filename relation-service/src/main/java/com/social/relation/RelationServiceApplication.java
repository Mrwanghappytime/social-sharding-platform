package com.social.relation;

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
@ComponentScan(basePackages = {"com.social.relation", "com.social.common"})
public class RelationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RelationServiceApplication.class, args);
    }
}
