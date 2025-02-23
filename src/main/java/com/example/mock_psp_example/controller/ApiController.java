package com.example.mock_psp_example.controller;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

@RestController
public class ApiController {
    Map<String, String> paymentTokenStorage = new ConcurrentHashMap<>();
    Set<String> idempotencyStorage = new ConcurrentSkipListSet<>();

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

        return new ResponseEntity<>(ResDto.builder()
                .paymentToken(paymentToken)
                .resultCode("OK").build(), HttpStatus.OK);
    }

    @PostMapping("/api/payment")
    public String handlePay(@RequestBody PayReqDto payReqDto) {
        String paymentToken = payReqDto.getPaymentToken();
        if(!paymentTokenStorage.containsKey(paymentToken)) {
            throw new IllegalArgumentException("INVALID Payment Token");
        }
        String redirectURL = paymentTokenStorage.get(paymentToken);
        return "redirect:/"
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
}
