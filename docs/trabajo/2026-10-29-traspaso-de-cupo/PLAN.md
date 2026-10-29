# Plan — Traspasar el cupo (CU-64) en la app móvil

- Fecha: 2026-10-29 · Repos afectados: PasanakuBackend (`apps/movil`) · Predecesor: ninguno
- Resultado observable: quien ocupa un cupo abre «Traspasar mi cupo» desde la pantalla del grupo,
  elige a otro integrante, escribe el motivo, revisa qué se entrega y qué NO se mueve, confirma en
  una hoja y ve el comprobante con el número del traspaso. Lenguaje visual de la orden P2P de
  Binance (tarjeta «Entregás / Recibe», contraparte, condiciones, pasos), **sin precio ni pago**.
- Kill-test: un toque doble en «Confirmar» manda dos POST, o un 422 AP-CU64-0X se muestra como
  un error genérico lejos del paso que lo causa.

## Alcance
- IN: `apps/movil/lib/pantallas/pasanaku/` (archivos nuevos del traspaso, una ruta en
  `rutas_operacion.dart`, el acceso en la vista del grupo), sus pruebas de widget y el inventario
  de capturas (flujo `grupos`).
- OUT: `packages/diseno_flutter` (no se edita), `pantalla_sorteo*.dart`, `ruleta_del_sorteo.dart`,
  el servidor (`servicios/grupos`), los clientes generados.
- Ambigüedades registradas:
  - A1 · El controlador exige `GRUPO_ADMINISTRAR` (rol ORGANIZADOR global) y revisa la deuda de
    QUIEN LLAMA, no del titular del cupo. Supuesto: la app ofrece el traspaso a quien ocupa el cupo
    y el 403 se explica en pantalla. Confirmar con producto/backend.
  - A2 · `hayAcuerdoSiSeExige = acuerdoId != null`: sin acuerdo, el servidor responde siempre
    AP-CU64-05. No hay endpoint para listar acuerdos aprobados. Supuesto: la ruta acepta
    `?acuerdo=<id>` opcional y la pantalla declara el hueco. Confirmar con producto.
  - A3 · Quien entra tiene que ser participante ACTIVO del grupo (`participanteDeOtro`). Se elige
    entre los integrantes de `consultarDetalleDelGrupo` con `usuarioId` y que no soy yo.

## H1 — Traspasar el cupo desde la app
**CA:** Dado que ocupo un cupo, cuando elijo a otro integrante, escribo el motivo y confirmo,
entonces se manda UN `POST /grupos/{g}/cupos/{c}/traspasos` con `Idempotency-Key` y veo el
comprobante; ante un 422 AP-CU64-0X veo el mensaje en el paso que lo causó y conservo lo cargado.
**DoD:** `dart analyze --fatal-infos lib test` · `python3 scripts/verificar_frontend.py movil` ·
suite pedida en verde · capturas 360/412 claro/oscuro miradas.
**Estado:** HECHO (ver REPORTE.md; sin prueba contra servidor real)

### H1.S1 — Dominio, pantalla, acceso y pruebas
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Notifier CU-64 con clave por intento | un envío por intento, misma clave en reintento de red, clave nueva tras 422 | test widget doble toque | HECHO |
| H1.S1.M2 | Mapa AP-CU64-01..05 → paso + mensaje | cada código aparece en su paso | test por código | HECHO |
| H1.S1.M3 | Pantalla de 3 pasos + hoja + comprobante | confirmación obligatoria antes del POST | test «sin confirmar no envía» | HECHO |
| H1.S1.M4 | Ruta y acceso solo para el titular del cupo | el botón aparece solo en mi cupo ocupado | test de la vista del grupo | HECHO |
| H1.S1.M5 | Inventario de capturas + mirar capturas | capturas 360/412 claro/oscuro | `flutter test test/capturas/rediseno/capturas_test.dart ...` | HECHO |
| H1.S1.M6 | Gates | analyze, verificar_frontend y suite en verde | comandos de la DoD de H1 | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| A2: el servidor siempre pide acuerdo | el flujo real termina en AP-CU64-05 | se dice en pantalla y en el reporte |
| Ejemplos del simulado con un solo integrante | la captura del harness muestra el estado vacío | captura adicional sintética fuera del repo |
