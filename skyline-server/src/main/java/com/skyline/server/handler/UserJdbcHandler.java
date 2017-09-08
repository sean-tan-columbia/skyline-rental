package com.skyline.server.handler;

import com.skyline.server.model.Rental;
import com.skyline.server.model.User;
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

    public void getRental(String posterId, Handler<AsyncResult<List<Rental>>> resultHandler) {
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

    public void getUser(String posterId, Handler<AsyncResult<User>> resultHandler) {
        connection.queryWithParams(
                "SELECT ID," +
                        "EMAIL " +
                        "FROM eventbus.USERS " +
                        "WHERE ID=?",
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
                                .setEmail(row.getString(1))
                                .setWechatId("");
                        resultHandler.handle(Future.succeededFuture(user));
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
