package com.journey.webserver.handler;

import com.journey.webserver.model.Rental;
import com.journey.webserver.model.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserJdbcHandler {

    private final static Logger LOG = LoggerFactory.getLogger(UserJdbcHandler.class);
    private SQLConnection connection;

    public UserJdbcHandler(JDBCClient jdbcClient) {
        connect(jdbcClient);
    }

    void getRental(String posterId, Handler<AsyncResult<List<Rental>>> resultHandler) {
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
                        "WHERE POSTER_ID=? AND STATUS='ACTIVE'",
                new JsonArray().add(posterId),
                r2 -> {
                    if (r2.succeeded()) {
                        List<JsonArray> rows = r2.result().getResults();
                        try {
                            List<Rental> rentals = new ArrayList<>();
                            for (JsonArray row : rows) {
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
                                rentals.add(rental);
                            }
                            resultHandler.handle(Future.succeededFuture(rentals));
                        } catch (ParseException e) {
                            resultHandler.handle(Future.failedFuture(e.getCause()));
                        }
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                }
        );
    }

    void getUser(String posterId, Handler<AsyncResult<User>> resultHandler) {
        connection.queryWithParams(
                "SELECT ID," +
                        "NAME," +
                        "EMAIL," +
                        "PHONE," +
                        "WECHAT_ID " +
                        "FROM eventbus.USERS " +
                        "WHERE ID=? AND STATUS='ACTIVE'",
                new JsonArray().add(posterId),
                r2 -> {
                    if (r2.succeeded()) {
                        List<JsonArray> rows = r2.result().getResults();
                        if (rows.size() == 0) {
                            resultHandler.handle(Future.failedFuture("Not Exists!"));
                            return;
                        }
                        JsonArray row = rows.get(0);
                        User user = new User(posterId)
                                .setName(row.getString(1))
                                .setEmail(row.getString(2))
                                .setPhone(row.getString(3))
                                .setWechatId(row.getString(4));
                        resultHandler.handle(Future.succeededFuture(user));
                    } else {
                        resultHandler.handle(Future.failedFuture(r2.cause()));
                    }
                }
        );
    }

    void update(User poster, Handler<AsyncResult<Long>> resultHandler) {
        connection.updateWithParams("UPDATE eventbus.USERS SET " +
                        "NAME=?," +
                        "PHONE=?," +
                        "WECHAT_ID=? " +
                        "WHERE ID=? AND STATUS='ACTIVE'",
                new JsonArray().add(poster.getName())
                        .add(poster.getPhone())
                        .add(poster.getWechatId())
                        .add(poster.getId()), r -> {
                    if (r.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r.cause()));
                    }
                });
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
