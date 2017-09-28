package com.journey.webserver.handler;

import com.journey.webserver.model.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;

import java.util.List;

public class UserAuthJdbcHandler {

    private final static Logger LOG = LoggerFactory.getLogger(UserAuthJdbcHandler.class);
    private SQLConnection connection;

    public UserAuthJdbcHandler(JDBCClient jdbcClient) {
        connect(jdbcClient);
    }

    public void exists(String email, Handler<AsyncResult<Boolean>> resultHandler) {
        connection.queryWithParams("SELECT COUNT(1) FROM eventbus.USERS WHERE EMAIL=? AND STATUS='ACTIVE'", new JsonArray().add(email), r -> {
            if (r.failed()) {
                resultHandler.handle(Future.failedFuture(r.cause()));
                return;
            }
            Integer count = r.result().getResults().get(0).getInteger(0);
            if (count == 1) {
                resultHandler.handle(Future.succeededFuture(true));
            } else if (count == 0) {
                resultHandler.handle(Future.succeededFuture(false));
            } else {
                resultHandler.handle(Future.failedFuture(new Exception("Duplicate User")));
            }
        });
    }

    public void select(String email, Handler<AsyncResult<User>> resultHandler) {
        connection.queryWithParams("SELECT ID,NAME,EMAIL,PHONE,WECHAT_ID FROM eventbus.USERS WHERE EMAIL=? AND STATUS='ACTIVE'",
                new JsonArray().add(email), r -> {
            if (r.failed()) {
                resultHandler.handle(Future.failedFuture(r.cause()));
                return;
            }
            List<JsonArray> rows = r.result().getResults();
            if (rows.size() == 1) {
                JsonArray row = rows.get(0);
                User user = new User(row.getString(0))
                        .setName(row.getString(1))
                        .setEmail(row.getString(2))
                        .setPhone(row.getString(3))
                        .setWechatId(row.getString(4));
                resultHandler.handle(Future.succeededFuture(user));
            } else if (rows.size() == 0) {
                resultHandler.handle(Future.failedFuture(new NullPointerException()));
            } else {
                resultHandler.handle(Future.failedFuture(new Exception("Duplicate User")));
            }
        });
    }

    public void insert(User user, String salt, String hash, Handler<AsyncResult<Long>> resultHandler) {
        connection.updateWithParams("INSERT INTO eventbus.USERS(" +
                        "ID," +
                        "NAME," +
                        "EMAIL," +
                        "PHONE," +
                        "WECHAT_ID," +
                        "PASSWORD," +
                        "PASSWORD_SALT," +
                        "STATUS," +
                        "LAST_LOGIN_TIMESTAMP," +
                        "CREATED_TIMESTAMP," +
                        "LAST_UPDATED_TIMESTAMP" +
                        ") VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                new JsonArray()
                        .add(user.getId())
                        .add(user.getName())
                        .add(user.getEmail())
                        .add(user.getPhone())
                        .add(user.getWechatId())
                        .add(hash)
                        .add(salt)
                        .add(user.getStatus())
                        .add(user.formatLastLoginTimestamp())
                        .add(user.formatCreatedTimestamp())
                        .add(user.formatLastUpdatedTimestamp()), r -> {
                    if (r.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(r.cause()));
                    }
                });
    }

    public void update(String id, String salt, String hash, Handler<AsyncResult<Long>> resultHandler) {
        connection.updateWithParams("UPDATE eventbus.USERS SET " +
                        "PASSWORD=?," +
                        "PASSWORD_SALT=? " +
                        "WHERE ID=? AND STATUS='ACTIVE'",
                new JsonArray().add(hash).add(salt).add(id), r -> {
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
