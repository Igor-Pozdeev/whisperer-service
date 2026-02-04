package ru.pozdeev.whispererservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class WhispererApplication {
    public static void main(String[] args) {
        SpringApplication.run(WhispererApplication.class, args);
    }
}
