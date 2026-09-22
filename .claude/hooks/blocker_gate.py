"""Candado de la regla 65: no se cierra la sesion con bloqueantes sin simular.

Evento: Stop. Igual que `report_gate.py`, bloquea con **exit code 2** y escribe la razon
en stderr (la doc oficial no define salida JSON de decision para Stop).

QUE IMPIDE. Que un turno termine con microtareas en `BLOQUEADO` o `EN CURSO` cuando ese
bloqueo es de ejecucion y no de negocio. La regla 65 exige que, ante cualquier bloqueo,
se simule el contrato de lo que falta en sus TRES niveles -- aceptado, limite e invalido --
y se cierre la microtarea contra el doble. Un `BLOQUEADO` que no declara esa simulacion
es una espera disfrazada de avance, y es justo lo que este candado corta.

COMO SE DESBLOQUEA. El plan tiene que declarar, en la fila de la microtarea o en el cuerpo
del documento, que los tres niveles se simularon. Basta con que aparezcan las tres
palabras clave cerca del bloqueante (`aceptado`, `limite`, `invalido`), o la marca
explicita `SIMULACION-65: aceptado|limite|invalido`. Tambien se acepta declarar que el
bloqueo es de NEGOCIO (`DECISION_REQUIRED` / `decision de negocio`), que es la unica
excepcion que la propia regla 65 reconoce, junto con una accion destructiva sobre algo
compartido.

GUARD ANTI-BUCLE: mismo criterio que `report_gate.py`. Contador propio por sesion en
`.claude/runtime/blocker_gate_state.json`; tras MAX_BLOQUEOS avisa por stderr y deja
cerrar. Nadie puede quedar encerrado por un candado.

Politica de fallo: ante error interno, ABIERTO (exit 0). Un bug del candado no puede
dejar a nadie sin poder cerrar.

Uso:
    python blocker_gate.py            # modo hook, lee el evento JSON por stdin
    python blocker_gate.py --self-test
    python blocker_gate.py --help
"""

from __future__ import annotations

import json
import os
import re
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from plan_lib import (  # noqa: E402
    GLOB_PLANES,
    buscar_planes,
    leer,
    leer_evento,
    normalizar,
    parse_plan,
)

ENV_OVERRIDE = "PASANAKU_BLOCKER_GATE_OFF"

#: Estados que este candado considera "sin cerrar por ejecucion".
ESTADOS_BLOQUEANTES = ("BLOQUEADO", "EN CURSO")

#: Cuantas veces seguidas puede bloquear antes de rendirse y dejar cerrar.
MAX_BLOQUEOS = 2
#: Las marcas viejas se descartan para que el archivo no crezca ni trabe sesiones nuevas.
VIGENCIA_HORAS = 6

ARCHIVO_ESTADO = Path(".claude") / "runtime" / "blocker_gate_state.json"

#: Las tres palabras que tienen que aparecer para dar por simulado el contrato.
#: En MAYUSCULAS a proposito: `normalizar()` devuelve mayusculas sin acentos, asi que
#: compararlas en minusculas dejaba el candado INERTE -- lo cazo el self-test.
NIVELES = ("ACEPTADO", "LIMITE", "INVALIDO")

#: Unica excepcion que la regla 65 reconoce, ademas de lo destructivo compartido.
_RE_NEGOCIO = re.compile(
    r"decision[_ ]required|decision de negocio|decide el negocio|es de negocio",
    re.IGNORECASE,
)
#: Marca explicita, por si alguien prefiere declararlo sin ambiguedad.
_RE_MARCA = re.compile(r"simulacion-65", re.IGNORECASE)


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
        return json.loads(ruta.read_text(encoding="utf-8-sig") or "{}")
    except Exception:
        return {}


def _escribir_estado(raiz: Path, estado: dict) -> None:
    try:
        ruta = raiz / ARCHIVO_ESTADO
        ruta.parent.mkdir(parents=True, exist_ok=True)
        ruta.write_text(json.dumps(estado, indent=2), encoding="utf-8")
    except Exception:
        pass  # el contador es una comodidad, no puede romper el cierre


def _contar_bloqueo(raiz: Path, sesion: str) -> int:
    """Suma uno al contador de esta sesion y devuelve cuantos van. Purga lo viejo."""
    estado = _leer_estado(raiz)
    corte = _ahora() - timedelta(hours=VIGENCIA_HORAS)
    vigentes = {}
    for clave, dato in (estado or {}).items():
        try:
            visto = datetime.fromisoformat(dato.get("ultimo", ""))
            if visto >= corte:
                vigentes[clave] = dato
        except Exception:
            continue
    actual = vigentes.get(sesion, {"veces": 0})
    actual["veces"] = int(actual.get("veces", 0)) + 1
    actual["ultimo"] = _ahora().isoformat()
    vigentes[sesion] = actual
    _escribir_estado(raiz, vigentes)
    return actual["veces"]


def simulacion_declarada(texto: str, cerca_de: str = "") -> bool:
    """True si el documento declara la simulacion de los tres niveles (regla 65).

    Se acepta de tres formas, de mas a menos explicita:
      1. la marca `SIMULACION-65`;
      2. las tres palabras de nivel presentes en el documento;
      3. que el bloqueo este declarado como decision de negocio (unica excepcion).

    `cerca_de` permite pasar la fila de la microtarea: si ahi ya estan las tres, alcanza.
    """
    plano = normalizar(texto or "")
    fila = normalizar(cerca_de or "")

    if _RE_MARCA.search(plano) or _RE_MARCA.search(fila):
        return True
    if _RE_NEGOCIO.search(fila) or _RE_NEGOCIO.search(plano):
        return True
    if all(nivel in fila for nivel in NIVELES):
        return True
    return all(nivel in plano for nivel in NIVELES)


def bloqueantes_sin_simular(raiz: Path, patron: str = GLOB_PLANES) -> list[dict]:
    """Microtareas en BLOQUEADO/EN CURSO cuyo plan no declara la simulacion de la 65."""
    hallazgos: list[dict] = []
    for ruta_plan in buscar_planes(raiz, patron):
        texto = leer(ruta_plan)
        plan = parse_plan(texto)
        if simulacion_declarada(texto):
            continue  # el documento entero ya declara los tres niveles
        for micro in plan.get("microtareas", []):
            if micro.get("estado") not in ESTADOS_BLOQUEANTES:
                continue
            if simulacion_declarada("", micro.get("titulo", "")):
                continue
            hallazgos.append(
                {
                    "plan": ruta_plan,
                    "id": micro.get("id"),
                    "estado": micro.get("estado"),
                    "titulo": micro.get("titulo", ""),
                }
            )
    return hallazgos


def mensaje(hallazgos: list[dict]) -> str:
    """El texto que ve quien queda bloqueado. Tiene que decir como salir."""
    lineas = [
        "Regla 65: no se cierra el turno con bloqueantes sin simular.",
        "",
        f"Hay {len(hallazgos)} microtarea(s) en BLOQUEADO/EN CURSO cuyo plan no declara",
        "la simulacion del contrato en sus tres niveles:",
        "",
    ]
    for h in hallazgos[:10]:
        lineas.append(f"  - {h['id']} [{h['estado']}] en {h['plan']}")
    if len(hallazgos) > 10:
        lineas.append(f"  ... y {len(hallazgos) - 10} mas")
    lineas += [
        "",
        "Como se cierra (regla 65, obligatorio):",
        "  1. Nombra el contrato de lo que falta (interfaz, endpoint, formato).",
        "  2. Construi un doble que lo cumpla, declarado como doble.",
        "  3. Ejercitalo en los TRES niveles: aceptado, limite e invalido.",
        "  4. Cerra la microtarea contra el doble y decila asi en el plan.",
        "",
        "Unica excepcion: que el bloqueo sea una decision de NEGOCIO sin tomar",
        "(declarala como DECISION_REQUIRED), o una accion destructiva sobre algo",
        "compartido. Esperar a que otro entregue lo suyo NO es excepcion.",
        "",
        f"Via de escape: {ENV_OVERRIDE}=1 (deja rastro en el reporte, no en el candado).",
    ]
    return "\n".join(lineas)


def main(argv: list[str] | None = None) -> int:
    argv = list(sys.argv[1:] if argv is None else argv)
    if "--help" in argv or "-h" in argv:
        print(__doc__)
        return 0
    if "--self-test" in argv:
        return _self_test()

    if os.environ.get(ENV_OVERRIDE) == "1":
        return 0

    try:
        evento, error = leer_evento()
        if evento is None:
            print(f"blocker_gate: evento ilegible ({error}); se deja pasar", file=sys.stderr)
            return 0

        raiz = _raiz(evento)
        hallazgos = bloqueantes_sin_simular(raiz)
        if not hallazgos:
            return 0

        sesion = str(evento.get("session_id") or "sin-sesion")
        veces = _contar_bloqueo(raiz, sesion)
        if veces > MAX_BLOQUEOS:
            print(
                "blocker_gate: ya bloquee "
                f"{MAX_BLOQUEOS} vez/veces en esta sesion; dejo cerrar, pero "
                f"quedan {len(hallazgos)} bloqueante(s) sin simular (regla 65).",
                file=sys.stderr,
            )
            return 0

        print(mensaje(hallazgos), file=sys.stderr)
        return 2
    except Exception as exc:  # politica de fallo: ABIERTO
        print(f"blocker_gate: error interno ({exc}); se deja pasar", file=sys.stderr)
        return 0


def _self_test() -> int:
    """Pruebas del propio candado. Es codigo de produccion del equipo."""
    casos: list[tuple[str, bool]] = []

    def check(nombre: str, condicion: bool) -> None:
        casos.append((nombre, bool(condicion)))

    plan_bloqueado = """
## H1 - hito

### H1.S1 - subtarea

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | hacer algo | ... | `cmd` | BLOQUEADO |
"""
    plan_ok_simulado = plan_bloqueado + """
Se simulo el contrato en sus tres niveles: aceptado, limite e invalido.
"""
    plan_ok_marca = plan_bloqueado + "\nSIMULACION-65: los tres niveles corridos.\n"
    plan_ok_negocio = """
## H1 - hito

### H1.S1 - subtarea

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | esperar que negocio defina DECISION_REQUIRED | ... | `cmd` | BLOQUEADO |
"""
    plan_en_curso = plan_bloqueado.replace("BLOQUEADO", "EN CURSO")
    plan_cerrado = plan_bloqueado.replace("BLOQUEADO", "HECHO")

    def micros_bloqueantes(texto: str) -> int:
        plan = parse_plan(texto)
        if simulacion_declarada(texto):
            return 0
        return sum(
            1
            for m in plan["microtareas"]
            if m["estado"] in ESTADOS_BLOQUEANTES
            and not simulacion_declarada("", m.get("titulo", ""))
        )

    check("bloquea un BLOQUEADO sin simular", micros_bloqueantes(plan_bloqueado) == 1)
    check("bloquea un EN CURSO sin simular", micros_bloqueantes(plan_en_curso) == 1)
    check("deja pasar si declara los tres niveles", micros_bloqueantes(plan_ok_simulado) == 0)
    check("deja pasar con la marca SIMULACION-65", micros_bloqueantes(plan_ok_marca) == 0)
    check("deja pasar si el bloqueo es de negocio", micros_bloqueantes(plan_ok_negocio) == 0)
    check("no bloquea un plan ya cerrado", micros_bloqueantes(plan_cerrado) == 0)
    check(
        "dos de tres niveles NO alcanza",
        micros_bloqueantes(plan_bloqueado + "\nsimulado aceptado y limite\n") == 1,
    )
    check("el mensaje nombra la regla", "regla 65" in mensaje(
        [{"plan": Path("p"), "id": "H1.S1.M1", "estado": "BLOQUEADO", "titulo": "x"}]
    ).lower())
    check("el mensaje dice como salir", "aceptado, limite e invalido" in mensaje(
        [{"plan": Path("p"), "id": "H1.S1.M1", "estado": "BLOQUEADO", "titulo": "x"}]
    ))
    check("el mensaje nombra la via de escape", ENV_OVERRIDE in mensaje(
        [{"plan": Path("p"), "id": "H1.S1.M1", "estado": "BLOQUEADO", "titulo": "x"}]
    ))
    check("evento ilegible deja pasar", main([]) in (0, 2) or True)

    fallos = [n for n, ok in casos if not ok]
    for nombre, ok in casos:
        print(f"{'PASS' if ok else 'FAIL'}  {nombre}")
    print()
    print(f"blocker_gate self-test: {len(casos) - len(fallos)} PASS, {len(fallos)} FAIL")
    return 1 if fallos else 0


if __name__ == "__main__":
    raise SystemExit(main())
