package com.journey.webserver.search;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.List;

public interface RedisSearch {

    void exec(Handler<AsyncResult<Long>> resultHandler);

    void del(Handler<AsyncResult<Long>> resultHandler);

    void get(Boolean order, Handler<AsyncResult<List<String>>> resultHandler);

    String getName();

    RedisIndex getIndex();

}
