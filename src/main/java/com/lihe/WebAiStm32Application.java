package com.lihe;

import com.lihe.netty.server.NettyServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class WebAiStm32Application {
    public static void main(String[] args) {
        SpringApplication.run(WebAiStm32Application.class, args);
    }
}
