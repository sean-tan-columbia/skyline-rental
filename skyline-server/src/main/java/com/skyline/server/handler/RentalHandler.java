package com.skyline.server.handler;

import com.skyline.server.model.Rental;
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
    private final RedisClient client;

    public RentalHandler(RedisClient client) {
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
                    context.response().setStatusCode(201).end();
                    LOG.info("Inserted Rental " + rentalJson.getString("id"));
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

    private void updateIndices(RoutingContext context, Rental rental) {
        client.zadd("lastUpdatedTimestamp", (double)rental.getLastUpdatedTimestamp().getTime(), rental.getId(), r -> {

        });
        client.zadd("price", rental.getPrice(), rental.getId(), r -> {

        });
        client.sadd("posterId:" + rental.getPosterId(), rental.getId(), r -> {

        });

    }

}
