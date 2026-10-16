#!/usr/bin/env python3
"""Evalua las reglas de alerta de despliegue/alertas/reglas.yml contra una PostgreSQL.

    python despliegue/alertas/evaluar_alarmas.py --psql "psql -h 127.0.0.1 -p 5439 -U pasanaku -d pasanaku" \
        --registro alarmas.jsonl
    python despliegue/alertas/evaluar_alarmas.py --psql "docker exec -i aportaya-pg-s2 psql -U pasanaku -d pasanaku"

Que hace, por regla:
  1. corre el detector (SQL de solo lectura) con los parametros de reglas.yml;
  2. si cuenta > 0, la alarma esta DISPARADA: junta la evidencia (identificadores acortados, nunca
     datos de personas), resuelve al responsable por ROL en el catalogo sembrado
     (identidad.asignacion_rol), escala al rol de escalamiento si nadie tiene el primero, y arma
     el cuerpo de la notificacion tal como lo espera POST /notificaciones (EntradaNotificacion);
  3. deja constancia en el registro (JSON por linea): alarma_disparada al abrirse y
     alarma_resuelta cuando el detector vuelve a cero, con la duracion. Una alarma que sigue
     abierta no se repite en el registro: se informa como vigente.

Codigos de salida: 0 nada disparado - 3 hay alarmas - 4 hay alarmas SIN destinatario (nadie a
quien avisar es un fallo de la propia alarma) - 2 error de ejecucion.

No envia nada: la entrega es de `notificaciones` (CU-80). Los umbrales de reglas.yml son
sinteticos (apto_produccion: false) y el script lo repite en cada salida mientras sea asi.
"""
import argparse
import datetime
import json
import pathlib
import shlex
import subprocess
import sys
import uuid

import yaml

RAIZ = pathlib.Path(__file__).resolve().parent.parent.parent
REGLAS = pathlib.Path(__file__).resolve().parent / "reglas.yml"

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")


def ahora():
    return datetime.datetime.now(datetime.timezone.utc)


class ErrorDeEjecucion(Exception):
    pass


def consultar(psql, sql, variables):
    """Corre SQL por stdin (psql no interpola variables en -c) y devuelve las filas como listas."""
    orden = shlex.split(psql) + ["-v", "ON_ERROR_STOP=1", "-q", "-tA", "-F", "|"]
    for clave, valor in variables.items():
        orden += ["-v", f"{clave}={valor}"]
    resultado = subprocess.run(orden, input=sql, capture_output=True, text=True, encoding="utf-8")
    if resultado.returncode != 0:
        raise ErrorDeEjecucion(resultado.stderr.strip() or "psql fallo")
    return [linea.split("|") for linea in resultado.stdout.splitlines() if linea.strip()]


def sql_del_outbox(config, solo_conteo):
    partes = []
    for esquema in config["outbox"]["esquemas"]:
        condicion = " ".join(config["outbox"]["condicion"].split())
        if solo_conteo:
            partes.append(f"SELECT count(*) AS n FROM {esquema}.evento_dominio WHERE {condicion}")
        else:
            partes.append(
                f"SELECT '{esquema}' AS esquema, count(*) AS detenidos, min(ocurrido_en) AS el_mas_viejo "
                f"FROM {esquema}.evento_dominio WHERE {condicion}"
            )
    union = " UNION ALL ".join(partes)
    if solo_conteo:
        return f"SELECT coalesce(sum(n), 0) FROM ({union}) t"
    return f"SELECT * FROM ({union}) t WHERE detenidos > 0 ORDER BY detenidos DESC LIMIT 5"


def sql_de(regla, clave, config):
    texto = regla[clave]
    if texto == "@outbox_detenido":
        return sql_del_outbox(config, True)
    if texto == "@outbox_detenido_por_esquema":
        return sql_del_outbox(config, False)
    return texto


def destinatarios(psql, rol):
    filas = consultar(
        psql,
        "SELECT ar.usuario_id FROM identidad.asignacion_rol ar JOIN identidad.rol r ON r.id = ar.rol_id "
        "WHERE r.codigo = :'rol' AND ar.revocada_en IS NULL "
        "AND (ar.vigente_hasta IS NULL OR ar.vigente_hasta > now()) ORDER BY ar.otorgada_en",
        {"rol": rol},
    )
    return [f[0] for f in filas]


def leer_registro(ruta):
    if not ruta or not pathlib.Path(ruta).exists():
        return {}
    ultimo = {}
    for linea in pathlib.Path(ruta).read_text(encoding="utf-8").splitlines():
        if linea.strip():
            evento = json.loads(linea)
            ultimo[evento["regla"]] = evento
    return ultimo


def anotar(ruta, evento):
    if ruta:
        with open(ruta, "a", encoding="utf-8") as f:
            f.write(json.dumps(evento, ensure_ascii=False) + "\n")


def evaluar(psql, ruta_registro):
    config = yaml.safe_load(REGLAS.read_text(encoding="utf-8"))
    parametros = {k: str(v) for k, v in config["parametros"].items()}
    previo = leer_registro(ruta_registro)
    disparadas = []
    sin_destinatario = 0
    for regla in config["reglas"]:
        conteo = int(float(consultar(psql, sql_de(regla, "detector", config), parametros)[0][0]))
        abierta = previo.get(regla["id"], {}).get("tipo") == "alarma_disparada"
        if conteo == 0:
            if abierta:
                inicio = datetime.datetime.fromisoformat(previo[regla["id"]]["detectada_en"])
                anotar(ruta_registro, {
                    "tipo": "alarma_resuelta", "regla": regla["id"], "resuelta_en": ahora().isoformat(),
                    "duracion_s": round((ahora() - inicio).total_seconds(), 1),
                })
                print(f"[{regla['id']}] {regla['nombre']}: RESUELTA tras {round((ahora() - inicio).total_seconds(), 1)} s")
            else:
                print(f"[{regla['id']}] {regla['nombre']}: en orden")
            continue

        evidencia = consultar(psql, sql_de(regla, "evidencia", config), parametros)
        nivel, quienes = "responsable", destinatarios(psql, regla["responsable_rol"])
        if not quienes:
            nivel, quienes = "escalamiento", destinatarios(psql, regla["escalamiento_rol"])
        if not quienes:
            nivel = "ninguno"
            sin_destinatario += 1
        alerta = {
            "tipo": "alarma_disparada", "regla": regla["id"], "nombre": regla["nombre"],
            "severidad": regla["severidad"], "detectada_en": ahora().isoformat(), "conteo": conteo,
            "evidencia": evidencia, "avisado_a_rol": regla["responsable_rol"] if nivel == "responsable"
            else regla["escalamiento_rol"] if nivel == "escalamiento" else None,
            "nivel": nivel, "destinatarios": quienes, "runbook": regla["runbook"],
            "apto_produccion": config["apto_produccion"],
            "notificaciones": [
                {"destinatarioId": q, "evento": regla["evento_notificable"],
                 "datos": {"regla": regla["id"], "severidad": regla["severidad"], "conteo": str(conteo),
                           "procedimiento": regla["runbook"]}}
                for q in quienes
            ],
        }
        disparadas.append(alerta)
        if abierta:
            print(f"[{regla['id']}] {regla['nombre']}: VIGENTE ({conteo}) desde {previo[regla['id']]['detectada_en']}")
        else:
            anotar(ruta_registro, alerta | {"alerta_id": str(uuid.uuid4())})
            print(f"[{regla['id']}] {regla['nombre']}: DISPARADA ({conteo}) -> {nivel}"
                  f" ({len(quienes)} destinatario/s) · procedimiento {regla['runbook']}")
    return config, disparadas, sin_destinatario


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--psql", required=True, help="comando psql completo, p. ej. 'docker exec -i CT psql -U u -d d'")
    ap.add_argument("--registro", help="archivo JSON-por-linea donde queda constancia de cada alarma")
    ap.add_argument("--json", action="store_true", help="ademas, imprime las alarmas disparadas como JSON")
    args = ap.parse_args()
    try:
        config, disparadas, sin_destinatario = evaluar(args.psql, args.registro)
    except ErrorDeEjecucion as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 2
    if not config["apto_produccion"]:
        print("AVISO: umbrales sinteticos (apto_produccion=false); fijarlos es decision de operaciones y tesoreria.")
    if args.json:
        print(json.dumps(disparadas, ensure_ascii=False, indent=2))
    if sin_destinatario:
        return 4
    return 3 if disparadas else 0


if __name__ == "__main__":
    sys.exit(main())
