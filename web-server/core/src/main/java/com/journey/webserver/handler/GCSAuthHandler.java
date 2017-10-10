package com.journey.webserver.handler;

import com.google.api.client.auth.oauth2.Credential;
import com.journey.webserver.Config;
import com.journey.webserver.model.GCSAuth;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.RedisClient;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by jtan on 6/29/17.
 */
public class GCSAuthHandler {

    private final static Logger LOG = LoggerFactory.getLogger(GCSAuthHandler.class);
    private static final String GCS_TOKEN_KEY_BASE = "auth.gcs.token:";
    private final String GCS_TOKEN_KEY;
    private final Vertx vertx;
    private final Credential credential;
    private final RedisClient redisClient;
    private final Long accessTokenTTL;

    public GCSAuthHandler(Vertx vertx, RedisClient redisClient, Credential credential, Config config) throws Exception {
        this.vertx = vertx;
        this.redisClient = redisClient;
        this.GCS_TOKEN_KEY = GCS_TOKEN_KEY_BASE + config.getGoogleApiClientId();
        this.accessTokenTTL = config.getGoogleApiTokenTTL(); // seconds
        this.credential = credential;
    }

    private void refreshAccessToken(Handler<AsyncResult<String>> resultHandler) {
        vertx.<Boolean>executeBlocking(future -> {
            Boolean isRefresh = false;
            try {
                isRefresh = credential.refreshToken();
            } catch (IOException e) {
                e.getCause();
            }
            future.complete(isRefresh);
        }, res -> {
            if (res.result()) {
                putAccessToken(r -> {
                    if (r.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(credential.getAccessToken()));
                    } else {
                        resultHandler.handle(Future.failedFuture(r.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
                LOG.error("Failed to refresh GCS Access Token!");
            }
        });
    }

    private void putAccessToken(Handler<AsyncResult<Long>> resultHandler) {
        GCSAuth gcsAuth = new GCSAuth(credential.getAccessToken());
        JsonObject gcsAuthJson = new JsonObject(Json.encode(gcsAuth));
        redisClient.hmset(GCS_TOKEN_KEY, gcsAuthJson, r1 -> {
            if (r1.succeeded()) {
                LOG.info("New GCS Access Token: " + credential.getAccessToken());
                setAccessTokenTTL(r2 -> {
                    if (r2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                });
            } else {
                LOG.error("Failed to insert new token!");
                resultHandler.handle(Future.failedFuture(r1.cause()));
            }
        });
    }

    public void getAccessToken(Handler<AsyncResult<String>> resultHandler) {
        redisClient.exists(GCS_TOKEN_KEY, e -> {
            if (e.succeeded() && e.result() > 0) {
                redisClient.hmget(GCS_TOKEN_KEY, Arrays.asList("accessToken"), r1 -> {
                    if (r1.succeeded()) {
                        String token = r1.result().getString(0);
                        resultHandler.handle(Future.succeededFuture(token));
                    } else {
                        resultHandler.handle(Future.failedFuture(r1.cause()));
                    }
                });
            } else {
                LOG.info("Access token not exist, refresh it");
                refreshAccessToken(r2 -> {
                    if (r2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(r2.result()));
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                });
            }
        });
    }

    private void setAccessTokenTTL(Handler<AsyncResult<Long>> resultHandler) {
        redisClient.expire(GCS_TOKEN_KEY, accessTokenTTL, r -> {
            if (r.succeeded() && r.result() > 0) {
                LOG.info("New GCS Access Token will expire in " + accessTokenTTL.toString() + " secs");
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(r.cause()));
                LOG.error("Failed to set new token TTL!");
            }
        });
    }

}
