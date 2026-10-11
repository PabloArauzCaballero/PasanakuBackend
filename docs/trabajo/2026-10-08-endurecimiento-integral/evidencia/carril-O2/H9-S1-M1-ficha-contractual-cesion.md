# H9.S1.M1 · Ficha contractual de la cesion del derecho a cobrar un turno

Documento DERIVADO del codigo (`servicios/entregas`, mercado/cesion) y de `evidencia/carril-C/PLAN-CARRIL-C.md`
(ambiguedades A7 a A10 y decision D4). Describe **lo que el sistema hace hoy**, no lo que un contrato deberia decir.
Toda decision legal esta marcada **ABIERTA** y con a quien consultar; el sistema no afirma ninguna. No es un dictamen
juridico ni un texto para el usuario.

## 1. El objeto: que se cede y que NO

| Se cede | NO se cede (queda donde estaba) |
|---|---|
| **El derecho a cobrar el pozo de UN turno** de un grupo (`oferta_turno.turno_id`), por el importe del turno (`monto_derecho`) | **La deuda de aportes**: el cedente sigue obligado a aportar todos los periodos (PLAN carril C §1.2, A10). `aportes` no cambia de deudor |
| El titulo se materializa en `cesion_derecho.participante_destino_id`; desde `TITULO_ASIGNADO`, el pozo de ese turno se entrega a ese participante (`CesionRepositorio.destinoDelTitulo`) | **La membresia**: el comprador ya es miembro activo del mismo grupo (A8); no entra, no sale, no cambia de cupo. Permuta y traspaso de cupo son otros actos (`grupos`, CU-62/64) |
| | El cupo, el administrador del grupo, la reputacion, los avales y la deuda con el fondo de garantia del cedente |

## 2. Partes, precio y cargos

- **Cedente (vendedor)**: titular vigente del turno; solo el publica y cancela su oferta (`AP-CU130-01`).
- **Cesionario (comprador)**: miembro **activo** del mismo grupo, distinto del vendedor (`AP-CU130-06`).
- **Precio** (`monto_precio`) y **cargos** (`monto_cargos`) en la moneda del turno; cargos <= precio (`ck_oferta_turno_cargos`).
  El vendedor cobra `precio - cargos`; los cargos van a la plataforma (**el vendedor los absorbe**, A9). Cuanto son los cargos
  lo debe fijar tarifas (H11): el servicio **no calcula tarifas**. El precio puede ser menor que el derecho (descuento): **no se anuncia como rentabilidad**.
- La plataforma no actua como parte ni como garante del cumplimiento del turno por parte del grupo: solo registra la cesion y
  mueve los fondos de la compra.

## 3. Vigencia y momento en que cambia de manos

1. Una oferta vive hasta `vigente_hasta` (maximo 168 h, **sintetico**, A del carril C). `PUBLICADA` -> `RESERVADA` (un comprador) -> `LIQUIDANDO` -> `VENDIDA`; o `CANCELADA` / `VENCIDA`.
2. **Orden de la saga de compra** (`ComprarOfertaDeTurno`): reservar oferta -> retener saldo del comprador (compensable) -> dar el titulo (compensable mientras el vendedor no cobre) -> pagar al vendedor (**lo irreversible**, al final) -> `LIQUIDADA`.
3. El titulo cambia de mano en `TITULO_ASIGNADO` (antes del pago) y la cesion se da por **liquidada** solo cuando el vendedor cobro. Entre ambos puntos la compra es reanudable y abortable; despues de cobrar **no se deshace a ciegas** (`AP-CU130-09`).
4. Una compra detenida mas de `reserva-maxima` (10 min, sintetico) en `VALIDADA`/`FONDOS_RETENIDOS` se aborta y libera el saldo.
5. **Una sola cesion viva por turno** y **sin reventa encadenada ni fraccionamiento** (A7): el codigo lo impide (`uq_oferta_turno_activa`, una cesion viva).

## 4. Escenarios de responsabilidad (lo que el sistema ya hace y lo que queda sin decidir)

| # | Escenario | Lo que hace el sistema hoy (con prueba) | Decision legal |
|---|---|---|---|
| R1 | El grupo fondea el pozo del turno MIENTRAS se vende | Gana el fondeo: la compra falla (`AP-CU130-02`), el saldo del comprador se libera, la oferta no se liquida (`CU130CompraBordesTest.fondeoGanaAlTitulo`, `CU130CarrerasTest`) | ABIERTA: si el comprador tiene derecho a compensacion por el tiempo retenido |
| R2 | El cedente deja de aportar tras vender | El pozo igual va al comprador; la deuda del cedente se reclama por recuperacion/aval/fondo, **no se descuenta del principal** (A6, A10) | ABIERTA: si el cedente responde ante el comprador y como |
| R3 | El cedente abandona el grupo (CU-65) | La cesion `LIQUIDADA` no se deshace (`CU130CompraBordesTest.abortar`); no hay regla sobre cesion en curso | ABIERTA: que pasa con una cesion en curso |
| R4 | El grupo se disuelve o el turno se reordena antes de cobrar | No modelado: el servicio no escucha `grupos` | ABIERTA: devolucion del precio y orden de pago |
| R5 | El comprador deja de aportar tras comprar | Su morosidad es la de cualquier miembro; **el derecho cedido no se retiene por sus aportes** (no hay compensacion) | ABIERTA: si el derecho adquirido puede retenerse por deuda del comprador |
| R6 | Cancelacion del vendedor y compra a la vez | Una sola transicion vence segun estado y version; la otra recibe `AP-CU130-05`; compra en curso no se cancela ni vence (`CU130LiquidacionCarrerasTest`) | Sin decision pendiente |
| R7 | El proveedor/nucleo falla a mitad | La saga se reanuda sin retener ni pagar dos veces (`CU130CompraTest`, `CU130LiquidacionTrasReinicioTest`) | Sin decision pendiente |
| R8 | El comprador se arrepiente despues de pagar | No hay derecho de retracto modelado; el aborto solo vale antes de que el vendedor cobre | ABIERTA: retracto del consumidor y su plazo |
| R9 | Disputa sobre la validez de la cesion | Sin flujo: la plataforma conserva la evidencia (eventos `entregas.oferta_reservada`, `entregas.derecho_cedido`, `entregas.cesion_fallida`) pero no resuelve | ABIERTA: instancia de resolucion y quien notifica |

## 5. Decisiones abiertas (el sistema NO las toma)

| ID | Pregunta | Supuesto implementado | A quien consultar |
|---|---|---|---|
| L1 | Naturaleza juridica de la cesion (cesion de credito, venta de derecho, otra) y su forma/forma escrita | Cesion de derecho de cobro de un turno, sin garantia de cumplimiento del grupo | Asesoria legal |
| L2 | Responsabilidad del cedente por aportes pendientes y por la existencia del derecho | El cedente conserva sus aportes; el comprador solo recibe el cobro | Asesoria legal |
| L3 | Notificacion/consentimiento del grupo o del organizador para la cesion | No se pide: basta ser miembro activo | Producto + legal + organizador |
| L4 | Retracto, plazo de arrepentimiento y devolucion | No existe | Legal (consumidor financiero) |
| L5 | Tratamiento tributario del precio recibido y de los cargos | No modelado | Fiscal |
| L6 | Tarifa de los cargos y quien la paga | Importe informado, absorbido por el vendedor | Tarifas (H11) + producto |
| L7 | La oferta debe expirar **antes del punto de cierre del beneficiario** del turno (PLAN H9): el codigo hoy impide vender un turno ya fondeado o en entrega, pero NO compara `vigente_hasta` con la fecha de cierre del turno | Parcial (por estado, no por fecha) | Producto: de donde sale esa fecha en `grupos` |
| L8 | Tope de edad/limite de precio para evitar presion o usura entre miembros | Ninguno (solo cargos <= precio) | Producto + legal |
| L9 | Alcance de reventas encadenadas o fraccionamiento | Prohibidos | Producto + legal |

## 6. Evidencia de que la ficha coincide con el codigo

- Reglas de oferta: `ReglasDeOferta.java`; saga: `ComprarOfertaDeTurno.java`, `TransicionesDeCesion.java`; destino del pozo: `CesionRepositorio.destinoDelTitulo`.
- Pruebas: `CU130OfertasTest`, `CU130CompraTest`, `CU130CompraBordesTest`, `CU130CarrerasTest`, `CU130LiquidacionCarrerasTest`, `CU130LiquidacionTrasReinicioTest`, `ContratoDeFondosDelComprador`.
- Contrato con nucleo: `evidencia/carril-C/contrato-nucleo-partidas-y-pago-vendedor.md`.
