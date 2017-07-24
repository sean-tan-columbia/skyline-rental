package com.skyline.server.search;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.AggregateOptions;

import java.util.*;

public class RedisSearch {

    private final static Logger LOG = LoggerFactory.getLogger(RedisSearch.class);
    private final String SEARCH_KEY_BASE = "data.redis.search:";
    private final String name;
    private final String min;
    private final String max;
    private final RedisIndex index;

    public RedisSearch(String searchName, RedisIndex redisIndex, String min, String max) {
        this.name = searchName;
        this.index = redisIndex;
        this.min = min;
        this.max = max;
    }

    public RedisSearch(RedisIndex redisIndex, String min, String max) {
        this.name = SEARCH_KEY_BASE + UUID.randomUUID().toString();
        this.index = redisIndex;
        this.min = min;
        this.max = max;
    }

    public void exec(Handler<AsyncResult<Long>> resultHandler) {
        RedisClient redisClient = index.getRedisClient();
        Map<String, Double> weights = new HashMap<>();
        weights.put(index.getName(), 1.0);
        redisClient.zinterstoreWeighed(name, weights, AggregateOptions.SUM, (AsyncResult<Long> res1) -> {
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

    public RedisIndex getIndex() {
        return this.index;
    }

    public String getName() {
        return this.name;
    }

}
