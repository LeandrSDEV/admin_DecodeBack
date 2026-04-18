package br.com.portal.decode_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class DecodeApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DecodeApiApplication.class, args);
	}

}
