package com.supplymind.platform_core.service.communication;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.supplymind.platform_core.dto.intel.email.EmailMessage;
import com.supplymind.platform_core.service.communication.EmailProvider;
import org.apache.commons.codec.binary.Base64;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;


import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

// @Service - Commented out as it was preventing the backend from starting.
@Primary
public class GmailAdapter implements EmailProvider {

    private static final String APPLICATION_NAME = "SupplyMind Platform";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.modify");

    private final Gmail gmailClient;

    public GmailAdapter() {
        try {
            this.gmailClient = authenticate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Gmail Adapter", e);
        }
    }

    private Gmail authenticate() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        InputStream in = GmailAdapter.class.getResourceAsStream("/credentials.json");
        if (in == null) throw new RuntimeException("credentials.json not found in resources");

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    public void sendEmail(String to, String subject, String body, File attachment) {
        try {
            // 1. Create a Jakarta Mail Session
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            // 2. Build the Email
            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress("me"));
            email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
            email.setSubject(subject);

            // 3. Handle Content
            MimeMultipart multipart = new MimeMultipart();

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(body, "text/html; charset=utf-8");
            multipart.addBodyPart(textPart);

            if (attachment != null) {
                MimeBodyPart attachPart = new MimeBodyPart();
                attachPart.attachFile(attachment);
                multipart.addBodyPart(attachPart);
            }

            email.setContent(multipart);

            // 4. Encode
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawBytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(rawBytes);

            // 5. Send via Google API
            Message message = new Message();
            message.setRaw(encodedEmail);

            gmailClient.users().messages().send("me", message).execute();
            System.out.println("✅ Email sent successfully to: " + to);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send email via Gmail Adapter", e);
        }
    }

    @Override
    public List<EmailMessage> searchEmails(String query) {
        return Collections.emptyList();
    }

    @Override
    public String getProviderName() {
        return "GMAIL";
    }
}