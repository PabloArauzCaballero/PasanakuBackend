---
titulo: "RFP PX-6 — Verificación de titularidad de la cuenta bancaria"
estado: lista para enviar una vez completados los campos [ … ] (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-6 y §PX-0; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, cuentas, titularidad]
---

# Solicitud de propuestas PX-6 — Verificación de titularidad de la cuenta bancaria

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

> Este servicio puede ofrecerlo un proveedor específico, el mismo proveedor de desembolso (PX-2) o el banco
> custodio (PX-3). La elección del medio se decide junto con esas dos RFP (PX6.M03, `DECISION_REQUIRED`). Los
> oferentes de PX-2 y PX-3 pueden responder esta RFP en la misma propuesta.

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia.
Antes de enviar dinero a una cuenta bancaria, verificamos que **el titular de la cuenta es la persona cuya
identidad validamos** (KYC). Sin esa verificación no hay desembolso. Hoy no movemos dinero real.

Volumen estimado: [cuentas nuevas o cambiadas por mes].

## 2. Calendario y forma de respuesta

| Hito | Fecha |
|---|---|
| Publicación | [fecha] |
| Preguntas por escrito hasta | [fecha] |
| Cierre de propuestas | [fecha] |
| Demostración en entorno de pruebas | [rango] |
| Resultado | [fecha] |

Se responde cada código con **Cumple** / **Cumple con desarrollo** / **No cumple**. Propuestas a [canal].

## 3. Contexto técnico

- Integración por API detrás de un puerto propio.
- Guardamos el número de cuenta cifrado, lo buscamos por un hash con *pepper* y lo mostramos enmascarado.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PX6-01 | Antes de habilitar una cuenta de destino, verificar que **el titular coincide** con la identidad validada, por el medio que ofrezca el proveedor o el banco (validación de titular o micro-depósito) | Medio ofrecido, bancos cubiertos, qué compara (nombre, documento), resultado (coincide / no coincide / no verificable) y tiempo de respuesta |
| RF-PX6-02 | Nosotros guardamos la cuenta **cifrada** con su versión de llave, la buscamos por hash con *pepper* y la mostramos enmascarada; el número que deduplicamos es el mismo que ciframos | Que el proveedor no nos obliga a enviar ni a guardar la cuenta en claro fuera de la operación; formato normalizado del número de cuenta que devuelve |
| RF-PX6-03 | Alta o cambio de cuenta → enfriamiento (plazo que fijamos) y aviso por otro canal; un cambio de cuenta seguido de un pedido urgente de desembolso es señal de alerta | Si el proveedor informa señales de riesgo de la cuenta (cuenta recién abierta, cuenta reportada) |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-6 |
|---|---|---|
| RM-01 | Sí | API documentada |
| RM-02 | Sí | Declarar medio de verificación, bancos cubiertos y consulta de estado |
| RM-03 | Sí | Nuestra clave por verificación |
| RM-04 | Sí | Webhook firmado si la verificación es asíncrona (por ejemplo, micro-depósito) |
| RM-05 | Sí | Secuencia de estados de una verificación asíncrona |
| RM-06 | Sí | Consulta de estado por nuestra clave |
| RM-07 | Sí | Timeout: la cuenta queda sin verificar; nunca verificada por defecto |
| RM-08 | Sí | Latencia de la verificación síncrona |
| RM-09 | Sí | Aviso de incidentes |
| RM-10 | Sí | Arranque gradual |
| RM-11 | Sí | Cláusulas y casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales sin corte |
| RM-13 | Sí | Solo número de cuenta, banco y la identidad mínima para comparar |
| RM-14 | Sí | Costo por verificación y por micro-depósito |
| RM-15 | Sí | Resultados históricos consultables después de la baja |
| RM-16 | Sí | Id de correlación, sin números de cuenta en logs |
| RM-17 | Sí | Sandbox con cuentas sintéticas para cada resultado |
| RM-18 | Sí | Versionado y deprecación |
| RM-19 | Sí | RTO/RPO y plan de continuidad |
| RM-20 | Sí | Criterio de coincidencia de nombre configurable por nosotros |

## 6. Casos obligatorios de prueba

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | El titular coincide con la identidad validada | Cuenta verificada; empieza el enfriamiento |
| Límite | Nombre con variaciones (orden de apellidos, acentos, abreviaturas) | Resultado según el criterio de coincidencia declarado, sin ambigüedad |
| Límite | Cambio de cuenta seguido de un pedido de desembolso | Bloqueado por enfriamiento y alerta |
| Inválido | El titular no coincide | Cuenta rechazada con motivo |
| Inválido | No verificable (banco no cubierto o proveedor caído) | Cuenta sin verificar; no se desembolsa |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: el número de cuenta nunca viaja en la URL.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A. Para este servicio además: el proveedor no conserva el número de cuenta
después de responder, salvo lo que exija la norma, y lo declara.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A.

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED`)

Razón social, marca y canal · fechas · volumen estimado · plazos abiertos del Anexo A · elección del medio
junto con PX-2 o PX-3 (PX6.M03).
