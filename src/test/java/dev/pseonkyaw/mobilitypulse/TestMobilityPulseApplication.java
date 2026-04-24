package dev.pseonkyaw.mobilitypulse;

import org.springframework.boot.SpringApplication;

public class TestMobilityPulseApplication {

	public static void main(String[] args) {
		SpringApplication.from(MobilityPulseApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
