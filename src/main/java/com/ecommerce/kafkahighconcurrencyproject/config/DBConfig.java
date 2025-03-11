package com.ecommerce.kafkahighconcurrencyproject.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Log4j2
@ConditionalOnProperty(name = "enable.ekart-db", havingValue = "true")
public class DBConfig {

    @Autowired
    private ConfigProperty configProperty;

    @Bean
    @Qualifier("ekart-db")
    public DataSource ekartDatasource() {
        HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
//        config.addDataSourceProperty("databaseName", configProperty.getEkartDBName());
//        config.setJdbcUrl(configProperty.getEkartDBUrl());
//        config.setUsername(configProperty.getEkartDBUsername());
//        config.setPassword(configProperty.getEkartDBPassword());
//        config.setSchema(configProperty.getEkartDBSchemaName());
//        config.addDataSourceProperty("databaseName", "ekart_new");
        config.addDataSourceProperty("url", configProperty.getEkartDBUrl());
        config.addDataSourceProperty("user", configProperty.getEkartDBUsername());
        config.addDataSourceProperty("password", configProperty.getEkartDBPassword());
        config.setMinimumIdle(10);
        config.setMaximumPoolSize(200);
        config.setConnectionTimeout(150000);

        log.error("using hikari"+config.getDataSourceProperties().getProperty("url"));
        log.info("using hikari: "+config.getDataSourceProperties().getProperty("url"));
        return new HikariDataSource(config);
    }
}