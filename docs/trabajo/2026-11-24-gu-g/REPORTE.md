# Reporte — Lote G · CU de gobierno de participantes (borrador para aprobación)

> **AVANCE: 6 / 7 — 85,7 %.** Falta H1.S3.M3: CI del PR en verde (se actualiza al cerrar).

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-g-cu-participantes` → PR a `dev`
- Peldaño de evidencia alcanzado: `RUNS` sobre la bóveda (los verificadores corren en exit 0 con el CU nuevo). Es un documento: no hay comportamiento que ejercitar; el CU queda **BORRADOR** hasta que Pablo lo apruebe (decisión P-11).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Relevamiento de lo existente con rutas (tabla «Hallazgos») | `grep -n "ck_usuario_estado\|ck_bloqueo_cuenta_motivo\|ck_restriccion_usuario_tipo" sql/10_tablas/01_identidad_usuarios/*.sql` | PASS — [evidencia/H1.S1.M1-checks.txt](./evidencia/H1.S1.M1-checks.txt) |
| H1.S2.M1 | `docs/CasosDeUso/CU-134 Directorio y ficha del participante, y suspensión o bloqueo de su cuenta por seguridad con debido proceso.md`, con las 13 secciones, 11 códigos `AP-CU134-01..11`, 17 alternativos, 12 escenarios gherkin, sección de datos sensibles y 15 decisiones requeridas | `python3 scripts/verificar_boveda.py` | PASS `TODO OK` — [evidencia/verificar_boveda.txt](./evidencia/verificar_boveda.txt) |
| H1.S2.M2 | Fila en `_CasosDeUso.md` (Identidad), `total_casos: 113`, nota del rango; «Ver también» recíproco en CU-07, 17, 27, 44, 52, 55, 93 | `python3 scripts/verificar_boveda.py` | PASS (`"Ver también" recíproco` OK) |
| H1.S2.M3 | Cifras «112 casos de uso» → 113 en 6 archivos de `planes/` | `python3 scripts/verificar_boveda.py` | PASS (`ninguna cifra desfasada`) |
| H1.S3.M1 | Verificadores de los workflows en local | ver «Evidencia» | PASS: seguridad, bóveda, carriles, criterios, inventario de endpoints, contratos y límites, maqueta, flujos, pruebas web, catálogo de datos → exit 0 |
| H1.S3.M2 | Este reporte con las preguntas para Pablo | `python3 .claude/hooks/report_gate.py --self-test` | `14 PASS, 0 FAIL` |

## A medias
### H1.S3.M3 — PR a `dev` con CI en verde
- Qué anda: todos los verificadores que corre la CI sobre la bóveda pasan en local (ver Evidencia).
- Qué no anda: todavía no se observó la CI del PR.
- Qué falta exactamente: commit, push, abrir el PR y ver los checks en verde.
- Dónde quedó: rama `claude/gu-g-cu-participantes`, solo documentos y el catálogo de datos regenerado.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Implementación de CU-134 (Ola 3) | `DECISION_REQUIRED` | Que Pablo apruebe el CU y responda D-1 a D-15 |

## Hallazgos (relevamiento)
| Hecho | Ruta |
|---|---|
| `usuario.estado` ya admite `SUSPENDIDO` y `BLOQUEADO_SEGURIDAD`; CU-04 rechaza el login de ambos con AP-CU04-02 y revoca el refresh | `sql/10_tablas/01_identidad_usuarios/usuario.sql:25` · `docs/CasosDeUso/CU-04 …md:305-325` |
| `bloqueo_cuenta.motivo` ∈ `FRAUDE, INTENTOS_FALLIDOS, ORDEN_ADMIN, SOLICITUD_USUARIO` (no hay motivo de cuenta comprometida) | `sql/10_tablas/01_identidad_usuarios/bloqueo_cuenta.sql:14` |
| `restriccion_usuario` es la vía de la mora (CU-27), con tipos `LIMITE_MONTO, NO_CREAR_GRUPO, NO_SER_ORGANIZADOR, NO_UNIRSE` | `restriccion_usuario.sql:17-18` |
| `sancion` y `apelacion_sancion` exigen `registro_id` de incumplimiento: no sirven para una medida de seguridad sin modificar el modelo | `docs/Modelos/Entidades/08 …/sancion.md` |
| `politica_sancion.grupo_id` es nulable: podría ser la política de plataforma con plazos | `docs/Modelos/Entidades/08 …/politica_sancion.md` |
| `registro_acceso_datos` exige justificación ≥ 10 caracteres, pero `tipo_dato` no tiene valor para ficha ni correo | `docs/Restricciones.md:1599-1600` · `sql/10_tablas/09*/registro_acceso_datos.sql:19-20` |
| El permiso `DATOS_SENSIBLES_LEER` y los roles `FRAUD_ANALYST`, `RESPONSABLE_SEGURIDAD`, `SOPORTE` ya existen en la semilla; no hay permiso para leer participantes | `sql/60_semillas/10-roles-y-permisos.sql` |
| CU-44 impone deber de reserva (no avisar al titular) y dice que la cuenta pasa a `CONGELADA`, que es estado de `cuenta_billetera`, no de `usuario` | `docs/CasosDeUso/CU-44 …md:38,56` · `sql/10_tablas/10*/cuenta_billetera.sql:25` |
| Bolivia no tiene ley integral de protección de datos; el modelo sigue principios tipo RGPD | `docs/Cumplimiento.md:389-396` |
| Ningún CU especificaba directorio/ficha del participante ni suspensión de su cuenta (P-11) | `INVENTARIO.md` de gobierno de usuarios |

## Preguntas para Pablo
Están completas, con opciones y recomendación, en la sección «Decisiones requeridas» del CU-134. Resumen:

| ID | Pregunta |
|---|---|
| D-1 | ¿Catálogo cerrado de causales: `CUENTA_COMPROMETIDA`, `FRAUDE_CON_EVIDENCIA`, `ABUSO_CONTRA_PARTICIPANTES`, `CUENTA_DUPLICADA_O_DE_TERCERO`? |
| D-2 | ¿Medidas: bloqueo cautelar + suspensión temporal? ¿Existe inhabilitación definitiva? |
| D-3 | Números de plazos (descargo, apelación, prescripción, duración máxima) y dónde viven (¿`politica_sancion` de plataforma?) |
| D-4 | ¿Expediente en tablas nuevas de `identidad` (recomendado) o generalizar `sancion`? |
| D-5 | ¿Quién propone, decide y resuelve la apelación? ¿Permisos nuevos? |
| D-6 | Con `SUSPENDIDO`, CU-04 cierra todo login: ¿modo restringido para defenderse, pagar y reclamar? |
| D-7 | Sin cautelar, ¿la suspensión rige al decidir o al quedar firme? |
| D-8 | ¿Qué medidas firmes afectan la reputación? |
| D-9 | Confirmar la base legal declarada en la ficha |
| D-10 | ¿Siguen exigibles los aportes del suspendido? |
| D-11 | **Dinero:** ¿qué pasa con su entrega si su turno cae durante la medida? |
| D-12 | ¿Se lo reemplaza en el grupo? |
| D-13 | Inhabilitación definitiva con saldo o deuda |
| D-14 | Ampliar `registro_acceso_datos.tipo_dato` con `FICHA_PARTICIPANTE` y `CORREO` |
| D-15 | Motivo `CUENTA_COMPROMETIDA` en `bloqueo_cuenta` |
| — | Confirmar el número **CU-134** (CU-133 y CU-136 ya se usan como prefijo de pruebas en `servicios/entregas`) |

## Evidencia
```text
$ python3 scripts/verificar_boveda.py | tail -4
          cifras vigentes: 113 casos de uso · 354 tablas · 334 entidades · 188 restricciones · 56 ADR

TODO OK
exit=0

$ python3 scripts/verificar_criterios.py | tail -2
pendientes de implementar (6): CU-05 (identidad), CU-134 (identidad), CU-23 (garantia), CU-26 (garantia), CU-51 (aportes), CU-57 (nucleo-financiero)
Sin divergencias entre la boveda y el codigo.          (exit=0)

$ python3 scripts/verificar_seguridad.py | tail -1
TODO OK · 6 aviso(s)                                     (exit=0; los mismos 6 avisos en dev sin este cambio)

$ python3 scripts/verificar_carriles.py | tail -1
TODO OK (con avisos)                                     (exit=0; aviso previo: inversiones sin ficha de carril)

$ python3 scripts/inventario_endpoints.py --check       → exit=0, hallazgos: 0
$ python3 scripts/verificar_contratos_limites.py        → exit=0
$ python3 scripts/verificar_maqueta.py                  → exit=0, TODO OK
$ python3 scripts/validar_flujos.py                     → exit=0, Sin fallas.
$ python3 scripts/verificar_pruebas_web.py              → exit=0
$ python3 -I scripts/test_catalogo_de_datos.py          → OK
$ python3 -I scripts/catalogo_de_datos.py --escribir    → 404 tablas; regenera docs/catalogo-de-datos.md y catalogo-datos.json (cambio commiteado: CU-134 nombra tablas)
```
Archivos completos: [evidencia/](./evidencia/).

## No cubierto
- El CU no se implementó ni se ejecutó contra nada: es especificación. Ninguna regla de él está verificada en runtime.
- No se dieron de alta en `docs/Restricciones.md` las restricciones nuevas ni en el modelo las entidades `expediente_medida_cuenta` / `apelacion_medida_cuenta`, los permisos `PARTICIPANTES_*` ni los valores de CHECK nuevos: quedan propuestos dentro del CU.
- No se corrió la CI completa (Gradle, Flutter, Postgres): el cambio no toca código; se espera la CI del PR.

## Desvíos del plan
- Se agregaron al alcance (anotado en el PLAN): la línea «Ver también» recíproca en 7 CU vecinos y el catálogo de datos regenerado, porque los verificadores de la CI los exigen.
- `planes/17 …` tenía además «los 112 CU» en la línea 577; también se actualizó.

## Riesgos residuales
- Choque menor de merge si otro lote edita el «Ver también» de CU-07, 17, 27, 44, 52, 55 o 93, o regenera el catálogo de datos (lote E toca `servicios/auditoria/consumo`, no el JSON del catálogo).
- Hasta que se decida D-6, la especificación de «el dinero propio no se toca» choca con el rechazo total de login de CU-04 para `SUSPENDIDO`.
- CU-44 menciona un estado `CONGELADA` de cuenta que no existe en `usuario.estado` (sí en `cuenta_billetera`): inconsistencia previa, anotada, no corregida (fuera de alcance).

## Decisiones y ambigüedades
- Número **CU-134** (no CU-133) — supuesto; confirmar con Pablo.
- Servicio dueño `identidad` — supuesto; confirmar con Pablo.
- Título del CU: el pedido literal del lote G.
- Todo lo marcado «PROPUESTA — requiere aprobación de Pablo» en el CU es un supuesto sin fuente, no una regla.
