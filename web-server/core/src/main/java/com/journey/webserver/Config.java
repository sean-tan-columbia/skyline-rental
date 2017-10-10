package com.journey.webserver;

import java.io.FileInputStream;
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
    private final String[] googleApiScopes;
    private final String googleApiCredPath;
    private final String googleApiCredUser;
    private final Long googleApiTokenTTL;
    private final Long sessionRetryTimeout;
    private final Long sessionTimeout;
    private final String serverHostName;

    public static Config getInstance(String env) throws IOException {
        if (instance == null) {
            instance = new Config(env);
        }
        return instance;
    }

    private Config(String env) throws IOException {
        String file = "config/dev.properties";
        if (env.equals("prod")) {
            file = "config/prod.properties";
        }
        Properties properties = new Properties();
        // properties.load(getClass().getClassLoader().getResourceAsStream(file));
        String path = file;
        properties.load(new FileInputStream(path));
        this.redisHost = properties.getProperty("database.redis.host");
        this.redisAuth = properties.getProperty("database.redis.auth");
        this.postgresHost = properties.getProperty("database.postgres.host");
        this.postgresName = properties.getProperty("database.postgres.name");
        this.postgresUser = properties.getProperty("database.postgres.user");
        this.postgresAuth = properties.getProperty("database.postgres.auth");
        this.postgresDriver = properties.getProperty("database.postgres.driver");
        this.googleApiClientId = properties.getProperty("auth.googleapi.client.id");
        this.googleApiClientSecret = properties.getProperty("auth.googleapi.client.secret");
        this.googleApiScopes = properties.getProperty("auth.googleapi.client.scopes").split(",");
        this.googleApiCredPath = properties.getProperty("auth.googleapi.cred.path");
        this.googleApiCredUser = properties.getProperty("auth.googleapi.cred.user");

        this.googleApiTokenTTL = Long.parseLong(properties.getProperty("auth.googleapi.token.ttl"));
        this.sessionTimeout = Long.parseLong(properties.getProperty("server.session.timeout"));
        this.sessionRetryTimeout = Long.parseLong(properties.getProperty("server.session.retry.timeout"));
        this.serverHostName = properties.getProperty("server.host.name");
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

    public String[] getGoogleApiScopes() {
        return googleApiScopes;
    }

    public String getGoogleApiCredPath() {
        return googleApiCredPath;
    }

    public String getGoogleApiCredUser() {
        return googleApiCredUser;
    }

    public Long getGoogleApiTokenTTL() {
        return googleApiTokenTTL;
    }

    public Long getSessionRetryTimeout() {
        return sessionRetryTimeout;
    }

    public Long getSessionTimeout() {
        return sessionTimeout;
    }

    public String getServerHostName() {
        return this.serverHostName;
    }
}
