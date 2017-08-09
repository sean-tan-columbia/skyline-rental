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
import io.vertx.ext.web.Session;
import io.vertx.ext.web.client.WebClient;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.RangeOptions;

import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by jtan on 6/5/17.
 */
public class RentalHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RentalHandler.class);
    private final static String RENTAL_KEY_BASE = "data.core.rental:";
    private final static String SEARCH_KEY_BASE = "data.redis.index:";
    private final static String NEG_INF = "-inf";
    private final static String POS_INF = "+inf";
    private final static String SESSION_USERNAME = "username";
    private final RedisContinuousIndex lastUpdatedTimestampIndex;
    private final RedisContinuousIndex moveInDateIndex;
    private final RedisContinuousIndex priceIndex;
    private final RedisCategoricalIndex quantifierIndex;
    private final RedisCategoricalIndex bedroomIndex;
    private final RedisCategoricalIndex bathroomIndex;
    private final RedisContinuousIndex latitudeIndex;
    private final RedisContinuousIndex longitudeIndex;
    private final RedisClient redisClient;
    private final GCSAuthHandler gcsAuthHandler;

    public RentalHandler(RedisClient redisClient, GCSAuthHandler gcsAuthHandler) {
        this.lastUpdatedTimestampIndex = new RedisContinuousIndex("lastUpdatedTimestamp", redisClient);
        this.moveInDateIndex = new RedisContinuousIndex("moveInDate", redisClient);
        this.priceIndex = new RedisContinuousIndex("price", redisClient);
        this.quantifierIndex = new RedisCategoricalIndex("quantifier", redisClient);
        this.bedroomIndex = new RedisCategoricalIndex("bedroom", redisClient);
        this.bathroomIndex = new RedisCategoricalIndex("bathroom", redisClient);
        this.latitudeIndex = new RedisContinuousIndex("latitude", redisClient);
        this.longitudeIndex = new RedisContinuousIndex("longitude", redisClient);
        this.redisClient = redisClient;
        this.gcsAuthHandler = gcsAuthHandler;
    }

    public void put(RoutingContext context) {
        Session session = context.session();
        if (session.get(SESSION_USERNAME) == null) {
            context.response().setStatusCode(403).end();
        }
        JsonObject rentalInfo = context.getBodyAsJson();
        Rental rental;
        try {
            rental = mapToRental(rentalInfo).setPosterId(session.get(SESSION_USERNAME));
        } catch (Exception e) {
            LOG.error(e.getCause());
            return;
        }
        JsonObject rentalJson = new JsonObject(Json.encode(rental));
        redisClient.hmset(RENTAL_KEY_BASE + rental.getId(), rentalJson, r1 -> {
            if (r1.succeeded()) {
                addRentalToIndices(rental, r2 -> {
                    if (r2.succeeded()) {
                        gcsAuthHandler.getAccessToken(r3 -> {
                            if (r3.succeeded()) {
                                context.response().setStatusCode(201).end(Json.encodePrettily(r3.result()));
                            } else {
                                context.response().setStatusCode(500).end();
                            }
                        });
                        LOG.info("Inserted Rental " + rentalJson.getString("id"));
                    }
                });
            } else {
                context.response().setStatusCode(500).end();
                LOG.error("Failed to insert Rental " + rentalJson.getString("id"));
            }
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

    private void addRentalToIndices(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        Future<Long> lastUpdatedTimestampFuture = Future.future();
        Future<Long> moveInDateFuture = Future.future();
        Future<Long> priceFuture = Future.future();
        Future<Long> quantifierFuture = Future.future();
        Future<Long> bedroomFuture = Future.future();
        Future<Long> bathroomFuture = Future.future();
        Future<Long> longitudeFuture = Future.future();
        Future<Long> latitudeFuture = Future.future();
        lastUpdatedTimestampIndex.add(rental.getId(), String.valueOf(rental.getLastUpdatedTimestamp().getTime() / 1000), lastUpdatedTimestampFuture.completer());
        moveInDateIndex.add(rental.getId(), String.valueOf(rental.getStartDate().getTime() / 1000), moveInDateFuture.completer());
        priceIndex.add(rental.getId(), String.valueOf(rental.getPrice()), priceFuture.completer());
        quantifierIndex.add(rental.getId(), String.valueOf(rental.getQuantifier().getVal()), quantifierFuture.completer());
        bedroomIndex.add(rental.getId(), String.valueOf(rental.getBedroom().getVal()), bedroomFuture.completer());
        bathroomIndex.add(rental.getId(), String.valueOf(rental.getBathroom().getVal()), bathroomFuture.completer());
        longitudeIndex.add(rental.getId(), String.valueOf(rental.getLongitude()), longitudeFuture.completer());
        latitudeIndex.add(rental.getId(), String.valueOf(rental.getLatitude()), latitudeFuture.completer());

        CompositeFuture.all(Arrays.asList(
                moveInDateFuture,
                priceFuture,
                quantifierFuture,
                bedroomFuture,
                bathroomFuture,
                longitudeFuture,
                latitudeFuture
        )).setHandler(res -> {
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
        redisClient.hgetall(RENTAL_KEY_BASE + rentalId, r -> {
            if (r.succeeded()) {
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

    public void delete(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        redisClient.hgetall(RENTAL_KEY_BASE + rentalId, r1 -> {
            if (r1.succeeded()) {
                Session session = context.session();
                String username = session.get(SESSION_USERNAME);
                if (username == null || !username.equals(r1.result().getString("posterId"))) {
                    LOG.warn("Username and posterId not matched for Rental: " + rentalId);
                    context.response().setStatusCode(202).end();
                } else {
                    redisClient.del(RENTAL_KEY_BASE + rentalId, r2 -> {
                        if (r2.succeeded()) {
                            delRentalFromIndices(rentalId, r3 -> {
                                if (r3.succeeded()) {
                                    gcsAuthHandler.getAccessToken(r4 -> {
                                        if (r4.succeeded()) {
                                            context.response().setStatusCode(200).end(Json.encodePrettily(r4.result()));
                                        } else {
                                            context.response().setStatusCode(500).end();
                                        }
                                    });
                                    LOG.info("Deleted Rental " + rentalId);
                                }
                            });
                        } else {
                            LOG.info("Failed to delete Rental " + rentalId);
                            context.response().setStatusCode(202).end();
                        }
                    });
                }
            } else {
                LOG.error("Failed to delete " + rentalId);
                context.response().setStatusCode(202).end();
            }
        });
    }

    private void delRentalFromIndices(String rentalId, Handler<AsyncResult<Long>> resultHandler) {
        Future<Long> lastUpdatedTimestampFuture = Future.future();
        Future<Long> moveInDateFuture = Future.future();
        Future<Long> priceFuture = Future.future();
        Future<Long> quantifierFuture = Future.future();
        Future<Long> bedroomFuture = Future.future();
        Future<Long> bathroomFuture = Future.future();
        Future<Long> longitudeFuture = Future.future();
        Future<Long> latitudeFuture = Future.future();
        lastUpdatedTimestampIndex.del(rentalId, lastUpdatedTimestampFuture.completer());
        moveInDateIndex.del(rentalId, moveInDateFuture.completer());
        priceIndex.del(rentalId, priceFuture.completer());
        quantifierIndex.del(rentalId, quantifierFuture.completer());
        bedroomIndex.del(rentalId, bedroomFuture.completer());
        bathroomIndex.del(rentalId, bathroomFuture.completer());
        longitudeIndex.del(rentalId, longitudeFuture.completer());
        latitudeIndex.del(rentalId, latitudeFuture.completer());

        CompositeFuture.all(Arrays.asList(
                moveInDateFuture,
                priceFuture,
                quantifierFuture,
                bedroomFuture,
                bathroomFuture,
                longitudeFuture,
                latitudeFuture
        )).setHandler(res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture());
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    public void update(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        redisClient.hgetall(RENTAL_KEY_BASE + rentalId, r1 -> {
            if (r1.succeeded()) {
                Session session = context.session();
                String username = session.get(SESSION_USERNAME);
                if (username == null || !username.equals(r1.result().getString("posterId"))) {
                    LOG.warn("Username and posterId not matched for Rental: " + rentalId);
                    context.response().setStatusCode(202).end();
                } else {
                    put(context);
                }
            } else {
                LOG.error("Failed to update " + rentalId);
                context.response().setStatusCode(202).end();
            }
        });
    }

    public void sort(RoutingContext context) {
        String sorter = context.request().getParam("sorter");
        String order = context.request().getParam("order");
        if (sorter.equals("last_updated_timestamp") && order.equals("desc")) {
            sort(context, "lastUpdatedTimestamp", false);
        } else if (sorter.equals("last_updated_timestamp") && order.equals("asc")) {
            sort(context, "lastUpdatedTimestamp", true);
        } else if (sorter.equals("price") && order.equals("desc")) {
            sort(context, "price", false);
        } else if (sorter.equals("price") && order.equals("asc")) {
            sort(context, "price", true);
        }
    }

    private void sort(RoutingContext context, String sorter, Boolean order) {
        if (order) { // Ascending
            redisClient.zrange(SEARCH_KEY_BASE + sorter, 0, -1, r -> {
                if (r.succeeded()) {
                    context.response().putHeader("content-type", "application/json")
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .end(Json.encodePrettily(r.result()));
                } else {
                    context.response().setStatusCode(500).end();
                }
            });
        } else {
            redisClient.zrevrange(SEARCH_KEY_BASE + sorter, 0, -1, RangeOptions.NONE, r -> {
                if (r.succeeded()) {
                    context.response().putHeader("content-type", "application/json")
                            .putHeader("Access-Control-Allow-Origin", "*")
                            .end(Json.encodePrettily(r.result()));
                } else {
                    context.response().setStatusCode(500).end();
                }
            });
        }
    }

    public void searchMap(RoutingContext context) {
        JsonObject searchInfo = context.getBodyAsJson();

        String searchId = UUID.randomUUID().toString();
        RedisSearch lastUpdatedTimestampSearch = new RedisContinuousSearch("lastUpdatedTimestampSearch:" + searchId,
                lastUpdatedTimestampIndex, NEG_INF, POS_INF);
        RedisSearch longitudeSearch = new RedisContinuousSearch("longitudeSearch:" + searchId, longitudeIndex,
                searchInfo.getString("lng_min"),
                searchInfo.getString("lng_max"));
        RedisSearch latitudeSearch = new RedisContinuousSearch("latitudeSearch:" + searchId, latitudeIndex,
                searchInfo.getString("lat_min"),
                searchInfo.getString("lat_max"));
        RedisCompositeSearch mapSearch = new RedisCompositeSearch(lastUpdatedTimestampSearch, longitudeSearch, latitudeSearch);
        mapSearch.exec(res1 -> {
            if (res1.succeeded()) {
                mapSearch.get(false, res2 -> {
                    if (res2.succeeded()) {
                        context.response().setStatusCode(200).end(Json.encodePrettily(res2.result()));
                    } else {
                        context.response().setStatusCode(500).end();
                    }
                });
            } else {
                context.response().setStatusCode(500).end();
            }
            mapSearch.del(res3 -> {
                if (res3.failed()) {
                    LOG.error(res3.cause().getMessage());
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    public void search(RoutingContext context) {
        JsonObject searchInfo = context.getBodyAsJson();

        String searchId = UUID.randomUUID().toString();
        String primary = searchInfo.getString("primary", "last_updated_timestamp");
        String order = searchInfo.getString("order", "desc");
        RedisSearch lastUpdatedTimestampSearch = new RedisContinuousSearch("lastUpdatedTimestampSearch:" + searchId,
                lastUpdatedTimestampIndex, NEG_INF, POS_INF);
        RedisSearch moveInDateSearch = new RedisContinuousSearch("moveInDateSearch:" + searchId, moveInDateIndex,
                NEG_INF, // Apartment available date should be before the user-input move-in date
                searchInfo.getString("move_in_date", POS_INF));
        RedisSearch priceSearch = new RedisContinuousSearch("priceSearch:" + searchId, priceIndex,
                searchInfo.getString("price_min", NEG_INF),
                searchInfo.getString("price_max", POS_INF));
        RedisSearch quantifierSearch = new RedisCategoricalSearch("quantifierSearch:" + searchId, quantifierIndex,
                searchInfo.getJsonArray("quantifiers", new JsonArray()).getList());
        RedisSearch bedroomSearch = new RedisCategoricalSearch("bedroomSearch:" + searchId, bedroomIndex,
                searchInfo.getJsonArray("bedrooms", new JsonArray()).getList());
        RedisSearch bathroomSearch = new RedisCategoricalSearch("bathroomSearch:" + searchId, bathroomIndex,
                searchInfo.getJsonArray("bathrooms", new JsonArray()).getList());
        RedisSearch longitudeSearch = new RedisContinuousSearch("longitudeSearch:" + searchId, longitudeIndex,
                searchInfo.getString("lng_min", NEG_INF),
                searchInfo.getString("lng_max", POS_INF));
        RedisSearch latitudeSearch = new RedisContinuousSearch("latitudeSearch:" + searchId, latitudeIndex,
                searchInfo.getString("lat_min", NEG_INF),
                searchInfo.getString("lat_max", POS_INF));

        RedisCompositeSearch redisCompositeSearch = getCompositeSearch(primary,
                lastUpdatedTimestampSearch, moveInDateSearch, priceSearch, quantifierSearch, bedroomSearch, bathroomSearch, longitudeSearch, latitudeSearch);
        redisCompositeSearch.exec(res1 -> {
            if (res1.succeeded()) {
                redisCompositeSearch.get(order.equals("asc"), res2 -> {
                    if (res2.succeeded()) {
                        context.response().setStatusCode(200).end(Json.encodePrettily(res2.result()));
                    } else {
                        context.response().setStatusCode(500).end();
                    }
                });
            } else {
                context.response().setStatusCode(500).end();
            }
            redisCompositeSearch.del(res3 -> {
                if (res3.failed()) {
                    LOG.error(res3.cause().getMessage());
                }
            });
        });
    }

    private RedisCompositeSearch getCompositeSearch(String primary,
                                                    RedisSearch lastUpdatedTimestampSearch,
                                                    RedisSearch moveInDateSearch,
                                                    RedisSearch priceSearch,
                                                    RedisSearch quantifierSearch,
                                                    RedisSearch bedroomSearch,
                                                    RedisSearch bathroomSearch,
                                                    RedisSearch longitudeSearch,
                                                    RedisSearch latitudeSearch) {
        switch (primary) {
            case "last_updated_timestamp":
                return new RedisCompositeSearch(lastUpdatedTimestampSearch, moveInDateSearch, priceSearch, quantifierSearch, bedroomSearch, bathroomSearch, longitudeSearch, latitudeSearch);
            case "move_in_date":
                return new RedisCompositeSearch(moveInDateSearch, lastUpdatedTimestampSearch, priceSearch, quantifierSearch, bedroomSearch, bathroomSearch, longitudeSearch, latitudeSearch);
            case "price":
                return new RedisCompositeSearch(priceSearch, moveInDateSearch, lastUpdatedTimestampSearch, quantifierSearch, bedroomSearch, bathroomSearch, longitudeSearch, latitudeSearch);
            default:
                return new RedisCompositeSearch(lastUpdatedTimestampSearch, moveInDateSearch, priceSearch, quantifierSearch, bedroomSearch, bathroomSearch, longitudeSearch, latitudeSearch);
        }
    }

}
