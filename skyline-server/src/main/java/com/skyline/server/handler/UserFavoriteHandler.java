package com.skyline.server.handler;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;

/**
 * Created by jtan on 7/21/17.
 */
public class UserFavoriteHandler {

    private final static Logger LOG = LoggerFactory.getLogger(UserAuthHandler.class);

    public void favor(RoutingContext context) {
        JsonObject jsonFavorite = context.getBodyAsJson();
        String rentalFavorite = jsonFavorite.getString("favorite", "");
        Cookie cookieFavorites = context.getCookie("favorites");
        String rentalFavorites = "";
        if (cookieFavorites != null && cookieFavorites.getValue().length() > 0 && rentalFavorite.length() > 0) {
            rentalFavorites += ("," + rentalFavorite);
        } else if (cookieFavorites != null && cookieFavorites.getValue().length() > 0) {
            rentalFavorites += cookieFavorites.getValue();
        } else if (rentalFavorite.length() > 0) {
            rentalFavorites += rentalFavorite;
        }
        context.addCookie(Cookie.cookie("favorites", rentalFavorites).setPath("/").setMaxAge(3600L * 24 * 365));
        context.response()
                .setChunked(true)
                .putHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*")
                .write("Cookie added -> favorite : " + rentalFavorites)
                .setStatusCode(200)
                .end();
    }

}
