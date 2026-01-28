package com.ssafy.config;

import com.ssafy.common.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Allow all preflight OPTIONS requests for CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Swagger
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-resources/**").permitAll()
                        // Actuator (헬스체크)
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        // SFU config for WebRTC clients
                        .requestMatchers("/api/v1/sfu/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        // 인증 없이 접근 가능
                        .requestMatchers("/api/v1/auth/**", "/oauth2/**", "/login/**").permitAll()
                        .requestMatchers("/api/v1/users").permitAll()
                        // News API - 테스트용 (나중에 인증 적용 가능)
                        .requestMatchers("/api/news/**", "/api/test/news/**").permitAll()
                        // Temporary: allow meeting APIs without auth for local testing
                        .requestMatchers("/api/v1/studies/*/meetings/**").permitAll()
                        // Study - TODO: 나중에 인증 적용 필요
                        .requestMatchers("/api/v1/my/**").permitAll()
                        .requestMatchers("/api/v1/study-templates/**").permitAll()
                        .requestMatchers("/api/v1/study/**").permitAll()
                        .requestMatchers("/api/v1/workspaces/**").permitAll()
                        .requestMatchers("/api/v1/studies/**").permitAll()
                        .requestMatchers("/files/**").permitAll()
                        // 퀴즈 코스
                        .requestMatchers("/api/v1/quiz-courses/**").permitAll()
                        // 게이미피케이션
                        .requestMatchers("/api/v1/gamification/test/**", "/api/v1/gamification/stats").permitAll()
                        // 데일리
                        .requestMatchers("/api/v1/dailies/**").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "https://localhost:3000",
                "http://localhost:3001",
                "https://localhost:3001",
                "http://localhost:3003",
                "https://localhost:3003",
                "http://192.168.100.90:3000",
                "https://192.168.100.90:3000",
                "https://modustudy.local:3000",
                "http://localhost:5173",
                "https://i14d106.p.ssafy.io"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}