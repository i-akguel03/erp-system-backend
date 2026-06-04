package com.erp.backend;

import io.github.cdimascio.dotenv.Dotenv;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@OpenAPIDefinition(
		info = @Info(title = "ERP System API", version = "1.0", description = "Verwaltung von Kunden, Produkten, Aufträgen.")
)
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ErpSystemBackendApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(e -> {
			if (System.getenv(e.getKey()) == null) {
				System.setProperty(e.getKey(), e.getValue());
			}
		});
		SpringApplication.run(ErpSystemBackendApplication.class, args);
	}

}
