---
tags:
  - arquitectura
  - adr
  - poder-de-compra
  - comercios
  - microservicios
  - dinero
titulo: "ADR-057 — «Poder de compra»: dónde vive el agregado del comercio aliado"
estado: propuesta
fecha: 2026-11-21
---

# ADR-057 — «Poder de compra»: dónde vive el agregado del comercio aliado

> **Estado: propuesta.** La escribe la sesión de diseño en paralelo del plan de correcciones del 2026-11-21
> (`PX10.M01`). La aprueba Pablo. Es **solo diseño**: la tanda 6 de `DECISIONES.md` dice «diseñar ahora,
> construir después», y el código de la fase 5 no empieza hasta tener (1) la opinión legal **L-11**
> (clasificación ante ASFI y garantía sobre bienes) y (2) al menos un comercio firmado. Este ADR no habilita
> dinero real ni crea tablas: el modelo de datos del comercio y del canje es `PX10.M04`, sobre la fuente
> `docs/entidades/*.puml` (regla 97.1).

## Contexto

**Qué es «Poder de compra»** (tandas 5 y 6; `PLAN.md` H5; `PLAN-PROVEEDORES.md` PX-10). Un grupo ahorra para
comprar un producto de un comercio aliado con descuento. El precio queda **congelado** durante el ciclo. Cuando
le llega el turno, el participante **canjea** su pozo en la tienda: muestra un QR de canje, el comercio lo
escanea al entregar el producto y recién entonces se le paga al comercio. Si el comercio no entrega, no cobra.
La **opción de recibir el dinero existe siempre**. La comisión por venta se le cobra al comercio, nunca al
ahorrista. Piloto: 3 a 5 comercios, 1 o 2 categorías.

**Qué existe hoy en el repo.**

- El servicio **`publicidad`** (M14) ya tiene un comercio con identidad propia: `socio_comercial` (razón social,
  documento único, rubro, contacto, estado, `verificado_por`) y `anunciante`
  (`docs/entidades/14_publicidad_campanas.puml:371-394`). Su regla de negocio es que el anunciante **siempre
  paga y nunca cobra**.
- La maqueta `docs/Views/Maqueta-Crecimiento/` ya dibujó el portal del comercio (incluida la pantalla «Validar
  vale» con control de doble canje) y su README decide **reutilizar `socio_comercial` + `anunciante`** como
  `Partner` en vez de crear un comercio nuevo (`README.md` «Cómo encaja con M13 y M14»), y declara como
  genuinamente nuevos el vale con ciclo propio y su canje.
- **`entregas`** es dueño de la entrega del turno y del puerto de desembolso (`ProveedorDeDesembolso`, PX-2),
  con cuenta verificada (PX-6), enfriamiento y confirmación por webhook firmado. PX-2 ya lista al comercio como
  destino posible (RF-PX2-06).
- **`grupos`** es dueño del grupo, su reglamento y los parámetros congelados al iniciar (tanda 7).
- **`tarifas`** es dueño de la comisión y de su facturación (PX-8).
- La regla **98.7** exige justificar por escrito un servicio nuevo con al menos uno de: ciclo de vida de datos
  propio con dueño claro, cadencia de escala o despliegue distinta, perfil de riesgo distinto que conviene
  aislar, o frontera de equipo existente. «Para que no crezca el otro» no alcanza.

**La pregunta.** ¿Dónde vive el agregado del comercio aliado (alta KYB, contrato, catálogo curado, oferta con
precio congelado) y dónde vive el canje que libera el pago al comercio?

## Opciones evaluadas contra la regla 98.7

| Criterio 98.7 | O1 · Servicio nuevo `comercios` con todo | O2 · Todo en `publicidad` | O3 · Todo en `entregas` | **O4 · Comercio en `publicidad`, canje y pago en `entregas`** |
|---|---|---|---|---|
| Ciclo de vida de datos propio y dueño claro | Sí, pero el comercio ya tiene dueño (`socio_comercial`): duplicaría el catálogo (regla 97.4.6) | Sí para el comercio; no para el canje, que es dinero del turno | No: el catálogo de comercios no tiene nada que ver con el ciclo de una entrega | **Sí**: cada dato queda con el dueño que ya tiene su ciclo |
| Cadencia de escala o despliegue distinta | No en un piloto de 3 a 5 comercios | — | — | No hace falta |
| Perfil de riesgo distinto | El canje mueve dinero: o se aísla o se duplica el puerto de desembolso | **Rompe** el perfil de `publicidad` («nunca cobra»): pasaría a ordenar pagos | El riesgo de dinero queda donde ya está aislado | **El dinero queda en `entregas`**, que ya está endurecido para desembolsar; `publicidad` sigue sin mover fondos |
| Frontera de equipo existente | No existe un equipo de alianzas | — | — | No hace falta |
| Costo (repositorio, pipeline, base, guardia, contrato) | Alto, para un piloto condicionado | Bajo | Bajo | Bajo |
| Saltos sincrónicos en el canje (98.2.5) | panel → comercios → entregas: 2 | panel → publicidad → entregas: 2 | panel → entregas: 1 | **panel → entregas: 1** (la verificación del comercio llega por evento) |

## Decisión (propuesta)

**O4. No se crea un servicio nuevo.** El agregado del comercio aliado es un **módulo nuevo dentro de
`publicidad`**, que extiende `socio_comercial`; el canje y el pago al comercio son un **destino de la entrega**
en `entregas`.

| Pieza | Dueño | Cómo |
|---|---|---|
| Comercio aliado: alta de la empresa (KYB), representante verificado, cuenta verificada, control de listas, contrato firmado y su estado | `publicidad` (módulo `alianzas`) | Extiende `socio_comercial`; las verificaciones se piden a `identidad` (PX-4, PX-6) y `cumplimiento` (PX-5) por sus APIs. La verificación **no la hace una persona**: `verificado_por` queda para la excepción |
| Catálogo curado y oferta (producto, precio de lista, descuento, **precio congelado**, vigencia, plazo de entrega, garantía y devoluciones a cargo del comercio) | `publicidad` (módulo `alianzas`) | Publica `oferta.publicada`, `oferta.retirada`, `comercio.verificado`, `comercio.suspendido` por outbox |
| Meta del grupo (qué producto, a qué precio congelado) | `grupos` | Copia por valor de la oferta al crear el grupo (`oferta_id`, precio, moneda, vigencia), tomada de su proyección local de ofertas; queda dentro del reglamento congelado (tanda 7). Un cambio posterior de la oferta no la toca |
| Canje: QR de canje, validación al escanear, elección «recibir mi dinero» | `entregas` | La entrega del turno tiene un destino: **cuenta del participante** (siempre disponible) o **comercio aliado** (solo si el grupo tiene meta). El QR de canje es un token de propósito único, de un solo uso, con vencimiento, atado a esa entrega |
| Pago al comercio | `entregas` | Desembolso por PX-2 a la **cuenta verificada del comercio**, con `clave_idempotencia` = id del canje, recién al validarse el escaneo |
| Comisión por venta al comercio y su factura | `tarifas` | Se cobra al comercio, nunca al ahorrista (RF-PX10-04); se factura por PX-8 |
| Reclamos del comprador al comercio | `publicidad` (alianzas) + canal de reclamos existente | La app media; **no retiene fondos** más allá de lo que permita L-11 (RF-PX10-05) |

### Reglas que este ADR fija para el diseño

1. **`publicidad` nunca ordena un pago.** Su regla «el anunciante siempre paga, nunca cobra» se amplía sin
   romperse: el comercio aliado **cobra** solo a través de `entregas`, con el dinero del pozo del participante,
   nunca de una cuenta de la empresa.
2. **El dinero del grupo no pasa por la cuenta de la empresa** (RN-18 y regla 91.5, aplicadas acá a la
   empresa): el desembolso al comercio sale de la custodia donde está el pozo.
3. **Canje atómico con precondición en la escritura** (regla 96.3.2): la transición
   `PENDIENTE_DE_CANJE → CANJEADA` y la elección «recibir mi dinero» (`PENDIENTE_DE_CANJE → A_CUENTA`) compiten
   por la misma fila; gana la primera y la segunda recibe 409 con un mensaje claro. Nunca se paga dos veces.
4. **El comercio cobra después de entregar**, nunca antes. Un escaneo válido es la constancia de entrega.
5. **La opción de recibir el dinero está siempre disponible** mientras el canje no se haya validado.
6. **Ninguna persona aprueba el canje** (ADR-055, cuando exista): lo valida el token y la máquina de estados.
7. La verificación del comercio en el canje no se consulta en caliente a `publicidad`: `entregas` usa su
   proyección de `comercio.verificado` / `comercio.suspendido` con desfase máximo declarado; un comercio
   suspendido no puede validar canjes nuevos.

## Motivo

- **No hay criterio de 98.7 que justifique un servicio nuevo** en un piloto de 3 a 5 comercios: el comercio
  ya tiene casa (`socio_comercial`) y la maqueta ya decidió reutilizarla; crear `comercios` sería un catálogo
  paralelo (regla 97.4.6).
- **El único perfil de riesgo distinto es el pago**, y ese ya está aislado en `entregas`, con la cuenta
  verificada, el enfriamiento, la idempotencia y la confirmación por webhook que exige la regla 91.3.3.
  Meterlo en `publicidad` le daría a un servicio de marketing la capacidad de ordenar desembolsos.
- **Un solo salto sincrónico** en el momento crítico (el escaneo en la tienda, con el cliente esperando).

**Cuándo revisar este ADR:** si el piloto pasa a más de [N] comercios o si aparece un equipo propio de
alianzas, se reevalúa un servicio `alianzas` (criterios 98.7 de escala y de equipo).

## Alternativas descartadas

- **O1 — servicio nuevo `comercios`.** Duplica `socio_comercial` y agrega un servicio con su pipeline, base y
  guardia para un piloto condicionado.
- **O2 — todo en `publicidad`.** Le da a `publicidad` la capacidad de ordenar pagos: cambia su perfil de
  riesgo y duplica el puerto de desembolso.
- **O3 — todo en `entregas`.** Mezcla el ciclo de vida de un comercio (KYB, contrato, catálogo) con el de
  una entrega.

## Consecuencias

- `publicidad` suma un módulo `alianzas` y eventos nuevos; su contrato OpenAPI se versiona (regla 98.1.3).
- `entregas` suma un destino de entrega y el canje; su máquina de estados gana `PENDIENTE_DE_CANJE`,
  `CANJEADA` y `A_CUENTA` (nombres a confirmar en `PX10.M04`).
- `grupos` suma la meta del grupo con su copia congelada de la oferta.
- `tarifas` suma la comisión al comercio y su factura.
- Las piezas de la maqueta que no pertenecen a esta fase (mercado de turnos, permutas, inversión) **no** se
  habilitan: la subasta y la cesión de turno se eliminaron (tanda 7).

## Lo que este ADR no decide (`DECISION_REQUIRED` o legal)

| Tema | Tipo | A quién |
|---|---|---|
| Clasificación de la modalidad ante ASFI y garantía sobre el bien (reserva de propiedad) | L-11 | Abogado |
| Plazo máximo entre el turno y el canje, y qué pasa al vencer (pasa solo a la cuenta del participante) | `DECISION_REQUIRED` + L-11 (retención de fondos) | Pablo y abogado |
| Turno temprano para personas sin historial pagando cuotas por adelantado (F5.M4) y cómo convive con el orden por nivel de puntualidad y sorteo (tanda 5) | `DECISION_REQUIRED` | Pablo |
| Si el precio de lista al canjear es **menor** que el congelado, ¿quién se queda la diferencia? (propuesta: el participante, a su cuenta) | `DECISION_REQUIRED` | Pablo |
| ¿La comisión al comercio se descuenta del pago (neteo) o se factura aparte? | `DECISION_REQUIRED` | Pablo y Cumplimiento |
| Interpretación de «opción de efectivo»: este ADR la lee como **recibir el pozo en la cuenta verificada**, porque la plataforma no opera dinero físico (ADR-039) | Ambigüedad a confirmar | Pablo |
| Porcentaje de la comisión al comercio | Parámetro (F3.S8.M2), sin valor por defecto | Pablo |

## Cómo se verifica (cuando se construya)

- `grep` de que `publicidad` no importa el puerto de desembolso ni publica órdenes de pago.
- Prueba de concurrencia real: escaneo del comercio y «recibir mi dinero» al mismo tiempo → un solo efecto.
- Prueba de idempotencia: el mismo escaneo dos veces → un solo desembolso (misma `clave_idempotencia`).
- Comercio suspendido → el escaneo se rechaza.
- Los cinco puntos de la regla 91.6 sobre el desembolso al comercio.

## Referencias

`DECISIONES.md` tandas 5 y 6 · `PLAN.md` H5 · `PLAN-PROVEEDORES.md` PX-10 y PX-2 · `PREGUNTAS-LEGALES.md` L-11
· [[ADR-039 Sin efectivo · la plataforma no opera dinero físico|ADR-039]] ·
[[ADR-017 Propiedad de datos por servicio|ADR-017]] · `docs/Views/Maqueta-Crecimiento/README.md` ·
`docs/poder-de-compra/contrato-tipo-comercio.md` · `docs/poder-de-compra/pantallas.md` · ADR-055, ADR-056 y
ADR-058 (los escriben otras sesiones del plan).
