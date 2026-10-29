#!/usr/bin/env python3
"""DesignOS color tool — contrast verification + perceptual ramp generation.

Zero dependencies (stdlib only).

USAGE
  color.py check "FG BG [label]" ["FG BG label" ...]     verify contrast pairs
  color.py check -                                        read pairs from stdin, one per line
  color.py ramp HUE [--chroma C] [--drift D] [--light L] [--dark L]
  color.py inspect HEX [HEX ...]

EXAMPLES
  color.py check "1C1A17 FAF7F2 body" "8A8580 FAF7F2 muted" "FFFFFF C24A2C button-label"
  color.py ramp 250 --chroma 0.13 --drift 6
  color.py inspect C96F4A

NOTES
  Hex may omit '#' (safer in shells). Labels set the requirement:
  'large', 'heading', 'display', 'h1'..'h3'  -> WCAG 3.0:1, APCA 60
  'ui', 'icon', 'border', 'non-text'         -> WCAG 3.0:1, APCA 45
  anything else (body, muted, placeholder..) -> WCAG 4.5:1, APCA 75
  WCAG 2.1 is the gate; APCA (0.0.98G-4g) is printed as advisory.
  Exit code 1 if any pair fails its WCAG requirement.
"""
import math
import sys
import argparse

# ---------- hex <-> rgb ----------

def parse_hex(s):
    s = s.strip().lstrip('#')
    if len(s) == 3:
        s = ''.join(c * 2 for c in s)
    if len(s) != 6 or any(c not in '0123456789abcdefABCDEF' for c in s):
        raise ValueError(f"bad hex: {s!r}")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))

def to_hex(rgb):
    return '#{:02X}{:02X}{:02X}'.format(*(max(0, min(255, round(c))) for c in rgb))

# ---------- WCAG 2.1 ----------

def _wcag_lin(c8):
    c = c8 / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4

def wcag_luminance(rgb):
    r, g, b = (_wcag_lin(c) for c in rgb)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b

def wcag_ratio(fg, bg):
    l1, l2 = sorted((wcag_luminance(fg), wcag_luminance(bg)), reverse=True)
    return (l1 + 0.05) / (l2 + 0.05)

# ---------- APCA-W3 0.0.98G-4g ----------

def _apca_y(rgb):
    r, g, b = ((c / 255.0) ** 2.4 for c in rgb)
    y = 0.2126729 * r + 0.7151522 * g + 0.0721750 * b
    if y < 0.022:
        y += (0.022 - y) ** 1.414
    return y

def apca_lc(fg, bg):
    """Signed Lc. Positive: dark text on light bg. Negative: light on dark."""
    ytxt, ybg = _apca_y(fg), _apca_y(bg)
    if abs(ybg - ytxt) < 0.0005:
        return 0.0
    if ybg > ytxt:
        sapc = (ybg ** 0.56 - ytxt ** 0.57) * 1.14
        return 0.0 if sapc < 0.1 else (sapc - 0.027) * 100.0
    sapc = (ybg ** 0.65 - ytxt ** 0.62) * 1.14
    return 0.0 if sapc > -0.1 else (sapc + 0.027) * 100.0

# ---------- OKLCH (Ottosson OKLab) ----------

def _srgb_lin(c8):
    c = c8 / 255.0
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4

def _lin_srgb(c):
    c = max(0.0, min(1.0, c))
    return (12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055) * 255.0

def _cbrt(x):
    return math.copysign(abs(x) ** (1.0 / 3.0), x)

def rgb_to_oklch(rgb):
    r, g, b = (_srgb_lin(c) for c in rgb)
    l = _cbrt(0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b)
    m = _cbrt(0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b)
    s = _cbrt(0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b)
    L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    C = math.hypot(a, bb)
    H = math.degrees(math.atan2(bb, a)) % 360.0
    return (L, C, H)

def _oklch_to_rgb_raw(L, C, H):
    a = C * math.cos(math.radians(H))
    b = C * math.sin(math.radians(H))
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (
        +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
        -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
        -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s,
    )

def _in_gamut(lin, tol=1e-4):
    return all(-tol <= c <= 1 + tol for c in lin)

def oklch_to_rgb(L, C, H):
    """Gamut-map by chroma reduction (binary search) — hue and lightness hold."""
    lin = _oklch_to_rgb_raw(L, C, H)
    if not _in_gamut(lin):
        lo, hi = 0.0, C
        for _ in range(28):
            mid = (lo + hi) / 2
            if _in_gamut(_oklch_to_rgb_raw(L, mid, H)):
                lo = mid
            else:
                hi = mid
        lin = _oklch_to_rgb_raw(L, lo, H)
    return tuple(_lin_srgb(c) for c in lin)

# ---------- requirements ----------

LARGE = ('large', 'heading', 'display', 'h1', 'h2', 'h3', 'title')
NONTEXT = ('ui', 'icon', 'border', 'non-text', 'nontext', 'graphic')

def requirement(label):
    low = label.lower()
    if any(k in low for k in NONTEXT):
        return 3.0, 45.0
    if any(k in low for k in LARGE):
        return 3.0, 60.0
    return 4.5, 75.0

def wcag_grade(ratio):
    if ratio >= 7.0:
        return 'AAA'
    if ratio >= 4.5:
        return 'AA'
    if ratio >= 3.0:
        return 'AA-lg'
    return 'below'

# ---------- suggestion for failing pairs ----------

def suggest(fg, bg, wcag_req, apca_req):
    """Move fg lightness away from bg (hue/chroma preserved) until both pass."""
    L, C, H = rgb_to_oklch(fg)
    darken = wcag_luminance(bg) > 0.18  # light bg -> darken text, else lighten
    step = -0.01 if darken else 0.01
    cur = L
    for _ in range(120):
        cur += step
        if not (0.02 <= cur <= 0.99):
            return None
        cand = oklch_to_rgb(cur, C, H)
        if wcag_ratio(cand, bg) >= wcag_req and abs(apca_lc(cand, bg)) >= apca_req:
            return to_hex(cand)
    return None

# ---------- commands ----------

def cmd_check(args):
    lines = []
    if args.pairs == ['-'] or not args.pairs:
        lines = [ln.strip() for ln in sys.stdin if ln.strip()]
    else:
        lines = args.pairs
    rows, failed = [], False
    for ln in lines:
        parts = ln.replace(',', ' ').split()
        if len(parts) < 2:
            print(f"skip (need 'FG BG [label]'): {ln!r}", file=sys.stderr)
            continue
        fg, bg = parse_hex(parts[0]), parse_hex(parts[1])
        label = ' '.join(parts[2:]) or 'body'
        wcag_req, apca_req = requirement(label)
        ratio = wcag_ratio(fg, bg)
        lc = apca_lc(fg, bg)
        ok_w = ratio >= wcag_req
        ok_a = abs(lc) >= apca_req
        verdict = 'PASS' if ok_w else 'FAIL'
        if not ok_w:
            failed = True
        tip = ''
        if not (ok_w and ok_a):
            fix = suggest(fg, bg, wcag_req, apca_req)
            if fix:
                tip = f'try fg {fix}'
        rows.append((label, to_hex(fg), to_hex(bg),
                     f'{ratio:.2f}:1', wcag_grade(ratio), f'{wcag_req:.1f} req',
                     f'{lc:+.1f}', f'{apca_req:.0f} req', 'ok' if ok_a else 'low',
                     verdict, tip))
    if not rows:
        print('no pairs given', file=sys.stderr)
        sys.exit(2)
    head = ('label', 'fg', 'bg', 'WCAG', 'grade', 'need', 'APCA Lc', 'need', 'apca', 'verdict', 'suggestion')
    widths = [max(len(str(r[i])) for r in rows + [head]) for i in range(len(head))]
    fmt = '  '.join('{:<%d}' % w for w in widths)
    print(fmt.format(*head))
    print(fmt.format(*('-' * w for w in widths)))
    for r in rows:
        print(fmt.format(*r))
    sys.exit(1 if failed else 0)

RAMP_STEPS = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950]

def cmd_ramp(args):
    n = len(RAMP_STEPS)
    print(f'# ramp hue={args.hue} chroma={args.chroma} drift={args.drift} '
          f'L {args.light}->{args.dark}   (paste into YAML primitives)')
    for i, step in enumerate(RAMP_STEPS):
        t = i / (n - 1)
        L = args.light + (args.dark - args.light) * t
        taper = 1.0 - 0.62 * (abs(t - 0.5) * 2) ** 1.5   # full chroma mid, ~38% at ends
        C = args.chroma * taper
        H = (args.hue + args.drift * (t - 0.5) * 2) % 360
        rgb = oklch_to_rgb(L, C, H)
        Lr, Cr, Hr = rgb_to_oklch(rgb)  # after gamut mapping
        print(f'{step:>4}: "{to_hex(rgb)}"   # oklch({Lr:.3f} {Cr:.3f} {Hr:.1f})')

def cmd_inspect(args):
    for h in args.hexes:
        rgb = parse_hex(h)
        L, C, H = rgb_to_oklch(rgb)
        lum = wcag_luminance(rgb)
        print(f'{to_hex(rgb)}  oklch({L:.3f} {C:.3f} {H:.1f})  Y={lum:.4f}')
        print(f'  as text on white: WCAG {wcag_ratio(rgb, (255,255,255)):.2f}:1  '
              f'APCA {apca_lc(rgb, (255,255,255)):+.1f}')
        print(f'  as text on black: WCAG {wcag_ratio(rgb, (0,0,0)):.2f}:1  '
              f'APCA {apca_lc(rgb, (0,0,0)):+.1f}')

def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest='cmd', required=True)

    c = sub.add_parser('check', help='verify contrast pairs (WCAG gate + APCA advisory)')
    c.add_argument('pairs', nargs='*', help="each: 'FG BG [label]'; or '-' for stdin")
    c.set_defaults(fn=cmd_check)

    r = sub.add_parser('ramp', help='OKLCH ramp 50-950, perceptually even')
    r.add_argument('hue', type=float)
    r.add_argument('--chroma', type=float, default=0.12)
    r.add_argument('--drift', type=float, default=0.0, help='hue rotation, light->dark (deg)')
    r.add_argument('--light', type=float, default=0.97, help='L of step 50')
    r.add_argument('--dark', type=float, default=0.22, help='L of step 950')
    r.set_defaults(fn=cmd_ramp)

    i = sub.add_parser('inspect', help='hex -> OKLCH + luminance + vs white/black')
    i.add_argument('hexes', nargs='+')
    i.set_defaults(fn=cmd_inspect)

    args = p.parse_args()
    args.fn(args)

if __name__ == '__main__':
    main()
