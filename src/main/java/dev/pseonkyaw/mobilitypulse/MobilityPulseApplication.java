package dev.pseonkyaw.mobilitypulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MobilityPulseApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobilityPulseApplication.class, args);
	}

}
