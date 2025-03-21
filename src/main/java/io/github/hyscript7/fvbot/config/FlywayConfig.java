package io.github.hyscript7.fvbot.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class FlywayConfig {
    @Bean
    public FlywayConfigurationCustomizer flywayCustomizer(@Nullable DataSource dataSource) {
        if (!(dataSource instanceof HikariDataSource)) {
            throw new IllegalStateException("Unsupported dataSource: " + dataSource);
        }
        String datasourceDriver = ((HikariDataSource) dataSource).getDriverClassName();
        return config -> {
            String migrationLocation;

            if (datasourceDriver.contains("h2") || datasourceDriver.isBlank()) {
                migrationLocation = "classpath:db/migration/h2";
            } else if (datasourceDriver.contains("postgresql")) {
                migrationLocation = "classpath:db/migration/postgresql";
            } else {
                throw new IllegalStateException("Unsupported database: " + datasourceDriver);
            }

            config.locations(migrationLocation);
        };
    }
}
