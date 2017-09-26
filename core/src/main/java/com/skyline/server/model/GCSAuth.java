package com.skyline.server.model;

/**
 * Created by jtan on 7/6/17.
 */
public class GCSAuth {

    private final String accessToken;

    public GCSAuth(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

}
