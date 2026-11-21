---
titulo: "RFP PX-4 — Verificación de identidad (documento, prueba de vida y comparación biométrica)"
estado: lista para enviar una vez completados los campos [ … ] (sección 10); tipos de documento según L-06
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-4 y §PX-0; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, kyc, identidad]
---

# Solicitud de propuestas PX-4 — Verificación de identidad (KYC)

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia. El
alta de cada usuario se valida de forma **automática** por un proveedor: captura del documento de identidad,
prueba de vida y comparación del rostro con el documento. Una persona de nuestro equipo de Cumplimiento revisa
**solo** las excepciones y los casos de personas expuestas políticamente (PEP). Hoy no movemos dinero real.

Usuarios estimados: [altas por mes], [pico diario]. Dispositivos: mayormente Android de gama media y baja,
con cámaras modestas y conexiones lentas.

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

- App móvil Flutter (Android primero) y backend propio. La captura puede hacerse con el componente del
  proveedor o con el nuestro; el resultado llega a nuestro backend por webhook firmado o por consulta.
- La app no consulta el estado cada pocos segundos: escucha el cambio de estado (evento o notificación push).

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PX4-01 | **Niveles de verificación** atados a acciones y comprobados en nuestro servidor: 0 registrado (ser invitado), 2 identificado (tomar cupo y aportar), 3 cobrable (cuenta verificada, recibir la entrega). El nivel mínimo por acción lo fijamos nosotros | Que el resultado permite asignar el nivel 2 sin intervención humana |
| RF-PX4-02 | Documento de un **catálogo cerrado** de tipos admitidos (los define nuestro Cumplimiento, pregunta L-06), con su número **normalizado** y único | Tipos de documento bolivianos que lee; extracción del número y su forma normalizada |
| RF-PX4-03 | **Prueba de vida y comparación con el documento** hechas por el proveedor; guardamos la evidencia y el **puntaje**, no solo la decisión | Puntajes devueltos, su escala, el umbral recomendado y la tasa de falsos positivos y negativos medida |
| RF-PX4-04 | Estados: iniciado → en revisión automática → {aprobado, rechazado, revisión manual} y vencido. A revisión manual van **solo** excepciones y PEP | Que el proveedor distingue «dudoso» de «rechazado» y devuelve el motivo |
| RF-PX4-05 | Rechazo con **motivo de un catálogo cerrado** y camino de reintento; los mensajes no revelan los criterios antifraude | Catálogo de motivos de rechazo y política de cambios |
| RF-PX4-06 | **Imágenes y biometría**, el dato más sensible: cifradas, nunca accesibles por URL adivinable, acceso auditado y retención declarada. Preferimos que el proveedor **no nos las devuelva**: guardamos solo la referencia y el resultado | Dónde quedan las imágenes y plantillas biométricas, cifrado, retención, borrado y país de procesamiento |
| RF-PX4-07 | Sospecha de **duplicado o multicuenta**: abre una revisión, no un bloqueo automático | Señal de duplicado (mismo rostro o documento en otra alta) y cómo se informa |
| RF-PX4-08 | **Vencimiento del documento** con aviso previo, nunca de golpe el día del cobro | Fecha de vencimiento extraída del documento |
| RF-PX4-09 | La app **escucha el cambio de estado** (evento o push); no consulta cada 30 segundos | Webhook firmado de cambio de estado |
| RF-PX4-10 | Funciona con **cámaras malas y conexiones lentas**: reintentos, ayuda contextual y vía de revisión manual | Tamaño del componente, consumo de datos por intento, comportamiento sin conexión, accesibilidad del componente de captura |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-4 |
|---|---|---|
| RM-01 | Sí | API documentada; componente de captura opcional y reemplazable |
| RM-02 | Sí | Declarar tipos de documento, idiomas, consulta de estado y límites |
| RM-03 | Sí | Nuestra clave por verificación: reintentar no crea dos verificaciones cobradas |
| RM-04 | Sí | Webhook firmado del resultado |
| RM-05 | Sí | Si cambia el resultado (por ejemplo, de dudoso a aprobado), manda el último |
| RM-06 | Sí | Consulta del resultado por nuestra clave |
| RM-07 | Sí | Proveedor caído: el alta queda pendiente con mensaje claro, sin bloquear la cuenta |
| RM-08 | Sí | Tiempo de respuesta de la decisión automática (percentiles) |
| RM-09 | Sí | Página de estado y aviso de incidentes |
| RM-10 | Sí | Porción de altas al principio |
| RM-11 | Sí | Cláusulas y casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales sin corte |
| RM-13 | Sí | Solo lo necesario para verificar; sin uso de las imágenes para otros fines |
| RM-14 | Sí | Costo por verificación completa, por reintento y por revisión |
| RM-15 | Sí | Consulta de resultados históricos (sin imágenes) después de la baja |
| RM-16 | Sí | Id de correlación, sin datos personales en logs |
| RM-17 | Sí | Sandbox con documentos y rostros sintéticos para provocar cada caso |
| RM-18 | Sí | Versionado y deprecación |
| RM-19 | Sí | RTO/RPO y plan de continuidad |
| RM-20 | Sí | Umbral de puntaje configurable por nosotros |

## 6. Casos obligatorios de prueba

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Documento válido, prueba de vida superada, puntaje alto | Aprobado sin persona |
| Límite | Puntaje justo en el umbral | Va a revisión manual |
| Límite | Documento por vencer | Aprobado con aviso de vencimiento |
| Inválido | Documento ya usado en otra alta | Revisión por duplicado, sin bloqueo automático |
| Inválido | Prueba de vida fallida | Rechazo con motivo y camino de reintento |
| Inválido | Proveedor caído | Alta pendiente con mensaje claro, cuenta sin bloquear |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: detección de ataques de presentación
(fotos de pantalla, máscaras, video inyectado) con su método y la evidencia de evaluación independiente si la
tiene; firma o atestación del componente de captura para que la imagen no se reemplace en el dispositivo.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A, con especial cuidado: **la biometría es el dato más sensible que
tratamos**. El país de procesamiento y de almacenamiento de imágenes y plantillas biométricas se declara por
escrito y entra al contrato. El proveedor no usa las imágenes para entrenar modelos ni para otro cliente.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además: al terminar el contrato, borrado certificado de
imágenes y biometría, y exportación de los resultados (sin imágenes) en formato abierto.

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED` de Pablo)

Razón social, marca y canal · fechas · volumen estimado · plazos abiertos del Anexo A · **tipos de documento
admitidos según la respuesta a L-06**.
