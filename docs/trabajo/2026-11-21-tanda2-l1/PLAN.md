# Plan — Tanda 2, lote L1 (`aportes` y `nucleo-financiero`)

- Fecha: 2026-11-21 · Repo: `PabloArauzCaballero/PasanakuBackend` · Base: `origin/dev` 50cf3be9
- Predecesor: [plan de correcciones](../2026-11-21-plan-correcciones/PLAN.md) · anexo [L1](../2026-11-21-plan-correcciones/anexos/ANEXO-L1-aportes-nucleo-financiero.md) · #411 (mayor en la base) · #412 (actor del token, `CuatroOjos`)
- Resultado observable: en `aportes` y `nucleo-financiero`, ningún cuerpo de petición decide una identidad, un aprobador, un canal o un importe que el servidor puede leer; los logs no llevan importes; los hechos de dinero quedan protegidos por la base.
- Kill-test: un `POST /billetera/reversos` con `autorizadaPor` inventado que reverse una transacción; un `POST /aportes/obligaciones/{id}/pagos` con `monto=1.00` que marque pagada una cuota de 100.00.

## Alcance
- IN: `servicios/aportes/**`, `servicios/nucleo-financiero/**`, `contratos/**` y `pactos/**` de esos dos servicios, `docs/entidades/*.puml` de esos dos servicios, sección propia de `docs/Restricciones.md`, esta carpeta.
- OUT: `plataforma/**`, `.github/**`, `build.gradle*` raíz, `docker-compose*.yml` raíz, `apps/**`, `packages/**`, servicios ajenos (`entregas` CU28/CU22 loguean importes: se anota, no se toca).
- Ambigüedades: AMB-L1-3 (bloqueo judicial humano) — supuesto del anexo: sí, con dos personas; confirmar con Cumplimiento.

## Reglas de ejecución de este carril
- Un PR por microtarea o grupo con causa común, rama `correcciones/tanda2-l1-<tema>`, hacia `dev`, con `Largo plazo:`.
- Pruebas Testcontainers: `NO COMPROBADO — requiere Docker`; las invariantes de base se prueban con `psql` en 5433.
- Un build de Gradle a la vez.

## H1 — F1 del lote L1 (seguridad, dinero, trazabilidad)
**CA:** Dado cualquier endpoint de `aportes`/`nucleo-financiero` del anexo F1, cuando se lo llama con datos inventados en el cuerpo, sin propiedad o sin permiso, entonces responde 4xx sin efecto.
**DoD:** pruebas web dirigidas en verde (`./gradlew :servicios:<s>:test`), `spotlessCheck` y `testBarrido` en verde, CI del PR en verde.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| L1.M00 | CU10/CU12/CU21 sin importes en logs; CU10 bajo 300 líneas formateado (F4.L1.M02 parte, pedido explícito) | Ningún `monto={}` en logs de los dos servicios; `spotlessCheck` y `testBarrido` en verde en `nucleo-financiero` | `grep -rn 'monto={}' servicios/{aportes,nucleo-financiero}/src/main` vacío · `./gradlew :servicios:nucleo-financiero:spotlessCheck :servicios:nucleo-financiero:testBarrido :servicios:aportes:testBarrido` | HECHO (`evidencia/M00-logs-sin-importes.txt`; CU10 ya estaba en 281 líneas y formateado en `dev` por f7f83a97) |
| F1.L1.M03 | Reverso en dos pasos: solicitar y autorizar desde dos tokens (`CuatroOjos`) | `autorizadaPor` no existe en el contrato; el mismo usuario no autoriza su solicitud | pruebas web de matriz negativa + `grep autorizadaPor contratos/nucleo-financiero*` vacío | HECHO — PR #425 (`evidencia/M03-*`) |
| F1.L1.M11 | Cobro sin canal/monto/referencia/recargo/proveedor del cuerpo | cuerpo con `monto` → 400 por campo no declarado | prueba web por campo prohibido | TODO — cambia el contrato que usa `apps/movil/.../cu21_cobrar_aporte.dart` (L6): coordinar |
| F1.L1.M01 | Esquema `Importe` en los contratos de entrada | `monto` negativo → 400 en reembolso | prueba web `PagosController` | A MEDIAS — PR #445: `Importe` en reembolso y disputa, borde 400 en el resto; falta `Importe` en nucleo (app L6) |
| F1.L1.M07 | BOLA en `aportes` | recurso ajeno → 404 | matriz negativa web | A MEDIAS — PR #441: estado y reembolso; morosos, primera-cuota y recaudo requieren identidad de servicio en grupos/entregas/garantia |
| F1.L1.M06 | La firma del webhook cubre el timestamp | reenvío con timestamp nuevo → 401 | prueba unitaria tres niveles + web CU100 | HECHO — PR #430 (`evidencia/M06-*`) |
| F1.L1.M05 | Secreto del webhook obligatorio, sin literal en `src/main` | arranque sin variable falla | prueba de arranque + grep | HECHO — PR #450 (`evidencia/M05-*`); ArranqueTest con Docker NO COMPROBADO |
| F1.L1.M13 | Sello del cierre diario con saldos, moneda y sello anterior | alterar saldo cambia el sello | prueba unitaria | HECHO — PR #436 (`evidencia/M13-*`) |
| F1.L1.M10 | Bloqueo judicial con permisos propios y segregación | sin permiso → 403; mismo registrador no levanta | pruebas web | HECHO — #458 (registrado_por, cuatro ojos, liberacion) + PR de permisos y ruta HTTP (`evidencia/M10-*`, `evidencia/M10b-*`) |
| F1.L1.M09 | Idempotencia real del reembolso | dos envíos con la misma clave → un reembolso, mismo id | prueba de integración (psql + Java) | HECHO — PR #447 (solicitud) + PR #456 (aprobación y alta de proveedor) (`evidencia/M09-*`, `evidencia/M09b-*`) |
| F1.L1.M12 | Moneda en reembolso y tablas de importe | reembolso de pago USD queda en USD | psql + prueba | HECHO — PR #449 (reembolso, disputa) + PR #454 (conciliacion, excepcion, extracto) (`evidencia/M12-*`, `evidencia/M12b-*`); columnas nullable (fase expandir, regla 97.3.5) |
| F1.L1.M08 | Hechos de dinero de `aportes` inmutables | `UPDATE aportes.pago` como `svc_aportes` → permission denied | psql con `SET ROLE` | A MEDIAS — PR #455: pago y reembolso sellados con historial de estados (R-BIL-30); falta webhook_pasarela (exige particionarla por su retención de 24 meses) |
| F1.L1.M04 | `nucleo-financiero` arranca sin dobles de desarrollo (adaptador fail-closed) | `ArranqueProduccionTest` sin `@MockitoBean` del puerto | `./gradlew :servicios:nucleo-financiero:test --tests '*ArranqueProduccion*'` | HECHO — PR #451 (`evidencia/M04-*`) |
| F1.S4.M2 | Destino de transferencia por identificador único | alias duplicado no desvía la transferencia | prueba dirigida | DESCARTADO para L1 — vive en `grupos` (F1.L3.M03, lote L3); decidido por el alcance de la tanda ("si vive en tus servicios") |

## H2 — F2 del lote L1
**CA:** ningún camino de respaldo empresarial ni canal manual en `aportes`/`nucleo-financiero`.
**DoD:** grep de control vacío + suites en verde.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F2.L1.M01 | Retirar el consumidor de asientos de cobertura empresarial/respaldo | no hay consumidor de esos tópicos | grep en `servicios/nucleo-financiero/src/main` vacío + suite | A MEDIAS — PR #452: tópicos y CAJA_EMPRESA fuera; RESERVA_RESPALDO/POZO_GRUPO/DESEMBOLSO los usan caminos vigentes (F3) |
| L1.P01 | Pedido de L2 (coordinador, 2026-11-21): retirar `OfertasDelMercado`/`OfertasPorHttp`, el pago a tercero, `PactoConEntregasContratoTest` y el pacto `nucleo-financiero-entregas.json` | `pagarAlTercero` responde 410 `AP-CU13-10` sin efecto; nada en `nucleo-financiero` consulta la oferta de `entregas` | `./gradlew :servicios:nucleo-financiero:test :servicios:nucleo-financiero:webTest` + `grep -rn OfertasPorHttp servicios/nucleo-financiero` vacío | HECHO — PR #461 (`evidencia/L2PEDIDO-retiro-ofertas.txt`) |
| F2.L1.M02 | Retirar cuentas 9.x y tipos de cobertura empresarial del esquema/semillas | base nueva sin 9.x | psql de verificación | TODO |
| F2.L1.M03 | Catálogo de canales de cobro solo QR y billetera | `INSERT` con `FONDO_GARANTIA` falla | psql | TODO |
| F2.L1.M04 | Cuotas nacen solas al abrirse el periodo | evento duplicado → una cuota por cupo | prueba dirigida | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambios de contrato rompen clientes Dart/TS (`apps/`, fuera de alcance) | CI de clientes rojo | regenerar clientes solo si el CI lo exige; declarar en el PR |
| Archivos compartidos (`sql/` generado, compose, `plataforma`) | fuera de alcance | parte del servicio + A MEDIAS con el cambio exacto |
| Sin Docker | Testcontainers no corre | `NO COMPROBADO — requiere Docker` con el comando |
