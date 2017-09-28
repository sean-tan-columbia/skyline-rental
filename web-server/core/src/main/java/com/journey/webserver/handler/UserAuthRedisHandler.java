package com.journey.webserver.handler;

import com.journey.webserver.model.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.RedisClient;

public class UserAuthRedisHandler {

    private final static Logger LOG = LoggerFactory.getLogger(UserAuthRedisHandler.class);
    private final static String USER_KEY_BASE = "auth.user.salt:";
    private final RedisClient client;

    public UserAuthRedisHandler(RedisClient client) {
        this.client = client;
    }

    public void put(String salt, User user, Handler<AsyncResult<Long>> resultHandler) {
        Buffer buff = Buffer.buffer();
        user.writeToBuffer(buff);
        client.setBinary(USER_KEY_BASE + salt, buff, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            client.expire(USER_KEY_BASE + salt, 3600, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
    }

    public void get(String salt, Handler<AsyncResult<User>> resultHandler) {
        client.getBinary(USER_KEY_BASE + salt, r -> {
            if (r.succeeded()) {
                Buffer buff = r.result();
                if (buff == null) {
                    resultHandler.handle(Future.failedFuture("User Not Exists!"));
                    return;
                }
                User user = new User();
                user.readFromBuffer(0, buff);
                resultHandler.handle(Future.succeededFuture(user));
            } else {
                resultHandler.handle(Future.failedFuture(r.cause()));
            }
        });
    }

}
