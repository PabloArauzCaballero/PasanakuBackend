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
| G — CU-134 participantes | `…PY4e3m7Fhxv5ifoZs` | `claude/gu-g-cu-participantes` | #514 y #518 **en dev**; **#519 abierto** | CU-134 **APROBADO** por Pablo. #519 = docs + modelo/DDL/semillas (5 tablas en identidad, `politica_medida_cuenta`, R-SEG-13..15, permisos `PARTICIPANTES_*`). **CI de #519: 3 rojos** (spotlessCheck, base efímera/semillas, esquema+humo). Falta TODO el código (controladores, casos de uso, entregas, backoffice); el contrato `[Participantes]` está como `evidencia/H3.S2-contrato-participantes.patch`. ~14/27. |
| D — backoffice «Personal y accesos» | `…NFv1aTGuXmnRXK9eP` | `claude/gu-d-backoffice` | (confirmar) | ~7/20. Contra el contrato de B, ahora ya en dev. |
| C — credencial/invitación/recuperación | `…NNuSvsGjpcaPm4hVu` | `claude/gu-c-credencial` | #522 | ~0/23 según plan. **CI: 3 rojos** (spotless, bóveda/esquema, dependencias). |
| F — roles propios + cuentas de servicio | `…KrP6yE7LdUMcadjtq` | `claude/gu-f-roles-servicio` | #520 (draft) | 0/13 según plan. **CI: 3 rojos** (bóveda/esquema, spotless, bóveda/seguridad/carriles). |
| H — pendientes de la app | `…NR2voBqug9DCL1pZ2` | `claude/gu-h-pendientes-app` | #521 | 4/7. **CI: 1 rojo** («Frontend — Flutter y Angular (ADR-044)», job 138865754811). |

Las sesiones hijas se cortaron por el **límite de uso de 5 h de la cuenta** (~00:00 UTC del 25-11) y se reanudaron con mensaje. Si ves una en `status_bucket: failed`, mirá `list_events kinds=["result"]`: `usage_limit_reached` = esperar el reseteo y mandarle `send_message` «retomá desde tu PLAN/REPORTE».

## Decisiones de Pablo vigentes (no volver a preguntar)

Ver `INVENTARIO.md` («Decisiones de Pablo»). Además:
- **B (2026-11-24):** suspender revoca todos los roles y reactivar vuelve vacío; baja laboral con estado nuevo `BAJA_LABORAL`; el `jti` del step-up consumido va a tabla con UNIQUE; entra a dev con CI verde; **no promover a test hasta que D esté en dev**. Códigos CU-08: A usó 08 y 12; B 09–11.
- **«Aprobado todo» (2026-11-24 ~23:30):** CU-134 con la recomendación de cada D-1…D-15 y el número CU-134; **D-3** plazos = descargo 5 días hábiles, apelación 5 días hábiles, prescripción 12 meses, suspensión máxima 90 días (propuesta aprobada por Pablo, **sin fuente normativa**, declarada así; viven en la tabla propia `politica_medida_cuenta` de identidad, desvío por la regla 98.1, editable sin código); **D-11** = la entrega del participante con medida vigente se **retiene** en custodia hasta resolver (si la medida queda firme, el destino de lo retenido **no está decidido**: ambigüedad A-1 a confirmar con Pablo, no inventar); las suposiciones A1–A6 del lote E tal como están en su PLAN; lote H: RESPALDO-003 (lámina 4 desde CU-23/CU-22, sin cifras), APROB-001 (se conservan AP-CU20-02/AP-CU69-10: el servidor aún los emite), grupos/detalle (la ruta ya existía), CONFIG-001 (firma de release por `APORTAYA_*`, falla sin ellas).

## Qué hacer ahora, en orden

1. **Arreglar los rojos de CI** de #519, #520, #521, #522 (cada sesión debería haberlo hecho; verificá). Los tres PRs de Java comparten «Formato, reglas propias y compilación» = `./gradlew spotlessCheck` (formato palantir; el formatter se puede correr localmente con el jar de palantir-java-format con `--palantir --replace`) y «bóveda/esquema no divergen» (regenerar con `generar_ddl.py`/`catalogo_de_datos.py --escribir` y `verificar_boveda.py`). Si hay 429 de Maven Central (`org.jacoco.ant`, `Could not resolve`), es ENVIRONMENT: esperar a que termine la corrida y `gh api -X POST …/actions/runs/<run>/rerun-failed-jobs`. **Nunca relanzar sobre una rama con corrida en curso** (`ci.yml` cancela la anterior por `concurrency`). Gradle local suele dar 429: Java se verifica en CI; para lo puro usar `javac -Xlint:all -Werror`.
2. **Mergear a dev** cada PR con CI verde (`gh api -X PUT repos/PabloArauzCaballero/PasanakuBackend/pulls/<n>/merge -f merge_method=merge -f sha=<head>`). Orden sugerido: #521 (H), #519 (G, modelo), #522 (C), #520 (F), PR de D. `identidad.yaml` y `catalogo` los comparten C, F, D, G: tras cada merge, los demás hacen merge de `origin/dev` y regeneran postman/inventario (`verificar_boveda` exige códigos de error correlativos por CU).
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
