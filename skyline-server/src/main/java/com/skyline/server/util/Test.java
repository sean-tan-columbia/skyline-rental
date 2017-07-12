package com.skyline.server.util;

import com.google.api.SystemParameter;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by jtan on 6/29/17.
 */
public class Test {

    public static void main(String[] args) {
        String userId = "sysops";
        try {
            GoogleAuthorizationCodeFlow authorizationFlow = new GoogleAuthorizationCodeFlow.Builder(
                    new NetHttpTransport(),
                    new JacksonFactory(),
                    "",
                    "",
                    Lists.newArrayList("https://www.googleapis.com/auth/devstorage.read_write"))
                    .setDataStoreFactory(new FileDataStoreFactory(new File("/Users/jtan/IdeaProjects/skyline-server")))
                    .setAccessType("offline")
                    .setApprovalPrompt("force")
                    .build();
            Credential credential = authorizationFlow.loadCredential(userId);
            if (credential != null) {
                credential.refreshToken();
                System.out.println(credential.getAccessToken());
                System.out.println(credential.getRefreshToken());
                return;
            }

            String authorizeUrl = authorizationFlow.newAuthorizationUrl().setRedirectUri("http://localhost:8080").build();
            System.out.println("Paste this url in your browser: \n" + authorizeUrl + '\n');

            // Wait for the authorization code.
            System.out.println("Type the code you received here: ");
            String authorizationCode = new BufferedReader(new InputStreamReader(System.in)).readLine();

            // Authorize the OAuth2 token.
            GoogleAuthorizationCodeTokenRequest tokenRequest = authorizationFlow.newTokenRequest(authorizationCode);
            tokenRequest.setRedirectUri("http://localhost:8080");
            GoogleTokenResponse tokenResponse = tokenRequest.execute();

            // Store the credential for the user.
            authorizationFlow.createAndStoreCredential(tokenResponse, userId);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

}
