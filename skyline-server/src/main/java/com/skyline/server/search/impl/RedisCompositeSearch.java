package com.skyline.server.search.impl;

import com.skyline.server.search.RedisIndex;
import com.skyline.server.search.RedisSearch;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.op.AggregateOptions;

import java.util.*;

public class RedisCompositeSearch implements RedisSearch {

    private final String SEARCH_KEY_BASE = "data.redis.search:";
    private final RedisSearch[] redisSearches;
    private final String name;

    public RedisCompositeSearch(String searchName, RedisSearch... redisSearches) {
        this.redisSearches = redisSearches;
        this.name = SEARCH_KEY_BASE + searchName;
    }

    public RedisCompositeSearch(RedisSearch... redisSearches) {
        this.redisSearches = redisSearches;
        this.name = SEARCH_KEY_BASE + UUID.randomUUID().toString();
    }

    @SuppressWarnings("unchecked")
    public void exec(Handler<AsyncResult<Long>> resultHandler) {
        Map<String, Double> weights = new HashMap<>();
        List<Future> results = new ArrayList<>();
        Arrays.stream(redisSearches).forEach(search -> {
            Future<Long> future = Future.future();
            results.add(future);
            search.exec(future.completer());
            if (search.getName() != null) {
                weights.put(search.getName(), 1.0);
            }
        });
        RedisClient client = redisSearches[0].getIndex().getRedisClient();
        CompositeFuture.all(results).setHandler(res1 -> {
            if (res1.failed()) {
                resultHandler.handle(Future.failedFuture(res1.cause()));
                return;
            }
            client.zinterstoreWeighed(name, weights, AggregateOptions.SUM, res2 -> {
                if (res2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(res2.result()));
                } else {
                    resultHandler.handle(Future.failedFuture(res2.cause()));
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    public void get(Handler<AsyncResult<List<String>>> resultHandler) {
        RedisClient redisClient = redisSearches[0].getIndex().getRedisClient();
        redisClient.zrange(name, 0, -1, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().getList()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void del(Handler<AsyncResult<Long>> resultHandler) {
        RedisClient client = redisSearches[0].getIndex().getRedisClient();
        List<Future> results = new ArrayList<>();
        Arrays.stream(redisSearches).forEach(s -> {
            Future<Long> future = Future.future();
            results.add(future);
            s.del(future.completer());
        });
        CompositeFuture.all(results).setHandler(res1 -> {
            if (res1.succeeded()) {
                client.del(name, res2 -> {
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

    public String getName() {
        return this.name;
    }

    public RedisIndex getIndex() {
        return null;
    }

}
