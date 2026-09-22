"""Candado de la regla 20: no se escribe codigo sin PLAN.md en disco.

Evento: PreToolUse con matcher `Edit|Write`.
Bloquea con la salida estructurada documentada para PreToolUse:

    {"hookSpecificOutput": {"hookEventName": "PreToolUse",
                            "permissionDecision": "deny",
                            "permissionDecisionReason": "..."}}

Politica de fallo (regla de `hooks-and-guardrails`, seccion 6):
  - Ante la condicion que vigila (no hay plan): CERRADO -> deny.
  - Ante un error interno del propio hook: ABIERTO -> exit 0, jamas romper la sesion.

Uso:
    python plan_gate.py            # modo hook, lee el evento JSON por stdin
    python plan_gate.py --self-test
    python plan_gate.py --help
"""

from __future__ import annotations

import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from plan_lib import GLOB_PLANES, buscar_planes, leer_evento  # noqa: E402

#: Desactiva el candado. Todo uso queda registrado (nunca un bypass silencioso).
ENV_OVERRIDE = "PASANAKU_PLAN_GATE_OFF"

#: Sufijos que el candado nunca bloquea: documentacion y notas.
SUFIJOS_LIBRES = {".md", ".markdown", ".txt", ".rst"}

#: Prefijos de ruta (relativos a la raiz) siempre permitidos.
PREFIJOS_LIBRES = ("docs/", ".claude/")

MENSAJE = """\
Regla 20 - plan obligatorio: no existe ningun PLAN.md y estas por escribir codigo.

Antes del primer Edit/Write de codigo, crea:

  docs/trabajo/{fecha}-<slug>/PLAN.md

con las tres capas obligatorias y su criterio de aceptacion + Definition of Done:

  ## H1 - <hito>            **CA:** dado/cuando/entonces   **DoD:** <comandos>   **Estado:** TODO
  ### H1.S1 - <subtarea>    **CA:** ...                    **DoD:** ...          **Estado:** TODO
  | ID | Microtarea | CA (binario) | DoD (comando) | Estado |
  | H1.S1.M1 | ... | ... | `<comando>` | TODO |

El detalle esta en .claude/rules/20-plan-obligatorio.md y en la skill `milestone-planning`.
Escribir archivos .md y cualquier cosa bajo docs/ nunca se bloquea: podes crear el plan ya.

Bypass deliberado (queda registrado): setear {env}=1"""


def _raiz(evento: dict) -> Path:
    bruto = os.environ.get("CLAUDE_PROJECT_DIR") or evento.get("cwd") or os.getcwd()
    return Path(bruto).resolve()


def _registrar_bypass(raiz: Path, destino: str) -> None:
    """Un bypass sin rastro convierte el candado en decoracion."""
    try:
        runtime = raiz / ".claude" / "runtime"
        runtime.mkdir(parents=True, exist_ok=True)
        marca = datetime.now(timezone.utc).isoformat(timespec="seconds")
        linea = f"{marca}\tplan_gate\t{ENV_OVERRIDE}=1\t{destino}\n"
        with (runtime / "bypass.log").open("a", encoding="utf-8") as fh:
            fh.write(linea)
    except OSError:
        pass  # el registro nunca debe impedir el trabajo


def _ruta_relativa(destino: Path, raiz: Path) -> str | None:
    try:
        return destino.resolve().relative_to(raiz).as_posix()
    except (ValueError, OSError):
        return None


def decidir(evento: dict, raiz: Path, patron: str = GLOB_PLANES) -> tuple[bool, str]:
    """(permitir, razon). Pura: no toca stdin ni stdout, por eso es testeable."""
    herramienta = evento.get("tool_name") or ""
    if herramienta not in ("Edit", "Write", "NotebookEdit"):
        return True, f"herramienta {herramienta!r} fuera del alcance del candado"

    entrada = evento.get("tool_input") or {}
    bruto = entrada.get("file_path") or entrada.get("notebook_path") or ""
    if not bruto:
        return True, "el evento no trae file_path; no hay nada que evaluar"

    destino = Path(bruto)
    if not destino.is_absolute():
        destino = raiz / destino

    if destino.suffix.lower() in SUFIJOS_LIBRES:
        return True, "archivo de documentacion: siempre permitido"

    relativa = _ruta_relativa(destino, raiz)
    if relativa is None:
        return True, "ruta fuera del proyecto (scratchpad, temporales): fuera de alcance"
    if relativa.startswith(PREFIJOS_LIBRES):
        return True, "ruta bajo docs/ o .claude/: siempre permitida"

    if buscar_planes(raiz, patron):
        return True, "existe al menos un PLAN.md"

    return False, MENSAJE.format(
        fecha=datetime.now().strftime("%Y-%m-%d"), env=ENV_OVERRIDE
    )


def main() -> int:
    if "--help" in sys.argv or "-h" in sys.argv:
        print(__doc__)
        return 0
    if "--self-test" in sys.argv:
        return self_test()

    evento, error = leer_evento()
    if evento is None:
        # Falla ABIERTO, pero nunca en silencio: un candado mudo es indepurable.
        print(f"plan_gate: no pude leer el evento ({error}); se permite la accion", file=sys.stderr)
        return 0

    try:
        raiz = _raiz(evento)
        if os.environ.get(ENV_OVERRIDE):
            entrada = evento.get("tool_input") or {}
            _registrar_bypass(raiz, str(entrada.get("file_path", "?")))
            print(f"plan_gate: candado desactivado por {ENV_OVERRIDE}", file=sys.stderr)
            return 0

        permitir, razon = decidir(evento, raiz)
        if permitir:
            return 0

        salida = {
            "hookSpecificOutput": {
                "hookEventName": "PreToolUse",
                "permissionDecision": "deny",
                "permissionDecisionReason": razon,
            }
        }
        print(json.dumps(salida, ensure_ascii=False))
        return 0
    except Exception as exc:  # falla abierto: un bug del hook no frena al equipo
        print(f"plan_gate: error interno, se permite la accion ({exc})", file=sys.stderr)
        return 0


# --- Autoprueba -----------------------------------------------------------------

def self_test() -> int:
    import tempfile

    casos_ok: list[str] = []
    casos_fallo: list[str] = []

    def chequear(nombre: str, obtenido, esperado) -> None:
        if obtenido == esperado:
            casos_ok.append(nombre)
        else:
            casos_fallo.append(f"{nombre}: esperado {esperado!r}, obtenido {obtenido!r}")

    with tempfile.TemporaryDirectory() as tmp:
        raiz = Path(tmp).resolve()
        (raiz / "src").mkdir(parents=True, exist_ok=True)
        (raiz / "docs").mkdir(parents=True, exist_ok=True)

        def ev(path, tool="Write"):
            return {"tool_name": tool, "tool_input": {"file_path": str(path)}, "cwd": str(raiz)}

        # --- Sin plan en disco ---
        chequear("sin plan: bloquea codigo .ts",
                 decidir(ev(raiz / "src" / "a.ts"), raiz)[0], False)
        chequear("sin plan: bloquea codigo sin extension conocida",
                 decidir(ev(raiz / "src" / "Dockerfile"), raiz)[0], False)
        chequear("sin plan: permite .md",
                 decidir(ev(raiz / "src" / "notas.md"), raiz)[0], True)
        chequear("sin plan: permite bajo docs/",
                 decidir(ev(raiz / "docs" / "trabajo" / "x" / "PLAN.md"), raiz)[0], True)
        chequear("sin plan: permite bajo .claude/",
                 decidir(ev(raiz / ".claude" / "hooks" / "x.py"), raiz)[0], True)
        chequear("sin plan: permite ruta fuera del proyecto",
                 decidir(ev(Path(tempfile.gettempdir()) / "otro" / "z.ts"), raiz)[0], True)
        chequear("sin plan: ignora herramienta fuera de alcance",
                 decidir(ev(raiz / "src" / "a.ts", tool="Bash"), raiz)[0], True)
        chequear("sin plan: evento sin file_path no bloquea",
                 decidir({"tool_name": "Write", "tool_input": {}}, raiz)[0], True)
        chequear("mensaje de bloqueo nombra la regla",
                 "Regla 20" in decidir(ev(raiz / "src" / "a.ts"), raiz)[1], True)

        # --- Con plan en disco ---
        destino = raiz / "docs" / "trabajo" / "2026-01-01-demo"
        destino.mkdir(parents=True, exist_ok=True)
        (destino / "PLAN.md").write_text("# Plan\n", encoding="utf-8")
        chequear("con plan: permite codigo",
                 decidir(ev(raiz / "src" / "a.ts"), raiz)[0], True)

        # --- Plan mal formado no debe bloquear (regla: solo bloquea la ausencia) ---
        (destino / "PLAN.md").write_text("basura \x00 sin estructura", encoding="utf-8")
        chequear("plan ilegible: igual permite (no bloquea por formato)",
                 decidir(ev(raiz / "src" / "a.ts"), raiz)[0], True)

    for nombre in casos_ok:
        print(f"PASS  {nombre}")
    for fallo in casos_fallo:
        print(f"FAIL  {fallo}")
    print(f"\nplan_gate self-test: {len(casos_ok)} PASS, {len(casos_fallo)} FAIL")
    return 1 if casos_fallo else 0


if __name__ == "__main__":
    sys.exit(main())
