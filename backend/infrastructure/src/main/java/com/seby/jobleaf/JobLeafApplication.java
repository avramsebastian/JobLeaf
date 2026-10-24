package com.seby.jobleaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot entry point for the jobleaf monolithic application (hexagonal adapters under {@code com.seby.jobleaf}).
 */
@SpringBootApplication(scanBasePackages = "com.seby.jobleaf")
@EnableScheduling
public class JobLeafApplication {

    static void main(String[] args) {
        SpringApplication.run(JobLeafApplication.class, args);
    }

}
