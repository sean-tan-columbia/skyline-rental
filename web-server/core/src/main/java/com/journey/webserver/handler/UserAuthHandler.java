package com.journey.webserver.handler;

import com.journey.webserver.model.User;
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
    private final GMailServiceHandler mailServiceHandler;

    public UserAuthHandler(UserAuthRedisHandler redisHandler,
                           UserAuthJdbcHandler jdbcHandler,
                           GMailServiceHandler mailServiceHandler,
                           JDBCAuth jdbcAuth) {
        this.redisHandler = redisHandler;
        this.jdbcHandler = jdbcHandler;
        this.authProvider = jdbcAuth;
        this.mailServiceHandler = mailServiceHandler;
    }

    public void authenticate(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        jdbcHandler.select(credentials.getString("email"), r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(404).end();
            }
            String id = r1.result().getId();
            credentials.put("username", id);
            authProvider.authenticate(credentials, login -> {
                if (login.failed()) {
                    LOG.error(login.cause());
                    context.fail(403);
                    return;
                }
                Session session = context.session();
                session.put("username", credentials.getString("username")); // username == id here.
                context.setUser(login.result());
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .setStatusCode(201).end();
            });
        });
    }

    public void setPassword(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        String type = credentials.getString("type");
        String password_1 = credentials.getString("password1");
        String password_2 = credentials.getString("password2");
        if (!password_1.equals(password_2)) {
            context.response().setStatusCode(400).end();
        }
        String salt = credentials.getString("salt");
        String hash = authProvider.computeHash(password_1, salt, 1);
        redisHandler.get(salt, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(404).end();
                return;
            }
            User user = r1.result();
            if (type.equals("c")) {
                jdbcHandler.insert(user, salt, hash, r3 -> {
                    if (r3.succeeded()) {
                        context.response().setStatusCode(201)
                                .putHeader("Access-Control-Allow-Origin", "*").end();
                    } else {
                        context.response().setStatusCode(500)
                                .putHeader("Access-Control-Allow-Origin", "*").end(r3.cause().getMessage());
                    }
                });
            } else if (type.equals("u")) {
                jdbcHandler.update(user.getId(), salt, hash, r4 -> {
                    if (r4.succeeded()) {
                        context.response().setStatusCode(200)
                                .putHeader("Access-Control-Allow-Origin", "*").end();
                    } else {
                        context.response().setStatusCode(500)
                                .putHeader("Access-Control-Allow-Origin", "*").end(r4.cause().getMessage());
                    }
                });
            } else {
                context.response().setStatusCode(400).putHeader("Access-Control-Allow-Origin", "*").end();
            }
        });
    }

    public void verify(RoutingContext context) {
        String salt = context.request().getParam("salt");
        if (salt == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        redisHandler.get(salt, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(404).end();
            }
            // Extend the salt;
            redisHandler.put(salt, r1.result(), r2 -> {
                if (r1.succeeded()) {
                    context.response().setStatusCode(200)
                            .putHeader("Access-Control-Allow-Origin", "*").end();
                } else {
                    context.response().setStatusCode(500)
                            .putHeader("Access-Control-Allow-Origin", "*").end();
                }
            });
        });
    }

    public void reset(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        String email = credentials.getString("email");
        jdbcHandler.select(email, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(404).end();
                return;
            }
            User user = r1.result();
            String salt = authProvider.generateSalt();
            redisHandler.put(salt, user, r2 -> {
                if (r2.failed()) {
                    context.response().setStatusCode(500).end();
                    return;
                }
                mailServiceHandler.sendResetEmail(email, user, salt, r3 -> {
                    if (r3.succeeded()) {
                        context.response().setStatusCode(201)
                                .putHeader("Access-Control-Allow-Origin", "*").end();
                    } else {
                        context.response().setStatusCode(500)
                                .putHeader("Access-Control-Allow-Origin", "*").end();
                    }
                });
            });
        });
    }

    public void register(RoutingContext context) {
        JsonObject credentials = context.getBodyAsJson();
        if (credentials == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        String id = credentials.getString("id");
        String name = credentials.getString("name");
        String email = credentials.getString("email");
        String phone = credentials.getString("phone", "");
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
            User user = new User(id).setName(name).setEmail(email).setPhone(phone).setWechatId(wechat);
            redisHandler.put(salt, user, r2 -> {
                if (r2.failed()) {
                    context.response().setStatusCode(500).end();
                    return;
                }
                mailServiceHandler.sendRegisterMail(email, user, salt, r3 -> {
                    if (r3.succeeded()) {
                        context.response().setStatusCode(201)
                                .putHeader("Access-Control-Allow-Origin", "*").end();
                    } else {
                        context.response().setStatusCode(500)
                                .putHeader("Access-Control-Allow-Origin", "*").end();
                    }
                });
            });
        });
    }

}
