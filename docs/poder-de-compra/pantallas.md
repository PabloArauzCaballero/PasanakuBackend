---
titulo: "«Poder de compra» — diseño funcional de pantallas (participante y panel del comercio)"
estado: diseño funcional para revisión de Pablo — sin implementación (PX10.M03)
fecha: 2026-11-21
origen: "PX10.M03 de PLAN-PROVEEDORES.md; DECISIONES.md tandas 5 y 6; ADR-057 (propuesta)"
tags: [poder-de-compra, pantallas, app-movil, comercios, diseno]
---

# «Poder de compra» — diseño funcional de pantallas

> **Qué es este documento.** El diseño funcional: qué ve cada persona, qué puede hacer, qué estados resuelve
> cada pantalla y qué reglas la gobiernan. No es el diseño visual: ese se hace con el sistema de diseño
> existente (`packages/diseno_flutter`, skills `pasanaku-diseno` y `pasanaku-ui-componentes`) cuando se
> construya, y se verifica con prueba visual (regla 95.7). **Se reutilizan** las pantallas ya dibujadas en
> `docs/Views/Maqueta-Crecimiento/` donde encajan (indicado en cada una); lo que la maqueta tiene de mercado de
> turnos, permutas o inversión **no** entra (la subasta y la cesión se eliminaron en la tanda 7).
>
> Los textos son borradores (P-16: los aprueba Pablo). Las reglas de dinero vienen del ADR-057.

## 0. Reglas que gobiernan todas las pantallas

| # | Regla | Origen |
|---|---|---|
| R1 | **La opción de recibir el dinero está siempre visible** mientras el canje no se validó, con el mismo peso visual que «canjear» | DECISIONES tanda 5; RF-PX10-06 |
| R2 | El **precio congelado** se muestra siempre con su moneda y la fecha en que se congeló, y nunca cambia en pantalla durante el ciclo | RF-PX10-01; F5.M2 |
| R3 | El comercio **cobra solo al escanear** el QR al entregar; la pantalla del participante lo dice | RF-PX10-03 |
| R4 | La comisión la paga el comercio; **el participante no ve ningún cargo extra** por elegir el producto | RF-PX10-04 |
| R5 | Las cuatro situaciones de red: cargando, con datos, vacío (que orienta) y error (accionable), más sin conexión donde aplica | Regla 95.2 |
| R6 | WCAG 2.2 AA, modo claro y oscuro, movimiento reducido; ningún estado se comunica solo por color | Reglas 95.4, 95.5 |
| R7 | El QR de canje es **sensible**: no se puede capturar en las pantallas recientes del sistema, se vence, y no contiene montos ni datos personales legibles | Regla 90.2; skill `qr-pagos` |
| R8 | Ninguna persona de la Plataforma aprueba nada en este flujo | ADR-055 (en elaboración por otra sesión) |

## 1. App del participante

### P1 — Elegir la meta al crear el grupo (catálogo curado)
Reutiliza: `app/objetivo-ahorro.html` de la maqueta.

- **Qué ve:** los comercios verificados y sus productos (3 a 5 comercios, 1 o 2 categorías en el piloto), con
  precio de lista, descuento, precio con descuento, plazo de entrega y garantía. Un filtro por categoría.
- **Qué hace:** elige un producto como meta del grupo, o «sin meta» (pasanaku normal).
- **Al confirmar:** la pantalla muestra «Precio congelado para este grupo: Bs [precio] — congelado el [fecha].
  Aunque el comercio cambie su precio, tu grupo paga este» y la cuota resultante con sus líneas (cuota al pozo,
  aporte al fondo del grupo, comisión del servicio), cada una con su moneda.
- **Estados:** cargando el catálogo · con datos · vacío («Todavía no hay comercios aliados en tu ciudad. Podés
  crear el grupo sin meta y ahorrar igual») · error («No pudimos cargar los comercios. Reintentá») · sin
  conexión (no se puede crear el grupo con meta sin el precio vigente).
- **Regla:** un comercio suspendido o una oferta retirada no aparecen para grupos nuevos (F5.M1).

### P2 — Invitación con meta
- El enlace de invitación (único medio de entrada, tanda 5) muestra la meta, el precio congelado y la cuota antes
  de aceptar el reglamento. Nada nuevo fuera de eso: el resto es el flujo de invitación de F3.S4.M1.

### P3 — Avance del grupo con meta
Reutiliza la pantalla de avance de F3.S7.M4 («8 de 10 pagaron») y le agrega una tarjeta: producto, precio
congelado, turno de cada uno según el orden por nivel y sorteo. No agrega gamificación propia.

### P4 — «Te llegó el turno»: elegir producto o dinero
Reutiliza: `app/que-hacer-con-tu-dinero.html` de la maqueta, con su regla 9 («retirar es siempre la primera
opción»).

- **Qué ve:** dos opciones del mismo tamaño y peso:
  - **«Recibir mi dinero»** — «Bs [monto] a tu cuenta [banco ••• últimos 4]».
  - **«Canjear [producto] en [comercio]»** — «Precio congelado Bs [precio]. El comercio cobra recién cuando te
    entrega el producto».
- **Qué hace:** elige. Si elige el dinero, se ordena el desembolso normal (F3.S2) y el flujo de canje termina.
  Si elige canjear, pasa a P5. **Puede cambiar a «Recibir mi dinero» en cualquier momento antes del escaneo**.
- **Estados:** cargando · con datos · error · **en proceso** («Estamos preparando tu entrega»), cuando el pozo
  todavía no está completo · **cuenta en enfriamiento** («Cambiaste tu cuenta hace poco. Podés recibir el dinero
  a partir del [fecha] o canjear ahora») — **`DECISION_REQUIRED`**: si el canje también espera al
  enfriamiento de la cuenta del participante.
- **Plazo de canje:** si hay plazo máximo (`DECISION_REQUIRED` + L-11), se muestra «Tenés hasta el [fecha] para
  canjear; después, el dinero va solo a tu cuenta».

### P5 — QR de canje
Reutiliza el componente de QR existente de la app, con las protecciones de R7.

- **Qué ve:** el QR, el producto, el comercio con su dirección, el vencimiento del QR y el texto «Mostralo en la
  tienda cuando te entreguen el producto. El comercio lo escanea y recién ahí le pagamos».
- **Acciones:** «Recibir mi dinero en su lugar» (siempre visible, R1) · «Cómo llegar» · «Ayuda».
- **Estados:** cargando · QR vigente · **QR vencido** («Este código venció. Generá uno nuevo» → genera un token
  nuevo; el anterior deja de servir) · **canjeado** (P6) · **ya elegiste recibir el dinero** · error de red
  («No pudimos generar tu código. Reintentá») · sin conexión (el QR ya generado se puede mostrar si sigue
  vigente; uno nuevo exige conexión) · **comercio suspendido** («[comercio] no puede entregar en este momento.
  Podés recibir tu dinero o esperar») .
- **Concurrencia:** si el participante toca «Recibir mi dinero» mientras el comercio escanea, gana la primera
  operación que llegue al servidor; la otra ve un mensaje claro («Tu producto ya fue entregado» o «Ya pediste tu
  dinero; este código dejó de servir»).

### P6 — Canje confirmado
- «Retiraste [producto] en [comercio] el [fecha y hora]». Comprobante con identificador, garantía del producto y
  botón «Tengo un problema con mi compra» (P7).
- Notificación push con el mismo contenido mínimo (sin montos, RF-PX7-04).

### P7 — Reclamo al comercio
- Formulario con motivo de un catálogo cerrado (no entregado completo, falla, distinto de lo ofrecido, otro),
  descripción y fotos opcionales (tipo real y tamaño validados en el servidor). Error por campo; lo escrito no se
  pierde ante un fallo; botón bloqueado mientras se envía (regla 95.3).
- Estados del reclamo: enviado · en respuesta del comercio (plazo [parámetro]) · resuelto · sin respuesta en
  plazo (se escala). **`[L-11]`**: si el reclamo puede retener fondos.

## 2. Panel del comercio (web, en el dominio del comercio)

Reutiliza: `partner/dashboard.html` y `partner/vales.html` de la maqueta («Validar vale», con su control de doble
canje). Lo que la maqueta tiene de audiencia y publicaciones queda fuera del piloto salvo el listado de ofertas.

### C1 — Ingreso
- Usuario del comercio con segundo factor. Cada usuario es una persona (no cuentas compartidas). Roles: dueño del
  comercio, cajero (solo escanea), administración (ve pagos y reclamos).

### C2 — Escanear canje (la pantalla crítica)
- **Qué ve:** cámara para escanear el QR del participante, o ingreso manual del código corto que el QR también
  muestra (accesibilidad y cámaras malas).
- **Antes de confirmar:** producto y precio congelado que corresponden a ese código, y el nombre de pila del
  participante para verificar en persona. Nada más de la persona (regla 90.1.8).
- **Acción:** «Confirmo que entregué el producto». Botón bloqueado mientras se procesa; doble toque no genera dos
  canjes.
- **Resultados:**
  - Canje válido → «Listo. Te pagamos Bs [precio] a tu cuenta ••• [4] — referencia [id]».
  - Código ya usado → «Este código ya se canjeó el [fecha]» (no se paga de nuevo).
  - Código vencido → «Este código venció. Pedile al cliente que genere uno nuevo».
  - El participante eligió el dinero → «Este código ya no es válido».
  - Código de otro comercio → «Este código no es de tu comercio».
  - Comercio suspendido → «Tu comercio está suspendido. Comunicate con nosotros».
  - Sin conexión → «Sin conexión. No entregues el producto hasta confirmar el canje».
- **Regla:** el comercio entrega **después** de ver «Listo»; el texto de la pantalla lo dice antes de escanear.

### C3 — Pagos recibidos
- Lista de canjes con fecha, producto, importe con moneda, estado del pago (ordenado, acreditado, rechazado por
  el banco) y referencia bancaria. Paginada, con orden estable (regla 96.6).
- Estados: cargando · con datos · vacío («Todavía no tenés canjes. Cuando un cliente retire su producto, lo vas a
  ver acá») · error.
- Si el pago fue rechazado por el banco: «Tu banco rechazó el pago. Revisá los datos de tu cuenta» con el camino
  para corregirla (que inicia el enfriamiento, cláusula 2.2 del contrato).

### C4 — Ofertas y precios
- Ver las ofertas vigentes y cuántos grupos activos tienen cada una con **precio congelado** (y hasta cuándo debe
  sostenerlo). Cambiar el precio afecta solo a grupos nuevos; la pantalla lo advierte antes de guardar.

### C5 — Reclamos
- Reclamos abiertos con su plazo de respuesta, la respuesta del comercio y el historial. Sin datos del
  participante más allá de los de la compra.

## 3. Backoffice (mínimo para el piloto)
- Alta y estado de comercios (resultado de la verificación automática, contrato vigente), retiro de una oferta
  por precio de lista inflado (RF-PX10-02) y bandeja de reclamos escalados. Sin aprobación manual de canjes ni de
  pagos (R8).

## 4. Cobertura de requisitos

| Requisito | Pantallas |
|---|---|
| RF-PX10-01 Alta KYB y precio congelado | C1, C4, backoffice; P1 (R2) |
| RF-PX10-02 Catálogo curado y control de precio de lista | P1, backoffice |
| RF-PX10-03 Canje por QR; si no entrega, no cobra | P5, P6, C2, C3 |
| RF-PX10-04 Comisión al comercio | R4, C3 |
| RF-PX10-05 Reclamos sin retener fondos más allá de L-11 | P7, C5 |
| RF-PX10-06 Opción de recibir el dinero siempre | R1, P4, P5 |

## 5. Abierto

Plazo de canje (`DECISION_REQUIRED` + L-11) · si el canje espera al enfriamiento de la cuenta del participante
(`DECISION_REQUIRED`) · turno temprano pagando por adelantado (F5.M4, `DECISION_REQUIRED` + L-11) · retención
durante un reclamo (L-11) · textos finales (P-16).
