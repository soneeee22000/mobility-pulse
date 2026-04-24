package dev.pseonkyaw.mobilitypulse.config;

import dev.pseonkyaw.mobilitypulse.simulator.SimulatorProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SimulatorProperties.class)
@ConfigurationPropertiesScan("dev.pseonkyaw.mobilitypulse")
public class AppConfig {
}
