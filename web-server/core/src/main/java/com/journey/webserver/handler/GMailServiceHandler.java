package com.journey.webserver.handler;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.journey.webserver.Config;
import com.journey.webserver.model.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GMailServiceHandler {

    private final static Logger LOG = LoggerFactory.getLogger(GMailServiceHandler.class);
    private static final String FROM_EMAIL = "support@journey.rentals";
    private final Vertx vertx;
    private final Gmail service;
    private final String serverHostName;

    public GMailServiceHandler(Vertx vertx, Credential credential, Config config) throws Exception {
        this.vertx = vertx;
        this.service = new Gmail.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("Journey Rentals GMail Service")
                .build();
        this.serverHostName = config.getServerHostName();
    }

    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private void sendMessage(Gmail service, MimeMessage emailContent, Handler<AsyncResult<String>> resultHandler) {
        vertx.<Message>executeBlocking(future -> {
            Message message = new Message();
            try {
                message = createMessageWithEmail(emailContent);
                message = service.users().messages().send("me", message).execute();
            } catch (Exception e) {
                e.getCause();
            }
            future.complete(message);
        }, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result().getId()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
                LOG.error("Failed to send email!");
            }
        });
    }

    void sendRegisterMail(String toEmail, User user, String salt, Handler<AsyncResult<String>> resultHandler) {
        String subject = "Welcome to Journey Rentals";
        String template = "Hi %s,\n\n" +
                "Thank you for signing up on Journey Rentals! " +
                "To continue and finish the registration, please click or copy-paste the following link to you browser.\n\n" +
                "%s/#/set_password/c/%s\n\n" +
                "This link will be expired in an hour.\n\n" +
                "Enjoy your journey!\n" +
                "The Journey Rentals Team";
        String body = String.format(template, user.getName(), serverHostName, salt);
        try {
            MimeMessage mimeMessage = createEmail(toEmail, FROM_EMAIL, subject, body);
            sendMessage(service, mimeMessage, resultHandler);
        } catch (MessagingException e) {
            LOG.error(e.getMessage());
        }
    }

    void sendResetEmail(String toEmail, User user, String salt, Handler<AsyncResult<String>> resultHandler) {
        String subject = "Reset Your Journey Rentals Password";
        String template = "Hi %s,\n\n" +
                "We received a request to reset your Journey Rentals password. " +
                "If you did not request a password reset, please ignore this email and take no further actions. " +
                "To continue and reset your password, please click or copy-paste the following link to you browser.\n\n" +
                "%s/#/set_password/u/%s\n\n" +
                "This link will be expired in an hour.\n\n" +
                "Enjoy your journey!\n" +
                "The Journey Rentals Team";
        String body = String.format(template, user.getName(), serverHostName, salt);
        try {
            MimeMessage mimeMessage = createEmail(toEmail, FROM_EMAIL, subject, body);
            sendMessage(service, mimeMessage, resultHandler);
        } catch (MessagingException e) {
            LOG.error(e.getMessage());
        }
    }

}
