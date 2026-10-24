# Pendientes heredados — inventario consolidado al 2026-10-24 (dev 46d285dd)

Este inventario lo armó un agente de solo lectura. Revisó a mano las 223 filas que no estaban en HECHO en `docs/trabajo/*/PLAN.md` y `REPORTE.md`, además de `PENDIENTES.md`, `PROGRESO.md` y `planes/informes/`. Cada fila se cruzó con el código, `git log`, los PRs de GitHub y el CI. Queda fuera el plan `2026-10-24-backoffice-correccion-y-atlas/`, porque lo lleva su propia sesión.

> `plan_status.py` da 888/1072, pero subcuenta: ignora los TODO al listar y no reconoce los IDs `H1.M1` de 9 planes. Por eso **no** sirve como fuente única de este inventario.

**Total vivo: unos 87 ítems.** Clase a: 16 · b: 24 · c: 16 · d: 25 · e: 6. Hay 30 que tocan dinero (💰).

## Orden de ejecución para la sesión de Pasanaku

1. **Higiene (primero, es barata).** Marcar en sus planes de origen como `HECHO` o `DESCARTADO` lo que ya resolvió trabajo posterior (sección «Ya resueltos»), citando la evidencia. Crear los `REPORTE.md` que faltan (`2026-10-15-backoffice-financiero`, `2026-10-18-backoffice-como-atlas`). Llevar los 9 planes con IDs `H1.M1` al formato `H<n>.S<n>.M<n>`. Archivar o actualizar `PENDIENTES.md` y `PROGRESO.md`. Con eso los candados `report_gate.py` y `blocker_gate.py` dejan de protestar.
2. **El plan de recarga** ([PLAN.md](./PLAN.md)).
3. **Clase (a):** se puede hacer ya en código.
4. **Clase (b):** se cierra contra un doble según la regla 65. Varios ítems de dinero se apoyan en el banco simulado del paso 2.
5. **Clases (c), (d) y (e):** no se pueden cerrar desde código. Se dejan como `BLOQUEADO` o `DECISION_REQUIRED`, con el dueño y qué los destraba. Esto también es «no dejar nada pendiente sin estado».

## Ya resueltos por trabajo posterior (descartar con esta evidencia)

- **PRs, CI y despliegues:**
  - `resolver-conflictos-60-63` y `arreglos-ci`: su REPORTE dice «Pendiente: ninguna».
  - `ci-formato-test` H1.S2.M3: merges 624571ff y 7fc65304.
  - `pendientes-carril-a` H3.S1.M2 y H4.S1.M8.
  - `estado-real`: #124 y #125.
  - `unirse-qr` H1.M4, H2.M6, H3.M3 y H3.M5: #157.
  - `enlace-qr` H6.M1: #152 y #153.
  - `catalogo-de-datos` H7.S2.M2: #170 y #174.
  - `inicio-tus-pasanakus`: #179 y #180.
  - `pulido`: #183 y #184.
  - `ci-verde` H1.S3.M2 y `perfil` H1.S6.M1: CI verde y 5b605dbf.
  - `app-arreglos` H10.M2 y Postman: 0ccacd83.
- **`kyc-diferido`:**
  - H1.M2: `CU01AltaSinDocumentoTest`.
  - H1.M5: `CU04CuentaSinDocumentoTest`.
  - H1.M8: #126 desplegado en TEST.
- **`alta-fecha`:**
  - H4.S1.M2: TEST está arriba.
  - H5.S2.M3, H7.S1.M1 y H7.S1.M2: `apk-de-prueba.yml` ya está en el repo. Por el mismo motivo quedan obsoletos los APK por versión: v8, v11, v12 y el de detalle-firma.
- **Campaña CU, defectos B1–B32 corregidos en código:**
  - `LibroDeBilletera.java:102` escribe como sistema.
  - `BilleteraController.java:146` exige `TESORERIA`.
  - `HechosDeGruposPorHttp.java:44`.
  - `TipoDeAcuerdo.java:36`.
  - `GruposController.java:286` es `@Publico`.
  - `SOLICITUD_INGRESO_VER` está sembrado.
  - B28 (H6.S10.M3) y B32 (H9.S2.M1) figuran HECHO.
  - Los bloqueos por memoria (H3.S15, H4.S1.M3, H5.S7, H5.S8, H6.S9) quedaron superados: `integrationTest` está en verde.
- **Endurecimiento H17.S9:**
  - M1: `InvitacionesWeb.java:136`.
  - M2: `RutasDeAdmision.java:59`.
  - M3: `verificar_carriles` OK.
  - M4 y M5: los goldens pasaron a Linux (D-A4, `ci.yml:422`).
  - M7: `PROGRESO.md:126`.
  - H17.S7.M3 y H3.S1.M4: cubiertos por el autodespliegue a TEST.
- **`alta-atlas` H10.S1.M6 y M7:** la referencia canónica es Linux (D-A4).
- **`carril-a` A4:** `nucleo-financiero.yaml:743-796`.
- **Catálogo de metadata:** HECHO en `como-atlas` H3.S1.M1.
- **Guía PR50:** `verificar_criterios.py` informa «Sin divergencias».
- **Informes de carril:**
  - El ensayo de restauración está hecho (`docs/operacion/ensayo-restauracion-y-reanudacion.md`).
  - Existe `carga/k6`.
  - `mobile_scanner`, el cliente de tarifas y patrol están en el pubspec.
  - ADR-038 está escrito.
  - Hay sitemap.

## (a) Se puede hacer ya en código — 16

| ID | Plan de origen | Qué falta | Evidencia de que sigue vivo |
|---|---|---|---|
| H1.S2.M3 💰 | `2026-10-16-tableros-por-rol` | CU-14 no escribe el contra-asiento en el mayor | `CU14ReversarTransaccion.java` no usa `AsientoRepositorio` |
| H12.S1.M2 💰 | `2026-10-08-endurecimiento-integral` | Separar el saldo gastable del retenido, invertido o de grupo en los clientes | A MEDIAS; solo hay textos |
| H4.S1.M6 💰 | ídem | Prueba del cliente: QR interno vs interoperable | Solo el backend tiene pruebas (`ClasificadorDeQr`) |
| H2.S1.M1 💰 | `2026-10-11-alta-atlas-sesion-premium` | Backfill auditado del rol PARTICIPANTE para las cuentas aprobadas antes del arreglo (hoy reciben 403 en la billetera) | `PROGRESO.md:350` |
| H1.M3 | `2026-10-17-kyc-diferido` | Integración `CU02IniciarVerificacionTest` (duplicado y segunda vez dan 422) | Solo existe el webTest |
| H1.M7 | ídem | Prueba de widget del aviso «Verificá tu identidad» | REPORTE, sección A medias |
| — | `2026-10-19-app-arreglos-del-telefono` | El servidor acepta contraseñas con espacios | `PoliticaDeClave.java` |
| H8.S4.M4, H10.S3.M1 | alta-atlas | Captura oscura y matriz visual del detalle de grupo | La ruta ya existe (`rutas.dart:82`) |
| H1.S1.M4 | `2026-09-30-portar-comportamiento-atlas` | Capturas de la guía superpuesta y del botón en Perfil | A MEDIAS |
| — | `2026-10-20-detalle-firma-y-sorteo` | El canje por número no guarda el nombre (muestra «Integrante») | REPORTE, No cubierto |
| H6.S6.M1 | `2026-10-21-catalogo-de-datos-y-rediseno` | Quitar el GET completo deprecado | DESCARTADO solo mientras se desplegaba |
| — | `planes/informes/carril-B5.md` | `SOLICITANTE_ACTUAL` está fijo (`pantalla-despliegues.ts:95`). Coordinarlo con el plan de backoffice del 24/10 | — |
| H17.S9.M6 | endurecimiento | `ap-dialogo` desborda a 360 px | Sin evidencia de arreglo |
| — | `planes/informes/carril-T.md` | `verificar_criterios` marca CU-05, CU-51 y CU-57 como pendientes cuando no lo están | Salida del script |
| H1.S6.M2 | `2026-10-21-perfil-del-integrante` | Lanzar `apk-de-prueba.yml` | La condición ya se cumplió |

## (b) Se cierra contra un doble (regla 65) — 24

| ID | Plan de origen | Qué falta | Evidencia |
|---|---|---|---|
| H3.S5.M1, H3.S14.M1 💰 | `2026-10-03-campania-cu-principales` | CU-10: la doble acreditación da un solo abono y el mayor cuadra; idempotencia de CU-10, 12, 21 y 22. **Se cubre con el plan de recarga (H3, H4.S3.M3, H8.M2)** | Sin corrida positiva desde el 04/10 |
| H3.S6.M1, H6.S4.M1 💰 | ídem | CU-12, transferencia por alias | Última evidencia: 404 |
| H3.S13.M1, H5.S4.M2 💰 | ídem | CU-11, retiro con MFA, aprobación de tesorería y conciliación | Solo se llegó a `AP-CU11-02` |
| H3.S12.M1 💰 | ídem | CU-22, idempotencia y asiento de la entrega | A MEDIAS |
| H5.S6.M1, H6.S8.M1 | ídem | Volver a correr `campania_todo.py` completo | A MEDIAS |
| H14.S1.M1, M4, M5 💰 | endurecimiento | Ciclo feliz F01–F21 con saldos conciliados, regresión y paquete de entrega | TODO |
| H4.S1.M8, H4.S2.M3, H5.S2.M2, H6.S1.M2, H6.S1.M3, H6.S2.M2, H6.S2.M3, H6.S3.M4 | tableros-por-rol | E2E contra la pila real: ya están simulados; cerrarlos como SIMULACION-65 o correrlos en una pila propia | BLOQUEADO por «pila compartida» |
| H10.S2.M2–M5 | alta-atlas | Recorridos en AVD: alta, sesión de 20 min, candado y cambio de cuenta (no hace falta teléfono) | BLOQUEADO «falta backend o cuenta» |

## (c) Necesita hardware o personas — 16 → `BLOQUEADO` con el dueño

- **iPhone, TestFlight y Mac:**
  - Instalar y abrir en el iPhone: H1.S3.M1, H1.S6.M3 y H1.S8.M4 (`2026-09-28-testflight-ios`).
  - Subir el build a TestFlight: H2.S1.M2 (`menu-plegable`; la subida la corre Pablo).
  - Correr `testflight_ios.sh`: H4.S1.M3 (`resolver-conflictos`).
  - TestFlight sin secretos de firma en el repo: H7.S2.M1 (`alta-fecha`).
- **Teléfono Android real:**
  - PIN/huella: H5.M2 (`app-arreglos`) y H2.M3 (`kyc-diferido`).
  - Compartir por WhatsApp o SMS: H4.M2 💰 (`app-aprobacion-recarga-invitar`).
  - Unirse por QR con dos teléfonos: H6.M3 (`enlace-qr`) y H2.M3 (`unirse-qr`).
  - Ruleta del sorteo: H2.M4 (`detalle-firma`).
  - Autocaptura del escáner: H5.S2.M6 (`alta-fecha`).
- **Rostros reales:** H6.S2.M3, 10 altas para calibrar `JuezDePose`.
- **Android de gama media y lector de pantalla real:** H6.S3.M8 y endurecimiento H12.S1.M4.

## (d) Decisión de negocio — 25 → `DECISION_REQUIRED` con a quién

- **Push** (Justin o Pablo): proveedor FCM/APNs, contrato y SDK. Portar-atlas H4.S2.M1, H4.S2.M2 y H4.S3.M1, y alta-atlas H10.S2.M1.
- **Voz de bienvenida (TTS):** portar-atlas H5.S1.M1.
- **Backoffice carril A:**
  - H7.S4.M1, D-15: el backoffice llama a `GET /grupos/solicitudes-escaladas`, que no existe.
  - H7.S5.M1, `/extraccion/accesos`.
  - H9.S1.M4, `GOBIERNO_SESIONAR`: no está sembrado.
  - Bloques A1, A2, A3 y A5.
- **Tipos de usuario «que habíamos dicho»** (Pablo): `2026-10-17-acceso-por-correo-y-backoffice` H5.S1.M1.
- **Select de soporte:** alta-atlas H3.S3.M3.
- 💰 **ADR de contradicciones** de garantía y comisiones: endurecimiento H1.S1.M3, en estado propuesto.
- 💰 **Habilitación real:** H15.S1.M1–M5 (jurídico, pasarela y custodia, capital, fiscal, certificación del adaptador real) y piloto H16.S1.M1–M4.
- 💰 **Apertura del primer período:** cuándo pasa el grupo a EN_CURSO después del sorteo (`detalle-firma`, CU-60 paso 6).

## (e) Acción de infraestructura o de una persona — 6 → `BLOQUEADO` con el dueño

| Qué falta | Origen |
|---|---|
| 💰 Secreto real del webhook de la pasarela | `SecretoWebhookSinConfigurar.java` (aportes) |
| Credenciales SMTP en Coolify | `2026-10-16-codigo-acceso-por-correo` H3.S1.M2 |
| Rotar la clave de la base, y la contraseña del operador de prueba que quedó en el chat | `resolver-conflictos` REPORTE:99 |
| Keystore y firma, cuentas de Play Store y App Store, Shorebird | `planes/informes/carril-F12.md` §9 |
| Servir `assetlinks.json` en TEST y que `adb` lo marque verified | `enlace-qr` H5.M1 |
| Correr en TEST el backfill de PARTICIPANTE | Complemento del ítem de la clase (a) |
