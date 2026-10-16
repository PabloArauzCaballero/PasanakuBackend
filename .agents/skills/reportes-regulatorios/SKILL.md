---
name: reportes-regulatorios
description: "Generar y remitir reportes al supervisor y a la UIF en AportaYa: catálogo con periodicidad y plazo, generación reproducible, envío con acuse, reenvío por observación y control de vencimientos. Úsala al implementar cualquier remisión periódica, al agregar un reporte al calendario, o cuando un reporte esté por vencer."
---

# Reportes regulatorios

Un reporte regulatorio no es una exportación: es una **declaración con plazo,
acuse y consecuencia**. Enviarlo tarde, incompleto o irreproducible es una
observación, y las observaciones se acumulan.

## El circuito

```
catalogo_reporte_regulatorio   qué reportes existen, periodicidad, plazo, base normativa
  → reporte_regulatorio        la instancia de un período concreto
    → envio_regulatorio        cada intento de remisión, con su acuse
      → observacion_regulatoria   lo que el organismo devuelve
```

Más `definicion_reporte`, `ejecucion_reporte` y `exportacion_reporte` (M9) para el
motor genérico, y `programacion_reporte` para el calendario.

## El calendario es dato

Periodicidad, plazo, formato y destinatario viven en el catálogo, con
`base_normativa` y vigencia. Cuando la norma cambia el plazo, cambia una fila.
**Ningún plazo dentro del código.**

De ahí sale, sin que nadie lo mantenga a mano:

- Qué reporte toca y cuándo vence.
- Qué está por vencer (alerta anticipada, no el día del vencimiento).
- Qué venció sin enviarse → `hallazgo_auditoria` automático.

## Reproducibilidad

**Un reporte del período pasado debe dar el mismo resultado hoy que el día que se
envió.** Eso obliga a tres cosas:

1. **Corte por fecha de registro, no por "lo que hay ahora".** Los datos que
   llegaron después pertenecen al período siguiente, o a una rectificación
   explícita.
2. **Los parámetros del cálculo se guardan** con el reporte: umbrales aplicados,
   tipos de cambio, versión de la definición.
3. **El archivo enviado se conserva**, con su hash. No se regenera para mostrarlo:
   se muestra el que se envió.

Si al reconstruir un reporte viejo sale un número distinto, el problema no es el
reporte: es que el sistema no conserva su historia.

## Envío y acuse

| Regla | Por qué |
| --- | --- |
| El envío es **asíncrono**, por la cola | La caída del canal del organismo no puede tumbar la operación |
| Cada intento deja fila en `envio_regulatorio` | Incluidos los fallidos: el intento en plazo es un hecho relevante |
| **El acuse se guarda** (número, fecha, respuesta) | Sin acuse, no hay prueba de haber enviado |
| Enviado sin acuse ⇒ **no está enviado** | Se reintenta y se escala; no se marca cumplido por optimismo |
| Los reintentos tienen tope y terminan en cola muerta visible | Un reporte perdido en silencio vence igual |

## Observaciones y rectificaciones

Una observación del organismo (`observacion_regulatoria`) tiene tipo, plazo de
respuesta y consecuencia. Reglas:

- **La rectificación es un envío nuevo enlazado al original**, nunca la edición del
  reporte enviado. Queda visible que hubo corrección, que es precisamente lo que el
  supervisor quiere ver.
- Una observación con multa abre `plan_accion_riesgo` con responsable y fecha.
- Un patrón de observaciones sobre el mismo reporte es una falla de proceso:
  `evento_riesgo_operativo`, no un reenvío más.

## Reportes de la UIF

Los formularios por umbral (PCC-01, ROG) se agrupan por `periodo_remision` y salen
en el envío mensual. El reporte de operación sospechosa es **distinto**: no tiene
periodicidad, se remite cuando el oficial de cumplimiento lo decide, y **jamás se
le comunica al titular** (skill `cumplimiento-uif`).

## Requerimientos de autoridad

Un oficio no es un reporte periódico: llega con su propio plazo y su propio
alcance. Se registra en `requerimiento_autoridad` con quién lo firma, qué pide y
para cuándo; la respuesta se arma con evidencia real y queda archivada. El acceso a
los datos que se entregan deja fila en `registro_acceso_datos`.

## Checklist

- [ ] El reporte está en el catálogo con periodicidad, plazo y base normativa.
- [ ] La generación es reproducible: corte por fecha de registro y parámetros
      guardados.
- [ ] El archivo enviado se conserva con su hash.
- [ ] El acuse se guarda; sin acuse el reporte no figura enviado.
- [ ] Hay alerta **antes** del vencimiento, no el mismo día.
- [ ] El vencido genera hallazgo automáticamente.
- [ ] La rectificación es un envío nuevo enlazado, no una edición.
- [ ] Existe prueba de que regenerar un reporte viejo da el mismo resultado.

## Ver también

`cumplimiento-uif` · `norma-nueva` · `semillas-catalogos` · `trabajos-outbox` ·
`observabilidad` · CU-43, CU-45 · `docs/Cumplimiento.md`
