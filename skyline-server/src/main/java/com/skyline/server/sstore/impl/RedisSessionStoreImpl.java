package com.skyline.server.sstore.impl;

import com.skyline.server.sstore.RedisSessionStore;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.auth.PRNG;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.sstore.impl.SessionImpl;
import io.vertx.redis.RedisClient;
import io.vertx.core.buffer.Buffer;

import java.util.stream.Collectors;

/**
 * Created by jtan on 7/17/17.
 */
public class RedisSessionStoreImpl implements RedisSessionStore {
    private final static String SESSION_KEY_BASE = "data.web.session:";
    private final Vertx vertx;
    private final RedisClient redisClient;
    private final PRNG random;
    private final String sessionMapName;
    private final long retryTimeout;

    public RedisSessionStoreImpl(Vertx vertx, RedisClient redisClient, String sessionMapName, long retryTimeout) {
        this.vertx = vertx;
        this.redisClient = redisClient;
        this.sessionMapName = sessionMapName;
        this.retryTimeout = retryTimeout;
        this.random = new PRNG(vertx);
    }

    public long retryTimeout() {
        return this.retryTimeout;
    }

    public Session createSession(long timeout) {
        return new SessionImpl(this.random, timeout, 16);
    }

    public Session createSession(long timeout, int length) {
        return new SessionImpl(this.random, timeout, length);
    }

    public void get(String id, Handler<AsyncResult<Session>> resultHandler) {
        redisClient.getBinary(SESSION_KEY_BASE + id, res -> {
            if (res.succeeded()) {
                SessionImpl session = new SessionImpl(this.random, 1800000L, 16);
                Buffer buff = res.result();
                if (buff != null) {
                    session.readFromBuffer(0, buff);
                }
                resultHandler.handle(Future.succeededFuture(session));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {
        redisClient.del(SESSION_KEY_BASE + id, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(Boolean.valueOf(res.result() != null)));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {
        redisClient.getBinary(SESSION_KEY_BASE + session.id(), (old) -> {
            SessionImpl newSession = (SessionImpl) session;
            SessionImpl oldSession;
            if (old.succeeded() && old.result() != null) {
                Buffer buff = old.result();
                oldSession = new SessionImpl(this.random, 1800000L, 16);
                oldSession.readFromBuffer(0, buff);
            } else {
                oldSession = null;
            }

            if (oldSession != null && oldSession.version() != newSession.version()) {
                resultHandler.handle(Future.failedFuture("Version mismatch"));
            } else {
                newSession.incrementVersion();
                Buffer buff = Buffer.buffer();
                ((SessionImpl) session).writeToBuffer(buff);
                redisClient.setBinary(SESSION_KEY_BASE + session.id(), buff, res -> {
                    if (res.succeeded()) {
                        redisClient.expire(SESSION_KEY_BASE + session.id(), session.timeout(), res2 -> {
                            resultHandler.handle(Future.succeededFuture(Boolean.valueOf(res2.result() != null)));
                        });
                    } else {
                        resultHandler.handle(Future.failedFuture(res.cause()));
                    }
                });
            }
        });
    }

    public void clear(Handler<AsyncResult<Boolean>> resultHandler) {
        redisClient.keys(SESSION_KEY_BASE + "*", res -> {
            if (res.succeeded()) {
                redisClient.delMany(res.result().stream().map(Object::toString).collect(Collectors.toList()), res2 -> {
                    if (res2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(Boolean.valueOf(res2.result() != null)));
                    } else {
                        resultHandler.handle(Future.failedFuture(res2.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void size(Handler<AsyncResult<Integer>> resultHandler) {
        redisClient.keys(SESSION_KEY_BASE + "*", res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().size()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void close() {
        this.random.close();
    }

}
