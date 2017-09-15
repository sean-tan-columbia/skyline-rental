package com.skyline.server.handler;

import com.skyline.server.model.User;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

/**
 * Created by jtan on 6/26/17.
 */
public class UserAuthHandler {

    private final static Logger LOG = LoggerFactory.getLogger(UserAuthHandler.class);
    private final JDBCAuth authProvider;
    private final UserAuthJdbcHandler jdbcHandler;
    private final UserAuthRedisHandler redisHandler;

    public UserAuthHandler(UserAuthRedisHandler redisHandler, UserAuthJdbcHandler jdbcHandler, JDBCAuth jdbcAuth) {
        this.redisHandler = redisHandler;
        this.jdbcHandler = jdbcHandler;
        this.authProvider = jdbcAuth;
    }

    public void authenticate(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.fail(400);
            return;
        }
        jdbcHandler.select(credentials.getString("email"), r1 -> {
            String id = r1.result();
            credentials.put("username", id);
            authProvider.authenticate(credentials, login -> {
                if (login.failed()) {
                    LOG.error(login.cause());
                    context.fail(403);
                    return;
                }
                Session session = context.session();
                session.put("username", credentials.getString("username"));
                context.setUser(login.result());
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .setStatusCode(201).end();
            });
        });
    }

    public void register(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        // String salt = authProvider.generateSalt();
        String password = credentials.getString("password");
        String salt = credentials.getString("salt");
        String hash = authProvider.computeHash(password, salt, 1);
        redisHandler.get(salt, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(400).end();
                return;
            }
            User user = r1.result();
            jdbcHandler.insert(user, salt, hash, r2 -> {
                if (r2.succeeded()) {
                    context.response().setStatusCode(201).end();
                } else {
                    context.response().setStatusCode(500).end();
                }
            });
        });
    }

    public void signUp(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        String id = credentials.getString("id");
        String name = credentials.getString("name");
        String email = credentials.getString("email");
        String wechat = credentials.getString("wechat", "");
        jdbcHandler.exists(email, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(500).end();
                return;
            }
            Boolean exists = r1.result();
            if (exists) {
                context.response().setStatusCode(400).end();
                return;
            }
            String salt = authProvider.generateSalt();
            User user = new User(id).setName(name).setEmail(email).setWechatId(wechat);
            redisHandler.put(salt, user, r2 -> {
                if (r2.succeeded()) {
                    context.response().setStatusCode(201).end();
                } else {
                    context.response().setStatusCode(500).end();
                }
            });
        });
    }

}
