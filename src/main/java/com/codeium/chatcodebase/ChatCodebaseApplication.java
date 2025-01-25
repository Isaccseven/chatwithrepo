package com.codeium.chatcodebase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class ChatCodebaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatCodebaseApplication.class, args);
    }
}
