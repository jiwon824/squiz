package com.ssafy.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
public class RestTemplateConfig {

    @Value("${app.sfu.allow-insecure:false}")
    private boolean allowInsecure;

    @Bean
    public RestTemplate restTemplate() {
        if (allowInsecure) {
            try {
                TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public X509Certificate[] getAcceptedIssuers() {
                                return new X509Certificate[0];
                            }
                        }
                };
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new SecureRandom());
                return new RestTemplate(new InsecureSslRequestFactory(
                        sslContext.getSocketFactory(),
                        (hostname, session) -> true
                ));
            } catch (Exception e) {
                // fallback to default if SSL setup fails
            }
        }
        return new RestTemplate();
    }

    private static class InsecureSslRequestFactory extends SimpleClientHttpRequestFactory {
        private final SSLSocketFactory sslSocketFactory;
        private final HostnameVerifier hostnameVerifier;

        private InsecureSslRequestFactory(SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
            this.sslSocketFactory = sslSocketFactory;
            this.hostnameVerifier = hostnameVerifier;
        }

        @Override
        protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
            if (connection instanceof HttpsURLConnection httpsConnection) {
                httpsConnection.setSSLSocketFactory(sslSocketFactory);
                httpsConnection.setHostnameVerifier(hostnameVerifier);
            }
            super.prepareConnection(connection, httpMethod);
        }
    }
}
