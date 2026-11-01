---
name: fintech-ux
description: |
  Fintech UX — design judgment for money products, encoded from 19+
  years of financial services design leadership. Activates whenever the
  work touches money: banking, payments, checkout and billing,
  invoicing, insurance, lending, wealth/investing, crypto wallets,
  payroll, expense tools — building new UI, reviewing existing UI, or
  designing critical flows (onboarding/KYC, transfers, failed payments,
  disputes). Covers trust architecture, financial-anxiety-aware design,
  money display craft (tabular figures, pending vs posted, negative
  conventions — with a bundled lint that computes violations from
  code), compliance as a design material (KYC, AML, PCI-DSS,
  Reg E, TILA), and the anti-patterns that erode trust or invite
  regulatory risk. Use when the user mentions a financial product or
  feature, a payment/checkout/billing flow, KYC or verification UX,
  displaying balances/transactions/rates, or asks "does this feel
  trustworthy?" — even if they never say the word "fintech".
license: MIT
metadata:
  author: Ravi Palwe
  version: "1.1"
  compatibility: python3 for scripts/money_display_lint.py (stdlib only). Standalone. Composes with roast-my-ui (fintech-aware crit), designos (money-product design systems), and prism (finserv research deliverables) when installed.
---

# Fintech UX — Design for Money

When users trust a screen with their money, every pixel carries weight. A misalignment that reads as "a little off" in a social app reads as "I don't trust this" in a banking app. Financial stress changes how people use software: they check balances at 2am, they abandon KYC flows at the first confusing step, they read a failed payment as *"where is my money?"* — and the interface either answers or loses them.

This skill applies whether you're **building** money UI, **auditing** it, or **designing a flow**. It's judgment, not a checklist: every rule below exists because violating it costs trust, money, or both.

## The Seven Laws of Money UI

1. **Trust is the product.** Features are why users arrive; trust is why they stay. Every decision either deposits or withdraws from the trust account — and trust is built in drops, lost in buckets.
2. **Calm is functional.** Financial anxiety is a usability bug, not a mood. Muted palettes, generous space, deliberate motion, and no alarm styling on ordinary events.
3. **Numbers are sacred.** Tabular figures, explicit signs, honest precision, visible "as of" freshness. Never rounding theater, never a jumping digit.
4. **Say if money moved.** Every error, timeout, and ambiguous state answers the user's only real question first: *did money leave my account?*
5. **Irreversible actions get explicit confirmation.** The CTA names the action and the amount ("Send $250.00 to Alex Rivera"), never "Confirm."
6. **Compliance is a design material.** KYC, disclosures, and consent are moments to design — explained steps convert; walls of forms abandon (Signicat's *Battle to Onboard* research measured 63–68% abandonment of badly-designed financial onboarding).
7. **Accessibility is law, not polish.** ADA / Section 508 / WCAG 2.2 AA are legal obligations for financial services. 48px targets for money actions, announced amounts, 200% zoom, no color-only status.

## Modes — each has a contract, not just a posture

**Build** — designing or implementing money UI. Load the reference for the surface you're touching (map below) and apply its patterns and tokens. When creating a design system for a money product, the trust/anxiety principles shape the *direction*, not just components — calm is a token-level decision.
*Exit criteria — before declaring done:* (1) run the relevant reference's audit checklist against **your own output** and show the pass/fail lines; (2) if you wrote code, run the lint (below) and clear every ERROR. "Done" means both are clean, not "it renders."

**Audit** — reviewing an existing money product. Run the laws as lenses over the UI, then load `references/trust.md` and `references/anxiety.md` for the deep checks. If code was provided, run the lint *first* — computed findings anchor the review. If `roast-my-ui` is installed, compose: its ten passes plus these domain lenses make a fintech-grade roast.
*Deliverable — always this shape:*

```
## Verdict
<one sentence — the biggest truth about this product's money UX>

## Findings — ranked by severity
<each finding, no exceptions:
**Law/reference violated** → evidence (screen or file:line — quote it)
→ impact (user harm + regulatory exposure, if any) → exact fix (values, copy)>

## Lint receipts
<script output, when code was provided — file:line, computed>

## What's working
<honest strengths to protect while fixing the rest>

## Fix order
<numbered, highest impact-per-effort first, one line each>
```

**Flow** — designing a critical money journey (onboarding/KYC, payment, transfer, dispute, recovery). Load `references/flows.md` and `references/compliance.md`.
*Deliverable — a flow spec, built in this order:*
1. **Unhappy paths first** — enumerate every failure, timeout, rejection, and abandonment point *before* drawing the happy path; each one gets its recovery designed, not just named.
2. **Per screen**: purpose, all states (loading / error / empty included), and the key copy — error copy answers "did money move?" in its first line.
3. **Every regulated element tagged inline with its regime** (`Reg E:`, `TILA:`, `KYC:`, `PCI:`) so the next designer doesn't "clean it up."
4. **Close with exit criteria**: the compliance.md audit checklist run against the spec, pass/fail shown.

## Receipts — compute, don't assert

When the input includes code (CSS/HTML/JSX/components), run the bundled lint before writing a single finding:

```
python3 scripts/money_display_lint.py <path> [--json]
```

It mechanically detects: hyphen-minus negatives (want U+2212), symbol–amount gaps, dropped cents, money columns without tabular figures, color-only negative styling, fee/disclosure text below WCAG 4.5:1 (contrast *computed*, including `var()` resolution), and hand-rolled formatting that should be `Intl.NumberFormat`. Cite findings as `file:line` receipts — "your transaction column uses proportional figures, here are the 4 files" is unanswerable. MD003/MD005 are questions to verify against context, not automatic verdicts. For colors the lint can't reach (screenshots, Figma), sample and verify with roast-my-ui's `scripts/color.py` when installed.

## Reference map — load just-in-time

| Working on | Load |
|---|---|
| Balances, amounts, transactions, rates, account numbers | `references/money-display.md` |
| Security UX, credibility, trust signals vs theater | `references/trust.md` |
| Stress moments, loss display, alerts, tone | `references/anxiety.md` |
| Onboarding/KYC, payments, errors, disputes, waiting | `references/flows.md` |
| KYC/AML/PCI/Reg E/TILA touchpoints, disclosures, consent | `references/compliance.md` |

Don't read them all upfront; each is dense and scoped.

## Anti-patterns — never, in any mode

These erode trust and several invite regulatory exposure (dark-pattern enforcement is active — CFPB, FTC, EU):

1. **Hidden fees** — costs revealed only at the final step. Fees appear before commitment, always.
2. **Urgency theater** — countdowns and "limited time" pressure on financial decisions.
3. **Confirm-shaming** — "No, I don't want to save money." Never.
4. **Color-only status** — success/pending/failed must carry icon or label, not just hue.
5. **Alarm styling on ordinary states** — a negative balance is information, not a siren. Minus sign + subtle color, no bold red panic.
6. **Buried safety controls** — card freeze, 2FA, fraud reporting reachable in ≤2 taps from anywhere.
7. **Black-box processing** — "being processed" with no timeline, no status, no way to check.
8. **Trap-door retention** — cancel/close flows harder than open flows. Regulators read this as a dark pattern; users read it as a warning.

## Composing with the family

- **roast-my-ui** installed → offer the fintech-grade roast (its crit + these lenses).
- **designos** installed → money products bias the direction toward calm worlds; token systems gain the money-display tokens; `scripts/color.py` there verifies the contrast this skill demands.
- **prism** installed → for *research* deliverables (personas, journey maps) on financial products, prism's finserv layer is the research-side counterpart to this design-side skill.

None are required — this skill stands alone.
