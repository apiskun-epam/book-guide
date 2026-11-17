package com.bookshelf.auth;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Initialises the Firebase Admin SDK. The service account JSON path comes from
 * {@code FIREBASE_SERVICE_ACCOUNT_PATH}; the project id from {@code firebase.project-id}
 * (set in the gitignored application-local.yml).
 */
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseAuth firebaseAuth(
            @Value("${firebase.service-account-path:}") String serviceAccountPath,
            @Value("${firebase.project-id:}") String projectId) throws IOException {
        if (serviceAccountPath.isBlank() || projectId.isBlank()) {
            throw new IllegalStateException(
                    "Firebase is not configured: set FIREBASE_SERVICE_ACCOUNT_PATH and firebase.project-id");
        }
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream credentials = new FileInputStream(serviceAccountPath)) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(credentials))
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }
        return FirebaseAuth.getInstance();
    }
}
