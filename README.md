# FraudSentinel 🚨

Real-time freight carrier fraud detection API built from 6 years of operational fraud investigation experience.

## Tech Stack
- Java 21 + Spring Boot 3.4.5
- MongoDB Atlas
- REST API

## Detection Rules
- **FailedPaymentsRule** — scores carriers with repeated payment failures
- **MarketPriceRule** — flags offers significantly above market average
- **HighOfferCountRule** — detects abnormal offer volume (shadow brokering)

## Endpoints
- `POST /api/transactions/analyze` — analyze a transaction for fraud
- `GET /api/transactions/alerts` — retrieve all fraud alerts

## Run locally
```bash
./mvnw spring-boot:run
```

