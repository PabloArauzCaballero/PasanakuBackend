# Trust — Built in Drops, Lost in Buckets

Trust isn't a section of the UI; it's the accumulated residue of every interaction. Users can't audit your security, so they audit your *behavior*: does this product do what it said, when it said, and tell me when it can't?

## The credibility stack (in order of leverage)

1. **Visual craft** — alignment, consistent spacing, professional type. Judged in the first 500ms, before a single word is read. Sloppy pixels = "sloppy with my money."
2. **Predictability** — same action, same result, same place. Consistent navigation, no surprise modals, no moved buttons. Predictable is boring; boring is trustworthy.
3. **Transparency** — statuses visible, fees upfront, timelines honest, "as of" stamps everywhere. The product never knows something the user can't find out.
4. **Recovery quality** — what happens when things go wrong is where trust is actually decided. A well-handled failure builds more trust than a hundred smooth successes.

Work bottom-up when auditing: products usually invest in 1 and neglect 4.

## Security UX — visible where it reassures, invisible where it protects

- **Invisible by default**: biometric-first login (password as fallback, not default), passive/behavioral authentication, device binding. Security the user doesn't perform doesn't get abandoned.
- **Visible at the moments of doubt**: adding a first-time recipient, large transfers, changing credentials, new device login. That's where a lock icon, a "we'll confirm this by text" line, or step-up auth *reassures* rather than annoys.
- **Step-up authentication scales with risk**: viewing a balance ≠ sending $5,000. Low-risk actions stay frictionless; high-risk actions earn one extra verification — explained ("Because this is your first transfer to this account…").
- **Session behavior**: warn at 60s before timeout, preserve form state through re-auth, never punish the user for the bank's paranoia.
- **Safety controls within reach**: freeze card, report fraud, enable 2FA — ≤2 taps from anywhere. Burying these tells users safety is an afterthought.

## Trust signals vs trust theater

| Signal (works) | Theater (backfires) |
|---|---|
| Real-time transaction notifications | Padlock icons sprinkled decoratively |
| "FDIC insured up to $250,000" *at the anxious moment* (large deposit, first funding) | The same line only in 8px footer legalese |
| Visible processing status ("Sent → In transit → Received") | "Bank-level security!" marketing copy with no substance |
| Honest limitation ("Transfers take 1–3 business days — here's why") | Fake urgency, inflated user counts, invented testimonials |
| Instant, specific fraud alerts with one-tap response | Security questions about your first pet, in 2026 |

The test: does the signal give the user *information they can act on or verify*? If not, it's theater — and users' scam-detection instincts are better than most products assume.

## First-session trust arc

The first session decides everything. Design it deliberately:

1. **Before signup** — fees and rates findable without an account. Hiding pricing behind signup reads as a trap.
2. **During onboarding** — every KYC step explains itself (see flows.md). Progress is visible. Nothing is asked twice.
3. **First funding** — the scariest moment: money leaves a place they trust for a place they don't yet. Confirmation, status visibility, and the insured/protected line belong *here*.
4. **First surprise** — the first pending hold, first fee, first delay. If the product explains it before the user asks, trust compounds; if the user has to search, it halves.

## Audit checklist

- [ ] Fees visible before commitment, everywhere money moves
- [ ] Every processing state has a status the user can check + an honest ETA
- [ ] Step-up auth present on: new recipient, large transfer, credential change, new device
- [ ] Freeze/report/2FA reachable in ≤2 taps
- [ ] FDIC/protection/insurance line present at anxious moments, not just legal footer
- [ ] Timeout warns, preserves state, re-auths gracefully
- [ ] No theater: every trust signal is verifiable or actionable
