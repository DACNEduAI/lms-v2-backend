package edu.ai.lms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.FullyQualifiedAnnotationBeanNameGenerator;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class)
@EnableScheduling
@MapperScan(
	basePackages = "edu.ai.lms",
	annotationClass = Mapper.class,
	nameGenerator = FullyQualifiedAnnotationBeanNameGenerator.class
)
public class LmsApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(LmsApplication.class, args);
	}

}
