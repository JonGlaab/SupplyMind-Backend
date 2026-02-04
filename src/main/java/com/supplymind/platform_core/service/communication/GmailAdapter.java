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
import org.apache.commons.codec.binary.Base64;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
@Primary
public class GmailAdapter implements EmailProvider {

    private static final String APPLICATION_NAME = "SupplyMind Platform";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.modify");

    // FIX 1: Removed 'final'. We will load this only when needed.
    private Gmail gmailClient;

    // FIX 2: Constructor is now empty.
    // This allows Spring to start PurchaseOrderController and AIContentService
    // even if Gmail isn't authenticated yet.
    public GmailAdapter() {
    }

    // FIX 3: Added a thread-safe getter to initialize the client only on first use.
    private synchronized Gmail getGmailClient() throws Exception {
        if (this.gmailClient == null) {
            this.gmailClient = authenticate();
        }
        return this.gmailClient;
    }

    private Gmail authenticate() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // 1. Load Credentials from Env Var or local resources
        String jsonConfig = System.getenv("GOOGLE_CREDENTIALS_JSON");
        GoogleClientSecrets clientSecrets;

        if (jsonConfig != null && !jsonConfig.isEmpty()) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new StringReader(jsonConfig));
        } else {
            InputStream in = GmailAdapter.class.getResourceAsStream("/credentials.json");
            if (in == null) throw new RuntimeException("credentials.json not found in resources");
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }

        // 2. Setup the tokens directory
        File tokenFolder = new File(TOKENS_DIRECTORY_PATH);
        if (!tokenFolder.exists()) tokenFolder.mkdirs();

        // 3. BRIDGE: Handle Base64 Token from Heroku
        String tokenDataEncoded = System.getenv("GMAIL_TOKEN_VALUE");
        if (tokenDataEncoded != null && !tokenDataEncoded.isEmpty()) {
            String cleanBase64 = tokenDataEncoded.replaceAll("\\s", "");
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64);
            java.nio.file.Files.write(
                    new File(tokenFolder, "StoredCredential").toPath(),
                    decodedBytes
            );
        }

        // 4. Build the Flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokenFolder))
                .setAccessType("offline")
                .build();

        // 5. Load the credential
        Credential credential = flow.loadCredential("user");

        // 6. Fallback for local dev (only runs if NOT on Heroku)
        if (credential == null && System.getenv("DYNO") == null) {
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        }

        if (credential == null) {
            throw new RuntimeException("Gmail credential could not be loaded. Please check GMAIL_TOKEN_VALUE.");
        }

        return new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    public void sendEmail(String to, String subject, String body, File attachment) {
        try {
            // FIX 4: Use the getter instead of the field directly
            Gmail client = getGmailClient();

            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            MimeMessage email = new MimeMessage(session);
            email.setFrom(new InternetAddress("me"));
            email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
            email.setSubject(subject);

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

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            email.writeTo(buffer);
            byte[] rawBytes = buffer.toByteArray();
            String encodedEmail = Base64.encodeBase64URLSafeString(rawBytes);

            Message message = new Message();
            message.setRaw(encodedEmail);

            // FIX 5: Use 'client' from the getter
            client.users().messages().send("me", message).execute();
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