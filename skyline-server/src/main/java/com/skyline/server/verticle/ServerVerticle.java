package com.skyline.server.verticle;

import com.skyline.server.Config;
import com.skyline.server.handler.GCSAuthHandler;
import com.skyline.server.handler.UserAuthHandler;
import com.skyline.server.handler.RentalHandler;
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
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * Created by jtan on 6/2/17.
 */
public class ServerVerticle extends AbstractVerticle {

//    private final String WEB_ROOT = "../../../../skyline-client"; // intellij-baseDir: src/main
//    private final String WEB_ROOT = "../../../skyline-client";
    private final static Logger LOG = LoggerFactory.getLogger(ServerVerticle.class);
    private JDBCClient jdbcClient;
    private RentalHandler rentalHandler;
    private JDBCAuth authProvider;
    private UserAuthHandler userAuthHandler;
    private GCSAuthHandler gcsAuthHandler;

    @Override
    public void start(Future<Void> future) throws Exception {

        Config config = Config.getInstance();
        RedisOptions redisConfig = new RedisOptions()
                .setHost(config.getRedisHost())
                .setAuth(config.getRedisAuth());
        RedisClient redisClient = RedisClient.create(vertx, redisConfig);
        this.rentalHandler = new RentalHandler(redisClient);

        this.jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:postgresql://104.196.206.124:5432/dev")
                .put("user", config.getPostgresUser())
                .put("password", config.getPostgresAuth())
                .put("driver_class", config.getPostgresDriver()));
        this.authProvider = createAuthProvider();
        this.userAuthHandler = new UserAuthHandler(this.authProvider, this.jdbcClient);
        this.gcsAuthHandler = new GCSAuthHandler(vertx, redisClient, config);

        Router router = createRouter();
        // HttpServer server = vertx.createHttpServer(new HttpServerOptions().setSsl(true).setKeyStoreOptions(
        //        new JksOptions().setPath("server-keystore.jks").setPassword("skyline")
        // ));
        // server.requestHandler(router::accept).listen(8443);

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

    private JDBCAuth createAuthProvider() {
        JDBCAuth authHandler = JDBCAuth.create(vertx, this.jdbcClient);
        authHandler.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM eventbus.USERS WHERE ID = ?");
        authHandler.setNonces(new JsonArray().add("KuMLwD0j1rB1yx0iOc").add("uDcCj0SkINwqOzxxGI"));
        return authHandler;
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        // Auth logic
        router.route().handler(CookieHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        router.route().handler(UserSessionHandler.create(authProvider));
        router.route("/api/public/login").handler(BodyHandler.create());
        router.post("/api/public/login").handler(userAuthHandler::authenticate);
        router.route("/api/public/register").handler(BodyHandler.create());
        router.post("/api/public/register").handler(userAuthHandler::register);

        router.route("/api/private/*").handler(RedirectAuthHandler.create(authProvider, "/login-view/login.html"));
        router.get("/api/private/gcstoken").handler(gcsAuthHandler::getAccessToken);

        // Main logic
        router.get("/api/public/rental/:rentalId").handler(rentalHandler::get);
        router.get("/api/public/discover").handler(rentalHandler::getMax);
        router.route("/api/private/rental").handler(BodyHandler.create());
        router.post("/api/private/rental").handler(rentalHandler::put);

        // Order is important, don't move the positions
        router.route("/post-view/post.html").handler(RedirectAuthHandler.create(authProvider, "/login-view/login.html"));
        router.route("/*").handler(StaticHandler.create().setCachingEnabled(false));

        return router;
    }

}
