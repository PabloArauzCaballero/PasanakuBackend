#!/usr/bin/env python3
"""Cruce de requisitos: PLAN-PROVEEDORES.md -> documentos de docs/proveedores/.

Verifica que cada requisito (RF-PXn-xx, RM-xx) y cada criterio de selección del plan de
proveedores aparezca en el documento que le corresponde. No modifica nada.

Uso (desde la raíz del repo):
  python3 docs/trabajo/2026-11-21-diseno-paralelo/evidencia/cruce_rfp.py <modo> [PLAN-PROVEEDORES.md]
  modo: plantilla | matriz | rfp | px9 | todo
El plan de proveedores vive hoy en la rama claude/nice-fermat-essf9e; por defecto se lee de
/tmp/plan/docs/trabajo/2026-11-21-plan-correcciones/PLAN-PROVEEDORES.md (worktree de esa rama).
Sale con código 1 si falta algo.
"""
import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parents[4]
PROV = RAIZ / "docs" / "proveedores"
PLAN_POR_DEFECTO = Path("/tmp/plan/docs/trabajo/2026-11-21-plan-correcciones/PLAN-PROVEEDORES.md")

RFP = {
    "PX-1": "rfp/PX-1-pasarela-de-cobro-qr.md",
    "PX-2": "rfp/PX-2-proveedor-de-desembolso.md",
    "PX-3": "rfp/PX-3-banco-custodio.md",
    "PX-4": "rfp/PX-4-verificacion-de-identidad.md",
    "PX-5": "rfp/PX-5-listas-y-pep.md",
    "PX-6": "rfp/PX-6-titularidad-de-cuenta.md",
    "PX-7": "rfp/PX-7-mensajeria.md",
    "PX-8": "rfp/PX-8-facturacion-electronica.md",
}
PX9 = "PX-9-central-de-riesgo-diseno.md"
CRITERIOS = ["Regulación", "Capacidades técnicas", "Seguridad", "Datos", "Disponibilidad", "Costo", "Salida"]
RM = [f"RM-{i:02d}" for i in range(1, 21)]


def ids_de_seccion(plan: str, px: str) -> list[str]:
    """IDs RF-PXn-xx que aparecen en la sección '## PX-n' del plan."""
    n = px.split("-")[1]
    m = re.search(rf"^## {px} .*?(?=^## |\Z)", plan, re.S | re.M)
    if not m:
        raise SystemExit(f"No encontré la sección {px} en el plan")
    return sorted(set(re.findall(rf"RF-PX{n}-\d\d", m.group(0))))


def fila(codigo: str, doc: str, texto: str) -> tuple[str, str, bool]:
    ok = re.search(rf"(?<![\w-]){re.escape(codigo)}(?![\w])", texto) is not None
    return codigo, doc, ok


def imprimir(filas, titulo):
    print(f"\n### {titulo}\n")
    print("| Requisito | Documento | Presente |")
    print("|---|---|---|")
    for c, d, ok in filas:
        print(f"| {c} | `{d}` | {'sí' if ok else '**NO**'} |")
    falt = [c for c, _, ok in filas if not ok]
    print(f"\n{len(filas) - len(falt)}/{len(filas)} presentes" + (f" · FALTAN: {', '.join(falt)}" if falt else ""))
    return falt


def modo_plantilla():
    doc = "00-plantilla-rfp.md"
    t = (PROV / doc).read_text(encoding="utf-8")
    return imprimir([fila(c, doc, t) for c in RM], "Plantilla de RFP: RM-01 a RM-20")


def modo_matriz():
    doc = "01-matriz-de-evaluacion.md"
    t = (PROV / doc).read_text(encoding="utf-8")
    filas = [(c, doc, f"| {c} |" in t) for c in CRITERIOS]
    filas.append(("pesos DECISION_REQUIRED", doc, t.count("DECISION_REQUIRED") >= len(CRITERIOS)))
    return imprimir(filas, "Matriz: siete criterios comunes y pesos abiertos")


def modo_rfp(plan: str, solo: str | None):
    falt = []
    for px, rel in RFP.items():
        if solo and px != solo:
            continue
        t = (PROV / rel).read_text(encoding="utf-8")
        filas = [fila(c, rel, t) for c in ids_de_seccion(plan, px)]
        filas += [fila(c, rel, t) for c in RM]
        for nivel in ("Correcto", "Límite", "Inválido"):
            filas.append((f"casos nivel {nivel}", rel, f"| {nivel} |" in t))
        for sec in ("## 7. Seguridad", "## 8. Datos", "## 9. Continuidad"):
            filas.append((f"sección «{sec[3:]}»", rel, sec in t))
        if px in ("PX-1", "PX-2"):
            filas.append(("segundo proveedor obligatorio", rel, "segundo proveedor" in t.lower()))
        if px == "PX-7":
            lineas = [l for l in t.splitlines() if "whatsapp" in l.lower()]
            filas.append(("WhatsApp solo como exclusión", rel,
                          all(re.search(r"sin whatsapp|no (se usa|incluye|es parte|se contrata)|excluid|E-11", l, re.I) for l in lineas)))
        falt += imprimir(filas, f"{px} — `{rel}`")
    return falt


def modo_px9(plan: str):
    t = (PROV / PX9).read_text(encoding="utf-8")
    filas = [fila(c, PX9, t) for c in ids_de_seccion(plan, "PX-9")]
    filas.append(("condicionado a L-04", PX9, "L-04" in t))
    return imprimir(filas, "PX-9 — diseño de consentimiento y reporte")


def main():
    if len(sys.argv) < 2:
        raise SystemExit(__doc__)
    modo = sys.argv[1]
    solo = sys.argv[2] if modo == "rfp" and len(sys.argv) > 2 and sys.argv[2].startswith("PX-") else None
    ruta = next((a for a in sys.argv[2:] if a.endswith(".md")), None)
    plan = Path(ruta or PLAN_POR_DEFECTO).read_text(encoding="utf-8")
    falt = []
    if modo in ("plantilla", "todo"):
        falt += modo_plantilla()
    if modo in ("matriz", "todo"):
        falt += modo_matriz()
    if modo in ("rfp", "todo"):
        falt += modo_rfp(plan, solo)
    if modo in ("px9", "todo"):
        falt += modo_px9(plan)
    print(f"\nTOTAL FALTANTES: {len(falt)}")
    sys.exit(1 if falt else 0)


if __name__ == "__main__":
    main()
