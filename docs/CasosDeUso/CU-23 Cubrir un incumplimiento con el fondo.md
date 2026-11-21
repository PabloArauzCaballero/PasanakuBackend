---
tags:
  - caso-uso
  - modulo/08-garantia-incumplimiento-cobranza-y-sanciones
  - modulo/10-billetera-custodia-y-dinero-electronico
codigo: CU-23
criticidad: alta
actores: [Sistema, Comité del grupo]
normas: [Contabilidad, debido proceso, ASFI riesgo]
---

# CU-23 — Cubrir un incumplimiento con el fondo de garantía

> **Objetivo.** Que el grupo no se detenga cuando alguien no aporta, y que esa
> cobertura **no sea un regalo**: la deuda de quien incumplió se conserva, con expediente.

> **Vigencia ([[ADR-055 Sin aprobaciones en el circuito|ADR-055]] E7 y ADR-056, modelo A).**
> Ninguna persona decide una cobertura. La **cobertura manual** del fondo mutual —con monto,
> días de mora y aprobador tomados del pedido, y un umbral de aprobación humana— **se retiró**
> (F1.L2.M02): `POST /incumplimientos/{id}/cobertura` está `deprecated` y responde **410
> `AP-CU23-14`** sin efecto. El camino que hoy cubre el faltante del corte es el **respaldo
> por ciclo** (`CU23ReservarRespaldo` / `CU23CubrirConRespaldo`, ADR-051), **transitorio**:
> se retira en F2.L2.M02 y la cobertura renace en F3.L2.M05 como acto del sistema con el
> **fondo del grupo**, al vencer el plazo de gracia, con mora y monto leídos de `aportes`.
> Las tablas del modelo anterior ([[politica_cobertura]], [[cobertura_incumplimiento]]) siguen
> en el esquema hasta su contracción; ningún código nuevo las escribe.

## Actores y disparador

- **Actor principal:** el sistema, al detectar en el corte un pozo incompleto antes de una entrega.
- **Sin actor humano:** nadie aprueba ni pide una cobertura (ADR-055).

## Precondiciones

1. Existe un turno con [[obligacion_aporte]] faltantes al corte.
2. El grupo tiene una [[reserva_respaldo]] vigente (camino transitorio).

## Flujo principal

1. `aportes` informa el recaudo del periodo; el faltante es pozo − confirmado − cubierto.
2. **En la misma transacción**, si la reserva alcanza para **todo** el faltante:
   - se crea [[cobertura_respaldo]] por el faltante, con una [[cobertura_respaldo_linea]] por obligación;
   - se registra el [[movimiento_reserva]] de aplicación (append-only);
   - se emite el hecho para el mayor; el [[asiento_contable]] lo escribe `nucleo-financiero`.
3. La deuda de quien no aportó se conserva como derecho de recuperación ([[recuperacion_respaldo]]).
4. La entrega del turno continúa ([[CU-22 Liquidar y entregar el fondo]]).

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | La reserva no alcanza para todo el faltante | No se cubre nada (todo o nada), la reserva queda intacta y se abre la contingencia de caja; la entrega queda `BLOQUEADA_POR_FONDO_INCOMPLETO` |
| 2b | El grupo no tiene reserva vigente | `SIN_RESERVA`, sin cobertura |
| 2c | El mismo pedido llega dos veces con la misma clave | Devuelve la misma cobertura, sin segundo efecto |
| 2d | Alguien pide la cobertura a mano | 410 `AP-CU23-14`, sin efecto (F1.L2.M02) |

## Postcondiciones

- O el faltante quedó cubierto entero y una sola vez, o no se cubrió nada.
- La deuda del incumplidor existe y es recuperable: cubrir no perdona.

## Contrato · `openapi/garantia.yaml`

```ts
export const ErroresCU23 = {
  FONDO_INSUFICIENTE: 'AP-CU23-01',      // retirado con la cobertura manual (F1.L2.M02)
  TOPE_POR_PARTICIPANTE: 'AP-CU23-02',   // retirado con la cobertura manual
  REQUIERE_ACUERDO: 'AP-CU23-03',        // retirado con la cobertura manual
  OBLIGACION_NO_VENCIDA: 'AP-CU23-04',   // retirado con la cobertura manual
  CAPACIDAD_INSUFICIENTE: 'AP-CU23-05',
  CLAVE_REUTILIZADA: 'AP-CU23-06',
  RESERVA_NO_DISPONIBLE: 'AP-CU23-07',
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `FONDO_INSUFICIENTE` | Retirado: era de la cobertura manual |
| `TOPE_POR_PARTICIPANTE` | Retirado: era de la cobertura manual |
| `REQUIERE_ACUERDO` | Retirado: era de la cobertura manual |
| `OBLIGACION_NO_VENCIDA` | Retirado: era de la cobertura manual |
| `CAPACIDAD_INSUFICIENTE` | No hay capacidad cargada, o no alcanza para la reserva |
| `CLAVE_REUTILIZADA` | La clave o el turno ya tienen otra operación distinta |
| `RESERVA_NO_DISPONIBLE` | La reserva no existe, está liberada o toda aplicada |

El camino del respaldo devuelve además, declarados en `openapi/garantia.yaml`: `AP-CU23-09`
(las líneas no suman el faltante, o no hay faltante), `AP-CU23-10` (contingencia de caja
activa), `AP-CU23-11` (cobertura no reversible), `AP-CU23-12` (`aportes` no responde: se
deniega por omisión) y `AP-CU23-13` (periodo o turno de otro grupo, o faltante distinto). La
ruta de la cobertura manual responde `AP-CU23-14` (410).

## Descomposición atómica

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Molécula | `RespaldoRepositorio` / `CoberturaRespaldoRepositorio` | Reserva, cobertura y su libro de movimientos |
| Organismo | `CU23CubrirConRespaldo` | Transacción: cobertura todo o nada, líneas y hecho para el mayor |
| Organismo | `ContingenciaDeCaja` | Plan de contingencia cuando la reserva no alcanza |
| Página | `POST /garantia/respaldo/coberturas` | Traduce y delega, sin lógica |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| `garantia.respaldo_aplicado` | Asiento en el mayor | Token de servicio |
| `garantia.respaldo_insuficiente` | Incidente y contingencia de caja | — |

## Interfaz

- **App:** El grupo ve si el pozo se completó y quién debe reponer.
- **Backoffice:** Reserva por ciclo: disponible, aplicado y recuperado.

## Restricciones aplicables

`R-GAR-08` · `R-GAR-09` · `R-GAR-10` · `R-GAR-11` · `R-GAR-12` · `R-GAR-13`

Las hace cumplir la base en el camino del **respaldo**:

- `R-GAR-08` · la capacidad empresarial comprometida **nunca supera su tope**. Dos grupos
  que activan a la vez compiten por la misma fila de capacidad: se bloquea primero y la
  base lo vuelve a exigir. Sin capacidad cargada se deniega; el tope es un dato.
- `R-GAR-09` · la reserva se mueve solo con **movimientos inmutables** y nunca se usa más
  de lo reservado (aplicado + liberado ≤ reservado; recuperado ≤ aplicado).
- `R-GAR-10` · **una cobertura viva por turno** (la reversada deja de contar) y su
  faltante cuadra con el pozo.
- `R-GAR-11` · las líneas de una cobertura **suman exactamente su faltante** y cada línea
  recupera a lo sumo lo que cubrió.
- `R-GAR-12` · el turno de una cobertura **es del grupo y del periodo que declara**.
- `R-GAR-13` · **una contingencia de caja viva por grupo**: si la reserva no alcanza sobre un
  compromiso que ya existe, se abre un `plan_contingencia` y no se reservan ciclos nuevos
  mientras dure (`AP-CU23-10`). La deuda se conserva: no se cubre una parte ni se cancela una
  obligación.

## Evidencia que deja

[[registro_incumplimiento]] · [[capacidad_respaldo]] · [[reserva_respaldo]] ·
[[movimiento_reserva]] · [[cobertura_respaldo]] · [[cobertura_respaldo_linea]] ·
[[recuperacion_respaldo]] · [[plan_contingencia]] · [[asiento_contable]]

Del modelo anterior, sin escritor nuevo hasta F3.L2.M05: [[cobertura_incumplimiento]] ·
[[movimiento_fondo]] · [[deuda_participante]] · [[subrogacion]] · [[politica_cobertura]] ·
[[gestion_cobranza]] · [[estrategia_cobranza]] · [[acuerdo_quita]].

## Criterios de aceptación

```gherkin
Dado un pozo de Bs 6.000 con Bs 5.000 confirmados y una reserva de Bs 8.000
Cuando se cubre el faltante del corte
Entonces se aplica una sola cobertura de Bs 1.000 con una línea por obligación faltante

Dada una reserva de Bs 800 y un faltante de Bs 1.000
Cuando se cubre el faltante del corte
Entonces no se cubre nada y la reserva queda intacta

Dado un expediente de incumplimiento
Cuando alguien pide la cobertura a mano con monto, mora y aprobador
Entonces responde 410 AP-CU23-14 y no se ejecuta ningún caso de uso
```

## Ver también

[[CU-22 Liquidar y entregar el fondo]] · [[CU-25 Declarar el incumplimiento con descargo y evidencia]] · [[CU-26 Ejecutar el aval y subrogar la deuda]] · [[CU-29 Devolver los aportes del fondo de garantía]] · [[CU-54 Registrar un evento de riesgo operativo]] · [[CU-66 Reemplazar a un participante moroso]]
