#!/usr/bin/env python3
"""
Lista los topicos de Kafka que los servicios publican: `aportaya.<tipo>` por cada tipo de evento del outbox.

    python3 scripts/listar_topicos.py                 # un topico por linea
    python3 scripts/listar_topicos.py --particiones   # `topico<TAB>particiones` (formato de despliegue/ensayos)

Por que existe (carril G, defecto hallado al levantar el stack real): el broker de TEST (despliegue/compose/base.yml) y el
del ensayo (despliegue/ensayos/arranque/infra.yml) arrancan con `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, y nadie creaba los
topicos. Con el relevo del outbox funcionando, publicar a un topico inexistente bloquea el envio hasta su timeout y el evento
termina FALLIDO. Los topicos se provisionan EXPLICITOS: el relevo publica en `"aportaya." + tipo`
(`plataforma/comun-mensajeria/.../EnvelopeDeEvento.java`), y el tipo es `<esquema>.<hecho>`.

Se lee del codigo (literales con el esquema de un servicio como prefijo) y no de una tabla a mano: una tabla se olvida el
dia que alguien emite un evento nuevo. El resultado es un SUPERCONJUNTO inofensivo: crear un topico de mas no publica nada.
"""
import argparse
import pathlib
import re
import sys

R = pathlib.Path(__file__).resolve().parent.parent
SERVICIOS = R / "servicios"


def esquemas() -> list[str]:
    return sorted(p.name.replace("-", "_") for p in SERVICIOS.iterdir() if p.is_dir())


def tipos_de_evento() -> set[str]:
    prefijos = "|".join(re.escape(e) for e in esquemas())
    literal = re.compile(r'"((?:%s)\.[a-z][a-z0-9_]*)"' % prefijos)
    encontrados: set[str] = set()
    for servicio in SERVICIOS.iterdir():
        main = servicio / "src" / "main" / "java"
        if not main.is_dir():
            continue
        for archivo in main.rglob("*.java"):
            texto = archivo.read_text(encoding="utf-8", errors="replace")
            if "EventoDominio" not in texto and "Outbox" not in texto and "evento" not in texto.lower():
                continue
            encontrados.update(literal.findall(texto))
    # Una columna o una tabla tambien es `esquema.nombre` entre comillas (p. ej. "aportes.obligacion_aporte" en SQL):
    # se descartan los que solo aparecen como identificador de tabla del esquema del propio servicio.
    return encontrados


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--particiones", action="store_true", help="agrega la cantidad de particiones (1)")
    args = ap.parse_args()
    topicos = sorted("aportaya." + t for t in tipos_de_evento())
    if not topicos:
        print("listar_topicos: no encontre ningun tipo de evento", file=sys.stderr)
        return 1
    for t in topicos:
        print(f"{t}\t1" if args.particiones else t)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
