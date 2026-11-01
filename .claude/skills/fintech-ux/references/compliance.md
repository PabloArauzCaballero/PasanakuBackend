# Compliance — A Design Material, Not an Obstacle

Regulation shapes money UX the way physics shapes architecture: fight it and everything is ugly; design *with* it and the constraints become structure. This is a designer's map of the touchpoints, not legal advice — flag open questions for the product's counsel.

## The regulatory touchpoints (US-centric; EU/UK analogues noted)

| Regime | Where it touches the UI | Design implication |
|---|---|---|
| **KYC** (Know Your Customer) | Onboarding identity verification | The make-or-break flow — see flows.md. Progressive steps, explained asks, tiered limits where permitted |
| **AML** (Anti-Money Laundering) | Transaction monitoring, account holds | Holds need the "funds are safe / why / what now" treatment. Never silent freezes |
| **PCI-DSS** | Card data entry & display | Mask PANs (`•••• 4821`), secure input fields, no card numbers in screenshots/receipts/emails |
| **Reg E** (Electronic Fund Transfers) | Error resolution, disputes | Dispute entry ≤2 taps; provisional-credit timeline visible; error flows are *regulated*, not optional |
| **Reg Z / TILA** (Truth in Lending) | Loan & credit displays | APR shown with legal precision before commitment; total-cost visibility; no burying the rate in a tooltip |
| **FCRA** | Credit scores, adverse actions | Plain-language score explanations; adverse action notices that a human can understand; dispute paths |
| **GLBA / privacy** (EU: GDPR) | Data-sharing consent | Granular, non-pre-checked consent; opt-outs as findable as opt-ins |
| **ADA / §508 / WCAG 2.2 AA** (EU: EAA) | Everything | Legal floor, not aspiration — 48px money-action targets, announced amounts, 200% zoom, no color-only status |
| **SOX** (business/reporting products) | Dashboards, exports | "As of" timestamps, audit trails visible, immutable histories |
| **FDIC / NCUA membership** | Trust displays | The insured line is a *regulated claim* — exact wording matters; deploy it at anxious moments (see trust.md) |

EU/UK additions when relevant: **PSD2/SCA** (step-up authentication design is mandated, not optional), **FCA Consumer Duty** (UK — fair value and comprehension are regulatory tests; comprehension-hostile design is now a compliance failure).

## Disclosures — designed to be read, not survived

The legal team's wall of text and the designer's job meet here:

- **Layered disclosure**: one-line plain-language summary → expandable detail → full legal text. All three present; the user chooses depth. Courts and regulators increasingly favor this; users always have.
- **Disclosure at the decision, not after it**: fee schedules before commitment, rate changes before renewal, terms before the signature — proximity is the whole point.
- **Scannable structure**: real headings, tables for numbers (rates, fees), bolded amounts. A disclosure formatted as a paragraph of 9px justified text is technically present and practically hidden — regulators call that "burying," and it's citable.
- **Comprehension over acknowledgment**: for high-stakes moments (loan acceptance, investment risk), a one-question comprehension check ("What's your monthly payment?") beats a checkbox nobody reads. Friction spent on understanding is friction well spent.
- Keep a **paper trail the user can access**: every accepted disclosure findable later in-app (dated, versioned). "What did I agree to?" should never require a support ticket.

## Consent — granular, honest, reversible

- One purpose per toggle. "Improve your experience" bundling data-sale consent is a dark pattern with enforcement history.
- Nothing pre-checked that shares data or costs money.
- Withdrawal as easy as grant — same screen depth, same tap count.
- State the consequence honestly: "Without this, you can't use instant transfers — everything else works."

## Working with compliance teams (the meta-skill)

- Bring compliance in at the *flow sketch* stage, not the polish stage — late legal review is how disclosures end up as bolted-on walls.
- Ask "what must be true?" not "is this OK?" — requirements come as constraints you can design with; approvals come as vetoes.
- Every regulated element gets a comment in the spec naming its regime ("Reg E: dispute link required here") so the next designer doesn't "clean it up."
- When a legal requirement genuinely hurts UX, the move is design *iteration with counsel* — there is almost always a compliant version that converts; the first draft just wasn't it.

## Audit checklist

- [ ] Fees/rates disclosed before every commitment, at legal precision
- [ ] Disputes & error resolution reachable per Reg E expectations
- [ ] Card data masked everywhere it appears
- [ ] Consent granular, un-pre-checked, reversible at equal depth
- [ ] Disclosures layered, scannable, and retrievable later
- [ ] Insured/protection claims use exact regulated wording
- [ ] Every regulated element labeled in the spec with its regime
