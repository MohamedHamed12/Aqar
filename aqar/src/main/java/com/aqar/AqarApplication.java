package com.aqar;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@ConfigurationPropertiesScan
@SpringBootApplication
public class AqarApplication {

	public static void main(String[] args) {
		SpringApplication.run(AqarApplication.class, args);
	}

}
