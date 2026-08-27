package com.devsleuth.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "devsleuth.github")
public class GitHubProperties {

    private static final Logger log = LoggerFactory.getLogger(GitHubProperties.class);

    private String clientId;
    private String clientSecret;
    private String appId;
    private String privateKeyPath;
    private String webhookSecret;

    /** Logs OAuth config presence at startup (client id is public; the secret value is never logged). */
    @PostConstruct
    void logConfig() {
        boolean secretSet = clientSecret != null && !clientSecret.isBlank();
        log.info("GitHub OAuth config -> clientId='{}', clientSecret={}",
                clientId, secretSet ? "set (" + clientSecret.length() + " chars)" : "MISSING");
    }

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
