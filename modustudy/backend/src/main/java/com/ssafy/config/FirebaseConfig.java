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
                    log.info("Firebase Admin SDK 초기화 완료");
                } else {
                    log.warn("Firebase 서비스 계정 파일을 찾을 수 없습니다. FCM 푸시 알림이 비활성화됩니다.");
                }
            }
        } catch (IOException e) {
            log.error("Firebase 초기화 실패: {}", e.getMessage());
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
                log.info("Firebase 서비스 계정 파일 로드: {}", credentialsPath);
                return new FileInputStream(credentialsPath);
            }

            // 2. classpath에서 찾기
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            if (resource.exists()) {
                log.info("Firebase 서비스 계정 파일 로드: classpath:firebase-service-account.json");
                return resource.getInputStream();
            }

            return null;
        } catch (IOException e) {
            log.error("Firebase 서비스 계정 파일 로드 실패: {}", e.getMessage());
            return null;
        }
    }
}
