package me.paulbaur.ict;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IctApplication {
    public static void main(String[] args) {
        SpringApplication.run(IctApplication.class, args);
    }
}
