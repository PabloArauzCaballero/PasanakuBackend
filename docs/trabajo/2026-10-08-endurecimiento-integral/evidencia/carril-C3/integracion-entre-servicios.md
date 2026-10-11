# Integración entre servicios: inversiones ↔ nucleo-financiero y nucleo-financiero ↔ entregas (carril C3)

Todo contra el contrato publicado en `contratos/pactos/*.json` (fuente única de cada par). Estado honesto por par.

## 1. inversiones → nucleo-financiero (libro)

Pacto: `contratos/pactos/inversiones-nucleo-financiero.json`. Siete interacciones del consumidor (saldo, retener, debitar,
acreditar, retener-segunda, liberar, retener-sin-saldo) y dos solo de productor (reintento con la misma clave; debitar como persona = 403).

| Lado | Prueba | Qué es real | Qué es doble |
|---|---|---|---|
| Consumidor | `servicios/inversiones/.../LibroPorHttpPactoContratoTest` | el cliente real `LibroPorHttp`, HTTP real | el servidor (HttpServer del JDK que responde lo pactado) |
| Productor | `servicios/nucleo-financiero/.../CU13PactoInversionesTest` | el contexto Spring COMPLETO de nucleo, HTTP real, PostgreSQL real, verificación RS256 de verdad contra un JWKS servido por HTTP | `aportes` (doble HTTP con la forma de su OpenAPI) e identidad (el token se firma en la prueba con la forma de identidad) |

Lo que se afirma: lo que el cliente real MANDA (método, ruta, cuerpo, quién firma, `Idempotency-Key` UUID estable) es lo que el productor real
ACEPTA, y lo que el productor responde es lo que el cliente ENTIENDE; el libro cuadra (titular 1000,00 → −300 → +300; custodia en delta cero;
una transacción `INVERSION` y una `RESCATE_INVERSION`). Negativos reales: sin token, token de otra clave y token vencido → 401; credencial de
`SERVICIO_ENTREGAS` en la ruta de inversiones → 403; persona con `BILLETERA_OPERAR` → 403.

**No es un cliente real contra un servidor real en el mismo proceso.** Son dos pruebas unidas por el mismo archivo de pacto. Un cliente de
inversiones apuntando a un nucleo levantado por Testcontainers exige que el módulo de inversiones dependa del de nucleo en pruebas
(viola el invariante 11: ningún servicio importa a otro). Se declara así.

## 2. nucleo-financiero → entregas (pagarAlVendedor)

Pacto: `contratos/pactos/nucleo-financiero-entregas.json`.

* `entregas` YA publica `GET /entregas/mercado/ofertas/{ofertaId}` como `consultarOfertaParaPago` (`entregas.yaml`, esquema `OfertaParaPago`,
  422 `AP-CU130-05`, exige `SERVICIO_ENTREGAS`) y lo implementa (`MercadoWeb.consultarOfertaParaPago`). El contrato propuesto del carril C y
  el publicado coinciden: `PactoConEntregasContratoTest.entregasPublicaLaOferta` lo compara contra el YAML REAL de entregas.
* `OfertasPorHttp` (cliente real de nucleo) contra un servidor HTTP del JDK con lo pactado: correcto, cada estado publicado, 422, 500, cuerpo ilegible,
  oferta ajena y sin vendedor (todo falla cerrado: no se paga).
* `pagarAlTercero` ya se probó de punta a punta en nucleo (`CU13MercadoTest`) con la oferta como doble de puerto (`Ofertas`).

**Queda contra doble:** el proceso real de `entregas` respondiendo a nucleo. Para cerrarlo falta el entorno completo (compose `todo` con
identidad, entregas y nucleo) y un token `SERVICIO_ENTREGAS` real: la cuenta de servicio de entregas no existe (ver `credenciales-de-servicio.md`) y
nucleo hoy PROPAGA el token de quien llama en vez de emitir uno propio. Prueba a correr cuando ambos estén levantados:
`pagarAlTercero` sobre una retención `COMPRA_EN_MERCADO` creada por una compra real en entregas (`CU130CompraTest` crea la oferta y la reserva).

## 3. garantía → nucleo-financiero (asientos por eventos)

El consumidor Kafka queda ENCENDIDO en el compose de TEST con un mapeo SINTÉTICO (`CU24SemillasTest`, `GuardiaDeCuentasLogicasTest`). No hay prueba
con broker real (Kafka) ni con el productor real `garantia`: solo `CU24EventosTest` con el evento tal como lo arma `AsientoPropuesto` de garantia.
