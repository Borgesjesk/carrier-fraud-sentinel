# FraudSentinel

**Live app:** https://fraudsentinel-app.onrender.com  
**Live backend:** https://fraudsentinel-api-mpb5.onrender.com  
**Swagger UI:** https://fraudsentinel-api-mpb5.onrender.com/swagger-ui.html  
_(cold start ~30s on first request)_

**Project board:** https://github.com/users/Borgesjesk/projects/3  
**User stories:** [docs/USER_STORIES.md](docs/USER_STORIES.md)

[![CI](https://github.com/Borgesjesk/carrier-fraud-sentinel/actions/workflows/ci.yml/badge.svg)](https://github.com/Borgesjesk/carrier-fraud-sentinel/actions/workflows/ci.yml)

A carrier fraud detection platform with role-based access control, multi-channel case management, real-time alert routing, and a full auth stack including MFA.

Built as the final project for the IT Academy Barcelona Java Backend bootcamp. The design comes directly from six years of fraud investigation work at a European freight exchange, where I dealt daily with the patterns this system detects: carriers gaming payment terms, escalating offer prices, and accumulating complaints across multiple categories.

---

## Screenshots

### Login and password recovery
| Login | Forgot password |
|-------|-----------------|
| ![Login](docs/screenshots/01-login.png) | ![Forgot](docs/screenshots/02-forgot-password.png) |

### Multi-factor authentication
| MFA challenge | MFA setup QR | Backup codes |
|---------------|--------------|--------------|
| ![MFA challenge](docs/screenshots/03-login-mfa-challenge.png) | ![MFA setup](docs/screenshots/04-mfa-setup-qr.png) | ![Backup codes](docs/screenshots/05-mfa-backup-codes.png) |

### Dashboard and case management
| Dashboard (color-coded status + two-party columns) | Alert detail with timeline |
|-----------------------------------------------------|----------------------------|
| ![Dashboard](docs/screenshots/06-dashboard-admin.png) | ![Alert detail](docs/screenshots/07-alert-detail.png) |

### Rule engine playground
![Simulate fraud](docs/screenshots/08-simulate-fraud.png)

### Client complaint list
![Client complaints](docs/screenshots/09-client-complaints.png)

### Swagger UI and CI
| Swagger UI | GitHub Actions |
|------------|----------------|
| ![Swagger](docs/screenshots/10-swagger-ui.png) | ![CI](docs/screenshots/11-github-actions.png) |

### Project board and analytics
| Project board | Analytics |
|---------------|-----------|
| ![Project](docs/screenshots/12-project-board.png) | ![Analytics](docs/screenshots/13-analytics.png) |

### Alert timeline
![Timeline](docs/screenshots/14-alert-timeline.png)

---

## What it does

FraudSentinel scores carrier transactions in real time through a rule engine, generates alerts with calculated severity, and routes them to the responsible department automatically. From the moment a case enters the system — whether triggered by detection rules or submitted by a client — it follows a tracked lifecycle: routed, claimed, investigated, resolved, or escalated, with full audit trail, bidirectional communication, and time-stamped events.

Two distinct user flows:

**Internal staff** (Admin, Analyst, Compliance) see alerts filtered by their role's visible departments. They can claim a case, investigate it, resolve it, escalate, or transfer to another department. Every case has a comment thread where staff and the affected client can communicate. Staff can also manually run the rule engine on any transaction via the Simulate page.

**External clients** (carriers submitting complaints) log in to a separate flow. They cannot see other carriers' cases or internal notes. They submit a complaint with a description and supporting documents (PDF or images), then track the status of their case and respond to investigator questions through the comment thread.

---

## Why it exists

I spent six years at a European carrier matching platform, investigating fraud, KYC, and AML compliance for European carriers. The patterns this system detects — payment reconciliation gaps, price escalation, complaint accumulation — are the exact patterns I worked with daily.

The detection rules and department routing reflect how real freight fraud investigation actually works. The CLIENT role exists because carriers need a way to submit cases without staff manually entering them. The comment thread exists because investigation is conversation, not just status updates.

This is a portfolio project, but it's not generic. It's the system I wished existed.

---

## Tech stack

**Backend**
- Java 21 (Eclipse Temurin)
- Spring Boot 3.5.14 (LTS)
- MongoDB Atlas (replica set, eu-west-3)
- Spring Security with JWT (HS256), BCrypt password hashing
- HttpOnly + Secure + SameSite cookie authentication
- Bucket4j for rate limiting
- googleauth for TOTP-based MFA
- Multipart file upload with disk-based storage (Strategy pattern, swap-ready for S3)
- RFC 7807 Problem Details for all error responses
- Springdoc OpenAPI 2.8.4 for Swagger UI

**Frontend**
- React 19 with TypeScript 5 (strict mode)
- Vite 8
- Tailwind CSS v4 (dark mode by default)
- React Router 7 with role-based protected routes
- Axios with silent auto-refresh interceptor
- qrcode.react for inline MFA QR generation
- Recharts for analytics visualization
- Lucide React for icons

**DevSecOps**
- OWASP Dependency-Check (fails build on CVSS >= 7.0)
- SpotBugs + FindSecBugs for static analysis
- JaCoCo coverage gate
- CycloneDX SBOM generation
- GitHub Actions CI/CD (backend + frontend + Docker build)
- Deployed on Render (backend + static site)

---

## Auth stack

The auth layer went deep because carrier fraud investigation is where credentials matter most.

- **HttpOnly cookie JWT** — token invisible to JavaScript; XSS payloads that try `document.cookie` or `localStorage.getItem('token')` find nothing
- **Access token 1h + refresh token 7d** — short blast radius on token theft; refresh persisted in MongoDB so it can be revoked instantly
- **Refresh token rotation with theft detection** — every refresh issues a new token and revokes the old one; reusing a revoked token triggers revocation of ALL of the user's refresh tokens
- **TOTP-based MFA** — enrollment via inline QR code + 10 backup codes; login flow: password → MFA challenge → session
- **Rate limiting** — Bucket4j token buckets, 5 login attempts/min and 60 general requests/min per IP; 429 responses in RFC 7807 format
- **Password reset with time-limited tokens** — 15-minute TTL, silent-fail on unknown email (prevents user enumeration)
- **RBAC enforced server-side** — the backend queries only the alerts each role should see; the frontend never receives forbidden data
- **Path traversal protection** — file storage resolves paths and verifies containment inside the storage root; filenames use UUIDs

---

## Detection rules

Three rules implemented with the Strategy pattern. Adding a new rule is a new class implementing the same interface, no changes to the dispatcher.

| Rule | Triggers on | Severity logic | Routes to |
|------|-------------|----------------|-----------|
| PaymentReconciliationRule | High unpaid invoice ratio | 0.0 to 1.0 by percent unpaid | MEDIATION |
| OfferPriceEscalationRule | Offer price more than 50% above baseline | 0.0 to 1.0 by deviation | FRAUD_INVESTIGATION |
| ComplaintAccumulationRule | Multiple complaint categories accumulating | 0.0 to 1.0 by incident weight | LEGAL or INSURANCE |

Domain validation rejects impossible inputs at the boundary — for example, 25 incidents on 2 offers returns 422 with an RFC 7807 problem document.

Staff can run the rule engine interactively via the Simulate page: enter transaction attributes, watch the rules score the input in real time, and see the resulting alert on the dashboard.

---

## Role-based access control

| Role | Visible departments | Real-world use case |
|------|---------------------|---------------------|
| ADMIN | All 11 | Platform administrator |
| COMPLIANCE | LEGAL, COMPLIANCE_REVIEW, DEPARTMENT_MANAGER | Regulatory review and oversight |
| ANALYST | MEDIATION, FRAUD_INVESTIGATION, INSURANCE, CUSTOMER_SERVICE | Day-to-day investigation work |
| CLIENT | None (own cases only) | External carrier submitting a complaint |

CLIENT is fundamentally different from the others — they don't have departmental visibility. They access only their own submitted complaints, the documents they attached, and the comment thread on those cases. Authorization for CLIENT is enforced at the controller by comparing `authentication.getName()` against the alert's `createdBy` field.

---

## Architecture

```
src/main/java/com/carrierfraud/
├── domain/          Entities, enums, value objects, business rules
├── application/     Services, use cases, detection rules (Strategy pattern)
├── infrastructure/  MongoDB repositories + disk storage implementation
├── api/             REST controllers, DTOs, exception handlers
├── security/        Spring Security config, JWT, MFA, password reset, refresh tokens
├── audit/           Tamper-resistant audit log
└── config/          Cookie, CORS, MongoDB index configuration

frontend/
├── src/types/       TypeScript interfaces matching backend DTOs
├── src/api/         Axios client + service modules
├── src/auth/        AuthContext with three-state session machine
├── src/components/  Reusable components (Timeline, CommentsThread, ProtectedRoute)
└── src/pages/       Login, Dashboard, AlertDetail, Analytics, Simulate, MfaSetup, ...
```

---

## Key endpoints

**Authentication and account**
- `POST /api/v1/auth/login` — issues HttpOnly cookie or returns `{mfaRequired: true}`
- `POST /api/v1/auth/login/mfa` — completes MFA challenge
- `POST /api/v1/auth/refresh` — silent access-token refresh (called automatically on 401)
- `POST /api/v1/auth/forgot-password` — request password reset link
- `POST /api/v1/auth/reset-password` — apply reset with time-limited token
- `POST /api/v1/auth/mfa/setup` + `/verify-setup` — enroll TOTP with backup codes
- `GET /api/v1/auth/me` — session bootstrap
- `POST /api/v1/auth/logout` — clears cookies, revokes refresh token

**Alerts (staff)**
- `GET /api/v1/transactions/alerts` — list filtered by role
- `GET /api/v1/transactions/alerts/{id}` — single alert with RBAC check
- `PUT /api/v1/transactions/alerts/{id}/accept|investigate|resolve|escalate|transfer` — workflow actions
- `POST /api/v1/transactions/analyze` — run rule engine on a transaction (used by Simulate)

**Complaints (client)**
- `POST /api/v1/complaints` — multipart submission with description and documents
- `GET /api/v1/complaints/mine` — own cases ordered by date desc
- `GET /api/v1/complaints/{id}/documents/{docId}` — secure download

**Communication**
- `GET|POST /api/v1/alerts/{id}/comments` — comment thread
- `GET|POST /api/v1/alerts/{id}/notes` — internal notes (staff-only)
- `GET /api/v1/alerts/unread-counts` — unread badge counts
- `POST /api/v1/alerts/{id}/read` — mark as read

**Public**
- `GET /api/v1/info/features` — anonymous version + feature list

---

## Running locally

### Quick start with Docker (recommended)

```bash
git clone https://github.com/Borgesjesk/carrier-fraud-sentinel
cd carrier-fraud-sentinel
docker compose up -d
```

Backend on http://localhost:8080, Swagger UI at /swagger-ui.html. Four demo users are seeded automatically: `admin`, `analyst`, `compliance`, `client1`. Default passwords are in `docker-compose.yml` — override via `.env` for non-local use.

### Manual setup

**Prerequisites**
- Java 21 or newer
- Node 20 or newer
- MongoDB Atlas connection string (free tier works)

**Backend**
```bash
cp env.example .env
# Edit .env with your MONGODB_URI, JWT_SECRET, seed passwords
set -a; source .env; set +a
./mvnw spring-boot:run
```

**Frontend**
```bash
cd frontend
npm install
npm run dev
```

Frontend on http://localhost:5173. Expects `VITE_API_BASE_URL=http://localhost:8080` in `frontend/.env.local`.

---

## Testing

```bash
./mvnw test                                  # 139+ unit + integration tests
./mvnw verify -P security                    # full DevSecOps pipeline
./mvnw org.cyclonedx:cyclonedx-maven-plugin:makeAggregateBom  # SBOM
```

---

## About me

I'm Jess Borges — Spanish-Brazilian, six years in fraud investigation and AML compliance at a European freight exchange, now transitioning into cybersecurity through the Ironhack IFCT0109 program. My goal is SOC Analyst then DevSecOps.

This project sits at the intersection of where I came from and where I'm going.

---

## License

MIT