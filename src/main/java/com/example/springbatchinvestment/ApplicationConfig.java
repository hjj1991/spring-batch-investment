package com.example.springbatchinvestment;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class ApplicationConfig {

    @Bean
    public ModelMapper modelMapper(){
        return new ModelMapper();
    }


    @Bean
    public WebClient webClient(){
        return WebClient.builder()
                .filter(ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
                            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
                            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
                            return Mono.just(clientRequest);
                        }
                ))
                .build();
    }

}
