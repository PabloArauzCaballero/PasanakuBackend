#!/usr/bin/env python3
"""fintech-ux money-display lint — computed receipts for money UI craft.

Scans CSS/SCSS/HTML/JSX/TSX/JS/TS/Vue/Svelte for mechanically detectable
violations of the rules in references/money-display.md.
Zero dependencies (stdlib only). No network calls.

USAGE
  money_display_lint.py [PATH ...] [--json] [--rules MD001,MD004]

  PATH defaults to the current directory. Directories are walked
  (node_modules, dist, build, .git etc. skipped).

RULES
  MD001  hyphen-minus in a negative amount (want U+2212 −)          ERROR
  MD002  space between currency symbol and amount ("$ 100")          ERROR
  MD003  whole-dollar amount — cents dropped? (fine on marketing,    INFO
         never on ledgers/transactions — check the surface)
  MD004  money amounts rendered without tabular-nums in reach        WARN
  MD005  negative/loss styling that may rely on color alone          WARN
  MD006  fee/disclaimer text below WCAG 4.5:1 (contrast COMPUTED;    ERROR
         WARN when the background had to be assumed white)
  MD007  hand-rolled money formatting (want Intl.NumberFormat)       WARN

NOTES
  CSS parsing is flat (top-level rule blocks); var(--x) references are
  resolved against custom properties found anywhere in the scanned set.
  A lint finding is a receipt, not a verdict — MD003/MD005 in particular
  ask you to check context before calling them violations.
  Exit code 1 if any ERROR-severity finding.
"""
import argparse
import json
import os
import re
import sys

SEV_ERROR, SEV_WARN, SEV_INFO = 'ERROR', 'WARN', 'INFO'

STYLE_EXT = {'.css', '.scss', '.less'}
MARKUP_EXT = {'.html', '.htm', '.jsx', '.tsx', '.vue', '.svelte'}
SCRIPT_EXT = {'.js', '.jsx', '.ts', '.tsx', '.mjs', '.cjs', '.vue', '.svelte'}
ALL_EXT = STYLE_EXT | MARKUP_EXT | SCRIPT_EXT
SKIP_DIRS = {'node_modules', '.git', 'dist', 'build', 'out', '.next', '.nuxt',
             'vendor', 'coverage', '.cache', '__pycache__'}

CUR = r'[$€£¥₹]'
RE_AMOUNT = re.compile(CUR + r'\s?\d[\d,]*(?:\.\d+)?')
RE_HYPHEN_NEG = re.compile(
    r'-\s?' + CUR + r'\s?\d[\d,]*(?:\.\d+)?|' + CUR + r'-\d[\d,]*(?:\.\d+)?')
RE_SYM_SPACE = re.compile(CUR + '[ \t\u00a0]+' + r'\d[\d,]*(?:\.\d+)?')
# ¥ excluded: JPY is a zero-decimal currency
RE_WHOLE = re.compile(r'[$€£₹]\d{1,3}(?:,\d{3})*(?![\d.,]|\s?[KkMmBb%])')
RE_TABULAR = re.compile(r'tabular-nums|font-variant-numeric|[\'"]tnum[\'"]')
RE_NEG_SELECTOR = re.compile(r'negative|debit|\bloss|declin|overdraft|\bdown\b', re.I)
RE_FEE_SELECTOR = re.compile(
    r'fee|fine[-_]?print|small[-_]?print|disclaimer|disclosure|legal|footnote|terms', re.I)
RE_MONEYISH = re.compile(
    r'amount|balance|price|total|change|delta|pnl|loss|negative|debit', re.I)
RE_CONCAT = re.compile(r'[\'"]' + CUR + r'[\'"]\s*\+|\+\s*[\'"]' + CUR + r'[\'"]')
RE_TPL_DOLLAR = re.compile(r'\$\$\{')
RE_TOFIXED = re.compile(r'\.toFixed\(\s*2\s*\)')
RE_BLOCK = re.compile(r'([^{}]+)\{([^{}]*)\}')
RE_VARDEF = re.compile(r'(--[-\w]+)\s*:\s*([^;{}]+)')
RE_STYLE_TAG = re.compile(r'<style[^>]*>(.*?)</style>', re.S | re.I)

NAMED_COLORS = {
    'white': (255, 255, 255), 'black': (0, 0, 0), 'red': (255, 0, 0),
    'gray': (128, 128, 128), 'grey': (128, 128, 128), 'silver': (192, 192, 192),
    'lightgray': (211, 211, 211), 'lightgrey': (211, 211, 211),
    'darkgray': (169, 169, 169), 'darkgrey': (169, 169, 169),
}


# ---------- color / WCAG (same math as roast-my-ui's color.py) ----------

def parse_color(value, cssvars, depth=0):
    if not value:
        return None
    v = value.strip().lower().rstrip('!important').strip()
    m = re.match(r'var\(\s*(--[-\w]+)', v)
    if m:
        if depth >= 5:
            return None
        return parse_color(cssvars.get(m.group(1)), cssvars, depth + 1)
    m = re.match(r'#([0-9a-f]{6}|[0-9a-f]{3})\b', v)
    if m:
        h = m.group(1)
        if len(h) == 3:
            h = ''.join(c * 2 for c in h)
        return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))
    m = re.match(r'rgba?\(\s*(\d+)[,\s]+(\d+)[,\s]+(\d+)', v)
    if m:
        return tuple(min(255, int(m.group(i))) for i in (1, 2, 3))
    return NAMED_COLORS.get(v)


def _lin(c8):
    c = c8 / 255.0
    return c / 12.92 if c <= 0.03928 else ((c + 0.055) / 1.055) ** 2.4


def wcag_ratio(fg, bg):
    def lum(rgb):
        return 0.2126 * _lin(rgb[0]) + 0.7152 * _lin(rgb[1]) + 0.0722 * _lin(rgb[2])
    l1, l2 = sorted((lum(fg), lum(bg)), reverse=True)
    return (l1 + 0.05) / (l2 + 0.05)


# ---------- helpers ----------

def finding(rule, sev, path, line, message, fix):
    return {'rule': rule, 'severity': sev, 'path': path, 'line': line,
            'message': message, 'fix': fix}


def blank_comments(text):
    """Replace /* */ comment contents with spaces, preserving newlines/offsets."""
    def repl(m):
        return ''.join(c if c == '\n' else ' ' for c in m.group(0))
    return re.sub(r'/\*.*?\*/', repl, text, flags=re.S)


def line_of(text, index):
    return text.count('\n', 0, index) + 1


def collect_files(paths):
    files = []
    for p in paths:
        if os.path.isfile(p):
            if os.path.splitext(p)[1].lower() in ALL_EXT:
                files.append(p)
        else:
            for root, dirs, names in os.walk(p):
                dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
                for n in sorted(names):
                    if os.path.splitext(n)[1].lower() in ALL_EXT:
                        files.append(os.path.join(root, n))
    return files


def read_text(path):
    try:
        with open(path, encoding='utf-8', errors='replace') as f:
            text = f.read()
    except OSError:
        return None
    if len(text) > 2_000_000 or any(len(l) > 5000 for l in text.splitlines()[:200]):
        return None  # generated/minified — receipts from these aren't actionable
    return text


def css_sources(path, text):
    """Yield (css_text, line_offset) for style files and embedded <style> blocks."""
    ext = os.path.splitext(path)[1].lower()
    if ext in STYLE_EXT:
        yield text, 0
    elif ext in MARKUP_EXT:
        for m in RE_STYLE_TAG.finditer(text):
            yield m.group(1), line_of(text, m.start(1)) - 1


# ---------- rules ----------

def lint_lines(path, text, ext, findings):
    for i, line in enumerate(text.splitlines(), 1):
        if '.replace(' in line:  # '$1' backreferences look like money
            continue
        m = RE_HYPHEN_NEG.search(line)
        if m:
            findings.append(finding(
                'MD001', SEV_ERROR, path, i,
                f'hyphen-minus in negative amount {m.group(0)!r}',
                'use true minus U+2212 (−$320.11) — or parentheses, but never mix'))
        m = RE_SYM_SPACE.search(line)
        if m:
            findings.append(finding(
                'MD002', SEV_ERROR, path, i,
                f'space between currency symbol and amount {m.group(0)!r}',
                'set the symbol tight: $1,234.56 — disambiguate with US$/USD, not a gap'))
        if ext in MARKUP_EXT:
            m = RE_WHOLE.search(line)
            if m:
                findings.append(finding(
                    'MD003', SEV_INFO, path, i,
                    f'whole-dollar amount {m.group(0)!r} — cents dropped?',
                    'transactional surfaces show cents ($12.00); marketing may round'))
            if 'text-red-' in line and RE_MONEYISH.search(line):
                findings.append(finding(
                    'MD005', SEV_WARN, path, i,
                    'red styling on a money value — negative by color alone?',
                    'pair color with a minus sign (U+2212) or icon; color is never the only signal'))
        if ext in SCRIPT_EXT:
            if RE_CONCAT.search(line) or RE_TPL_DOLLAR.search(line):
                findings.append(finding(
                    'MD007', SEV_WARN, path, i,
                    'hand-rolled money formatting (currency symbol concatenated)',
                    "new Intl.NumberFormat(locale, {style:'currency', currency:'USD'}).format(n)"))
            elif RE_TOFIXED.search(line) and (RE_AMOUNT.search(line)
                                              or re.search(r'price|amount|balance|total|fee|cost',
                                                           line, re.I)):
                findings.append(finding(
                    'MD007', SEV_WARN, path, i,
                    '.toFixed(2) on a money value — locale-blind formatting',
                    'Intl.NumberFormat handles decimals, grouping, and locale honesty'))


def lint_css(path, css, offset, cssvars, findings):
    css = blank_comments(css)
    for m in RE_BLOCK.finditer(css):
        selector = m.group(1).strip().splitlines()[-1].strip()
        body = m.group(2)
        sel_pos = m.start(1) + len(m.group(1)) - len(m.group(1).lstrip())
        line = line_of(css, sel_pos) + offset
        decls = dict(re.findall(r'([-\w]+)\s*:\s*([^;]+)', body))
        color_val = decls.get('color')
        if RE_NEG_SELECTOR.search(selector) and color_val and 'content' not in decls:
            findings.append(finding(
                'MD005', SEV_WARN, path, line,
                f'{selector!r} styles a negative state with color — sign or icon present?',
                'ensure a minus sign (U+2212) or icon accompanies the color'))
        if RE_FEE_SELECTOR.search(selector) and color_val:
            fg = parse_color(color_val, cssvars)
            if fg:
                bg_val = decls.get('background-color') or decls.get('background')
                bg = parse_color(bg_val, cssvars) if bg_val else None
                assumed = bg is None
                ratio = wcag_ratio(fg, bg or (255, 255, 255))
                if ratio < 4.5:
                    against = 'declared background' if not assumed else 'assumed white background — verify'
                    findings.append(finding(
                        'MD006', SEV_WARN if assumed else SEV_ERROR, path, line,
                        f'{selector!r} fee/disclosure text at {ratio:.2f}:1 ({against})',
                        'fee lines never recede below 4.5:1 — "de-emphasized" fees are a '
                        'dark pattern AND a WCAG failure'))


def lint_tabular(path, text, ext, global_tabular, findings):
    if ext not in MARKUP_EXT:
        return
    amounts = list(RE_AMOUNT.finditer(text))
    if len(amounts) < 3 or RE_TABULAR.search(text):
        return
    first = line_of(text, amounts[0].start())
    if global_tabular:
        findings.append(finding(
            'MD004', SEV_INFO, path, first,
            f'{len(amounts)} money amounts; tabular-nums exists in project styles '
            f'but not in this file — verify it applies here',
            'font-variant-numeric: tabular-nums on every aligning/changing number'))
    else:
        findings.append(finding(
            'MD004', SEV_WARN, path, first,
            f'{len(amounts)} money amounts and no tabular-nums anywhere in the scanned files',
            'font-variant-numeric: tabular-nums (or the tabular-nums utility class) '
            'on amounts in lists, tables, tickers'))


# ---------- driver ----------

def run(paths, rules_filter):
    files = collect_files(paths)
    texts = {}
    for p in files:
        t = read_text(p)
        if t is not None:
            texts[p] = t

    cssvars = {}
    for p, t in texts.items():
        for src, _ in css_sources(p, t):
            cssvars.update(RE_VARDEF.findall(blank_comments(src)))
    global_tabular = any(RE_TABULAR.search(t) for t in texts.values())

    findings = []
    for p, t in sorted(texts.items()):
        ext = os.path.splitext(p)[1].lower()
        lint_lines(p, t, ext, findings)
        lint_tabular(p, t, ext, global_tabular, findings)
        for src, offset in css_sources(p, t):
            lint_css(p, src, offset, cssvars, findings)

    if rules_filter:
        findings = [f for f in findings if f['rule'] in rules_filter]
    findings.sort(key=lambda f: (f['path'], f['line'], f['rule']))
    return findings, len(texts)


def main():
    ap = argparse.ArgumentParser(
        description='Lint money display craft in CSS/HTML/JSX — see references/money-display.md')
    ap.add_argument('paths', nargs='*', default=['.'])
    ap.add_argument('--json', action='store_true', help='machine-readable output')
    ap.add_argument('--rules', help='comma-separated rule ids to run (e.g. MD001,MD006)')
    args = ap.parse_args()

    rules_filter = set(args.rules.upper().split(',')) if args.rules else None
    findings, nfiles = run(args.paths or ['.'], rules_filter)

    if args.json:
        json.dump(findings, sys.stdout, indent=2)
        print()
    else:
        by_path = {}
        for f in findings:
            by_path.setdefault(f['path'], []).append(f)
        for path, items in by_path.items():
            print(path)
            for f in items:
                print(f"  {f['line']:>4}  {f['rule']} {f['severity']:<5} {f['message']}")
                print(f"        ↳ {f['fix']}")
            print()
        counts = {s: sum(1 for f in findings if f['severity'] == s)
                  for s in (SEV_ERROR, SEV_WARN, SEV_INFO)}
        print(f"{counts[SEV_ERROR]} errors, {counts[SEV_WARN]} warnings, "
              f"{counts[SEV_INFO]} info — {nfiles} files scanned")

    sys.exit(1 if any(f['severity'] == SEV_ERROR for f in findings) else 0)


if __name__ == '__main__':
    main()
