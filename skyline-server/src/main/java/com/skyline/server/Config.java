package com.skyline.server;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by jtan on 6/12/17.
 */
public class Config {

    private static Config instance;
    private final String redisHost;
    private final String redisAuth;
    private final String postgresHost;
    private final String postgresName;
    private final String postgresUser;
    private final String postgresAuth;
    private final String postgresDriver;
    private final String googleApiClientId;
    private final String googleApiClientSecret;
    private final String googleApiScope;
    private final String googleApiCredPath;
    private final Long googleApiTokenTTL;


    public static Config getInstance() throws IOException {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private Config() throws IOException {
        Properties properties = new Properties();
        properties.load(getClass().getClassLoader().getResourceAsStream("config/dev.properties"));
        this.redisHost = properties.getProperty("database.redis.host");
        this.redisAuth = properties.getProperty("database.redis.auth");
        this.postgresHost = properties.getProperty("database.postgres.host");
        this.postgresName = properties.getProperty("database.postgres.name");
        this.postgresUser = properties.getProperty("database.postgres.user");
        this.postgresAuth = properties.getProperty("database.postgres.auth");
        this.postgresDriver = properties.getProperty("database.postgres.driver");
        this.googleApiClientId = properties.getProperty("auth.googleapi.client.id");
        this.googleApiClientSecret = properties.getProperty("auth.googleapi.client.secret");
        this.googleApiScope = properties.getProperty("auth.googleapi.client.scope");
        this.googleApiCredPath = properties.getProperty("auth.googleapi.cred.path");
        this.googleApiTokenTTL = Long.parseLong(properties.getProperty("auth.googleapi.token.ttl"));
    }

    public String getRedisHost() {
        return this.redisHost;
    }

    public String getRedisAuth() {
        return this.redisAuth;
    }

    public String getPostgresHost() {
        return postgresHost;
    }

    public String getPostgresName() {
        return postgresName;
    }

    public String getPostgresUser() {
        return postgresUser;
    }

    public String getPostgresAuth() {
        return postgresAuth;
    }

    public String getPostgresDriver() {
        return this.postgresDriver;
    }

    public String getGoogleApiClientId() {
        return googleApiClientId;
    }

    public String getGoogleApiClientSecret() {
        return googleApiClientSecret;
    }

    public String getGoogleApiScope() {
        return googleApiScope;
    }

    public String getGoogleApiCredPath() {
        return googleApiCredPath;
    }

    public Long getGoogleApiTokenTTL() {
        return googleApiTokenTTL;
    }
}
