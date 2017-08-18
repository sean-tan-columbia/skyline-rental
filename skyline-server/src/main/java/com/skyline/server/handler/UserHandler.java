package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import com.skyline.server.model.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

import java.util.List;

public class UserHandler {

    private final static String SESSION_USERNAME = "username";
    private final UserRedisHandler redisHandler;
    private final UserJdbcHandler jdbcHandler;

    public UserHandler(UserRedisHandler redisHandler, UserJdbcHandler jdbcHandler) {
        this.redisHandler = redisHandler;
        this.jdbcHandler = jdbcHandler;
    }

    public void get(RoutingContext context) {
        Session session = context.session();
        if (session == null) {
            context.response().setStatusCode(202).end("Unauthorized!");
            return;
        }
        if (session.get(SESSION_USERNAME) == null) {
            context.response().setStatusCode(202).end("Unauthorized!");
            return;
        }
        String posterId = session.get(SESSION_USERNAME);
        redisHandler.get(posterId, r1 -> {
            if (r1.succeeded()) {
                context.response().setStatusCode(200).end(Json.encodePrettily(r1.result()));
                return;
            }
            this.get(posterId, r2 -> {
                if (r2.failed()) {
                    context.response().setStatusCode(500).end(r2.cause().getMessage());
                    return;
                }
                User poster = r2.result();
                redisHandler.put(poster, r3 -> {
                    if (r3.succeeded()) {
                        context.response().setStatusCode(200).end(Json.encodePrettily(poster));
                    } else {
                        context.response().setStatusCode(500).end(r3.cause().getMessage());
                    }
                });
            });
        });
    }

    private void get(String posterId, Handler<AsyncResult<User>> resultHandler) {
        jdbcHandler.getUser(posterId, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            User poster = r1.result();
            jdbcHandler.getRental(posterId, r2 -> {
                if (r2.succeeded()) {
                    List<Rental> rentalList = r2.result();
                    poster.setRentals(rentalList);
                    resultHandler.handle(Future.succeededFuture(poster));
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
    }

}
