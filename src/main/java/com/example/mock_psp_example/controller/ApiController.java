package com.example.mock_psp_example.controller;

import com.example.mock_psp_example.service.WebhookService;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@RestController
@CrossOrigin(origins = "*")
@Slf4j
@RequiredArgsConstructor
public class ApiController {
    private final WebhookService webhookService;
    private final Environment environment;
    Map<String, String> paymentTokenStorage = new ConcurrentHashMap<>();
    Set<String> idempotencyStorage = new ConcurrentSkipListSet<>();
    Set<String> paymentDoneChecker = new ConcurrentSkipListSet<>();

    @PostMapping("/api/payment/enroll")
    public ResponseEntity<ResDto> handleEnroll(@RequestHeader(value = "X-PopularPsp-Idempotency-Key") String idempotencyKey, @RequestBody EnrollReqDto reqDto) {
        if(idempotencyStorage.contains(idempotencyKey)) {
            return new ResponseEntity<>(ResDto.builder()
                    .paymentToken("")
                    .resultCode("")
                    .build(), HttpStatus.TOO_MANY_REQUESTS);
        }
        idempotencyStorage.add(idempotencyKey);

        String redirectUrl = reqDto.getRedirectUrl();
        String paymentToken = UUID.randomUUID().toString();
        paymentTokenStorage.put(paymentToken, redirectUrl);
        log.info("[PaymentToken - RedirectURL] {} - {}", paymentToken, redirectUrl);

        return new ResponseEntity<>(ResDto.builder()
                .paymentToken(paymentToken)
                .resultCode("OK").build(), HttpStatus.OK);
    }

    @PostMapping("/api/payment")
    public ResponseEntity<Void> handlePay(@RequestBody PayReqDto payReqDto) {
        String paymentToken = payReqDto.getPaymentToken();
        if(!paymentTokenStorage.containsKey(paymentToken)) {
            throw new IllegalArgumentException("INVALID Payment Token");
        }
        if(paymentDoneChecker.contains(paymentToken)) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, environment.getProperty("url.invalid-access"))
                    .build();
        }
        paymentDoneChecker.add(paymentToken);

        // TODO. 비동기로 웹훅을 호출하는 코드 작성하기
        int a = (int) (Thread.currentThread().getId() % 4);
        String status = a < 3 ? "SUCCESS" : "FAILED";
        WebhookEvenReqDto webhookEvenReqDto = WebhookEvenReqDto.builder()
                .paymentToken(paymentToken)
                .eventType("PAYMENT_STATUS_CHANGED")
                .status(status)
                .build();
        webhookService.sendWebhookEvent(webhookEvenReqDto);


        String redirectURL = paymentTokenStorage.get(paymentToken);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectURL + "?paymentToken=" + paymentToken)
                .build();
    }


    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class WebhookEvenReqDto {
        private String paymentToken;
        private String eventType;
        private String status;
        @Builder
        public WebhookEvenReqDto(String paymentToken, String eventType, String status) {
            this.paymentToken = paymentToken;
            this.eventType = eventType;
            this.status = status;
        }
    }


    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    private static class PayReqDto {
        private String paymentToken;
        private String cardSecret;

        @Builder
        private PayReqDto(String paymentToken, String cardSecret) {
            this.paymentToken = paymentToken;
            this.cardSecret = cardSecret;
        }
    }

    @Getter
    @NoArgsConstructor
    private static class ResDto {
        private String resultCode;
        private String paymentToken;

        @Builder
        private ResDto(String resultCode, String paymentToken) {
            this.resultCode = resultCode;
            this.paymentToken = paymentToken;
        }
    }

    @Getter
    @NoArgsConstructor
    private static class EnrollReqDto {
        private String redirectUrl;
        private String amount;
        private String currency;

        private List<PaymentItem> paymentItems;

        @Builder
        private EnrollReqDto(String redirectUrl, String amount, String currency, List<PaymentItem> paymentItems) {
            this.redirectUrl = redirectUrl;
            this.amount = amount;
            this.currency = currency;
            this.paymentItems = paymentItems;
        }
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @Getter
        public static class PaymentItem {
            private String paymentOrderId; // 멱등키
            private String sellerInfo;
            @Builder
            private PaymentItem(String paymentOrderId, String sellerInfo) {
                this.paymentOrderId = paymentOrderId;
                this.sellerInfo = sellerInfo;
            }
        }
    }
}
