package com.journey.webserver.search.impl;

import com.journey.webserver.search.RedisIndex;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;

import java.util.ArrayList;
import java.util.List;

public class RedisCategoricalIndex implements RedisIndex {

    private final static String INDEX_KEY_BASE = "data.redis.index:";
    private final RedisClient redisClient;
    private final String name;

    public RedisCategoricalIndex(String indexName, RedisClient redisClient) {
        this.redisClient = redisClient;
        this.name = INDEX_KEY_BASE + indexName;
    }

    public void add(String key, String val, Handler<AsyncResult<Long>> resultHandler) {
        // Inverted Index: data.redis.index:bedroom:0 -> rentalId
        redisClient.sadd(name + ":" + val, key, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void del(String key, Handler<AsyncResult<Long>> resultHandler) {
        redisClient.keys(name + ":*", r1 -> {
            if (r1.succeeded()) {
                List<Future> results = new ArrayList<>();
                r1.result().getList().forEach(s -> {
                    Future<Long> future = Future.future();
                    redisClient.srem(s.toString(), key, future.completer());
                    results.add(future);
                });
                CompositeFuture.all(results).setHandler(r2 -> {
                    if (r2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(r1.cause()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void query(String val, Handler<AsyncResult<List<String>>> resultHandler) {
        redisClient.smembers(name + ":" + val, r -> {
            if (r.succeeded()) {
                resultHandler.handle(Future.succeededFuture(r.result().getList()));
            } else {
                resultHandler.handle(Future.failedFuture(r.cause()));
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
