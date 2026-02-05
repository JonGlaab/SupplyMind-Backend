package com.supplymind.platform_core.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleConfig {

    private static final String APPLICATION_NAME = "SupplyMind Platform";

    private static final String CREDENTIALS_FILE_PATH = "src/main/resources/credentials.json";

    @Bean
    public Gmail gmail() throws IOException, GeneralSecurityException {

        FileInputStream credentialStream = new FileInputStream(CREDENTIALS_FILE_PATH);

        GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream)
                .createScoped(Collections.singleton(GmailScopes.GMAIL_LABELS));


        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }
}