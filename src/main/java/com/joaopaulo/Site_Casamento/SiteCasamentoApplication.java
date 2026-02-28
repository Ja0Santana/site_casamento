package com.joaopaulo.Site_Casamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SiteCasamentoApplication {
	public static void main(String[] args) {
		SpringApplication.run(SiteCasamentoApplication.class, args);
	}
}
