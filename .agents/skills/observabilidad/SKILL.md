---
name: observabilidad
description: "Dejar rastro en AportaYa: bitácora de eventos, trazas correlacionadas, indicadores, alertas de cumplimiento, incidentes operativos y eventos de riesgo. Úsala al agregar registro a un flujo, al definir qué se monitorea, cuando haga falta investigar qué pasó con una operación, o cuando un incidente tenga que convertirse en pérdida cuantificada."
---

# Rastro, monitoreo e incidentes

La diferencia entre esta plataforma y una aplicación común: acá el registro **es
evidencia regulatoria**, no una ayuda para depurar. Si no quedó escrito, no pasó.

## Las cuatro tablas y qué distingue a cada una

| Tabla | Qué guarda | Quién la lee |
| --- | --- | --- |
| `bitacora_evento` | Quién hizo qué, cuándo, desde dónde. *Append-only* | Auditoría y soporte |
| `evento_dominio` | Hechos de negocio, escritos en la transacción (*outbox*) | El worker, para disparar efectos |
| `registro_acceso_datos` | Quién **leyó** datos personales de un tercero, y por qué | Auditoría y protección de datos |
| `incidente_operativo` | Fallas de servicio, con su línea de tiempo | Operación y riesgos |

Confusión frecuente: `evento_dominio` no reemplaza a `bitacora_evento`. Uno es para
actuar, el otro para responder preguntas. Un flujo relevante escribe en los dos.

## Qué se registra siempre

- Toda operación con dinero: quién, desde qué sesión, qué dispositivo, qué IP.
- Todo cambio de estado de un expediente (KYC, reclamo, caso, incidente).
- Toda lectura de datos personales por parte de un operador, con motivo.
- Todo acceso o cambio a catálogos regulatorios.
- Todo rechazo por restricción o por política: **el rechazo también es evidencia**,
  y es lo que demuestra que el control funciona.

## Qué no se registra nunca

Contraseñas, tokens, números de cuenta completos, documentos de identidad
completos, el contenido de un reporte de operación sospechosa en un registro
general. Un registro que filtra es peor que no tener registro: reproduce el dato
sensible en un lugar con menos protección.

## Trazas correlacionadas

Un `trazaId` por request y por trabajo, que viaja del controlador al organismo, al
worker y al proveedor externo, y aparece en la respuesta de error. Es lo único que
el usuario le dicta al soporte, y lo que permite reconstruir un flujo que cruzó
tres procesos.

Cuando un trabajo nace de un evento, hereda la traza del request que lo originó: si
no, la cadena se corta exactamente donde empieza lo asíncrono.

## Indicadores y alertas

```
indicador_kpi     qué se mide
regla_cumplimiento → alerta_cumplimiento     qué dispara aviso
umbral_operativo  a partir de qué valor
```

Los que importan acá, y que se revisan sin que nadie los pida:

| Indicador | Por qué se mira |
| --- | --- |
| Encaje por día (`ratio_cobertura`) | Si baja de 1, el dinero de los clientes no está completo |
| Excepciones de conciliación abiertas | Bloquean el cierre diario |
| Reclamos vencidos y por vencer | Observación directa del supervisor |
| Reportes regulatorios por vencer | Igual |
| Alertas de monitoreo sin conclusión | Incumplimiento de `R-UIF-07` |
| Trabajos en cola muerta | Efectos que nunca ocurrieron y nadie notó |
| Planes de acción vencidos | Hallazgo garantizado en auditoría |

Un umbral de alerta es **dato con vigencia**, no una constante en el código.

## De incidente a pérdida

```
incidente_operativo / incidente_seguridad  →  evento_riesgo_operativo  →  plan_accion_riesgo
      qué falló                                  cuánto costó                cómo se evita
```

`evento_riesgo_operativo` es *append-only* y exige la taxonomía completa:
`categoria_evento` (seis), `factor_riesgo` (cinco), `linea_negocio`, las tres
fechas —ocurrencia, detección, contabilización—, `perdida_bruta`, `recuperacion` y
`causa_raiz`. La pérdida neta es generada.

| Regla | Por qué |
| --- | --- |
| Los casi-pérdida se registran con `perdida_bruta = 0` | La frecuencia también es información |
| La recuperación se agrega; la pérdida bruta **no se edita** | Es lo que hace comparable la base de pérdidas |
| Todo evento cierra con plan de acción, responsable y fecha | Un evento sin remediación es un evento que vuelve |
| El plan vencido escala a `hallazgo_auditoria` | Automático, sin que nadie lo decida |

## Incidentes de seguridad: tres relojes

Contención, reporte al organismo y notificación a titulares corren en paralelo y
**cada plazo se calcula al detectar y se guarda**. Con datos personales afectados,
el incidente no se cierra sin notificar (`R-SEG-05`). No se espera a conocer el
alcance exacto para notificar: se notifica con lo que se sabe y se actualiza.

## Retención

`politica_retencion` fija cuánto se conserva cada tipo de registro. Borrar antes
del plazo también es incumplir. Los registros que sostienen evidencia financiera se
conservan aunque el titular pida supresión.

## Checklist

- [ ] El flujo escribe en `bitacora_evento` con actor identificable.
- [ ] Si es asíncrono, la traza se hereda y no se corta.
- [ ] Los rechazos por restricción quedan registrados.
- [ ] Ningún dato sensible aparece en un registro.
- [ ] Los indicadores del flujo tienen umbral con vigencia y destinatario.
- [ ] Una falla con impacto monetario genera evento de riesgo con taxonomía completa.
- [ ] Todo evento tiene plan de acción con responsable y fecha.
- [ ] La cola muerta es visible en el backoffice, no solo en los registros.

## Ver también

`trabajos-outbox` · `seguridad-sesion-rls` · `errores-api` ·
`reclamos-consumidor` · CU-54, CU-55, CU-56 · familias `R-AUD` y `R-RIS`
