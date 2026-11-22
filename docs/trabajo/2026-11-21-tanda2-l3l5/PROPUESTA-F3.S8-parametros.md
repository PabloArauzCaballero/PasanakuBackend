# Propuesta de modelo — Registro central de parámetros financieros (F3.S8.M2 y M6)

- Estado: **PROPUESTA, sin código.** Se valida contra el contrato que L2 deja en
  `docs/trabajo/2026-11-21-tanda2-l2/` (lo que `garantia` necesita consumir) antes de tocar el `.puml`.
- Dueño propuesto: servicio `tarifas` (decisión de coordinación, **a confirmar con Pablo**).
- Fuentes: DECISIONES tandas 1 («configurables y obligatorios, sin valor por defecto»), 6 («editables desde el
  backoffice; ninguno fijo en código ni en archivo de configuración»), 7 («rigen solo para grupos nuevos; cada grupo
  congela sus parámetros al iniciar»; «dos personas: una propone, otra distinta aprueba, ambas del token»);
  ADR-056 §6; reglas 91, 97.4.5 y 97.5.4.

## Lo que ya existe y no se duplica (regla 97.4.6)

| Existente | Qué es | Relación con el registro |
|---|---|---|
| `tarifas.regla_tarifa` | Regla de cálculo de una comisión por concepto, con vigencia | La **comisión** (D-L5-1: porcentaje único) se expresa como `regla_tarifa`; el registro **no** guarda otra copia de la comisión: la referencia. A confirmar con L2/F3.L5.M36 |
| `aportes.politica_mora`, `garantia.politica_cobertura` | Política **por grupo**, con valores fijos | Pasan a ser el **congelamiento** de cada servicio dueño: se llenan al iniciar el grupo desde el registro, no desde valores escritos a mano. Su estructura no la toca este lote (son de L1/L2) |
| `tarifas.politica_redondeo` | Unidad mínima y modo por moneda | Fuera del registro (no es un parámetro de negocio del grupo) |

## Tablas propuestas (esquema `tarifas`)

1. `parametro_financiero` — **catálogo cerrado de códigos** (concepto codificado, regla 97.4.7): `codigo` UNIQUE,
   `descripcion`, `unidad` (`PORCENTAJE` | `DIAS` | `MONTO` | `CONTEO`), `moneda CHAR(3)` cuando la unidad es
   `MONTO`, `servicio_consumidor`. Códigos: solo los que nombran DECISIONES (aporte al fondo del grupo, días de
   gracia, tasa de recargo, umbral de disolución por mora, días para cancelar un grupo inactivo, comisión, reducción
   de aporte por «Grupo perfecto»). La lista final la cierra el contrato de L2. **Sin valores.**
2. `version_parametro` — append-only: `parametro_id`, `valor DECIMAL(16,6)` con `CHECK >= 0`, `vigente_desde`,
   `procedencia` (fuente, referencia, fecha — regla 97.4.2), `propuesto_por`, `propuesto_en`, `aprobado_por`,
   `aprobado_en`, `estado` (`PROPUESTA` | `APROBADA` | `RECHAZADA`), `clave_idempotencia` UNIQUE.
   - `CHECK (aprobado_por IS NULL OR aprobado_por <> propuesto_por)` — cuatro ojos en la base, además del token.
   - Aprobar es una transición `PROPUESTA → APROBADA` con la precondición en el `UPDATE`; corregir es una versión
     nueva, nunca un `UPDATE` del valor (`REVOKE UPDATE (valor, vigente_desde)` al rol de la aplicación).
3. `congelamiento_grupo` — append-only: `grupo_id`, `parametro_id`, `version_parametro_id`, `congelado_en`;
   `UNIQUE (grupo_id, parametro_id)`. Se escribe una vez, al iniciar el grupo (evento `grupo_iniciado` por
   outbox, consumidor idempotente por `message_id`). Un grupo en curso nunca ve una versión posterior.
4. Bitácora: la de plataforma (`comun.bitacora_evento`) por cada propuesta, aprobación y congelamiento.

## Contrato propuesto (a alinear con L2)

- `GET /tarifas/parametros/vigentes` → las versiones aprobadas vigentes, para grupos nuevos.
- `GET /tarifas/parametros/grupos/{grupoId}` → lo congelado de ese grupo. Si falta un parámetro: **422 con el código
  que falta** («la operación falla con un error explícito», F3.S8.M2), nunca un valor por omisión.
- `POST /tarifas/parametros/{codigo}/versiones` (propone, `Idempotency-Key`) y
  `POST /tarifas/parametros/versiones/{id}/aprobacion` (aprueba otra persona; actores de `CuatroOjos`, #412).

## Microtareas (en PLAN.md, después de #448)

| ID | Qué | DoD |
|---|---|---|
| F3.S8.M2a | Modelo en `11_tarifas_comisiones.puml` + filas de `tarifas` en `docs/Restricciones.md`, alineado al contrato de L2 | `generar_ddl.py` + aplicar en PostgreSQL local + deriva + `psql` que viole cada `CHECK` (PR chico) |
| F3.S8.M2b | Casos de uso: proponer, aprobar (cuatro ojos del token), consultar vigentes; falla explícita si falta | unitarias + web con matriz negativa |
| F3.S8.M6 | Congelamiento al iniciar el grupo (consumidor de `grupo_iniciado`) y consulta por grupo | prueba de duplicado del evento + `psql` |
| F3.S8.M4 | Pantalla del backoffice | prueba visual 3 viewports × 2 temas + E2E |

## Preguntas abiertas
- Confirmar con Pablo que el dueño es `tarifas`.
- Lista cerrada de códigos y unidades: la fija el contrato de L2 y DECISIONES; los **valores** son F3.S8.M3
  (`DECISION_REQUIRED`, Pablo y Cumplimiento).
