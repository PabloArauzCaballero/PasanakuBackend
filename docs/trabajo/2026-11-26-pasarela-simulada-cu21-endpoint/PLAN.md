# Plan — CU21 conectado a la pasarela simulada + endpoint para disparar el aviso de pago (solo TEST)

- Fecha: 2026-11-26 · Repos afectados: PasanakuBackend (`servicios/aportes`, `scripts/` generados) · Predecesor: `docs/trabajo/2026-11-26-pasarela-hexagonal` (#540, fusionado)
- Resultado observable: en `local`/`test`, un pago de cuota con saldo (`CU21PagarConSaldo`) abre una orden simulada y la guarda como
  `pago.referencia_proveedor`; un administrador dispara el aviso firmado de esa orden por HTTP y CU-100 lo verifica y concilia. Fuera de
  `local`/`test` el endpoint no existe (404). Las respuestas y códigos de error de CU21 no cambian.
- Kill-test: (1) `CU21PagoConSaldoTest` existente sigue verde (solo cambia su constructor); (2) con perfil `production` la ruta no está mapeada.

## Decisiones recibidas (relay de Pablo, 2026-11-26)
1. Conectar CU21 a `crearOrdenDeCobro` solo en modo simulada, sin cambiar contrato/flujo (regla 96.4).
2. Endpoint HTTP que dispara el aviso simulado: autenticado, permiso explícito, 404 fuera de local/test, firma real del verificador,
   idempotente por `clave_idempotencia`, auditado, sin PII en logs, matriz de autorización negativa.
3. `QR_INTEROP` como MOCK que se comporte como uno real (estados, errores, ciclo), declarado como doble.

## Descubrimiento (hechos)
- `CU21PagarConSaldo.pagarConSaldo` (`aplicacion/CU21PagarConSaldo.java:43-77`) es el único camino de cobro de la cuota: canal saldo,
  `referenciaProveedor = "SALDO-<obligacion>-<clave>"`, `proveedorId` vacío; `CU21CobrarAporte.acreditar` resuelve el reintento por clave
  ANTES de mirar monto/referencia. No existe la cadena `orden_cobro → intento_pago`.
- CU-100 concilia por `pago.referencia_proveedor` (`PagoRepositorio.porReferenciaProveedor`), sin mirar `proveedor_id`.
- `QR_INTEROP` está sembrado con `activo = FALSE`, `soporta_webhook = TRUE` (`sql/60_semillas/14-proveedores-externos.sql:6`);
  `ProveedorPagoRepositorio.activoConWebhook` exige `activo` ⇒ en TEST el aviso fallaría la verificación hasta activarlo.
- Rutas: `/qr` es prefijo reservado de aportes (`ArranqueTest`) y no tiene operaciones ⇒ una API generada propia (`QrApi`) permite un
  controlador que solo existe por perfil. Precedente de permiso: `@Permiso("ADMIN_PLATAFORMA")` en `darDeAltaProveedor`.
- Auditoría: `AuditoriaRepositorio.registrar` (`comun.bitacora_evento`). Registro de operaciones: `scripts/generar_catalogo_operaciones.py`.

## Alcance
- IN: capacidad `registraOrdenPorCobro()` en el puerto; CU21PagarConSaldo; caso de uso CU-100; controlador `QrApi` por perfil; OpenAPI;
  catálogos generados; pruebas 3 niveles; documentación.
- OUT: activar `QR_INTEROP` (decisión de negocio, regla 97.1.5/97.4); permiso nuevo en la bóveda de roles; adaptador real; cadena
  `orden_cobro`; `nucleo-financiero`.
- Ambigüedades: (a) `QR_INTEROP` inactivo en el seed: el endpoint responde 422 explicativo; activar es decisión de Pablo/Cumplimiento.
  (b) Permiso: se reusa `ADMIN_PLATAFORMA` (precedente en el servicio) en vez de sembrar uno nuevo; confirmar con Pablo.
  (c) «Mismos estados que uno real»: no hay proveedor real que copiar; el doble conserva PENDIENTE/PAGADO. (d) «aviso→acreditación»: CU-100
  concilia el pago ya registrado; no acredita saldo.

## H1 — CU21 conectado a la orden simulada
**CA:** Dado `modo=simulada`, cuando una persona paga su cuota con saldo, entonces la respuesta y los errores son los de hoy y el pago queda
con la referencia de una orden simulada consultable; el reintento con la misma clave devuelve el mismo pago; con otro modo, nada cambia.
**DoD:** `CU21PagoConSaldoTest` sin cambios en verde + `CU21PagoConOrdenSimuladaTest` (3 niveles) en verde. **Estado:** A MEDIAS

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | `PasarelaDePagos.registraOrdenPorCobro()` (defecto false; el doble true) | el doble lo declara | `PasarelaSimuladaTest` | HECHO |
| H1.M2 | `CU21PagarConSaldo` pide la orden en el primer intento y cae a la referencia vieja si no puede | mismas respuestas/errores | `CU21PagoConSaldoTest` + `CU21PagoConOrdenSimuladaTest` | HECHO |

## H2 — Endpoint que dispara el aviso simulado
**CA:** Dado un administrador autenticado en `local`/`test`, cuando pide el aviso de una orden simulada, entonces CU-100 lo verifica y devuelve
PROCESADO (o DUPLICADO la segunda vez); sin token 401, otro rol 403, ruta inexistente fuera de `local`/`test`, orden desconocida 422.
**DoD:** tests de caso de uso con base + `AvisoSimuladoWebTest` (matriz negativa) + `PasarelaSimuladaPerfilTest` + CI de bóveda en verde. **Estado:** A MEDIAS

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | OpenAPI `POST /qr/simulador/avisos-de-pago` + esquemas estrictos | contrato con 200/401/403/422 | generación OpenAPI compila | HECHO |
| H2.M2 | `CU100EmitirAvisoDePagoSimulado` (emite, entrega por CU-100, audita) | PROCESADO/DUPLICADO/422, auditoría escrita | `CU100AvisoSimuladoTest` | HECHO |
| H2.M3 | `PasarelaSimuladaController` `@Profile`+propiedad, `@Permiso` | solo existe en local/test | `AvisoSimuladoWebTest`, `PasarelaSimuladaPerfilTest` | HECHO |
| H2.M4 | Regenerar catálogo de operaciones / postman / backoffice | CI de bóveda en verde | script + `git diff --exit-code` | HECHO |

## H3 — Documentación y cierre
| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.M1 | Actualizar `docs/operacion/pasarela-real-como-enchufar.md` | refleja CU21, endpoint y límites | lectura | HECHO |
| H3.M2 | spotless, un push, PR contra `dev`, CI verde, REPORTE, aviso a `@parent` | PR fuera de borrador y verde | checks del PR | A MEDIAS |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambiar la referencia del pago en TEST | datos de TEST distintos | solo con `modo=simulada`; respuesta idéntica; documentado |
| Orden en memoria huérfana si la transacción revierte | ninguno funcional | declarado; el doble es en proceso |
| `QR_INTEROP` inactivo | aviso 422 en TEST | mensaje explícito; decisión de negocio registrada |
