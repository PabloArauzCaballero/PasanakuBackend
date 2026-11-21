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
| F1.L1.M03 | Reverso en dos pasos: solicitar y autorizar desde dos tokens (`CuatroOjos`) | `autorizadaPor` no existe en el contrato; el mismo usuario no autoriza su solicitud | pruebas web de matriz negativa + `grep autorizadaPor contratos/nucleo-financiero*` vacío | TODO |
| F1.L1.M11 | Cobro sin canal/monto/referencia/recargo/proveedor del cuerpo | cuerpo con `monto` → 400 por campo no declarado | prueba web por campo prohibido | TODO |
| F1.L1.M01 | Esquema `Importe` en los contratos de entrada | `monto` negativo → 400 en reembolso | prueba web `PagosController` | TODO |
| F1.L1.M07 | BOLA en `aportes` | recurso ajeno → 404 | matriz negativa web | TODO |
| F1.L1.M06 | La firma del webhook cubre el timestamp | reenvío con timestamp nuevo → 401 | prueba unitaria tres niveles + web CU100 | TODO |
| F1.L1.M05 | Secreto del webhook obligatorio, sin literal en `src/main` | arranque sin variable falla | prueba de arranque + grep | TODO |
| F1.L1.M13 | Sello del cierre diario con saldos, moneda y sello anterior | alterar saldo cambia el sello | prueba unitaria | TODO |
| F1.L1.M10 | Bloqueo judicial con permisos propios y segregación | sin permiso → 403; mismo registrador no levanta | pruebas web | TODO |
| F1.L1.M09 | Idempotencia real del reembolso | dos envíos con la misma clave → un reembolso, mismo id | prueba de integración (psql + Java) | TODO |
| F1.L1.M12 | Moneda en reembolso y tablas de importe | reembolso de pago USD queda en USD | psql + prueba | TODO |
| F1.L1.M08 | Hechos de dinero de `aportes` inmutables | `UPDATE aportes.pago` como `svc_aportes` → permission denied | psql con `SET ROLE` | TODO |
| F1.L1.M04 | `nucleo-financiero` arranca sin dobles de desarrollo (adaptador fail-closed) | `ArranqueProduccionTest` sin `@MockitoBean` del puerto | `./gradlew :servicios:nucleo-financiero:test --tests '*ArranqueProduccion*'` | TODO |
| F1.S4.M2 | Destino de transferencia por identificador único | alias duplicado no desvía la transferencia | prueba dirigida | TODO |

## H2 — F2 del lote L1
**CA:** ningún camino de respaldo empresarial ni canal manual en `aportes`/`nucleo-financiero`.
**DoD:** grep de control vacío + suites en verde.
**Estado:** TODO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F2.L1.M01 | Retirar el consumidor de asientos de cobertura empresarial/respaldo | no hay consumidor de esos tópicos | grep en `servicios/nucleo-financiero/src/main` vacío + suite | TODO |
| F2.L1.M02 | Retirar cuentas 9.x y tipos de cobertura empresarial del esquema/semillas | base nueva sin 9.x | psql de verificación | TODO |
| F2.L1.M03 | Catálogo de canales de cobro solo QR y billetera | `INSERT` con `FONDO_GARANTIA` falla | psql | TODO |
| F2.L1.M04 | Cuotas nacen solas al abrirse el periodo | evento duplicado → una cuota por cupo | prueba dirigida | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambios de contrato rompen clientes Dart/TS (`apps/`, fuera de alcance) | CI de clientes rojo | regenerar clientes solo si el CI lo exige; declarar en el PR |
| Archivos compartidos (`sql/` generado, compose, `plataforma`) | fuera de alcance | parte del servicio + A MEDIAS con el cambio exacto |
| Sin Docker | Testcontainers no corre | `NO COMPROBADO — requiere Docker` con el comando |
