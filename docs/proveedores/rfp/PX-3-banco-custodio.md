---
titulo: "RFP PX-3 — Banco custodio: cuentas de custodia, cuentas segregadas por grupo y extractos"
estado: lista para enviar una vez completados los campos [ … ] y con la opinión legal L-01 (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-3 y §PX-0; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, custodia, conciliacion]
---

# Solicitud de propuestas PX-3 — Banco custodio

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

> **Condición previa al envío.** La figura jurídica de las cuentas segregadas por grupo (cuenta en una entidad
> regulada, fideicomiso u otra) depende de la opinión legal **L-01** (si retener el aporte al fondo del grupo
> constituye captación). Esta RFP pregunta qué figuras ofrece el banco, sin elegir una. No enviar sin que el
> abogado o Cumplimiento hayan leído la sección 4.

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia.
Buscamos una entidad financiera que mantenga **cuentas separadas** para el dinero de los usuarios, para el
fondo de cada grupo y para la operación de la empresa, y que nos entregue extractos diarios para conciliar
cada centavo contra nuestro libro mayor. El dinero de los usuarios y de los grupos **nunca se mezcla** con el
de la empresa. Hoy no movemos dinero real.

Qué es el fondo del grupo: cada participante aporta un porcentaje extra a un fondo de su grupo; el fondo cubre
la cuota de quien no paga (esa persona queda debiéndole al fondo) y, al cerrar el ciclo, se devuelve a cada
participante su aporte menos su parte de lo que no se pudo recuperar. La empresa nunca pone dinero en el fondo.

Volumen estimado: [grupos activos], [saldo total promedio en custodia en BOB], [movimientos diarios].

## 2. Calendario y forma de respuesta

| Hito | Fecha |
|---|---|
| Publicación | [fecha] |
| Preguntas por escrito hasta | [fecha] |
| Cierre de propuestas | [fecha] |
| Reunión técnica (preseleccionados) | [rango] |
| Resultado | [fecha] |

Se responde cada código con **Cumple** / **Cumple con desarrollo** / **No cumple**. Propuestas a [canal].

## 3. Contexto técnico

- Integración por API o por archivo programado detrás de un puerto propio (extractos y saldos).
- Importamos cada extracto **idempotentemente**: guardamos el crudo con su hash, origen y periodo; volver a
  recibir el mismo extracto no duplica movimientos.
- Importes en decimal exacto con moneda `BOB`; nunca en coma flotante.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el banco |
|---|---|---|
| RF-PX3-01 | **Cuentas separadas:** custodia de saldos de usuarios; una cuenta o subcuenta segregada **por cada fondo de grupo**; una cuenta operativa de la empresa (comisiones). Nunca se mezclan. La figura jurídica depende de L-01 | Qué figuras ofrece (cuentas, subcuentas, fideicomiso u otra), cómo se abren por API a medida que se crean grupos y qué protección tienen frente a terceros |
| RF-PX3-02 | **Extracto diario** por API o archivo, que guardamos crudo con hash, origen y periodo; importación idempotente por (cuenta, periodo, hash); importes en decimal exacto con su moneda | Formato del extracto, horario de disponibilidad, identificador estable por movimiento y por extracto |
| RF-PX3-03 | Emparejamos por niveles (referencia, id de la pasarela, monto con fecha, sugerencia con tolerancia declarada); cada emparejamiento guarda su regla y su confianza | Qué referencias del pago trae cada movimiento (nuestra referencia, id de la pasarela, glosa) |
| RF-PX3-04 | Clasificamos las excepciones (no identificado, faltante, sobrante, parcial, duplicado, comisión, reverso) con dueño, antigüedad y alerta | Cómo informa reversos, comisiones bancarias y devoluciones en el extracto |
| RF-PX3-05 | **Cierre diario:** saldo del banco = saldo del mayor ± partidas en tránsito; si no cuadra, el día queda abierto con la diferencia registrada; toda corrección es un asiento | Saldo de cierre diario certificado por cuenta |
| RF-PX3-06 | **Cuadre del fondo de cada grupo:** el saldo de su cuenta segregada coincide con nuestro mayor del fondo; si no, suspendemos las coberturas de ese grupo y alertamos | Saldo por cuenta o subcuenta segregada, consultable por API |
| RF-PX3-07 | **Rendimiento:** si el banco paga intereses sobre estas cuentas, no se reparten; se registran y su destino lo decide la empresa (decisión abierta) | Si las cuentas generan intereses y cómo se informan por separado |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-3 |
|---|---|---|
| RM-01 | Sí | API documentada o formato de archivo publicado |
| RM-02 | Sí | Declarar canales (API, archivo), horarios de corte y formatos |
| RM-03 | Parcial | No hay órdenes de pago en este servicio; la idempotencia es nuestra, por (cuenta, periodo, hash) del extracto |
| RM-04 | Si aplica | Si el banco notifica movimientos por webhook, firmado; si no, por archivo |
| RM-05 | Sí | Movimientos con fecha valor y fecha contable; reversos identificables contra el original |
| RM-06 | Sí | Consulta de saldo y de movimientos por API |
| RM-07 | No aplica | No hay operaciones salientes en este servicio (las hace PX-2) |
| RM-08 | Sí | Horario de disponibilidad del extracto y latencia de la API de saldos |
| RM-09 | Sí | Aviso de incidentes que demoren extractos |
| RM-10 | No aplica | Un solo banco custodio por figura; no hay reparto de tráfico |
| RM-11 | Sí | Cláusulas de confidencialidad, auditoría y continuidad; casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales de API o de llaves de transferencia de archivos sin corte |
| RM-13 | Sí | Los extractos no deberían traer más datos del ordenante de los necesarios para conciliar |
| RM-14 | Sí | Costo de mantenimiento por cuenta o subcuenta, por movimiento y por extracto |
| RM-15 | Sí | Extractos históricos disponibles después de la baja durante el plazo de conservación |
| RM-16 | Sí | Id de correlación en la API |
| RM-17 | Sí | Entorno de pruebas con extractos sintéticos |
| RM-18 | Sí | Versionado del formato de extracto con aviso de cambios |
| RM-19 | Sí | RTO/RPO y plan de continuidad del acceso a saldos y extractos |
| RM-20 | Sí | Horarios y formatos configurables cuando el banco lo permita |

## 6. Casos obligatorios de prueba

El plan de proveedores no trae tabla de casos para PX-3; estos se derivan de RF-PX3-02 a RF-PX3-06.

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Extracto diario con movimientos identificables por referencia | Todos emparejados; el día cierra cuadrado |
| Límite | El mismo extracto entregado dos veces | Se importa una sola vez |
| Límite | Movimiento con fecha valor de un día y fecha contable del siguiente | Queda como partida en tránsito y cuadra al día siguiente |
| Límite | Cuenta segregada de un grupo sin movimientos en el día | Saldo informado igual al del día anterior |
| Inválido | Movimiento sin referencia reconocible | Excepción «no identificado» con dueño y alerta |
| Inválido | Reverso de un movimiento ya conciliado | Excepción «reverso» y contra-asiento |
| Inválido | Saldo de la cuenta segregada distinto del mayor del fondo | Se suspenden las coberturas del grupo y salta la alerta |
| Inválido | Extracto no disponible en el horario comprometido | El día queda abierto con alerta |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: acceso por API con autenticación mutua o
canal de archivos cifrado; separación de permisos entre consulta de extractos y cualquier operación saliente.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A. Para este servicio además: cómo se conservan los extractos y por cuánto
tiempo (el repo registra un deber de conservación de libros y documentos de al menos diez años desde el último
asiento — `docs/Cumplimiento.md` §1.6; Cumplimiento confirma si aplica a nuestra figura).

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además: procedimiento y plazo para trasladar los saldos
custodiados a otra entidad si el contrato termina, sin que el dinero pase por la cuenta operativa de la empresa.

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED` de Pablo)

Razón social, marca y canal · fechas · volumen estimado · plazos abiertos del Anexo A · **opinión legal L-01**
leída antes de enviar · destino del rendimiento de las cuentas (RF-PX3-07).
