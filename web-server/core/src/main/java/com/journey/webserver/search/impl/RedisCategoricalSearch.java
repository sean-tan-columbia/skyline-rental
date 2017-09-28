package com.journey.webserver.search.impl;

import com.journey.webserver.search.RedisIndex;
import com.journey.webserver.search.RedisSearch;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.AggregateOptions;
import io.vertx.redis.op.RangeOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RedisCategoricalSearch implements RedisSearch {

    private final String SEARCH_KEY_BASE = "data.redis.search:";
    private final String name;
    private final List<String> values;
    private final RedisCategoricalIndex index;

    public RedisCategoricalSearch(String searchName, RedisCategoricalIndex redisCategoricalIndex, List<String> values) {
        this.name = values.size() > 0 ? (SEARCH_KEY_BASE + searchName) : null;
        this.index = redisCategoricalIndex;
        this.values = values;
    }

    public RedisCategoricalSearch(RedisCategoricalIndex redisCategoricalIndex, List<String> values) {
        this.name = values.size() > 0 ? (SEARCH_KEY_BASE + UUID.randomUUID().toString()) : null;
        this.index = redisCategoricalIndex;
        this.values = values;
    }

    public void exec(Handler<AsyncResult<Long>> resultHandler) {
        if (values.size() == 0) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }
        RedisClient redisClient = index.getRedisClient();
        Map<String, Double> weights = new HashMap<>();
        values.forEach(v -> weights.put(index.getName() + ":" + v, 1d));
        redisClient.zunionstoreWeighed(name, weights, AggregateOptions.SUM, (AsyncResult<Long> res) -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                System.out.println(res.cause().getMessage());
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    @SuppressWarnings("unchecked")
    public void get(Boolean order, Handler<AsyncResult<List<String>>> resultHandler) {
        RedisClient redisClient = index.getRedisClient();
        if (order) {
            redisClient.zrange(name, 0, -1, res -> {
                if (res.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(res.result().getList()));
                } else {
                    resultHandler.handle(Future.failedFuture(res.cause()));
                }
            });
        } else {
            redisClient.zrevrange(name, 0, -1, RangeOptions.NONE, res -> {
                if (res.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(res.result().getList()));
                } else {
                    resultHandler.handle(Future.failedFuture(res.cause()));
                }
            });
        }
    }

    public void del(Handler<AsyncResult<Long>> resultHandler) {
        if (values.size() == 0) {
            resultHandler.handle(Future.succeededFuture());
            return;
        }
        RedisClient redisClient = index.getRedisClient();
        redisClient.del(name, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public String getName() {
        return this.name;
    }

    public RedisIndex getIndex() {
        return this.index;
    }

}
