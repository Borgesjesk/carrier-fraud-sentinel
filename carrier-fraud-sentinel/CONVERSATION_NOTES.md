# FraudSentinel - Conversation Notes & Decisions

## Session 1: Project Setup (May 14, 2025)
**Duration:** ~2 hours

### Key Decisions Made
1. ✅ **Option B Selected:** Mock WTRANSNET integration (not real API)
   - Reason: You don't work at WTRANSNET anymore, but will soon
   - Benefit: Self-contained demo, swappable architecture
   - Future: Will be trivial to replace mock with real API calls

2. ✅ **Jess Builds Backend, Claude Code Generates Frontend**
   - Why: You own the domain logic, understand every line
   - You use Claude Code for frontend (demonstrates AI fluency)
   - Result: Defensible, teachable code for professor & interviews

3. ✅ **Production-Grade from Day 1**
   - Not a bootcamp project, a product
   - Full validation, error handling, tests
   - Evolves over 1 month, ready to show

### Business Rules Confirmed (LOCKED)

**Rule 1: Payment Reconciliation**
- Carrier accepts offer but never pays
- Source: WTRANSNET (mocked)
- Alert: Unpaid rate > 80%

**Rule 2: Offer Price Escalation**
- Carrier sees cheaper offer, accepts inflated later
- Source: WTRANSNET visibility logs (mocked)
- Alert: Price increase > 20%

**Rule 3: Complaint Accumulation (MOST CRITICAL)**
- 5+ open cases simultaneously = ALERT
- 10+ mediated cases/week = ALERT
- 20+ cases/month = ALERT
- 2+ accidents/month = ALERT
- 3+ unresolved commercial disputes = CRITICAL ALERT

### Architecture Confirmed
- Java 21 + Spring Boot 3.4.5
- MongoDB (local dev, Atlas for prod)
- React + Vite (frontend)
- Docker Compose (one-command deploy)
- JUnit 5 + Mockito (tests)
- Conventional Commits (git history)

---

## Next Session: Phase 1 - Domain Models

**What Jess needs to answer:**
1. Transaction validation rules (required fields, valid ranges)
2. Complaint data structure confirmation
3. Any domain-specific rules I'm missing

**What we'll build:**
1. `Transaction.java` with validation
2. `Complaint.java` with time windows
3. `RiskAlert.java` as MongoDB entity
4. `AlertStatus.java` enum

**How it works:**
1. I explain the concept first (why immutability? why records?)
2. You ask questions
3. I show you a hint (not the full code)
4. You write it
5. We review, improve, commit

---

## Project Evolution Log
- **May 14:** Foundation & project setup ✅
- **May 15:** Phase 1 (Domain models)
- **May 16:** Phase 2 (Validation)
- **May 17-18:** Phase 3 (Fraud rules)
- **May 19:** Phase 4 (Service & integration)
- **May 20:** Phase 5 (REST API)
- **May 21-22:** Phase 6 (Tests)
- **May 23:** Phase 7 (Frontend with Claude Code)
- **May 24-25:** Phase 8 (Polish, Docker, final docs)
- **May 26:** Presentation prep

---

## Critical Contacts & Resources
- **Professor:** Excited about FraudSentinel (approved for Sprint 5)
- **IT Academy:** Sprint 5 deadline (locked, 1 month)
- **Target Interview:** WTRANSNET (Alpega Iberia PT/ES)
- **Capstone Value:** Production-ready fraud detection system

