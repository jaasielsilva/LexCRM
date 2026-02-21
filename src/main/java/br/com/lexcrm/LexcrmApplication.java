package br.com.lexcrm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LexcrmApplication {

	public static void main(String[] args) {
		SpringApplication.run(LexcrmApplication.class, args);
	}

}
