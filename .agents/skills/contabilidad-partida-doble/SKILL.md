---
name: contabilidad-partida-doble
description: "Mover dinero en AportaYa: libro de billetera con partida doble, asiento contable, custodia y encaje, y cierre diario. Úsala en cualquier flujo que acredite, debite, retenga, reverse o cobre —recarga, aporte, entrega, comisión, retiro, devolución— y cuando aparezca un descuadre o un saldo que no coincide. Explica por qué el saldo no se escribe nunca."
---

# Mover dinero

El dinero de AportaYa se registra **tres veces, para tres lectores distintos**, y
las tres tienen que cuadrar:

| Libro | Tablas | Para quién |
| --- | --- | --- |
| **Libro de billetera** | `transaccion_billetera` + `movimiento_billetera` | El usuario y el soporte: qué pasó en cada cuenta |
| **Contabilidad** | `asiento_contable` + `movimiento_contable` + `cuenta_contable` | El auditor y el contador |
| **Custodia** | `cuenta_custodia` + `movimiento_custodia` + `conciliacion_custodia` | El supervisor: que el dinero real exista en el banco |

Si un flujo toca uno solo de los tres, está incompleto.

## La regla que no admite excepción

**El saldo no se escribe: se deriva.**

```sql
-- PROHIBIDO, en cualquier variante
UPDATE cuenta_billetera SET saldo_disponible = saldo_disponible - 150.00 WHERE id = $1;
```

Se insertan movimientos con contrapartida y el trigger
`tg_movimiento_sincroniza_saldo` (`R-BIL-16`) sincroniza la caché de saldo dentro
de la misma transacción. `cuenta_billetera.saldo_*` es **caché**, no verdad: la
verdad es la suma del libro, y hay una consulta de verificación que compara ambas
y debe devolver cero filas.

## Anatomía de un movimiento de dinero

```
transaccion_billetera          una por hecho económico
  clave_idempotencia  UNIQUE   ← el reintento no crea otra
  tipo, origen_tipo, origen_id ← de dónde vino (pago, orden_recarga, cargo_comision…)
  hash_registro / hash_anterior← encadenado, append-only
  └── movimiento_billetera     dos o más, suma cero
        sentido  DEBITO | CREDITO
        monto    > 0             ← el signo lo da el sentido, nunca el monto
        saldo_disponible_posterior, saldo_retenido_posterior
```

`SUM(débitos) = SUM(créditos)` en cada transacción, verificado por un
`CONSTRAINT TRIGGER … DEFERRABLE INITIALLY DEFERRED`. Diferido a propósito: la
primera fila insertada siempre descuadra.

**El monto siempre es positivo.** Un `monto` negativo significa que alguien no
entendió el modelo, y la base lo rechaza (`CHECK monto > 0`).

## Toda salida tiene contrapartida

No existe el crédito que aparece de la nada. Si se acredita saldo a un usuario, se
debita **alguna** cuenta: la de custodia, una cuenta técnica de plataforma, o la
cuenta de otro usuario. Por eso existen cuentas técnicas cuyo saldo disponible
puede ser negativo, y por eso el `CHECK saldo >= 0` es **condicional al tipo de
cuenta**, no universal.

| Flujo | Debe | Haber |
| --- | --- | --- |
| Recarga | Custodia / recaudador | Saldo del usuario (pasivo exigible) |
| Aporte al grupo | Saldo del participante | Cuenta de billetera del grupo |
| Entrega del fondo | Cuenta del grupo | Saldo del beneficiario |
| Comisión | Saldo del pagador | Ingreso por comisiones |
| Retiro | Saldo del usuario | Custodia / banco |

## Corregir: nunca editar

Las tablas de dinero son *append-only* (`REVOKE UPDATE, DELETE` + trigger). Una
corrección es **un movimiento nuevo en sentido inverso**, enlazado por
`reverso_transaccion` con su motivo. El histórico conserva el error y su
corrección: eso es exactamente lo que un auditor viene a leer.

| Situación | Qué se hace |
| --- | --- |
| Acreditación duplicada | Reverso con motivo, más `evento_riesgo_operativo` |
| Monto equivocado | Reverso total y transacción nueva por el importe correcto |
| Comisión mal cobrada | `devolucion_comision` + nota de crédito (skill `facturacion-sin`) |
| Glosa mal escrita | Se deja. No justifica tocar el libro |

## Retención no es débito

Retener (`retencion_saldo`) mueve saldo de *disponible* a *retenido*: el total no
cambia. Confundirlo con un débito infla el pasivo y rompe el encaje. Se usa para
garantía, disputa y bloqueo preventivo.

## Custodia y encaje

Cada día, `conciliacion_custodia` compara el dinero electrónico emitido contra lo
que hay en las cuentas de custodia:

```
diferencia      = saldo_custodia − saldo_dinero_electronico   (GENERATED)
ratio_cobertura = saldo_custodia / saldo_dinero_electronico   (GENERATED)
cumple_encaje   = ratio_cobertura >= 1
```

Si no cumple, se abre `descuadre_custodia` con severidad y plan de acción, y —si
hubo pérdida— `evento_riesgo_operativo`. **Un descuadre no explicado no se
cierra**: se explica o se escala.

## Cierre diario

`cierre_diario` sella la fecha. No cierra si hay excepciones de conciliación
abiertas, asientos sin confirmar o encaje incumplido (`R-BIL-12`). Al cerrar,
`saldo_diario_billetera` guarda los saldos encadenados por hash. Reabrir exige
autorización y deja `reabierto_en` con el motivo.

## Checklist de un flujo con dinero

- [ ] Una sola transacción de base para todo el hecho económico.
- [ ] `clave_idempotencia` validada **antes** de la primera escritura.
- [ ] Los movimientos suman cero y todos tienen `monto > 0`.
- [ ] Existe el `asiento_contable` correspondiente, equilibrado.
- [ ] Si el dinero entra o sale del sistema, hay `movimiento_custodia`.
- [ ] Ningún `UPDATE` sobre saldo: solo inserciones y el trigger.
- [ ] La corrección prevista es un reverso, no una edición.
- [ ] Hay prueba de cuadre: `SUM(monto por sentido)` de la transacción da cero.
- [ ] Las consultas de verificación de [[Restricciones]] devuelven cero filas.

## Ver también

`dinero-decimal` · `idempotencia-reintentos` · `restriccion` · `qr-pagos` ·
`pruebas-cu` · `docs/Arquitectura/Flujo de una transacción.md` ·
familias `R-BIL` y `R-AUD` de `docs/Restricciones.md`
