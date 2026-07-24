package com.englishlearningcopilot.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class EnglishLearningCopilotBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishLearningCopilotBackendApplication.class, args);
    }
}
