package br.com.rytechh.greenshift;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GreenShiftApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenShiftApplication.class, args);
    }
}
