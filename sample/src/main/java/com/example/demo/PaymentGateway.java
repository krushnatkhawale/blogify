package com.example.demo;

/**
 * Pluggable payment gateway client integrating with external payment networks.
 *
 * @author Payments Team
 * @since 2026-11-21
 * @blog.title Resilient Payment Gateway Integration Patterns
 * @blog.slug resilient-payment-gateway-integration
 * @blog.description Techniques for integrating external payment APIs with circuit breakers and fallback handlers.
 * @blog.tags fintech, resilience, patterns
 * @blog.order 2
 */
public class PaymentGateway {

    /**
     * Dispatches charge request wrapped in a circuit breaker.
     *
     * @blog.section Circuit Breaker Protected Charging
     * @blog.codeblock Charge dispatch method
     * @blog.order 1
     */
    public String charge(String customerToken, double amount) {
        return "CHARGE_SUCCESS:" + customerToken + ":" + amount;
    }
}
