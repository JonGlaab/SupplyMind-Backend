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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class GmailAdapter implements EmailProvider {

    private static final String APPLICATION_NAME = "SupplyMind Platform";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList("https://www.googleapis.com/auth/gmail.modify");

    private Gmail gmailClient;

    public GmailAdapter() {
        try {
            this.gmailClient = authenticate();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Gmail Adapter", e);
        }
    }

    private Gmail authenticate() throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        // Load Credentials JSON
        InputStream in;
        String jsonConfig = System.getenv("GOOGLE_CREDENTIALS_JSON");

        if (jsonConfig != null && !jsonConfig.isEmpty()) {
            // Priority 1: Use Environment Variable if present
            System.out.println("Found GOOGLE_CREDENTIALS_JSON in environment. Using for authentication.");
            in = new ByteArrayInputStream(jsonConfig.getBytes(StandardCharsets.UTF_8));
        } else {
            // Priority 2: Fallback to classpath resource for local development
            System.out.println("GOOGLE_CREDENTIALS_JSON not found in environment. Attempting to load from classpath: /credentials.json");
            in = GmailAdapter.class.getResourceAsStream("/credentials.json");
            if (in == null) {
                throw new FileNotFoundException("Resource not found: /credentials.json. Please ensure the file exists in src/main/resources or the GOOGLE_CREDENTIALS_JSON environment variable is set.");
            }
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));


        // Setup the tokens directory
        File tokenFolder = new File(TOKENS_DIRECTORY_PATH);
        if (!tokenFolder.exists()) tokenFolder.mkdirs();

        // --- HEROKU BINARY BRIDGE ---
        String tokenDataEncoded = System.getenv("GMAIL_TOKEN_VALUE");
        if (tokenDataEncoded != null && !tokenDataEncoded.isEmpty()) {
            try {
                // Remove any accidental whitespace/newlines from copy-pasting
                String cleanBase64 = tokenDataEncoded.replaceAll("\\s", "");
                byte[] decodedBytes = java.util.Base64.getDecoder().decode(cleanBase64);

                // Write the actual binary file to Heroku's ephemeral disk
                java.nio.file.Files.write(
                        new File(tokenFolder, "StoredCredential").toPath(),
                        decodedBytes
                );
                System.out.println("✅ Successfully restored Gmail token from Environment Variable.");
            } catch (Exception e) {
                System.err.println("❌ Failed to decode GMAIL_TOKEN_VALUE: " + e.getMessage());
            }
        }
        // ----------------------------

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokenFolder))
                .setAccessType("offline")
                .build();

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    @Override
    public void sendEmail(String to, String subject, String body, File attachment) {
        try {
            System.out.println(" Sending email to: " + to);

            MimeMessage message = mailSender.createMimeMessage();
            // 'true' means multipart (supports attachments)
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // 'true' means HTML body

            // Handle Attachment
            if (attachment != null && attachment.exists()) {
                FileSystemResource file = new FileSystemResource(attachment);
                helper.addAttachment(file.getFilename(), file);
            }

            mailSender.send(message);
            System.out.println("✅ Email sent successfully!");

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }
}