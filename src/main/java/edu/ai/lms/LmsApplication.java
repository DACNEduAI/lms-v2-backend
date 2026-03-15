package edu.ai.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableJdbcAuditing
@EnableScheduling
@EnableJdbcRepositories(basePackages = "edu.ai.lms", nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
public class LmsApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(LmsApplication.class, args);
	}

}
