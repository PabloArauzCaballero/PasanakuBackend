---
name: financial-close-reporting
description: Cierre contable y reportería financiera — el cierre diario como control que cuadra o no cierra, periodos contables y su bloqueo, definición y ejecución de reportes con parámetros y procedencia, reportes reproducibles (mismo periodo, mismo resultado), exportes auditados y con datos mínimos, reportes regulatorios con su plazo, y por qué un número de un reporte siempre tiene que poder rastrearse hasta sus asientos. Usar al construir el cierre, un reporte financiero, un export, un dashboard con cifras de dinero, o al investigar por qué dos reportes del mismo periodo dan distinto.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Cierre contable y reportería

Un reporte financiero es una afirmación firmada por la empresa. Si dos pantallas dan números
distintos para el mismo periodo, el problema no es "un bug de reportes": es que nadie sabe
cuánta plata hay.

Entidades: `cierre_diario`, `definicion_reporte`, `ejecucion_reporte`, `exportacion_reporte`.
Reglas duras: **91.1.5** (todo sale del mayor) y **91.6**.

## 1. El cierre diario cuadra, o no cierra

`cierre_diario` es el momento en que el sistema declara que el día es consistente. Verifica, como
mínimo:

| Control | Fuentes |
|---|---|
| Suma de débitos = suma de créditos, por asiento y del día | `contabilidad` |
| Saldo por cuenta = saldo anterior + movimientos del día | `contabilidad` |
| Acreditaciones del día = movimientos conciliados del banco ± partidas en tránsito | `payment-reconciliation` |
| Desembolsos ordenados = neto + deducciones aplicadas | `disbursement-payouts` |
| Saldo del fondo = mayor del fondo | `guarantee-fund-workflows` |
| Sin huérfanos entre servicios | `distributed-data-integrity` |

- **No cuadra ⇒ el día no se cierra.** Queda abierto, con la diferencia registrada, dueño y
  alerta. Cerrar un día con diferencia "chica" es escribir una mentira que después nadie puede
  deshacer, y las diferencias chicas sistemáticas son la firma de un bug de redondeo.
- El cierre es **idempotente**: correrlo dos veces no duplica nada (`money-movement-safety` §2).
- Genera su propio registro con los totales verificados, para poder comparar históricos.

## 2. Periodos contables y bloqueo

- Un periodo cerrado **se bloquea**: no admite asientos nuevos con fecha dentro.
- Lo que aparece después se registra **en el periodo abierto**, con referencia al hecho
  original. Esa es la práctica contable correcta y además es la única compatible con un mayor
  append-only (regla 91.1.3).
- El bloqueo se impone **en la base o en el dominio**, no confiando en que nadie lo intente.
- Reapertura de un periodo: excepcional, con autorización, motivo y auditoría. Si pasa seguido,
  el problema está en el proceso de cierre.

## 3. Reportes: definición, ejecución, procedencia

Separar las tres cosas es lo que hace que un reporte sea confiable:

| Entidad | Qué guarda |
|---|---|
| `definicion_reporte` | Qué calcula, con qué consulta, qué parámetros acepta, **versionada** |
| `ejecucion_reporte` | Cuándo se corrió, con qué parámetros, qué versión de la definición, quién |
| `exportacion_reporte` | Qué se exportó, en qué formato, quién se lo llevó |

**Reproducibilidad:** ejecutar el mismo reporte, con los mismos parámetros, sobre un periodo
cerrado, **da exactamente el mismo resultado hoy y dentro de un año**. Si no, el reporte no
sirve como evidencia.

Lo que rompe la reproducibilidad, y hay que evitar:

- Consultas con `NOW()` o "últimos 30 días" relativos a la corrida, sobre datos históricos.
- Leer de una proyección con desfase en vez del mayor
  (`eventual-consistency-read-models`).
- Definición editada sin versionar: el reporte de marzo ya no se puede recomputar.
- Datos mutables aguas arriba. Por eso el mayor es append-only.

## 4. Todo número se rastrea hasta sus asientos

Regla de diseño: **desde cualquier cifra de un reporte se puede llegar a las líneas que la
componen, y de ahí a los hechos que las originaron.**

```
total del reporte → asientos incluidos → movimientos → operación (pago/entrega/cobertura)
                                                            → expediente / webhook / orden
```

Un total que no se puede abrir es un número que nadie puede defender ante un participante, un
auditor o un regulador. Si tu reporte hace `SUM()` sobre una tabla desnormalizada sin guardar
qué filas entraron, no tenés trazabilidad.

## 5. Exportes

- **Autorización explícita** por reporte, no "quien entra al panel".
- **Datos mínimos** (`data-privacy-financial` §8): un reporte de totales no lleva documentos ni
  cuentas bancarias.
- **Auditados**: quién, cuándo, qué parámetros, cuántas filas. El export es la superficie de
  fuga más grande del sistema.
- Con límite de volumen y de tasa (`api-gateway-bff` §5); los pesados van asíncronos con
  notificación al terminar, no bloqueando un request.
- Formato declarado y estable (`terminology-value-sets` para los códigos que contenga).

## 6. Reportes regulatorios

- Su contenido, formato y plazo los define el marco normativo y el oficial de cumplimiento:
  **se validan con ellos** (`regulatory-compliance-mapping`, `aml-sanctions-screening`).
- El sistema aporta: generación reproducible, acuse de envío, hash del archivo enviado y
  retención por el plazo exigido.
- Un reporte regulatorio enviado **se conserva tal cual se envió**. Regenerarlo con la definición
  nueva y decir que es el mismo es falsear evidencia.

## 7. Dashboards con cifras de dinero

- El número grande de un dashboard está sujeto a las mismas reglas: viene del mayor, dice **a
  qué fecha y hora** corresponde, y si la fuente está atrasada **lo dice** (regla 98.4).
- Prohibido un KPI de dinero calculado con una consulta distinta de la del reporte oficial: dos
  fórmulas para el mismo concepto es garantía de discrepancia (`dashboard-data-ui`).
- Las cifras por grupo respetan lo que el reglamento permite mostrar
  (`transparency-reputation`).

## Anti-patrones

- Cerrar un día con diferencia.
- Periodo cerrado que admite asientos.
- Corregir un periodo cerrado con `UPDATE`.
- Definición de reporte editada sin versionar.
- Reporte con `NOW()` sobre datos históricos.
- Reporte que lee de una proyección atrasada.
- Total imposible de abrir hasta sus asientos.
- Export sin auditoría, sin límite o con datos de más.
- Dos consultas distintas para el mismo indicador.
- Regenerar un reporte regulatorio ya enviado y presentarlo como el original.

## Checklist

- [ ] El cierre diario verifica todos los controles de §1 y no cierra si no cuadra.
- [ ] El cierre es idempotente y deja registro con sus totales.
- [ ] Periodos cerrados bloqueados en base o dominio; reapertura auditada.
- [ ] Definiciones de reporte versionadas; ejecuciones con parámetros y versión.
- [ ] Reproducibilidad verificada sobre un periodo cerrado.
- [ ] Todo total se puede abrir hasta sus asientos y hasta la operación de origen.
- [ ] Exportes autorizados, mínimos, auditados y limitados.
- [ ] Reportes regulatorios conservados tal cual se enviaron, con acuse y hash.
- [ ] Un solo cálculo por indicador, compartido entre dashboard y reporte.

## Evidencia / DoD

1. Salida del cierre diario cuadrado, control por control.
2. Salida de un cierre que no cuadra: el día queda abierto con la diferencia registrada.
3. Mismo reporte ejecutado dos veces sobre un periodo cerrado: salidas idénticas (diff vacío).
4. Recorrido de un total hasta sus asientos y hasta la operación de origen.
5. Registro de auditoría de un export, con parámetros y cantidad de filas.
