package de.lucahenn.chatwithcodebase;

import org.springframework.boot.SpringApplication;
import com.codeium.chatcodebase.ChatCodebaseApplication;

public class TestChatWithCodebaseApplication {

    public static void main(String[] args) {
        SpringApplication.from(ChatCodebaseApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
