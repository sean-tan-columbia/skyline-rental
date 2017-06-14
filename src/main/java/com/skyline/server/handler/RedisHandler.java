package com.skyline.server.handler;

import com.skyline.server.Config;
import com.skyline.server.model.Rental;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;
import io.vertx.redis.RedisClient;

import java.text.SimpleDateFormat;


/**
 * Created by jtan on 6/5/17.
 */
public class RedisHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RedisHandler.class);
    private final Config config;
    private final RedisClient client;

    public RedisHandler(RedisClient client, Config config) {
        this.client = client;
        this.config = config;
    }

    public void put(RoutingContext context) {
        JsonObject rentalInfo = context.getBodyAsJson();
        if (!rentalInfo.containsKey("posterId")) {
            LOG.error("PosterId not provided.");
            return;
        }
        Rental rental = mapToRental(rentalInfo);
        JsonObject rentalJson = new JsonObject(Json.encode(rental));
        client.hmset(rental.getId(), rentalJson, r -> {
            if (r.succeeded()) {
                context.response().setStatusCode(201).end();
                LOG.info("Inserted Rental " + rentalJson.getString("id"));
            } else {
                context.response().setStatusCode(500).end();
                LOG.error("Failed to insert Rental " + rentalJson.getString("id"));
            }
        });
    }

    private Rental mapToRental(JsonObject rentalInfo) {
        Rental rental = new Rental(rentalInfo.getString("posterId"), this.config.getImagePath());
        rental.setRentalType(Rental.RentalType.valueOf(rentalInfo.getString("rentalType", "")));
        rental.setAddress(rentalInfo.getString("address", ""));
        rental.setNeighborhood(rentalInfo.getString("neighborhood", ""));
        rental.setPrice(rentalInfo.getDouble("price", 0.0));
        rental.setQuantifier(Rental.Quantifier.valueOf(rentalInfo.getString("quantifier", "")));
        rental.setDescription(rentalInfo.getString("description"));
        rentalInfo.getJsonArray("imageIds").stream().forEach(i -> rental.addImageIds(i.toString()));
        try {
            rental.setStartDate(new SimpleDateFormat("yyyy-MM-dd").parse(rentalInfo.getString("startDate", "")));
            rental.setEndDate(new SimpleDateFormat("yyyy-MM-dd").parse(rentalInfo.getString("endDate", "")));
        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return rental;
    }

    public void get(RoutingContext context) {
        String rentalId = context.request().getParam("rentalId");
        if (rentalId == null) {
            context.response().setStatusCode(400).end();
            return;
        }
        client.hgetall(rentalId, r -> {
            if (r.succeeded()) {
                LOG.info("Received Rental " + rentalId);
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .end(Json.encodePrettily(r.result()));
            } else {
                LOG.error("Failed to retrieve " + rentalId);
                context.response().setStatusCode(500);
            }
        });
    }

    public void getMax(RoutingContext context) {
        client.keys("*", r -> {
            if (r.succeeded()) {
                context.response()
                        .putHeader("content-type", "application/json")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .end(Json.encodePrettily(r.result()));
            } else {
                context.response().setStatusCode(400).end();
            }
        });
    }

}
