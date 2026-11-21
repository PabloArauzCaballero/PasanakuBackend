---
titulo: "Plantilla de solicitud de propuestas (RFP) para proveedores externos"
estado: borrador para aprobación de Pablo
fecha: 2026-11-21
origen: "PX0.M10 de PLAN-PROVEEDORES.md (plan de correcciones del 2026-11-21)"
tags: [proveedores, rfp, contratacion]
---

# Plantilla de solicitud de propuestas (RFP)

> **Cómo se usa.** Cada RFP de `docs/proveedores/rfp/` copia esta estructura y completa la sección 4 con
> los requisitos de su proveedor. Las secciones 1 a 3 y 5 a 9 son comunes y **no se recortan**: un proveedor
> que no puede responder una de ellas lo dice por escrito. La evaluación se hace con la
> [matriz de evaluación](./01-matriz-de-evaluacion.md).
>
> **Reglas de redacción** (decisiones de Pablo, tanda 8, y regla 00):
> - No se nombra ninguna empresa proveedora. El mercado se abre desde cero; no hay contactos previos.
> - No se cita ninguna norma que no esté en el repo. Lo normativo que falta se escribe «a confirmar por
>   Cumplimiento» y se pregunta al proveedor qué autorización tiene, sin presuponerla.
> - Ningún dato de personas, ningún secreto y ningún importe real en la RFP. Los volúmenes son estimaciones
>   que completa Pablo antes de enviar.

Los campos entre corchetes `[…]` los completa Pablo antes de enviar. Están listados en la sección 10.

---

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** (marca comercial [marca]), una aplicación de pasanaku
digital en Bolivia: grupos de personas que se conocen ahorran juntas, cada una paga una cuota por periodo y,
por turno, una de ellas recibe el pozo del periodo.

Principios del producto que el proveedor tiene que conocer porque condicionan la integración:

1. **El flujo solo avanza con pagos electrónicos confirmados por el proveedor.** Ninguna persona aprueba un
   paso del circuito a mano. Un pago no existe para nosotros hasta que llega su confirmación firmada.
2. **La empresa no pone dinero por quien no paga y no custodia el dinero del grupo en su cuenta.** Los
   fondos de usuarios y de cada grupo están en cuentas separadas de la cuenta operativa de la empresa.
3. **El ingreso de la empresa es una comisión de porcentaje único**, igual para todos, cobrada como línea
   aparte de la cuota.
4. **No operamos dinero en efectivo.** Los ingresos son electrónicos y las salidas van a una cuenta bancaria
   verificada del titular.
5. **Hoy no movemos dinero real.** La habilitación depende de la autorización regulatoria, que está en
   trámite. La contratación tiene que estar lista antes de la puesta en producción.

Servicio que se solicita en esta RFP: **[PX-n — nombre del servicio]**.

## 2. Calendario y forma de respuesta

| Hito | Fecha |
|---|---|
| Publicación de la RFP | [fecha] |
| Preguntas por escrito hasta | [fecha] |
| Respuestas a las preguntas (a todos los oferentes por igual) | [fecha] |
| Cierre de la recepción de propuestas | [fecha] |
| Demostración técnica en entorno de pruebas (preseleccionados) | [rango de fechas] |
| Comunicación del resultado | [fecha] |

- Las propuestas se envían a [canal de recepción]. Las preguntas y sus respuestas se comparten con todos
  los oferentes, sin identificar a quién preguntó.
- La propuesta responde **cada requisito por su código** (por ejemplo `RF-PX1-03`, `RM-04`) con una de estas
  respuestas: **Cumple** (cómo, con referencia a la documentación), **Cumple con desarrollo** (qué falta y
  en qué plazo), **No cumple**. Un requisito sin respuesta cuenta como «No cumple».
- La propuesta adjunta: documentación de la API, política de versionado, ejemplos de webhook, certificados
  vigentes, el SLA propuesto, el borrador de contrato y la tabla de precios.
- Confidencialidad: [Razón social de la empresa] firma un acuerdo de confidencialidad con quien lo pida antes
  de recibir la propuesta.

## 3. Contexto técnico de la integración

- Arquitectura de microservicios. Cada familia de proveedores se integra **detrás de un puerto propio**: un
  adaptador por proveedor, sin que la lógica de negocio dependa de un SDK. Se privilegia una API HTTP
  documentada y versionada sobre un SDK propietario.
- Operamos con **más de un proveedor por servicio** cuando el servicio es crítico, con un enrutador que
  reparte el tráfico según la salud medida de cada uno. El oferente tiene que aceptar recibir una porción del
  tráfico y competir por calidad.
- Toda operación lleva **nuestra clave de idempotencia**, que no cambia aunque reintentemos.
- Los importes se expresan en decimal exacto con su moneda ISO 4217 (`BOB`). No aceptamos importes en coma
  flotante.
- Fechas y horas en ISO 8601 con zona horaria. Zona de negocio: `America/La_Paz`.

## 4. Requisitos funcionales del servicio

> Se completa en cada RFP con la tabla de requisitos `RF-PXn-xx` de su sección, en lenguaje para el
> proveedor, y con lo que se le pide confirmar de cada uno.

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PXn-01 | … | … |

## 5. Requisitos comunes de integración (aplican a todo proveedor)

Origen: marco común de integración, requisitos RM-01 a RM-20 del plan de proveedores. Cada RFP indica cuáles
no aplican a su servicio y por qué; si no lo dice, aplican todos.

| Código | Requisito para el proveedor |
|---|---|
| RM-01 | API HTTP documentada (contrato publicado, por ejemplo OpenAPI) que permita integrar sin SDK propietario. Si hay SDK, es opcional |
| RM-02 | Declarar por escrito las capacidades: consulta de estado por clave, idempotencia, reembolso o reverso, montos mínimo y máximo por operación, monedas y horarios de operación |
| RM-03 | Aceptar **nuestra** clave de idempotencia por operación, devolverla en respuestas y webhooks, y garantizar que dos solicitudes con la misma clave producen un solo efecto |
| RM-04 | Webhooks firmados (HMAC o firma asimétrica) con marca de tiempo e identificador único de evento, reintento con backoff hasta recibir 2xx, y tolerancia a que respondamos 2xx en menos de 2 segundos y procesemos después |
| RM-05 | Los eventos permiten ordenar el ciclo de vida de una operación (marca de tiempo del proveedor o número de secuencia). Documentar qué pasa si llegan fuera de orden |
| RM-06 | API de consulta de estado de una operación por nuestra clave, para reconciliar operaciones sin webhook |
| RM-07 | Documentar el comportamiento ante un timeout nuestro: la operación puede haberse ejecutado y se consulta, nunca se duplica. Si el servicio no tiene consulta de estado, declararlo |
| RM-08 | Latencias comprometidas (percentil 95 y 99), límites de tasa y comportamiento al excederlos |
| RM-09 | Métricas o página de estado del servicio y aviso proactivo de incidentes y degradaciones |
| RM-10 | Aceptar una puesta en marcha gradual: tráfico bajo al principio, que sube según la calidad medida |
| RM-11 | Contrato con cláusulas de **confidencialidad**, **derecho de auditoría** y **continuidad**, y aprobar los casos obligatorios de prueba (sección 6) en un entorno de pruebas antes de la puesta en producción |
| RM-12 | Credenciales y llaves de firma rotables **sin cortar el servicio** (dos llaves válidas durante la ventana de rotación), entregadas por un canal seguro y nunca por correo |
| RM-13 | **Minimización de datos**: el proveedor pide solo lo que la operación necesita; los códigos de resultado y de rechazo están documentados en un catálogo cerrado y estable |
| RM-14 | Precio por operación detallado, sin costos ocultos, y reporte periódico de las operaciones facturadas para que podamos conciliar el costo |
| RM-15 | Al terminar el contrato, las operaciones históricas siguen consultables durante [plazo] para conciliación y auditoría |
| RM-16 | Identificador de correlación en cada respuesta y en el soporte; los registros del proveedor no guardan datos personales más allá de lo necesario |
| RM-17 | Entorno de pruebas (sandbox) donde se puedan provocar los casos de la sección 6: éxito, demora, rechazo, duplicado, firma inválida y caída |
| RM-18 | Contrato de API versionado con política de deprecación publicada (aviso mínimo de [plazo] antes de un cambio incompatible) |
| RM-19 | Plan de continuidad del servicio con RTO y RPO declarados y la evidencia de su última prueba. Aceptar que operemos con un segundo proveedor del mismo servicio |
| RM-20 | Los umbrales, límites y ventanas que nos afectan son configurables por nosotros (por API o por solicitud) sin desarrollo del proveedor |

## 6. Casos obligatorios de prueba

> Se completa en cada RFP con la tabla de tres niveles de su sección. El proveedor los ejecuta con nosotros en
> su entorno de pruebas **antes de firmar la puesta en producción**. Un caso no aprobado impide activar el
> servicio: nuestro sistema se niega por código a usar un proveedor sin contrato vigente y sin estos casos
> aprobados (RM-11).

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | … | … |
| Límite | … | … |
| Inválido | … | … |

## 7. Seguridad

| Código | Exigencia |
|---|---|
| SEG-01 | Certificaciones vigentes que apliquen al servicio (por ejemplo ISO/IEC 27001; PCI DSS solo si el servicio toca datos de tarjeta). Adjuntar certificado y alcance |
| SEG-02 | Transporte cifrado (TLS 1.2 o superior); autenticación mutua (mTLS) o credenciales de servicio con alcance acotado |
| SEG-03 | Firma de webhooks verificable en tiempo constante, con ventana temporal contra repeticiones |
| SEG-04 | Gestión de vulnerabilidades y pruebas de intrusión periódicas; resumen ejecutivo del último informe |
| SEG-05 | Respuesta a incidentes: punto de contacto 24×7, plazo de notificación de un incidente que nos afecte y de una brecha de datos personales [plazo a fijar en el contrato] |
| SEG-06 | Control de acceso de su personal a nuestros datos: mínimo privilegio, segundo factor y registro de accesos |
| SEG-07 | Ningún dato sensible (número de cuenta, documento, biometría, payload de un QR) en URLs, logs ni mensajes de error |

## 8. Datos personales y financieros

| Código | Exigencia |
|---|---|
| DAT-01 | Lista exacta de los datos que pide por operación y para qué. Se rechaza lo que no sea necesario |
| DAT-02 | **País de procesamiento** y de almacenamiento de cada dato, y suboperadores con su país |
| DAT-03 | Plazo de retención, borrado al vencer y certificación de borrado al terminar el contrato |
| DAT-04 | No usar nuestros datos para fines propios (entrenamiento de modelos, publicidad, venta o perfiles) |
| DAT-05 | Colaborar con los derechos del titular (acceso, rectificación, supresión) y con la conservación que exija la norma, sin decidir por su cuenta |
| DAT-06 | Notificar cualquier cambio de suboperador o de país de procesamiento con [plazo] de anticipación |

El repo trata como referencia de diseño los principios de protección de datos de `docs/Cumplimiento.md` §5.
Los requisitos legales concretos los confirma Cumplimiento.

## 9. Continuidad, disponibilidad, costo y salida

| Código | Exigencia |
|---|---|
| CON-01 | SLA de disponibilidad mensual comprometido, ventanas de mantenimiento con aviso previo y penalidades |
| CON-02 | Historial de incidentes de los últimos 12 meses (resumen) |
| CON-03 | Soporte: horarios, idioma español, canales y tiempos de respuesta por severidad |
| CON-04 | RTO y RPO del servicio (RM-19) y la fecha de la última prueba de continuidad |
| CON-05 | Costo: por operación, mínimos mensuales, costo de implementación y **costo de salida** |
| CON-06 | Salida: exportación de los datos en formato abierto, plazo de aviso de terminación y cooperación durante la migración a otro proveedor |
| CON-07 | Autorización regulatoria con la que presta el servicio en Bolivia, con su número y la autoridad que la emitió. Qué autorización corresponde la determina nuestro Cumplimiento |

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED` de Pablo)

| Campo | Dónde |
|---|---|
| Razón social, marca y canal de recepción | §1, §2 |
| Fechas del calendario | §2 |
| Volúmenes estimados por mes (operaciones, montos promedio) | §4 de cada RFP |
| Plazo de consulta posterior a la baja | RM-15 |
| Aviso mínimo de deprecación | RM-18 |
| Plazos de notificación de incidentes y de brechas | SEG-05 |
| Aviso de cambio de suboperador | DAT-06 |

## 11. Cómo se decide

1. Se descartan las propuestas que no cumplen un requisito **eliminatorio** (ver la matriz).
2. Las restantes se puntúan con la [matriz de evaluación](./01-matriz-de-evaluacion.md) y sus pesos.
3. Cumplimiento y el abogado revisan cláusulas, país de procesamiento y si hay que comunicar la contratación
   al organismo (proveedor crítico).
4. Se firma el contrato y se registra como contrato de tercero vigente; recién entonces se integra el
   adaptador real y se corren los casos de la sección 6 en el entorno de pruebas.
5. La activación empieza con tráfico bajo y sube según la salud medida (RM-10).
