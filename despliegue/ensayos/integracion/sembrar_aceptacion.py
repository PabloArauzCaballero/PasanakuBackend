#!/usr/bin/env python3
"""
Siembra el "mundo" de la aceptacion con DOS usuarios en modo REAL y lo imprime como JSON (stdout).

    python despliegue/ensayos/integracion/sembrar_aceptacion.py        # lo invoca apps/backoffice/e2e/aceptacion/apoyo/siembra-real.ts

Cada llamada crea personas NUEVAS (telefono y clave aleatorios, hash Argon2id real con la pimienta del entorno) y un
grupo nuevo con un cupo libre: las pruebas no comparten nada. Las claves nacen aca, viajan por stdout al proceso de
Playwright y no se escriben en ningun archivo del repositorio.

Lo que se siembra por SQL es lo que ningun servicio crea hoy: las personas con su rol, y el grupo (grupos expone
crearGrupo, pero exige un organizador habilitado en el servicio `organizador`, un tarifario vigente y la licencia: tres
servicios que este conjunto no levanta). Todo lo demas -invitar, canjear, proponer, resolver- es HTTP real.
"""
from __future__ import annotations

import json
import sys

from lib import sembrar_usuarios
from mundo import grupo_de_admision, persona


def main() -> int:
    organizador = persona("ACO", "ORGANIZADOR", "Organizador")
    participante = persona("ACA", "PARTICIPANTE", "ParticipanteA")
    participante2 = persona("ACB", "PARTICIPANTE", "ParticipanteB")
    operador = persona("ACP", "ADMIN_PLATAFORMA", "Operador")
    operador2 = persona("ACQ", "ADMIN_PLATAFORMA", "OperadorDos")
    sin_permiso = persona("ACS", "AUDITOR_INTERNO", "SinPermiso")
    relleno = persona("ACR", "PARTICIPANTE", "Relleno")  # ocupa el segundo cupo del grupo
    todos = [organizador, participante, participante2, operador, operador2, sin_permiso, relleno]
    sembrar_usuarios(todos)

    g = grupo_de_admision(organizador, relleno, "500.00")

    def cuenta(c) -> dict:
        d = {"id": c.id, "telefono": c.telefono, "clave": c.clave, "rol": c.rol}
        if c.rol in ("ADMIN_PLATAFORMA", "AUDITOR_INTERNO", "TESORERIA", "OFICIAL_CUMPLIMIENTO"):
            d["factor"] = "000000"  # perfil local de identidad: DesafioDeDesarrollo acepta este codigo
        return d

    sys.stdout.write(json.dumps({
        "grupoId": g,
        "organizador": cuenta(organizador),
        "participante": cuenta(participante),
        "participante2": cuenta(participante2),
        "operador": cuenta(operador),
        "operador2": cuenta(operador2),
        "sinPermiso": cuenta(sin_permiso),
    }))
    return 0


if __name__ == "__main__":
    sys.exit(main())
