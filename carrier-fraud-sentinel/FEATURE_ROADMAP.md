# FraudSentinel - Feature Roadmap (1 Month Sprint)

## ✅ Phase 0: Foundation (COMPLETE)
- [x] Project initialized with git
- [x] Maven + Spring Boot 3.4.5
- [x] Application properties with all thresholds
- [x] Domain rules locked (Transaction validation, Complaints, Routing)
- [x] Database design documented
- [x] Architecture defined

## 📝 Phase 1: Domain Models (NEXT - 2 days)
**What:** Build the core Java entities that represent your fraud domain

**Classes to build:**
- `Transaction.java` - Enriched with validation rules
- `Complaint.java` - Case tracking with time windows
- `RiskAlert.java` - MongoDB document with routing fields
- `AlertStatus.java` & `ComplaintType.java` - Enums
- `Department.java` - Enum for routing

**Learning:** Immutability, validation annotations, records vs classes

**You write it.** I guide you. We commit after every class.

---

## 🔍 Phase 2: Validation & Exceptions (2 days)
**What:** Build the gatekeeper that prevents garbage data

**Classes to build:**
- `TransactionValidator.java` - Enforces all business rules
- `ComplaintValidator.java` - Validates complaint documentation
- Custom exceptions (InvalidTransactionException, etc.)
- `ValidationResult.java` - Structured error feedback

**Learning:** Defensive programming, TDD (tests first)

**Tests:** Happy path + 10+ unhappy paths per validator

---

## 🎯 Phase 3: Fraud Detection Rules (3 days)
**What:** Implement your three rules as Strategy pattern

**Classes to build:**
- `StrategyRule.java` - Interface (already exists)
- `PaymentReconciliationRule.java` - Rule 1 (80%+ unpaid)
- `OfferPriceEscalationRule.java` - Rule 2 (20%+ price jump)
- `ComplaintAccumulationRule.java` - Rule 3 (10/20/2/3 thresholds)
- `RuleEvaluationResult.java` - Score + which rules fired

**Learning:** Strategy pattern, injecting thresholds from properties, complex logic

**Tests:** Each rule tested with real-world scenarios

---

## 🔗 Phase 4: Service & Repository (2 days)
**What:** Wire rules to data layer

**Classes to build:**
- `FraudDetectionService.java` - Orchestrator (refactor existing)
- `AlertRoutingService.java` - Auto-routes to departments
- `RiskAlertRepository.java` - MongoDB interface
- `ComplaintRepository.java` - MongoDB interface

**Learning:** Dependency injection, Spring Data MongoDB, orchestration

---

## 🌐 Phase 5: REST API (2 days)
**What:** Expose fraud detection to the world

**Classes to build:**
- `TransactionController.java` - POST /api/transactions/analyze
- `AlertController.java` - GET /api/alerts, POST /api/alerts/{id}/accept
- `ComplaintController.java` - POST/GET complaints
- DTOs for request/response
- Global exception handler

**Learning:** Spring MVC, REST best practices, error handling

---

## ✅ Phase 6: Comprehensive Tests (2 days)
**What:** Prove everything works

**Test files to build:**
- Unit tests for each rule (50+ tests)
- Integration tests (full flow)
- Validation tests (boundary conditions)
- Repository tests
- Controller tests (with @WebMvcTest)

**Learning:** JUnit 5, Mockito, test doubles, integration testing

---

## 🎨 Phase 7: Frontend (1 day)
**What:** Use Claude Code to generate React SIEM dashboard

**What happens:**
1. You: "Generate a SIEM dashboard for FraudSentinel"
2. Claude Code: Generates React components
3. You: Analyze code, ask for adjustments
4. Connect to backend endpoints
5. Document the AI interaction

---

## 🐳 Phase 8: DevOps & Polish (2 days)
**What:** Ship it production-ready

**What to build:**
- `docker-compose.yml` (MongoDB + Spring Boot + React)
- `.dockerignore` + `Dockerfile`
- `README.md` (complete documentation)
- Scripts for local setup
- Pre-seeded test data

**Learning:** Docker, containers, one-command deployment

---

## 📊 Phase 9: Presentation Prep (1 day)
**What:** Tell the story

**Deliverables:**
- Git history walkthrough (why each commit?)
- Code review (domain knowledge explanation)
- Live demo (fraud alert fires → routes → person accepts)
- Business impact (how this solves WTRANSNET)
- Learning reflection (what you gained)

---

## Timeline
- **May 14** → Foundation ✅
- **May 15-16** → Phase 1 (Domain Models)
- **May 17-18** → Phase 2 (Validation)
- **May 19-21** → Phase 3 (Rules)
- **May 22-23** → Phase 4 (Service)
- **May 24-25** → Phase 5 (API)
- **May 26-27** → Phase 6 (Tests)
- **May 28** → Phase 7 (Frontend)
- **May 29-30** → Phase 8 (Docker)
- **May 31** → Phase 9 (Presentation)

**Deadline:** June 1 (locked)

---

## Success Criteria
- [x] Professor impressed ("Wow, this is real")
- [x] You understand every line
- [x] Production-grade code (validation, tests, error handling)
- [x] Live demo that works
- [x] Git history tells the story
- [x] Ready to show WTRANSNET
