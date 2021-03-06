package com.journey.webserver.verticle;

import com.journey.webserver.util.Runner;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;


/**
 * Created by jtan on 6/2/17.
 */
public class MainVerticle extends AbstractVerticle {

    private final static Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Future<Void> future) {
        vertx.deployVerticle(ServerVerticle.class.getName(), new DeploymentOptions(), res -> {
            if (res.succeeded()) {
                LOG.info("Server Verticle Started: " + res.result());
            } else {
                LOG.error(res.cause());
            }
        });
    }

    public static void main(String[] args) {
        System.setProperty("com.mchange.v2.c3p0.cfg.xml", "config/c3p0-config.xml");
        Runner.run(MainVerticle.class);
    }

}
