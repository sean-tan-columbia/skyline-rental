package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import com.skyline.server.search.RedisSearch;
import com.skyline.server.search.impl.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
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
    private final RedisContinuousIndex moveInDateIndex;
    private final RedisContinuousIndex priceIndex;
    private final RedisCategoricalIndex quantifierIndex;
    private final RedisCategoricalIndex bedroomIndex;
    private final RedisCategoricalIndex bathroomIndex;
    private final RedisClient client;

    public RentalHandler(RedisClient client) {
        this.moveInDateIndex = new RedisContinuousIndex("moveInDate", client);
        this.priceIndex = new RedisContinuousIndex("price", client);
        this.quantifierIndex = new RedisCategoricalIndex("quantifier", client);
        this.bedroomIndex = new RedisCategoricalIndex("bedroom", client);
        this.bathroomIndex = new RedisCategoricalIndex("bathroom", client);
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
        Future<Long> priceFuture = Future.future();
        Future<Long> quantifierFuture = Future.future();
        Future<Long> bedroomFuture = Future.future();
        Future<Long> bathroomFuture = Future.future();

        this.moveInDateIndex.update(rental.getId(), String.valueOf(rental.getStartDate().getTime()), moveInDateFuture.completer());
        this.priceIndex.update(rental.getId(), String.valueOf(rental.getPrice()), priceFuture.completer());
        this.quantifierIndex.update(rental.getId(), String.valueOf(rental.getQuantifier().getVal()), quantifierFuture.completer());
        this.bedroomIndex.update(rental.getId(), String.valueOf(rental.getBedroom().getVal()), bedroomFuture.completer());
        this.bathroomIndex.update(rental.getId(), String.valueOf(rental.getBathroom().getVal()), bathroomFuture.completer());

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

    @SuppressWarnings("unchecked")
    public void search(RoutingContext context) {
        JsonObject searchInfo = context.getBodyAsJson();

        RedisSearch moveInDateSearch = new RedisContinuousSearch(moveInDateIndex,
                NEG_INF, // Apartment available date should be before the user-input move-in date
                searchInfo.getString("move_in_date", POS_INF));
        RedisSearch priceSearch = new RedisContinuousSearch(priceIndex,
                searchInfo.getString("price_min", NEG_INF),
                searchInfo.getString("price_max", POS_INF));
        RedisSearch quantifierSearch = new RedisCategoricalSearch(quantifierIndex,
                searchInfo.getJsonArray("quantifiers", new JsonArray()).getList());
        RedisSearch bedroomSearch = new RedisCategoricalSearch(bedroomIndex,
                searchInfo.getJsonArray("bedrooms", new JsonArray()).getList());
        RedisSearch bathroomSearch = new RedisCategoricalSearch(bathroomIndex,
                searchInfo.getJsonArray("bathrooms", new JsonArray()).getList());

        RedisCompositeSearch redisCompositeSearch =
                new RedisCompositeSearch(moveInDateSearch, priceSearch, quantifierSearch, bedroomSearch, bathroomSearch);
        redisCompositeSearch.exec(res1 -> {
            if (res1.succeeded()) {
                redisCompositeSearch.get(res2 -> {
                    if (res2.succeeded()) {
                        context.response().setStatusCode(200).end(Json.encodePrettily(res2.result()));
                    } else {
                        context.response().setStatusCode(500).end();
                    }
                });
                redisCompositeSearch.del(res3 -> {
                    if (res3.failed()) {
                        LOG.error(res3.cause().getMessage());
                    }
                });
            } else {
                context.response().setStatusCode(500).end();
            }
        });
    }

}
