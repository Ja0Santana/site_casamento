package com.joaopaulo.Site_Casamento.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class MercadoPagoConfig {
    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Bean
    public RestClient mercadoPagoClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://api.mercadopago.com")
                .defaultHeader("Authorization", "Bearer " + accessToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
