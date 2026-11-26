# Pasarela de pagos: cómo enchufar el adaptador real

Estado hoy (2026-11-26, decisión de Pablo): la pasarela está **simulada**. `aportes` solo conoce *puertos*; el doble
los cumple en `local`/`test`. Este documento dice qué hacer el día que exista un proveedor contratado (P-15) sin tocar
`dominio`, `aplicacion` ni `web`.

## Mapa

| Pieza | Dónde | Quién la cumple hoy |
|---|---|---|
| Pedir: crear orden de cobro / consultar estado | `dominio/PasarelaDePagos` | `infraestructura/pasarela/simulada/PasarelaSimulada` |
| Avisar: traducir el cuerpo del webhook | `dominio/InterpreteDeWebhook` | `.../simulada/InterpreteDeWebhookJson` |
| Avisar: firma y ventana ±5 min | `dominio/VerificadorDeFirmaWebhook` (HMAC-SHA256 sobre `timestamp.cuerpo`) | el mismo, sin cambios |
| Avisar: secreto de verificación | `dominio/ProveedorDeSecretoWebhook` | `infraestructura/SecretoWebhookDeEntorno` (`APORTES_WEBHOOK_SECRETO`) |
| Emitir un aviso firmado de prueba | `dominio/SimuladorDePagoDePasarela` | solo `PasarelaSimulada` (el real NO lo implementa) |
| Dedup + conciliación del aviso | `aplicacion/CU100RecibirWebhookPasarela` | sin cambios (usa los puertos) |

## Configuración

| Propiedad (variable) | Valores | Efecto |
|---|---|---|
| `aportaya.aportes.pasarela.modo` (`APORTES_PASARELA_MODO`) | `simulada` (defecto) · `real` | Elige el adaptador. |
| `aportaya.aportes.pasarela.proveedor-codigo` (`APORTES_PASARELA_PROVEEDOR`) | código de `proveedor_pago` (defecto `QR_INTEROP`, sembrado) | Proveedor para el que firma el doble. |

**Fail-closed (regla 90.3.4):** `modo=simulada` con un perfil distinto de `local`/`test` (staging, production, sin perfil)
aborta el arranque (`GuardiaDePasarelaSimulada`). `modo=real` sin adaptador también lo aborta
(`PasarelaRealNoImplementada`). TEST corre con `PERFIL_SPRING=local`, así que sigue usando el doble.

## Pasos para añadir el adaptador real

1. Crear clases en `infraestructura/pasarela/real/` (nunca en otra carpeta; hay un `package-info`):
   - `PasarelaRealXxx implements PasarelaDePagos` — `@Component` con `@ConditionalOnProperty(name="aportaya.aportes.pasarela.modo", havingValue="real")`.
     Reglas: **timeout explícito**, reintento solo con la clave de idempotencia de la solicitud, circuit breaker con
     fallback declarado (regla 98.2; ver `infraestructura/clientes/ClienteConCircuito`), sin PII ni importes en logs.
   - `InterpreteDeWebhookXxx implements InterpreteDeWebhook` — el único lugar con los nombres de campo del proveedor;
     debe lanzar `PayloadIlegible` ante un formato inesperado y devolver `monto` vacío si no puede leerlo exacto (sin `double`).
   - Si el proveedor firma distinto de HMAC-SHA256 sobre `timestamp.cuerpo`, **eso sí** requiere decisión: hoy la firma
     es Q-02 y está en `VerificadorDeFirmaWebhook`; no se cambia sin registrarla.
   - Secretos del proveedor: resolverlos por `ProveedorDeSecretoWebhook` (vault/`referencia_credenciales`), nunca en el repo.
2. **Borrar** `real/PasarelaRealNoImplementada.java`.
3. No tocar `dominio`, `aplicacion`, `web` ni `infraestructura/pasarela/simulada`: `ArquitecturaTest` falla si lo haces
   (`elNucleoNoConoceLosAdaptadoresDeLaPasarela`, `elAdaptadorRealNoDependeDelDoble`).
4. Activar con `APORTES_PASARELA_MODO=real` y cargar el secreto de verificación en el almacén de secretos.

## Qué verificar contra el proveedor real (nada de esto está verificado hoy)

- Contrato de `crearOrdenDeCobro` / `consultarEstado` contra el sandbox del proveedor (idempotencia por clave: ¿la API del
  proveedor la respeta?).
- Formato real del webhook ↔ `InterpreteDeWebhook`; esquema de firma real ↔ `VerificadorDeFirmaWebhook`.
- Ventana de timestamp con el reloj del proveedor (±5 min, Q-02); reintentos del proveedor (mismo `idEvento` ⇒ `DUPLICADO`).
- Prueba de caída del proveedor (regla 98.8.4) y traza con `correlationId`.
- Los **tres niveles** de las pruebas del doble repetidos contra el sandbox: correcto, límite, inválido.

## Qué quedó verificado solo contra el doble (regla 65)

Todo lo de `PasarelaSimuladaTest`, `InterpreteDeWebhookJsonTest` y `PasarelaSimuladaFlujoTest`: firma válida, mismo evento
dos veces, monto distinto, orden inexistente, firma inválida y timestamp al borde. Prueban CU-100 real contra un emisor
sintético, **no** contra un proveedor.

## Límites conocidos

- `CU21CobrarAporte` todavía no usa `crearOrdenDeCobro`: no existe la cadena `orden_cobro → intento_pago` (ver
  `CU100RecibirWebhookPasarela`). El puerto está listo para cuando exista (decisión de producto).
- No hay endpoint HTTP para disparar el webhook simulado en el servidor TEST; hoy se recorre en pruebas de integración.
- El doble guarda sus órdenes en memoria: un reinicio las olvida.
