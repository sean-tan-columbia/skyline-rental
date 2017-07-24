package com.skyline.server.search;

import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;
import io.vertx.core.Future;
import io.vertx.redis.op.AggregateOptions;

import java.util.*;

public class RedisMultiSearch {

    private final String SEARCH_KEY_BASE = "data.redis.search:";
    private RedisSearch[] redisSearches;
    private String name;

    public RedisMultiSearch(String searchName, RedisSearch... redisSearches) {
        this.redisSearches = redisSearches;
        this.name = searchName;
    }

    public RedisMultiSearch(RedisSearch... redisSearches) {
        this.redisSearches = redisSearches;
        this.name = UUID.randomUUID().toString();
    }

    @SuppressWarnings("unchecked")
    public void exec(Handler<AsyncResult<List<String>>> resultHandler) {
        Map<String, Double> weights = new HashMap<>();
        List<Future> results = new ArrayList<>();
        Arrays.stream(redisSearches).forEach(s -> {
            Future<Long> future = Future.future();
            results.add(future);
            s.exec(future.completer());
            weights.put(s.getName(), 1.0);
        });
        RedisClient client = redisSearches[0].getIndex().getRedisClient();
        CompositeFuture.all(results).setHandler(res1 -> {
            if (res1.succeeded()) {
                client.zinterstoreWeighed(SEARCH_KEY_BASE + name, weights, AggregateOptions.SUM, res2 -> {
                    if (res2.succeeded()) {
                        client.zrange(SEARCH_KEY_BASE + name, 0, -1, res3 -> {
                            if (res3.succeeded()) {
                                resultHandler.handle(Future.succeededFuture(res3.result().getList()));
                            } else {
                                resultHandler.handle(Future.failedFuture(res3.cause()));
                            }
                        });
                    } else {
                        resultHandler.handle(Future.failedFuture(res2.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(res1.cause()));
            }
        });
    }

    public void del(Handler<AsyncResult<List<String>>> resultHandler) {
        RedisClient client = redisSearches[0].getIndex().getRedisClient();
        List<Future> results = new ArrayList<>();
        Arrays.stream(redisSearches).forEach(s -> {
            Future<Long> future = Future.future();
            results.add(future);
            s.del(future.completer());
        });
        CompositeFuture.all(results).setHandler(res1 -> {
            if (res1.succeeded()) {
                client.del(SEARCH_KEY_BASE + name, res2 -> {
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

}
