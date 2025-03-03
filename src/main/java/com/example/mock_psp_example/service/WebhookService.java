package com.example.mock_psp_example.service;

import com.example.mock_psp_example.controller.ApiController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    private final RestTemplate restTemplate;
    private final Environment environment;
    @Async
    public void sendWebhookEvent(ApiController.WebhookEvenReqDto event) {
        try {
            Thread.sleep(3000L);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<ApiController.WebhookEvenReqDto> request = new HttpEntity<>(event, headers);
            restTemplate.postForEntity(environment.getProperty("webhook.url"), request, String.class);
            log.info("Success to send Webhook: {}", event.getPaymentToken());
        } catch (Exception e) {
            log.error("Failed to send Webhook: {}", e.getMessage(), e);
        }
    }
}
