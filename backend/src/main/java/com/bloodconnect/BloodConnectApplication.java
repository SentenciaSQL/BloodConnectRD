package com.bloodconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BloodConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(BloodConnectApplication.class, args);
    }
}
