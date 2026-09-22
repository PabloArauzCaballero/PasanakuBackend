#!/usr/bin/env python3
"""
El inventario de endpoints que el CI vigila (H2.S1 del carril PR4-seguridad).

    python3 scripts/inventario_endpoints.py --self-test   (pruebas propias, sin tocar el repo)
    python3 scripts/inventario_endpoints.py --check        (falla si algo esta desalineado)
    python3 scripts/inventario_endpoints.py                (genera endpoints.md y sale 0/1 igual)

Cruza tres fuentes por servicio:

  1. El CONTRATO   `openapi/*.yaml`             -> Method, Path, operationId
  2. La IMPLEMENTACION `**/*Controller.java`    -> que operationId tiene su metodo,
                                                   y que rutas Spring literales existen
                                                   por FUERA de cualquier interfaz generada
  3. Las PRUEBAS    `**/*WebTest.java`          -> si el controlador tiene su matriz

**Kill-test del encargo**: un endpoint agregado a mano con `@GetMapping`/`@PostMapping`
directo en un `@RestController` (no heredado de una interfaz `*Api` generada desde el
contrato) es exactamente lo que ADR-020 prohibe — el codigo se aparto del contrato en
silencio. Este script lo encuentra sin necesitar que el proceso arranque: barre las
anotaciones de mapeo literales del archivo fuente.

Devuelve 1 si:
  - un `operationId` del contrato no tiene metodo que lo implemente,
  - un controlador declara una ruta Spring LITERAL que ningun contrato del servicio
    declara (endpoint sin contrato — el caso que el kill-test describe),
  - un controlador con al menos una operacion no tiene su `SeguridadWebTest`.

Genera `docs/auditoria-produccion/endpoints.md` con una fila por endpoint del
contrato: Service, Method, Path, Permission, Ownership, Idempotency, Rate limit, MFA,
Audit, Tests, Sensible (columna definida por las palabras clave de seguridad-datos
del metaprompt §90.5/§26: dinero, MFA, credenciales, datos personales).
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

import yaml

RAIZ = Path(__file__).resolve().parent.parent
SERVICIOS = RAIZ / "servicios"
DOC = RAIZ / "docs" / "auditoria-produccion" / "endpoints.md"

VERBOS = ("get", "post", "put", "patch", "delete")
RE_MAPEO_LITERAL = re.compile(
    r'@(?:Get|Post|Put|Patch|Delete|Request)Mapping\(\s*(?:value\s*=\s*)?"([^"]*)"'
)
# `ResponseEntity<...>` con hasta UN nivel de genericos anidados adentro
# (`ResponseEntity<List<X>>`, `ResponseEntity<Map<String, Object>>`): un `[^>]*`
# simple corta en el PRIMER `>` que encuentra —el de adentro— y deja de emparejar
# `ResponseEntity<List<X>>` enteros. Sin esto, todo operationId cuyo controlador
# devuelva una coleccion salia como "sin implementar" — un falso positivo real,
# encontrado corriendo esto contra el repo (cumplimiento, identidad, transparencia).
_GENERICO = r"<(?:[^<>]|<[^<>]*>)*>"
RE_OPERACION = re.compile(rf"public\s+ResponseEntity{_GENERICO}\s+(\w+)\s*\(")
RE_PERMISO_METODO = re.compile(rf'@Permiso\("([A-Z_]+)"\)\s*\n\s*public\s+ResponseEntity{_GENERICO}\s+(\w+)\s*\(')
RE_PUBLICO_METODO = re.compile(rf'@Publico\("[^"]*"\)\s*\n\s*public\s+ResponseEntity{_GENERICO}\s+(\w+)\s*\(')
RE_RESTCONTROLLER = re.compile(r"^\s*@RestController\b", re.M)

# Rutas que no son del producto: infraestructura de Spring/observabilidad. La misma
# lista que `Endpoint.esDeInfraestructura` en comun-web/testFixtures, para no divergir.
INFRAESTRUCTURA = ("/actuator", "/.well-known", "/error")

PALABRAS_SENSIBLES = (
    "retiro", "transferencia", "aprobacion", "rechazo", "mfa", "password", "clave",
    "credencial", "webhook", "pago", "cobro", "saldo", "cuenta", "documento", "kyc",
    "biometria", "reversa", "aval", "garantia", "sancion", "permiso", "rol",
)


@dataclass
class Endpoint:
    servicio: str
    metodo: str
    ruta: str
    operation_id: str
    permiso: str | None
    idempotencia: bool
    cu: str | None = None


@dataclass
class Hallazgo:
    mensaje: str


def _yaml_del_servicio(servicio: Path) -> dict | None:
    candidatos = sorted(servicio.glob("src/main/resources/openapi/*.yaml"))
    if not candidatos:
        return None
    return yaml.safe_load(candidatos[0].read_text(encoding="utf-8"))


def _endpoints_declarados(servicio: Path) -> list[Endpoint]:
    spec = _yaml_del_servicio(servicio)
    if not spec:
        return []
    salida: list[Endpoint] = []
    for ruta, verbos in (spec.get("paths") or {}).items():
        for verbo, op in verbos.items():
            if verbo not in VERBOS or not isinstance(op, dict):
                continue
            parametros = op.get("parameters") or []
            exige_clave = any(
                (p.get("$ref", "").endswith("/ClaveIdempotencia"))
                or (str(p.get("name", "")).lower() == "idempotency-key")
                for p in parametros
                if isinstance(p, dict)
            )
            cu = None
            m = re.search(r"CU-(\d+)", (op.get("description") or "") + (op.get("summary") or ""))
            if m:
                cu = f"CU-{m.group(1)}"
            salida.append(
                Endpoint(
                    servicio=servicio.name,
                    metodo=verbo.upper(),
                    ruta=ruta,
                    operation_id=op.get("operationId") or "",
                    permiso=None,
                    idempotencia=exige_clave,
                    cu=cu,
                )
            )
    return salida


def _controladores(servicio: Path) -> list[Path]:
    return [
        c
        for c in sorted(servicio.glob("src/main/java/**/*Controller.java"))
        if RE_RESTCONTROLLER.search(c.read_text(encoding="utf-8"))
    ]


def _metodo_de_operacion(textos: list[str], operation_id: str) -> str | None:
    """El permiso del metodo que implementa `operation_id`, o "PUBLICO", o None si no esta."""
    for texto in textos:
        for m in RE_PERMISO_METODO.finditer(texto):
            if m.group(2) == operation_id:
                return m.group(1)
        for m in RE_PUBLICO_METODO.finditer(texto):
            if m.group(1) == operation_id:
                return "PUBLICO"
        if re.search(rf"public\s+ResponseEntity{_GENERICO}\s+{re.escape(operation_id)}\s*\(", texto):
            return "SIN_ANOTACION"
    return None


def _rutas_literales_sin_contrato(servicio: Path, rutas_del_contrato: set[str]) -> list[str]:
    """Rutas @XxxMapping escritas a mano en el codigo, fuera de INFRAESTRUCTURA."""
    encontradas = []
    for controlador in _controladores(servicio):
        texto = controlador.read_text(encoding="utf-8")
        for m in RE_MAPEO_LITERAL.finditer(texto):
            ruta = m.group(1)
            if not ruta:
                continue
            if any(ruta.startswith(p) for p in INFRAESTRUCTURA):
                continue
            if ruta not in rutas_del_contrato:
                encontradas.append(f"{servicio}: {controlador.name} declara \"{ruta}\" a mano, sin contrato OpenAPI")
    return encontradas


def _tiene_seguridad_web_test(servicio: Path) -> bool:
    return any(servicio.glob("src/test/java/**/SeguridadWebTest.java"))


def _es_sensible(endpoint: Endpoint) -> bool:
    texto = f"{endpoint.operation_id} {endpoint.ruta}".lower()
    return any(palabra in texto for palabra in PALABRAS_SENSIBLES)


def relevar() -> tuple[list[Endpoint], list[Hallazgo]]:
    endpoints: list[Endpoint] = []
    hallazgos: list[Hallazgo] = []

    for servicio in sorted(p for p in SERVICIOS.iterdir() if (p / "build.gradle.kts").is_file()):
        declarados = _endpoints_declarados(servicio)
        if not declarados:
            continue
        controladores = _controladores(servicio)
        textos = [c.read_text(encoding="utf-8") for c in controladores]

        rutas_del_contrato = {e.ruta for e in declarados}
        for msg in _rutas_literales_sin_contrato(servicio, rutas_del_contrato):
            hallazgos.append(Hallazgo(msg))

        if controladores and not _tiene_seguridad_web_test(servicio):
            hallazgos.append(Hallazgo(f"{servicio}: tiene controladores pero no `SeguridadWebTest`"))

        for e in declarados:
            permiso = _metodo_de_operacion(textos, e.operation_id)
            if permiso is None:
                hallazgos.append(
                    Hallazgo(
                        f"{servicio}: el contrato declara \"{e.operation_id}\" ({e.metodo} {e.ruta}) "
                        f"y ningun controlador lo implementa"
                    )
                )
            e.permiso = permiso or "SIN_IMPLEMENTAR"
            endpoints.append(e)

    return endpoints, hallazgos


def _tabla(endpoints: list[Endpoint]) -> str:
    filas = [
        "| Service | Method | Path | Permission | Ownership | Idempotency | Rate limit | MFA | Audit | Tests | Sensible |",
        "|---|---|---|---|---|---|---|---|---|---|---|",
    ]
    for e in sorted(endpoints, key=lambda x: (x.servicio, x.ruta, x.metodo)):
        ownership = "revisar (id en la ruta)" if re.search(r"\{\w+Id\}", e.ruta) else "n/a"
        rate_limit = "borde (gateway, H7.S4 — Pablo)" if _es_sensible(e) else "n/a"
        mfa = "si (retiro)" if "retiro" in e.operation_id.lower() else "no aplica"
        audit = "critica (ver security-matrix.md)" if _es_sensible(e) else "no critica"
        prueba = "ver *WebTest del servicio"
        filas.append(
            "| {servicio} | {metodo} | `{ruta}` | {permiso} | {ownership} | {idem} | {rate} | {mfa} | {audit} | {tests} | {sensible} |".format(
                servicio=e.servicio,
                metodo=e.metodo,
                ruta=e.ruta,
                permiso=e.permiso,
                ownership=ownership,
                idem="si" if e.idempotencia else "no",
                rate=rate_limit,
                mfa=mfa,
                audit=audit,
                tests=prueba,
                sensible="si" if _es_sensible(e) else "no",
            )
        )
    return "\n".join(filas)


def generar_documento(endpoints: list[Endpoint]) -> str:
    encabezado = (
        "# Inventario de endpoints — generado por `scripts/inventario_endpoints.py`\n\n"
        "> No editar a mano: se regenera con `python3 scripts/inventario_endpoints.py`. "
        "El CI falla (`--check`) si un endpoint implementado no esta en su OpenAPI, o "
        "viceversa (H2.S1 del carril PR4-seguridad).\n\n"
        f"**Total de endpoints relevados:** {len(endpoints)}.\n\n"
    )
    return encabezado + _tabla(endpoints) + "\n"


def main(argv: list[str] | None = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="solo verifica, no escribe el documento")
    parser.add_argument("--self-test", action="store_true", help="pruebas propias del script, sin tocar el repo")
    args = parser.parse_args(argv)

    if args.self_test:
        return _self_test()

    endpoints, hallazgos = relevar()

    if not args.check:
        DOC.parent.mkdir(parents=True, exist_ok=True)
        DOC.write_text(generar_documento(endpoints), encoding="utf-8", newline="\n")
        print(f"escrito: {DOC.relative_to(RAIZ)} ({len(endpoints)} endpoints)")

    print(f"endpoints relevados: {len(endpoints)} · hallazgos: {len(hallazgos)}")
    if hallazgos:
        print(f"\n{len(hallazgos)} FALLAS:")
        for h in hallazgos:
            print(f"  - {h.mensaje}")
        return 1
    print("El inventario esta alineado: todo lo implementado tiene contrato y viceversa.")
    return 0


def _self_test() -> int:
    """Pruebas contra fixtures en memoria (via tempfile), no contra el repo real."""
    import tempfile

    fallas = []

    # 1) Un contrato con un operationId sin metodo Java que lo implemente -> hallazgo.
    with tempfile.TemporaryDirectory() as tmp:
        servicio = Path(tmp) / "demo"
        (servicio / "src/main/resources/openapi").mkdir(parents=True)
        (servicio / "src/main/java/demo").mkdir(parents=True)
        (servicio / "src/test/java/demo").mkdir(parents=True)
        (servicio / "build.gradle.kts").write_text("// demo", encoding="utf-8")
        (servicio / "src/main/resources/openapi/demo.yaml").write_text(
            "paths:\n"
            "  /demo/algo:\n"
            "    get:\n"
            "      operationId: leerAlgo\n"
            "      responses: { '200': { description: ok } }\n",
            encoding="utf-8",
        )
        (servicio / "src/main/java/demo/DemoController.java").write_text(
            "@RestController\npublic class DemoController implements DemoApi {\n"
            "  @Permiso(\"X\")\n  public ResponseEntity<Object> otraCosa() { return null; }\n}\n",
            encoding="utf-8",
        )
        (servicio / "src/test/java/demo/SeguridadWebTest.java").write_text("class SeguridadWebTest {}", encoding="utf-8")

        global SERVICIOS
        original = SERVICIOS
        SERVICIOS = Path(tmp)
        try:
            _, hallazgos = relevar()
        finally:
            SERVICIOS = original
        if not any("leerAlgo" in h.mensaje for h in hallazgos):
            fallas.append("self-test 1: no detecto operationId sin implementar")

    # 2) Un endpoint agregado a mano (@GetMapping directo) sin contrato -> hallazgo. Es
    #    EXACTAMENTE el kill-test del encargo: "endpoint agregado a mano sin contrato".
    with tempfile.TemporaryDirectory() as tmp:
        servicio = Path(tmp) / "demo2"
        (servicio / "src/main/resources/openapi").mkdir(parents=True)
        (servicio / "src/main/java/demo2").mkdir(parents=True)
        (servicio / "src/test/java/demo2").mkdir(parents=True)
        (servicio / "build.gradle.kts").write_text("// demo", encoding="utf-8")
        (servicio / "src/main/resources/openapi/demo2.yaml").write_text(
            "paths:\n"
            "  /demo2/conocido:\n"
            "    get:\n"
            "      operationId: leerConocido\n"
            "      responses: { '200': { description: ok } }\n",
            encoding="utf-8",
        )
        (servicio / "src/main/java/demo2/Demo2Controller.java").write_text(
            "@RestController\npublic class Demo2Controller implements Demo2Api {\n"
            "  @Permiso(\"X\")\n  public ResponseEntity<Object> leerConocido() { return null; }\n\n"
            "  @GetMapping(\"/demo2/agregado-a-mano\")\n"
            "  public ResponseEntity<Object> agregadoAMano() { return null; }\n}\n",
            encoding="utf-8",
        )
        (servicio / "src/test/java/demo2/SeguridadWebTest.java").write_text("class SeguridadWebTest {}", encoding="utf-8")

        SERVICIOS = Path(tmp)
        try:
            _, hallazgos = relevar()
        finally:
            SERVICIOS = original
        if not any("agregado-a-mano" in h.mensaje for h in hallazgos):
            fallas.append("self-test 2 (KILL-TEST): no detecto el endpoint agregado a mano sin contrato")

    if fallas:
        print(f"{len(fallas)} FALLAS en el self-test:")
        for f in fallas:
            print(f"  - {f}")
        return 1
    print("self-test: 2 PASS (operationId sin implementar; endpoint agregado a mano sin contrato)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
