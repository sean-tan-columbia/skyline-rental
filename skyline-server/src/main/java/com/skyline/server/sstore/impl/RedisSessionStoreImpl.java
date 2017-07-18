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
    private final RedisClient redisClient;
    private final PRNG random;
    private final String sessionKeyBase;
    private final long retryTimeout;

    public RedisSessionStoreImpl(Vertx vertx, RedisClient redisClient, String sessionKeyBase, long retryTimeout) {
        this.redisClient = redisClient;
        this.sessionKeyBase = sessionKeyBase;
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
        redisClient.getBinary(sessionKeyBase + id, res -> {
            if (res.succeeded()) {
                Buffer buff = res.result();
                if (buff != null) {
                    SessionImpl session = new SessionImpl(this.random);
                    session.readFromBuffer(0, buff);
                    resultHandler.handle(Future.succeededFuture(session));
                } else {
                    resultHandler.handle(Future.succeededFuture(null));
                }
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void delete(String id, Handler<AsyncResult<Boolean>> resultHandler) {
        redisClient.del(sessionKeyBase + id, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(Boolean.valueOf(res.result() != null)));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void put(Session session, Handler<AsyncResult<Boolean>> resultHandler) {
        redisClient.getBinary(sessionKeyBase + session.id(), (old) -> {
            SessionImpl newSession = (SessionImpl) session;
            SessionImpl oldSession;
            if (old.succeeded() && old.result() != null) {
                Buffer buff = old.result();
                oldSession = new SessionImpl(this.random);
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
                redisClient.setBinary(sessionKeyBase + session.id(), buff, res -> {
                    if (res.succeeded()) {
                        redisClient.expire(sessionKeyBase + session.id(), session.timeout(), res2 -> {
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
        redisClient.keys(sessionKeyBase + "*", res -> {
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
        redisClient.keys(sessionKeyBase + "*", res -> {
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
