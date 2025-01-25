package de.lucahenn.chatwithcodebase;

import org.springframework.boot.SpringApplication;

public class TestChatWithCodebaseApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChatWithCodebaseApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
