---
title: "Synchronous Direct Billing Engine (Legacy Architecture)"
slug: "legacy-synchronous-billing"
author: "Legacy Billing Team"
date: "2024-03-15"
description: "Original monolithic synchronous payment processing engine. Kept for historical reference."
tags: [legacy, payments, monolith]
version: "v0.8.0"
commit: "1a2b3c4"
repoUrl: "https://github.com/example/blogify"
status: "retired"
supersededBy: "resilient-payment-gateway"
supersededMessage: "This architecture was decommissioned in Q3 2025. Please refer to Resilient Payment Gateway for the current multi-PSP mesh."
problem: |
  Single-threaded synchronous HTTP invocations to banking APIs held application threads open during carrier delays,
  resulting in thread pool exhaustion during peak flash sales.
insight: |
  Basic connection timeouts prevented permanent hangs, but could not prevent thread-pool saturation under sustained load.
takeaways:
  - "Synchronous payment processing does not scale to high-concurrency e-commerce workloads."
  - "Monolithic single-gateway integrations create critical business single points of failure."
---

## Legacy Implementation Notes
This article documents the legacy payment flow prior to the rollout of circuit breakers and distributed routing.

```java
@Deprecated
public void chargeLegacy(Customer customer, BigDecimal amount) {
    // Synchronous blocking call without circuit breaker protection
    bankHttpClient.postDirectCharge(customer.accountId(), amount);
}
```
