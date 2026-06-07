# 🛡️ FraudSentinel

**Real-time carrier fraud detection and case-routing platform for freight exchanges.**

Built to address a common bottleneck in European freight-exchange platforms: carrier dispute cases that sit in a single queue for manual triage, with no automated routing, prioritization, or pattern detection across cases.

---

## The Problem

Freight-exchange platforms — used by tens of thousands of transport companies across Europe — handle a high volume of carrier disputes monthly. The industry-wide pattern I observed during my six years as a fraud investigator in this sector: cases sit in a single queue for human triage, with no prioritization between low-value payment disputes and high-value fraud, and no detection of repeat-offender patterns across cases. FraudSentinel is one approach to closing that gap.

- **Days** of lead time before the right team even sees the case
- **No prioritization** between a €200 payment dispute and a €50K insurance fraud
- **No detection** of repeat-offender patterns across cases
- **No audit trail** for compliance / GDPR Article 32



---

## What It Does

| Rule | Trigger | Severity |
|---|---|---|
| **Payment Reconciliation** | Carrier has ≥80% unpaid invoice rate | ALERT |
| **Offer Price Escalation** | Price deviation outside historical band | ALERT |
| **Complaint Accumulation** | 5+ open / 10+ per week / 20+ per month | ALERT |
| **Repeat Accidents** | ≥2 accidents per month | ALERT |
| **Commercial Disputes** | ≥3 unresolved disputes | **CRITICAL** |

**Auto-routing by domain:**
LEGAL · INSURANCE · PAYMENT_RECONCILIATION · FRAUD_INVESTIGATION · COMPLIANCE_REVIEW · OPERATIONS · SALES (churn) · ACCOUNT_MANAGEMENT (upsell)

**Status flow:** `UNASSIGNED → ASSIGNED → ACCEPTED → IN_PROGRESS → RESOLVED`

---

## 🏗️ Architecture

Hexagonal architecture with explicit boundaries between domain logic, application services, and infrastructure adapters.

```
src/main/java/com/carrierfraud/
├── api/              # REST controllers + RFC 7807 error handling
├── application/      # Service orchestration, observers
├── domain/           # Entities, value objects, business rules
├── infrastructure/   # MongoDB adapters, audit observers
└── security/         # JWT, RBAC, Spring Security config
```

**Design Patterns Applied:**
- **Strategy** — pluggable detection rules
- **Observer** — alert routing to departments
- **Repository** — domain-infrastructure boundary
- **Aggregate Root** — case lifecycle integrity

---

## 🔐 Security & Quality Pipeline

This is a portfolio piece built to demonstrate **DevSecOps fundamentals**, not just CRUD plumbing.

Every Maven build runs the following gates — failing any one fails the build:

| Layer | Tool | Threshold | Purpose |
|---|---|---|---|
| **Authentication** | Spring Security + JWT (jjwt 0.12.6) | HS256, 60s clock skew, issuer-bound | Stateless RBAC |
| **Test Coverage** | JaCoCo 0.8.12 | Lines ≥50% (ratcheting toward 80%) | Regression confidence |
| **SAST** | SpotBugs 4.8 + FindSecBugs 1.13 | effort=Max, threshold=Medium | Static security analysis |
| **SCA** | OWASP Dependency-Check 10.0 | Fail on CVSS ≥7.0 (High/Critical) | Vulnerable dependency detection |
| **SBOM** | CycloneDX 2.9 | All formats, on every build | Supply-chain transparency |
| **Build Reproducibility** | Maven Enforcer 3.5 | Java 21+, Maven 3.9+ | Deterministic builds |

**Documented Security Decisions** (the interview-relevant part):
- `spotbugs-exclude.xml` — every suppression has written rationale (no blind silencing)
- `failBuildOnCVSS=7` — fintech-grade gate; blocks all High and Critical CVEs
- Audit log path is hardcoded constant — eliminates CWE-22 (path traversal) at the design level rather than mitigating it
- JWT secret has no default value — application fails to start without `JWT_SECRET`, preventing accidental hardcoded secrets reaching prod
- Authentication events log at WARN with remote IP — enables Wazuh/Splunk SIEM correlation for brute-force detection (MITRE ATT&CK T1110)
- Generic error messages on auth failures — defends against username enumeration (OWASP A07)

---

## 🚀 Quick Start

### Prerequisites
- Java 21+ (Temurin recommended)
- Maven 3.9+
- MongoDB Atlas cluster OR local MongoDB (Docker Compose included)
- NVD API key for OWASP scan ([request free key](https://nvd.nist.gov/developers/request-an-api-key))

### 1. Clone and configure environment

```bash
git clone https://github.com/Borgesjesk/Carrier-fraud-sentinel.git
cd Carrier-fraud-sentinel
cp env.example .env
# Edit .env: set MONGODB_URI, JWT_SECRET, SEED_*_PASSWORD, NVD_API_KEY
set -a; source .env; set +a
```

### 2. Run

```bash
export SPRING_PROFILES_ACTIVE=dev
./mvnw spring-boot:run
```

App is now serving at `http://localhost:8080`.

### 3. Test the login flow

```bash
# Should return 200 + JWT
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"$SEED_ADMIN_PASSWORD\"}"

# Should return 401 + RFC 7807 problem detail
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong-password-attempt"}'
```

### 4. Run the full quality pipeline

```bash
./mvnw clean verify
```

Reports generated:
- `target/site/jacoco/index.html` — coverage
- `target/spotbugsXml.xml` — SAST findings
- `target/dependency-check-report.html` — CVE scan
- `target/fraud-sentinel-sbom.json` — CycloneDX SBOM

---

## 🛠️ Tech Stack

**Core:** Java 21 · Spring Boot 3.5.13 · Spring Security 6 · MongoDB · Maven
**Security:** jjwt 0.12.6 · Spring Security · BCrypt (strength 12)
**Testing:** JUnit 5 · Mockito · Testcontainers · MockMvc
**DevSecOps:** JaCoCo · SpotBugs · FindSecBugs · OWASP Dependency-Check · CycloneDX

---

## 📅 Coverage Ratchet

The 50% coverage gate is a starting line, not the target. Each sprint raises it:

- **Sprint 5 (current):** ≥50% — security layer, domain entities
- **Sprint 6:** ≥65% — infrastructure adapters
- **Sprint 7:** ≥75% — REST controllers and integration tests
- **Sprint 8 target:** ≥80% — production-ready

---

## 🛣️ Roadmap

- [ ] ML layer — predictive risk scoring from historical case outcomes
- [ ] Carrier API — partner-facing webhooks for real-time alert delivery
- [ ] Refresh token mechanism + JTI claim for token revocation
- [ ] Rate limiting on `/api/v1/auth/**` (Bucket4j)
- [ ] Distributed tracing — MDC `traceId` enrichment across logs

---

## 👤 Built By

**Jess Borges** — transitioning into SOC / DevSecOps after 6+ years investigating freight fraud at Alpega Group.

- 🎓 Java Backend Bootcamp (IT Academy Barcelona, 2026)
- 🎓 IFCT0109 Cybersecurity Certification (Ironhack Barcelona, 500h)
- 🎓 Google Cybersecurity Certificate
- 🛡️ TryHackMe SOC Level 1 path · Wazuh SIEM home lab · MITRE ATT&CK mapping

[LinkedIn](https://linkedin.com/in/jessborgesb) · [GitHub](https://github.com/Borgesjesk)

---

*FraudSentinel is a portfolio project and is not affiliated with any commercial freight-exchange platform.*