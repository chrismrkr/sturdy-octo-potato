package com.example.mock_psp_example.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@CrossOrigin(origins = "*")
public class ViewController {
    @GetMapping("/payment")
    public ModelAndView showPaymentPage(@RequestParam("paymentToken") String paymentToken) {
        ModelAndView mav = new ModelAndView("payment"); // templates/payment.html 렌더링
        mav.addObject("paymentToken", paymentToken); // paymentToken을 모델에 추가
        return mav;
    }

    @GetMapping("/payment/error")
    public ModelAndView showInvalidPaymentPage() {
        ModelAndView modelAndView = new ModelAndView(("payment-error"));
        return modelAndView;
    }
}
