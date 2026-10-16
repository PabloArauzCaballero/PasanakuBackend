# Carril L3 (9.ª vuelta) — Recorrido por API real contra TEST con sandbox de licencia y contrato sintético de A

Fecha: 2026-10-11. API: `https://api.aportaya.161.97.85.216.sslip.io/api/v1`. Cuentas: las de `Temp\aportaya-s\cuentas-test.txt` (fuera del repo;
teléfonos, tokens y hex64 enmascarados; verificado con grep). Worktree rama `justin/feature/cierre-endurecimiento-2026-10-08`.

## Peldaño por área
* **Crear grupo en TEST (A): VERIFIED** — `POST /grupos` → `202`, `grupoId` real; persistió: `GET /grupos/participaciones` de A pasó de `[]` a la fila `ACEPTADO_PENDIENTE_FIRMA` (dos grupos, G1 y G2). Las dos compuertas anteriores (licencia, contrato) están abiertas.
* **Emitir invitación (A→B): VERIFIED con canal `SMS`; ROTO con `ENLACE`** (`500 AP-INT-01`, lo que la app ofrece por omisión).
* **Revocar y reemitir (G2): VERIFIED en identidad/grupos** — revocación `204` (idempotente: segunda vez `204`), un tercero recibe `403 AP-SEG-01`, `validar` pasa de `true` a `false`, y se puede reemitir.
* **B consulta/acepta el enlace: BLOQUEADO por un defecto de código — NO ocurrió.** B sigue con `participaciones = []`.
* **Rechazo por B / canje con backoffice / emulador: no ejecutados** (ver «No cubierto»).

## Hallazgos (cada uno con evidencia; ninguno es un dato que yo deba sembrar)
**H1 — PRODUCT_BUG, bloqueante: un grupo creado por la API queda en `BORRADOR` para siempre; por eso ninguna invitación por enlace se puede consultar ni aceptar.**
`POST /enlace/consultar` de B con un enlace que identidad da por `valido:true` (llamada directa a `/usuarios/tokens/invitacion/validar`, `03`, `05` paso 22) responde `422 AP-CU69-05`
(«Esa invitación ya no es válida»). Causa: `CU69Enlace.detalle` (`:157`) exige `g.estado IN ('CONFORMADO','ABIERTO_A_INSCRIPCION')` y `CreacionRepositorio:73` crea `BORRADOR`.
`CU20CrearGrupo` (javadoc `:32-36`) dice que `tarifas` congela el precio y `nucleo-financiero` abre la cuenta al consumir `grupos.grupo_creado`, y que entonces pasa a `ABIERTO_A_INSCRIPCION`. **Esa coreografía no existe:**
nadie consume `grupo_creado` (único archivo con ese nombre en `servicios/*/src/main` aparte del emisor: el contrato; consumidores Kafka de tarifas = ninguno, de nucleo = solo aportes y asientos) y ningún código escribe `grupo.estado` salvo el `BORRADOR` inicial (`11-evidencia-estatica-codigo.txt`).
Solo las semillas dev (`16-campania`) crean grupos ya abiertos, y los tests usan dobles (`DoblesDeTarifasYNucleo`). No es tarifas/cuenta del grupo «faltante de datos»: es el consumidor que falta (o la transición). Dueños: `grupos` + `tarifas` + `nucleo-financiero` (CU-20, regla 65: contrato + doble en tres niveles).
Nota de honestidad: no leí el estado del grupo en la base (no tenía autorización para usar las credenciales del servidor); es inferencia desde el código + la respuesta, no una consulta.

**H2 — PRODUCT_BUG + DATA de entorno: `canal=ENLACE` → `500 AP-INT-01` en TEST.** Log (`10`): identidad `AP-CU69-05 «No se pudo emitir esa invitacion»` y grupos lo registra como «fallo no previsto» (`HechosPorHttp.tokenDeInvitacion:185`, el `422` ajeno llega al cliente como `500 «Volvé a intentar»`).
`EmitirTokenDeInvitacion` rechaza si el canal no está en `politica_token.canales_permitidos`. El commit `d8e103ec` cambió la semilla de `SMS,WHATSAPP,CORREO` a `ENLACE,SMS,WHATSAPP,CORREO`, pero el sembrado es `ON CONFLICT (proposito, vigente_desde) DO NOTHING` y la fila de TEST conserva la vieja. Prueba por contraste: el mismo pedido con `SMS` → `201`.
Qué lo destraba (NO lo sembré): confirmar con `SELECT canales_permitidos FROM identidad.politica_token WHERE proposito='INVITACION_GRUPO'` y, si falta ENLACE, insertar una versión nueva de la política (`vigente_desde` posterior; no pisar la fila) — dueño: identidad/cumplimiento. Defecto de código aparte: el rechazo de otro servicio no debe traducirse a `500`.

**H3 — PRODUCT_BUG: `canje` quema el enlace antes de validar la postulación.** `RutasDeInvitacion.canjearInvitacion:103` consume el token en identidad (commit propio) y recién después `canjes.ejecutar` valida/crea la solicitud. B tenía una postulación pendiente (la creé yo en `03`); el canje falló con `422 AP-CU68-06` y el token quedó `CONSUMIDO`:
`validar` → `false`, A no puede revocar (`422 «No se pudo revocar esa invitacion»`, `04`) ni reemitir (`422 AP-CU69-09`, hay una invitación local vigente): el par (G1, B) quedó sin salida. Cualquier fallo de `postular` tras el consumo produce lo mismo. Además la reemisión fallida (`AP-CU69-09`) ya había emitido un token huérfano en identidad (`10`, 22:15:28) que cuenta para el tope diario.

**Observabilidad:** `HechosPorHttp.enlaceDeInvitacionValido` traga cualquier `RuntimeException` y devuelve `false` sin log: un fallo de red/permiso entre servicios se ve igual que «enlace vencido».

## Qué se hizo, en orden (`01`–`05`)
Login A y B con segundo factor `200`; `licencia/alcance GRUPO_PASANAKU` = `habilitado:true, via:SANDBOX`; `POST /grupos` G1 y G2 `202`; invitación `ENLACE` `500`, `SMS` `201`; B `validar` directo `true`; B `consultar` `422` (H1); B `postulaciones` `201` (el motor devuelve `SIN_DATOS_REPUTACION`: requiere valoración humana); canje `422` (H3); G2: invitar `201`, revocar `204`, validar `false`, reemitir `201`, validar `true`.
Efectos que dejé en TEST (sintéticos, para el dueño): G1 y G2 de A («L3 API uno/dos»), una postulación pendiente de B en G1, un enlace consumido y uno huérfano, una invitación vigente a B en G2 (SMS).

## Emulador / APK
No se abrió el emulador: la condición era que el API permitiera crear **y aceptar**, y aceptar no es posible (H1). Sin cambios en `apps/movil`, sin APK nuevo; `aportaya-test-5-TEST.apk` sigue vigente. Hallazgo para la app cuando se arregle H2/H1: la pantalla «Invitar» usa `ENLACE` por omisión (`pantalla_invitar.dart:39`) y hoy en TEST eso termina en el 500 genérico.

## No cubierto
Aceptar por enlace, rechazo por B (la app no tiene «Rechazar»; el rechazo es la revocación de A, probada por API), efecto en A tras aceptar, canje resuelto por backoffice, firma del reglamento de A (`ACEPTADO_PENDIENTE_FIRMA`), UI en emulador, D4 (billetera), estado real de `grupo.estado` y de `politica_token` en la base de TEST (solo inferido), regresión de código.
