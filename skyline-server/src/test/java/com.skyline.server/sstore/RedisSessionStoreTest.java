package com.skyline.server.sstore;

import com.skyline.server.Config;
import com.skyline.server.sstore.impl.RedisSessionStoreImpl;
import com.sun.xml.internal.fastinfoset.sax.SystemIdResolver;
import com.sun.xml.internal.ws.api.message.ExceptionHasMessage;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.Session;
import io.vertx.redis.RedisClient;
import io.vertx.redis.RedisOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Created by jtan on 7/17/17.
 */
@RunWith(VertxUnitRunner.class)
public class RedisSessionStoreTest {

    Vertx vertx;
    Session session;
    RedisSessionStore redisSessionStore;

    @Before
    public void before(TestContext context) {
        vertx = Vertx.vertx();
        Config config;
        try {
            config = Config.getInstance();
        } catch (Exception e) {
            context.asyncAssertFailure();
            return;
        }
        RedisOptions redisConfig = new RedisOptions()
                .setHost(config.getRedisHost())
                .setAuth(config.getRedisAuth());
        RedisClient redisClient = RedisClient.create(vertx, redisConfig);
        this.redisSessionStore = RedisSessionStore.create(vertx, redisClient);
    }

    @After
    public void after(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }

    @Test
    public void testCreateSession(TestContext context) {
        Session session = redisSessionStore.createSession(30000L);
        context.assertNotNull(session);
        context.assertNotNull(session.id());
        context.assertEquals(session.timeout(), 30000L);
    }

    @Test
    public void testPutGet(TestContext context) {
        Session session = redisSessionStore.createSession(30000L);
        String sessionId = session.id();
        redisSessionStore.put(session, res -> {
            if (res.succeeded()) {
                System.out.println("Put");
                redisSessionStore.get(sessionId, res2 -> {
                    if (res2.succeeded()) {
                        context.assertNotNull(res2.result());
                        System.out.println(res2.result().id());
                        context.assertEquals(sessionId, res2.result().id());
                    } else {
                        res2.cause();
                    }
                });
            } else {
                System.out.println("Not Put");
                res.cause();
            }
        });
    }

}
