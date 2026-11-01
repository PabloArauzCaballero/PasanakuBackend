# Money Display — Numbers Are Sacred

The craft of showing amounts, balances, rates, and accounts. Users audit these pixels with their gut: a wobbling column or a vague timestamp reads as "can I trust any number here?"

## Typography

- **Tabular figures on every number that aligns or changes** — `font-variant-numeric: tabular-nums` (fallback: a mono face for digits). Lists, tables, timers, tickers, animated balances. Proportional figures in a transaction column is the single most common fintech craft failure.
- **A type role per money tier**: `amount-lg` (the hero balance — largest thing on screen), `amount-md` (transaction rows, secondary balances), `amount-sm` (inline, table cells), `rate` (APR/APY/percentages), `account-number` (always monospace, grouped: `•••• •••• •••• 4821`).
- Currency symbol tight to the amount: `$1,234.56`, never `$ 1,234.56`. Multi-currency products disambiguate: `US$1,234.56` or `1,234.56 USD` — `$` alone is ambiguous across USD/CAD/AUD/MXN.
- Cents never dropped on transactional surfaces (`$12.00`, not `$12`). Marketing surfaces may round; ledgers may not. Decimal alignment in columns comes free with tabular figures + right-alignment.
- Locale honesty: `1.234,56 €` for the locales that write it that way; RTL layouts keep numerals LTR within the flow.

## Signing and state

- **Negative = minus sign AND color, never color alone.** `−$320.11` (true minus U+2212 beats hyphen). Parentheses `( $320.11 )` acceptable in accounting-culture products — pick one convention and never mix.
- **Pending visually distinct from posted**: muted color or lighter weight, plus a label or icon. Users plan against available money; blurring pending/posted causes real overdrafts.
- Positive inflows: a `+` prefix earns its place in mixed lists. Green on gains is convention — but never green *alone* (color-blind users, and green loses meaning if everything healthy is green).
- **Hide/show balance toggle**: respect it, persist it across sessions, mask as `••••••` sized to the hidden value (no layout shift).

## Precision and honesty

- No rounding theater: `$12,847.50` is the balance; "$12.8K" belongs only on charts with the exact value one interaction away.
- Rates get their real precision: APY `4.35%`, not `~4%`. TILA and fee disclosures have legal precision requirements — design for the real number, not the pretty one.
- **Freshness is part of the number**: balances and quotes carry "as of" context ("as of 2:14 PM ET", "updated just now"). A stale number presented as live is a trust withdrawal — and for business accounts, an audit problem.
- Exchange rates: show the rate, the fee, and the final converted amount *before* commitment. "You send $100.00 → they receive €91.42 (rate 0.9235, fee $1.00)."

## Motion

- Balance changes **count up/down briefly** (200–400ms, ease-out) — continuity, not surprise. Never slot-machine spins, never a jump-cut on the number a user stares at.
- New transaction rows enter with a subtle fade-rise; nothing bounces. Confetti belongs to goal milestones (opt-in moments), never to money movement itself.
- `prefers-reduced-motion`: counts become instant swaps.

## Components

**Balance display** — states: available / pending / total / hidden. Available is primary; the others are secondary by size and weight, not just position. Negative balance: informational, not alarming.

**Transaction row** — anatomy: status → payee/description → category → date → amount (right-aligned, tabular). Status = icon + text. Group by date with sticky headers. One truncation-tested long payee in every design review.

**Rate/fee line** — label left, value right, tabular; fee lines never styled to recede below legibility (4.5:1 minimum — "de-emphasized" fees are a dark pattern *and* a WCAG failure).

**Charts of money** — down-and-to-the-right must remain calm (see anxiety.md); axis starts at zero unless clearly labeled; the current value is text, not just a line endpoint; color pairs with direction markers.

## Screen reader honesty

Amounts announce with currency and context: aria reads "Available balance: 1,234 dollars and 56 cents", not "one two three four point five six". Signs are announced ("minus 320 dollars 11 cents"). Masked accounts read as "card ending 4821".
