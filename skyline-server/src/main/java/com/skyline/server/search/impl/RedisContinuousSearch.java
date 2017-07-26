package com.skyline.server.search.impl;

import com.skyline.server.search.RedisSearch;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.AggregateOptions;

import java.util.*;

public class RedisContinuousSearch implements RedisSearch {

    private final String SEARCH_KEY_BASE = "data.redis.search:";
    private final String name;
    private final String min;
    private final String max;
    private final RedisContinuousIndex index;

    public RedisContinuousSearch(String searchName, RedisContinuousIndex redisContinuousIndex, String min, String max) {
        this.name = SEARCH_KEY_BASE + searchName;
        this.index = redisContinuousIndex;
        this.min = min;
        this.max = max;
    }

    public RedisContinuousSearch(RedisContinuousIndex redisContinuousIndex, String min, String max) {
        this.name = SEARCH_KEY_BASE + UUID.randomUUID().toString();
        this.index = redisContinuousIndex;
        this.min = min;
        this.max = max;
    }

    public void exec(Handler<AsyncResult<Long>> resultHandler) {
        RedisClient redisClient = index.getRedisClient();
        Map<String, Double> weights = new HashMap<>();
        weights.put(index.getName(), 1.0);
        redisClient.zunionstoreWeighed(name, weights, AggregateOptions.SUM, (AsyncResult<Long> res1) -> {
            if (res1.succeeded()) {
                Future<Long> leftRangeFuture = Future.future();
                redisClient.zremrangebyscore(name, "-inf", "(" + min, leftRangeFuture.completer());
                Future<Long> rightRangeFuture = Future.future();
                redisClient.zremrangebyscore(name, "(" + max, "+inf", rightRangeFuture.completer());
                CompositeFuture.all(leftRangeFuture, rightRangeFuture).setHandler(res2 -> {
                    if (res2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(res2.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(res1.cause()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void get(Handler<AsyncResult<List<String>>> resultHandler) {
        RedisClient redisClient = index.getRedisClient();
        redisClient.zrange(name, 0, -1, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().getList()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void del(Handler<AsyncResult<Long>> resultHandler) {
        RedisClient redisClient = index.getRedisClient();
        redisClient.del(name, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public RedisContinuousIndex getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

}
