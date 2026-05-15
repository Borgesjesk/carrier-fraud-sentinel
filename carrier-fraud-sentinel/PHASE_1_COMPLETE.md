# Phase 1: Domain Model - COMPLETE ✅

## What We Built

### 6 Type-Safe Enums (Business Classifications)
1. **ComplaintType** — OPEN_CASE, ACCIDENT, COMMERCIAL_DISPUTE, INSURANCE, REVIEWING
2. **ComplaintStatus** — UNRESOLVED, SOLVED
3. **ResolutionReason** — 6 ways a complaint can be resolved
4. **AlertSeverity** — CRITICAL, HIGH, MEDIUM, LOW, INFO (determines SLA)
5. **Department** — 8 routing destinations (LEGAL, INSURANCE, PAYMENT_RECONCILIATION, etc.)
6. **AlertAssignmentStatus** — 6-state machine (UNASSIGNED → ASSIGNED → ACCEPTED → IN_PROGRESS → RESOLVED, + ESCALATED)

### 3 Domain Entities (Business Logic)

#### Transaction (Immutable Snapshot)
- **Purpose:** Represents a freight offer on WTRANSNET platform
- **Immutability:** All fields final, no setters, thread-safe
- **Validation:** 7 business invariants enforced in constructor
- **Computed Properties:** 3 fraud signals (payment success rate, incident ratio, avg price)
- **Why:** Audit trail (alert fired based on THIS state at THIS time)

#### Complaint (State Machine - Has Behavior)
- **Purpose:** Represents a transportista's complaint against non-payment/incident
- **States:** UNRESOLVED → SOLVED (with guarded transitions)
- **Behavior:** `resolve()`, `reopen()`, `getDaysOpen()`, `isUnresolvedDispute()`
- **Invariants:** Can't resolve without documentation + reason
- **Why:** Complaint is a PROCESS, not just data

#### RiskAlert (Operational Workflow - SLA Tracking)
- **Purpose:** Alert that fires from fraud rules, gets routed, assigned, and resolved
- **States:** UNASSIGNED → ASSIGNED → ACCEPTED → IN_PROGRESS → RESOLVED (or ESCALATED)
- **Behavior:** `accept(person)`, `startInvestigation()`, `resolve(notes)`, `escalate(dept)`
- **SLA Tracking:** 5 severity levels with different deadlines (15min for CRITICAL, 5days for LOW)
- **Metrics:** Time to Ownership, SLA Met?, Minutes Overdue
- **Why:** Solves WTRANSNET bottleneck — cases reach right person in MINUTES not DAYS

---

## Key Design Decisions

### 1. Immutability (Transaction)
- Thread-safe concurrent rule evaluation
- Audit trail (non-repudiation)
- Side-effect free functions
- No synchronization overhead

### 2. State Machines (Complaint & RiskAlert)
- Guarded transitions (can't skip states)
- Invariant enforcement (can't resolve without docs)
- Behavior encapsulated (methods on entities, not services)
- Self-documenting (state names are clear)

### 3. Validation vs. Business Rules
- **Validation:** Is data well-formed? (in constructor)
- **Business Rules:** What does data mean? (in fraud detection layer)
- **Key Insight:** A carrier with 500 offers/month is VALID but UNUSUAL (detected by rules, not rejected by validator)

### 4. Computed Properties
- `Transaction.getPaymentSuccessRate()` — used by Rule 1
- `Complaint.getDaysOpen()` — used by Rule 3 time windows
- `RiskAlert.isOverdue()` — used by dashboard SLA display
- Why: Single Responsibility (entity knows its own calculations)

### 5. Javadoc Everywhere
- Not just "what" but "why"
- Business context included
- Examples for complex methods
- Senior engineers write documentation

---

## Metrics

- **9 files committed** (6 enums + 3 entities)
- **1,200+ lines of code**
- **100% Javadoced**
- **No external dependencies** (pure Java)
- **Thread-safe by design**
- **Testable architecture** (no hidden state)

---

## What's Next (Phase 2)

### Input Validation Layer
- `TransactionValidator` — enforces all business rules
- `ComplaintValidator` — checks required documentation
- Custom exceptions with clear messages
- Tests: 50+ test cases (happy + unhappy paths)

---

## Interview Ready?

When asked "walk me through your fraud detection system":

> "I built a domain-driven fraud detection system with three layers:
> 
> **Domain Layer:** Type-safe enums, immutable domain entities, state machines
> - Transaction is immutable (thread-safe, audit trail)
> - Complaint is a state machine (UNRESOLVED → SOLVED with guarded transitions)
> - RiskAlert tracks operational workflow (auto-routing with SLA)
>
> **Why This Architecture:**
> - Immutability means concurrent rule evaluation is safe
> - State machines prevent invalid transitions
> - Validation separates "data integrity" from "business meaning"
> - Computed properties avoid calculation bugs
> - Type-safe enums prevent magic strings
>
> **Production Impact:**
> - Scales: immutable objects don't need synchronization
> - Maintains audit trail: state preserved
> - SLA compliant: alert reaches right person in minutes not days
> - Testable: no hidden state, easy to mock"

That's senior-level architecture thinking.

