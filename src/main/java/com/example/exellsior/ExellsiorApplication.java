package com.example.exellsior;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class ExellsiorApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExellsiorApplication.class, args);
	}

}
