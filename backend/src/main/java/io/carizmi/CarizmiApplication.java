package io.carizmi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "io.carizmi.domain")
@ConfigurationPropertiesScan("io.carizmi.infrastructure.bootstrap")
public class CarizmiApplication {
  public static void main(String[] args) {
    SpringApplication.run(CarizmiApplication.class, args);
  }
}
