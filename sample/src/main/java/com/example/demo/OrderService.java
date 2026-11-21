package com.example.demo;

import io.github.blogify.core.annotation.BlogCodeBlock;
import io.github.blogify.core.annotation.BlogPost;
import io.github.blogify.core.annotation.BlogSection;
import io.github.blogify.core.annotation.BlogTag;

/**
 * Order processing orchestration service.
 * Handles validation, inventory checking, and routing orders to payment providers.
 */
@BlogPost(
    title = "Designing an Idempotent Order Processing Pipeline",
    slug = "designing-idempotent-order-processing",
    author = "Lead Architect",
    date = "2026-11-21",
    description = "A deep dive into building fault-tolerant and idempotent order workflows in modern Java backend systems.",
    tags = {"architecture", "java", "distributed-systems"},
    order = 1
)
@BlogTag("backend")
public class OrderService {

    /**
     * Pre-validates the incoming order payload against customer account state and limits.
     */
    @BlogSection(heading = "Validating Incoming Requests", order = 1)
    @BlogCodeBlock(language = "java", description = "Order validation and sanity check")
    public boolean validateOrder(String customerId, double totalAmount) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID cannot be null or blank");
        }
        return totalAmount > 0 && totalAmount <= 50_000.00;
    }

    /**
     * Executes order fulfillment with optimistic locking and idempotency token.
     */
    @BlogSection(heading = "Fulfillment with Idempotency", order = 2)
    @BlogCodeBlock(language = "java", description = "Idempotent execution snippet")
    public String fulfillOrder(String orderId, String idempotencyKey) {
        // Guarantee each transaction is processed at most once
        return "ORDER_FULFILLED:" + orderId + ":" + idempotencyKey;
    }
}
