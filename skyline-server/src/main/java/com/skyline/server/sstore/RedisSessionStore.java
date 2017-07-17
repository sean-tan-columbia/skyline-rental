package com.skyline.server.sstore;

import com.skyline.server.sstore.impl.RedisSessionStoreImpl;
import io.vertx.core.Vertx;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.redis.RedisClient;

/**
 * Created by jtan on 7/17/17.
 */
public interface RedisSessionStore extends SessionStore {
    String DEFAULT_SESSION_MAP_NAME = "vertx-web.sessions";
    long DEFAULT_RETRY_TIMEOUT = 5000L;

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient, String sessionMapName) {
        return new RedisSessionStoreImpl(vertx, redisClient, sessionMapName, DEFAULT_RETRY_TIMEOUT);
    }

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient, String sessionMapName, long retryTimeout) {
        return new RedisSessionStoreImpl(vertx, redisClient, sessionMapName, retryTimeout);
    }

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient) {
        return new RedisSessionStoreImpl(vertx, redisClient, DEFAULT_SESSION_MAP_NAME, DEFAULT_RETRY_TIMEOUT);
    }

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient, long retryTimeout) {
        return new RedisSessionStoreImpl(vertx, redisClient, DEFAULT_SESSION_MAP_NAME, retryTimeout);
    }
}
