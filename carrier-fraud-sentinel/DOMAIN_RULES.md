# FraudSentinel - Domain Rules (From Jess's Fraud Investigation Expertise)

## Transaction Validation Rules (LOCKED)

### carrierName
- ✅ **Required:** YES
- ✅ **Length:** Max 50 characters
- ✅ **Rule:** Must be a valid carrier identifier from WTRANSNET

### transportName
- ✅ **Required:** YES
- ✅ **Length:** Max 50 characters
- ✅ **Rule:** Must identify the specific transport/shipment

### failedPayments
- ✅ **Allow Negative:** NO
- ✅ **Alert Trigger:** N/A (part of payment reconciliation rule)
- ✅ **Max Value:** No upper limit (not validation constraint)

### succeededPayments
- ✅ **Allow Negative:** NO
- ✅ **Alert Trigger:** YES - if < 10 per month = CONTACT ALERT
  - **Meaning:** Low activity customer = potential churn risk
  - **Action:** Sales team reaches out, check if they know platform
  - **Context:** Check if customer understands freight exchange features
- ✅ **Max Value:** No validation upper limit
- ✅ **High Activity Threshold:** > 100 per month
  - **Meaning:** Upsell opportunity, customer using platform heavily
  - **Action:** Review subscription tier, offer premium products
  - **Context:** May need better package/support

### offerPrice
- ✅ **Allow Negative:** NO
- ✅ **Allow Zero:** NO (must be actual price)
- ✅ **Min Value:** > 0 (strictly positive)
- ✅ **Max Value:** NONE (freight prices have no ceiling)
- ✅ **Rule:** Rejection if <= 0

### numberOfOffers
- ✅ **Min Value:** 0
- ✅ **Max Value:** NONE (no validation upper limit)
- ✅ **Rule:** Explained in succeededPayments logic
  - < 10/month = low activity alert
  - > 100/month = upsell opportunity
  - These are business signals, not validation constraints

### reportedIncidents
- ✅ **Allow Negative:** NO
- ✅ **Max Value:** 50 (ALERT TRIGGER)
  - **Meaning:** At threshold 50, flag for company review
  - **Action:** Evaluate company stability
  - **Evaluation Process:**
    - Calculate problem/incident ratio
    - Count good vs bad payments
    - Pros & cons assessment
    - Decide: worth keeping in freight exchange or delist?

---

## Complaint Data Structure (LOCKED - From Jess's Operational Knowledge)

### Core Fields
```
id: UUID or Long
carrierName: String (the transportista filing the complaint)
complaintType: Enum {
  INSURANCE,          // Insurance claim path
  REVIEWING,          // Under review by platform
  ACCIDENT,           // From Rule 3 threshold
  COMMERCIAL_DISPUTE, // From Rule 3 threshold
  OPEN_CASE           // From Rule 3 threshold
}
status: Enum {
  UNRESOLVED,  // Still open, not resolved
  SOLVED       // Resolved (see resolutionReason for outcome)
}
resolutionReason: Enum (only set when status=SOLVED) {
  RECEIVED_PAYMENT,    // Carrier got paid
  NOT_RECEIVED,        // Payment never came
  INCIDENT,            // Non-payment due to incident during transport
  ACCIDENT,            // Accident during transport
  COMMERCIAL_DISPUTE,  // Dispute over commercial terms
  INSURANCE            // Handled by insurance
}

createdDate: LocalDateTime (when complaint filed)
resolvedDate: LocalDateTime (null if UNRESOLVED, set when status=SOLVED)
filedBy: String = carrierName (always the transportista)
```

### Supporting Documentation (Required for Valid Complaint)
The transportista must provide to prove WTRANSNET should intervene:
1. **CMR** - International waybill (official transport document)
2. **ALBARAN** - Delivery receipt
3. **ORDEN DE CARGA** - Bill of lading / contract / loading order
4. **Communication Trail:**
   - Text messages
   - Emails
   - WhatsApp conversations
   - Signed page from WTRANSNET platform (proof of complaint filing)
5. **Description:** Narrative of what happened
6. **Photos:** Evidence images (optional but helpful)

### Business Rules for Complaints

**Filed By:** Always the transportista (carrier) who didn't receive payment after completing transport

**Time Window Calculation (for Rule 3 Complaint Accumulation Rule):**
- **"10+ mediated cases in 1 week"** = count cases with status=UNRESOLVED created in last 7 rolling days
- **"20+ cases in 1 month"** = count all cases (any status) created in last 30 rolling days
- **"2+ accidents in 1 month"** = count cases where complaintType=ACCIDENT created in last 30 rolling days
- **"3+ unresolved commercial disputes"** = count cases where status=UNRESOLVED AND complaintType=COMMERCIAL_DISPUTE (no time limit, cumulative)

**Resolution Logic:**
- When transportista provides complete documentation → status moves from UNRESOLVED to SOLVED
- resolutionReason is set based on investigation outcome
- If resolved as COMMERCIAL_DISPUTE and status=UNRESOLVED → counts toward Rule 3 CRITICAL threshold

---

## Business Context

### Why These Rules Matter

**succeededPayments Logic:**
- < 10/month: Customer might not understand platform or be considering alternatives
- Sales touch: Personal outreach, training, reassurance
- WTRANSNET wants healthy, engaged customers

**reportedIncidents @ 50 threshold:**
- Not a ban, but a "review" trigger
- Carrier might be new (learning curve) vs dishonest
- Need comprehensive evaluation:
  - How many of their offers → successful loads?
  - Payment reliability score?
  - Does incident pattern suggest intentional fraud or incompetence?
  - Recovery plan: education, warning, or delisting?

**offerPrice:**
- Freight prices vary wildly by route, distance, urgency
- No ceiling makes sense (emergency shipments = premium prices)
- Floor of 0.01 EUR minimum makes business sense

**Complaint Documentation:**
- WTRANSNET requires proof before intervening
- CMR + ALBARAN + communication trail = official evidence
- Photos strengthen case for accidents/damage
- Signed platform proof = prevents false claims


---

## Alert Routing & Department Assignment (CRITICAL FEATURE)

### The Problem (Current WTRANSNET Process)
- Alert/case is filed by transportista
- Sits in a queue waiting for a person to notice it
- That person has to manually route it to the right department
- If person is unavailable, sick, or busy → cases pile up
- **Days of delay** before someone actually owns the case
- **No accountability** — who's responsible for this case?

### The Solution (FraudSentinel Smart Routing)
Alerts auto-route to the RIGHT department + person takes ownership immediately.

**Like a SOC (Security Operations Center) workflow:**
- Alert fires → auto-routes to correct department
- Department member sees it on THEIR dashboard
- They click "ACCEPT" → case is assigned to them
- **They are now the DRI (Directly Responsible Individual)**
- Real-time visibility: who's working what, what's pending, SLA tracking

### Auto-Routing Logic by Alert Type

**Rule 3 Alert: Complaint Accumulation**
- 3+ unresolved disputes → Route to **LEGAL DEPT**
- 2+ accidents/month → Route to **INSURANCE DEPT**
- 10+ cases/week → Route to **OPERATIONS MGMT** (volume crisis)
- 5+ open simultaneously → Route to **PAYMENT RECONCILIATION TEAM**

**Rule 1 Alert: Payment Reconciliation**
- 80%+ unpaid rate → Route to **PAYMENT RECONCILIATION TEAM**

**Rule 2 Alert: Price Escalation**
- Suspicious pricing pattern → Route to **FRAUD INVESTIGATION TEAM**

**Soft Alerts (Not fraud, business opportunity):**
- < 10 successful/month → Route to **SALES TEAM** (retention call)
- > 100 successful/month → Route to **ACCOUNT MGMT** (upsell opportunity)
- 50+ incidents → Route to **COMPLIANCE REVIEW**

### Dashboard Workflow (SIEM-Style)

**Alert Queue (For all departments):**
```
[CRITICAL] 3 unresolved disputes - Carrier X
  Status: UNASSIGNED
  Created: 2 hours ago
  Route: LEGAL DEPT
  
  [ACCEPT] [REJECT] [REASSIGN]
```

**When person clicks [ACCEPT]:**
```
Alert becomes ASSIGNED
- Assignee: Person_Name
- Department: LEGAL DEPT
- Appears in THEIR personal dashboard
- SLA Timer starts (e.g., 24hrs to review)
- Timestamp: accepted_at
- Status: IN_PROGRESS
```

**Personal Dashboard View:**
```
MY ASSIGNED CASES (5)
[URGENT] Case #1234 - Carrier X - Due in 6 hours
[HIGH]   Case #1235 - Carrier Y - Due in 18 hours
[MEDIUM] Case #1236 - Carrier Z - Due in 2 days
[LOW]    Case #1237 - Carrier W - Due in 3 days

PENDING CASES (8) - Not yet assigned to anyone
[CRITICAL] Case #1240 - 3 disputes - Waiting for LEGAL
[URGENT]   Case #1241 - 2 accidents - Waiting for INSURANCE
```

### Data Model for Assignment

```
RiskAlert must track:
- assignedTo: String (person's username/email)
- assignedDepartment: Enum {
    LEGAL,
    INSURANCE,
    PAYMENT_RECONCILIATION,
    FRAUD_INVESTIGATION,
    SALES,
    ACCOUNT_MANAGEMENT,
    COMPLIANCE_REVIEW,
    OPERATIONS_MANAGEMENT
  }
- assignmentDate: LocalDateTime (when auto-routed)
- acceptedDate: LocalDateTime (when person clicked ACCEPT)
- acceptedBy: String (person who accepted)
- slaDeadline: LocalDateTime (based on severity)
- status: Enum {
    UNASSIGNED,      // Just fired, waiting for someone
    ASSIGNED,        // Routed to department
    ACCEPTED,        // Person took ownership
    IN_PROGRESS,     // Being investigated
    RESOLVED,        // Closed
    ESCALATED        // Moved to higher authority
  }
- escalatedTo: String (if escalated)
- notes: String (what is person doing? findings?)
```

### Benefits to WTRANSNET

1. **Speed:** No waiting for lucky person to check email
2. **Accountability:** Each case has an owner
3. **SLA Tracking:** Did we respond in 24hrs?
4. **Visibility:** Manager can see what each team is working on
5. **Scalability:** New case = auto-routes, no manual triage
6. **Quality:** Right expertise handles right case type

### For Your Capstone Presentation

This is your **differentiator.** Most fraud systems detect fraud. **YOU built a fraud system that routes it intelligently.**

"I didn't just build fraud detection. I built the operational workflow that makes fraud detection actually MATTER to the business. Cases reach the right person in minutes, not days."

That's a product story that sells.
