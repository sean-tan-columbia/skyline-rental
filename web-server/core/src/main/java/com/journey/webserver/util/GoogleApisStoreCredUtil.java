package com.journey.webserver.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.common.collect.Lists;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.journey.webserver.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class GoogleApisStoreCredUtil {

    public void generateGoogleApisStoredCred(String env) throws Exception {
        Config config = Config.getInstance(env);
        String redirectUrl = config.getServerHostName();
        System.out.println("ClientId: " + config.getGoogleApiClientId());
        System.out.println("ClientSecret: " + config.getGoogleApiClientSecret());
        System.out.println("CredPath: " + config.getGoogleApiCredPath());

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
        if (credential != null) {
            credential.refreshToken();
            System.out.println("AccessToken: " + credential.getAccessToken());
            System.out.println("RefreshToken: " + credential.getRefreshToken());
            return;
        }

        String authorizeUrl = authorizationFlow.newAuthorizationUrl().setRedirectUri(redirectUrl).build();
        System.out.println("Paste this url in your browser: \n" + authorizeUrl + '\n');

        // Wait for the authorization code.
        System.out.println("Type the code you received here (Skip the # at the end): ");
        String authorizationCode = new BufferedReader(new InputStreamReader(System.in)).readLine();

        // Authorize the OAuth2 token.
        GoogleAuthorizationCodeTokenRequest tokenRequest = authorizationFlow.newTokenRequest(authorizationCode);
        tokenRequest.setRedirectUri(redirectUrl);
        GoogleTokenResponse tokenResponse = tokenRequest.execute();

        // Store the credential for the user.
        authorizationFlow.createAndStoreCredential(tokenResponse, config.getGoogleApiCredUser());
    }

    public static void main(String[] args) {
        try {
            GoogleApisStoreCredUtil googleApisStoreCredUtil = new GoogleApisStoreCredUtil();
            googleApisStoreCredUtil.generateGoogleApisStoredCred(args[0]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
