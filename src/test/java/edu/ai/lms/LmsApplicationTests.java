package edu.ai.lms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.modulith.core.ApplicationModules;

@SpringBootTest
class LmsApplicationTests {

	ApplicationModules modules = ApplicationModules.of(LmsApplication.class);

	@Test
	void contextLoads() {
	}

	@Test
	void verifyModulesStructure() {
		System.out.println(modules);
		modules.verify();
	}

}
