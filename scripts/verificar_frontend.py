#!/usr/bin/env python3
"""Las reglas propias del frontend como barrido de texto (planes/10 §6).

    python3 scripts/verificar_frontend.py diseno|ui|movil|backoffice|web|todo

Corre en el `lint` de cada app hasta que F1 las escriba como custom_lint (Flutter) y
reglas de angular-eslint (Angular). Enumera; no revisa.

  1 · sin red en vista        dio/http/HttpClient/fetch fuera de dominio/ y nucleo/
  2 · sin literal de diseno   Color(0x, Colors., EdgeInsets.all(N), fontFamily:, hex, px sueltos fuera de tokens
  3 · sin formato de dinero   NumberFormat, toStringAsFixed, toFixed, Intl.NumberFormat fuera de Monto/formatear
  4 · sin plataforma en vista Platform.is* fuera de infraestructura/ (ADR-036)
  5 · tamano                  archivo de pantalla/componente > 200 lineas bloquea
  6 · sin print               print/debugPrint/console.* en runtime
  7 · shell intacto           (informativo) que archivos del shell existen
"""
import pathlib
import re
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

R = pathlib.Path(__file__).resolve().parent.parent
fallas = []


def check(ok, msg):
    print(f"  {'OK   ' if ok else 'FALLA'} · {msg}")
    if not ok:
        fallas.append(msg)


def archivos(raiz, sufijos, excluir=()):
    for p in raiz.rglob("*"):
        if p.suffix in sufijos and p.is_file() and not any(e in p.as_posix() for e in excluir):
            yield p


def barrer(nombre, raiz, sufijos, patron, permitido, excluir=("/test/", "/generado/", ".spec.", ".g.dart", "node_modules", "/e2e/")):
    hallazgos = []
    for p in archivos(raiz, sufijos, excluir):
        if permitido(p):
            continue
        for n, linea in enumerate(p.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
            if re.search(patron, linea) and "// permitido:" not in linea and "/* permitido:" not in linea:
                hallazgos.append(f"{p.relative_to(R)}:{n}")
    check(not hallazgos, f"{nombre} {hallazgos[:5] or ''}")


def movil():
    raiz = R / "apps/movil/lib"
    print("=== apps/movil (Flutter) ===")
    barrer("sin red en vista", raiz, {".dart"}, r"\b(Dio\(|HttpClient\(|http\.get|http\.post)",
           lambda p: "/dominio/" in p.as_posix())
    barrer("sin literal de diseño", raiz, {".dart"}, r"(Color\(0x|Colors\.(?!transparent)[a-z]|EdgeInsets\.(all|symmetric|only)\(\s*\d|fontSize:\s*\d|fontFamily:\s*')",
           lambda p: p.name == "tokens.dart")
    barrer("sin formato de dinero", raiz, {".dart"}, r"(NumberFormat|toStringAsFixed|double\.parse|num\.parse)",
           lambda p: p.name in ("formatear.dart", "monto.dart"))
    barrer("sin plataforma en vista", raiz, {".dart"}, r"Platform\.is[A-Z]",
           lambda p: "/infraestructura/" in p.as_posix())
    barrer("sin print", raiz, {".dart"}, r"\b(print|debugPrint)\(", lambda p: False)
    grandes = [f"{p.relative_to(R)} ({len(p.read_text().splitlines())})" for p in archivos(raiz, {".dart"}, ("tokens.dart",))
               if len(p.read_text().splitlines()) > 200]
    check(not grandes, f"ningún archivo de más de 200 líneas {grandes or ''}")
    for shell in ("navegacion/rutas.dart", "dominio/cliente.dart", "proveedores/sesion.dart"):
        check((raiz / shell).exists(), f"el shell tiene {shell}")


def angular(app):
    raiz = R / f"apps/{app}/src/app"
    print(f"=== apps/{app} (Angular) ===")
    barrer("sin red en vista", raiz, {".ts"}, r"((?<!provide)HttpClient\b|fetch\(|XMLHttpRequest)",
           lambda p: "/nucleo/" in p.as_posix() or "/dominio/" in p.as_posix() or p.name.endswith(".server.ts") or p.name == "app.config.ts")
    barrer("sin literal de diseño", raiz, {".ts", ".html", ".css"}, r"(#[0-9a-fA-F]{3,6}\b|(?<![\w-])\d+px\b|font-family:(?!\s*var\())",
           lambda p: False)
    barrer("sin formato de dinero", raiz, {".ts", ".html"}, r"(toFixed\(|Intl\.NumberFormat|'Bs '|\"Bs \")",
           lambda p: p.name.startswith("monto") or p.name.startswith("formatear"))
    barrer("sin console", raiz, {".ts"}, r"console\.(log|warn|error)\(", lambda p: p.name == "main.ts")
    grandes = [f"{p.relative_to(R)} ({len(p.read_text().splitlines())})" for p in archivos(raiz, {".ts", ".html"})
               if len(p.read_text().splitlines()) > 200]
    check(not grandes, f"ningún archivo de más de 200 líneas {grandes or ''}")
    ui = R / "packages/ui/src"
    if ui.exists():
        barrer("sin literal de diseño en @aportaya/ui", ui, {".ts", ".html", ".css"}, r"(#[0-9a-fA-F]{3,6}\b|(?<![\w-])\d+px\b|font-family:(?!\s*var\())", lambda p: False)


def diseno():
    raiz = R / "packages/diseno_flutter/lib"
    print("=== packages/diseno_flutter (Flutter) ===")
    barrer("sin red en el sistema de diseño", raiz, {".dart"}, r"\b(Dio\(|HttpClient\(|http\.get|http\.post)", lambda p: False)
    barrer("sin literal de diseño", raiz, {".dart"}, r"(Color\(0x|Colors\.(?!transparent)[a-z]|EdgeInsets\.(all|symmetric|only)\(\s*\d|fontSize:\s*\d|fontFamily:\s*')",
           lambda p: p.name == "tokens.dart")
    barrer("sin formato de dinero", raiz, {".dart"}, r"(NumberFormat|toStringAsFixed|double\.parse|num\.parse)",
           lambda p: p.name in ("formatear.dart", "monto.dart"))
    barrer("sin plataforma", raiz, {".dart"}, r"Platform\.is[A-Z]", lambda p: False)
    barrer("sin print", raiz, {".dart"}, r"\b(print|debugPrint)\(", lambda p: False)
    grandes = [f"{p.relative_to(R)} ({len(p.read_text().splitlines())})" for p in archivos(raiz, {".dart"}, ("tokens.dart", "/catalogo/"))
               if len(p.read_text().splitlines()) > 200]
    check(not grandes, f"ningún archivo de más de 200 líneas (catalogo/ es datos y queda fuera) {grandes or ''}")


def ui():
    raiz = R / "packages/ui/src"
    print("=== packages/ui (Angular) ===")
    barrer("sin red en el sistema de diseño", raiz, {".ts"}, r"(HttpClient\b|fetch\(|XMLHttpRequest)", lambda p: False)
    barrer("sin literal de diseño", raiz, {".ts", ".html", ".css"}, r"(#[0-9a-fA-F]{3,6}\b|(?<![\w-])\d+px\b|font-family:(?!\s*var\())",
           lambda p: False)
    barrer("sin formato de dinero", raiz, {".ts", ".html"}, r"(toFixed\(|Intl\.NumberFormat|parseFloat\(|'Bs '|\"Bs \")",
           lambda p: p.name.startswith("monto") or p.name.startswith("formatear"))
    barrer("sin console", raiz, {".ts"}, r"console\.(log|warn|error)\(", lambda p: False)
    grandes = [f"{p.relative_to(R)} ({len(p.read_text().splitlines())})" for p in archivos(raiz, {".ts"}, ("/catalogo/",))
               if len(p.read_text().splitlines()) > 200]
    check(not grandes, f"ningún archivo de más de 200 líneas (catalogo/ es datos y queda fuera) {grandes or ''}")
    piezas = sorted(d.name for d in raiz.iterdir() if d.is_dir() and d.name not in ("catalogo",))
    sin_archivo = [d for d in piezas if not (raiz / d / f"{d}.ts").exists()]
    check(not sin_archivo, f"cada pieza tiene su archivo con el mismo nombre {sin_archivo or ''}")


def main():
    que = sys.argv[1] if len(sys.argv) > 1 else "todo"
    if que in ("diseno", "todo"):
        diseno()
    if que in ("ui", "todo"):
        ui()
    if que in ("movil", "todo"):
        movil()
    if que in ("backoffice", "todo"):
        angular("backoffice")
    if que in ("web", "todo"):
        angular("web")
    print()
    if fallas:
        print(f"{len(fallas)} FALLAS")
        return 1
    print("TODO OK")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
