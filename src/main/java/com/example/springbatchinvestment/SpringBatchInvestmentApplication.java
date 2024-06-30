package com.example.springbatchinvestment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;


@EnableJpaAuditing
@SpringBootApplication
public class SpringBatchInvestmentApplication {

    public static void main(String[] args) {

        System.exit(SpringApplication
                .exit(SpringApplication.run(SpringBatchInvestmentApplication.class, args)));
//        SpringApplication.run(SpringBatchInvestmentApplication.class, args);
    }
}
