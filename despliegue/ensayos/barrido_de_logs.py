#!/usr/bin/env python3
"""Barrido de LOGS REALES en busca de datos personales y credenciales (H13.S1.M3).

    python despliegue/ensayos/barrido_de_logs.py [--raiz .] [--salida informe.json]

Lee la salida estandar y de error que las pruebas dejaron en `**/build/test-results/**/*.xml`
(<system-out> y <system-err>: lo que los servicios LOGUEARON mientras corrian contra una base real)
y busca patrones de dato personal o credencial. Es el complemento DINAMICO del barrido de fuentes
(`SalidasSensiblesBarridoTest`): aquel mira lo que el codigo podria loguear, este mira lo que
logueo.

Patrones (nunca se imprime el valor encontrado, solo su tipo, el archivo y un hash corto):
  telefono E.164 · correo · JWT · Bearer · token hex de 64 · par clave=valor de secreto ·
  documento de identidad con etiqueta.

Los datos de prueba son sinteticos, asi que un hallazgo aca significa «el log escribe el
CONTENIDO de un dato», no «hay un dato real». Sale con 1 si hay hallazgos no esperados.
"""
import argparse
import hashlib
import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8")

PATRONES = {
    "telefono_e164": re.compile(r"(?<![\w.])\+\d{8,15}(?!\w)"),
    "correo": re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+\.[A-Za-z.]{2,}"),
    "jwt": re.compile(r"eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}"),
    "bearer": re.compile(r"(?i)bearer\s+[A-Za-z0-9._~+/=-]{16,}"),
    "token_hex_64": re.compile(r"(?<![0-9a-fA-F])[0-9a-f]{64}(?![0-9a-fA-F])"),
    "secreto_clave_valor": re.compile(r"(?i)\b(password|passwd|contrasena|secret|secreto|api[_-]?key|clave|token|pimienta)\s*[=:]\s*[^\s,;\"'}{]{6,}"),
    "documento_con_etiqueta": re.compile(r"(?i)\b(documento|numero_documento|nit|ci|cedula)\s*[=:]\s*\d{6,}"),
}

# Falsos positivos conocidos y declarados: hashes de huellas publicas (SHA-256 de contenido, no secreto)
# aparecen en logs de integridad. Se filtran SOLO por contexto explicito, nunca por valor.
CONTEXTOS_INOFENSIVOS = re.compile(
    r"(?i)(hash|huella|sha-?256|checksum|digest|semilla_comprometida"
    r"|testcontainers|ryuk|container \S+ is (starting|started)|creating container)"  # id de contenedor Docker, no secreto
)


def corto(valor):
    return hashlib.sha256(valor.encode()).hexdigest()[:8]


def texto_de(xml):
    try:
        raiz = ET.parse(xml).getroot()
    except ET.ParseError:
        return ""
    return "\n".join((e.text or "") for e in raiz.iter() if e.tag in ("system-out", "system-err"))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--raiz", default=str(pathlib.Path(__file__).resolve().parent.parent.parent))
    ap.add_argument("--salida")
    args = ap.parse_args()
    raiz = pathlib.Path(args.raiz)
    archivos = sorted(p for p in raiz.glob("**/build/test-results/**/*.xml") if "node_modules" not in p.parts)
    lineas_totales, hallazgos = 0, []
    con_salida = 0
    for xml in archivos:
        texto = texto_de(xml)
        if not texto.strip():
            continue
        con_salida += 1
        for n, linea in enumerate(texto.splitlines(), 1):
            lineas_totales += 1
            for tipo, patron in PATRONES.items():
                for m in patron.finditer(linea):
                    if tipo == "token_hex_64" and CONTEXTOS_INOFENSIVOS.search(linea):
                        continue
                    hallazgos.append({"tipo": tipo, "archivo": str(xml.relative_to(raiz)).replace("\\", "/"),
                                      "linea": n, "huella_corta": corto(m.group(0))})
    por_tipo = {}
    for h in hallazgos:
        por_tipo[h["tipo"]] = por_tipo.get(h["tipo"], 0) + 1
    print(f"informes XML revisados: {len(archivos)} · con salida estandar: {con_salida} · lineas de log: {lineas_totales}")
    print(f"hallazgos: {len(hallazgos)} " + json.dumps(por_tipo, ensure_ascii=False))
    for h in hallazgos[:40]:
        print(f"  {h['tipo']:24} {h['archivo']}:{h['linea']}  (huella {h['huella_corta']})")
    if len(hallazgos) > 40:
        print(f"  ... y {len(hallazgos) - 40} mas")
    if args.salida:
        pathlib.Path(args.salida).write_text(json.dumps(
            {"xml": len(archivos), "con_salida": con_salida, "lineas": lineas_totales, "hallazgos": hallazgos},
            ensure_ascii=False, indent=1), encoding="utf-8")
    return 1 if hallazgos else 0


if __name__ == "__main__":
    sys.exit(main())
