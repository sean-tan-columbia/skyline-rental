package com.skyline.server.verticle;

import com.skyline.server.Config;
import com.skyline.server.handler.RedisHandler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;

/**
 * Created by jtan on 6/2/17.
 */
public class ServerVerticle extends AbstractVerticle {

    private final String WEB_ROOT = "../../../../skyline-client"; // intellij-baseDir: src/main
    private final static Logger LOG = LoggerFactory.getLogger(ServerVerticle.class);
    private RedisHandler redisHandler;

    @Override
    public void start(Future<Void> future) throws Exception {

        Config config = Config.getInstance();
        RedisOptions redisConfig = new RedisOptions()
                .setHost(config.getRedisHost())
                .setAuth(config.getRedisAuth());
        this.redisHandler = new RedisHandler(RedisClient.create(vertx, redisConfig));

        Router router = createRouter();
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                future.complete();
                            } else {
                                future.fail(result.cause());
                            }
                        }
                );
    }

    private Router createRouter() {
        Router router = Router.router(vertx);
        router.route("/rental").handler(BodyHandler.create());
        router.post("/rental").handler(redisHandler::put);
        router.get("/rental/:rentalId").handler(redisHandler::get);
        router.get("/discover").handler(redisHandler::getMax);
        router.route("/*").handler(StaticHandler.create().setWebRoot(WEB_ROOT).setCachingEnabled(false));
        return router;
    }

}
