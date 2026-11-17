package com.cabgo.web;

import com.cabgo.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final PaymentService payments;

    public ConfigController(PaymentService payments) {
        this.payments = payments;
    }

    @GetMapping
    public Map<String, Object> config() {
        return Map.of("stripeEnabled", payments.enabled(),
                "stripePublishableKey", payments.enabled() ? payments.publishableKey() : "",
                "currency", payments.currency());
    }
}
