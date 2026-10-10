package com.reelshop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.Map;

@Slf4j
@Service
public class SmsService {

    @Value("${app.msg91.auth-key}")
    private String authKey;

    @Value("${app.msg91.template-id}")
    private String templateId;

    @Value("${app.msg91.sender-id}")
    private String senderId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Async
    public void sendOtp(String phoneNumber, String otp) {
        // In dev/test, just log — no real SMS sent
        if (authKey == null || authKey.isBlank()) {
            log.info("──────────────────────────────");
            log.info("DEV MODE — OTP for +91{}: {}", phoneNumber, otp);
            log.info("──────────────────────────────");
            return;
        }

        try {
            String url = "https://api.msg91.com/api/v5/otp";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authkey", authKey);

            Map<String, String> body = Map.of(
                "template_id", templateId,
                "mobile",      "91" + phoneNumber,
                "otp",         otp,
                "sender",      senderId
            );

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("MSG91 response for {}: {}", phoneNumber, response.getStatusCode());

        } catch (Exception e) {
            // Don't crash the OTP flow if SMS provider is down
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
        }
    }
}
