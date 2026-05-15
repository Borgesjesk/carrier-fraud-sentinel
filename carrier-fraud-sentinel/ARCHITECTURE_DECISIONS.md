# FraudSentinel - Architecture Decisions & Design Patterns

## Design Decision 1: Immutable Domain Models

### Why Immutable?
1. **Thread Safety & Concurrency**
   - Multiple concurrent fraud checks = no race conditions
   - Dashboard queries while rules execute = safe
   
2. **Security & Non-Repudiation**
   - Audit trail: what was the original transaction state?
   - Can't "accidentally" modify historical data
   - Compliance: prove this alert was based on THIS data at THIS time
   
3. **Side-Effect-Free Functions**
   - Rules evaluate transactions → no hidden mutations
   - Easier to reason about (fraud scoring is deterministic)
   - Testable: same input = same output always

### Implementation
```java
// Use records for pure data (no behavior)
public record Transaction(
    String carrierName,
    String transportName,
    int failedPayments,
    // ... other fields
) {}

// Use immutable classes for entities with behavior
public class RiskAlert {
    private final String id;
    private final String carrierName;
    private final double riskScore;
    // All fields private final
    // Getters only (no setters)
    // Transformations return NEW objects
}
```

---

## Design Decision 2: Complaint Entity - Hybrid Approach

### Why NOT Pure Record?
Complaint has:
- Simple data (id, carrierName, type)
- **State transitions** (UNRESOLVED → SOLVED)
- **Specialized behavior** (accepting documentation, calculating daysOpen)
- **Invariant enforcement** (resolvedDate can only be set when status=SOLVED)

### Implementation: Class + Inner Records

```java
public class Complaint {
    // Core immutable fields
    private final String id;
    private final String carrierName;
    private final ComplaintType type;
    
    // Mutable tracking (but controlled)
    private ComplaintStatus status;  // UNRESOLVED → SOLVED
    private LocalDateTime resolvedDate;
    
    // Inner record for structured documentation
    public record SupportingDocumentation(
        String cmr,
        String albaran,
        String ordenDeCarga,
        List<CommunicationChannel> communications,
        List<String> photoUrls
    ) {}
    
    private SupportingDocumentation documentation;
    
    // Behavior: State transition
    public void resolve(ComplaintStatus newStatus, SupportingDocumentation docs) {
        // Enforce invariants
        if (newStatus == UNRESOLVED && docs == null) {
            throw new InvalidComplaintStateException("Cannot resolve without documentation");
        }
        this.status = newStatus;
        this.resolvedDate = LocalDateTime.now();
        this.documentation = docs;
    }
    
    // Computed property
    public int daysOpen() {
        LocalDateTime endDate = resolvedDate != null ? resolvedDate : LocalDateTime.now();
        return (int) ChronoUnit.DAYS.between(createdDate, endDate);
    }
}
```

---

## Design Decision 3: Validation Strategy

### Why Constructor-based + Builder Pattern?

**Constructor: Fail-Fast Principle**
```java
public class Transaction {
    public Transaction(
        String carrierName,
        String transportName,
        int failedPayments,
        // ... all fields
    ) {
        // FAIL IMMEDIATELY if invalid
        this.carrierName = Objects.requireNonNull(carrierName, "carrierName cannot be null");
        if (carrierName.isBlank() || carrierName.length() > 50) {
            throw new InvalidTransactionException("carrierName must be 1-50 chars");
        }
        if (offerPrice <= 0) {
            throw new InvalidTransactionException("offerPrice must be > 0");
        }
        // ... other validations
    }
}
```

**Builder: Defensive Programming + Domain Invariants**
```java
public class TransactionBuilder {
    private String carrierName;
    private String transportName;
    // ...
    
    public TransactionBuilder withCarrierName(String name) {
        // Validation at builder step
        if (name == null || name.isBlank()) {
            throw new InvalidTransactionException("carrierName required");
        }
        this.carrierName = name;
        return this;
    }
    
    public Transaction build() {
        // Final domain invariant check
        if (offerPrice < successfulPayments * avgPaymentAmount) {
            throw new InvalidTransactionException(
                "offerPrice inconsistent with payment history (Domain Invariant)"
            );
        }
        return new Transaction(
            carrierName,
            transportName,
            // ...
        );
    }
}
```

### Impact on Testing
- **Unit tests:** Validate each field constraint (50+ test cases)
- **Integration tests:** Validate domain invariants (business logic combinations)
- **Mutation testing:** Prove your validations actually matter

---

## Design Decision 4: Why This Matters for Fraud Detection

1. **Immutability = Audit Trail**
   - "This alert fired because Transaction state was X at time T"
   - Cannot be refuted later
   - Compliance requirement for financial systems

2. **Fail-Fast = No Garbage Alerts**
   - Bad data rejected before rules run
   - No "alert fired on corrupted data" disasters
   - SLA: detect bad data in milliseconds

3. **Domain Invariants = Business Logic as Code**
   - Encode your 6 years of knowledge into constraints
   - Self-documenting: code IS the business rules
   - No "but I forgot to check X" moments

---

## Next: Implement Phase 1

You're building:
1. Transaction (record) + TransactionBuilder
2. Complaint (class with inner record)
3. RiskAlert (immutable class)
4. Enums + Exceptions

Ready?
