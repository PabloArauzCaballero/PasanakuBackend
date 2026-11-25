# Traspaso — gobierno de usuarios y traspaso general de Pasanaku

> **Para quien lo retome (otra cuenta u otra sesión):** leé este archivo entero, después `INVENTARIO.md` (misma carpeta) y `docs/trabajo/2026-11-23-continuar-traspaso/{PLAN,REPORTE}.md`. Cada lote tiene su `PLAN.md`/`REPORTE.md` (el avance en la primera línea) en su rama; son su memoria. Lo que no está escrito acá o en esos archivos no se hizo. Actualizá la tabla «Estado» y el «Registro» cuando cambie algo y commiteá en `claude/modest-newton-4ckcid`.

- Repos: `PabloArauzCaballero/PasanakuBackend` (monorepo: servicios Java, backoffice Angular, app Flutter, bóveda de CU) y `PasanakuFrontend` (solo dependabot).
- **Autorización vigente de Pablo (2026-11-24 23:25 UTC):** mergear a `dev` lo que haga falta y promover `dev → test` por PR. **`main` sigue prohibido** sin autorización explícita (los PRs a `main` se cierran sin mergear). Sin secretos ni datos personales en logs, reportes ni capturas.
- Última actualización: 2026-11-25 ~15:05 UTC. Peldaño global: `TESTED` (CI), nada `VERIFIED` (sin Kafka real, sin despliegue observado).
- Ramas: `dev` (head `90774d90`, incluye #514, #515, #516, #517, #518), `test` (head `f957167b`, promoción #513; **no incluye** lo posterior al 24-11 ~19:00).

## Estado (2026-11-25 ~15:05 UTC)

| Lote | Sesión (`session_…`) | Rama | PR | Estado |
|---|---|---|---|---|
| Traspaso general H1–H7 | principal `01ATKb2QAkFb98LJ18k7D6w4` | `claude/modest-newton-4ckcid` | — | 34/40. En `test`. Falta `estado` de TEST (Pablo). |
| A — segregación de funciones | `…NdFtpsf82ZU3SGQYf` | `claude/gu-a-segregacion` | #515 | **HECHO, en dev** `2c963e46`. |
| E — revocación, inactividad, auditoría | `…PBBXchB5voCrHukT6` | `claude/gu-e-revocacion` | #517 | **HECHO, en dev** `5eb62e76`. |
| B — CU-08 por asignación | `…QKXrRcFwGMW5Biabx` | `claude/gu-b-cu08` | #516 | **HECHO, en dev** `90774d90` (TEST_BUG de fixtures corregido, `53710c43`). Con B solo, el backoffice responde 422 `AP-CU08-09` en alta/suspender/reactivar/desbloquear hasta que entre D. |
| G — CU-134 participantes | `…PY4e3m7Fhxv5ifoZs` | `claude/gu-g-cu-participantes` | #514 y #518 **en dev**; **#519 abierto** | CU-134 **APROBADO** por Pablo. **Todo subido** (último commit `82d14870f`, dev integrado). #519 = docs + modelo/DDL/semillas (5 tablas en identidad, `politica_medida_cuenta`, R-SEG-13..15, permisos `PARTICIPANTES_*`). Los 3 rojos de CI corregidos en esa rama (spotless en los átomos; la prueba de humo esperaba 53 tablas selladas y ahora son 55): falta que cierre la CI. **Falta H3.S3–S7**: repositorios, organismos, controlador y contrato `[Participantes]` (hoy `evidencia/H3.S2-contrato-participantes.patch`), pruebas CU134, retención de la entrega en `servicios/entregas`, modo restringido y backoffice. Paso a paso en `docs/trabajo/2026-11-24-gu-g/REPORTE.md`. ~14/27. Build local bloqueado por el 429 de Maven Central. |
| D — backoffice «Personal y accesos» | `…NFv1aTGuXmnRXK9eP` | `claude/gu-d-backoffice` | **#524 (draft)** | ~7/20. **Todo subido** (último commit `230f2dbed`, dev mergeado limpio). Falta: H3/H4 specs de usuarios-internos en rojo (URL `?limite=50`, permisos en sesión), H5 informe de accesos + revocar, roles-internos, simulado/E2E/capturas/gates. Detalle y próximo paso en `docs/trabajo/2026-11-25-gu-d/REPORTE.md`. |
| C — credencial/invitación/recuperación | `…NNuSvsGjpcaPm4hVu` | `claude/gu-c-credencial` (+ `claude/gu-c-credencial-ui`) | #522 | ~0/23 según plan. **Todo subido**: backend en `e92ec3a80` (#522); UI en la rama `claude/gu-c-credencial-ui` `b1ee5e6ea` (**WIP sin verificar**, un subagente se cortó por el límite). Corregidos gitleaks, catálogo de datos y 2 de 7 archivos de spotless. Falta: formato de 5 archivos (`RecuperacionesDeAcceso`, `PlantillaCorreoDeInvitacion`, `RecuperacionesRepositorio`, `TokensDeCuentaRepositorio`, `CU09CuentaDeOperadorTest`: leer el diff de `spotlessJavaCheck` en la próxima CI), compilación y tests (nunca corrieron), enganche del hook de alta y step-up con #516 (ya en dev), UI sin capturas. Detalle en `docs/trabajo/2026-11-25-gu-c/REPORTE.md`. |
| F — roles propios + cuentas de servicio | `…KrP6yE7LdUMcadjtq` | `claude/gu-f-roles-servicio` | #520 (draft) | 0/13 según plan. **Todo subido** (último commit `ff37562c`, dev mergeado). Corregidos spotless (`f9e5c579`) y bóveda/CU-136. Falta: resultado de la CI de esa corrida (Java sin compilar aún por 429), 5 entidades huérfanas ajenas en `verificar_boveda`, y todo el backoffice (requiere un componente de step-up, que D construye) + capturas. Detalle en `docs/trabajo/2026-11-25-gu-f/REPORTE.md`. |
| H — pendientes de la app | `…NR2voBqug9DCL1pZ2` | `claude/gu-h-pendientes-app` | #521 | 4/7. **Todo subido** (último commit `7de86687`). El rojo era lint móvil (`textos.dart` 202 líneas, límite 200), corregido en `4c3b0b48`; falta que cierre la CI de ese commit. Falta: `assembleRelease` sin secretos (solo CI/SDK) y matriz de capturas de grupos/detalle (clientes Dart no generables en la nube por el 429). |

Las sesiones hijas se cortaron por el **límite de uso de 5 h de la cuenta** (~00:00 UTC del 25-11) y se reanudaron con mensaje. Si ves una en `status_bucket: failed`, mirá `list_events kinds=["result"]`: `usage_limit_reached` = esperar el reseteo y mandarle `send_message` «retomá desde tu PLAN/REPORTE».

## Decisiones de Pablo vigentes (no volver a preguntar)

Ver `INVENTARIO.md` («Decisiones de Pablo»). Además:
- **B (2026-11-24):** suspender revoca todos los roles y reactivar vuelve vacío; baja laboral con estado nuevo `BAJA_LABORAL`; el `jti` del step-up consumido va a tabla con UNIQUE; entra a dev con CI verde; **no promover a test hasta que D esté en dev**. Códigos CU-08: A usó 08 y 12; B 09–11.
- **«Aprobado todo» (2026-11-24 ~23:30):** CU-134 con la recomendación de cada D-1…D-15 y el número CU-134; **D-3** plazos = descargo 5 días hábiles, apelación 5 días hábiles, prescripción 12 meses, suspensión máxima 90 días (propuesta aprobada por Pablo, **sin fuente normativa**, declarada así; viven en la tabla propia `politica_medida_cuenta` de identidad, desvío por la regla 98.1, editable sin código); **D-11** = la entrega del participante con medida vigente se **retiene** en custodia hasta resolver (si la medida queda firme, el destino de lo retenido **no está decidido**: ambigüedad A-1 a confirmar con Pablo, no inventar); las suposiciones A1–A6 del lote E tal como están en su PLAN; lote H: RESPALDO-003 (lámina 4 desde CU-23/CU-22, sin cifras), APROB-001 (se conservan AP-CU20-02/AP-CU69-10: el servidor aún los emite), grupos/detalle (la ruta ya existía), CONFIG-001 (firma de release por `APORTAYA_*`, falla sin ellas).

## Qué hacer ahora, en orden

1. **Arreglar los rojos de CI** de #519, #520, #521, #522 (cada sesión debería haberlo hecho; verificá). Los tres PRs de Java comparten «Formato, reglas propias y compilación» = `./gradlew spotlessCheck` (formato palantir; el formatter se puede correr localmente con el jar de palantir-java-format con `--palantir --replace`) y «bóveda/esquema no divergen» (regenerar con `generar_ddl.py`/`catalogo_de_datos.py --escribir` y `verificar_boveda.py`). Si hay 429 de Maven Central (`org.jacoco.ant`, `Could not resolve`), es ENVIRONMENT: esperar a que termine la corrida y `gh api -X POST …/actions/runs/<run>/rerun-failed-jobs`. **Nunca relanzar sobre una rama con corrida en curso** (`ci.yml` cancela la anterior por `concurrency`). Gradle local suele dar 429: Java se verifica en CI; para lo puro usar `javac -Xlint:all -Werror`.
2. (Un vigilante en segundo plano de la sesión principal mergea #519–#523 solos con CI verde; si esa sesión ya no existe, hacelo a mano.) **Mergear a dev** cada PR con CI verde (`gh api -X PUT repos/PabloArauzCaballero/PasanakuBackend/pulls/<n>/merge -f merge_method=merge -f sha=<head>`). Orden sugerido: #521 (H), #519 (G, modelo), #522 (C), #520 (F), PR de D. `identidad.yaml` y `catalogo` los comparten C, F, D, G: tras cada merge, los demás hacen merge de `origin/dev` y regeneran postman/inventario (`verificar_boveda` exige códigos de error correlativos por CU).
3. **Implementar CU-134 completo (lote G):** controladores y casos de uso en `servicios/identidad` (directorio y ficha enmascarada con `registro_acceso_datos`, suspensión/bloqueo con expediente, descargo, decisión por otra persona, firmeza, apelación por una tercera, prescripción, reversión, notificación), contrato `[Participantes]` en `identidad.yaml`, retención de la entrega en `servicios/entregas` (contrato entre servicios: outbox, idempotencia, prueba de duplicado y de caída del dependido; regla 91.6), y backoffice «Participantes» (coordinar el menú con D). Ver `docs/CasosDeUso/CU-134 …md` y `docs/trabajo/2026-11-24-gu-g/{PLAN,REPORTE}.md`.
4. **Promover `dev → test`** por PR (CI verde) **solo cuando D esté en dev**. Antes de promover, en el despliegue de TEST crear el tema Kafka `aportaya.identidad.revocacion` con `despliegue/host/crear-topicos.sh` (sin él los servicios rechazan con 401 los tokens con `sesion_id` hasta cargar la lista de revocaciones).
5. Actualizar `INVENTARIO.md`, los PLAN/REPORTE y el HTML de avance (artefacto «Avance Pasanaku», https://claude.ai/artifact/SKh1SnUvcNRzCcacHcmVFE; fuente de las cifras: `python .claude/hooks/plan_status.py --path <PLAN.md>` por lote).

## Pendiente que solo Pablo puede hacer

1. Correr `estado` en TEST (el SSH está bloqueado desde la nube).
2. **Crear la keystore de release de Android y sus secretos de Actions**; pasos exactos en `docs/operacion/firma-android-release.md` (lote H, #521).
3. Crear el tema Kafka al desplegar (punto 4 de arriba).
4. Prueba en dispositivo real (push FCM, rostros reales, goldens de macOS).
5. Confirmar A-1: destino de la entrega retenida si la medida de CU-134 queda firme.

## Notas operativas

- Máx. 4 sesiones hijas simultáneas (CLAUDE.md, regla 70); lotes con archivos disjuntos. Pablo pidió gastar tokens solo en lo necesario: no leer archivos enteros, una corrida dirigida por cambio, sin polling de CI (esperar el aviso del PR), capturas solo de lo que cambia, mensajes cortos.
- Capturas de UI: 390/834/1440 × claro/oscuro, `animations: 'disabled'`, sin `fullPage` en overlays, mirándolas de verdad.
- Ninguna rutina programada propia activa (solo una de un lote ya cerrado). Si querés una red de seguridad, creá una tarea horaria con `create_trigger` (`create_new_session_on_fire`) cuyo prompt sea: «leé `docs/trabajo/2026-11-24-gobierno-de-usuarios/TRASPASO.md` y seguí la sección “Qué hacer ahora”».

## Registro

| Fecha UTC | Qué pasó |
|---|---|
| 2026-11-24 19:30 | #515 (A) y #517 (E) en dev. |
| 2026-11-24 23:24–23:32 | Lanzados D, C, F, H; #514 en dev; Pablo aprobó todo lo bloqueado. |
| 2026-11-24 23:50 | #516 (B) con fix de fixtures (`53710c43`); #518 en dev. |
| 2026-11-25 ~00:00 | Todas las sesiones cortadas por límite de uso (~15 h sin avance). |
| 2026-11-25 ~00:21 | #516 mergeado a dev (`90774d90`). |
| 2026-11-25 ~14:50 | Reanudación; #519/#520/#521/#522 con CI en rojo (ver tabla). Pablo pide traspaso: cada sesión sube todo lo local y deja PLAN/REPORTE al día. |
| 2026-11-25 ~14:55 | B (#523 docs, 20/20), H y D confirmaron todo subido. Esperando C, F, G. |
| 2026-11-25 ~14:55 | C y F confirmaron todo subido. Falta G. |
| 2026-11-25 ~14:57 | G confirmó todo subido. Las seis sesiones confirmaron: nada local sin subir. |
| 2026-11-25 ~16:05 | #519 (G, modelo) y #521 (H) mergeados a dev (`282a1522`). #523 (B, docs) mergeado. G abrió #525 (draft; backend CU-134) con spotless en rojo: el parche de formato Palantir está en `docs/trabajo/2026-11-24-gobierno-de-usuarios/parches/gu-g-spotless-3aa9d4627.patch` (se aplica con `git apply` sobre `claude/gu-g-cu-participantes`). Los vigilantes de auto-merge de la sesión principal terminaron por límite de tiempo: **hacer los merges a mano** con CI verde (#520 F, #522 C, #524 D, #525 G). Apareció #526 (`codex/gu-revocacion-lista-kafka`): no es de ninguna sesión de este trabajo, **no tocarlo sin confirmar su origen con Pablo**. |
| 2026-11-25 ~16:30 | #526 (Codex, endurece la lista de revocaciones: tema ausente = lista no disponible, sin auto-crear tema, `despliegue/TEST.md` con la comprobación previa) mergeado a dev (`0dfe408e`) por orden expresa de Pablo; su único rojo era la subida del SARIF de Trivy a Code Scanning (permisos del entorno). Pendiente de Pablo: comprobar el tema Kafka y las réplicas en TEST. |
