package com.website.gis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GisApplication {

	public static void main(String[] args) {
		SpringApplication.run(GisApplication.class, args);
	}

}
