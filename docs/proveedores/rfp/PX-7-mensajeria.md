---
titulo: "RFP PX-7 — Mensajería: SMS (principal y respaldo), notificaciones push y correo (sin WhatsApp)"
estado: lista para enviar una vez completados los campos [ … ] (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-7 y §PX-0; decisión de Pablo, tanda 8; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, notificaciones, sms, push, correo]
---

# Solicitud de propuestas PX-7 — Mensajería

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia.
Necesitamos enviar mensajes a nuestros usuarios por **tres canales: SMS, notificaciones push y correo
electrónico. Sin WhatsApp: ese canal no se contrata ni se integra.** Hoy no movemos dinero real.

La RFP se divide en **cuatro lotes**. Se puede ofertar uno o varios, pero **el lote B no puede adjudicarse al
mismo proveedor que el lote A** (el respaldo de los códigos de verificación tiene que ser independiente):

| Lote | Servicio | Para qué |
|---|---|---|
| A | SMS principal | Códigos de verificación (OTP) y avisos críticos a quien no tiene la app abierta |
| B | SMS de respaldo | El OTP cuando el lote A falla (la otra vía de respaldo es el correo verificado) |
| C | Notificaciones push | Recordatorios, invitaciones al grupo, enlace de pago, subida de nivel, rachas y «Grupo perfecto» |
| D | Correo electrónico | Comprobantes, facturas y avisos formales con plazo (mora, descargo y apelación) |

Volumen estimado por lote: [mensajes por mes y pico por hora, por lote].

## 2. Calendario y forma de respuesta

| Hito | Fecha |
|---|---|
| Publicación | [fecha] |
| Preguntas por escrito hasta | [fecha] |
| Cierre de propuestas | [fecha] |
| Demostración en entorno de pruebas | [rango] |
| Resultado | [fecha] |

Se responde cada código, **por lote**, con **Cumple** / **Cumple con desarrollo** / **No cumple**.
Propuestas a [canal].

## 3. Contexto técnico

- Integración por API detrás del puerto de envío de nuestro servicio de notificaciones, que enruta por canal y
  despacha. Cada envío lleva **nuestra clave de idempotencia por evento, destinatario y canal**.
- Las plantillas son nuestras, versionadas; los textos los redacta nuestro equipo y los aprueba la dirección.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Lotes | Qué tiene que demostrar el proveedor |
|---|---|---|---|
| RF-PX7-01 | **Un envío por evento, destinatario y canal**; la clave de idempotencia es del envío, no del proveedor | A, B, C, D | Aceptar nuestra clave y no enviar dos veces con la misma |
| RF-PX7-02 | Mensajes por propósito: OTP (principal y respaldo), invitación al grupo (enlace con token de un solo uso), recordatorio antes del vencimiento, enlace de pago rápido, aviso de mora y de descargo con plazos, subida de nivel y «Grupo perfecto» | según la tabla de la sección 1 | Soporte de los tipos de mensaje y prioridades (el OTP tiene prioridad) |
| RF-PX7-03 | **Consentimiento** por canal y por propósito, y horarios y frecuencia acotados para recordatorios y cobranza (lo controlamos nosotros) | A, B, C, D | Listas de supresión, gestión de bajas y su sincronización con nosotros |
| RF-PX7-04 | **Contenido mínimo**: el texto no lleva montos atribuibles a una persona ni datos de terceros; los detalles se ven dentro de la app | A, B, C, D | Que el proveedor no agrega contenido ni enriquece el mensaje |
| RF-PX7-05 | Plantillas versionadas; se aprueban ante el canal **solo cuando el canal lo exige** (por ejemplo, remitentes de SMS registrados si la norma lo pide). Los textos los redacta y aprueba nuestro equipo | A, B, D | Registro de remitente o identificador de SMS y dominio de correo (SPF, DKIM, DMARC) |
| RF-PX7-06 | Si falla un canal, no se aborta el resto ni se duplican los envíos que ya salieron; **el OTP conmuta** del SMS principal al de respaldo o al correo verificado | A, B, D | Estados de entrega rápidos y confiables para decidir la conmutación |
| RF-PX7-07 | **Webhooks de estado de entrega firmados** (entregado, demorado, rechazado), que recibimos por nuestro ingreso común de webhooks | A, B, C, D | Firma, marca de tiempo e id de evento; reintentos hasta 2xx |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-7 |
|---|---|---|
| RM-01 | Sí | API documentada por canal |
| RM-02 | Sí | Declarar operadores cubiertos (SMS), plataformas (push: Android e iOS), tamaño máximo y tasas |
| RM-03 | Sí — **eliminatorio** | Nuestra clave por envío |
| RM-04 | Sí — **eliminatorio** | Webhooks de estado firmados |
| RM-05 | Sí | Si llegan «entregado» y «rechazado» del mismo envío, manda el último acuse |
| RM-06 | Sí | Consulta del estado de un envío por nuestra clave |
| RM-07 | Sí | Un timeout al enviar no produce un segundo envío con la misma clave |
| RM-08 | Sí | Latencia de entrega del OTP (percentiles) y límites de tasa |
| RM-09 | Sí | Página de estado y aviso de incidentes por operador |
| RM-10 | Sí | Arranque con porción baja de tráfico |
| RM-11 | Sí | Cláusulas y casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales y de llaves de firma sin corte |
| RM-13 | Sí | Solo número de teléfono, token de push o correo, y el texto |
| RM-14 | Sí | Costo por mensaje por operador y por canal |
| RM-15 | Sí | Estados de entrega históricos consultables después de la baja |
| RM-16 | Sí | Id de correlación; sin el contenido del mensaje en logs del proveedor más allá de lo necesario |
| RM-17 | Sí | Sandbox que permita provocar entregado, demorado, rechazado y webhook duplicado |
| RM-18 | Sí | Versionado y deprecación |
| RM-19 | Sí | RTO/RPO; el lote B existe precisamente como continuidad del OTP |
| RM-20 | Sí | Tasas, ventanas horarias y reintentos configurables por nosotros |

## 6. Casos obligatorios de prueba

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Mensaje entregado con webhook firmado | Un envío, estado «entregado» |
| Límite | Mensaje demorado más allá del umbral del OTP | Se conmuta al SMS de respaldo o al correo verificado, sin repetir el envío original |
| Límite | Webhook de estado duplicado | Un solo registro de estado |
| Límite | Mismo evento enviado dos veces con la misma clave | Un solo mensaje |
| Inválido | Mensaje rechazado por el operador | Estado «rechazado»; el resto de los canales sigue |
| Inválido | Webhook con firma inválida | Sin efecto |
| Inválido | Proveedor del lote A caído | El OTP sale por el lote B o por correo |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: protección contra fraude de tráfico de SMS
(bombeo artificial), límites por número y por destino, y alertas de picos anómalos; el código OTP nunca queda
legible en logs ni en paneles del proveedor más allá de lo necesario para entregarlo.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A. Para este servicio además: el proveedor no usa los números, tokens ni
correos para otros fines ni los comparte; el contenido de los mensajes se conserva el mínimo tiempo necesario.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además: si un canal falla, los demás siguen (no
dependemos de un solo proveedor para el OTP: lotes A y B, más correo).

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED` de Pablo)

Razón social, marca y canal · fechas · volumen por lote · plazos abiertos del Anexo A · si la norma exige
registrar remitentes de SMS (lo confirma Cumplimiento).
