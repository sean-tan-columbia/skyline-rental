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
    private final String imagePath;
    private final String webroot;

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
        this.imagePath = properties.getProperty("storage.image.path");
        this.webroot = properties.getProperty("storage.file.webroot");
    }

    public String getRedisHost() {
        return this.redisHost;
    }

    public String getRedisAuth() {
        return this.redisAuth;
    }

    public String getImagePath() {
        return this.imagePath;
    }

    public String getWebroot() {
        return this.webroot;
    }

}
