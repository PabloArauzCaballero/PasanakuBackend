# Critical Flows — Design the Unhappy Paths First

Money flows are judged at their worst moment, not their best. Design failure, delay, and dispute *before* polishing the happy path — recovery quality is where users decide whether to stay.

## Onboarding / KYC

Most people who start a badly-designed financial onboarding never finish it — Signicat's *Battle to Onboard* research measured 63–68% abandonment across its 2020–2022 studies. The pattern that converts:

- **One task per screen**, explicit step count ("Step 2 of 4" — a number, not just a progress bar), 5–7 fields maximum per screen, ever.
- **Every request explains itself**: "We need this to verify your identity — it's required by law and keeps your account secure." Explained steps feel like protection; unexplained ones feel like interrogation.
- **Document upload**: live preview, retake affordance, real-time quality feedback ("Glare detected — try again away from the light"). The camera moment is where drop-off spikes.
- **Verification wait**: set the expectation ("Usually 2–3 minutes"), show live status, and *let them leave* — email/push when done beats a spinner hostage situation.
- **Verification failure**: never a dead end. Why it failed, an alternative path (different document, manual review), and a human escalation visible.
- **Progressive commitment**: let users see value before the heaviest asks. Account browsing before funding; funding before full KYC where regulation permits tiered limits.

## Payments & transfers

**The confirmation screen** (any irreversible move):
- Single focus — one action, one screen. Recipient → amount → source → fee → total → arrival estimate.
- **CTA names the action**: "Send $250.00 to Alex Rivera" — never "Confirm", "Submit", "OK".
- Edit and cancel visibly available; no countdowns; no default-selected extras.
- First-time recipient = trust moment: show the step-up auth, verify account details back to the user ("Chase account ending 4821").

**After commit**: status timeline the user can reopen ("Sent → In transit → Arrives Thu, Jul 3"). Push on completion. Receipt findable forever.

**Recurring payments**: the *management* UX is the product — pause/skip/cancel as easy as start, next-charge date always visible, notify-before-charge on by default for variable amounts.

## Failed payments & errors

Answer the only question first — **did money leave my account?** — in the first line, before error codes, before apologies:

> "Your transfer didn't go through — **no money left your account.** The receiving bank rejected the account number. → Check the number and try again."

Anatomy: what happened → money status → why (if known) → the fix → escape hatch (support, with context attached so they never repeat the story).

- Card declined: private, instant, blame-free; offer retry with another method inline.
- Timeout mid-payment: state preserved; on return, show *whether the payment committed* — never make the user guess-and-double-pay. Idempotency is a UX feature.
- If money DID move and the transaction failed downstream: say exactly that, with the reversal timeline ("The debit will reverse within 3–5 business days — here's the reference number").

## Disputes & fraud

- **Dispute initiation ≤2 taps from the transaction row.** Hiding it invites chargebacks through the bank instead — worse for everyone.
- **Timeline visualization**: filed → under review → provisional credit → resolved, with dates. Reg E has real deadlines (provisional credit ~10 business days) — showing the clock is both compliance and comfort.
- Fraud alert: one question ("Was this you?"), two one-tap answers, immediate consequence statement ("Card frozen. A new one ships Monday. Your money is safe.").
- Account freeze/AML hold: the scariest state in fintech. First sentence: funds are safe. Second: why (as far as legally allowed). Third: exactly what to do and how long it takes. Silence here is how products end up on the news.

## Waiting states

Financial rails are slow; the interface must not be opaque:

- Every wait has: honest ETA ("1–3 business days" — and say *business* days), current stage, and a way to check later without support.
- Under ~10 seconds: progress indicator. Minutes: status + leave-and-notify. Days: timeline view + proactive push on stage changes.
- **Never fake progress** — a looping bar pretending work is happening teaches users your signals lie. An honest "still processing, next update by 5 PM" outperforms theater.
- Delay beyond promise: proactively tell them *before* they ask, with the new ETA. The apology that arrives first is worth ten that are extracted.

## Cancel / close — the exit is part of the product

Cancel a card, close an account, end a subscription: as findable and as short as the open flow. Retention offers: one, honest, dismissible. Regulators now treat trap-door cancellation as a dark pattern (FTC click-to-cancel; CFPB scrutiny) — and users treat it as proof they were right to leave. A clean exit is the last trust deposit, and the one they tell friends about.
