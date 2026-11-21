---
titulo: "RFP PX-2 — Proveedor de desembolso / transferencia saliente (principal y segundo proveedor)"
estado: lista para enviar una vez completados los campos [ … ] (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-2 y §PX-0; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, desembolsos]
---

# Solicitud de propuestas PX-2 — Desembolso (transferencia saliente)

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia.
Necesitamos enviar transferencias salientes **a cuentas bancarias verificadas** de nuestros usuarios, de forma
automática y confirmada por el proveedor: ninguna persona marca a mano una transferencia como acreditada. Hoy
no movemos dinero real; la contratación tiene que estar lista antes de la puesta en producción.

**Buscamos dos proveedores de este servicio. Un segundo proveedor contratado es obligatorio** antes de la
puesta en producción. El oferente indica si se postula como principal, segundo o cualquiera de los dos.

Destinos de las transferencias:
- Al beneficiario del turno: la entrega del pozo.
- Al titular: retiros de su billetera.
- A cada participante: la devolución de su aporte al fondo del grupo al cierre del ciclo.
- A un comercio aliado verificado (piloto futuro, sujeto a opinión legal).

Volumen estimado: [transferencias por mes], [monto promedio en BOB], [pico diario].

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

- Integración por API HTTP detrás de un puerto propio. Toda orden lleva **nuestra clave de idempotencia**, que
  viaja al proveedor y no cambia aunque reintentemos o conmutemos de proveedor.
- Máquina de estados de nuestro lado con un punto de no retorno: una orden enviada al proveedor nunca se
  reenvía a otro proveedor mientras su estado sea desconocido.
- Importes en decimal exacto con moneda `BOB`; fechas ISO 8601 con zona horaria.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PX2-01 | **Una orden por operación** con nuestra clave de idempotencia única, que **viaja al proveedor**; reintentar la creación devuelve la misma orden y nunca una segunda transferencia | API de creación idempotente por nuestra clave y su comportamiento ante la clave repetida |
| RF-PX2-02 | Solo transferimos hacia una **cuenta verificada** y fuera del enfriamiento posterior a un cambio de cuenta (lo controlamos nosotros; el enfriamiento no se saltea por pedido de soporte) | Si el proveedor ofrece validación de cuenta o de titular previa, cómo funciona |
| RF-PX2-03 | La acreditación se confirma **solo** por webhook firmado o por consulta de estado; nunca la marca una persona | Webhook firmado de estado final y API de consulta |
| RF-PX2-04 | **Rechazo bancario** (cuenta cerrada, datos erróneos): el dinero vuelve, registramos la incidencia, corregimos y reintentamos con una **orden nueva** | Códigos de rechazo, plazo de devolución del dinero y su notificación |
| RF-PX2-05 | **Un timeout es estado desconocido**: consultamos por nuestra clave. Sin consulta de estado no hay reintento automático | API de consulta de estado por nuestra clave (eliminatorio) |
| RF-PX2-06 | Destinos: beneficiario del turno, titular, cada participante del grupo y comercio aliado | Bancos y tipos de cuenta alcanzables en Bolivia |
| RF-PX2-07 | **Límites por operación y por día** que fijamos nosotros, y alertas de patrón (misma cuenta para varios beneficiarios, cambio de cuenta reciente, monto fuera del histórico) conectadas a nuestro monitoreo de prevención de lavado | Límites configurables; qué datos de la operación devuelve para nuestro monitoreo |
| RF-PX2-08 | Ante un cambio de cuenta de destino avisamos al usuario por un canal distinto al que hizo el cambio | Ninguno del proveedor (lo hace nuestro servicio de mensajería); se declara para que conozca el flujo |
| RF-PX2-09 | Pedimos al beneficiario que confirme la recepción; si falta después de N días, hacemos seguimiento, sin bloquear la contabilidad | Comprobante o referencia bancaria de la transferencia que podamos mostrarle al beneficiario |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-2 |
|---|---|---|
| RM-01 | Sí | API HTTP documentada |
| RM-02 | Sí | Declarar consulta de estado, idempotencia, montos mínimo y máximo, horarios de procesamiento y corte |
| RM-03 | Sí — **eliminatorio** | Nuestra clave de idempotencia por orden |
| RM-04 | Sí — **eliminatorio** | Webhook firmado de estado final |
| RM-05 | Sí | El webhook puede llegar antes que la respuesta HTTP; documentar la secuencia de estados |
| RM-06 | Sí — **eliminatorio** | Consulta de estado por nuestra clave |
| RM-07 | Sí — **eliminatorio** | Un timeout nunca termina en dos transferencias |
| RM-08 | Sí | Latencia de aceptación y plazo de acreditación por banco destino |
| RM-09 | Sí | Página de estado y aviso de incidentes y de demoras bancarias |
| RM-10 | Sí | Arranque con porción baja de tráfico |
| RM-11 | Sí | Cláusulas y casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales y llave de firma sin corte |
| RM-13 | Sí | Solo los datos de la cuenta destino y del titular que la transferencia exige |
| RM-14 | Sí | Costo por transferencia, por rechazo y por devolución |
| RM-15 | Sí | Consulta histórica después de la baja |
| RM-16 | Sí | Id de correlación |
| RM-17 | Sí | Sandbox con timeout programable, rechazo bancario y webhook adelantado |
| RM-18 | Sí | Versionado y deprecación |
| RM-19 | Sí — **segundo proveedor obligatorio** | RTO/RPO; convivir con otro proveedor; las órdenes nuevas pueden ir al otro |
| RM-20 | Sí | Límites y umbrales configurables por nosotros |

## 6. Casos obligatorios de prueba

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Desembolso acreditado por webhook firmado | Un solo asiento |
| Límite | La misma orden enviada dos veces | Una sola transferencia |
| Límite | El webhook llega antes de la respuesta HTTP | Estado final correcto, sin duplicar |
| Límite | Una cuenta que sale del enfriamiento justo a tiempo | Se transfiere |
| Inválido | Timeout sin respuesta | Consulta de estado, sin duplicar |
| Inválido | Rechazo bancario | Dinero devuelto; reintento con orden nueva |
| Inválido | Firma inválida en el webhook | Sin efecto |
| Inválido | Cuenta en enfriamiento | Bloqueado de nuestro lado con motivo (no llega al proveedor) |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: autenticación mutua (mTLS) o firma de las
solicitudes de creación de órdenes; el número de cuenta destino nunca aparece en URLs ni en logs.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A. Declarar si el proveedor conserva el número de cuenta destino y por cuánto
tiempo; preferimos que lo use solo para ejecutar la orden.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además:
- **Degradación declarada de nuestro lado:** si el proveedor cae, las entregas quedan ordenadas y en cola, sin
  perderse, y la app avisa la demora. Se conmuta al segundo proveedor **solo para órdenes nuevas**; nunca se
  reenvía a otro proveedor una orden en estado desconocido.
- Origen de fondos: las transferencias salen de nuestras cuentas de custodia (usuarios) o de la cuenta
  segregada del fondo de cada grupo, nunca de la cuenta operativa de la empresa.

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED` de Pablo)

Razón social, marca y canal · fechas · volumen estimado · plazos abiertos del Anexo A (RM-15, RM-18, SEG-05,
DAT-06).
