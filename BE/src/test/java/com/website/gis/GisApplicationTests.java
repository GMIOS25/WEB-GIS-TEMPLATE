package com.website.gis;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class GisApplicationTests {

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("app.jwt.secret",
				() -> "test-secret-key-for-integration-tests-only-must-be-at-least-32-bytes-long");
		registry.add("app.jwt.expiration-ms", () -> "86400000");
	}

	@Test
	void contextLoads() {
	}

}
