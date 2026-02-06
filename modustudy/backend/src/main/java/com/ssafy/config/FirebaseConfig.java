package com.ssafy.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Firebase Admin SDK 초기화 설정
 */
 @Configuration
 @Slf4j
 public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getCredentialsStream();

                if (serviceAccount != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
} else {
}
            }
        } catch (IOException e) {
}
    }

    /**
     * Firebase 서비스 계정 자격 증명 스트림 가져오기
     * 1. 환경변수로 지정된 경로
     * 2. classpath의 firebase-service-account.json
     */
    private InputStream getCredentialsStream() {
        try {
            // 1. 환경변수로 지정된 경로 확인
            if (credentialsPath != null && !credentialsPath.isEmpty()) {
                return new FileInputStream(credentialsPath);
            }

            // 2. classpath에서 찾기
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (resource.exists()) {
                return resource.getInputStream();
            }

            return null;
        } catch (IOException e) {
            return null;
        }
    }
}

