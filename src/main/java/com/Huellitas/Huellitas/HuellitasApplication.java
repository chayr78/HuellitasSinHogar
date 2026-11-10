package com.Huellitas.Huellitas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/*
 * Punto de entrada: arranca la aplicación Spring Boot.
 * @EnableAsync activa los métodos @Async (los correos se envían en segundo plano).
 */
@SpringBootApplication
@EnableAsync
public class HuellitasApplication {

	public static void main(String[] args) {
		SpringApplication.run(HuellitasApplication.class, args);
	}

}
