package org.sofumar.portal.dbsync;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@Profile("liquibase-diff")
@EntityScan(basePackages = "org.sofumar.portal.core.vo")
public class JpaLiquibaseConfig {

    @Bean
    public DataSource dataSource() {
        // You can externalize these to application.yml or system properties
        return DataSourceBuilder.create()
                .url("jdbc:mysql://localhost:3306/sofumar?useSSL=false&allowPublicKeyRetrieval=true")
                .username("sofumar")
                .password("sofumar_pwd")
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("org.sofumar.portal.core.vo"); // entities
        emf.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.setProperty("hibernate.hbm2ddl.auto", "none"); // let Liquibase handle schema
        emf.setJpaProperties(props);

        return emf;
    }

    @Bean
    public JpaTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        assert emf.getObject() != null;
        return new JpaTransactionManager(emf.getObject());
    }

}