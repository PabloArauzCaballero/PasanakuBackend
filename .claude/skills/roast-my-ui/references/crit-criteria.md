# Crit Criteria — The Ten Passes

Run all ten against any input. A finding needs three parts: **evidence** (where, what — quote it, point at it, measure it), **impact** (which users it hurts and how), **fix** (specific, with exact values when you have them). Findings missing any part get cut.

## 1. Squint test — hierarchy
Blur it mentally. Does one thing lead? Findings: primary action doesn't pop; everything's the same visual weight; the loudest element is decorative; two elements fighting for #1. Fix language: name what should lead and what must step back (size, weight, color — in that order).

## 2. Contrast — computed, never guessed
Every text/background pair through `scripts/color.py check` when values are available (`python3 scripts/color.py check "1C1A17 FAF7F2 body"` — hex without `#` is shell-safe). Gate: WCAG 2.1 (4.5:1 body, 3:1 large/UI). Advisory: APCA (|Lc| 75 body / 60 large / 45 UI). Special suspects: muted/placeholder text, text over images or gradients, text on tinted badges, disabled states, dark mode. From screenshots, sample and label as approximate — flag "looks borderline, verify" rather than asserting.

## 3. Alignment walk
Trace edges. Findings: elements almost-aligned (2–5px off reads as sloppy, not intentional); too many distinct alignment lines (calm layouts have few); centered text on left-aligned layouts; icons optically off-center in their containers. Fix: name the line things should share.

## 4. Rhythm — spacing
Spacing should come from a scale and encode relationships: gaps within a group smaller than gaps between groups (≥1.5×). Findings: orphan values (13px next to 16px next to 22px); labels floating equidistant between fields (whose label is it?); padding inside boxes larger than margins between them; section spacing identical to element spacing.

## 5. Consistency
Same thing, same look: one button recipe per role, one card treatment, one focus style, consistent radii family, consistent icon stroke weights. Different things must look different: destructive actions dressed like safe ones is a finding with teeth. Also: mixed date formats, mixed title casing, mixed shadow directions.

## 6. Content honesty
Lorem ipsum in a shipped screen. "John Doe" / `user@example.com`. Perfectly round fake numbers ($100, 50%, 10,000+). Invented trust badges and testimonials. Buttons that say "Submit" / "Click here" / "Learn more" instead of the actual action. Empty states that say "No data" instead of selling the first action. Error messages that blame or bare error codes. Words are design — quote the bad ones and rewrite them in the fix.

## 7. States
What happens on hover, focus, disabled, loading, empty, error, overflow? Findings: no visible focus ring (hard a11y fail); hover-only affordances with no mobile equivalent; skeletons that don't match final layout; lists with no empty state; text that will truncate with no plan (test: double the longest string). From a static screenshot, flag the states you *can't see* as open questions, not accusations.

## 8. Slop scan — the AI-median look
The recognizable generated-design defaults. Any hit must be deliberate and defended, or it's a finding:
1. Inter/Poppins + purple-to-blue gradient + glassmorphism cards
2. Everything center-aligned; hero → 3 feature cards → testimonials → CTA template scroll
3. Identical border-radius and identical shadow on every element
4. Gradient text headlines; emoji standing in for icons
5. Purple/indigo accent that nobody chose
6. Three-column icon-title-blurb grids
7. Gray placeholder boxes where imagery should be
8. Dark mode as pure inversion (#000 + neon)
The roast line for this category: "this could be any product." The fix is always the same shape: pick one signature element, quiet everything else, and choose an accent someone actually chose.

## 9. Distinctiveness
Close the tab. What do you remember? Findings: nothing memorable (no signature element); or five competing signatures (diluted identity); or the memorable thing is a bug (janky animation, weird color collision). Strong products have exactly one thing you can describe to a friend. Name what theirs could be — it's usually already there, buried.

## 10. Accessibility beyond contrast
Touch targets under 24px (WCAG 2.2 floor; 44px for primary mobile actions). Color as the only signal (status dots with no label, red/green-only states). Heading levels skipping. Text under 12px doing real work. Animation with no reduced-motion consideration. Form fields with placeholder-as-label. Focus order that would jump around. Each of these is a 🔥🔥 minimum — they exclude people, not just annoy them.

## Severity calibration

- 🔥 **Annoying** — craft misses that professionals notice: orphan spacing, near-alignment, inconsistent radii, weak microcopy.
- 🔥🔥 **Hurting users** — friction with a body count: failed contrast on secondary text, missing states, ambiguous primary action, targets too small, slop that erodes trust.
- 🔥🔥🔥 **Driving people away** — conversion killers: unreadable primary content, invisible/indistinguishable CTA, broken hierarchy on the money screen, a11y failures on the core flow, "looks like a scam" trust collapse.

Calibrate to product stage: a weekend prototype gets its 🔥🔥🔥s and a lighter hand on 🔥s; a funded product's marketing site gets the full ten passes at full heat.
