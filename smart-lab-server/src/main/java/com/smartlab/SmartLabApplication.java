package com.smartlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartLabApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartLabApplication.class, args);
    }
}