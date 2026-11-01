# Plan — Disponibilidad 30d, presupuesto de error y última interrupción en Sistemas › Servicios

- Fecha: 2026-10-29 · Repos: PasanakuBackend (dev, test) · Predecesor: 2026-10-29-imagenes-sobreviven-limpieza-coolify
- Resultado observable: en el backoffice, Sistemas › Servicios y SLO muestra por servicio la
  disponibilidad medida en los últimos 30 días, el presupuesto de error restante contra un SLO
  de 99,5 % y la hora de la última interrupción, en vez de «—».
- Kill-test: `GET /indicadores/operacion/servicios` en TEST sin los campos nuevos, o la pantalla
  todavía con «—» después de que se tomaron muestras.

## Hallazgos
- Pantalla: `apps/backoffice/src/app/rutas/sistemas/dominio/salud-de-servicios.ts:20-31` pone
  «—» a propósito: el contrato `SaludDeServicio` (`servicios/auditoria/.../openapi/auditoria.yaml:998`)
  solo trae servicio, estado y latenciaMs.
- `SondaDeSalud` consulta el readiness en vivo y no persiste nada. No corre Prometheus en
  TEST (`PROMETHEUS_URL` vacío en el contenedor de `auditoria`).
- Esquema dirigido por modelo: `docs/entidades/09_auditoria_reportes.puml` → `scripts/generar_ddl.py`
  → `sql/`. jOOQ se genera por introspección de la base viva (`aportaya.jooq`).
- Jobs: `@Scheduled` + ShedLock (`servicios/cumplimiento/.../trabajos/ControlDeIncidentesVencidos.java`).

## Decisiones (de Pablo, 2026-10-29)
- Fuente: `auditoria` guarda una muestra por servicio y minuto (opción recomendada).
- SLO: 99,5 % para todos los servicios, ventana de 30 días.

## Decisiones técnicas propias
- Disponibilidad = minutos ARRIBA / minutos medidos (no se cuentan como caída los minutos sin
  muestra: si `auditoria` estuvo caído no se sabe). El contrato expone desde cuándo hay muestras.
- Presupuesto restante = 1 − (fracción no ARRIBA / 0,005); puede ser negativo (SLO excedido).
- **Corregido durante la ejecución:** en vez de una muestra por minuto con purga, un resumen por
  servicio y día (`auditoria.disponibilidad_servicio_dia`). Motivo: los roles de servicio no tienen
  DELETE (`sql/00_base/03_permisos.sql:12`), así que una purga exigía cambiar el generador de
  permisos. El minuto se suma con UPSERT cuya condición va en la escritura
  (`WHERE ultimo_minuto_en < minuto`): repetir el mismo minuto no cuenta dos veces.
  Ventana: los 30 días calendario UTC más recientes, incluido hoy.
- Campos nuevos opcionales en el contrato: compatible hacia atrás.

## Alcance
- IN: modelo + DDL generado de `muestra_salud_servicio`; job de muestreo + purga; cálculo en
  `SaludDeServicios`; contrato y controlador; pantalla del backoffice.
- OUT: Operación › Estado (misma fuente, no se toca); Prometheus.
- Ambigüedad: «Última interrupción» = última muestra no ARRIBA (incluye DEGRADADO). Confirmar con Pablo.

## H1 — La pantalla muestra disponibilidad, presupuesto y última interrupción reales
**CA:** Dado que `auditoria` tomó muestras, cuando un operador abre Sistemas › Servicios, entonces
ve por servicio la disponibilidad, el presupuesto contra 99,5 % y la última interrupción.
**DoD:** tests dirigidos verdes; en TEST, respuesta real del endpoint con los campos y captura de la pantalla.
**Estado:** HECHO (funcional; captura visual pendiente)

### H1.S1 — Persistencia de muestras
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Entidad `disponibilidad_servicio_dia` en el puml y DDL regenerado | el archivo sql existe y el generador sale 0 | `python3 scripts/generar_ddl.py; echo $?` → 0 | HECHO |
| H1.S1.M2 | Esquema aplicado en base local y jOOQ generado | compila | `./gradlew :servicios:auditoria:compileJava` → BUILD SUCCESSFUL | HECHO |
| H1.S1.M3 | Repositorio: sumar minuto idempotente y resumen 30d | test de integración contra Postgres real | `./gradlew :servicios:auditoria:integrationTest --tests '*DisponibilidadRepositorioTest'` verde | HECHO |

### H1.S2 — Muestreo y cálculo
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Job por minuto que suma la sonda | test unitario | `./gradlew :servicios:auditoria:test --tests '*MuestreoDeSalud*'` verde | HECHO |
| H1.S2.M2 | `SaludDeServicios` agrega disponibilidad, presupuesto, última interrupción | test unitario con casos 100 %, 99,5 %, sin muestras, presupuesto negativo | `--tests '*DisponibilidadDelServicioTest' '*SaludDeServiciosTest'` verde | HECHO |

### H1.S3 — Contrato y pantalla
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Contrato + controlador con los campos opcionales | web test verde | `./gradlew :servicios:auditoria:webTest` verde | HECHO |
| H1.S3.M2 | Pantalla formatea los campos (y «—» si faltan) | spec verde | `npx ng test --include='src/app/rutas/sistemas/**/*.spec.ts'` verde | HECHO |
| H1.S3.M3 | Desplegado en TEST: endpoint real con datos | respuesta con campos | curl autenticado / captura | HECHO |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Volumen | consultas lentas | ~14 filas por día; índice único (dia, servicio) |
| Varias réplicas de auditoria muestrean a la vez | duplicados | ShedLock + ON CONFLICT DO NOTHING |

## Microtareas agregadas en el camino
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | Narrativa de la tabla y cifra de tablas en los planes (verificadores de CI) | los verificadores pasan | `verificar_narrativas.py` y `verificar_boveda.py` → sin fallas | HECHO |
| H1.S4.M2 | «Última interrupción» se ordena por instante, no por texto | `valorParaOrden` usa el ISO | spec de sistemas verde | HECHO |
