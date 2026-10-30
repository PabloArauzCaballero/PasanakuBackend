# Plan — Traspaso del cupo por subasta (puja con pago entre las partes)

- Fecha: 2026-10-30 · Repos afectados: PasanakuBackend (`servicios/entregas`, `servicios/grupos`,
  `servicios/nucleo-financiero`, `servicios/notificaciones`, `apps/movil`, `docs/entidades`, `sql`)
  · Predecesor: `docs/trabajo/2026-10-29-traspaso-de-cupo` y `docs/trabajo/2026-10-30-traspaso-servidor`
  (traspaso SIN precio y CON votación — lo que el usuario no pidió).
- Rama: `claude/traspaso-por-subasta` (desde `origin/test` @ `ebe0ceb0`).
- Resultado observable: el titular de un cupo lo pone en subasta desde la app con un precio base y
  un plazo; **a todos los usuarios** les llega el aviso; cualquiera que cumpla KYC/reputación del
  grupo puja con su saldo (queda retenido); quien queda superado recupera su saldo al instante; al
  cierre gana la oferta más alta: el cupo pasa al ganador y el saliente cobra la puja. Sin votación.
- Kill-test: dos pujas simultáneas por el mismo monto quedan las dos como «más alta», o un postor
  superado no recupera su saldo, o el saliente cobra y el cupo sigue siendo suyo.

## Decisiones del usuario (2026-10-30, por pregunta explícita)
- D1 · Pueden pujar **todos los usuarios** (también los del grupo) que cumplan KYC y reputación del grupo.
- D2 · El pago va **por la app, retenido**: se libera al saliente recién cuando el cupo cambia de dueño.
- D3 · Gana **la mayor oferta al cierre** de un plazo fijo; todos ven la oferta más alta.
- D4 · **Sin votación** del grupo.

## Hallazgos (descubrimiento, con ruta)
- F1 · El traspaso actual (CU-64) es entre integrantes del MISMO grupo, exige votación aprobada
  (`GruposController.java:231`, `R-GRP-10` forzado) y no tiene precio. No es subasta.
- F2 · Ya existe un **mercado** en `entregas` (`MercadoWeb`, `ComprarOfertaDeTurno`,
  `TransicionesDeCesion`, tablas `entregas.oferta_turno` / `entregas.cesion_derecho`) que vende el
  turno a precio fijo con saga: retener saldo → asignar título → pagar al vendedor → evento
  `entregas.derecho_cedido`, que `grupos` aplica cambiando el titular del cupo
  (`AplicarCesionDeDerecho.java`). **La subasta se monta sobre esto** (regla 96.1.6), no en paralelo.
- F3 · Ese mercado **nunca pudo publicar por HTTP**: `HechosDeGruposPorHttp.turno()` devuelve
  `Optional.empty()` siempre (`HechosDeGruposPorHttp.java:46`) porque `grupos` no expone los hechos del turno.
- F4 · Comprar exige ser miembro ACTIVO del grupo (`ComprarOfertaDeTurno.java:62`) y
  `AplicarCesionDeDerecho` rechaza un destino que no sea participante ACTIVO → un ganador de afuera
  no puede entrar sin un alta previa en `grupos`.
- F5 · `nucleo-financiero` solo deja liberar una retención con la sesión de su dueño
  (`BilleteraController.java:188`, `BILLETERA_OPERAR`). Devolver el saldo a un postor superado
  ocurre en la sesión de otro o en un trabajo programado → hace falta liberación servicio-a-servicio
  (`SERVICIO_ENTREGAS`), como ya existe `pagarAlTercero` (`BilleteraController.java:204`).
- F6 · `pagarAlTercero` pregunta a `entregas` el precio de la oferta y exige que sea igual a lo
  retenido: para una subasta adjudicada, el «precio para pago» es la puja ganadora.
- F7 · El esquema sale de `docs/entidades/*.puml` vía `scripts/generar_ddl.py`; columnas nuevas en
  tablas existentes van en `sql/15_infra/columnas_agregadas.sql` (`ADD COLUMN IF NOT EXISTS`).
- F8 · Trabajos programados: paquete `trabajos/` con `@Scheduled` (p.ej. `aportes/trabajos/RecargosDeMoraDiarios.java`).

## Alcance
- IN: lo de los hitos H1–H6.
- OUT: el endpoint CU-64 actual (queda, la app deja de usarlo); compra a precio fijo del mercado
  (no se cambia su comportamiento); comisión/cargos de plataforma en la subasta (= 0, ver A2).
- Ambigüedades registradas (supuesto tomado · a quién confirmar: Pablo):
  - A1 · Incremento mínimo entre pujas: no definido. Supuesto: la nueva puja tiene que superar
    estrictamente a la más alta y ser ≥ precio base.
  - A2 · Cargos de plataforma sobre la puja: no definidos. Supuesto: 0 (no se inventa tarifa, regla 97.5.4).
  - A3 · Plazo de la subasta: el saliente elige entre 24 h, 48 h y 72 h; siempre antes del cierre del turno.
  - A4 · Si el ganador ya es integrante del grupo con otro cupo: se respeta `permite_cupos_multiples` del grupo.
  - A5 · Obligaciones futuras del cupo pasan al ganador; la deuda vencida queda con el saliente (R-GRP-11, CU-64).
  - A6 · «Les llega a todos»: notificación push a todos los usuarios con la app (no solo elegibles). Confirmar si molesta.

## H1 — `grupos` expone lo que la subasta necesita
**CA:** Dado un cupo OCUPADO con turno no cobrado, cuando `entregas` pregunta por su turno, por la
elegibilidad de un usuario y pide el alta del ganador, entonces recibe los hechos reales y el ganador
queda participante ACTIVO del grupo (o se rechaza con código).
**DoD:** `./gradlew :servicios:grupos:test :servicios:grupos:integrationTest` en verde con pruebas nuevas.
**Estado:** A MEDIAS (M1–M3 HECHO; M4 se verifica en el recorrido real)

> Desvío (2026-10-30): H1.M1 era «hechos del turno»; la subasta es del CUPO (antes del sorteo no hay turno), así que la consulta es de subastabilidad del cupo. El arreglo de `HechosDeGruposPorHttp.turno()` del mercado a precio fijo queda FUERA (H3.M2 DESCARTADO).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | `GET /grupos/{g}/cupos/{c}/subastabilidad` (solo el titular): subastable o motivo, y el aporte | titular ve subastable; otro recibe AP-CU64-06; deuda o turno cobrado → no subastable | `integrationTest --tests *CU64SubastaTest` + `webTest` | HECHO |
| H1.M2 | `GET /grupos/{g}/elegibilidad-para-subasta`: restricción, KYC, reputación (con historial), máximo de grupos | elegible/no elegible con motivo, en ese orden | idem | HECHO |
| H1.M3 | `POST /grupos/{g}/cupos/{c}/traspasos-por-subasta` (`SERVICIO_ENTREGAS`): alta del ganador + traspaso en una transacción, idempotente, serializado por candado del cupo | ganador de afuera entra ACTIVO con el cupo; repetir = mismo traspaso; dos cierres a la vez = uno solo; participante → 403 | idem | HECHO |
| H1.M4 | Obligaciones de los períodos futuros al ganador | las emite `aportes` desde el calendario de `grupos`, que lee el titular actual del cupo (`CalendarioDelPeriodoPorHttp.java:41`): no hace falta código. Sin prueba de punta a punta todavía | recorrido H6.M2 | A MEDIAS |

## H2 — `nucleo-financiero` libera retenciones del mercado servicio-a-servicio
**CA:** Dada una retención `COMPRA_EN_MERCADO`, cuando `entregas` la libera con su identidad de
servicio, entonces el saldo vuelve al dueño; una sesión de usuario recibe 403.
**DoD:** `./gradlew :servicios:nucleo-financiero:test` en verde con pruebas nuevas.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | `POST /billetera/retenciones/{id}/liberacion-de-mercado` (`SERVICIO_ENTREGAS`), idempotente | libera; 403 a usuario; repetir = mismo resultado | prueba web + integración | TODO |

## H3 — `entregas`: la subasta
**CA:** Dado un cupo en subasta, cuando varios usuarios pujan, entonces solo la más alta queda
retenida, las superadas se liberan, y al vencer el plazo el cupo pasa a la más alta y el saliente
cobra exactamente esa puja; sin pujas, la subasta queda VENCIDA y nada se mueve.
**DoD:** `./gradlew :servicios:entregas:test :servicios:entregas:integrationTest` en verde; prueba de
concurrencia (dos pujas a la vez); prueba de idempotencia (misma clave dos veces); asientos y cuadre (regla 91.6).
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.M1 | Modelo: tablas `subasta_cupo` + `puja_subasta` en el `.puml` → DDL generado | `generar_ddl.py` sin pendientes; tabla y columna en el SQL | `python3 scripts/generar_ddl.py` | TODO |
| H3.M2 | ~~`HechosDeGruposPorHttp.turno()`~~ — la subasta no usa el mercado por turno | — | — | DESCARTADO (decisión técnica 2026-10-30: la subasta es del cupo y tiene su propio agregado; el defecto del mercado a precio fijo queda anotado en el REPORTE) |
| H3.M3 | Publicar subasta (precio base, plazo) | crea oferta SUBASTA; emite `entregas.subasta_abierta` | prueba de integración | TODO |
| H3.M4 | Pujar: retener → la más alta bajo candado → superada se libera | dos pujas simultáneas: una gana, la otra recibe rechazo y su saldo vuelve | prueba de concurrencia | TODO |
| H3.M5 | Listar subastas abiertas para todos + detalle con la más alta | lista paginada, orden determinista; sin datos personales | prueba web | TODO |
| H3.M6 | Cierre programado: adjudica a la más alta → alta en grupos → cesión con la retención → pago | saliente cobra = puja; cupo del ganador; sin pujas = VENCIDA | prueba de integración | TODO |
| H3.M7 | «Oferta para pago» de una subasta adjudicada = monto ganador, cargos 0 | nucleo acepta el pago | prueba web | TODO |
| H3.M8 | Cancelar subasta sin pujas | con pujas: rechazo; sin pujas: CANCELADA | prueba web | TODO |

## H4 — Aviso a todos los usuarios
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.M1 | `notificaciones` consume `entregas.subasta_abierta` y notifica a todos (idempotente por evento) | un evento = un aviso por usuario | prueba del consumidor | TODO |

## H5 — App: subastar, ver y pujar
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.M1 | «Subastar mi cupo» reemplaza el traspaso con votación | precio base + plazo + confirmación → POST único | `flutter test test/pasanaku/...subasta...` | TODO |
| H5.M2 | Lista «Cupos en subasta» (todos los usuarios) con 4 estados | carga/datos/vacío/error | test de widget | TODO |
| H5.M3 | Detalle + pujar (oferta más alta, mínimo, saldo, hoja de confirmación) | 409 «alguien ofreció más» en el paso; sin doble envío | test de widget | TODO |
| H5.M4 | Gates de la app | analyze, verificar_frontend, suite | comandos | TODO |
| H5.M5 | Capturas 360/412 claro/oscuro miradas | capturas | test de capturas | TODO |

## H6 — Desplegado en TEST y recorrido real
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H6.M1 | PR a `dev` y `test`, desplegar y aplicar SQL | servicios sanos | healthchecks | TODO |
| H6.M2 | Recorrido real: subastar, dos pujas de dos usuarios, cierre, cupo cambia, saldos | asientos y saldos cuadran | script + salida | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Alcance de varios servicios (4) | no se cierra en un turno | se cierra por hitos; el reporte dice exacto dónde quedó |
| Pago al saliente antes de que `grupos` aplique el cupo | plata movida sin cupo | el alta del ganador se hace ANTES del pago (compensable primero, regla 98.3.5) |
| Liberación de superadas falla con `nucleo` caído | saldo retenido de más | estado `SUPERADA` persistido + reintento en el trabajo programado |
