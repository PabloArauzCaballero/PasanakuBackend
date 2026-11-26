# Plan — Pasarela de pagos simulada con arquitectura hexagonal

- Fecha: 2026-11-26 · Repos afectados: PasanakuBackend (`servicios/aportes`) · Predecesor: F1.L1.M05/M06 (secreto y firma del webhook)
- Resultado observable: `aportes` habla con la pasarela solo por puertos con tipos propios; hoy los cumple un adaptador
  SIMULADO y determinista (local/TEST), y el adaptador real se enchufa en `infraestructura/pasarela/real` sin tocar
  dominio ni aplicación. En perfiles distintos de `local`/`test` el modo simulado impide el arranque.
- Kill-test: un test de arquitectura que falla si `dominio`/`aplicacion`/`web` importan `infraestructura.pasarela..`; y un
  arranque con perfil `production` + modo simulado que NO levanta.

## Descubrimiento (hechos, con ruta)
- Webhook: `aplicacion/CU100RecibirWebhookPasarela.java:81-168` verifica firma (`dominio/VerificadorDeFirmaWebhook.java`,
  HMAC-SHA256 sobre `timestamp.cuerpo`, ventana ±5 min), dedup por `(proveedor_id, clave_idempotencia)` y concilia contra
  `pago.referencia_proveedor`. **Parsea el JSON del proveedor dentro del caso de uso** (`:119-131`, `:176-195`): ese
  formato es del proveedor y hoy está en la aplicación.
- Secreto: `dominio/ProveedorDeSecretoWebhook.java` (puerto) + `infraestructura/SecretoWebhookDeEntorno.java:131-160`
  (exige `APORTES_WEBHOOK_SECRETO` ≥32 en todos los perfiles; `application.yml:36-38`).
- Persistencia: `infraestructura/WebhookRepositorio.java` (`uq_webhook_idem`), `PagoRepositorio.java:97` (`porReferenciaProveedor`).
- Cobro: `CU21CobrarAporte` acredita síncrono con `referenciaProveedor` que llega del cliente (`:106-111`); **no existe**
  la cadena `orden_cobro → intento_pago` (`CU100…java:43-49`). Ningún código crea órdenes/QR ni consulta estado a una pasarela.
- Enrutado de proveedor: `CU99EnrutarProveedor` (catálogo `proveedor_pago`, `sql/60_semillas/14-proveedores-externos.sql`, código `QR_INTEROP`).
- Fuera de aportes: `nucleo-financiero` tiene su propio puerto de recargas (`ProveedorDeRecargas`, `RecargasProveedorLocal`
  `@Profile({"local","test"})`, `GuardiaDeRecargaDePrueba`): es otro servicio, **no se toca** (regla 98.1). Patrón seguido.
- Perfiles: TEST corre con `PERFIL_SPRING=local` (`despliegue/TEST.md:89`). Único `@SpringBootTest` de aportes: `ArranqueTest`.
- ArchUnit ya presente: `servicios/aportes/src/test/java/bo/aportaya/aportes/ArquitecturaTest.java`.
- Desconocido: el formato real de ningún proveedor (no se inventa; P-15: sin pasarela contratada).

## Alcance
- IN: puertos `PasarelaDePagos`, `InterpreteDeWebhook`, `SimuladorDePagoDePasarela` (dominio); adaptador simulado e
  intérprete JSON plano en `infraestructura/pasarela/simulada`; esqueleto `infraestructura/pasarela/real` que falla
  cerrado; guardia de perfil; CU100 consume `InterpreteDeWebhook` en vez de Jackson; pruebas 3 niveles; regla ArchUnit; doc.
- OUT: conectar `CU21` a `crearOrdenDeCobro` (no hay cadena `orden_cobro`: decisión de producto); endpoint HTTP que dispare
  el webhook simulado; esquema SQL; `nucleo-financiero`; cambiar `PERFIL_SPRING` de TEST; adaptador real.
- Ambigüedades: (1) ¿Debe existir un endpoint para que QA dispare el webhook simulado en el servidor TEST? Supuesto: no,
  queda como puerto + test de integración; confirmar con Pablo. (2) Código de proveedor del doble: se reusa `QR_INTEROP`
  (sembrado) por configuración; confirmar con Pablo.

## H1 — Puertos y adaptador simulado detrás de configuración
**CA:** Dado `aportaya.aportes.pasarela.modo=simulada` en `local`/`test`, cuando se crea una orden de cobro, se consulta su
estado y se emite el webhook de pago, entonces el flujo recorre CU-100 real hasta `PROCESADO` firmado con el secreto del
entorno, sin secreto de proveedor; y con perfil distinto de `local`/`test` el arranque falla nombrando la propiedad.
**DoD:** `./gradlew :servicios:aportes:spotlessCheck test webTest` verde (salida pegada) · ArquitecturaTest verde y
mutado a propósito para verlo fallar · tests de los 3 niveles · doc en `docs/operacion/`.
**Estado:** A MEDIAS

### H1.S1 — Puertos de dominio
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `PasarelaDePagos` + records (`Dinero`, sin float) | Dado monto nulo/≤0 o clave vacía, la solicitud se rechaza | `PasarelaSimuladaTest` inválidos | HECHO |
| H1.S1.M2 | `InterpreteDeWebhook` + `EventoDePago` + `PayloadIlegible` | Cuerpo no JSON ⇒ `PayloadIlegible` | `InterpreteDeWebhookJsonTest` | HECHO |
| H1.S1.M3 | `SimuladorDePagoDePasarela` + `WebhookFirmado` | Emite cuerpo+firma+timestamp verificables | `PasarelaSimuladaTest` correcto | HECHO |

### H1.S2 — Adaptador simulado y selección por configuración
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `PasarelaSimulada` determinista, idempotente por clave | Misma clave ⇒ misma orden; otra monto ⇒ rechazo | `PasarelaSimuladaTest` | HECHO |
| H1.S2.M2 | `InterpreteDeWebhookJson` (mueve el parseo de CU100) | Mismo contrato JSON que hoy | `InterpreteDeWebhookJsonTest` | HECHO |
| H1.S2.M3 | Guardia `!local & !test` + `real/` fail-closed + `application.yml` | production+simulada no arranca | `PasarelaConfiguracionTest` (ApplicationContextRunner) | HECHO |
| H1.S2.M4 | CU100 usa `InterpreteDeWebhook`; tests existentes adaptados | `CU100WebhookTest` sigue verde | `./gradlew :servicios:aportes:test --tests '*CU100*'` | HECHO |
| H1.S2.M5 | `ArranqueTest` con `@ActiveProfiles("test")` | Contexto levanta | `ArranqueTest` | A MEDIAS |

### H1.S3 — Pruebas de integración y arquitectura
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | E2E simulador→CU100: correcto, duplicado, monto distinto, orden inexistente, firma inválida, timestamp al borde | Cada caso devuelve el estado esperado y la base lo confirma | `CU100PasarelaSimuladaTest` | HECHO |
| H1.S3.M2 | Regla ArchUnit pasarela | Importar el adaptador desde dominio/aplicacion/web falla | ArquitecturaTest + mutación manual | HECHO |

### H1.S4 — Documentación y cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S4.M1 | `docs/operacion/pasarela-real-como-enchufar.md` | Dice qué interfaces, config y verificaciones; qué quedó solo contra el doble | lectura + enlace desde REPORTE | HECHO |
| H1.S4.M2 | spotless, push, PR contra `dev`, CI verde, REPORTE | CI verde y PR fuera de borrador | `gh`/MCP checks | A MEDIAS |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin Docker, los tests con base necesitan PostgreSQL local | no poder correr integración | PG16 local + `APORTAYA_TEST_DB_URL`; si no, BLOQUEADO declarado |
| Guardia `!local & !test` rompe un entorno con perfil `staging` | arranque falla | es el comportamiento pedido (fail-closed); documentado |
