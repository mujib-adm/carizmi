package org.sofumar.portal.dbsync;

import org.apache.commons.lang3.StringUtils;
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
        String dbUrl = System.getenv("DB_URL");
        if (StringUtils.isBlank(dbUrl)) {
            dbUrl = System.getProperty("DB_URL", "jdbc:mysql://db:3306/sofumar?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&zeroDateTimeBehavior=CONVERT_TO_NULL&characterEncoding=UTF-8");
        }

        String dbUser = System.getenv("DB_USER");
        if (StringUtils.isBlank(dbUser)) {
            dbUser = System.getProperty("DB_USER", "sofumar");
        }

        String dbPass = System.getenv("DB_PASS");
        if (StringUtils.isBlank(dbPass)) {
            dbPass = System.getProperty("DB_PASS", "sofumar_pwd");
        }

        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(dbUser)
                .password(dbPass)
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