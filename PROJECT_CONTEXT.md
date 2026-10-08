# ScamShield: PROJECT_CONTEXT.md
Hackatopia 2K26 • Team The Saturn • Backend / Intelligence

## 1. Project Identity and Purpose
ScamShield is a pre-payment firewall for the Hackatopia 2K26 24-hour MVP. It is not a generic phishing detector. It connects the message, destination, payee, and amount before authorization to identify contextual mismatches.

## 2. Core Product Idea
The central question is: **"Does the payment match the story?"**
Core chain: MESSAGE → LINK → QR → PAYEE → PAYMENT
Workflow: Collect → Analyze → Correlate → Explain + Act
Decision: SAFE → PROCEED, SUSPICIOUS → VERIFY, HIGH_RISK → STOP

## 3. Backend Ownership/Boundary
The backend is the **source of truth for ALL risk decisions**. The backend/intelligence layer owns the FastAPI application, schemas, logic, and tests. The frontend/UX is owned by a teammate. The frontend must **not** duplicate risk scoring logic.

## 4. Approved Technology Stack
- Python + FastAPI for the API
- Pydantic for request/response validation
- Deterministic Python rules for the P0 intelligence engine
- Optional lightweight QR decoding library (when implemented)
- Python standard-library URL parsing
- No database for the initial MVP unless genuinely necessary
- Environment variables for secrets/configuration
- Optional external reputation/NLP services only *after* the deterministic core works.

## 5. Required Repository/Backend Structure
Work primarily inside `backend/` and `tests/`. Do not modify frontend files unless explicitly asked.
Recommended structure:
```
backend/
├── main.py
├── schemas.py
├── engine/
│   ├── message_shield.py
│   ├── link_shield.py
│   ├── qr_shield.py
│   ├── payment_shield.py
│   ├── correlation.py
│   ├── scoring.py
│   └── explanations.py
├── scenarios/
└── tests/
```

## 6. POST /analyze Contract
This is the primary analysis endpoint. The API contract must be preserved unless changes are agreed upon.

**Request:**
```json
{
  "message": "Your electricity bill is overdue. Pay ₹850 immediately.",
  "url": "https://example.com/pay",
  "qr_payload": null,
  "payee": "ABC Utilities",
  "amount": 850,
  "currency": "INR"
}
```

**Response:**
```json
{
  "interaction_id": "abc123",
  "risk_level": "SUSPICIOUS",
  "score": 48,
  "signals": ["urgent_language"],
  "reasons": ["The message uses urgent payment language."],
  "recommendation": "VERIFY",
  "coverage": {
    "message": "checked",
    "url": "checked",
    "qr": "not_provided",
    "payment": "checked"
  }
}
```
Risk levels: SAFE, SUSPICIOUS, HIGH_RISK.
Recommendations: PROCEED, VERIFY, STOP.

## 7. GET /health Requirement
The `GET /health` endpoint should remain lightweight to ensure the server starts reliably.

## 8. Shield Responsibilities
Each Shield is a small module with clear inputs, outputs, and error states reporting what was checked, signals fired, what couldn't be checked, and extracted context.
- **Message Shield**: Detect financial intent, urgency, impersonation, and scam patterns (urgent payment language, OTP/PIN requests, account blocking threats).
- **Link Shield**: Parse supplied URLs and inspect obvious indicators (malformed URLs, suspicious domain patterns, mismatch with destination) without blindly opening arbitrary user URLs.
- **QR Shield**: Decode QR payload, extract destination/payment information, and compare it with surrounding context. Invalid/unavailable data is explicitly represented.
- **Payment Shield**: Compare simulated payment details (payee, amount, currency) against what the message requested.

## 9. Context Correlation Responsibility
The Context Correlation Engine combines findings across all inputs (message intent + URL/QR destination + payee + payment context) under one interaction ID. It identifies meaningful inconsistencies (e.g., "Pay ₹500 to ABC Store" followed by a ₹5,000 payment to an unrelated person).

## 10. Risk Scoring Bands and Weights
Risk scoring converts findings into a weighted score, then a risk band, and finally an explanation.
**These are prototype rules from the project specification, not scientifically validated probabilities.**

**Signals:**
- `urgent_language`: +25
- `impersonation`: +20
- `suspicious_url/domain`: +25
- `qr_mismatch`: +20
- `payee_mismatch`: +25
- `amount_mismatch`: +20
- `missing/unverifiable`: +10
- `consistent_low_risk`: -15

**Bands:**
- **SAFE**: 0–29 (→ PROCEED)
- **SUSPICIOUS**: 30–59 (→ VERIFY)
- **HIGH_RISK**: 60+ (→ STOP)

## 11. Explainability Rules
Every returned reason in the `reasons` field must map to a real signal/rule that fired. Never invent a reason after scoring merely to make the result sound convincing. Reasons should be plain-language and useful to the user.

## 12. Multilingual Boundary
The risk engine should remain language-neutral. Risk levels, signals, and internal rule identifiers must remain stable. If multilingual explanations are implemented in the backend, use deterministic translations for known signals/recommendations. Do not make the core fraud decision depend on an external translation model.

## 13. Security Rules
- **Do not blindly open arbitrary user-supplied URLs.**
- Never expose API keys, hard-code secrets, or expose raw server errors/stack traces to the client.
- If outbound URL fetching is added later, implement SSRF protections.
- External reputation failures must be represented as UNAVAILABLE, never silently treated as SAFE.

## 14. Error-Handling Rules
- Malformed input → clear validation error.
- Missing optional input → coverage reports `not_provided` or `unavailable`.
- Shield-specific failure → report unavailable/failed status without crashing the whole analysis when possible.
- External-service failure → UNAVAILABLE, never SAFE.
- Unexpected internal failure → server-side logging plus generic client-safe error.

## 15. P0 Non-Goals
- **Real UPI/bank integration and real-money movement are out of scope.**
- WhatsApp/SMS interception.
- Full banking backend.
- Training a large model from scratch.
- Complex microservices or adding a database initially.
- Authentication (unless genuinely required).
- Production fraud guarantees.
- Advanced analytics/history before the core demo is reliable.

## 16. Deterministic Demo Scenarios
- **SAFE**: message requests ₹799 to ABC Broadband and payment shows the same payee and amount → SAFE / PROCEED.
- **SUSPICIOUS**: urgent account-verification payment request with incomplete or suspicious context → SUSPICIOUS / VERIFY.
- **HIGH_RISK**: message requests ₹850 to an electricity provider, but payment shows ₹8,500 to an unrelated person plus a suspicious URL → HIGH_RISK / STOP.

## 17. Testing Expectations
- Tests should cover core shield and correlation behavior.
- Expected matrix: Message (normal/urgent/impersonation/empty), URL (normal/suspicious/malformed/missing), QR (valid/invalid/mismatch), Payment (match/wrong amount/wrong payee/missing), Correlation (consistent/mismatches), Services (unavailable behavior).
- End-to-end SAFE, SUSPICIOUS, and HIGH_RISK scenarios must be repeatable.

## 18. Git Coordination Rules
- Pull before starting work.
- Commit frequently with focused messages.
- Push frequently so integration is visible.
- Communicate API contract changes to the teammate before merging.
- Deliberately inspect and resolve merge conflicts; never randomly delete code.

## 19. Implementation Priority
When time is tight, follow this sequence:
1. Make `/analyze` work with schemas
2. Message Shield
3. Payment mismatch
4. Correlation
5. Scoring
6. Explanations
7. Link/QR
8. Frontend integration
9. Simulated payment warning
10. Polish
11. Optional APIs/NLP/History

**Deterministic rules come before optional NLP/Gemini/external services.** Make the intelligence reliable, explainable, and demonstrable in seconds.

## 20. Rules for AI Coding Agents
- Use the workflow: DISCUSS → COMPARE → RECOMMEND → APPROVE → IMPLEMENT.
- Do not build the "whole backend" at once. Take one small task at a time (schemas, then `/analyze`, then one shield, then correlation, etc.).
- Inspect the existing repository (`backend/`, `tests/`) and `PROJECT_CONTEXT.md` before changing anything. Never assume a file, dependency, or component exists.
- Do not modify frontend files unless explicitly asked.
- Do not introduce unnecessary frameworks, dependencies, databases, or microservices without an MVP reason.
- Do not rewrite working code without a reason.
- Preserve the documented `POST /analyze` contract; if changes are needed, explain them before implementation.
- After implementation:
  1. List files changed.
  2. Explain exactly what changed.
  3. Explain how to run it.
  4. Explain how to test it.
  5. State assumptions and remaining issues.
  6. Confirm whether the API contract changed.
  7. Do not modify anything outside the requested scope.
