package com.skyline.server.handler;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.collect.Lists;
import com.skyline.server.Config;
import com.skyline.server.model.GCSAuth;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by jtan on 6/29/17.
 */
public class GCSAuthHandler {

    private final static Logger LOG = LoggerFactory.getLogger(GCSAuthHandler.class);
    private static final String USER_ID = "sysops";
    private static final String GCS_TOKEN_KEY_BASE = "auth.gcs.token:";
    private final String GCS_TOKEN_KEY;
    private final Vertx vertx;
    private final Credential credential;
    private final RedisClient redisClient;
    private final Long accessTokenTTL;

    public GCSAuthHandler(Vertx vertx, RedisClient redisClient, Config config) throws Exception {
        this.vertx = vertx;
        this.redisClient = redisClient;
        GoogleAuthorizationCodeFlow authorizationFlow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                config.getGoogleApiClientId(),
                config.getGoogleApiClientSecret(),
                Lists.newArrayList(config.getGoogleApiScope()))
                .setDataStoreFactory(new FileDataStoreFactory(new File(config.getGoogleApiCredPath())))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        this.GCS_TOKEN_KEY = GCS_TOKEN_KEY_BASE + config.getGoogleApiClientId();
        this.accessTokenTTL = config.getGoogleApiTokenTTL(); // seconds
        this.credential = authorizationFlow.loadCredential(USER_ID);
        if (this.credential == null) {
            throw new Exception("Failed to load credential!");
        }
    }

    private void refreshAccessToken(RoutingContext context) {
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
                putAccessToken(context);
            } else {
                LOG.error("Failed to refresh GCS Access Token!");
            }
        });
    }

    private void putAccessToken(RoutingContext context) {
        GCSAuth gcsAuth = new GCSAuth(credential.getAccessToken());
        JsonObject gcsAuthJson = new JsonObject(Json.encode(gcsAuth));
        redisClient.hmset(GCS_TOKEN_KEY, gcsAuthJson, r -> {
            if (r.succeeded()) {
                LOG.info("New GCS Access Token: " + credential.getAccessToken());
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .end(Json.encodePrettily(credential.getAccessToken()));
                setAccessTokenTTL();
            } else {
                LOG.error("Failed to insert new token!");
            }
        });
    }

    public void getAccessToken(RoutingContext context) {
        redisClient.exists(GCS_TOKEN_KEY, e -> {
            if (e.succeeded() && e.result() > 0) {
                System.out.println("Access token exist, return it");
                redisClient.hmget(GCS_TOKEN_KEY, Arrays.asList("accessToken"), r -> {
                    context.response()
                            .putHeader("content-type", "application/json")
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .end(Json.encodePrettily(r.result().getString(0)));
                });
            } else {
                System.out.println("Access token not exist, refresh it");
                refreshAccessToken(context);
            }
        });
    }

    private void setAccessTokenTTL() {
        redisClient.expire(GCS_TOKEN_KEY, accessTokenTTL, r -> {
            if (r.succeeded() && r.result() > 0) {
                LOG.info("New GCS Access Token will expire in " + accessTokenTTL.toString() + " secs");
            } else {
                LOG.error("Failed to set new token TTL!");
            }
        });
    }


}
