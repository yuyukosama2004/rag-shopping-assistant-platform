package com.biyesheji.order.service;

import com.biyesheji.entity.OrderNotificationOutbox;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

@Component
public class OrderWebhookSender {
    private final RestClient restClient;
    @Getter private final boolean enabled;
    private final String url;
    private final String secret;

    public OrderWebhookSender(RestClient.Builder builder,
                              @Value("${notification.webhook.enabled:false}") boolean enabled,
                              @Value("${notification.webhook.url:}") String url,
                              @Value("${notification.webhook.secret:}") String secret) {
        this.restClient = builder.build();
        this.enabled = enabled;
        this.url = url;
        this.secret = secret;
    }

    @PostConstruct
    void validate() {
        if (!enabled) return;
        if (!StringUtils.hasText(url) || !StringUtils.hasText(secret) || secret.length() < 32) {
            throw new IllegalStateException("启用订单 Webhook 时必须配置 URL 和至少32字符的签名密钥");
        }
        String scheme = URI.create(url).getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalStateException("订单 Webhook URL 仅支持 http/https");
        }
    }

    public void send(OrderNotificationOutbox event) {
        restClient.post().uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Id", event.getEventId())
                .header("X-Webhook-Event", event.getEventType())
                .header("X-Webhook-Signature", "sha256=" + sign(event.getPayload()))
                .body(event.getPayload())
                .retrieve().toBodilessEntity();
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Webhook 签名失败", e);
        }
    }
}
