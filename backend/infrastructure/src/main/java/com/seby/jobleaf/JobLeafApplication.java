package com.seby.jobleaf;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the jobleaf monolithic application (hexagonal adapters under {@code com.seby.jobleaf}).
 */
@SpringBootApplication(scanBasePackages = "com.seby.jobleaf")
public class JobLeafApplication {

    static void main(String[] args) {
        SpringApplication.run(JobLeafApplication.class, args);
    }

}
