---
name: distributed-data-integrity
description: Cómo se mantiene la integridad del dinero cuando el estado está repartido entre servicios — invariantes globales que ninguna base puede imponer sola, conciliación interna periódica entre pagos, contabilidad, entregas y garantía, sumas de control y cuadre automatizado, detección de deriva y de registros huérfanos, qué hacer cuando dos servicios discrepan (nunca "emparejar" a mano), y el cierre diario como control transversal. Usar al diseñar un flujo que reparte una invariante de dinero entre servicios, al escribir un chequeo de cuadre, cuando un total no coincide entre dos pantallas, o al investigar una diferencia contable.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Integridad de datos distribuida

En un monolito, un `CHECK` y una transacción defienden la invariante. Repartido en seis
servicios, **ninguna base puede ver la invariante completa**. Si nadie la verifica a posteriori,
la deriva ocurre y se descubre el día que un participante reclama.

Esta skill es el control compensatorio: **lo que no se puede imponer, se concilia**.

## 1. Invariantes que cruzan servicios

Escribilas explícitamente; son el contrato real del sistema:

| Invariante | Servicios | Cómo se verifica |
|---|---|---|
| Todo pago acreditado tiene exactamente un asiento balanceado | `pagos` ↔ `contabilidad` | Conteo y suma por periodo |
| La suma de aportes acreditados de un periodo = bolsa bruta de la entrega | `pagos` ↔ `entregas` | Cuadre por grupo y periodo |
| Toda deducción aplicada tiene una deuda o cobertura que la respalda | `entregas` ↔ `garantia` | Join lógico por conciliación |
| Toda cobertura del fondo tiene su subrogación registrada | `garantia` ↔ `contabilidad` | Saldo del fondo vs mayor |
| Todo cupo con obligación vencida tiene expediente o está cubierto | `grupos` ↔ `garantia` | Barrido diario |
| El total del panel de transparencia = saldo del mayor | `confianza` ↔ `contabilidad` | Comparación en el cierre |

**La fuente de verdad es siempre el mayor** (regla 91.1.5). Cuando dos números difieren, el que
manda es el de `contabilidad`; el otro es el que está mal.

## 2. Conciliación interna: el chequeo que no se saltea

Distinta de la conciliación bancaria (`payment-reconciliation`, que compara contra el banco).
Esta compara **tus propios servicios entre sí**.

```sql
-- Ejemplo: pagos acreditados sin asiento, por periodo. Corre en el servicio de auditoría,
-- sobre modelos de lectura alimentados por evento — NUNCA contra las bases ajenas.
SELECT p.periodo_id, COUNT(*) AS pagos, SUM(p.monto) AS suma_pagos,
       COUNT(a.id)  AS asientos, COALESCE(SUM(a.monto),0) AS suma_asientos
FROM proy_pago_acreditado p
LEFT JOIN proy_asiento a ON a.origen_id = p.id
GROUP BY p.periodo_id
HAVING COUNT(*) <> COUNT(a.id) OR SUM(p.monto) <> COALESCE(SUM(a.monto),0);
```

Reglas:

1. Corre **automáticamente** (diario como mínimo; por periodo en los grupos activos).
2. Cualquier fila devuelta es una **alerta**, no una línea de log. Con dueño y runbook.
3. El resultado se guarda: una serie histórica muestra si la deriva crece.
4. Se apoya en modelos de lectura poblados por eventos (`eventual-consistency-read-models`), no
   en acceso cruzado a bases (regla 98.1.1).

## 3. Qué buscar

| Patrón | Qué significa | Causa típica |
|---|---|---|
| **Huérfano** | Un lado tiene el registro, el otro no | Evento perdido: outbox mal usado |
| **Duplicado** | Dos efectos para un hecho | Consumidor no idempotente |
| **Descuadre de importe** | Los conteos coinciden, las sumas no | Redondeo distinto en dos lados, o cambio de semántica |
| **Estado incoherente** | Obligación "cumplida" sin pago acreditado | Saga que compensó a medias |
| **Deriva creciente** | La diferencia aumenta con el tiempo | Proyección atrasada o bug sistemático |
| **Cola muerta con dinero** | Mensajes en DLQ de un tópico de pagos | Nadie mira la DLQ |

El descuadre de importe con conteos iguales es el más peligroso: **nada falla, todo suma mal**.
Suele ser un cambio de semántica que pasó sin versionar (`service-contracts-versioning` §1).

## 4. Cuando dos servicios discrepan

Procedimiento, en este orden. **Prohibido empezar por el paso 4.**

1. **Congelar el criterio**: la fuente de verdad es el mayor. Anotar la diferencia exacta, con
   la consulta que la produjo.
2. **Encontrar la causa**: buscar por `correlationId` los eventos de las operaciones afectadas
   (`distributed-tracing-correlation`). La causa está en el flujo, no en la fila.
3. **Arreglar la causa** y desplegarlo. Si no, la corrección se vuelve a desviar mañana.
4. **Corregir el dato**: con una operación de negocio auditada —contra-asiento, reproceso del
   evento, reconstrucción de la proyección— **nunca con un `UPDATE` manual** (regla 91.1.3).
5. **Registrar**: el incidente, la diferencia, la causa, la corrección y cómo se evita
   (`incident-response-postmortem`).

> [!warning] "Emparejar a mano" es la peor decisión posible.
> Un `UPDATE` que iguala dos números destruye la evidencia de la causa, deja el bug vivo y rompe
> la cadena de auditoría. Si el rol de base tiene los permisos revocados como manda la regla
> 91.2.5, ni siquiera vas a poder hacerlo — y está bien que sea así.

## 5. El cierre diario como control transversal

`cierre_diario` es el momento en que el sistema declara, por escrito, que el día cuadra
(`financial-close-reporting`):

- Suma de asientos del día = suma de movimientos, por cuenta.
- Acreditaciones del día = confirmaciones de la pasarela conciliadas.
- Entregas del día = desembolsos ordenados = deducciones aplicadas + neto.
- Saldo del fondo de garantía = mayor del fondo.
- Cola de pendientes: cero huérfanos, o listados con dueño.

Un cierre que no cuadra **no se cierra**: se deja abierto con la diferencia registrada. Cerrar un
día que no cuadra es escribir una mentira que después nadie puede deshacer.

## 6. Prevención: lo que evita la deriva desde el diseño

- **Outbox** en toda publicación que no tolera pérdida (regla 98.3.3).
- **Consumidores idempotentes** con deduplicación transaccional.
- **Clave de idempotencia** de punta a punta, la misma en todos los reintentos.
- **Sagas con compensación**, sin pasos irreversibles al principio.
- **Redondeo definido en un solo lugar** y el mismo en todos los servicios (regla 91.1.1).
- **DLQ con dueño y alerta**: un mensaje de dinero en DLQ es un descuadre que todavía no pasó.
- **Eventos con el estado resultante**, no deltas, donde el orden es frágil.

## Anti-patrones

- Descubrir el descuadre por un reclamo de un participante.
- Conciliación interna manual, cuando alguien se acuerda.
- `UPDATE` de ajuste sobre el mayor o sobre una tabla append-only.
- Chequeo de cuadre que consulta las bases de los otros servicios.
- Tratar una fila descuadrada como un log en vez de una alerta.
- Cerrar el día con diferencia "chica".
- Corregir el dato sin arreglar la causa.
- DLQ de un tópico de pagos sin dueño.
- Dos servicios redondeando distinto.

## Checklist

- [ ] Las invariantes que cruzan servicios están escritas, con qué las verifica.
- [ ] El chequeo de conciliación interna corre automático y alerta con dueño.
- [ ] El chequeo usa modelos de lectura propios, sin tocar bases ajenas.
- [ ] La fuente de verdad está declarada: el mayor.
- [ ] Existe procedimiento escrito para discrepancias, con el `UPDATE` manual prohibido.
- [ ] El cierre diario incluye los cuadres de §5 y no cierra si no cuadra.
- [ ] Outbox, idempotencia, DLQ con dueño y redondeo único están en su lugar.
- [ ] Serie histórica de las diferencias, para ver si la deriva crece.

## Evidencia / DoD

1. Salida del chequeo de conciliación interna, en cero, con la consulta pegada.
2. Salida del cierre diario cuadrado, por cuenta.
3. Para una diferencia encontrada: causa identificada, corrección por operación de negocio y la
   verificación posterior en cero.
4. Estado de las DLQ de los tópicos de dinero, con su dueño.
