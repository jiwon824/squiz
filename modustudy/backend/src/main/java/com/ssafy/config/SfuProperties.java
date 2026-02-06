package com.ssafy.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.sfu")
public class SfuProperties {
    // SFU endpoint and ICE server configuration for WebRTC clients.
    private String baseUrl;
    private String controlUrl;
    private List<IceServer> iceServers = new ArrayList<>();

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getControlUrl() {
        return controlUrl;
    }

    public void setControlUrl(String controlUrl) {
        this.controlUrl = controlUrl;
    }

    public List<IceServer> getIceServers() {
        return iceServers;
    }

    public void setIceServers(List<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public static class IceServer {
        private String urls;
        private String username;
        private String credential;

        public String getUrls() {
            return urls;
        }

        public void setUrls(String urls) {
            this.urls = urls;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCredential() {
            return credential;
        }

        public void setCredential(String credential) {
            this.credential = credential;
        }
    }
}
