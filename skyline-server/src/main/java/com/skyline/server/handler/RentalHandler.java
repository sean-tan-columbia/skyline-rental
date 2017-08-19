package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;

import java.util.Date;
import java.util.stream.Collectors;

public class RentalHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RentalHandler.class);
    private final static String SESSION_USERNAME = "username";
    private final RentalRedisHandler redisHandler;
    private final RentalJdbcHandler jdbcHandler;
    private final GCSAuthHandler gcsAuthHandler;

    public RentalHandler(RentalRedisHandler redisHandler, RentalJdbcHandler jdbcHandler, GCSAuthHandler gcsAuthHandler) {
        this.redisHandler = redisHandler;
        this.jdbcHandler = jdbcHandler;
        this.gcsAuthHandler = gcsAuthHandler;
    }

    public void put(RoutingContext context) {
        Session session = context.session();
        if (session == null || session.get(SESSION_USERNAME) == null) {
            context.response().setStatusCode(401).end("User Not Authorized to Create!");
            return;
        }
        JsonObject rentalInfo = context.getBodyAsJson();
        Rental rental;
        try {
            rental = mapToRental(rentalInfo).setPosterId(session.get(SESSION_USERNAME));
        } catch (Exception e) {
            context.response().setStatusCode(202).end(e.getCause().getMessage());
            return;
        }
        this.put(rental, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(500).end(r1.cause().getMessage());
                return;
            }
            gcsAuthHandler.getAccessToken(r2 -> {
                if (r2.succeeded()) {
                    context.response().setStatusCode(201).end(Json.encodePrettily(r2.result()));
                } else {
                    context.response().setStatusCode(500).end(r2.cause().getMessage());
                }
            });
        });
    }

    private void put(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        jdbcHandler.insert(rental, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            redisHandler.put(rental, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
    }

    public void get(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end("Invalid Rental ID!");
            return;
        }
        redisHandler.get(rentalId, r -> {
            if (r.succeeded()) {
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .end(Json.encodePrettily(r.result()));
            } else {
                context.response().setStatusCode(500).end(r.cause().getMessage());
            }
        });
    }

    public void update(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end("Invalid Rental ID!");
            return;
        }
        Session session = context.session();
        if (session == null || session.get(SESSION_USERNAME) == null) {
            context.response().setStatusCode(401).end("User Not Authorized to Update!");
            return;
        }
        String username = session.get(SESSION_USERNAME);
        jdbcHandler.select(rentalId, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(202).end(r1.cause().getMessage());
                return;
            }
            if (username == null || !username.equals(r1.result().getPosterId())) {
                context.response().setStatusCode(202).end("User Not Authorized to Update!");
                return;
            }
            JsonObject rentalInfo = context.getBodyAsJson();
            Rental rental;
            try {
                rental = mapToRental(rentalInfo).setPosterId(username);
            } catch (Exception e) {
                context.response().setStatusCode(202).end(e.getCause().getMessage());
                return;
            }
            this.update(rental, r2 -> {
                if (r2.failed()) {
                    context.response().setStatusCode(500).end(r2.cause().getMessage());
                    return;
                }
                gcsAuthHandler.getAccessToken(r3 -> {
                    if (r3.succeeded()) {
                        context.response().setStatusCode(200).end(Json.encodePrettily(r3.result()));
                    } else {
                        context.response().setStatusCode(500).end(r3.cause().getMessage());
                    }
                });
            });
        });
    }

    private void update(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        jdbcHandler.update(rental, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            redisHandler.put(rental, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
    }

    public void delete(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end("Invalid Rental ID!");
            return;
        }
        Session session = context.session();
        if (session == null || session.get(SESSION_USERNAME) == null) {
            context.response().setStatusCode(401).end("User Not Authorized to Delete!");
            return;
        }
        String username = session.get(SESSION_USERNAME);
        jdbcHandler.select(rentalId, r1 -> {
            if (r1.failed()) {
                context.response().setStatusCode(202).end(r1.cause().getMessage());
                return;
            }
            if (username == null || !username.equals(r1.result().getPosterId())) {
                context.response().setStatusCode(202).end("User Not Authorized to Delete!");
                return;
            }
            this.delete(rentalId, r2 -> {
                if (r2.failed()) {
                    context.response().setStatusCode(500).end(r2.cause().getMessage());
                }
                gcsAuthHandler.getAccessToken(r3 -> {
                    if (r3.succeeded()) {
                        context.response().setStatusCode(200).end(Json.encodePrettily(r3.result()));
                    } else {
                        context.response().setStatusCode(500).end(r3.cause().getMessage());
                    }
                });
            });
        });
    }

    private void delete(String rentalId, Handler<AsyncResult<Long>> resultHandler) {
        jdbcHandler.delete(rentalId, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            redisHandler.delete(rentalId, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
    }

    private Rental mapToRental(JsonObject rentalInfo) {
        Rental rental = new Rental(rentalInfo.getString("id"));
        if (rentalInfo.getLong("move_out_date") != null) {
            rental.setEndDate(new Date(rentalInfo.getLong("move_out_date")));
        }
        return rental.setRentalType(Rental.RentalType.values()[rentalInfo.getInteger("rental_type", 1)])
                .setStartDate(new Date(rentalInfo.getLong("move_in_date")))
                .setPrice(rentalInfo.getDouble("price"))
                .setQuantifier(Rental.Quantifier.values()[rentalInfo.getInteger("quantifier")])
                .setBedroom(Rental.Bedroom.values()[rentalInfo.getInteger("bedroom")])
                .setBathroom(Rental.Bathroom.values()[rentalInfo.getInteger("bathroom")])
                .setLatitude(rentalInfo.getDouble("lat"))
                .setLongitude(rentalInfo.getDouble("lng"))
                .setImageIds(rentalInfo.getJsonArray("image_ids")
                        .stream().map(i -> new JsonObject(i.toString()).getValue("id").toString()).collect(Collectors.toList()))
                .setAddress(rentalInfo.getString("address", ""))
                .setNeighborhood(rentalInfo.getString("neighborhood", ""))
                .setDescription(rentalInfo.getString("description", ""));
    }

    public void search(RoutingContext context) {
        JsonObject searchInfo = context.getBodyAsJson();
        redisHandler.search(searchInfo, r -> {
            if (r.succeeded()) {
                context.response().setStatusCode(200).end(Json.encodePrettily(r.result()));
            } else {
                context.response().setStatusCode(500).end(r.cause().getMessage());
            }
        });
    }

    public void searchLocation(RoutingContext context) {
        JsonObject searchInfo = context.getBodyAsJson();
        redisHandler.searchLocation(searchInfo, r -> {
            if (r.succeeded()) {
                context.response().setStatusCode(200).end(Json.encodePrettily(r.result()));
            } else {
                context.response().setStatusCode(500).end(r.cause().getMessage());
            }
        });
    }

}
