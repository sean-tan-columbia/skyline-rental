package com.journey.webserver.verticle;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.collect.Lists;
import com.journey.webserver.Config;
import com.journey.webserver.handler.*;
import com.journey.webserver.sstore.RedisSessionStore;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
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

import java.io.File;
import java.util.Properties;

/**
 * Created by jtan on 6/2/17.
 */
public class ServerVerticle extends AbstractVerticle {

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
                .put("driver_class", config.getPostgresDriver()));
        LOG.info("Redis Host: " + config.getRedisHost());
        LOG.info("Postgres Host: " + config.getPostgresHost());

        UserRedisHandler userRedisHandler = new UserRedisHandler(this.redisClient);
        UserJdbcHandler userJdbcHandler = new UserJdbcHandler(this.jdbcClient);
        this.userHandler = new UserHandler(userRedisHandler, userJdbcHandler);

        Credential credential = getGoogleApisCredential(config);
        GMailServiceHandler mailServiceHandler = new GMailServiceHandler(vertx, credential, config);
        GCSAuthHandler gcsAuthHandler = new GCSAuthHandler(vertx, redisClient, credential, config);

        RentalRedisHandler rentalRedisHandler = new RentalRedisHandler(this.redisClient);
        RentalJdbcHandler rentalJdbcHandler = new RentalJdbcHandler(this.jdbcClient);
        this.rentalHandler = new RentalHandler(rentalRedisHandler, rentalJdbcHandler, gcsAuthHandler, userHandler);

        UserAuthRedisHandler userAuthRedisHandler = new UserAuthRedisHandler(this.redisClient);
        UserAuthJdbcHandler userAuthJdbcHandler = new UserAuthJdbcHandler(this.jdbcClient);
        this.jdbcAuthProvider = createJdbcAuthProvider();
        this.userAuthHandler = new UserAuthHandler(userAuthRedisHandler, userAuthJdbcHandler, mailServiceHandler, this.jdbcAuthProvider);

        this.sessionHandler = SessionHandler.create(RedisSessionStore.create(vertx, redisClient, config.getSessionRetryTimeout())).setSessionTimeout(config.getSessionTimeout());
        this.userSessionHandler = UserSessionHandler.create(jdbcAuthProvider);
        this.redirectAuthHandler = RedirectAuthHandler.create(jdbcAuthProvider, "/login-view/login.html");

        Router router = createRouter();

        /*
        vertx.createHttpServer(new HttpServerOptions().setSsl(true).setKeyStoreOptions(new JksOptions()
                .setPath("/Users/jtan/IdeaProjects/Journey/ssl/journey-rentals-dev.jks")
                .setPassword("jtanFoundJourney17!")
        )).requestHandler(router::accept).listen(8443, result -> {
            if (result.succeeded()) {
                LOG.info("Server started");
            } else {
                LOG.error(result.cause().getMessage());
            }
        });
        */

        vertx.createHttpServer().requestHandler(router::accept)
            .listen(8080, result -> {
                if (result.succeeded()) {
                    future.complete();
                } else {
                    future.fail(result.cause());
                }
            }
        );
    }

    private Credential getGoogleApisCredential(Config config) throws Exception {
        GoogleAuthorizationCodeFlow authorizationFlow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(),
                new JacksonFactory(),
                config.getGoogleApiClientId(),
                config.getGoogleApiClientSecret(),
                Lists.newArrayList(config.getGoogleApiScopes()))
                .setDataStoreFactory(new FileDataStoreFactory(new File(config.getGoogleApiCredPath())))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        Credential credential = authorizationFlow.loadCredential(config.getGoogleApiCredUser());
        if (credential == null) {
            throw new Exception("Failed to load credential!");
        }
        return credential;
    }

    private JDBCAuth createJdbcAuthProvider() {
        JDBCAuth authProvider = JDBCAuth.create(vertx, this.jdbcClient);
        authProvider.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM eventbus.USERS WHERE ID=?");
        authProvider.setNonces(new JsonArray().add("KuMLwD0j1rB1yx0iOc").add("uDcCj0SkINwqOzxxGI"));
        return authProvider;
    }

    private void redirectHttpToHttps(RoutingContext context) {
        HttpServerRequest request = context.request();
        if (request.headers().contains("X-Forwarded-Proto")
                && request.getHeader("X-Forwarded-Proto").equals("http")) {
            context.response().setStatusCode(302)
                    .putHeader("location", request.absoluteURI().replace("http://", "https://"))
                    .end();
        } else {
            context.next();
        }
    }

    private Router createRouter() {
        Router router = Router.router(vertx);

        router.route("/.well-known/acme-challenge/*").handler(StaticHandler.create().setWebRoot("../../webroot"));
        router.route().handler(this::redirectHttpToHttps);
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

        router.route("/*").handler(StaticHandler.create().setCachingEnabled(false).setWebRoot("../../webroot"));

        return router;
    }

}
