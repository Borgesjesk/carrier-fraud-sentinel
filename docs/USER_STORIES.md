# User Stories - FraudSentinel MVP

## US-01: Client submits fraud complaint
**As a** carrier client
**I want to** submit a fraud complaint with supporting documents
**So that** my case is investigated by the appropriate team

Acceptance criteria:
- Form accepts carrier name, complaint type, description (20-2000 chars)
- Supports multiple document uploads (PDF/JPEG/PNG, max 10MB each)
- Auto-routes to correct department based on complaint type
- Client receives confirmation with case ID

## US-02: Staff triages incoming alerts
**As a** fraud analyst
**I want to** see incoming alerts sorted by priority
**So that** I can respond to UNASSIGNED cases first

## US-03: Analyst accepts and investigates case
**As a** fraud analyst
**I want to** claim a case and update its status
**So that** other analysts know it's being handled

## US-04: Staff and client communicate on the case
**As a** stakeholder on a case
**I want to** post comments visible to the other party
**So that** we can exchange information without leaving the platform

## US-05: Staff records internal notes hidden from client
**As a** fraud investigator
**I want to** write internal notes only visible to other staff
**So that** sensitive investigation details stay confidential

## US-06: Analyst transfers case to correct department
**As a** fraud analyst
**I want to** transfer a case to another department when I identify wrong routing
**So that** the right team handles it without duplicated work

## US-07: System detects stale cases without activity
**As a** compliance manager
**I want to** see cases untouched for 72+ hours
**So that** SLA breaches don't go unnoticed

## US-08: User authenticates securely with MFA
**As a** platform user
**I want to** enable two-factor authentication
**So that** my account is protected even if my password leaks
