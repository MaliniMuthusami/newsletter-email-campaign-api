package com.newsletter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NewsletterApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewsletterApiApplication.class, args);
    }
}
