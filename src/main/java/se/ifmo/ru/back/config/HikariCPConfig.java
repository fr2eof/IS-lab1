package se.ifmo.ru.back.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class HikariCPConfig {

    private static final Logger logger = LoggerFactory.getLogger(HikariCPConfig.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void logHikariCPConfiguration() {
        if (dataSource instanceof HikariDataSource hikariDataSource) {
            logger.info("=== HikariCP Connection Pool Configuration ===");
            logger.info("Pool Name: {}", hikariDataSource.getPoolName());
            logger.info("Minimum Idle: {}", hikariDataSource.getMinimumIdle());
            logger.info("Maximum Pool Size: {}", hikariDataSource.getMaximumPoolSize());
            logger.info("Idle Timeout: {} ms ({} seconds)", 
                    hikariDataSource.getIdleTimeout(), 
                    hikariDataSource.getIdleTimeout() / 1000);
            logger.info("Max Lifetime: {} ms ({} minutes)", 
                    hikariDataSource.getMaxLifetime(), 
                    hikariDataSource.getMaxLifetime() / 60000);
            logger.info("Connection Timeout: {} ms ({} seconds)", 
                    hikariDataSource.getConnectionTimeout(), 
                    hikariDataSource.getConnectionTimeout() / 1000);
            logger.info("Auto Commit: {}", hikariDataSource.isAutoCommit());
            logger.info("Leak Detection Threshold: {} ms ({} seconds)", 
                    hikariDataSource.getLeakDetectionThreshold(), 
                    hikariDataSource.getLeakDetectionThreshold() / 1000);
            logger.info("Connection Test Query: {}", hikariDataSource.getConnectionTestQuery());
            logger.info("JDBC URL: {}", hikariDataSource.getJdbcUrl());
            logger.info("Driver Class Name: {}", hikariDataSource.getDriverClassName());
            logger.info("=============================================");
        } else {
            logger.warn("DataSource is not an instance of HikariDataSource. Actual type: {}", 
                    dataSource.getClass().getName());
        }
    }
}

