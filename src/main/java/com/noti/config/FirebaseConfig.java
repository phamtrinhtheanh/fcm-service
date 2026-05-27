package com.noti.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;

import java.io.*;

@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.project-id:}")
    private String projectId;

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${firebase.credentials-json:}")
    private String credentialsJson;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!StringUtils.hasText(projectId)) {
            log.info("[Firebase] Chua cau hinh, app chay khong FCM.");
            return null;
        }

        try {
            GoogleCredentials credentials;
            if (StringUtils.hasText(credentialsJson)) {
                credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(credentialsJson.getBytes()));
            } else if (StringUtils.hasText(credentialsPath)) {
                String path = credentialsPath;
                if (path.startsWith("file:")) {
                    path = path.substring("file:".length());
                }
                InputStream is;
                if (path.startsWith("classpath:")) {
                    is = new ClassPathResource(path.substring("classpath:".length())).getInputStream();
                } else {
                    is = new FileInputStream(path);
                }
                credentials = GoogleCredentials.fromStream(is);
            } else {
                log.warn("[Firebase] Thieu credentials, app chay khong FCM.");
                return null;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();

            FirebaseApp app = FirebaseApp.initializeApp(options, projectId);
            log.info("[Firebase] OK project={}", projectId);
            return app;
        } catch (Exception e) {
            log.warn("[Firebase] Loi: {}. App chay khong FCM.", e.getMessage());
            return null;
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(ObjectProvider<FirebaseApp> provider) {
        FirebaseApp app = provider.getIfAvailable();
        return app != null ? FirebaseMessaging.getInstance(app) : null;
    }
}
