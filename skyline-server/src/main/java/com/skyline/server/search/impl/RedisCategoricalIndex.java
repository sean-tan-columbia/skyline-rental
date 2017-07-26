package com.skyline.server.search.impl;

import com.skyline.server.search.RedisIndex;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;

public class RedisCategoricalIndex implements RedisIndex {

    private final static String INDEX_KEY_BASE = "data.redis.index:";
    private final RedisClient redisClient;
    private final String name;

    public RedisCategoricalIndex(String indexName, RedisClient redisClient) {
        this.redisClient = redisClient;
        this.name = INDEX_KEY_BASE + indexName;
    }

    public void update(String key, String val, Handler<AsyncResult<Long>> resultHandler) {
        // Inverted Index: data.redis.index:bedroom:0 -> rentalId
        redisClient.sadd(name + ":" + val, key, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public RedisClient getRedisClient() {
        return this.redisClient;
    }

    public String getName() {
        return this.name;
    }

}
