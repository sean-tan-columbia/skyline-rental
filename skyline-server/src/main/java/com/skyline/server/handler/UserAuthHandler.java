package com.skyline.server.handler;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by jtan on 6/26/17.
 */
public class UserAuthHandler {

    private final static Logger LOG = LoggerFactory.getLogger(UserAuthHandler.class);
    private final JDBCAuth authProvider;
    private final JDBCClient client;

    public UserAuthHandler(JDBCAuth jdbcAuth, JDBCClient jdbcClient) {
        this.authProvider = jdbcAuth;
        this.client = jdbcClient;
    }

    public void authenticate(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.fail(400);
            return;
        }
        authProvider.authenticate(credentials, login -> {
            if (login.failed()) {
                LOG.info(credentials.getString("username") + " fail!");
                context.fail(403);
                return;
            }
            LOG.info(credentials.getString("username") + " login!");
            context.setUser(login.result());
            context.response()
                    .putHeader("content-type", "application/json")
                    .putHeader("Access-Control-Allow-Origin", "*")
                    .setStatusCode(201).end();
        });
    }

    public void register(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        client.getConnection(r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(403).end();
                return;
            }
            String username = credentials.getString("username");
            String password = credentials.getString("password");
            String nickname = credentials.getString("nickname", "");
            String email = credentials.getString("email");
            String salt = authProvider.generateSalt();
            String hash = authProvider.computeHash(password, salt, 1);
            SQLConnection connection = r1.result();
            connection.updateWithParams("INSERT INTO eventbus.USERS(ID,NAME,EMAIL,PASSWORD,PASSWORD_SALT) VALUES (?,?,?,?,?)",
                    new JsonArray().add(username).add(nickname).add(email).add(hash).add(salt), r2 -> {
                if (r2.succeeded()) {
                    LOG.info("Success registering new user: " + username);
                    context.response().setStatusCode(201).end();
                } else {
                    LOG.error("Error registering new user: " + r2.cause());
                    context.response().setStatusCode(500).end();
                }
            });
        });
    }

}
