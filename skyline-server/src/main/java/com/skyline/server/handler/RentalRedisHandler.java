package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import com.skyline.server.search.RedisSearch;
import com.skyline.server.search.impl.*;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.redis.RedisClient;
import io.vertx.redis.op.RangeOptions;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RentalRedisHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RentalRedisHandler.class);
    private final static String RENTAL_KEY_BASE = "data.core.rental:";
    private final static String SEARCH_KEY_BASE = "data.redis.index:";
    private final static String NEG_INF = "-inf";
    private final static String POS_INF = "+inf";
    private final RedisContinuousIndex lastUpdatedTimestampIndex;
    private final RedisContinuousIndex moveInDateIndex;
    private final RedisContinuousIndex priceIndex;
    private final RedisCategoricalIndex quantifierIndex;
    private final RedisCategoricalIndex bedroomIndex;
    private final RedisCategoricalIndex bathroomIndex;
    private final RedisContinuousIndex latitudeIndex;
    private final RedisContinuousIndex longitudeIndex;
    private final RedisClient client;

    public RentalRedisHandler(RedisClient redisClient) {
        this.lastUpdatedTimestampIndex = new RedisContinuousIndex("lastUpdatedTimestamp", redisClient);
        this.moveInDateIndex = new RedisContinuousIndex("moveInDate", redisClient);
        this.priceIndex = new RedisContinuousIndex("price", redisClient);
        this.quantifierIndex = new RedisCategoricalIndex("quantifier", redisClient);
        this.bedroomIndex = new RedisCategoricalIndex("bedroom", redisClient);
        this.bathroomIndex = new RedisCategoricalIndex("bathroom", redisClient);
        this.latitudeIndex = new RedisContinuousIndex("latitude", redisClient);
        this.longitudeIndex = new RedisContinuousIndex("longitude", redisClient);
        this.client = redisClient;
    }

    void put(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        if (rental == null) {
            resultHandler.handle(Future.failedFuture(new NullPointerException()));
            return;
        }
        Buffer buff = Buffer.buffer();
        rental.writeToBuffer(buff);
        client.setBinary(RENTAL_KEY_BASE + rental.getId(), buff, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            addRentalToIndices(rental, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
        });
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

    void get(String rentalId, Handler<AsyncResult<Rental>> resultHandler) {
        if (rentalId == null) {
            resultHandler.handle(Future.failedFuture(new NullPointerException()));
            return;
        }
        client.getBinary(RENTAL_KEY_BASE + rentalId, r -> {
            if (r.succeeded()) {
                Buffer buff = r.result();
                if (buff == null) {
                    resultHandler.handle(Future.failedFuture("Rental Not Exists!"));
                    return;
                }
                Rental rental = new Rental();
                rental.readFromBuffer(0, buff);
                resultHandler.handle(Future.succeededFuture(rental));
            } else {
                resultHandler.handle(Future.failedFuture(r.cause()));
            }
        });
    }

    void delete(String rentalId, Handler<AsyncResult<Long>> resultHandler) {
        if (rentalId == null) {
            resultHandler.handle(Future.failedFuture(new NullPointerException()));
            return;
        }
        client.del(RENTAL_KEY_BASE + rentalId, r1 -> {
            if (r1.failed()) {
                resultHandler.handle(Future.failedFuture(r1.cause()));
                return;
            }
            delRentalFromIndices(rentalId, r2 -> {
                if (r2.succeeded()) {
                    resultHandler.handle(Future.succeededFuture());
                } else {
                    resultHandler.handle(Future.failedFuture(r2.cause()));
                }
            });
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

    void searchLocation(JsonObject searchInfo, Handler<AsyncResult<List<String>>> resultHandler) {
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
                        resultHandler.handle(Future.succeededFuture(res2.result()));
                    } else {
                        resultHandler.handle(Future.failedFuture(res2.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(res1.cause()));
            }
            mapSearch.del(res3 -> {
                if (res3.failed()) {
                    resultHandler.handle(Future.failedFuture(res3.cause()));
                }
            });
        });
    }

    @SuppressWarnings("unchecked")
    void search(JsonObject searchInfo, Handler<AsyncResult<List<String>>> resultHandler) {
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
                        resultHandler.handle(Future.succeededFuture(res2.result()));
                    } else {
                        resultHandler.handle(Future.failedFuture(res2.cause()));
                    }
                });
            } else {
                resultHandler.handle(Future.failedFuture(res1.cause()));
            }
            redisCompositeSearch.del(res3 -> {
                if (res3.failed()) {
                    resultHandler.handle(Future.failedFuture(res3.cause()));
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

    void sort(String sorter, Boolean order, Handler<AsyncResult<JsonArray>> resultHandler) {
        if (order) { // Ascending
            client.zrange(SEARCH_KEY_BASE + sorter, 0, -1, r -> {
                if (r.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(r.result()));
                } else {
                    resultHandler.handle(Future.failedFuture(r.cause()));
                }
            });
        } else {
            client.zrevrange(SEARCH_KEY_BASE + sorter, 0, -1, RangeOptions.NONE, r -> {
                if (r.succeeded()) {
                    resultHandler.handle(Future.succeededFuture(r.result()));
                } else {
                    resultHandler.handle(Future.failedFuture(r.cause()));
                }
            });
        }
    }

}
