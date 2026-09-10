#!/usr/bin/env python3
"""
Verifica que la capa web este probada, y que lo que declara se pueda cumplir.

    python3 scripts/verificar_pruebas_web.py            (desde la raiz)

Convierte en mecanica lo que ADR-043 pide en prosa. Comprueba cuatro cosas:

  1. cada servicio con controladores tiene su `SeguridadWebTest`  -> DURO
  2. cada controlador tiene su `<Controlador>WebTest`             -> con lista de deuda
  3. cada @Permiso("X") existe en el catalogo sembrado            -> DURO
  4. cada endpoint declara @Permiso o @Publico                    -> DURO
  5. el estado HTTP que devuelve el codigo lo declara el contrato -> con lista de deuda

La tercera es la que evita el fallo mas caro de esta capa: una anotacion con un
codigo que el catalogo no tiene deja el endpoint inalcanzable para todo el mundo, y
el sintoma es un 403 que nadie sabe explicar. El arranque no lo puede comprobar
—el catalogo esta en la base, no en el classpath— asi que se comprueba aca.

## La lista de deuda

`DEUDA` nombra los controladores que todavia no tienen su matriz propia. **Solo
puede achicarse**: si un controlador de la lista ya tiene su prueba, el gate falla
pidiendo que se lo saque. Sin esa segunda mitad, una lista de deuda es una lista de
excusas que nadie borra.

Un controlador NUEVO no entra a la lista: nace con el gate duro encima.

Ojo con lo que la deuda NO significa: todos los controladores, con o sin deuda,
estan cubiertos por la sabana de seguridad de su servicio (401, 403, y que el error
no filtre nada). Lo que falta en los de la lista es la matriz de su contrato — el
camino feliz, sus validaciones y su JSON.

Devuelve 1 si algo falla, para usarlo como paso del CI.
"""
import json
import pathlib
import re
import sys

import yaml

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

R = pathlib.Path(__file__).resolve().parent.parent
SERVICIOS = R / "servicios"
CATALOGO = R / "seeders/minimos/10-roles-y-permisos.json"

# Controladores sin matriz propia todavia. SOLO PUEDE ACHICARSE.
# Cada linea es trabajo pendiente con nombre y apellido, no una exencion.
DEUDA: dict[str, list[str]] = {
    # Vacia. Los treinta y un controladores tienen su matriz de contrato HTTP.
    #
    # La lista se conserva —y no se borra el mecanismo— porque un controlador nuevo
    # que llegue sin prueba tiene que poder entrar aca de forma EXPLICITA, con nombre
    # y apellido, en vez de que alguien afloje el gate. Solo puede achicarse: si un
    # controlador de la lista ya tiene su prueba, el gate falla pidiendo que se lo saque.
}

RE_PERMISO = re.compile(r'@Permiso\("([A-Z_]+)"\)')
RE_MAPEO = re.compile(r"@(Get|Post|Put|Patch|Delete|Request)Mapping")
RE_OPERACION = re.compile(r"public\s+ResponseEntity<[^>]*>\s+(\w+)\s*\(")

# Al principio de la linea, y no en cualquier lado: el javadoc de `AdhesionController`
# menciona `{@code @RestController}` justo para explicar que NO lo es, y un `in` sobre
# el texto lo contaba como controlador.
RE_RESTCONTROLLER = re.compile(r"^\s*@RestController\b", re.M)

ESTADO = {"OK": "200", "CREATED": "201", "ACCEPTED": "202", "NO_CONTENT": "204"}

# Operaciones cuyo estado HTTP no coincide con el que declara su OpenAPI.
# SOLO PUEDE ACHICARSE.
#
# Casi todas son el mismo patron: el codigo devuelve 201 cuando la operacion produjo el
# efecto y 200 cuando un reintento devolvio el que ya existia —que es lo que hace que
# reintentar sea seguro sin adivinar— y el contrato declara UNO de los dos. El cliente
# generado solo conoce el declarado.
#
# Resolverlo es una decision de contrato y no de codigo: o el OpenAPI declara los dos
# estados, o el controlador deja de distinguirlos. Lo segundo perderia informacion que
# hoy el cliente usa. Hasta que se decida, la lista impide que crezca.
DIVERGENCIA_DE_ESTADO = {
    "aportes": ["cobrarAporte", "solicitarReembolso", "registrarDisputa"],
    "entregas": ["registrarCuentaDestino", "emitirOrdenDesembolso", "anotarRespuestaDesembolso"],
    "garantia": [
        "devolverFondo",
        "iniciarDisolucion",
        "declararIncumplimiento",
        "presentarDescargo",
        "cubrirIncumplimiento",
        "ejecutarAval",
        "restringirDeudor",
        "proponerReemplazo",
    ],
    "organizador": [
        "programarTarea",
        "anotarEjecucion",
        "postularOrganizador",
        "emitirContrato",
        "evaluarDesempeno",
        "apelarSancion",
    ],
    "tarifas": ["cotizarComision", "devengarComision", "devolverComision", "cerrarLiquidacion"],
    "transparencia": ["registrarEventoReputacion"],
}


def estados_declarados(servicio):
    """operationId -> los 2xx que declara el contrato."""
    yamls = list((SERVICIOS / servicio).glob("src/main/resources/openapi/*.yaml"))
    if not yamls:
        return {}
    spec = yaml.safe_load(yamls[0].read_text(encoding="utf-8"))
    declarados = {}
    for verbos in spec.get("paths", {}).values():
        for verbo, op in verbos.items():
            if verbo in ("get", "post", "put", "patch", "delete"):
                declarados[op.get("operationId")] = {
                    c for c in op.get("responses", {}) if str(c).startswith("2")
                }
    return declarados


def estados_devueltos(texto, nombre):
    """Los 2xx que el metodo `nombre` puede devolver, leidos del codigo."""
    m = RE_OPERACION.search(texto, 0)
    while m and m.group(1) != nombre:
        m = RE_OPERACION.search(texto, m.end())
    if not m:
        return set()
    fin = texto.find("\n    }", m.end())
    cuerpo = texto[m.end(): fin if fin > 0 else len(texto)]
    usados = {ESTADO[h] for h in re.findall(r"HttpStatus\.(\w+)", cuerpo) if h in ESTADO}
    if "ResponseEntity.ok(" in cuerpo:
        usados.add("200")
    if "ResponseEntity.noContent()" in cuerpo:
        usados.add("204")
    if "ResponseEntity.created(" in cuerpo:
        usados.add("201")
    return usados


def codigos_del_catalogo():
    """Los codigos que un @Permiso puede nombrar: los de `rol` y los de `permiso`."""
    datos = json.loads(CATALOGO.read_text(encoding="utf-8"))
    codigos = set()
    for bloque in datos["bloques"]:
        if bloque.get("tabla") in {"rol", "permiso"}:
            codigos.update(f["codigo"] for f in bloque.get("filas", []))
    return codigos


def controladores():
    """servicio -> [(nombre, ruta)], ordenado."""
    hallados = {}
    for carpeta in sorted(SERVICIOS.iterdir()):
        if not (carpeta / "build.gradle.kts").is_file():
            continue
        # Solo los que EXPONEN rutas. `AdhesionController` de cumplimiento es un
        # delegado de paquete sin `@RestController`: no tiene rutas propias, y exigirle
        # una prueba de contrato HTTP seria pedir el contrato de algo que no lo tiene.
        ctrls = [
            c
            for c in sorted(carpeta.glob("src/main/java/**/*Controller.java"))
            if RE_RESTCONTROLLER.search(c.read_text(encoding="utf-8"))
        ]
        if ctrls:
            hallados[carpeta.name] = [(c.stem, c) for c in ctrls]
    return hallados


def tiene_prueba(servicio, nombre):
    return any((SERVICIOS / servicio).glob(f"src/test/java/**/{nombre}WebTest.java"))


def main():
    fallas = []
    catalogo = codigos_del_catalogo()
    todos = controladores()
    con_matriz = 0
    pendientes = 0

    for servicio, ctrls in todos.items():
        # 1 · la sabana del servicio entero
        if not any((SERVICIOS / servicio).glob("src/test/java/**/SeguridadWebTest.java")):
            fallas.append(f"{servicio}: sin SeguridadWebTest; sus rutas no se barren")

        deuda = set(DEUDA.get(servicio, []))
        declarados_del_servicio = estados_declarados(servicio)
        for nombre, ruta in ctrls:
            texto = ruta.read_text(encoding="utf-8")

            # 2 · la matriz propia, o la deuda declarada
            if tiene_prueba(servicio, nombre):
                con_matriz += 1
                if nombre in deuda:
                    fallas.append(
                        f"{servicio}/{nombre}: ya tiene su {nombre}WebTest — sacalo de DEUDA "
                        f"en scripts/verificar_pruebas_web.py"
                    )
            elif nombre in deuda:
                pendientes += 1
            else:
                fallas.append(
                    f"{servicio}/{nombre}: falta {nombre}WebTest (matriz de su contrato HTTP)"
                )

            # 3 · el permiso que declara tiene que existir en el catalogo
            for codigo in sorted(set(RE_PERMISO.findall(texto))):
                if codigo not in catalogo:
                    fallas.append(
                        f"{servicio}/{nombre}: @Permiso(\"{codigo}\") no esta en el catalogo "
                        f"(seeders/minimos/10-roles-y-permisos.json): el endpoint queda "
                        f"inalcanzable para todos"
                    )

            # 5 · el estado que devuelve el codigo lo declara el contrato
            deuda_estado = set(DIVERGENCIA_DE_ESTADO.get(servicio, []))
            for m in RE_OPERACION.finditer(texto):
                operacion = m.group(1)
                declarados = declarados_del_servicio.get(operacion)
                devueltos = estados_devueltos(texto, operacion)
                if not declarados or not devueltos:
                    continue
                sobran = sorted(devueltos - declarados)
                if sobran and operacion not in deuda_estado:
                    fallas.append(
                        f"{servicio}/{nombre}.{operacion}: devuelve {sobran} y el contrato "
                        f"declara {sorted(declarados)}; el estado es parte del contrato"
                    )
                elif not sobran and operacion in deuda_estado:
                    fallas.append(
                        f"{servicio}/{nombre}.{operacion}: ya coincide con su contrato — sacalo de "
                        f"DIVERGENCIA_DE_ESTADO en scripts/verificar_pruebas_web.py"
                    )

            # 4 · ningun endpoint sin decision de acceso
            mapeos = len(RE_MAPEO.findall(texto))
            decisiones = len(RE_PERMISO.findall(texto)) + texto.count("@Publico(")
            if mapeos and decisiones < mapeos and "@Permiso" not in texto.split("class")[0]:
                fallas.append(
                    f"{servicio}/{nombre}: {mapeos} endpoints y {decisiones} decisiones de "
                    f"acceso; alguno no declara @Permiso ni @Publico"
                )

    # la deuda no puede nombrar algo que no existe
    for servicio, nombres in DEUDA.items():
        existentes = {n for n, _ in todos.get(servicio, [])}
        for n in nombres:
            if n not in existentes:
                fallas.append(f"DEUDA nombra {servicio}/{n}, que ya no existe: sacalo de la lista")

    total = sum(len(c) for c in todos.values())
    divergencias = sum(len(v) for v in DIVERGENCIA_DE_ESTADO.values())
    print(f"controladores: {total} · con matriz propia: {con_matriz} · en deuda: {pendientes}")
    print(f"operaciones con el estado HTTP fuera de su contrato: {divergencias}")
    print(f"servicios con sabana de seguridad: {len(todos)}")
    if fallas:
        print(f"\n{len(fallas)} FALLAS:")
        for f in fallas:
            print(f"  - {f}")
        return 1
    print("La capa web esta cubierta segun ADR-043.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
