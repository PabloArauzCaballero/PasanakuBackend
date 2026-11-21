---
titulo: "RFP PX-5 — Control de listas (sanciones, listas de la UIF) y PEP"
estado: lista para enviar una vez completados los campos [ … ] y la definición de listas de Cumplimiento (sección 10)
fecha: 2026-11-21
origen: "PLAN-PROVEEDORES.md §PX-5 y §PX-0; plantilla docs/proveedores/00-plantilla-rfp.md"
tags: [proveedores, rfp, cumplimiento, listas, pep]
---

# Solicitud de propuestas PX-5 — Control de listas y PEP

Se envía junto con el **Anexo A — Requisitos comunes** (`docs/proveedores/00-plantilla-rfp.md`, secciones
5, 7, 8 y 9).

## 1. Quiénes somos y qué buscamos

[Razón social de la empresa] opera **Pasanaku** ([marca]), una aplicación de pasanaku digital en Bolivia.
Necesitamos controlar a cada usuario **al alta y periódicamente** contra las listas que defina nuestro
Cumplimiento (a identificar: listas de la UIF, sanciones internacionales y personas expuestas políticamente),
con evidencia de cada control. Una coincidencia abre un caso que analiza Cumplimiento. **Al titular nunca se
le avisa de la coincidencia** (deber de reserva). Hoy no movemos dinero real.

Usuarios estimados a controlar: [altas por mes], [base total para el control periódico].

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

- Integración por API detrás de un puerto propio. El control al alta es síncrono o por webhook; el periódico,
  por lote o por reevaluación de la base cuando cambian las listas.
- Si en lugar de un proveedor se usan fuentes públicas, se importan con su procedencia y versión, nunca
  escritas a mano (ver RF-PX5-05): el oferente puede proponerse como proveedor de esas fuentes.

## 4. Requisitos funcionales del servicio

| Código | Requisito | Qué tiene que demostrar el proveedor |
|---|---|---|
| RF-PX5-01 | Control **al alta y periódico** (frecuencia que fijamos) contra las listas que defina Cumplimiento: listas de la UIF, sanciones internacionales y PEP | Listas cubiertas, frecuencia de actualización de cada una y cómo nos avisa de un usuario ya controlado que pasa a coincidir |
| RF-PX5-02 | Coincidencia → alerta → caso en nuestro sistema, donde quien analiza y quien revisa son personas distintas. **Nunca se avisa al titular** | Que ningún mensaje ni correo del proveedor llega al titular |
| RF-PX5-03 | El resultado se guarda con su **evidencia**: versión de la lista, puntaje de coincidencia y fecha | Que cada respuesta trae versión de la lista, puntaje y campos que coincidieron |
| RF-PX5-04 | Si el proveedor está caído, el alta queda pendiente de control y **no se habilita dinero** hasta completarlo | Consulta de estado y reintento; aviso de incidentes |
| RF-PX5-05 | Si las listas se importan de una fuente pública en lugar de un proveedor, se importan con su **procedencia y versión**, nunca escritas a mano | Si el oferente entrega las listas como archivos: formato, firma o hash, fecha y fuente de cada una |

## 5. Requisitos comunes (Anexo A §5) aplicados a este servicio

| Código | Aplica | Precisión para PX-5 |
|---|---|---|
| RM-01 | Sí | API documentada |
| RM-02 | Sí | Declarar listas, coincidencia difusa (algoritmo y umbral), idiomas y transliteración |
| RM-03 | Sí | Nuestra clave por control: repetir no cobra dos veces |
| RM-04 | Sí | Webhook firmado para resultados asíncronos y para coincidencias nuevas del control periódico |
| RM-05 | Sí | Si una lista se corrige, el resultado nuevo indica la versión que reemplaza |
| RM-06 | Sí | Consulta del resultado por nuestra clave |
| RM-07 | Sí | Timeout: el alta queda pendiente, nunca aprobada por defecto |
| RM-08 | Sí | Latencia del control al alta |
| RM-09 | Sí | Aviso de incidentes y de demoras en la actualización de listas |
| RM-10 | Sí | Arranque gradual |
| RM-11 | Sí | Cláusulas y casos de la sección 6 |
| RM-12 | Sí | Rotación de credenciales sin corte |
| RM-13 | Sí | Solo nombre, documento, fecha de nacimiento y nacionalidad, salvo que se justifique más |
| RM-14 | Sí | Costo por control al alta y por usuario en control periódico |
| RM-15 | Sí | Evidencia de controles históricos consultable después de la baja |
| RM-16 | Sí | Id de correlación, sin datos personales en logs |
| RM-17 | Sí | Sandbox con identidades sintéticas que coinciden de forma exacta y difusa |
| RM-18 | Sí | Versionado y deprecación |
| RM-19 | Sí | RTO/RPO y plan de continuidad |
| RM-20 | Sí | Umbral de coincidencia y frecuencia configurables por nosotros |

## 6. Casos obligatorios de prueba

| Nivel | Caso | Resultado esperado |
|---|---|---|
| Correcto | Identidad sin coincidencias | Control superado con versión de lista y fecha |
| Límite | Coincidencia difusa justo en el umbral | Alerta para Cumplimiento con el puntaje y los campos que coincidieron |
| Límite | Usuario ya aprobado que pasa a figurar en una lista en el control periódico | Alerta nueva, sin aviso al titular |
| Inválido | Coincidencia exacta | Alerta y caso; el titular no recibe ningún aviso |
| Inválido | Proveedor caído | Alta pendiente de control; no se habilita dinero |

## 7. Seguridad

Aplican SEG-01 a SEG-07 del Anexo A. Para este servicio además: confidencialidad reforzada de las coincidencias
(su divulgación puede violar el deber de reserva); acceso del personal del proveedor a nuestras consultas
registrado y auditable.

## 8. Datos personales y financieros

Aplican DAT-01 a DAT-06 del Anexo A. Para este servicio además: el proveedor no conserva nuestras consultas para
construir perfiles ni las comparte con otros clientes.

## 9. Continuidad, disponibilidad, costo y salida

Aplican CON-01 a CON-07 del Anexo A. Para este servicio además: exportación de la evidencia de todos los
controles (versión, puntaje y fecha) al terminar el contrato.

## 10. Campos a completar antes de enviar (`DECISION_REQUIRED`)

Razón social, marca y canal · fechas · volumen estimado · plazos abiertos del Anexo A · **listas exactas que
define Cumplimiento** (PX5.M03) · frecuencia del control periódico.
