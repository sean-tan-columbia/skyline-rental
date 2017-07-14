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
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by jtan on 6/29/17.
 */
public class Collection {

    public static void main(String[] args) {
        generateHttpsServerKeyStore();
    }

    private static void generateGoogleApisStoredCred() {
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

    private static void generateHttpsServerKeyStore() {
        try {
            // Generate a self-signed key pair and certificate.
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, null);
            CertAndKeyGen keyPair = new CertAndKeyGen("RSA", "SHA1WithRSA", null);
            X500Name x500Name = new X500Name("localhost", "Engineering", "Skyline", "New York", "NY", "US");
            keyPair.generate(1024);
            PrivateKey privateKey = keyPair.getPrivateKey();
            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = keyPair.getSelfCertificate(x500Name, new Date(), (long) 365 * 24 * 60 * 60);
            keyStore.setKeyEntry("skyline-server-dev", privateKey, "skyline".toCharArray(), chain);
            keyStore.store(new FileOutputStream("skyline-server-dev.jks"), "skyline".toCharArray());
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException | NoSuchProviderException | InvalidKeyException | SignatureException ex) {
            Logger.getLogger(Collection.class.getName()).log(Level.SEVERE, "Failed to generate a self-signed cert and other SSL configuration methods failed.", ex);
        }
    }

}
