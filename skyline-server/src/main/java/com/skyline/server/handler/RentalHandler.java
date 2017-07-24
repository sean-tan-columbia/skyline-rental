package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import com.skyline.server.search.RedisIndex;
import com.skyline.server.search.RedisMultiSearch;
import com.skyline.server.search.RedisSearch;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;

/**
 * Created by jtan on 6/5/17.
 */
public class RentalHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RentalHandler.class);
    private final static String RENTAL_KEY_BASE = "data.core.rental:";
    private final static String NEG_INF = "-inf";
    private final static String POS_INF = "+inf";
    private final RedisIndex moveInDateIndex;
    private final RedisIndex priceIndex;
    private final RedisIndex quantifierIndex;
    private final RedisIndex bedroomIndex;
    private final RedisIndex bathroomIndex;
    private final RedisClient client;

    public RentalHandler(RedisClient client) {
        this.moveInDateIndex = new RedisIndex("moveInDate", client);
        this.priceIndex = new RedisIndex("price", client);
        this.quantifierIndex = new RedisIndex("quantifier", client);
        this.bedroomIndex = new RedisIndex("bedroom", client);
        this.bathroomIndex = new RedisIndex("bathroom", client);
        this.client = client;
    }

    public void put(RoutingContext context) {
        JsonObject rentalInfo = context.getBodyAsJson();
        if (!rentalInfo.containsKey("posterId")) {
            LOG.error("PosterId not provided.");
            return;
        }
        try {
            Rental rental = mapToRental(rentalInfo);
            JsonObject rentalJson = new JsonObject(Json.encode(rental));
            client.hmset(RENTAL_KEY_BASE + rental.getId(), rentalJson, r -> {
                if (r.succeeded()) {
                    updateIndex(rental, res -> {
                        context.response().setStatusCode(201).end();
                        LOG.info("Inserted Rental " + rentalJson.getString("id"));
                    });
                } else {
                    context.response().setStatusCode(500).end();
                    LOG.error("Failed to insert Rental " + rentalJson.getString("id"));
                }
            });
        } catch (Exception e) {
            LOG.error(e);
        }
    }

    private Rental mapToRental(JsonObject rentalInfo) throws ParseException {
        return new Rental(rentalInfo.getString("id"), rentalInfo.getString("posterId"))
            .setRentalType(Rental.RentalType.valueOf(rentalInfo.getString("rentalType", "")))
            .setAddress(rentalInfo.getString("address", ""))
            .setNeighborhood(rentalInfo.getString("neighborhood", ""))
            .setPrice(rentalInfo.getDouble("price", 0.0))
            .setQuantifier(Rental.Quantifier.valueOf(rentalInfo.getString("quantifier", "")))
            .setDescription(rentalInfo.getString("description"))
            .setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(rentalInfo.getString("startDate", "")))
            .setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(rentalInfo.getString("endDate", "")))
            .setLatitude(rentalInfo.getDouble("lat", 0.0))
            .setLongitude(rentalInfo.getDouble("lng", 0.0))
            .setImageIds(rentalInfo.getJsonArray("imageIds").stream()
                    .map(i -> new JsonObject(i.toString()).getValue("id").toString())
                    .collect(Collectors.toList()));
    }

    private void updateIndex(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        Future<Long> moveInDateFuture = Future.future();
        this.moveInDateIndex.update(rental.getId(), (double) rental.getStartDate().getTime(), moveInDateFuture.completer());
        Future<Long> priceFuture = Future.future();
        this.priceIndex.update(rental.getId(), rental.getPrice(), priceFuture.completer());
        Future<Long> quantifierFuture = Future.future();
        this.quantifierIndex.update(rental.getId(), (double) rental.getQuantifier().getVal(), quantifierFuture.completer());
        Future<Long> bedroomFuture = Future.future();
        this.bedroomIndex.update(rental.getId(), (double) rental.getBedroom().getVal(), bedroomFuture.completer());
        Future<Long> bathroomFuture = Future.future();
        this.bathroomIndex.update(rental.getId(), (double) rental.getBathroom().getVal(), bathroomFuture.completer());
        CompositeFuture.all(moveInDateFuture, priceFuture, quantifierFuture, bedroomFuture, bathroomFuture).setHandler(res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void get(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        client.hgetall(RENTAL_KEY_BASE + rentalId, r -> {
            if (r.succeeded()) {
                LOG.info("Received Rental " + rentalId);
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .end(Json.encodePrettily(r.result()));
            } else {
                LOG.error("Failed to retrieve " + rentalId);
                context.response().setStatusCode(500).end();
            }
        });
    }

    public void getMax(RoutingContext context) {
        client.keys(RENTAL_KEY_BASE + "*", r -> {
            if (r.succeeded()) {
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .end(Json.encodePrettily(r.result().stream()
                                .map(id -> id.toString().split(":")[1])
                                .collect(Collectors.toList())));
            } else {
                context.response().setStatusCode(500).end();
            }
        });
    }

    public void search(RoutingContext context) {
        JsonObject searchInfo = context.getBodyAsJson();

        RedisSearch moveInDateSearch = new RedisSearch(moveInDateIndex,
                searchInfo.getString("move_in_date_min", NEG_INF),
                searchInfo.getString("move_in_date_max", POS_INF));
        RedisSearch priceSearch = new RedisSearch(priceIndex,
                searchInfo.getString("price_min", NEG_INF),
                searchInfo.getString("price_max", POS_INF));
        RedisSearch quantifierSearch = new RedisSearch(quantifierIndex,
                searchInfo.getString("quantifier", NEG_INF),
                searchInfo.getString("quantifier", POS_INF));
        RedisSearch bedroomSearch = new RedisSearch(bedroomIndex,
                searchInfo.getString("bedroom", NEG_INF),
                searchInfo.getString("bedroom", POS_INF));
        RedisSearch bathroomSearch = new RedisSearch(bathroomIndex,
                searchInfo.getString("bathroom", NEG_INF),
                searchInfo.getString("bathroom", POS_INF));

        RedisMultiSearch redisMultiSearch =
                new RedisMultiSearch(moveInDateSearch, priceSearch, quantifierSearch, bedroomSearch, bathroomSearch);
        redisMultiSearch.exec(res1 -> {
            if (res1.succeeded()) {
                context.response().setStatusCode(200).end(Json.encodePrettily(res1.result()));
                redisMultiSearch.del(res2 -> {
                    if (res2.failed()) {
                        LOG.error(res2.cause().getMessage());
                    }
                });
            } else {
                context.response().setStatusCode(500).end();
            }
        });
    }

}
