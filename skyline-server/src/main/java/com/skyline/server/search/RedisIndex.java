package com.skyline.server.search;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.redis.RedisClient;

public interface RedisIndex {

    void add(String key, String val, Handler<AsyncResult<Long>> resultHandler);

    void del(String key, Handler<AsyncResult<Long>> resultHandler);

    RedisClient getRedisClient();

    String getName();

}
