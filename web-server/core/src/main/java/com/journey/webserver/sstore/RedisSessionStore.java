package com.journey.webserver.sstore;

import com.journey.webserver.sstore.impl.RedisSessionStoreImpl;
import io.vertx.core.Vertx;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.redis.RedisClient;

/**
 * Created by jtan on 7/17/17.
 */
public interface RedisSessionStore extends SessionStore {
    String DEFAULT_SESSION_KEY_BASE = "data.web.session:";
    long DEFAULT_RETRY_TIMEOUT = 5000L;

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient, String sessionKeyBase) {
        return new RedisSessionStoreImpl(vertx, redisClient, sessionKeyBase, DEFAULT_RETRY_TIMEOUT);
    }

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient, String sessionKeyBase, long retryTimeout) {
        return new RedisSessionStoreImpl(vertx, redisClient, sessionKeyBase, retryTimeout);
    }

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient) {
        return new RedisSessionStoreImpl(vertx, redisClient, DEFAULT_SESSION_KEY_BASE, DEFAULT_RETRY_TIMEOUT);
    }

    static RedisSessionStore create(Vertx vertx, RedisClient redisClient, long retryTimeout) {
        return new RedisSessionStoreImpl(vertx, redisClient, DEFAULT_SESSION_KEY_BASE, retryTimeout);
    }
}
