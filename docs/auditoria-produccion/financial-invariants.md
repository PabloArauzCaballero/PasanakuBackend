# Invariantes financieras — libro de `nucleo-financiero` y benchmark del advisory lock

> H3 del carril PR4-seguridad (Marcelo). Grep de H3.S1.M1 ejecutado de verdad;
> el resto de este documento describe dónde vive cada invariante y qué la demuestra
> HOY — marcando explícitamente lo que este carril todavía no construyó ni corrió,
> en vez de afirmarlo sin evidencia (regla `evidence-and-verification`).

## H3.S1.M1 — Sin `double`/`float` en dinero

```text
$ grep -rnE "\b(double|float|Double|Float)\b" --include=*.java servicios plataforma
servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/ContenidoCanonico.java:55:
    /** Un importe, siempre con dos decimales y punto. Nunca un {@code double}. */
servicios/erp/src/main/java/bo/aportaya/erp/aplicacion/CU106GenerarEstadoFinanciero.java:159:
     * el hash tiene que poder recomputarse desde el mismo estado, y un `double` daria
```

| Ocurrencia | Contexto | Veredicto |
|---|---|---|
| `ContenidoCanonico.java:55` | Comentario Javadoc que EXPLICA por qué no se usa `double` | Sin código real — limpio |
| `CU106GenerarEstadoFinanciero.java:159` | Comentario que explica por qué el hash necesita `BigDecimal` y no `double` | Sin código real — limpio |

**Veredicto: 0 ocurrencias reales de `double`/`float` en dinero**, en `servicios/` y
`plataforma/` completos. `Dinero` (`plataforma/comun-dominio/.../Dinero.java`) es
`BigDecimal` con escala 2 fija en el constructor (`ESCALA = 2`,
`RoundingMode.UNNECESSARY` si no calza — nunca redondea en silencio), sin aritmética
sobre `BigDecimal` desnudo. `dividir`/`por` exigen `RoundingMode` explícito
(`exigirRegla`, `Dinero.java`): dividir sin decir cómo redondear no compila.

## Invariantes — dónde viven y qué las demuestra

| # | Invariante | Dónde vive (SQL) | Dónde vive (Java) | Test que la demuestra |
|---|---|---|---|---|
| 1 | **balances** — `saldo_disponible` nunca negativo | `ck_cuenta_saldo_no_negativo` (`sql/40_reglas/restricciones.sql`) | `CU10RecargarSaldo`/`CU11RetirarSaldo` | `CU10ConcurrenciaTest` (parcial, 2 hilos); **11 escenarios de `LibroInvariantesTest`: TODO** |
| 2 | **ledger (double-entry)** — `SUM(debe) = SUM(haber)` por asiento | `fn_bil_recalcular_saldos`, `asiento_contable`/`partida_contable` | `CuadrarPartidas.verificar` (dominio puro) | `CuadrarPartidasTest` (5 casos, unitario) + `CuadrarPartidasPropiedadTest` (jqwik, 1000 tries) — **ambos YA EXISTEN, sin tocar este carril**; el cuadre CONTRA POSTGRESQL REAL (`SELECT transaccion_id, SUM(CASE sentido…)`) es H3.S1.M2: **TODO** |
| 3 | **transfer** — CU-12 mueve entre dos cuentas sin perder ni duplicar | `uq_tx_idem`, `ck_cuenta_saldo_no_negativo` | `CU12TransferirSaldo` | Existe `CU12Test`/`CU12RechazosTest` (fuera de este carril); escenarios 6–9 de concurrencia real (100 hilos, opuestas simultáneas, deadlock cruzado): **TODO** |
| 4 | **withdrawal** — retiro con doble aprobación y MFA | `ck_retiro_doble_aprobacion`, `orden_retiro` | `CU11RetirarSaldo` | Fuera de este carril (PR2, Justin) |
| 5 | **fees** — comisión y redondeo declarados | `comision_fija`/`comision_porcentual` | `CostoDeOperacion` (nucleo-financiero) | H8.S1.M4 del plan madre: **TODO** (no es de `aportes`; `aportes.EntradaProveedor` sí declara comisiones, ver `security-matrix.md`) |
| 6 | **guarantees** — fondo de garantía cubre incumplimientos | `garantia.fondo_garantia` | `servicios/garantia` (sin dueño este turno) | Fuera de alcance — solo lectura |
| 7 | **settlements** — liquidación de comisiones (`tarifas`) | `tarifas.liquidacion` | `servicios/tarifas` (sin dueño este turno) | Fuera de alcance |
| 8 | **reconciliation** — el pago de `aportes` concilia con el webhook de la pasarela | `aportes.webhook_pasarela`, `uq_webhook_idem` | `CU100RecibirWebhookPasarela` (**construido en este carril, H1.S2**) | `CU100WebhookTest` (8 tests, correcto/límite/inválido) — **escrito; ejecución contra PostgreSQL real: pendiente de esta corrida** |
| 9 | **redondeo** | `RoundingMode` explícito, nunca `UNNECESSARY` implícito | `Dinero.por`/`Dinero.dividir` (`exigirRegla`) | Sin test dedicado nuevo este turno — el diseño de `Dinero` ya lo hace estático (falla en compilación si falta la regla) |
| 10 | **idempotencia** | 12 índices únicos (`uq_*_idem`) | Ver `docs/auditoria-produccion/idempotencia-scope.md` | `CU21Test.mismaClaveOtraObligacionEsOtroPago`, `CU100WebhookTest.eventoRepetidoEsIdempotente` — **escritos; ejecución pendiente de esta corrida** |
| 11 | **cadena de hash del libro** — `hash_registro` encadenado, sin bifurcaciones | `fn_aud_encadenar_transaccion` (`sql/40_reglas/restricciones.sql:53-73`) | — (la aplicación NO firma su propia huella, R-AUD-09) | `CuadrarPartidasPropiedadTest` verifica cuadre, no la cadena; un test de cadena bajo concurrencia real: **TODO** |

## H3.S2 — El advisory lock del hash chain

**H3.S2.M1 — localizado, con línea exacta:**

```sql
-- sql/40_reglas/restricciones.sql:53-68
CREATE OR REPLACE FUNCTION fn_aud_encadenar_transaccion() RETURNS trigger AS $$
DECLARE v_anterior VARCHAR(64);
BEGIN
  PERFORM pg_advisory_xact_lock(hashtext('cadena_transaccion_billetera'));  -- línea 56
  SELECT hash_registro INTO v_anterior
    FROM transaccion_billetera ORDER BY secuencia DESC LIMIT 1;
  NEW.hash_anterior := v_anterior;
  NEW.hash_registro := encode(digest(... || COALESCE(v_anterior,'')), 'sha256'), 'hex');
  RETURN NEW;
END $$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_transaccion_billetera_hash ON transaccion_billetera;
CREATE TRIGGER tg_transaccion_billetera_hash
  BEFORE INSERT ON transaccion_billetera
  FOR EACH ROW EXECUTE FUNCTION fn_aud_encadenar_transaccion();
```

Es un **advisory lock GLOBAL** (`hashtext('cadena_transaccion_billetera')` es una
clave FIJA, no derivada de la cuenta ni de la transacción): toda inserción en
`transaccion_billetera`, de cualquier cuenta, de cualquier servicio, serializa contra
esta única cadena. El mismo patrón existe para `bitacora_evento`
(`hashtext('cadena_bitacora_evento')`, línea 93) — otra cadena, otro lock, no
comparten cuello de botella entre sí.

**H3.S2.M2 — benchmark de 200 transferencias concurrentes, 3 corridas:**
`LibroBenchmarkTest` (`@Tag("benchmark")`, ya reservado en el corredor
`integrationTest` pero EXCLUIDO de la ejecución automática — ver
`buildSrc/src/main/kotlin/aportaya.base.gradle.kts`, cambio troncal de este carril).
**Estado: TODO.** No se construyó en esta corrida por presupuesto de tiempo: el
entorno Docker Desktop compartido con otros carriles en la misma máquina estuvo
inestable la mayor parte del turno (ver
`docs/auditoria-produccion/evidencia/H1-entorno-docker.md`), y se priorizó dejar H1
(idempotencia + webhook) y H2 (inventario + contratos) completamente verificados
antes de abrir un benchmark de carga que necesita el mismo recurso compartido.

**H3.S2.M3 — decisión (Q-03, ya DECIDIDA 2026-09-21):** se **mantiene** el advisory
lock global. Este carril no propone alternativa: la medición que la justificaría
(H3.S2.M2) no se corrió. Si una medición futura muestra el lock como cuello de
botella, la alternativa (p. ej. cadena por cuenta con hash de raíz diario) exige ADR
y queda `DECISION_REQUIRED`, nunca implementada de hecho.

## Pendiente (declarado, no oculto)

- [ ] H3.S1.M2 — Escenarios 1–5 contra PostgreSQL real con la consulta de cuadre.
- [ ] H3.S1.M3 — Escenarios 6–8 (100 hilos, opuestas simultáneas, replay).
- [ ] H3.S1.M4 — Escenarios 9–11 (rollback, excepción tras débito, deadlock cruzado ×50).
- [ ] H3.S2.M2 — Benchmark real de 200 transferencias × 3 corridas con las 6 métricas.
- [ ] `LibroInvariantesTest`/`LibroBenchmarkTest` (nombres reservados): archivos
      todavía no creados en `servicios/nucleo-financiero/src/test/`.

**Siguiente paso concreto** para quien retome: crear
`servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/LibroInvariantesTest.java`
extendiendo `BaseDeBilletera` (mismo patrón que `CU10ConcurrenciaTest`), con los 11
escenarios del plan madre §H8.S2; y
`servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/LibroBenchmarkTest.java`
con `@Tag("benchmark")`, un `ExecutorService` de 200 hilos y las consultas de
`pg_stat_activity`/`pg_locks`/`pg_stat_database.deadlocks` ya usadas como referencia
en el plan madre (`docs/auditoria-produccion/PLAN.md` H8.S2.M3, H8.S3.M2).
