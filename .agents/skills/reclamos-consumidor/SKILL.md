---
name: reclamos-consumidor
description: "Atender reclamos y proteger al consumidor financiero en AportaYa: plazos hábiles guardados, prórroga, reparación obligatoria, segunda instancia, transparencia de información y contratos de adhesión. Úsala al implementar el circuito de reclamos, al agregar un canal de atención, o cuando un flujo pueda terminar en 'el cliente tiene razón'."
---

# Consumidor financiero

Este módulo es el que un supervisor mira primero, porque es medible sin entender el
producto: cuántos reclamos, cuántos fuera de plazo, cuántos favorables sin
reparación. Los tres números salen de consultas, no de un informe redactado.

## El circuito

```
punto_reclamo → reclamo_cliente → (respuesta) → reparación
                      ↓ disconforme
                instancia_reclamo (defensoría, regulador, arbitraje, judicial)
```

## Los plazos se calculan al ingresar y se guardan

```
fecha_ingreso        cuándo entró
plazo_respuesta      calculado en días hábiles administrativos, PERSISTIDO
dias_habiles_plazo   con cuántos días se calculó (R-CON-01)
```

Persistir el plazo, y no calcularlo en la consulta, es lo que permite responder
dentro de dos años **con qué plazo regía ese día**. Si mañana la norma cambia de 5
a 7 días, los reclamos viejos conservan el suyo.

Los días son **hábiles administrativos**: excluyen sábados, domingos y feriados. El
átomo que los calcula es puro y se prueba solo, incluido el caso del feriado móvil.

## Prórroga: dos escalones, dos pruebas

| Escalón | Requisito | Columna |
| --- | --- | --- |
| Extender dentro del máximo | Comunicar al cliente **dentro del plazo original** | `plazo_prorrogado_hasta`, `prorroga_comunicada_al_cliente_en` (`R-CON-02`) |
| Superar el máximo | Comunicar además **por escrito al supervisor**, con justificación | `prorroga_comunicada_al_organismo_en`, `justificacion_prorroga` (`R-CON-03`) |

Una prórroga sin la comunicación registrada no es una prórroga: es un
incumplimiento con una fecha distinta.

## Reparación obligatoria

**Un reclamo con resultado favorable y monto reclamado no se cierra sin la
reparación asociada** (`R-CON-04`). La base lo impide. La reparación es una
devolución de comisión con su nota de crédito, o una transacción de resarcimiento
—en ambos casos, dinero que se mueve y queda en el libro.

Darle la razón al cliente y no devolverle nada es la forma más cara de perder una
inspección, porque queda escrita.

## Canales

`punto_reclamo` define los canales habilitados (app, web, teléfono, presencial,
correo). Reglas:

- **Ningún canal exige ser cliente registrado para reclamar.** El canal es público.
- Un canal deshabilitado se rechaza al ingresar, no después.
- El reclamo recibe **código único correlativo** que se le comunica al cliente en el
  acto. Sin número, el cliente no puede hacer seguimiento ni el supervisor
  auditarlo.

## Conservación

`conservar_hasta` = 10 años desde el cierre. El reclamo entra además en el reporte
periódico al supervisor (`incluido_en_reporte_mensual`). Un pedido de supresión de
datos personales **no borra** el expediente del reclamo: la obligación de conservar
gana, y se documenta por qué (skill `seguridad-sesion-rls`).

## Segunda instancia

`instancia_reclamo` con el expediente completo: reclamo, respuesta y **evidencia
técnica** (`bitacora_evento`, `movimiento_billetera`, `cotizacion_comision`).

> Si no hay evidencia técnica suficiente para armar el expediente, eso **ya es un
> hallazgo**: significa que el flujo original no dejó rastro. Se registra como tal,
> no se completa el expediente a mano.

Una resolución en contra con multa genera `observacion_regulatoria` y
`plan_accion_riesgo` con responsable y fecha.

## Reclamos repetidos = falla sistémica

Varios reclamos por la misma causa no son varios casos: son uno operativo. Se
agrupan y se abre `evento_riesgo_operativo` (skill `observabilidad`). Además se
evalúa comunicación proactiva a los demás afectados —los que no reclamaron
todavía—, que es la diferencia entre gestionar el problema y esperar a que crezca.

## Transparencia de información

| Obligación | Dónde vive |
| --- | --- |
| Contrato de adhesión vigente y aceptado, con versión | `contrato_adhesion` + `aceptacion_contrato` |
| Tarifario público y actualizado | `documento_publicado` |
| Preaviso de cambios de condiciones | `cambio_tarifario` + notificación obligatoria |
| Constancia de lo aceptado, recuperable por el cliente | `aceptacion_contrato` con su versión exacta |

El cliente debe poder recuperar **la versión que aceptó**, no la vigente hoy.

## Checklist

- [ ] El plazo se calcula al ingresar y queda persistido con sus días hábiles.
- [ ] El código del reclamo se entrega al cliente en el acto.
- [ ] La prórroga exige comunicación registrada, y la base lo verifica.
- [ ] Cierre de favorable sin reparación: **rechazado**, con prueba.
- [ ] El vencido aparece en el tablero y escala a hallazgo.
- [ ] La segunda instancia arma expediente con evidencia técnica real.
- [ ] Reclamos repetidos por la misma causa se detectan y agrupan.
- [ ] `conservar_hasta` fijado; la supresión de datos no lo borra.

## Ver también

`facturacion-sin` · `observabilidad` · `seguridad-sesion-rls` ·
`reportes-regulatorios` · CU-52, CU-53 · familia `R-CON`
