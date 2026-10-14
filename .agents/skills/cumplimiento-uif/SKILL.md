---
name: cumplimiento-uif
description: "Implementar la prevención de legitimación de ganancias ilícitas en AportaYa: umbrales PCC-01 y ROG con ventana acumulada, debida diligencia por riesgo, monitoreo, alertas, casos y reporte de operación sospechosa. Úsala en cualquier flujo que acredite o retire dinero, al tocar umbrales, al escribir reglas de monitoreo, o cuando aparezca la palabra PEP, ROS, ROG o UIF."
---

# Cumplimiento UIF

Tres obligaciones distintas que se confunden todo el tiempo:

| Obligación | Se dispara por | Tabla | Se avisa al cliente |
| --- | --- | --- | --- |
| **Formulario por umbral** (PCC-01, ROG) | Un monto objetivo | `registro_operacion_relevante` | Sí, se le pide la declaración |
| **Debida diligencia** | El perfil de riesgo del cliente | `debida_diligencia`, `calificacion_riesgo_cliente` | Sí, se le piden documentos |
| **Reporte de operación sospechosa** | El criterio del oficial de cumplimiento | `reporte_operacion_sospechosa` | **Nunca.** Rige el deber de reserva |

Mezclarlas produce el peor error posible del módulo: avisarle al cliente que se lo
está reportando.

## Los umbrales son datos, no código

```
umbral_reporte_uif
  formulario            'PCC-01' | 'ROG-01'…'ROG-04'
  concepto_operacion    a qué operación aplica
  umbral_usd            el monto objetivo
  es_acumulado          individual o suma de ventana
  ventana_dias_calendario
  vigente_desde / vigente_hasta
  base_normativa        la cita del artículo
```

**Jamás** un umbral dentro de un `CHECK` ni de un `if`. Cuando la UIF cambia el
número, cambia una fila —no se despliega. Las filas viejas no se borran: hay que
poder explicar qué umbral regía el día de una operación de hace dos años.

## La ventana acumulada, que es donde todos se equivocan

```
La ventana empieza en la operación SIGUIENTE a la última que alcanzó el umbral,
y avanza como máximo ventana_dias_calendario.
```

Cuando el acumulado alcanza el umbral, el formulario corresponde **a la última
operación que lo alcanza** —no a todas las de la ventana—, y esa operación marca
el inicio de la ventana siguiente. Se guardan `ventana_desde`, `ventana_hasta`,
`operacion_inicio_ventana_id` y `monto_acumulado_ventana` para poder reproducir el
cálculo sin recalcularlo.

## Convertir a dólares: se guarda la cotización

Los umbrales están en dólares y las operaciones en bolivianos. Se convierte con
`tipo_cambio` del día y **se persiste** `tipo_cambio_aplicado`,
`monto_equivalente_usd` y `umbral_aplicado_usd` en el registro (`R-UIF-04`).
Recalcular después con la cotización de hoy da otro número y destruye la
evidencia. Si no hay cotización del día, la operación se rechaza: es preferible a
un registro irreproducible.

## Exentos se registran igual

Operativa propia entre entidades reguladas, pago con tarjeta, bonos sociales,
servicios básicos, impuestos, tasas y regalías van con `exento=true` y
`motivo_exencion`. **No se omiten**: la ausencia de fila es indistinguible de un
olvido, y así es como se lee en una inspección.

## Debida diligencia por riesgo

| Nivel | Cuándo | Qué exige de más |
| --- | --- | --- |
| `SIMPLIFICADA` | Riesgo bajo según `matriz_riesgo_lft` | — |
| `ESTANDAR` | Caso general | Identidad verificada y perfil transaccional |
| `REFORZADA` | PEP, país de riesgo, actividad sensible, alerta previa | **Segunda revisión independiente** (`R-UIF-10`) y aprobación de nivel superior |

Persona expuesta políticamente (`declaracion_pep`) implica reforzada siempre, y el
alcance se extiende a `beneficiario_final`. La revisión periódica
(`revision_periodica_kyc`) tiene fecha y vence: un cliente reforzado sin revisión
al día es un hallazgo.

## Monitoreo, alerta, caso, reporte

```
regla_monitoreo_lft   → alerta_monitoreo_lft → caso_investigacion_lft → reporte_operacion_sospechosa
   la tipología, dato       lo que saltó          el expediente             el ROS
```

Reglas del circuito:

1. **Ninguna alerta se cierra sin conclusión escrita** (`R-UIF-07`). "Falso
   positivo" no es una conclusión; el motivo sí.
2. **Quien analiza no revisa.** El revisor debe ser distinto del analista.
3. **El caso tiene plazo** calculado y guardado según severidad.
4. **Decidir reportar exige narrativa y tipología**, no un checkbox.
5. **Reserva absoluta**: el flujo no genera notificación al titular, ni pista
   indirecta (un bloqueo se explica como medida operativa, sin motivo de
   inteligencia financiera).
6. Información nueva sobre un caso cerrado abre un **caso nuevo enlazado**; el
   cerrado no se reescribe.

## Listas restrictivas

`lista_restrictiva_externa` + `coincidencia_lista`: la coincidencia se registra
con su puntaje y su resolución (verdadera o descartada, con quién y cuándo). Una
coincidencia descartada sin firma es lo mismo que no haber mirado.

## Remisión mensual

Los registros del período se agrupan por `periodo_remision` y salen en el envío
mensual (`reporte_regulatorio` + `envio_regulatorio`), con acuse guardado. Ver
skill `reportes-regulatorios`.

## Checklist

- [ ] Ningún umbral, plazo ni porcentaje escrito en el código.
- [ ] La ventana acumulada se guarda completa y es reproducible.
- [ ] Tipo de cambio persistido junto al equivalente en dólares.
- [ ] Exentos registrados con motivo.
- [ ] Reforzada exige segunda revisión independiente, probada.
- [ ] Cierre de alerta sin conclusión: **rechazado por la base**.
- [ ] Ningún camino del código notifica al titular de un ROS. Hay prueba de eso.
- [ ] El acceso al expediente queda en `registro_acceso_datos`.

## Ver también

`norma-nueva` · `semillas-catalogos` · `reportes-regulatorios` ·
`seguridad-sesion-rls` · CU-40 a CU-46 · familia `R-UIF` de `docs/Restricciones.md` ·
`docs/Cumplimiento.md`
