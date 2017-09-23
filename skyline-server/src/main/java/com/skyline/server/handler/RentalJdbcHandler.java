package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.SQLConnection;

import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

public class RentalJdbcHandler {

    private final static Logger LOG = LoggerFactory.getLogger(RentalJdbcHandler.class);
    private SQLConnection connection;

    public RentalJdbcHandler(JDBCClient jdbcClient) {
        connect(jdbcClient);
    }

    void select(String rentalId, Handler<AsyncResult<Rental>> resultHandler) {
        connection.queryWithParams(
                "SELECT ID," +
                        "POSTER_ID," +
                        "ADDRESS," +
                        "RENTAL_TYPE," +
                        "NEIGHBORHOOD," +
                        "LATITUDE," +
                        "LONGITUDE," +
                        "PRICE," +
                        "QUANTIFIER," +
                        "BEDROOM," +
                        "BATHROOM," +
                        "START_DATE," +
                        "END_DATE," +
                        "DESCRIPTION," +
                        "IMAGE_ID," +
                        "STATUS," +
                        "LAST_UPDATED_TIMESTAMP," +
                        "CREATED_TIMESTAMP " +
                        "FROM eventbus.RENTALS " +
                        "WHERE ID=? AND STATUS='ACTIVE'",
                new JsonArray().add(rentalId),
                r2 -> {
                    if (r2.succeeded()) {
                        List<JsonArray> rows = r2.result().getResults();
                        if (rows.size() == 0) {
                            resultHandler.handle(Future.failedFuture("Not Exists!"));
                            return;
                        }
                        JsonArray row = rows.get(0);
                        try {
                            Rental rental = new Rental(row.getString(0))
                                    .setPosterId(row.getString(1))
                                    .setAddress(row.getString(2))
                                    .setRentalType(Rental.RentalType.valueOf(row.getString(3)))
                                    .setNeighborhood(row.getString(4))
                                    .setLatitude(row.getDouble(5))
                                    .setLongitude(row.getDouble(6))
                                    .setPrice(row.getDouble(7))
                                    .setQuantifier(Rental.Quantifier.valueOf(row.getString(8)))
                                    .setBedroom(Rental.Bedroom.valueOf(row.getString(9)))
                                    .setBathroom(Rental.Bathroom.valueOf(row.getString(10)))
                                    .setStartDate(row.getString(11))
                                    .setEndDate(row.getString(12))
                                    .setDescription(row.getString(13))
                                    .setImageIds(Arrays.asList(row.getString(14).split(",")))
                                    .setStatus(Rental.Status.valueOf(row.getString(15)))
                                    .setLastUpdatedTimestamp(row.getString(16))
                                    .setCreatedTimestamp(row.getString(17));
                            resultHandler.handle(Future.succeededFuture(rental));
                        } catch (ParseException e) {
                            resultHandler.handle(Future.failedFuture(e.getCause()));
                        }
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                }
        );
    }

    void insert(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        connection.updateWithParams(
                "INSERT INTO eventbus.RENTALS " +
                        "(ID," +
                        "POSTER_ID," +
                        "ADDRESS," +
                        "RENTAL_TYPE," +
                        "NEIGHBORHOOD," +
                        "LATITUDE," +
                        "LONGITUDE," +
                        "PRICE," +
                        "QUANTIFIER," +
                        "BEDROOM," +
                        "BATHROOM," +
                        "START_DATE," +
                        "END_DATE," +
                        "DESCRIPTION," +
                        "IMAGE_ID," +
                        "STATUS," +
                        "LAST_UPDATED_TIMESTAMP," +
                        "CREATED_TIMESTAMP) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new JsonArray()
                        .add(rental.getId())
                        .add(rental.getPosterId())
                        .add(rental.getAddress())
                        .add(rental.getRentalType())
                        .add(rental.getNeighborhood())
                        .add(rental.getLatitude())
                        .add(rental.getLongitude())
                        .add(rental.getPrice())
                        .add(rental.getQuantifier())
                        .add(rental.getBedroom())
                        .add(rental.getBathroom())
                        .add(rental.formatStartDate())
                        .add(rental.formatEndDate())
                        .add(rental.getDescription())
                        .add(String.join(",", rental.getImageIds()))
                        .add(rental.getStatus())
                        .add(rental.formatLastUpdatedTimestamp())
                        .add(rental.formatCreatedTimestamp()),
                r2 -> {
                    if (r2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                }
        );
    }

    void update(Rental rental, Handler<AsyncResult<Long>> resultHandler) {
        connection.updateWithParams(
                "UPDATE eventbus.RENTALS SET " +
                        "ADDRESS=?," +
                        "RENTAL_TYPE=?," +
                        "NEIGHBORHOOD=?," +
                        "LATITUDE=?," +
                        "LONGITUDE=?," +
                        "PRICE=?," +
                        "QUANTIFIER=?," +
                        "BEDROOM=?," +
                        "BATHROOM=?," +
                        "START_DATE=?," +
                        "END_DATE=?," +
                        "DESCRIPTION=?," +
                        "IMAGE_ID=?," +
                        "STATUS=?," +
                        "LAST_UPDATED_TIMESTAMP=? " +
                        "WHERE ID=? AND STATUS='ACTIVE'",
                new JsonArray()
                        .add(rental.getAddress())
                        .add(rental.getRentalType())
                        .add(rental.getNeighborhood())
                        .add(rental.getLatitude())
                        .add(rental.getLongitude())
                        .add(rental.getPrice())
                        .add(rental.getQuantifier())
                        .add(rental.getBedroom())
                        .add(rental.getBathroom())
                        .add(rental.formatStartDate())
                        .add(rental.formatEndDate())
                        .add(rental.getDescription())
                        .add(String.join(",", rental.getImageIds()))
                        .add(rental.getStatus())
                        .add(rental.formatLastUpdatedTimestamp())
                        .add(rental.getId()),
                r2 -> {
                    if (r2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                }
        );
    }

    void delete(String rentalId, Handler<AsyncResult<Long>> resultHandler) {
        connection.updateWithParams(
                "UPDATE eventbus.RENTALS SET " +
                        "STATUS='INACTIVE'," +
                        "LAST_UPDATED_TIMESTAMP=now()::timestamp(0) " +
                        "WHERE ID=? AND STATUS='ACTIVE'",
                new JsonArray().add(rentalId),
                r2 -> {
                    if (r2.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                }
        );
    }

    private void connect(JDBCClient jdbcClient) {
        jdbcClient.getConnection(r -> {
            if (r.succeeded()) {
                LOG.info("Connected to Postgres");
                connection = r.result();
            } else {
                LOG.error("Error connecting to Postgres");
                connection = null;
            }
        });
    }

}
