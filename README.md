# FraudSentinel: Carrier Fraud Detection System

**Production-ready fraud detection platform for freight exchange platforms (WTRANSNET).**

Built with **Spring Boot 3.4.5**, **MongoDB**, **Domain-Driven Design**, and operational best practices.

---

## 🎯 What is FraudSentinel?

FraudSentinel detects and routes carrier fraud cases in real-time, solving the **WTRANSNET bottleneck:**
- ❌ **Before:** Fraud cases waited in queue for days (manual routing)
- ✅ **After:** Cases auto-route to correct department in MINUTES with SLA tracking

**3 Fraud Detection Rules:**
1. **Payment Reconciliation** — Detects carriers with 80%+ unpaid rate
2. **Offer Price Escalation** — Detects price manipulation
3. **Complaint Accumulation** — Detects complaint patterns (5+ open, 10+/week, 20+/month, 2+ accidents, 3+ disputes)

**Auto-Routing to Departments:**
- LEGAL (disputes, critical alerts)
- INSURANCE (accidents)
- PAYMENT_RECONCILIATION (payment failures)
- FRAUD_INVESTIGATION (price anomalies)
- COMPLIANCE_REVIEW (incident volume)
- OPERATIONS_MANAGEMENT (high-volume complaints)
- SALES (churn risk: <10 offers/month)
- ACCOUNT_MANAGEMENT (upsell: >100 offers/month)

---

## 🏗️ Architecture


