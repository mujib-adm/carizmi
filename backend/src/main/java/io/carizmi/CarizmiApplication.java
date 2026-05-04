package io.carizmi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@EnableJpaRepositories(basePackages = {"io.carizmi.domain", "io.carizmi.infrastructure.outbox"})
@ConfigurationPropertiesScan("io.carizmi.infrastructure.bootstrap")
public class CarizmiApplication {
  public static void main(String[] args) {
    SpringApplication.run(CarizmiApplication.class, args);
  }
}
