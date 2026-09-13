#!/usr/bin/env python3
"""Pone una contrasena de desarrollo a los usuarios de prueba, y confia en un dispositivo.

**Por que existe.** Las cuentas de `sql/61_dev/` se siembran con hashes, nunca con la
contrasena en claro: el repositorio no guarda credenciales, y el barrido de seguridad
lo comprueba. El efecto secundario era que nadie podia iniciar sesion contra el
backend de verdad — habia usuarios de prueba que no servian para probar. Esto cierra
el hueco sin escribir un secreto en ningun archivo: la contrasena la elegis vos, sale
de una variable de entorno, y a la base entra ya hasheada.

    CLAVE_DEV='...' python3 scripts/clave_dev.py
    CLAVE_DEV='...' python3 scripts/clave_dev.py --huella <32 hex>

El hash es Argon2id con los parametros de `Argon2Hasheador` (m=65536, t=3, p=2) sobre
`contrasena + pimienta`, que es lo que el servicio verifica. La pimienta local sale de
`despliegue/compose/servicios.yml`.

Con `--huella` ademas marca ese dispositivo como de confianza para las cuentas de
prueba. Sin eso, `ExigeSegundoFactor` pide MFA a todo dispositivo desconocido, y como
las cuentas de prueba no tienen factor enrolado, el ingreso no termina nunca. La huella
la genera la app al instalarse y vive en su almacen seguro.

Solo toca una base marcada `app.entorno = 'dev'`.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys

CONTENEDOR = "aportaya-postgres"
BASE = "pasanaku"
USUARIO_BD = "pasanaku"
PIMIENTA_LOCAL = "pimienta-local-no-es-la-de-produccion"
CUENTAS = [f"USR{n:06d}" for n in list(range(1, 10)) + [90, 91]]


def hashear(clave: str, pimienta: str) -> dict[str, str]:
    """Un hash por cuenta, con sal fija: el resultado es reproducible y su diff, legible."""
    guion = (
        "import json, os\n"
        "from argon2.low_level import hash_secret, Type\n"
        "clave = os.environ['ENTRADA_CLAVE']\n"
        "pimienta = os.environ['ENTRADA_PIMIENTA']\n"
        "cuentas = json.loads(os.environ['ENTRADA_CUENTAS'])\n"
        "salidas = {}\n"
        "for c in cuentas:\n"
        "    sal = ('aportaya-dev-' + c[-3:]).encode()\n"
        "    salidas[c] = hash_secret((clave + pimienta).encode(), sal,\n"
        "                             time_cost=3, memory_cost=65536, parallelism=2,\n"
        "                             hash_len=32, type=Type.ID).decode()\n"
        "print(json.dumps(salidas))\n"
    )
    entorno = {
        "ENTRADA_CLAVE": clave,
        "ENTRADA_PIMIENTA": pimienta,
        "ENTRADA_CUENTAS": json.dumps(CUENTAS),
    }
    try:
        import argon2  # noqa: F401

        proceso = subprocess.run(
            [sys.executable, "-"],
            input=guion,
            env={**os.environ, **entorno},
            capture_output=True,
            text=True,
            check=True,
        )
    except ImportError:
        # Sin argon2-cffi en la maquina se calcula en un contenedor: una herramienta de
        # desarrollo que exige instalar cosas a mano es una herramienta que no se usa.
        # Los datos van por variables de entorno y el guion por la entrada estandar:
        # asi nada pasa por la linea de comandos ni queda en el historial del shell.
        orden = ["docker", "run", "--rm", "-i"]
        for clave_entorno in entorno:
            orden += ["-e", clave_entorno]
        orden += ["python:3.12-slim", "sh", "-c",
                  "pip install -q argon2-cffi >/dev/null 2>&1; exec python -"]
        proceso = subprocess.run(
            orden,
            input=guion,
            env={**os.environ, **entorno},
            capture_output=True,
            text=True,
            check=True,
        )
    return json.loads(proceso.stdout.strip().splitlines()[-1])


def sentencias(hashes: dict[str, str], huella: str | None) -> str:
    partes = [
        "DO $$ BEGIN IF current_setting('app.entorno', true) IS DISTINCT FROM 'dev' THEN "
        "RAISE EXCEPTION 'Esta base no esta marcada como dev'; END IF; END $$;"
    ]
    for cuenta, valor in hashes.items():
        partes.append(
            "INSERT INTO identidad.credencial_acceso "
            "(usuario_id, hash_contrasena, algoritmo, parametros_kdf, requiere_cambio, cambiada_en) "
            f"SELECT u.id, '{valor}', 'argon2id', '{{\"m\":65536,\"t\":3,\"p\":2}}'::jsonb, false, now() "
            f"FROM identidad.usuario u WHERE u.codigo_publico = '{cuenta}' "
            "ON CONFLICT (usuario_id) DO UPDATE SET hash_contrasena = EXCLUDED.hash_contrasena, "
            "algoritmo = EXCLUDED.algoritmo, parametros_kdf = EXCLUDED.parametros_kdf, cambiada_en = now();"
        )
    if huella:
        lista = ",".join(f"'{c}'" for c in CUENTAS)
        partes.append(
            "INSERT INTO identidad.dispositivo (usuario_id, huella, plataforma, modelo, version_app, "
            "es_confiable, autorizado_en, ultimo_uso_en) "
            f"SELECT u.id, '{huella}', 'IOS', 'Dispositivo de desarrollo', '0.1.0', TRUE, now(), now() "
            f"FROM identidad.usuario u WHERE u.codigo_publico IN ({lista}) "
            "ON CONFLICT (usuario_id, huella) DO UPDATE SET es_confiable = TRUE, "
            "autorizado_en = now(), revocado_en = NULL;"
        )
    return "\n".join(partes)


def main() -> int:
    argumentos = argparse.ArgumentParser(description=__doc__)
    argumentos.add_argument("--huella", help="huella del dispositivo a marcar como confiable (32 hex)")
    argumentos.add_argument("--pimienta", default=os.environ.get("SEGURIDAD_PIMIENTA", PIMIENTA_LOCAL))
    opciones = argumentos.parse_args()

    clave = os.environ.get("CLAVE_DEV")
    if not clave or len(clave) < 8:
        print("Falta CLAVE_DEV (8 caracteres o mas). Ejemplo:\n"
              "  CLAVE_DEV='...' python3 scripts/clave_dev.py", file=sys.stderr)
        return 2
    if opciones.huella and not re.fullmatch(r"[0-9a-f]{32}", opciones.huella):
        print("La huella son 32 digitos hexadecimales.", file=sys.stderr)
        return 2

    sql = sentencias(hashear(clave, opciones.pimienta), opciones.huella)
    subprocess.run(
        ["docker", "exec", "-i", "-e", "PGPASSWORD=pasanaku", CONTENEDOR,
         "psql", "-U", USUARIO_BD, "-d", BASE, "-v", "ON_ERROR_STOP=1", "-q"],
        input=sql, text=True, check=True,
    )
    print(f"Credenciales puestas en {len(CUENTAS)} cuentas de prueba.")
    if opciones.huella:
        print(f"Dispositivo {opciones.huella[:8]}… marcado como de confianza.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
