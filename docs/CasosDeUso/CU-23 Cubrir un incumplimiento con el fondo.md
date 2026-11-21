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

> [!warning] **Pendiente de implementar** ([[ADR-055 Sin aprobaciones en el circuito|ADR-055]] E7 y ADR-056, modelo A)
> Los dos caminos anteriores se retiraron:
> - la **cobertura manual** del fondo mutual, con monto, mora y aprobador del pedido (F1.L2.M02):
>   `POST /incumplimientos/{id}/cobertura` responde **410 `AP-CU23-14`**;
> - el **respaldo empresarial** por ciclo (F2.L2.M02, ADR-051 reemplazado): las seis rutas
>   `/garantia/respaldo/*` responden **410 `AP-CU23-15`**. La empresa no reserva ni cubre nada.
>
> Lo que sigue describe el **objetivo** (F3.L2.M05): todavía no tiene código. Hasta que llegue, un turno con
> faltante queda BLOQUEADO con la deuda conservada ([[CU-22 Liquidar y entregar el fondo]]). Las tablas del
> respaldo y del modelo anterior siguen en el esquema hasta su contracción (F2.L2.M03).

## Actores y disparador

- **Actor principal:** el sistema, al vencer el plazo de gracia de una cuota impaga.
- **Sin actor humano:** nadie pide ni aprueba una cobertura (ADR-055).

## Precondiciones

1. Existe una [[obligacion_aporte]] impaga con el plazo de gracia vencido (parámetro obligatorio, F3.L2.M14).
2. El grupo tiene su [[fondo_garantia]].

## Flujo principal (objetivo, F3.L2.M05)

1. Al vencer la gracia, el sistema lee de `aportes` el importe y la moneda de la cuota impaga.
2. **En la misma transacción**, una sola vez por (grupo, periodo, participante):
   - cubre la cuota con el fondo del grupo ([[movimiento_fondo]], append-only) y propone su
     [[asiento_contable]] (F3.L2.M04);
   - crea la [[deuda_participante]] por ese importe con acreedor «fondo del grupo» ([[subrogacion]]).
3. Publica el evento que destraba la entrega del turno (F3.L2.M01).
4. Lo que el deudor pague después vuelve al fondo.

## Flujos alternativos

| # | Situación | Resultado |
| :-: | --- | --- |
| 2a | El fondo del grupo no alcanza | No se cubre: la entrega queda en espera explícita y visible, sin dinero de la empresa |
| 2b | El vencimiento se procesa dos veces | No cubre dos veces: unicidad por (grupo, periodo, participante) y clave por `message_id` |
| 2c | Alguien pide la cobertura a mano | 410 `AP-CU23-14`, sin efecto (F1.L2.M02) |
| 2d | Alguien llama una ruta del respaldo empresarial | 410 `AP-CU23-15`, sin efecto (F2.L2.M02) |

## Postcondiciones

- O la cuota quedó cubierta una sola vez por el fondo del grupo, con su deuda, o no se cubrió nada.
- Cubrir no perdona: la deuda del incumplidor existe y es recuperable.

## Contrato · `openapi/garantia.yaml`

```ts
export const ErroresCU23 = {
  FONDO_INSUFICIENTE: 'AP-CU23-01',      // retirado con la cobertura manual (F1.L2.M02)
  TOPE_POR_PARTICIPANTE: 'AP-CU23-02',   // retirado con la cobertura manual
  REQUIERE_ACUERDO: 'AP-CU23-03',        // retirado con la cobertura manual
  OBLIGACION_NO_VENCIDA: 'AP-CU23-04',   // retirado con la cobertura manual
  CAPACIDAD_INSUFICIENTE: 'AP-CU23-05',  // retirado con el respaldo (F2.L2.M02)
  CLAVE_REUTILIZADA: 'AP-CU23-06',       // retirado con el respaldo
  RESERVA_NO_DISPONIBLE: 'AP-CU23-07',   // retirado con el respaldo
} as const
```

| Error | Cuándo se devuelve |
| --- | --- |
| `FONDO_INSUFICIENTE` | Retirado: era de la cobertura manual |
| `TOPE_POR_PARTICIPANTE` | Retirado: era de la cobertura manual |
| `REQUIERE_ACUERDO` | Retirado: era de la cobertura manual |
| `OBLIGACION_NO_VENCIDA` | Retirado: era de la cobertura manual |
| `CAPACIDAD_INSUFICIENTE` | Retirado: era del respaldo empresarial |
| `CLAVE_REUTILIZADA` | Retirado: era del respaldo empresarial |
| `RESERVA_NO_DISPONIBLE` | Retirado: era del respaldo empresarial |

Declarados en `openapi/garantia.yaml`: `AP-CU23-09` a `AP-CU23-13` eran del respaldo y quedan retirados con él;
`AP-CU23-14` (410, cobertura manual) y `AP-CU23-15` (410, respaldo) son los que responden hoy. Los códigos de la
cobertura del modelo A se declaran con F3.L2.M05.

## Descomposición atómica (objetivo)

| Nivel | Pieza | Responsabilidad |
| --- | --- | --- |
| Puerto | `RecaudoDelPeriodo` (`RecaudoPorHttp`) | Lee de `aportes` la cuota impaga; hoy sin uso |
| Organismo | Cobertura del fondo del grupo (F3.L2.M05) | Transacción: movimiento del fondo, deuda y asiento propuesto |
| Consumidor | Vencimiento de la gracia | Dispara la cobertura, idempotente por `message_id` |

## Eventos, trabajos y permisos

| Emite | Dispara | Exige |
| --- | --- | --- |
| Cobertura aplicada (F3.L2.M05) | Destraba la entrega del turno | Sistema |

## Interfaz

- **App:** El grupo ve si la cuota quedó cubierta por el fondo y quién la debe reponer.
- **Backoffice:** Fondo por grupo: saldo, cubierto, recuperado.

## Restricciones aplicables

`R-GAR-08` · `R-GAR-09` · `R-GAR-10` · `R-GAR-11` · `R-GAR-12` · `R-GAR-13`

Son del **respaldo empresarial retirado** y la base las sigue haciendo cumplir sobre sus tablas hasta la
contracción del esquema (F2.L2.M03), que las retira con ellas: capacidad nunca mayor a su tope (R-GAR-08),
reserva movida solo por movimientos inmutables (R-GAR-09), una cobertura viva por turno (R-GAR-10), líneas que
suman el faltante (R-GAR-11), turno del grupo y periodo declarados (R-GAR-12) y una contingencia de caja viva
por grupo (R-GAR-13). Las restricciones de la cobertura del modelo A se declaran con F3.L2.M05.

## Evidencia que deja

[[registro_incumplimiento]] · [[movimiento_fondo]] · [[deuda_participante]] · [[subrogacion]] ·
[[asiento_contable]]

Del modelo anterior, sin escritor hasta su contracción: [[cobertura_incumplimiento]] · [[politica_cobertura]] ·
[[gestion_cobranza]] · [[estrategia_cobranza]] · [[acuerdo_quita]] · [[capacidad_respaldo]] ·
[[reserva_respaldo]] · [[movimiento_reserva]] · [[cobertura_respaldo]] · [[cobertura_respaldo_linea]] ·
[[recuperacion_respaldo]] · [[plan_contingencia]]

## Criterios de aceptación

```gherkin
Dado un participante que no pagó al vencer la gracia
Cuando corre el vencimiento
Entonces la entrega del turno sale completa con dinero del fondo del grupo y existe una deuda a su nombre por ese importe

Dado un vencimiento ya procesado
Cuando se procesa otra vez
Entonces no se cubre dos veces

Dado un fondo del grupo que no alcanza
Cuando corre el vencimiento
Entonces la entrega queda en espera visible y no se escribe ningún asiento de la empresa
```

## Ver también

[[CU-22 Liquidar y entregar el fondo]] · [[CU-25 Declarar el incumplimiento con descargo y evidencia]] · [[CU-26 Ejecutar el aval y subrogar la deuda]] · [[CU-29 Devolver los aportes del fondo de garantía]] · [[CU-54 Registrar un evento de riesgo operativo]] · [[CU-66 Reemplazar a un participante moroso]]
