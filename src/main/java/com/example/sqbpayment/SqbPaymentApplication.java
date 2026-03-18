package com.example.sqbpayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SqbPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqbPaymentApplication.class, args);
    }
}
