# Consolidado — Auditorías L1 a L6 de Pasanaku (origin/test, 2026-11-20)

Este documento junta los seis informes de solo lectura. Cada hallazgo se verificó en su propia sesión, leyendo el código.
Este consolidado **no los volvió a verificar**: copia sus ID para que se puedan abrir en el informe de origen.
Todos los hallazgos están en el peldaño `DISCOVERED` (regla 30). La única excepción es TYA-DINERO-001, que se reprodujo en una base
local efímera. Ninguna sesión pudo correr Gradle con integración (no hay Docker), ni Flutter, ni el lint o el typecheck
del front, ni la evidencia de dinero de la regla 91.6.

## 1. Informes de origen

| Lote | Rama | Informe | Commit |
|---|---|---|---|
| L1 | `auditoria/aportes-nucleo-financiero-2026-11-20` | `docs/trabajo/2026-11-20-auditoria-aportes-nucleo-financiero/AUDITORIA-aportes-nucleo-financiero.md` | `b97f370d` |
| L2 | `auditoria/garantia-entregas-2026-11-20` | `docs/trabajo/2026-11-20-auditoria-garantia-entregas/AUDITORIA-garantia-entregas.md` | `75769da2` |
| L3 | `auditoria/grupos-organizador-2026-11-20` | `docs/trabajo/2026-11-20-auditoria-grupos-organizador/AUDITORIA-grupos-organizador.md` | `399fb689` |
| L4 | `auditoria/identidad-cumplimiento-2026-11-20` | `docs/trabajo/2026-11-20-auditoria-identidad-cumplimiento/AUDITORIA-identidad-cumplimiento.md` | `74d962eb` |
| L5 | `auditoria/transversal-y-apoyo-2026-11-20` | `docs/trabajo/2026-11-20-auditoria-transversal-y-apoyo/AUDITORIA-transversal-y-apoyo.md` | `6b46e2bd` |
| L6 | `auditoria/frontends-compartidos-2026-11-20` | `docs/trabajo/2026-11-20-auditoria-frontends-compartidos/AUDITORIA-frontends-compartidos.md` | `a607edbd` |

## 2. Totales (tomados de la fila «Total» de cada informe)

| Lote | S1 | S2 | S3 | S4 | Total |
|---|---|---|---|---|---|
| L1 aportes-nucleo-financiero | 8 | 20 | 16 | 1 | 45 |
| L2 garantia-entregas | 13 | 25 | 24 | 6 | 68 |
| L3 grupos-organizador | 2 | 25 | 25 | 3 | 55 |
| L4 identidad-cumplimiento | 3 | 61 | 81 | 31 | 176 |
| L5 transversal-y-apoyo | 5 | 34 | 41 | 8 | 88 |
| L6 frontends-compartidos | 1 | 14 | 24 | 3 | 42 |
| **Total** | **32** | **179** | **211** | **52** | **474** |

Puede haber hallazgos duplicados entre lotes, porque un mismo problema se ve desde ambos lados de una conexión. La sección 4 los agrupa.
L6 leyó completa solo alrededor del 54 % de su huella; el resto lo escaneó o lo muestreó.

## 3. Los 32 S1

| # | ID | Qué pasa | Tema (§4) |
|---|---|---|---|
| 1 | L1 FLUJO-QR-001 | El aporte se da por cobrado con lo que declara el cliente, sin confirmación de la pasarela | T1 |
| 2 | L1 FLUJO-QR-002 | El webhook de la pasarela no confirma nada: solo archiva lo que recibe | T1 |
| 3 | L1 FLUJO-QR-003 | El «confirmado» que usa la entrega del pozo sale de `monto_pagado`, no del mayor ni de la pasarela | T1 |
| 4 | L1 FLUJO-QR-004 | Si el canal no es billetera, `nucleo-financiero` no mueve dinero pero la cuota igual queda pagada | T1 |
| 5 | L1 DINERO-001 | Un reembolso con importe negativo pasa las validaciones y, al aprobarse, aumenta lo pagado de la cuota | T5 |
| 6 | L1 UX-A11Y-001 | La app permite declarar un aporte escribiendo la referencia de un comprobante | T1 |
| 7 | L1 SEG-005 | Los «cuatro ojos» del reverso de billetera se cumplen escribiendo cualquier UUID en el cuerpo | T3 |
| 8 | L1 RESPALDO-003 | El consumidor que asienta la cobertura empresarial está activo en el despliegue de TEST | T2 |
| 9 | L2 RESPALDO-001 | Cada fondeo con faltante dispara automáticamente la cobertura con dinero de la empresa | T2 |
| 10 | L2 RESPALDO-002 | Reservar, ampliar, liberar y reversar el respaldo empresarial siguen expuestos y mueven caja de la empresa | T2 |
| 11 | L2 RESPALDO-003 | El esquema trata el respaldo empresarial como fuente del pozo (6 tablas, R-GAR-08..13, `monto_cubierto_empresa`) | T2 |
| 12 | L2 DINERO-001 | `ejecutarEntrega` marca ENTREGADA con un monto libre y puede convivir con un desembolso en curso | T5 |
| 13 | L2 DINERO-003 | `POST /entregas` (versión anterior) sigue vivo: confía en bruto y recaudado del cliente, ignora la idempotencia y admite EFECTIVO_ORGANIZADOR | T3 |
| 14 | L2 DEBIDO-PROCESO-001 | El aval se ejecuta y la deuda se subroga sin expediente firme y antes de que pague el avalista | T7 |
| 15 | L2 SEG-001 | `recuperarRespaldo` pasa dinero del pozo a la caja de la empresa con un pago que solo afirma el cliente | T1/T2 |
| 16 | L2 SEG-002 | Los endpoints `GRUPO_ADMINISTRAR` de garantía no comprueban que el organizador sea de ese grupo (BOLA) | T4 |
| 17 | L2 SEG-003 | El organizador cubre un incumplimiento con días de mora y aprobador que manda en el cuerpo | T3 |
| 18 | L2 SEG-004 | La devolución del fondo se decide con `grupoCerrado` y `deudasVivas` que manda el cuerpo | T3 |
| 19 | L2 SEG-005 | Una persona marca a mano el desembolso como acreditado (`exitoso=true`) y eso genera el asiento en el mayor | T1 |
| 20 | L2 SEG-006 | El desembolso no comprueba que la cuenta destino sea del beneficiario | T4 |
| 21 | L2 SEG-007 | El cliente puede mandar su propio `numeroCifrado`, distinto del número que se enmascara y deduplica | T3 |
| 22 | L3 DINERO-002 | La transferencia por «alias» resuelve el destino con un nombre que no es único, usando `LIMIT 1` sin orden | T5 |
| 23 | L3 SEG-001 | El pedido de retiro de un grupo no comprueba quién llama: el creador puede retirar a otro participante | T4 |
| 24 | L4 IDCU-SEG-075 | Suspender, bloquear o dar de baja una cuenta no impide volver a iniciar sesión | T6 |
| 25 | L4 IDCU-HUECO-077 | El segundo factor de los operadores no existe; en TEST cualquier factor es `000000` | T6 |
| 26 | L4 IDCU-SEG-119 | Los «cuatro ojos» de la diligencia PEP los declara el cliente HTTP con dos UUID cualesquiera | T3 |
| 27 | L5 TYA-DINERO-001 | El mayor acepta líneas tardías y negativas en asientos ya confirmados, y ningún control lo detecta (**reproducido**) | T5 |
| 28 | L5 TYA-DINERO-002 | La devolución de comisión no es idempotente, no tiene candado y toma el autorizante del cuerpo | T3/T5 |
| 29 | L5 TYA-FLUJO-QR-001 | El esquema permite acreditar un pago sin QR ni webhook con firma válida | T1 |
| 30 | L5 TYA-APROB-001 | Cualquier participante puede marcar una comisión como cobrada o fallida por HTTP | T1/T4 |
| 31 | L5 TYA-SEG-001 | Un participante elige su propio descuento, su exención total y a quién se le cobra la comisión | T3 |
| 32 | L6 DINERO-002 | `CampoMonto` borra todos los puntos: «12.50» se envía como `1250.00` (×100) al recargar, retirar, transferir, pujar, subastar y crear un grupo (reproducido como función; no corrido en dispositivo) | T5 |

L1 HUECO-006 se descartó como S1 durante la revisión de la propia sesión: quedó en S3. `frontends-compartidos-RESPALDO-002` es S2 en L6, porque el camino vivo de la cobertura es S1 y se cuenta en L2.

## 4. Temas transversales (lo que se repite entre lotes)

- **T1 — El dinero avanza sin QR confirmado (choca con R-NUEVA-1 y la regla 91.3).** Hay tres caminos. El cobro se registra por lo que declara el cliente (L1). Los cobros, las comisiones y los desembolsos se marcan a mano (L2 SEG-005, L5 APROB-001). El esquema admite pagos sin webhook firmado (L5). El webhook archiva y no acredita (L1 FLUJO-QR-002), así que hoy **no existe** el camino «QR → webhook → aporte cobrado».
- **T2 — Respaldo empresarial vivo y automático (choca con R-NUEVA-2).** El fondeo con faltante llama a `CU23CubrirConRespaldo` por HTTP (L2 RS-01). `nucleo-financiero` lo asienta, con el consumidor activo en TEST (L1 RS-04). Las cuentas 9.x se siembran también en producción (L5 RS-01). El mock y los clientes publican `cubiertoEmpresa` (L6). Los textos públicos prometen que «el fondo cubre la entrega» (L6 RE-01..06).
- **T3 — Autorización o datos de negocio que se toman del cuerpo de la petición.** Aprobadores, «cuatro ojos», días de mora, `grupoCerrado`, descuentos, el monto de la entrega y `numeroCifrado` llegan desde el cliente. Aparece en L1, L2, L4 y L5.
- **T4 — Falta el control de propiedad del recurso (BOLA/IDOR).** Garantía (L2 SEG-002), la cuenta destino del desembolso (L2 SEG-006), el retiro de un grupo (L3 SEG-001) y las comisiones (L5).
- **T5 — Integridad del importe y del mayor.** Líneas tardías y negativas en el mayor (L5, reproducido); el reembolso negativo (L1); el monto libre de la entrega (L2); el ×100 del front (L6); el alias resuelto con `LIMIT 1` (L3).
- **T6 — Acceso.** Las cuentas suspendidas siguen entrando (L4); los operadores no tienen segundo factor real (L4).
- **T7 — Debido proceso (regla 91.4).** El aval se ejecuta sin expediente firme (L2); la sanción baja la reputación de forma automática (L2 DEBIDO-PROCESO-003); el recargo se calcula automáticamente (L1).
- **T8 — Fuentes que se contradicen.** ADR-051 (regla 7), el catálogo `aporte-mora-cobertura.yaml` (`cubrir-faltante`, `recuperar-respaldo`), los CU-22 y CU-23, y los contratos `garantia.yaml` y `entregas.yaml` describen el respaldo como vigente, y `scripts/validar_flujos.py` los da por buenos.
- **T9 — Candado del repo.** `blocker_gate` impide cerrar cualquier sesión porque hay 9 microtareas viejas, de planes del 2026-10-03 al 2026-10-30, en BLOQUEADO o EN CURSO. Lo reportó L5, y hoy afecta a cualquier sesión que trabaje en el repo.

## 5. Inventario unificado de aprobaciones

Los informes suman unas **79 filas**: L1 14, L2 18, L3 6, L4 17, L5 12 y L6 8. Varias se repiten entre lotes.
Agrupadas por punto de control real:

| Punto de control | Filas de origen | Clasificación que dieron las sesiones |
|---|---|---|
| Declarar el pago o subir un comprobante; marcar como cobrado a mano | L1 AP-01, AP-06, AP-10 · L5 AP-01..04 · L2 AP-03, AP-05 | ELIMINABLE (reemplazar por QR + webhook) |
| Generar las cuotas del periodo a mano | L1 AP-02 | ELIMINABLE (automatizar) |
| Admisión humana a un grupo (el organizador propone y backoffice resuelve) | L3 APR-01 · L6 AP-04..06 · L4 APR-17 | ELIMINABLE; `application.yml:74` la marca «DECISION PENDIENTE (negocio y cumplimiento)» |
| Inicio del grupo y sorteo disparados a mano | L3 APR-02 | ELIMINABLE (ADR-054 D3); hoy **no hay** trabajo programado que los reemplace |
| Habilitación del organizador profesional / postulaciones | L3 APR-03 · L4 APR-16 · L6 AP-07, AP-08 | DECISIÓN REQUERIDA (A-1) |
| Autorizar la entrega (R-SEG-04) y ejecutarla a mano | L2 AP-01, AP-02, AP-04 | AP-01: DECISIÓN REQUERIDA · ejecución: ELIMINABLE |
| Verificar la cuenta destino | L2 AP-06 | DECISIÓN REQUERIDA (A-1) |
| KYC con revisión humana (la app consulta cada 30 s) | L4 APR-01, APR-12 · L6 AP-01, AP-02 | DECISIÓN REQUERIDA (A-1) |
| Doble aprobación de retiros a partir de 5000.00 (ADR-049) | L1 AP-08 · L4 APR-07 · L6 AP-03 · L2 AP-17 | DECISIÓN REQUERIDA (A-1). `docs/Cumplimiento.md:170-176` dice que la norma pide niveles de autorización aprobados por el Directorio, no aprobación operación por operación |
| Quién instruye el pago de un retiro autorizado | L1 AP-14 · L4 FLUJO-FALTANTE-036 | DECISIÓN REQUERIDA; hoy **ningún retiro sale** |
| Reembolsos, reversos y cierre de billetera con segunda persona | L1 AP-03, AP-09, AP-12 | DECISIÓN REQUERIDA |
| Bloqueo judicial y requerimientos de la autoridad | L1 AP-11 · L4 APR-11 | CONSERVAR POR NORMA o DECISIÓN REQUERIDA (Cumplimiento) |
| Diligencia PEP, caso LFT y reporte UIF (analista ≠ revisor) | L4 APR-08..10 | CONSERVAR POR NORMA (`docs/Cumplimiento.md` §2) |
| Confirmación humana de acciones sensibles (R-ORG-06) | L2 AP-15 · L3 APR-04 | Sanción: CONSERVAR (91.4) · entrega, cobro y cobertura: ELIMINABLE |
| Descargo, declaración de incumplimiento, aval y restricción | L2 AP-08..11 | Debido proceso: CONSERVAR (91.4); falta decidir quién resuelve sin organizador |
| Reemplazo de participante, disolución y devolución del fondo | L2 AP-12, AP-13 | DECISIÓN REQUERIDA (ADR-054) |
| Retiro voluntario de un participante del grupo (`PENDIENTE`, nadie lo aprueba) | L3 APR-06 | ELIMINABLE o DECISIÓN REQUERIDA (A-4) |
| Gobierno interno (tarifario por comité, ERP, publicidad, reseñas, roles internos) | L5 AP-05..10 · L4 APR-04, APR-14 | CONSERVAR (fuera del circuito); el tarifario es DECISIÓN REQUERIDA |

## 6. Inventario unificado de respaldo y cobertura

| Qué | Origen | Clasificación |
|---|---|---|
| Cobertura automática del faltante del pozo con la reserva de la empresa (`CU22EntregarPozoCompleto` → `RespaldoPorHttp` → `CU23CubrirConRespaldo`) | L2 RS-01 · L5 RS-03 | CONTRADICE R-NUEVA-2 — **camino vivo** |
| Reservar, ampliar, liberar, recuperar y reversar el respaldo; plan de contingencia | L2 RS-02..05, AP-14, AP-18 | CONTRADICE R-NUEVA-2 |
| Consumidor de asientos de cobertura y respaldo en `nucleo-financiero` (activo en TEST) | L1 RS-04 · L5 RS-02 | CONTRADICE R-NUEVA-2 |
| Cuentas 9.x «Caja de la empresa» y «Reserva de respaldo», sembradas también en producción | L1 RS-05 · L2 RS-07 · L5 RS-01 | CONTRADICE R-NUEVA-2 · falta decidir qué hacer con lo ya asentado (A-3) |
| Tablas `*_respaldo*`, `fondeo_entrega.monto_cubierto_empresa`, R-GAR-08..13 | L2 RS-06 · L5 RS-04 | CONTRADICE R-NUEVA-2 · A-3 |
| Canal `FONDO_GARANTIA` para pagar una cuota | L1 RS-03 · L5 RS-10 | L1: CONTRADICE · L5: fondo mutual, A-3 |
| **Fondo mutual** (dinero de los participantes) que cubre al que no pagó; `politica_cobertura`; `cubiertoMutual` | L2 RS-09 · L1 RS-02 · L5 RS-05 | DECISIÓN REQUERIDA (A-3) |
| Aval entre participantes (`CU26EjecutarAval`) | L2 RS-10 · L3 RSP-06 · L4 RSP-03 | NO APLICA (tiene defectos de debido proceso) |
| Recargo de mora que va a la reserva de respaldo | L1 RS-06 · L2 A-4 | DECISIÓN REQUERIDA (A-4) |
| Cuenta con `permiteNegativo`, que acredita al pozo sin saldo | L1 RS-07 | DECISIÓN REQUERIDA |
| La empresa como acreedora del deudor (`acreedor PLATAFORMA`) | L2 RS-08 | DECISIÓN REQUERIDA |
| Textos públicos y de la app: «el fondo cubre la entrega», «el turno se entrega igual» | L6 RE-01..07 · L4 RSP-01 · L2 RS-11 · L1 RS-09 | CONTRADICE R-NUEVA-2 si el «fondo» es de la empresa; A-3 si es mutual |
| Plantillas, insignias, reportes e indicadores de cobertura | L5 RS-06..09 · L2 RS-11 | DECISIÓN REQUERIDA (A-3) |
| ADR-051, catálogo de flujos, CU-22/23, contratos OpenAPI y pactos | L2 RS-12 · L3 RSP-01, RSP-02 | CONTRADICE R-NUEVA-2 (documentación) |
| Métrica `coberturas_consumidas` del organizador | L3 RSP-04 | DECISIÓN REQUERIDA |

## 7. Ambigüedades A-1 a A-4 (ninguna está decidida)

| ID | Lo que muestran los seis informes | A quién confirmar |
|---|---|---|
| A-1 | Hay controles humanos de tres tipos: (a) **con norma citada en el repo**: diligencia PEP, LFT/ROS, reporte UIF, requerimientos; (b) **sin norma citada**: KYC manual, doble aprobación de retiros, cuatro ojos en la entrega, verificación de cuenta, habilitación del organizador, reembolsos y reversos; (c) **ELIMINABLE** según las sesiones: admisión, inicio, sorteo, comprobantes | Pablo; Cumplimiento para (a) y (b) |
| A-2 | Cuando falta plata al corte, hoy la entrega queda **BLOQUEADA_POR_FONDO_INCOMPLETO** con incidencia CRÍTICA. ADR-051:113 prohíbe la entrega parcial. No hay rama de entrega diferida, devolución ni reasignación | Pablo |
| A-3 | El respaldo empresarial está vivo. El fondo mutual está vivo pero no deja asiento (L2 HUECO-001). El aval está vivo, con defectos | Pablo |
| A-4 | El recargo se calcula solo y su destino es ambiguo: fondo de garantía o reserva de respaldo. La política de mora (2 %) no tiene procedencia. La mora no avanza con el tiempo (L1 DEFECTO-003) | Pablo; Cumplimiento o Finanzas |

## 8. Preguntas a responder antes del plan de correcciones

Estas preguntas no se respondieron con supuestos. El plan las espera.

**Flujo nuevo**
- P-01 (A-2) Cuando al corte falta plata porque alguien no pagó, ¿qué pasa con quien tenía el turno? ¿Se espera hasta completar (como hoy), se entrega lo que hay, se difiere, se devuelve a los que pagaron o se reasigna el turno?
- P-02 (A-3) ¿R-NUEVA-2 alcanza también al **fondo mutual** (5 % que aportan los participantes)? ¿Se elimina, se mantiene o se redefine?
- P-03 (A-3) ¿El aval entre participantes se mantiene?
- P-04 (A-3) ¿Qué se hace con lo construido para el respaldo empresarial: borrarlo, desactivarlo y conservar el histórico, o dejarlo de solo lectura? ¿Hay asientos reales en las cuentas 9.x en algún entorno?
- P-05 (A-4) Sin cobertura, ¿existe el recargo por mora? Si existe, ¿con qué tasa, de qué fuente y a dónde va el dinero?
- P-06 ¿Pagar la cuota con saldo de billetera recargado por QR cuenta como «pago por QR»? ¿Y la transferencia P2P?
- P-07 ¿El comprobante manual desaparece del todo, o queda solo como evidencia para un reclamo atendido por una persona?

**Aprobaciones (A-1)**
- P-08 KYC: ¿sigue la revisión humana del expediente o se pasa a un proveedor automático? ¿Cuál?
- P-09 Doble aprobación de retiros a partir de 5000.00: ¿se mantiene, se reemplaza por la política de niveles del Directorio o se elimina? ¿En qué moneda está ese umbral?
- P-10 «Cuatro ojos» para autorizar la entrega del pozo (R-SEG-04): ¿se mantiene ahora que el pago por QR mueve el flujo?
- P-11 Habilitación del organizador profesional: ¿sigue existiendo el organizador humano opcional? Si existe, ¿lo aprueba backoffice?
- P-12 R-ORG-06 y debido proceso: sin organizador obligatorio, ¿quién resuelve un descargo y quién declara un incumplimiento? ¿Puede derivarse del vencimiento sin pago?
- P-13 Reembolsos, reversos y cierre de billetera con segunda persona: ¿se conservan?

**Operación y entorno**
- P-14 ¿Hoy se mueve dinero real en algún entorno (TEST o producción), o todo sigue condicionado al dossier H15? Define la urgencia de los S1.
- P-15 ¿Qué pasarela QR y qué proveedor de desembolso existen hoy? ¿Cuál es su contrato de webhook y su firma?
- P-16 ¿Quién escribe los textos nuevos de la web y de la app (la promesa de «el fondo cubre»)? ¿Se retiran ya?
- P-17 Descuentos y exenciones de comisión (`tarifas`): ¿quién puede darlos y con qué regla?
- P-18 Orden de ataque: ¿primero los S1 de seguridad y de integridad del mayor, independientes de las decisiones de negocio, y después el flujo nuevo? ¿Un PR por módulo hacia `dev`? ¿Quién revisa (NO_SELF_APPROVAL)?
- P-19 ¿Dónde se puede correr la integración (Docker) para producir la evidencia 91.6 y 98.8? En la nube no hay Docker.
- P-20 ¿Se cierran las 9 microtareas viejas que traban `blocker_gate` (T9)? ¿Quién decide su estado?
