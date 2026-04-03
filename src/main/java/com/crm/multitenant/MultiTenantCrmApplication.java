package com.crm.multitenant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
public class MultiTenantCrmApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiTenantCrmApplication.class, args);
    }

}
