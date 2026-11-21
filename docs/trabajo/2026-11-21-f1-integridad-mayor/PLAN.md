# Plan — F1.S1: el mayor y los importes, garantizados por la base

- Fecha: 2026-11-21 · Repo: `PasanakuBackend` · Rama: `correcciones/f1-integridad-mayor-2026-11-21` (desde `origin/dev` 850e0f51)
- Predecesor: plan de correcciones `docs/trabajo/2026-11-21-plan-correcciones/PLAN.md` (rama `claude/nice-fermat-essf9e`), H1.S1.
- Hallazgos de origen: L5 `TYA-DINERO-001` (S1, reproducido), L1 `ANF-DINERO-001` (S1), L1 `ANF-DINERO-004` (S2),
  y las microtareas de anexo que tratan invariantes de la base: F1.L1.M01, F1.L1.M02, F1.L5.M06 (y, como
  referencia de alcance, F1.L1.M08/M09/M12, F1.L5.M07, F1.L2.M11, F1.L4.M24).
- Resultado observable: con el rol de la aplicación (`svc_nucleo_financiero`, `svc_aportes`), la base rechaza
  (a) una línea del mayor negativa, con dos lados o sin lado; (b) una línea agregada a un asiento firme desde otra
  transacción; (c) un asiento firme descuadrado o sin líneas; (d) un importe negativo de aporte, pago, reembolso o
  disputa; (e) `UPDATE`/`DELETE` sobre toda tabla append-only; (f) una `clave_idempotencia` repetida.
- Kill-test: el guion `evidencia/repro_tya_dinero_001.sql` (copia del experimento de la auditoría) **inserta** filas
  en `dev`. Si después del cambio inserta alguna, F1.S1.M1 no está hecho.

## Fuente de verdad (regla 97.1)
`docs/entidades/*.puml` (anotaciones `<<CK: …>>`) y `docs/Restricciones.md` (bloques `sql`) → `python3 scripts/generar_ddl.py`
→ `sql/`. Nunca se edita `sql/` a mano. Las pruebas `psql` usan `SET ROLE <svc>` desde el superusuario local: los
privilegios efectivos son los del rol de la aplicación (`application.yml` de cada servicio conecta como `svc_*`).

## Alcance
- IN: `docs/entidades/03_aportes_pagos_qr.puml`, `docs/Restricciones.md`, el `sql/` regenerado, y este directorio
  (plan, reporte, evidencia y guiones de verificación `psql`). Java solo si un cambio de la base rompe una prueba o
  un caso de uso (mínimo arreglo, en el repositorio jOOQ afectado).
- OUT (anotado, no se toca): contrato OpenAPI y tipo `Importe` (F1.L1.M01 partes 1-2, carril L1); moneda de
  `reembolso`/`disputa_pago` (F1.L1.M12, carril L1); hechos de `aportes` append-only (F1.L1.M08, carril L1);
  `clave_idempotencia` + captura 200 en reembolso/garantía/entregas/billetera (F1.L1.M09, F1.L5.M07, F1.L2.M11);
  importes de `garantia` (F1.L2.M11, depende de F2); tablas append-only de identidad/cumplimiento/grupos
  (F1.L4.M24, L3); importes de billetera que no son aporte/deducción/reembolso/movimiento.
- Ambigüedades registradas:
  - `aportes.movimiento_bancario.monto` no tiene columna de sentido: no se sabe si un débito del extracto viene con
    signo. Supuesto: no se le pone `CHECK`. Confirmar con quien diseñe la conciliación (carril L1).
  - F1.L5.M07 propone `UNIQUE (origen_tipo, origen_id)` en `asiento_contable`, pero `AsientoRepositorio.originalSinReversar`
    admite varios asientos confirmados del mismo origen (original + reversa + reemplazo). No se agrega; se anota
    para el carril L5.
- Desvío declarado: EJECUCION.md pide un PR por microtarea, y esta sesión tiene **una sola rama** autorizada. Se
  abre **un PR** para el grupo F1.S1 (comparten causa: la base no garantiza el mayor), con un commit por microtarea.

## H1 — El mayor y los importes, garantizados por la base (F1.S1)

Correspondencia de IDs: H1.S2 = F1.S1.M1 · H1.S3 = F1.S1.M2 · H1.S4 = F1.S1.M3 · H1.S5 = F1.S1.M4 · H1.S1 y H1.S6 = línea base y cierre.
**CA:** Dado el esquema regenerado aplicado sobre PostgreSQL 16, cuando el rol de la aplicación intenta cada una de
las violaciones (a)–(f), entonces la base las rechaza, y los flujos legítimos (prueba de humo, semillas de dev,
suites de `nucleo-financiero` y `aportes`) siguen pasando.
**DoD:** salidas de los guiones `psql` en `evidencia/`; prueba de humo sin `FALLA` y verificaciones en 0 filas;
regeneración idempotente (segunda corrida sin diff); verificación de deriva `puml → sql → base` en verde;
`./gradlew :servicios:nucleo-financiero:test` y `:servicios:aportes:test` (las de Testcontainers declaradas aparte).
**Estado:** A MEDIAS — H1.S4.M2 y H1.S5.M2 quedan en otros carriles; la integración Java contra la base (Testcontainers) no se corrió (ver REPORTE)

### H1.S1 — Línea base y reproducción
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Reproducir TYA-DINERO-001 en `dev` | Las 5 violaciones entran en `dev` | `psql -f evidencia/repro_tya_dinero_001.sql` → 5 asientos anómalos listados | HECHO |
| H1.S1.M2 | Línea base de regresión del esquema en `dev` | Humo sin FALLA, verificaciones 0 filas | `regresion.sh reg_base` → `OK=165 FALLA=0`, 0 filas | HECHO |

### H1.S2 — F1.S1.M1: cuadre y líneas del mayor (TYA-DINERO-001, ANF-DINERO-004)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `CHECK` de línea: `debe >= 0`, `haber >= 0` y exactamente un lado distinto de cero | Línea negativa, con dos lados o en cero → error `23514` | `evidencia/M1-mayor-despues.txt` | HECHO |
| H1.S2.M2 | Trigger inmediato: una línea solo entra a un asiento firme (`CONFIRMADO`/`REVERSADO`) creado en la misma transacción | Línea (o par cuadrado) tardía → error `R-AUD-14`; savepoints dentro de la misma transacción siguen pasando | `evidencia/M1-mayor-despues.txt` | HECHO |
| H1.S2.M3 | Cuadre diferido también desde la línea (cierra el adelanto con `SET CONSTRAINTS ALL IMMEDIATE`), y asiento firme con al menos dos líneas | Asiento firme sin líneas → error; línea descuadrante tras adelantar → error | `evidencia/M1-mayor-despues.txt` | HECHO |
| H1.S2.M4 | Consulta de cuadre por asiento en `verificaciones.sql` | La consulta devuelve las filas de los asientos anómalos de `dev` y 0 en el esquema nuevo | `evidencia/M1d-verificaciones.txt` | HECHO |

### H1.S3 — F1.S1.M2: append-only con permisos revocados
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | Guion que recorre cada tabla con trigger `*_append_only` e intenta `UPDATE` y `DELETE` con el rol dueño del esquema (y con `svc_*` en `comun`) | Toda combinación → `permission denied` (`42501`) | `psql -f evidencia/verificar_append_only.sql` → 0 filas «PERMITIDO» (1650 intentos, 97 tablas; control negativo detectado) | HECHO (ya resuelto en `dev`, sin cambio de esquema) |
| H1.S3.M2 | Cobertura de la regla 91.1.3: asiento, movimiento contable, bitácora, evento de reputación, movimiento de fondo y abono de recuperación están en la lista | Las 6 categorías presentes | `evidencia/M2-append-only.txt` | HECHO |

### H1.S4 — F1.S1.M3: importes nunca negativos (ANF-DINERO-001)
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | `<<CK>>` en el modelo: `reembolso.monto > 0`; `>= 0` en `pago.monto_comision_proveedor`, `pago.monto_neto_acreditado`, `obligacion_aporte.{monto_pagado,monto_recargo,monto_condonado,monto_cubierto_garantia}`, `disputa_pago.monto_disputado`, `orden_cobro.monto_exacto` | `INSERT`/`UPDATE` negativo con `svc_aportes` → `23514` en cada columna | `evidencia/M3-importes-antes.txt` (18/29) y `M3-importes-despues.txt` (29/29, re-aplicado sobre base existente) | HECHO |
| H1.S4.M2 | Respuesta 422 de la API ante reembolso negativo | — | — | TODO (carril L1, F1.L1.M01 partes 1-2: contrato y tipo `Importe`) |

### H1.S5 — F1.S1.M4: unicidad de `clave_idempotencia`
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S5.M1 | Inventario: toda columna `clave_idempotencia` tiene índice `UNIQUE` nombrado, y la violación se demuestra en las tablas de dinero | 0 columnas sin `UNIQUE`; `23505` en el segundo `INSERT` | `evidencia/M4a-idempotencia.txt` (10 tablas ejercitadas, 2 sin filas) | HECHO (ya resuelto en `dev`, sin cambio de esquema) |
| H1.S5.M2 | `clave_idempotencia` en las tablas de dinero que no la tienen (`reembolso`, `movimiento_fondo`, `abono_recuperacion`, `retencion_saldo`, `devolucion_comision`, `deduccion_entrega`) + captura como 200 con el mismo `id` | — | — | TODO (carriles L1/L2/L5: F1.L1.M09, F1.L2.M11, F1.L5.M07; exige cambiar contrato y caso de uso) |

### H1.S6 — Regresión y cierre
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S6.M1 | Regeneración idempotente y deriva | `generar_ddl.py` dos veces sin diff; columnas/constraints del modelo presentes en la base | `evidencia/M5a-deriva.txt` (0 problemas; control negativo con 3) | HECHO |
| H1.S6.M2 | Regresión del esquema | Humo `FALLA=0` y sin errores nuevos; verificaciones 0 filas; semillas dev cargan | `evidencia/M5b-regresion.txt` | HECHO |
| H1.S6.M3 | Suites Gradle | `:servicios:nucleo-financiero:test` y `:servicios:aportes:test` sin fallos nuevos | `evidencia/M5c-*.txt`: 94/94 y 53/53 (corredor `test`; el corredor `integrationTest` exige Docker) | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Un flujo legítimo agrega líneas a un asiento firme en otra transacción | Se rompería el registro contable | Revisado: `AsientoRepositorio` y los fixtures insertan cabecera y líneas en la misma transacción; sin savepoints en `nucleo-financiero` |
| Pruebas de Testcontainers no corren acá (sin Docker) | No se ejecuta la integración Java contra la base | Las invariantes se prueban con `psql`; comando exacto para la máquina de Pablo en el REPORTE |
| Datos existentes en TEST que violen los CHECK nuevos | `aplicar.sql` fallaría al reaplicar | La consulta de `verificaciones.sql` y los guiones detectan filas previas; corrección por contra-asiento, nunca `UPDATE` |
