package com.vn.vnpay.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "vn-pay")
@Getter
@Setter
public class VnPayConfig {
    private String payUrl;
    private String returnUrl;
    private String apiUrl;
    private String tmnCode;
    private String secretKey;
    private String version;
    private Command command;
    private String orderType;
    private String currCode;

    @Getter
    @Setter
    public static class Command {
        private String pay;
        private String query;
        private String refund;
    }
}