package com.example.demo;

import io.github.blogify.core.annotation.BlogCodeBlock;
import io.github.blogify.core.annotation.BlogPost;
import io.github.blogify.core.annotation.BlogSection;

/**
 * Pluggable payment gateway client integrating with external payment networks.
 */
@BlogPost(
    title = "Resilient Payment Gateway Integration Patterns",
    slug = "resilient-payment-gateway-integration",
    author = "Payments Team",
    date = "2026-11-21",
    description = "Techniques for integrating external payment APIs with circuit breakers and fallback handlers.",
    tags = {"fintech", "resilience", "patterns"},
    order = 2
)
public class PaymentGateway {

    /**
     * Dispatches charge request wrapped in a circuit breaker.
     */
    @BlogSection(heading = "Circuit Breaker Protected Charging", order = 1)
    @BlogCodeBlock(language = "java", description = "Charge dispatch method")
    public String charge(String customerToken, double amount) {
        return "CHARGE_SUCCESS:" + customerToken + ":" + amount;
    }
}
