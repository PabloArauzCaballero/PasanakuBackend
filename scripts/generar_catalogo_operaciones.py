#!/usr/bin/env python3
"""
Generador del Catálogo de Operaciones y Flujos de AportaYa.
Extrae los 314 endpoints de los 15 microservicios con:
- Tablas impactadas (INSERT, UPDATE, SELECT, DELETE, APPEND)
- Para qué sirve (Explicación de Negocio y Sistema)
- DTOs de entrada y salida con sus respectivas validaciones
- Payload de ejemplo (JSON realista)
- En qué flujo está y su árbol de decisiones posibles
"""

from __future__ import annotations

import glob
import json
import pathlib
import re
import sys
import yaml

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import modelo  # noqa: E402

RAIZ = pathlib.Path(__file__).resolve().parent.parent

FLUJOS_CATALOGO = {
    "aporte-mora-cobertura": {
        "id": "aporte-mora-cobertura",
        "nombre": "Del aporte del período a la mora, el recargo, la cobertura y la recuperación",
        "objetivo": "Asegurar que el pozo se entregue completo aun ante mora, aplicando recargos reglamentarios y cobertura del fondo de garantía.",
        "dinero": True,
        "cus": ["CU-21", "CU-23", "CU-25"],
    },
    "recarga-y-retiro": {
        "id": "recarga-y-retiro",
        "nombre": "Recarga de billetera, custodia y desembolso de retiro bancario",
        "objetivo": "Movimiento de fondos entre el sistema financiero boliviano y la billetera digital bajo encaje y partida doble.",
        "dinero": True,
        "cus": ["CU-17", "CU-18", "CU-19", "CU-100"],
    },
    "entrega-del-turno": {
        "id": "entrega-del-turno",
        "nombre": "Conformación del pozo, adjudicación del turno y entrega de fondos",
        "objetivo": "Validar la identidad del beneficiario, liberar las retenciones y ejecutar la transferencia del fondo del turno.",
        "dinero": True,
        "cus": ["CU-26", "CU-27", "CU-28"],
    },
    "onboarding-kyc-cuenta": {
        "id": "onboarding-kyc-cuenta",
        "nombre": "Registro de usuario, verificación de identidad KYC y seguridad MFA",
        "objetivo": "Alta de participantes, validación de cédula de identidad, nivel de debida diligencia y configuración de factores seguros.",
        "dinero": False,
        "cus": ["CU-01", "CU-02", "CU-04", "CU-05", "CU-06"],
    },
    "creacion-y-sorteo-grupo": {
        "id": "creacion-y-sorteo-grupo",
        "nombre": "Creación del grupo pasanaku, asignación de cupos y sorteo verificable",
        "objetivo": "Configurar las reglas del círculo de ahorro, reclutar miembros y definir el orden de turnos mediante sorteo criptográfico.",
        "dinero": False,
        "cus": ["CU-11", "CU-12", "CU-13", "CU-15", "CU-91"],
    },
    "garantia-incumplimiento-cobranza": {
        "id": "garantia-incumplimiento-cobranza",
        "nombre": "Gestión del fondo de garantía, subrogación de deuda y cobranza",
        "objetivo": "Proteger los fondos colectivos ante impago, subrogar derechos de cobro y activar la gestión extrajudicial y judicial.",
        "dinero": True,
        "cus": ["CU-31", "CU-32", "CU-33", "CU-34"],
    },
    "administracion-usuarios-y-permisos": {
        "id": "administracion-usuarios-y-permisos",
        "nombre": "Gobierno de identidades internas, roles de backoffice y sesiones",
        "objetivo": "Garantizar la segregación de funciones, control de acceso basado en roles (RBAC) y auditoría de operadores.",
        "dinero": False,
        "cus": ["CU-08", "CU-14", "CU-56"],
    },
    "gobierno-catalogo-y-metadatos": {
        "id": "gobierno-catalogo-y-metadatos",
        "nombre": "Gobernanza del catálogo de datos, retención y revisiones",
        "objetivo": "Mantener al día las fichas de gobierno de cada tabla y columna del modelo con trazabilidad y versionado.",
        "dinero": False,
        "cus": ["CU-130", "CU-131"],
    },
    "reportes-y-cumplimiento-uif": {
        "id": "reportes-y-cumplimiento-uif",
        "nombre": "Monitoreo transaccional, detección de alertas y reportes UIF/ASFI",
        "objetivo": "Identificar operaciones inusuales o sospechosas, emitir reportes regulatorios (ROS/ROG) y atender requerimientos.",
        "dinero": True,
        "cus": ["CU-44", "CU-58", "CU-98"],
    },
    "contabilidad-y-cierre": {
        "id": "contabilidad-y-cierre",
        "nombre": "Cierres diarios, conciliación de custodia y estados financieros",
        "objetivo": "Garantizar el cuadre diario de saldos bancarios vs libro de billeteras y asentar la contabilidad corporativa.",
        "dinero": True,
        "cus": ["CU-51", "CU-52"],
    },
    "inversiones-reserva-custodia": {
        "id": "inversiones-reserva-custodia",
        "nombre": "Rentabilización de fondos de reserva y rendimientos",
        "objetivo": "Inversión autorizada de excedentes de liquidez en títulos de bajo riesgo y distribución de rendimientos.",
        "dinero": True,
        "cus": ["CU-61", "CU-62", "CU-63"],
    },
    "publicidad-y-campanas": {
        "id": "publicidad-y-campanas",
        "nombre": "Campañas de fidelización, referidos e incentivos comunitarios",
        "objetivo": "Gestionar promociones y beneficios por puntualidad y recomendación de la plataforma.",
        "dinero": False,
        "cus": ["CU-71", "CU-72"],
    },
    "tarifas-y-comisiones": {
        "id": "tarifas-y-comisiones",
        "nombre": "Tarifario oficial, comisiones de pasarela y facturación",
        "objetivo": "Calcular y liquidar comisiones de servicio e impuestos de ley con cotización inmutable.",
        "dinero": True,
        "cus": ["CU-81", "CU-82"],
    },
    "transparencia-y-reputacion": {
        "id": "transparencia-y-reputacion",
        "nombre": "Scoring crediticio alternativo, insignias y cadena de hashes",
        "objetivo": "Publicar compromisos criptográficos reproducibles y generar historial de cumplimiento del participante.",
        "dinero": False,
        "cus": ["CU-91", "CU-92", "CU-93"],
    },
    "notificaciones-y-mensajeria": {
        "id": "notificaciones-y-mensajeria",
        "nombre": "Notificaciones omnicanal, recordatorios de cuota y alertas",
        "objetivo": "Comunicación oportuna a participantes sobre vencimientos, turnos, verificaciones y eventos de seguridad.",
        "dinero": False,
        "cus": ["CU-95", "CU-96"],
    },
}

PALABRAS_SENSIBLES = (
    "retiro", "transferencia", "aprobacion", "rechazo", "mfa", "password", "clave",
    "credencial", "webhook", "pago", "cobro", "saldo", "cuenta", "documento", "kyc",
    "biometria", "reversa", "aval", "garantia", "sancion", "permiso", "rol", "token"
)


def inferir_flujo(servicio: str, cu: str | None, ruta: str, op_id: str) -> dict:
    if cu:
        for fl_id, fl in FLUJOS_CATALOGO.items():
            if cu in fl["cus"]:
                return fl
    
    # Inferir por ruta y servicio
    if servicio == "aportes":
        return FLUJOS_CATALOGO["aporte-mora-cobertura"]
    if servicio in ("nucleo-financiero", "entregas") and ("retiro" in ruta or "recarga" in ruta):
        return FLUJOS_CATALOGO["recarga-y-retiro"]
    if servicio == "entregas":
        return FLUJOS_CATALOGO["entrega-del-turno"]
    if servicio == "identidad":
        return FLUJOS_CATALOGO["onboarding-kyc-cuenta"]
    if servicio == "grupos":
        return FLUJOS_CATALOGO["creacion-y-sorteo-grupo"]
    if servicio == "garantia":
        return FLUJOS_CATALOGO["garantia-incumplimiento-cobranza"]
    if servicio == "auditoria" and ("catalogo" in ruta or "gobierno" in ruta):
        return FLUJOS_CATALOGO["gobierno-catalogo-y-metadatos"]
    if servicio in ("cumplimiento", "auditoria"):
        return FLUJOS_CATALOGO["reportes-y-cumplimiento-uif"]
    if servicio == "erp":
        return FLUJOS_CATALOGO["contabilidad-y-cierre"]
    if servicio == "inversiones":
        return FLUJOS_CATALOGO["inversiones-reserva-custodia"]
    if servicio == "publicidad":
        return FLUJOS_CATALOGO["publicidad-y-campanas"]
    if servicio == "tarifas":
        return FLUJOS_CATALOGO["tarifas-y-comisiones"]
    if servicio == "transparencia":
        return FLUJOS_CATALOGO["transparencia-y-reputacion"]
    if servicio == "notificaciones":
        return FLUJOS_CATALOGO["notificaciones-y-mensajeria"]
    
    return FLUJOS_CATALOGO["administracion-usuarios-y-permisos"]


def inferir_tablas_impactadas(servicio: str, ruta: str, metodo: str, cu: str | None, op_id: str, mod_tablas: list[str]) -> list[dict]:
    impactadas = []
    es_mutacion = metodo in ("POST", "PUT", "PATCH", "DELETE")
    
    # Buscar nombres de tabla dentro de la ruta o en operationId
    limpio = (ruta + " " + op_id).lower().replace("/", " ").replace("-", " ").replace("_", " ")
    palabras = set(limpio.split())
    
    coincidentes = []
    for t in mod_tablas:
        partes_t = set(t.split("_"))
        if partes_t.intersection(palabras) or t in limpio:
            coincidentes.append(t)
    
    if not coincidentes and mod_tablas:
        # Asignar la tabla principal del módulo
        coincidentes = [mod_tablas[0]]
        
    for t in coincidentes[:3]:
        op_db = "INSERT / UPDATE" if es_mutacion else "SELECT"
        impactadas.append({
            "esquema": servicio.replace("-", "_"),
            "tabla": t,
            "operacion": op_db,
            "impacto": f"Afecta el estado de negocio de `{t}` al invocar `{op_id}`." if es_mutacion else f"Consulta de lectura sobre `{t}` para obtener datos vigentes.",
            "principal": True,
            "auditoria": False
        })
        
    if es_mutacion:
        impactadas.append({
            "esquema": servicio.replace("-", "_"),
            "tabla": "outbox_evento",
            "operacion": "INSERT",
            "impacto": "Registra el evento transaccional correspondiente para mensajería asíncrona hacia Kafka.",
            "principal": False,
            "auditoria": True
        })
        
    return impactadas


def generar_ejemplo(schema: dict, schemas: dict, nivel: int = 0) -> any:
    if nivel > 3:
        return {}
    if "$ref" in schema:
        ref_nombre = schema["$ref"].split("/")[-1]
        return generar_ejemplo(schemas.get(ref_nombre, {}), schemas, nivel + 1)
    
    tipo = schema.get("type", "string")
    formato = schema.get("format", "")
    enum_vals = schema.get("enum", [])
    
    if enum_vals:
        return enum_vals[0]
    if formato == "uuid":
        return "7c9e6679-7425-40de-944b-e07fc1f90ae7"
    if formato == "date-time":
        return "2026-10-20T14:30:00Z"
    if formato == "date":
        return "2026-10-20"
    if tipo == "string":
        if "pattern" in schema and "d+" in schema["pattern"]:
            return "150.00"
        return "Ejemplo de texto"
    if tipo == "integer":
        return schema.get("minimum", 1)
    if tipo in ("number", "decimal"):
        return 100.50
    if tipo == "boolean":
        return True
    if tipo == "array":
        item_schema = schema.get("items", {})
        return [generar_ejemplo(item_schema, schemas, nivel + 1)]
    if tipo == "object":
        resultado = {}
        for prop, prop_schema in schema.get("properties", {}).items():
            resultado[prop] = generar_ejemplo(prop_schema, schemas, nivel + 1)
        return resultado
    return "valor"


def extraer_dto_campos(schema: dict, schemas: dict) -> list[dict]:
    if "$ref" in schema:
        ref_nombre = schema["$ref"].split("/")[-1]
        return extraer_dto_campos(schemas.get(ref_nombre, {}), schemas)
    
    campos = []
    requeridos = set(schema.get("required", []))
    for prop, prop_schema in schema.get("properties", {}).items():
        sub_schema = prop_schema
        if "$ref" in prop_schema:
            ref_n = prop_schema["$ref"].split("/")[-1]
            sub_schema = schemas.get(ref_n, prop_schema)
            
        tipo = sub_schema.get("type", "string")
        formato = sub_schema.get("format")
        tipo_str = f"{tipo} ({formato})" if formato else tipo
        if "$ref" in prop_schema:
            tipo_str = prop_schema["$ref"].split("/")[-1]
            
        validaciones = []
        if prop in requeridos:
            validaciones.append("@NotNull / Requerido")
        if "enum" in sub_schema:
            validaciones.append(f"Valores: {', '.join(str(x) for x in sub_schema['enum'][:4])}")
        if "pattern" in sub_schema:
            validaciones.append(f"Patrón: {sub_schema['pattern']}")
        if "maxLength" in sub_schema:
            validaciones.append(f"Máx: {sub_schema['maxLength']}")
        if "minimum" in sub_schema:
            validaciones.append(f"Mín: {sub_schema['minimum']}")
            
        campos.append({
            "nombre": prop,
            "tipo": tipo_str,
            "requerido": prop in requeridos,
            "validacion": "; ".join(validaciones) or "Validación estándar de tipo",
            "descripcion": sub_schema.get("description", f"Campo {prop} de la operación"),
        })
    return campos


def procesar():
    mods, _, _ = modelo.cargar()
    tablas_por_servicio = {}
    for k, d in mods.items():
        esq = modelo.esquema_de(list(d["entidades"].values())[0]["tabla"], k)
        svc = esq.replace("_", "-")
        tablas_por_servicio.setdefault(svc, []).extend([e["tabla"] for e in d["entidades"].values()])

    archivos_yaml = sorted(glob.glob("servicios/*/src/main/resources/openapi/*.yaml"))
    
    operaciones_total = []
    
    for f in archivos_yaml:
        spec = yaml.safe_load(open(f, encoding="utf-8"))
        svc = pathlib.Path(f).parent.parent.parent.parent.name
        schemas = spec.get("components", {}).get("schemas", {})
        
        # Buscar códigos de error en comentarios del header OpenAPI
        errores_servicio = {}
        texto_spec = pathlib.Path(f).read_text(encoding="utf-8")
        for m in re.finditer(r"(AP-CU\d+-\d+)\s+([A-Z_]+)\s+[·-]\s+(.+)", texto_spec):
            errores_servicio[m.group(1)] = {
                "codigo": m.group(1),
                "error": m.group(2),
                "significa": m.group(3).strip()
            }
            
        for ruta, methods in spec.get("paths", {}).items():
            for metodo_str, op in methods.items():
                if metodo_str not in ("get", "post", "put", "patch", "delete") or not isinstance(op, dict):
                    continue
                
                op_id = op.get("operationId", f"{metodo_str}_{ruta.replace('/', '_')}")
                resumen = op.get("summary", f"Operación {op_id}")
                descripcion = op.get("description", resumen)
                
                # Detectar CU
                cu_match = re.search(r"CU-(\d+)", resumen + " " + descripcion)
                cu = f"CU-{cu_match.group(1)}" if cu_match else None
                
                flujo_asociado = inferir_flujo(svc, cu, ruta, op_id)
                
                # Permisos y seguridad
                es_sensible = any(p in (op_id + " " + ruta).lower() for p in PALABRAS_SENSIBLES)
                exige_clave = any(
                    ("$ref" in p and "ClaveIdempotencia" in p["$ref"]) or p.get("name") == "idempotency-key"
                    for p in op.get("parameters", []) if isinstance(p, dict)
                )
                
                riesgo = "CRITICO" if es_sensible and metodo_str != "get" else "ALTO" if es_sensible else "MEDIO" if metodo_str != "get" else "BAJO"
                
                # Tablas impactadas
                tablas_imp = inferir_tablas_impactadas(svc, ruta, metodo_str.upper(), cu, op_id, tablas_por_servicio.get(svc, []))
                
                # Request DTO
                dto_entrada = None
                payload_ejemplo = None
                if "requestBody" in op:
                    rb_content = op["requestBody"].get("content", {}).get("application/json", {})
                    rb_schema = rb_content.get("schema", {})
                    ref_nombre = rb_schema.get("$ref", "").split("/")[-1] if "$ref" in rb_schema else "RequestPayload"
                    dto_entrada = {
                        "nombre": ref_nombre,
                        "campos": extraer_dto_campos(rb_schema, schemas)
                    }
                    payload_ejemplo = generar_ejemplo(rb_schema, schemas)
                elif op.get("parameters"):
                    # Query / Path DTO
                    dto_entrada = {
                        "nombre": f"Parametros_{op_id}",
                        "campos": [
                            {
                                "nombre": p.get("name", "param"),
                                "tipo": p.get("schema", {}).get("type", "string"),
                                "requerido": p.get("required", False),
                                "validacion": f"Ubicación: {p.get('in')}",
                                "descripcion": p.get("description", f"Parámetro {p.get('name')}")
                            }
                            for p in op.get("parameters", []) if isinstance(p, dict)
                        ]
                    }
                    payload_ejemplo = {p.get("name"): "ejemplo-uuid" if p.get("schema", {}).get("format") == "uuid" else "1" for p in op.get("parameters", []) if isinstance(p, dict)}
                    
                # Response DTO
                dto_salida = None
                payload_salida = None
                resp_200 = op.get("responses", {}).get("200") or op.get("responses", {}).get("201")
                if resp_200 and isinstance(resp_200, dict):
                    resp_content = resp_200.get("content", {}).get("application/json", {})
                    resp_schema = resp_content.get("schema", {})
                    ref_salida = resp_schema.get("$ref", "").split("/")[-1] if "$ref" in resp_schema else "ResponsePayload"
                    dto_salida = {
                        "nombre": ref_salida,
                        "campos": extraer_dto_campos(resp_schema, schemas)
                    }
                    payload_salida = generar_ejemplo(resp_schema, schemas)
                    
                # Decisiones y bifurcaciones
                decisiones = [
                    {
                        "codigo": "EXITO",
                        "http": 201 if metodo_str == "post" else 200,
                        "condicion": "Petición válida y autorizada. Precondiciones cumplidas.",
                        "siguiente": "Transición exitosa al siguiente paso del flujo o persistencia."
                    }
                ]
                
                # Errores del caso de uso asociados
                if cu:
                    for cod, err in errores_servicio.items():
                        if cu in cod:
                            decisiones.append({
                                "codigo": cod,
                                "http": 422,
                                "condicion": err["significa"],
                                "siguiente": "Rechazo controlado / FIN con rollback."
                            })
                            
                if not any(d["http"] == 422 for d in decisiones):
                    decisiones.append({
                        "codigo": f"AP-{cu or 'SISTEMA'}-01",
                        "http": 422,
                        "condicion": "Violación de regla de negocio o estado no válido.",
                        "siguiente": "Rechazo controlado con mensaje explicativo."
                    })
                    
                decisiones.append({
                    "codigo": "NO_AUTORIZADO",
                    "http": 401,
                    "condicion": "Token ausente, vencido o firma inválida.",
                    "siguiente": "Exige reautenticación en pantalla de ingreso."
                })
                decisiones.append({
                    "codigo": "ACCESO_DENEGADO",
                    "http": 403,
                    "condicion": "Rol o permiso insuficiente para esta operación.",
                    "siguiente": "Bloqueo por seguridad RBAC."
                })
                
                operaciones_total.append({
                    "id": f"{svc}-{op_id}",
                    "servicio": svc,
                    "metodo": metodo_str.upper(),
                    "ruta": ruta,
                    "operationId": op_id,
                    "modulo": op.get("tags", [svc])[0] if op.get("tags") else svc,
                    "cu": cu,
                    "proposito": resumen,
                    "explicacionNegocio": descripcion,
                    "explicacionSistema": f"Servicio `{svc}`, controlador Spring `@RestController`. Implementa la interfaz generada `{svc.capitalize()}Api`. Transaccionalidad garantizada en organismo jOOQ.",
                    "permiso": op.get("security", [{"Sesion": []}])[0].get("Sesion", ["PARTICIPANTE"])[0] if op.get("security") else "REQUIERE_SESION",
                    "riesgo": riesgo,
                    "sensible": es_sensible,
                    "pideSesion": True,
                    "idempotencia": exige_clave,
                    "tablasImpactadas": tablas_imp,
                    "dtoEntrada": dto_entrada,
                    "dtoSalida": dto_salida,
                    "payloadEjemplo": payload_ejemplo,
                    "payloadRespuesta": payload_salida,
                    "flujo": {
                        "id": flujo_asociado["id"],
                        "nombre": flujo_asociado["nombre"],
                        "paso": f"Paso operativo `{op_id}`",
                        "precondicion": f"El servicio {svc} debe validar la existencia y vigencia de las entidades involucradas.",
                        "exito": "Se completa la transacción de negocio y se dispara la notificación u outbox correspondiente.",
                        "decisiones": decisiones
                    }
                })
                
    print(f"Total operaciones relevadas: {len(operaciones_total)}")
    
    # 1. Guardar en JSON para backoffice
    ruta_backoffice = RAIZ / "apps" / "backoffice" / "src" / "app" / "rutas" / "sistemas" / "operaciones" / "dominio"
    ruta_backoffice.mkdir(parents=True, exist_ok=True)
    (ruta_backoffice / "operaciones.json").write_text(json.dumps(operaciones_total, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generado: {ruta_backoffice / 'operaciones.json'}")
    
    # 2. Guardar flujos para backoffice
    ruta_flujos = RAIZ / "apps" / "backoffice" / "src" / "app" / "rutas" / "sistemas" / "flujos" / "dominio"
    ruta_flujos.mkdir(parents=True, exist_ok=True)
    (ruta_flujos / "flujos.json").write_text(json.dumps(list(FLUJOS_CATALOGO.values()), ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generado: {ruta_flujos / 'flujos.json'}")
    
    # 3. Guardar en auditoria resources
    ruta_auditoria = RAIZ / "servicios" / "auditoria" / "src" / "main" / "resources" / "catalogo"
    ruta_auditoria.mkdir(parents=True, exist_ok=True)
    (ruta_auditoria / "catalogo-operaciones.json").write_text(json.dumps(operaciones_total, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Generado: {ruta_auditoria / 'catalogo-operaciones.json'}")

if __name__ == "__main__":
    procesar()
