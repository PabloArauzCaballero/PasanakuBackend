---
name: money-movement-safety
description: Gate duro de toda operación que mueve dinero en Pasanaku — representación exacta del importe y su moneda, idempotencia de punta a punta con clave única en base, inmutabilidad del mayor y corrección por contra-asiento, reversas y devoluciones como operaciones de negocio, límites y doble control en operaciones grandes, redondeo con residuo asignado, ventanas de enfriamiento, y las pruebas que hay que ejecutar antes de decir que está hecho. Usar antes de escribir, revisar o cerrar cualquier código que calcule, prometa, acredite, deduzca, desembolse, condone o reverse un importe.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Seguridad del movimiento de dinero

Este es el gate más duro del catálogo. Aplica **aunque nadie lo haya pedido** en todo PR que
toque un importe, incluido un endpoint de lectura que lo muestra: un saldo mal calculado en
pantalla es una promesa falsa con la firma de la empresa.

Las prohibiciones están en la **regla 91**. Acá está cómo se cumplen.

## 1. El importe

```ts
// ❌ jamás
const total: number = 350.75;            // float: pierde centavos al acumular
const total = a + b;                     // sin moneda: sumás Bs con USD sin enterarte

// ✅ decimal exacto + moneda, juntos e inseparables
interface Importe { readonly monto: Decimal; readonly moneda: 'BOB' | 'USD'; }
```

- Base: `DECIMAL(14,2)` (`16,2` para acumulados) + `moneda CHAR(3)` ISO-4217, siempre al lado.
- Código: decimal exacto o entero de centavos. **Nunca** `number` de JavaScript para dinero.
- JSON entre servicios: string decimal (`"350.75"`), no `number` — `JSON.parse` lo convierte en
  float y ya perdiste (`service-contracts-versioning`).
- Prohibido sumar monedas distintas. Si hay conversión, se registra tasa, fuente y fecha.
- **Redondeo**: modo definido una vez para toda la casa (medio-par), aplicado en un solo lugar.
  Cuando una división no es exacta, **el residuo se asigna**, no se pierde:

```ts
// Repartir 1000,00 entre 3 cupos: 333,33 + 333,33 + 333,34 = 1000,00
// La última línea absorbe el residuo. Documentá cuál es "la última" y por qué.
```

## 2. Idempotencia de punta a punta

| Capa | Mecanismo |
|---|---|
| API | Cabecera `Idempotency-Key`; misma clave ⇒ misma respuesta, sin repetir el efecto |
| Base | Índice `UNIQUE` sobre `clave_idempotencia`. **Nunca** un `SELECT` previo: entre el select y el insert entra el duplicado |
| Consumidor de eventos | Deduplicación transaccional por `message_id` (`async-messaging-events`) |
| Proveedor externo | Se le pasa **nuestra** clave; un reintento reusa la misma |
| Saga | Una clave por saga, propagada a todos los pasos (`saga-distributed-transactions`) |

```ts
// ✅ el duplicado se detecta por constraint, dentro de la transacción
try {
  await em.transactional(async (tx) => {
    tx.persist(new Pago({ claveIdempotencia, monto, moneda }));
    tx.persist(asientoDe(pago));
  });
} catch (e) {
  if (esViolacionUnica(e, 'uq_pago_clave_idem')) return pagoExistente(claveIdempotencia); // 200
  throw e;
}
```

La clave la genera **quien origina la intención** (el cliente al tocar "pagar", el planificador
al emitir la orden), no cada salto. Reintento ⇒ misma clave. Intención nueva ⇒ clave nueva.

## 3. El mayor no se edita

- `asiento_contable`, `movimiento_contable`, `movimiento_fondo`, `abono_recuperacion`,
  `bitacora_evento` y `evento_reputacion` son **append-only**, con `UPDATE`/`DELETE` revocados a
  nivel de rol de base (regla 91.2.5).
- Corrección = **contra-asiento** que referencia al original, con motivo y actor. El original
  queda visible. Quien audite ve el error y su corrección: eso es lo correcto, no una vergüenza.
- Un `UPDATE` sobre el mayor en un PR es un bloqueante automático en revisión
  (`code-review-standard`).

## 4. Reversas, reembolsos y devoluciones — no son lo mismo

| Operación | Cuándo | Qué genera |
|---|---|---|
| **Reversa contable** | El asiento estaba mal | Contra-asiento; el dinero no se movió |
| **Reembolso** (`reembolso`) | El participante pagó de más o por error, antes de entregar | Devolución al pagador + asientos |
| **Devolución de fondo** (`devolucion_fondo`) | Sale del fondo de garantía por resolución | Movimiento de fondo + asientos + expediente |
| **Disputa** (`disputa_pago`) | El participante desconoce un cargo | Expediente con debido proceso (`dispute-resolution`) |

Cada una tiene su entidad, su ciclo de estados y su autorización. **Prohibido resolver cualquiera
de ellas con un ajuste directo de saldo.**

## 5. Límites, doble control y enfriamiento

| Control | Dónde aplica |
|---|---|
| **Límite por operación y por día** | Desembolsos, devoluciones, condonaciones |
| **Doble control (cuatro ojos)** | Toda operación manual sobre dinero por encima del umbral: la pide un actor, la aprueba otro, y el sistema lo impone |
| **Ventana de enfriamiento** | Cambio de cuenta bancaria del beneficiario antes de un desembolso (`disbursement-payouts`) |
| **Lista de verificación previa** | `validacion_pre_entrega` antes de liquidar |

El doble control **no es una pantalla**: es una restricción de datos. `aprobado_por` distinto de
`solicitado_por`, verificado por constraint o por regla en la escritura, no por la UI.

## 6. Concurrencia

- Lectura-y-luego-escritura sobre un saldo ⇒ locking (`concurrency-and-locking`). El clásico:
  dos desembolsos simultáneos del mismo fondo, los dos leen saldo suficiente.
- Preferí operaciones que la base pueda resolver atómicamente (`INSERT` de movimiento + saldo
  calculado) sobre leer-modificar-escribir un campo `saldo`.
- Un campo `saldo` materializado es una caché: se recalcula desde el mayor y se verifica en el
  cierre (`distributed-data-integrity`).

## 7. Lo que hay que probar antes de decir que está hecho

Regla 91.6. Sin estas cinco salidas pegadas, el peldaño es `TESTED`, no `VERIFIED`:

1. **Asientos generados**, con débitos, créditos y su suma.
2. **Doble ejecución**: la misma operación disparada dos veces con la misma clave ⇒ un solo
   efecto, con la consulta de verificación pegada.
3. **Cuadre** contra el mayor.
4. **Reversa** ejercitada, mostrando el contra-asiento.
5. **Redondeo**: el caso que no divide exacto, mostrando dónde quedó el residuo.

Y si cruza servicios, además lo de la regla 98.8 (`microservices-testing`).

## Anti-patrones

- `float`/`number` para dinero, en cualquier capa, "porque es solo para mostrar".
- Importe sin moneda al lado.
- `SELECT` previo como control de duplicados.
- Clave de idempotencia generada en cada reintento.
- `UPDATE saldo = saldo - x` sin lock ni movimiento registrado.
- Corregir con `UPDATE` sobre el mayor.
- Doble control implementado solo en el frontend.
- Residuo de redondeo perdido, o "se ajusta en el cierre".
- Mostrar `0` cuando el servicio de contabilidad no respondió (`resilience-patterns`).
- Cerrar con "los tests pasan" sin las cinco evidencias.

## Checklist

- [ ] Todo importe es decimal exacto y viaja con su moneda, en base, código y contrato.
- [ ] Redondeo con modo único y residuo asignado explícitamente.
- [ ] `clave_idempotencia` con índice `UNIQUE`, y la clave se origina una sola vez.
- [ ] El efecto y su asiento ocurren en la misma transacción.
- [ ] Nada edita ni borra tablas append-only; permisos revocados verificados.
- [ ] Reversa / reembolso / devolución / disputa usan su entidad y su ciclo.
- [ ] Límites, doble control y enfriamiento aplicados donde corresponde, en el servidor.
- [ ] Concurrencia resuelta con lock o con operación atómica.
- [ ] Las cinco evidencias de la regla 91.6, ejecutadas y pegadas.

## Evidencia / DoD

1. Asientos de la operación, con `SUM(debe)` y `SUM(haber)` iguales.
2. Salida de la doble ejecución con la misma clave: efecto único.
3. Consulta de cuadre contra el mayor.
4. Contra-asiento de la reversa.
5. Caso de redondeo no exacto, con el destino del residuo.
