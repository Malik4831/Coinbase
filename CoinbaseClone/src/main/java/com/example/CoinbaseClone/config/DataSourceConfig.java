package com.example.CoinbaseClone.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;

@Configuration
public class DataSourceConfig {

    @Bean
    @Primary
    public DataSource dataSource(Environment environment) {
        String rawUrl = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        String password = environment.getProperty("spring.datasource.password");
        String driverClassName = environment.getProperty(
                "spring.datasource.driver-class-name",
                "org.postgresql.Driver"
        );

        ParsedDatabaseUrl parsed = parseDatabaseUrl(rawUrl);

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setJdbcUrl(parsed.jdbcUrl());
        dataSource.setUsername(hasText(username) ? username : parsed.username());
        dataSource.setPassword(hasText(password) ? password : parsed.password());
        return dataSource;
    }

    private ParsedDatabaseUrl parseDatabaseUrl(String rawUrl) {
        if (!hasText(rawUrl)) {
            throw new IllegalStateException("spring.datasource.url must not be empty");
        }

        if (rawUrl.startsWith("jdbc:")) {
            return new ParsedDatabaseUrl(rawUrl, null, null);
        }

        if (!rawUrl.startsWith("postgres://") && !rawUrl.startsWith("postgresql://")) {
            throw new IllegalStateException("Unsupported datasource URL: " + rawUrl);
        }

        try {
            URI uri = new URI(rawUrl);
            String host = uri.getHost();
            int port = uri.getPort();
            String path = uri.getPath();
            String query = uri.getQuery();
            String jdbcUrl = "jdbc:postgresql://" + host + (port > 0 ? ":" + port : "") + path;
            if (hasText(query)) {
                jdbcUrl += "?" + query;
            }

            String username = null;
            String password = null;
            String userInfo = uri.getUserInfo();
            if (hasText(userInfo)) {
                String[] parts = userInfo.split(":", 2);
                username = parts[0];
                password = parts.length > 1 ? parts[1] : null;
            }

            return new ParsedDatabaseUrl(jdbcUrl, username, password);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Invalid datasource URL: " + rawUrl, exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record ParsedDatabaseUrl(String jdbcUrl, String username, String password) {
    }
}
