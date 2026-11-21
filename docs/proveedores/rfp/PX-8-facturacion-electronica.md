---
titulo: "RFP PX-8 — Proveedor de facturación electrónica autorizado (factura de la comisión)"
estado: lista para enviar una vez completados los campos [ … ] (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-8 y §PX-0; decisión de Pablo, tanda 8; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, facturacion]
---

# Solicitud de propuestas PX-8 — Facturación electrónica de la comisión

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia. La
empresa cobra **una comisión de porcentaje único** por servicio, como línea aparte en el mismo QR de la cuota,
y tiene que facturarla. Buscamos un **proveedor de facturación electrónica autorizado** que emita esas
facturas: **no nos integramos directo con la administración tributaria**. Hoy no movemos dinero real.

Lo que el repo registra sobre la facturación en línea (`docs/Cumplimiento.md` §4) y que el oferente tiene que
cubrir: código único de facturación diario (CUFD, vigencia de 24 horas extensible hasta 72 en ciertos casos),
código único por documento (CUF), firma digital en la modalidad en línea y registro de eventos significativos
con plazo de hasta 48 horas tras concluir una contingencia. El detalle normativo lo confirma nuestro
Cumplimiento.

Volumen estimado: [facturas por mes], [pico diario].

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

- Integración por API detrás de un puerto propio de nuestro servicio de tarifas.
- La emisión la dispara un **evento de cobro confirmado** por la pasarela; nunca una marca manual.
- Importes en decimal exacto con moneda `BOB`.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PX8-01 | **Una factura por cobro de comisión confirmado**, idempotente por cobro (nuestra clave) | API de emisión idempotente; misma clave → misma factura |
| RF-PX8-02 | **Anulación o nota de crédito** cuando la comisión se reembolsa o se revierte (nosotros registramos su asiento) | API de anulación y de nota de crédito, con su resultado firmado |
| RF-PX8-03 | **Contingencia**: si el servicio fiscal no responde, se registra el evento significativo y las facturas se regularizan dentro del plazo que fija la norma (el repo registra hasta 48 h tras concluir la contingencia; lo confirma Cumplimiento) | Emisión fuera de línea, registro de inicio y fin del evento, envío posterior en lote y aviso de su resultado |
| RF-PX8-04 | Los datos fiscales del participante (NIT u otro) se piden **solo si la norma lo exige**, con un valor por defecto para consumidor final, según lo que confirme Cumplimiento | Emisión a consumidor final sin datos adicionales; cómo se cargan datos fiscales opcionales |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-8 |
|---|---|---|
| RM-01 | Sí | API documentada |
| RM-02 | Sí | Declarar modalidad (en línea, contingencia), tipos de documento y consulta de estado |
| RM-03 | Sí — **eliminatorio** | Nuestra clave por factura y por anulación |
| RM-04 | Sí | Webhook firmado del estado fiscal (validada, rechazada, en contingencia) |
| RM-05 | Sí | Una factura emitida en contingencia y luego validada informa el último estado |
| RM-06 | Sí | Consulta del estado fiscal por nuestra clave |
| RM-07 | Sí | Un timeout nunca produce dos facturas por el mismo cobro |
| RM-08 | Sí | Latencia de emisión |
| RM-09 | Sí | Aviso de caídas del servicio fiscal y del propio |
| RM-10 | No aplica | Un solo proveedor de facturación; no hay reparto de tráfico |
| RM-11 | Sí | Cláusulas y casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales y custodia de la firma digital sin corte |
| RM-13 | Sí | Solo los datos fiscales que la norma exige |
| RM-14 | Sí | Costo por factura, por anulación y mínimos |
| RM-15 | Sí | Facturas históricas consultables y descargables durante el plazo de conservación |
| RM-16 | Sí | Id de correlación |
| RM-17 | Sí | Entorno de pruebas fiscal que permita provocar rechazo y contingencia |
| RM-18 | Sí | Versionado y aviso de cambios de la norma que afecten la API |
| RM-19 | Sí | RTO/RPO y plan de continuidad |
| RM-20 | Sí | Punto de venta, sucursal y parámetros fiscales configurables por nosotros |

## 6. Casos obligatorios de prueba

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Cobro de comisión confirmado | Una factura emitida y validada |
| Límite | El mismo evento de cobro recibido dos veces | Una sola factura |
| Límite | Comisión revertida después de facturada | Anulación o nota de crédito |
| Inválido | Rechazo fiscal (datos inválidos) | Estado «rechazada» con motivo; se corrige y se reemite sin duplicar |
| Inválido | Servicio fiscal caído | Emisión en contingencia, evento significativo registrado y regularización en plazo |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: custodia de la firma digital de la empresa
(dónde, quién accede, cómo se revoca).

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además: exportación de todas las facturas emitidas en
formato abierto al terminar el contrato, sin cortar la numeración.

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED`)

Razón social, marca y canal · fechas · volumen estimado · plazos abiertos del Anexo A · confirmación de
Cumplimiento sobre datos fiscales del participante (RF-PX8-04) y contingencia (RF-PX8-03).
