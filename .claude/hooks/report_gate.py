"""Candado de la regla 40: no se cierra la sesion sin REPORTE.md completo.

Evento: Stop. La doc oficial confirma que `Stop` bloquea con **exit code 2** y que la
razon se escribe en stderr; no documenta una salida JSON de decision para Stop, asi que
este hook usa exclusivamente exit 2 + stderr.

GUARD ANTI-BUCLE (importante): la documentacion oficial de hooks **no menciona** ningun
campo `stop_hook_active`, y tampoco documenta un mecanismo contra bucles en `Stop`. Por
eso este hook NO depende de ese campo: lleva su propio contador por sesion en
`.claude/runtime/report_gate_state.json` y despues de MAX_BLOQUEOS avisa y deja pasar,
de modo que una sesion nunca puede quedar trabada. Si el campo existiera, igual se
respeta (leerlo es inofensivo).

Politica de fallo: ante error interno, ABIERTO (exit 0). Un bug del candado no puede
dejar a nadie sin poder cerrar.

Uso:
    python report_gate.py            # modo hook, lee el evento JSON por stdin
    python report_gate.py --self-test
    python report_gate.py --help
"""

from __future__ import annotations

import json
import os
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from plan_lib import (  # noqa: E402
    GLOB_PLANES,
    SECCIONES_REPORTE,
    leer_evento,
    trabajos_activos,
)

ENV_OVERRIDE = "PASANAKU_REPORT_GATE_OFF"

#: Cuantas veces seguidas puede bloquear antes de rendirse y dejar cerrar.
MAX_BLOQUEOS = 2
#: Las marcas viejas se descartan para que el archivo no crezca ni trabe sesiones nuevas.
VIGENCIA_HORAS = 6

ARCHIVO_ESTADO = Path(".claude") / "runtime" / "report_gate_state.json"


def _raiz(evento: dict) -> Path:
    bruto = os.environ.get("CLAUDE_PROJECT_DIR") or evento.get("cwd") or os.getcwd()
    return Path(bruto).resolve()


def _ahora() -> datetime:
    return datetime.now(timezone.utc)


def _leer_estado(raiz: Path) -> dict:
    try:
        ruta = raiz / ARCHIVO_ESTADO
        if not ruta.is_file():
            return {}
        datos = json.loads(ruta.read_text(encoding="utf-8"))
        return datos if isinstance(datos, dict) else {}
    except Exception:
        return {}


def _guardar_estado(raiz: Path, estado: dict) -> None:
    try:
        ruta = raiz / ARCHIVO_ESTADO
        ruta.parent.mkdir(parents=True, exist_ok=True)
        ruta.write_text(json.dumps(estado, ensure_ascii=False, indent=1), encoding="utf-8")
    except OSError:
        pass


def _purgar(estado: dict) -> dict:
    limite = _ahora() - timedelta(hours=VIGENCIA_HORAS)
    limpio = {}
    for clave, valor in estado.items():
        try:
            marca = datetime.fromisoformat(str(valor.get("ultimo")))
            if marca.tzinfo is None:
                marca = marca.replace(tzinfo=timezone.utc)
            if marca >= limite:
                limpio[clave] = valor
        except Exception:
            continue
    return limpio


def _contar_bloqueo(raiz: Path, sesion: str) -> int:
    estado = _purgar(_leer_estado(raiz))
    entrada = estado.get(sesion) or {"bloqueos": 0}
    entrada["bloqueos"] = int(entrada.get("bloqueos", 0)) + 1
    entrada["ultimo"] = _ahora().isoformat(timespec="seconds")
    estado[sesion] = entrada
    _guardar_estado(raiz, estado)
    return entrada["bloqueos"]


def _limpiar_sesion(raiz: Path, sesion: str) -> None:
    estado = _purgar(_leer_estado(raiz))
    if sesion in estado:
        estado.pop(sesion, None)
        _guardar_estado(raiz, estado)


def construir_mensaje(activos: list[dict]) -> str:
    """Mensaje de bloqueo: dice exactamente que falta y donde."""
    partes = ["Regla 40 - reporte obligatorio: hay trabajo activo sin reporte completo.", ""]
    for item in activos:
        carpeta = item["plan"].parent
        res = item["resumen"]
        partes.append(f"* {carpeta.as_posix()}/")
        partes.append(
            f"    microtareas: {res['hechas']}/{res['total']} HECHO ({res['porcentaje']}%)"
        )
        activas = ", ".join(m["id"] for m in res["activas"][:8]) or "ninguna"
        partes.append(f"    en TODO/EN CURSO: {activas}")
        if not item["reporte_existe"]:
            partes.append(f"    FALTA el archivo REPORTE.md en {carpeta.as_posix()}/")
        else:
            partes.append(
                "    REPORTE.md existe pero le faltan secciones: "
                + ", ".join(item["faltantes"])
            )
        partes.append("")
    partes.append(
        "El reporte debe tener SIEMPRE las tres secciones, aunque alguna diga 'ninguna': "
        + " / ".join(SECCIONES_REPORTE)
        + "."
    )
    partes.append(
        "Cada item en 'A medias' exige las cuatro respuestas: que anda, que no anda, "
        "que falta exactamente, donde quedo."
    )
    partes.append(
        "Las microtareas que queden en EN CURSO al cerrar deben pasar a A MEDIAS o BLOQUEADO "
        "con el detalle de lo que falta (regla 20, seccion 7)."
    )
    partes.append(f"Plantilla en .claude/rules/40-reporte-obligatorio.md. Bypass: {ENV_OVERRIDE}=1")
    return "\n".join(partes)


def evaluar(raiz: Path, patron: str = GLOB_PLANES) -> tuple[bool, str, list[dict]]:
    """(bloquear, mensaje, activos). Pura respecto de stdin/stdout."""
    activos = trabajos_activos(raiz, patron)
    if not activos:
        return False, "", []
    incompletos = [a for a in activos if not a["reporte_existe"] or a["faltantes"]]
    if not incompletos:
        return False, "", activos
    return True, construir_mensaje(incompletos), incompletos


def main() -> int:
    if "--help" in sys.argv or "-h" in sys.argv:
        print(__doc__)
        return 0
    if "--self-test" in sys.argv:
        return self_test()

    evento, error = leer_evento()
    if evento is None:
        print(f"report_gate: no pude leer el evento ({error}); se permite cerrar", file=sys.stderr)
        return 0

    try:
        # Campo no documentado; si algun dia existe, respetarlo es correcto.
        if evento.get("stop_hook_active"):
            return 0

        raiz = _raiz(evento)
        sesion = str(evento.get("session_id") or "sin-sesion")

        if os.environ.get(ENV_OVERRIDE):
            print(f"report_gate: candado desactivado por {ENV_OVERRIDE}", file=sys.stderr)
            return 0

        bloquear, mensaje, _ = evaluar(raiz)
        if not bloquear:
            _limpiar_sesion(raiz, sesion)
            return 0

        veces = _contar_bloqueo(raiz, sesion)
        if veces > MAX_BLOQUEOS:
            _limpiar_sesion(raiz, sesion)
            print(
                "report_gate: ya se bloqueo "
                f"{MAX_BLOQUEOS} veces sin que apareciera el reporte; se permite cerrar "
                "para no trabar la sesion. EL REPORTE SIGUE FALTANDO.",
                file=sys.stderr,
            )
            return 0

        print(mensaje, file=sys.stderr)
        return 2  # mecanismo verificado de bloqueo para Stop
    except Exception as exc:
        print(f"report_gate: error interno, se permite cerrar ({exc})", file=sys.stderr)
        return 0


# --- Autoprueba -----------------------------------------------------------------

PLAN_ACTIVO = """# Plan - demo

## H1 - hito
**CA:** dado x cuando y entonces z
**DoD:** `yarn test`
**Estado:** EN CURSO

### H1.S1 - subtarea
**CA:** ...
**DoD:** ...
**Estado:** EN CURSO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | hacer a | ... | `yarn test a` | HECHO |
| H1.S1.M2 | hacer b | ... | `yarn test b` | EN CURSO |
| H1.S1.M3 | hacer c | ... | `yarn test c` | TODO |
"""

PLAN_CERRADO = PLAN_ACTIVO.replace("| EN CURSO |", "| HECHO |").replace("| TODO |", "| HECHO |")

REPORTE_COMPLETO = """# Reporte - demo
## Completado
| H1.S1.M1 | listo | `yarn test a` | PASS |
## A medias
### H1.S1.M2
- Que anda: ...
## Pendiente
ninguna
"""

REPORTE_INCOMPLETO = """# Reporte - demo
## Completado
nada
"""


def self_test() -> int:
    import tempfile

    ok: list[str] = []
    fallos: list[str] = []

    def chequear(nombre, obtenido, esperado):
        if obtenido == esperado:
            ok.append(nombre)
        else:
            fallos.append(f"{nombre}: esperado {esperado!r}, obtenido {obtenido!r}")

    with tempfile.TemporaryDirectory() as tmp:
        raiz = Path(tmp).resolve()
        carpeta = raiz / "docs" / "trabajo" / "2026-01-01-demo"
        carpeta.mkdir(parents=True, exist_ok=True)

        # Sin ningun plan: nada que exigir.
        chequear("sin planes: no bloquea", evaluar(raiz)[0], False)

        # Plan activo sin reporte: bloquea.
        (carpeta / "PLAN.md").write_text(PLAN_ACTIVO, encoding="utf-8")
        bloquea, mensaje, _ = evaluar(raiz)
        chequear("plan activo sin reporte: bloquea", bloquea, True)
        chequear("mensaje nombra el archivo faltante", "FALTA el archivo REPORTE.md" in mensaje, True)
        chequear("mensaje trae porcentaje calculado", "1/3 HECHO (33.3%)" in mensaje, True)
        chequear("mensaje lista las microtareas vivas", "H1.S1.M2" in mensaje and "H1.S1.M3" in mensaje, True)

        # Reporte incompleto: sigue bloqueando y dice que secciones faltan.
        (carpeta / "REPORTE.md").write_text(REPORTE_INCOMPLETO, encoding="utf-8")
        bloquea, mensaje, _ = evaluar(raiz)
        chequear("reporte sin secciones: bloquea", bloquea, True)
        chequear("mensaje nombra A MEDIAS faltante", "A MEDIAS" in mensaje, True)
        chequear("mensaje nombra PENDIENTE faltante", "PENDIENTE" in mensaje, True)

        # Reporte completo: deja cerrar.
        (carpeta / "REPORTE.md").write_text(REPORTE_COMPLETO, encoding="utf-8")
        chequear("reporte completo: no bloquea", evaluar(raiz)[0], False)

        # Plan sin trabajo vivo: no exige nada aunque no haya reporte.
        (carpeta / "REPORTE.md").unlink()
        (carpeta / "PLAN.md").write_text(PLAN_CERRADO, encoding="utf-8")
        chequear("plan todo HECHO sin reporte: no bloquea", evaluar(raiz)[0], False)

        # Plan ilegible: no debe trabar el cierre.
        (carpeta / "PLAN.md").write_text("basura sin estructura", encoding="utf-8")
        chequear("plan ilegible: no bloquea", evaluar(raiz)[0], False)

        # Guard anti-bucle: al pasar MAX_BLOQUEOS deja de bloquear.
        (carpeta / "PLAN.md").write_text(PLAN_ACTIVO, encoding="utf-8")
        sesion = "sesion-de-prueba"
        cuentas = [_contar_bloqueo(raiz, sesion) for _ in range(MAX_BLOQUEOS + 1)]
        chequear("contador de bloqueos incrementa", cuentas, [1, 2, 3])
        chequear("supera el tope tras MAX_BLOQUEOS", cuentas[-1] > MAX_BLOQUEOS, True)
        _limpiar_sesion(raiz, sesion)
        chequear("limpiar sesion reinicia el contador", _contar_bloqueo(raiz, sesion), 1)

    for nombre in ok:
        print(f"PASS  {nombre}")
    for f in fallos:
        print(f"FAIL  {f}")
    print(f"\nreport_gate self-test: {len(ok)} PASS, {len(fallos)} FAIL")
    return 1 if fallos else 0


if __name__ == "__main__":
    sys.exit(main())
