package com.skyline.server.verticle;

import com.skyline.server.Config;
import com.skyline.server.handler.*;
import com.skyline.server.sstore.RedisSessionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

import java.util.Properties;

/**
 * Created by jtan on 6/2/17.
 */
public class ServerVerticle extends AbstractVerticle {

    //private final String WEB_ROOT = "../../../../skyline-client"; // intellij-baseDir: src/main
    //private final String WEB_ROOT = "../../../skyline-client";
    private final static Logger LOG = LoggerFactory.getLogger(ServerVerticle.class);
    private JDBCClient jdbcClient;
    private RentalHandler rentalHandler;
    private UserHandler userHandler;
    private JDBCAuth jdbcAuthProvider;
    private UserAuthHandler userAuthHandler;
    private RedisClient redisClient;
    private SessionHandler sessionHandler;
    private UserSessionHandler userSessionHandler;
    private AuthHandler redirectAuthHandler;

    @Override
    public void start(Future<Void> future) throws Exception {

        Properties properties = System.getProperties();
        LOG.info(properties.getProperty("env"));

        Config config = Config.getInstance(properties.getProperty("env"));
        this.redisClient = RedisClient.create(vertx, new RedisOptions()
                .setHost(config.getRedisHost())
                .setAuth(config.getRedisAuth()));
        this.jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:postgresql://" + config.getPostgresHost() + "/" + config.getPostgresName())
                .put("user", config.getPostgresUser())
                .put("password", config.getPostgresAuth())
                .put("driver_class", config.getPostgresDriver())
                .put("socketTimeout", 5));
        LOG.info("Redis Host: " + config.getRedisHost());
        LOG.info("Postgres Host: " + config.getPostgresHost());

        UserRedisHandler userRedisHandler = new UserRedisHandler(this.redisClient);
        UserJdbcHandler userJdbcHandler = new UserJdbcHandler(this.jdbcClient);
        this.userHandler = new UserHandler(userRedisHandler, userJdbcHandler);

        RentalRedisHandler rentalRedisHandler = new RentalRedisHandler(this.redisClient);
        RentalJdbcHandler rentalJdbcHandler = new RentalJdbcHandler(this.jdbcClient);
        GCSAuthHandler gcsAuthHandler = new GCSAuthHandler(vertx, redisClient, config);
        this.rentalHandler = new RentalHandler(rentalRedisHandler, rentalJdbcHandler, gcsAuthHandler, userHandler);

        UserAuthRedisHandler userAuthRedisHandler = new UserAuthRedisHandler(this.redisClient);
        UserAuthJdbcHandler userAuthJdbcHandler = new UserAuthJdbcHandler(this.jdbcClient);
        this.jdbcAuthProvider = createJdbcAuthProvider();
        this.userAuthHandler = new UserAuthHandler(userAuthRedisHandler, userAuthJdbcHandler, this.jdbcAuthProvider);

        this.sessionHandler = SessionHandler.create(RedisSessionStore.create(vertx, redisClient, config.getSessionRetryTimeout())).setSessionTimeout(config.getSessionTimeout());
        this.userSessionHandler = UserSessionHandler.create(jdbcAuthProvider);
        this.redirectAuthHandler = RedirectAuthHandler.create(jdbcAuthProvider, "/login-view/login.html");

        Router router = createRouter();
        // vertx.createHttpServer(new HttpServerOptions().setSsl(true).setKeyStoreOptions(
        //     new JksOptions().setPath("server-keystore.jks").setPassword("skyline")
        // )).requestHandler(router::accept).listen(8443);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8080,
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        }
                );
    }

    private JDBCAuth createJdbcAuthProvider() {
        JDBCAuth authProvider = JDBCAuth.create(vertx, this.jdbcClient);
        authProvider.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM eventbus.USERS WHERE ID=?");
        authProvider.setNonces(new JsonArray().add("KuMLwD0j1rB1yx0iOc").add("uDcCj0SkINwqOzxxGI"));
        return authProvider;
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        // Auth logic
        router.route().handler(CookieHandler.create());
        router.route().handler(BodyHandler.create());

        router.route("/api/public/user/login").handler(sessionHandler);
        router.route("/api/public/user/login").handler(userSessionHandler);
        router.post("/api/public/user/login").handler(userAuthHandler::authenticate);
        router.post("/api/public/user/register").handler(userAuthHandler::register);
        router.post("/api/public/user/reset").handler(userAuthHandler::reset);
        router.get("/api/public/user/verify/:salt").handler(userAuthHandler::verify);
        router.post("/api/public/user/password").handler(userAuthHandler::setPassword);

        router.post("/api/public/rental/search").handler(rentalHandler::search);
        router.post("/api/public/rental/location").handler(rentalHandler::searchLocation);
        router.get("/api/public/rental/:rentalId").handler(rentalHandler::get);
        // router.get("/api/public/discover/:sorter/:order").handler(rentalHandler::sort);

        router.route("/api/private/*").handler(sessionHandler);
        router.route("/api/private/*").handler(userSessionHandler);
        router.post("/api/private/rental").handler(rentalHandler::put);
        router.put("/api/private/rental/:rentalId").handler(rentalHandler::update);
        router.delete("/api/private/rental/:rentalId").handler(rentalHandler::delete);
        router.get("/api/private/user").handler(userHandler::get);
        router.put("/api/private/user").handler(userHandler::update);

        router.route("/*").handler(StaticHandler.create().setCachingEnabled(false));

        return router;
    }

}
