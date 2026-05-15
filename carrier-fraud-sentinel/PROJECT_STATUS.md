# FraudSentinel - Project Status & Next Steps

## ✅ Completed (Foundation)
- [x] Project initialized with git
- [x] Maven pom.xml with Spring Boot 3.4.5
- [x] Directory structure (domain, application, infrastructure, api, config)
- [x] application.properties with ALL YOUR THRESHOLDS
- [x] .gitignore configured
- [x] Git history clean (4 meaningful commits)
- [x] PROJECT_CONTEXT.md saved for continuous knowledge

## 📋 Your Tasks (Build Phase)

### Phase 1: Domain Models (TODAY)
We'll build the core entities that represent your fraud domain:

**Files to create:**
1. `Transaction.java` - Enriched with validation
2. `Complaint.java` - Case/complaint tracking
3. `RiskAlert.java` - MongoDB document
4. `AlertStatus.java` - Enum (NEW, IN_PROGRESS, RESOLVED, etc.)

**What you'll learn:**
- How domain models enforce business rules at the entity level
- Why immutability matters in fraud detection
- Validation annotations (@NotNull, @Min, etc.)

### Phase 2: Validation Layer (TOMORROW)
Build the gatekeeper that prevents garbage data:

**Files to create:**
1. `TransactionValidator.java` - All your business validation rules
2. Custom exceptions (InvalidTransactionException, etc.)

**What you'll learn:**
- Defensive programming (assume all input is hostile)
- How to validate complex business logic (not just null checks)
- Writing tests BEFORE implementation (TDD)

### Phase 3: Fraud Rules (DAYS 3-4)
Implement your three rules as Strategy pattern:

**Files to create:**
1. `StrategyRule.java` (interface)
2. `PaymentReconciliationRule.java` - Rule 1
3. `OfferPriceEscalationRule.java` - Rule 2
4. `ComplaintAccumulationRule.java` - Rule 3

**What you'll learn:**
- Strategy pattern (extensible, testable)
- How to inject thresholds from properties
- Complex business logic detection

### Phase 4: Service & Integration (DAY 5)
Orchestrate everything:

**Files to create:**
1. `FraudDetectionService.java` - Orchestrator
2. Repository interfaces
3. MongoDB integration

### Phase 5: REST API (DAY 6)
Expose to the world:

**Files to create:**
1. `TransactionController.java`
2. Request/Response DTOs
3. Global exception handling

### Phase 6: Comprehensive Tests (DAYS 7-8)
Prove it works:

**Files to create:**
1. Unit tests for each rule (happy + unhappy)
2. Integration tests
3. Validation tests

### Phase 7: Frontend (DAY 9)
Use Claude Code:

**What happens:**
1. We generate React SIEM dashboard
2. You analyze & adjust
3. Document the process

### Phase 8: Final Polish (DAY 10)
Ship it:

**What happens:**
1. Docker Compose setup
2. README documentation
3. Git cleanup
4. Presentation materials

---

## 🎯 Starting RIGHT NOW

**Question 1: What makes a Transaction VALID?**

From your Alpega brain, list the validation rules:

```
A valid Transaction must have:
- carrierName: [YOUR RULE]
- transportName: [YOUR RULE]
- failedPayments: [YOUR RULE]
- succeededPayments: [YOUR RULE]
- offerPrice: [YOUR RULE]
- numberOfOffers: [YOUR RULE]
- reportedIncidents: [YOUR RULE]
- ... etc
```

Example answers:
- carrierName: not null, not empty, 1-255 chars
- offerPrice: >= 0 and <= 50000
- numberOfOffers: >= 0 and <= 10000

**I need YOUR business rules, not generic ones.**

Once you answer, we'll code Phase 1 together. 🚀

---

**Project Location:** `/home/claude/carrier-fraud-sentinel`  
**Git Status:** Clean, 4 commits  
**Next Action:** Answer validation rules → We code domain models  
**Timeline:** 10 days to production  
