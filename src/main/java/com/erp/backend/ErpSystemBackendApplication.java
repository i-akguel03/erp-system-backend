package com.erp.backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(
		info = @Info(title = "ERP System API", version = "1.0", description = "Verwaltung von Kunden, Produkten, Aufträgen.")
)
@SpringBootApplication
@EnableScheduling
public class ErpSystemBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ErpSystemBackendApplication.class, args);
	}

}
