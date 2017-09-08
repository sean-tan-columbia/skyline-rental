package com.skyline.server.handler;

import com.skyline.server.model.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.redis.RedisClient;

public class UserRedisHandler {

    private final static String USER_KEY_BASE = "data.temp.user:";
    private final static String RENTAL_KEY_BASE = "data.temp.user:";
    private final Long ttl = 600L;
    private final RedisClient client;

    public UserRedisHandler(RedisClient redisClient) {
        this.client = redisClient;
    }

    void put(User poster, Handler<AsyncResult<Long>> resultHandler) {
        if (poster == null) {
            resultHandler.handle(Future.failedFuture("Invalid input user!"));
            return;
        }
        Buffer buff = Buffer.buffer();
        poster.writeToBuffer(buff);
        client.setBinary(USER_KEY_BASE + poster.getId(), buff, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            client.expire(USER_KEY_BASE + poster.getId(), ttl, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
    }

    void get(String posterId, Handler<AsyncResult<User>> resultHandler) {
        if (posterId == null) {
            resultHandler.handle(Future.failedFuture("Invalid User ID!"));
            return;
        }
        client.getBinary(USER_KEY_BASE + posterId, r -> {
            if (r.succeeded()) {
                Buffer buff = r.result();
                if (buff == null) {
                    resultHandler.handle(Future.failedFuture("User Not Exists!"));
                    return;
                }
                User poster = new User();
                poster.readFromBuffer(0, buff);
                resultHandler.handle(Future.succeededFuture(poster));
            } else {
                resultHandler.handle(Future.failedFuture(r.cause()));
            }
        });
    }

    void del(String posterId, Handler<AsyncResult<Long>> resultHandler) {
        if (posterId == null) {
            resultHandler.handle(Future.failedFuture("Invalid User ID!"));
            return;
        }
        client.del(USER_KEY_BASE + posterId, r -> {
            if (r.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(r.cause()));
            }
        });
    }

}
