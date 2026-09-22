package com.justtarot.tarot_reading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TarotReadingApplication {

	public static void main(String[] args) {
		SpringApplication.run(TarotReadingApplication.class, args);
	}

}
