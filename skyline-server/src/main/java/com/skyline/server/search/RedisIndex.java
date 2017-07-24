package com.skyline.server.search;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.RangeLimitOptions;

import java.util.List;

public class RedisIndex {

    private final static String INDEX_KEY_BASE = "data.redis.index:";
    private final RedisClient redisClient;
    private final String name;

    public RedisIndex(String indexName, RedisClient redisClient) {
        this.redisClient = redisClient;
        this.name = INDEX_KEY_BASE + indexName;
    }

    public void update(String key, Double val, Handler<AsyncResult<Long>> resultHandler) {
        redisClient.zadd(name, val, key, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void query(String min, String max, Handler<AsyncResult<List<String>>> resultHandler) {
        redisClient.zrangebyscore(name, min, max, new RangeLimitOptions(), res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().getList()));
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
