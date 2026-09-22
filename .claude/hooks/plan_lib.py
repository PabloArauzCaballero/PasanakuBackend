"""Parseo compartido de PLAN.md / REPORTE.md para los candados de proceso.

Implementa las reglas `.claude/rules/20-plan-obligatorio.md` y `40-reporte-obligatorio.md`.
Sin dependencias externas. Compatible con Windows (pathlib, UTF-8 explicito).

Principio de diseno: este modulo NUNCA lanza por contenido malformado. Ante un PLAN.md
que no se puede interpretar, devuelve `malformado=True` y la lista de razones; decidir
que hacer con eso es responsabilidad de quien llama (las reglas dicen: no bloquear por
un plan mal escrito, solo por un plan ausente).
"""

from __future__ import annotations

import re
import unicodedata
from pathlib import Path

# --- Vocabulario cerrado de la regla 20, seccion 5 -------------------------------

ESTADOS = ("TODO", "EN CURSO", "HECHO", "A MEDIAS", "BLOQUEADO", "DESCARTADO")
#: Estados que significan "hay trabajo vivo en este plan".
ESTADOS_ACTIVOS = ("TODO", "EN CURSO")
#: Estados que cierran una microtarea sin haberla completado.
ESTADOS_INCOMPLETOS = ("EN CURSO", "A MEDIAS", "BLOQUEADO")
ESTADO_DESCONOCIDO = "DESCONOCIDO"

#: Secciones que la regla 40 exige siempre en REPORTE.md.
SECCIONES_REPORTE = ("COMPLETADO", "A MEDIAS", "PENDIENTE")

#: Ubicacion por defecto de los trabajos (regla 20, seccion 1).
GLOB_PLANES = "docs/trabajo/*/PLAN.md"

_MICRO_ID = re.compile(r"H\d+\.S\d+\.M\d+", re.IGNORECASE)
# Fila que PARECE una microtarea (empieza con un ID tipo H<n>...) aunque su ID sea invalido.
# Sirve para avisar en vez de descartarla en silencio.
_CANDIDATA_ID = re.compile(r"^\s*`?\s*H\d+[.\w]*\s*`?\s*$", re.IGNORECASE)
_SUB_ID = re.compile(r"^H\d+\.S\d+$", re.IGNORECASE)
_HITO_ID = re.compile(r"^H\d+$", re.IGNORECASE)
_HEADING = re.compile(r"^\s{0,3}#{1,6}\s+(.*)$")
_ESTADO_LINEA = re.compile(r"\*\*Estado:?\*\*\s*:?\s*(.+)$", re.IGNORECASE)


def normalizar(texto: str) -> str:
    """Mayusculas, sin acentos, sin adornos markdown, espacios colapsados.

    Permite comparar 'a medias', '`A MEDIAS`' y '**A Medias**' como el mismo estado.
    """
    if texto is None:
        return ""
    limpio = texto.replace("`", " ").replace("*", " ").replace("_", " ")
    limpio = unicodedata.normalize("NFKD", limpio)
    limpio = "".join(c for c in limpio if not unicodedata.combining(c))
    return " ".join(limpio.split()).upper()


def _estado_de(texto: str) -> str:
    """Devuelve el estado canonico contenido en `texto`, o DESCONOCIDO."""
    norm = normalizar(texto)
    if not norm:
        return ESTADO_DESCONOCIDO
    # Coincidencia exacta primero para no confundir 'TODO' dentro de otra palabra.
    for estado in ESTADOS:
        if norm == estado:
            return estado
    for estado in ESTADOS:
        if re.search(r"(?<![A-Z])" + re.escape(estado) + r"(?![A-Z])", norm):
            return estado
    return ESTADO_DESCONOCIDO


def _celdas(linea: str) -> list[str]:
    """Celdas de una fila de tabla markdown, sin los vacios de los bordes."""
    if "|" not in linea:
        return []
    partes = linea.split("|")
    if partes and not partes[0].strip():
        partes = partes[1:]
    if partes and not partes[-1].strip():
        partes = partes[:-1]
    return [p.strip() for p in partes]


def _es_separador(celdas: list[str]) -> bool:
    return bool(celdas) and all(set(c) <= set("-: ") and c for c in celdas)


def parse_plan(texto: str) -> dict:
    """Extrae microtareas, subtareas e hitos de un PLAN.md.

    Devuelve::

        {
          "microtareas": [{"id": "H1.S1.M1", "estado": "TODO", "titulo": "..."}],
          "hitos":       [{"id": "H1", "estado": "TODO", "titulo": "..."}],
          "subtareas":   [{"id": "H1.S1", "estado": "HECHO", "titulo": "..."}],
          "malformado":  bool,
          "razones":     [str, ...],
        }
    """
    microtareas: list[dict] = []
    hitos: list[dict] = []
    subtareas: list[dict] = []
    razones: list[str] = []
    vistos: set[str] = set()

    contexto_id = None  # ultimo H1 / H1.S1 visto por encabezado
    contexto_titulo = ""
    contexto_tipo = None

    for bruto in (texto or "").splitlines():
        linea = bruto.rstrip()

        encabezado = _HEADING.match(linea)
        if encabezado:
            titulo = encabezado.group(1).strip()
            # Formato de la regla 20: "## H1 - nombre" / "### H1.S1 - nombre"
            primero = titulo.split()[0].strip(":.-") if titulo.split() else ""
            if _HITO_ID.match(primero):
                contexto_id, contexto_titulo, contexto_tipo = primero.upper(), titulo, "hito"
                hitos.append({"id": primero.upper(), "estado": ESTADO_DESCONOCIDO, "titulo": titulo})
            elif _SUB_ID.match(primero):
                contexto_id, contexto_titulo, contexto_tipo = primero.upper(), titulo, "subtarea"
                subtareas.append({"id": primero.upper(), "estado": ESTADO_DESCONOCIDO, "titulo": titulo})
            else:
                contexto_id, contexto_titulo, contexto_tipo = None, "", None
            continue

        # "**Estado:** TODO" aplica al hito/subtarea del encabezado vigente.
        marca = _ESTADO_LINEA.search(linea)
        if marca and contexto_tipo:
            estado = _estado_de(marca.group(1))
            destino = hitos if contexto_tipo == "hito" else subtareas
            for item in reversed(destino):
                if item["id"] == contexto_id:
                    item["estado"] = estado
                    break
            if estado == ESTADO_DESCONOCIDO:
                razones.append(f"{contexto_id}: estado no reconocido ({marca.group(1).strip()!r})")
            continue

        # Filas de tabla con una microtarea.
        celdas = _celdas(linea)
        if not celdas or _es_separador(celdas):
            continue
        m = _MICRO_ID.search(celdas[0])
        if not m:
            # No descartar en silencio: una fila que PARECE microtarea pero cuyo ID no
            # respeta H<n>.S<n>.M<n> (p. ej. "H3.M1", saltandose la capa de subtarea que
            # exige la regla 20) desapareceria del conteo y el avance quedaria inflado.
            if len(celdas) >= 4 and _CANDIDATA_ID.match(celdas[0]):
                razones.append(
                    f"fila ignorada: {celdas[0]!r} no es un ID de microtarea valido "
                    f"(se espera H<n>.S<n>.M<n>; la regla 20 exige las tres capas)"
                )
            continue
        mid = m.group(0).upper()
        if mid in vistos:
            razones.append(f"{mid}: microtarea duplicada")
            continue
        vistos.add(mid)

        estado = ESTADO_DESCONOCIDO
        for celda in reversed(celdas[1:]):
            cand = _estado_de(celda)
            if cand != ESTADO_DESCONOCIDO:
                estado = cand
                break
        if estado == ESTADO_DESCONOCIDO:
            razones.append(f"{mid}: sin estado reconocible en la fila")
        titulo = celdas[1] if len(celdas) > 1 else ""
        microtareas.append({"id": mid, "estado": estado, "titulo": titulo})

    if not microtareas:
        razones.append("el plan no declara ninguna microtarea (regla 20: las tres capas son obligatorias)")

    return {
        "microtareas": microtareas,
        "hitos": hitos,
        "subtareas": subtareas,
        "malformado": bool(razones),
        "razones": razones,
    }


def resumen(plan: dict) -> dict:
    """Conteos y porcentaje CALCULADO (regla 20: nunca estimado)."""
    micros = plan.get("microtareas", [])
    total = len(micros)
    por_estado = {e: 0 for e in ESTADOS}
    por_estado[ESTADO_DESCONOCIDO] = 0
    for m in micros:
        por_estado[m["estado"]] = por_estado.get(m["estado"], 0) + 1
    hechas = por_estado.get("HECHO", 0)
    return {
        "total": total,
        "por_estado": por_estado,
        "hechas": hechas,
        "porcentaje": round(100.0 * hechas / total, 1) if total else 0.0,
        "activas": [m for m in micros if m["estado"] in ESTADOS_ACTIVOS],
        "incompletas": [m for m in micros if m["estado"] in ESTADOS_INCOMPLETOS],
    }


def tiene_trabajo_activo(plan: dict) -> bool:
    """True si el plan declara microtareas en TODO o EN CURSO."""
    return any(m["estado"] in ESTADOS_ACTIVOS for m in plan.get("microtareas", []))


def secciones_faltantes(texto_reporte: str) -> list[str]:
    """Secciones obligatorias de la regla 40 que no aparecen como encabezado."""
    presentes = set()
    for linea in (texto_reporte or "").splitlines():
        enc = _HEADING.match(linea)
        if not enc:
            continue
        titulo = normalizar(enc.group(1))
        for seccion in SECCIONES_REPORTE:
            if seccion in titulo:
                presentes.add(seccion)
    return [s for s in SECCIONES_REPORTE if s not in presentes]


def leer(ruta: Path) -> str:
    """Lectura tolerante: UTF-8 explicito, nunca lanza por bytes raros."""
    try:
        return ruta.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def leer_evento(stream=None) -> tuple[dict | None, str]:
    """Lee el evento JSON del hook desde stdin. Devuelve (evento, error).

    Windows importa aca: PowerShell antepone un BOM UTF-8 (``\\xef\\xbb\\xbf``) al
    contenido que manda por la tuberia, y `json.load` falla con "Unexpected UTF-8 BOM".
    Sin esto los candados quedan INERTES en Windows sin que nadie se entere, que es
    justo el anti-patron que `hooks-and-guardrails` advierte. Por eso se leen bytes
    crudos y se decodifica con ``utf-8-sig``, que consume el BOM si esta.

    Nunca lanza: ante cualquier problema devuelve (None, motivo) para que quien llama
    decida, y el motivo SIEMPRE debe terminar en stderr (un candado mudo es indepurable).
    """
    import json as _json
    import sys as _sys

    try:
        buffer = getattr(stream or _sys.stdin, "buffer", None)
        if buffer is not None:
            crudo = buffer.read()
            texto = crudo.decode("utf-8-sig", errors="replace")
        else:  # stream de texto (pruebas)
            texto = (stream or _sys.stdin).read()
            if texto.startswith("﻿"):
                texto = texto[1:]
        texto = texto.strip()
        if not texto:
            return None, "stdin vacio"
        datos = _json.loads(texto)
        if not isinstance(datos, dict):
            return None, f"el evento no es un objeto JSON (es {type(datos).__name__})"
        return datos, ""
    except Exception as exc:
        return None, f"{type(exc).__name__}: {exc}"


def buscar_planes(raiz: Path, patron: str = GLOB_PLANES) -> list[Path]:
    """Todos los PLAN.md bajo docs/trabajo/*/ ordenados por nombre."""
    try:
        return sorted(raiz.glob(patron))
    except OSError:
        return []


def trabajos_activos(raiz: Path, patron: str = GLOB_PLANES) -> list[dict]:
    """Planes con trabajo vivo, con su reporte y las secciones que le faltan."""
    activos = []
    for ruta_plan in buscar_planes(raiz, patron):
        plan = parse_plan(leer(ruta_plan))
        if not tiene_trabajo_activo(plan):
            continue
        ruta_reporte = ruta_plan.parent / "REPORTE.md"
        existe = ruta_reporte.is_file()
        activos.append(
            {
                "plan": ruta_plan,
                "reporte": ruta_reporte,
                "reporte_existe": existe,
                "faltantes": secciones_faltantes(leer(ruta_reporte)) if existe else list(SECCIONES_REPORTE),
                "resumen": resumen(plan),
                "malformado": plan["malformado"],
            }
        )
    return activos
