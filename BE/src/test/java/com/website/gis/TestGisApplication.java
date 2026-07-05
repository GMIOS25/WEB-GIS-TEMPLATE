package com.website.gis;

import org.springframework.boot.SpringApplication;

public class TestGisApplication {

	public static void main(String[] args) {
		SpringApplication.from(GisApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
