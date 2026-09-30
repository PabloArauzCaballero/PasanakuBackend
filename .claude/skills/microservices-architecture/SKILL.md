---
name: microservices-architecture
description: Gate de arquitectura distribuida de Pasanaku — mapa de servicios y sus dueños, cómo se traza un límite (bounded context) y cómo se detecta uno mal trazado, base por servicio sin excepciones, qué va en un servicio nuevo y qué es solo un módulo, datos compartidos sin base compartida, el monolito modular como punto de partida legítimo y las falacias distribuidas. Usar antes de crear un servicio, antes de mover una responsabilidad de un servicio a otro, cuando dos servicios siempre se despliegan juntos, cuando alguien propone leer la tabla de otro, o al ubicar dónde vive una funcionalidad nueva.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Arquitectura de microservicios — Pasanaku

Pasanaku está partido en servicios porque el dinero, la identidad y la reputación tienen perfiles
de riesgo y cadencias distintas. Ese corte se paga caro: **cada límite que cruzás pierde la
transacción de base**. Esta skill es el gate que impide pagar ese precio sin recibir nada a cambio.

Las prohibiciones duras están en la **regla 98**. Acá está el criterio para diseñar.

## 1. Mapa de servicios y dueños del dato

Cada módulo del modelo de datos tiene un dueño y **uno solo**. Si no encontrás acá dónde vive un
dato, no lo inventes: `anti-hallucination-guard`.

| Servicio | Módulo | Es dueño de | Nunca es dueño de |
|---|---|---|---|
| `identidad` | M1 | Usuario, credencial, factor MFA, dispositivo, token de verificación, documento de identidad, consentimiento | Nada de grupos ni importes |
| `grupos` | M2 | Grupo, cupo, participante, periodo, turno, reglamento, acuerdo, sorteo | Saldos, pagos |
| `pagos` | M3 | Obligación de aporte, orden de cobro, enlace/QR, pago, conciliación, extracto | El mayor contable |
| `contabilidad` | M3 | Plan de cuentas, asiento, movimiento, cierre diario | Reglas de cobro |
| `entregas` | M4 | Entrega de fondo, deducción, cuenta bancaria del beneficiario, confirmación | Cálculo de deuda |
| `notificaciones` | M5 | Plantilla, canal vinculado, cola de envío, evento de entrega | El contenido de negocio |
| `confianza` | M6 | Bloque de transparencia, score y componentes, certificado de reputación | El expediente de mora |
| `organizador` | M7 | Contrato del organizador, tarea automatizada, evaluación de desempeño | Ejecución de dinero |
| `garantia` | M8 | Fondo, cobertura, registro de incumplimiento, deuda, aval, sanción, apelación | La reputación resultante |
| `auditoria` | M9 | Bitácora encadenada, evento de dominio publicado, definición y ejecución de reportes | Datos operativos |

**Regla del dueño único:** un dato tiene exactamente un servicio que lo escribe. Los demás lo
leen por API o lo reciben por evento, y lo que guardan de él es una **copia declarada como
copia** (`eventual-consistency-read-models`), nunca una segunda fuente de verdad.

## 2. Cómo se traza un límite

Un límite bien trazado separa cosas que **cambian por razones distintas y a ritmos distintos**.

Preguntas que lo revelan, en orden:

1. **¿Quién decide que este dato cambió?** Si la respuesta es el mismo actor y la misma regla de
   negocio, están del mismo lado.
2. **¿Puede este lado quedar inconsistente con el otro por unos segundos sin que nadie pierda
   plata?** Si la respuesta es no, el límite está mal: **una invariante de dinero no se parte**.
   El saldo de un aporte y su asiento contable pueden vivir en servicios distintos; el asiento y
   sus movimientos, no.
3. **¿Se despliegan juntos siempre?** Si sí, son uno.
4. **¿El vocabulario cambia de significado al cruzar?** "Cupo" en `grupos` es una posición en el
   calendario; en `pagos` es solo un identificador al que se le cobra. Ese cambio de significado
   **es** el límite.

### Señales de un límite mal trazado

| Síntoma | Qué significa |
|---|---|
| Un cambio de negocio toca siempre los mismos dos servicios | El límite corta por el medio de un concepto |
| Un servicio llama a otro tres veces para resolver un request | Le falta el dato, o le sobra la responsabilidad |
| Necesitás una transacción que abarque los dos | No son dos |
| Un servicio expone un endpoint que solo usa otro servicio, y es un CRUD de su tabla | Es una base compartida con HTTP encima |
| El "servicio de utilidades" que todos llaman | No es un servicio: es una librería mal ubicada |

## 3. Base por servicio — sin excepciones

- **Un esquema por servicio, credenciales propias, sin permisos cruzados.** El usuario de base de
  `pagos` no tiene `SELECT` sobre el esquema de `grupos`. Eso se verifica con una consulta a
  `information_schema.role_table_grants`, no con buena voluntad.
- ¿Necesitás un reporte que cruza servicios? Va contra `auditoria`, que **recibe eventos** y arma
  su propio modelo de lectura. No contra las bases de producción de los demás.
- ¿Necesitás un `JOIN` con datos de otro servicio en un listado? Replicá el puñado de campos que
  mostrás (`cupo_id`, nombre visible) por evento, y declará el desfase tolerable.
- Compartir la instancia física de PostgreSQL entre servicios es aceptable al principio;
  compartir **esquema o tablas**, nunca. Uno es una decisión de costo, el otro es de diseño.

## 4. Cuándo NO crear un servicio

El monolito modular es un punto de partida legítimo y muchas veces el correcto. **La regla 98.7
fija los cuatro motivos válidos**; todo lo demás es un módulo.

No es motivo suficiente:

- "Queda más ordenado" · "así lo hacen todos" · "para que el repo no crezca"
- "Este equipo quiere su repo" cuando el equipo son dos personas que trabajan en todo
- "Lo vamos a escalar" sin un número medido de carga que lo justifique

**Costo real de un servicio nuevo**, para presupuestarlo antes de decidir: repositorio y
plantilla, pipeline, imagen y despliegue, base y migraciones, secretos, health checks, alertas,
dashboard, runbook, contrato versionado con tests, y para siempre: un salto de red más en cada
flujo que lo toque.

## 5. Las falacias de la computación distribuida

Escritas por Peter Deutsch y compañía; siguen costando plata:

1. La red es confiable — **no**: diseñá el reintento y el fallback (`resilience-patterns`).
2. La latencia es cero — **no**: cada salto suma; medila, no la supongas.
3. El ancho de banda es infinito — **no**: no mandes el agregado entero "por si acaso".
4. La red es segura — **no**: autenticá entre servicios (`service-to-service-security`).
5. La topología no cambia — **no**: nada de IPs fijas en el código.
6. Hay un solo administrador — **no**: el otro servicio se despliega sin avisarte.
7. El transporte no cuesta nada — **no**: serializar y deserializar aparece en el perfil.
8. La red es homogénea — **no**: versiones de cliente distintas conviviendo.

Corolario para Pasanaku: **cada salto es un lugar donde el dinero puede quedar a medio camino.**
Por eso todo flujo de dinero que cruza servicios es saga + outbox (`saga-distributed-transactions`).

## 6. Composición: dónde se junta la respuesta

Cuando una pantalla necesita datos de tres servicios:

| Opción | Cuándo | Costo |
|---|---|---|
| El cliente hace 3 llamadas | Pantallas simples, datos independientes | Latencia en móvil, lógica en el front |
| BFF / gateway compone | Lo normal en Pasanaku (`api-gateway-bff`) | Un lugar más que mantener |
| Un servicio compone llamando a otros | Solo si es su responsabilidad de negocio | Acopla el que compone a los tres |
| Modelo de lectura replicado | Listados de alto tráfico y tolerantes a desfase | Consistencia eventual visible |

**Prohibido componer encadenando servicios de negocio** (`grupos` → `pagos` → `contabilidad` en
fila para responder un `GET`). Eso es la cadena de tres saltos que prohíbe la regla 98.2.5.

## Anti-patrones

- **Monolito distribuido**: N servicios que se despliegan juntos, comparten base y se llaman en
  cadena. Tenés todos los costos y ninguna de las ventajas.
- **Servicio anémico**: un CRUD sobre una tabla, sin reglas propias. Es una tabla con latencia.
- **Base compartida** "solo para este reporte".
- **Entidad del ORM como contrato** entre servicios.
- **Servicio Dios** (`core`, `common-service`) del que todos dependen: un despliegue suyo es un
  despliegue de todos.
- Crear el servicio antes de entender el dominio. Un límite mal puesto cuesta diez veces más que
  no haberlo puesto.
- Consistencia fuerte simulada con reintentos y "ya va a llegar".

## Checklist

- [ ] La funcionalidad nueva tiene un dueño identificado en el mapa de §1.
- [ ] Ningún dato quedó con dos escritores.
- [ ] Ninguna invariante de dinero quedó partida entre dos servicios.
- [ ] Sin acceso a base ajena: verificado con la consulta de permisos, pegada.
- [ ] Si es un servicio nuevo: motivo de la regla 98.7 declarado por escrito y costo asumido.
- [ ] Los datos ajenos que necesito llegan por API o por evento, y la copia está declarada.
- [ ] La composición de pantallas no arma cadenas sincrónicas de más de dos saltos.
- [ ] El contrato de lo que expongo está versionado y con test (`service-contracts-versioning`).

## Evidencia / DoD

1. Diagrama o tabla de los servicios tocados y qué hace cada uno en el flujo.
2. Salida de la consulta de permisos de base demostrando que no hay acceso cruzado.
3. Para un servicio nuevo: ADR con el motivo, el costo y la alternativa descartada
   (`technical-docs-and-adr`).
4. Lista de los datos ajenos usados, con la vía (API o evento) y el desfase tolerado.
