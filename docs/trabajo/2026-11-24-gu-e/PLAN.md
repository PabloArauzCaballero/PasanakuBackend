# Plan — GU-E · Revocación inmediata, inactividad del backoffice y bitácora de cambios de acceso

- Fecha: 2026-11-24 · Repos afectados: PasanakuBackend (monorepo) · Rama: `claude/gu-e-revocacion` (desde `dev` 6729629d, incluye #512)
- Predecesor: [Gobierno de usuarios — INVENTARIO](https://github.com/PabloArauzCaballero/pasanakubackend/blob/claude/modest-newton-4ckcid/docs/trabajo/2026-11-24-gobierno-de-usuarios/INVENTARIO.md) (decisiones P-5 y P-8 de Pablo, 2026-11-24)
- Resultado observable: (1) cuando identidad revoca una sesión (cierre, robo, cuenta no habilitada, revocación de rol, inactividad), cualquier servicio rechaza con 401 un token de esa sesión aunque no haya vencido, típicamente en menos de un segundo; (2) un operador del backoffice que deja la pantalla 15 minutos sin actividad recibe un aviso dos minutos antes y después queda fuera, del lado del cliente y del servidor; (3) cada evento `identidad.usuario_interno_*` / `identidad.rol_*` (y los de CU-08 del lote B) queda escrito una sola vez en `comun.bitacora_evento`, consultable por `GET /auditoria/bitacora`.
- Kill-test: un token con `sesion_id` cuya revocación ya está en la lista sigue pasando el decodificador de `comun-web` → no está hecho. Un evento de acceso entregado dos veces deja dos filas en la bitácora → no está hecho.

## Alcance

- IN:
  - `plataforma/comun-web/.../seguridad/` (clases nuevas de revocación + cableado en `ConfiguracionDelDecodificador`).
  - `servicios/identidad`: publicador nuevo de revocaciones (`infraestructura/VigiaDeRevocaciones`), chequeo de inactividad en `RenovarSesion`, `application.yml` (claves nuevas).
  - Índice `ix_sesion_revocada_en` por la fuente de verdad (`docs/entidades/01_identidad_usuarios.puml` → `generar_ddl.py`).
  - `servicios/auditoria/.../consumo/` + caso de uso nuevo para la bitácora de accesos.
  - `despliegue/host/crear-topicos.sh`: tema compactado `aportaya.identidad.revocacion`.
  - Backoffice: vigilante de inactividad (`nucleo/`), aviso en el shell, mensaje en el ingreso.
- OUT (no se toca aunque se vea): archivos de los lotes A, B y G (`SegregacionDeFunciones`, semilla de roles, `UsuarioInternoMapeador`, `identidad.yaml` de accesos/usuarios internos, `AccesosController`, `CU08*`, `RotacionDeRolesInternos`, `GestionarUsuariosInternos`, `UsuariosInternosRepositorio`, `trabajos/` de identidad, `docs/CasosDeUso/` nuevo). El gateway (ADR-030 §5) y el endpoint de respaldo `GET /sesion/validez/{jti}` (ADR-030 §3): ver ambigüedades.
- Ambigüedades registradas (a confirmar con Pablo):
  - **A1 · latencia.** P-8 dice «< 1 s»; ADR-030 §4 fija «≤ 5 s». Supuesto: el diseño apunta a < 1 s típico (vigía cada 250 ms + Kafka sin espera) y la alerta/medición queda con la métrica de ADR-030; no se reescribe el ADR.
  - **A2 · clave del tema.** ADR-030 §1 dice clave = `jti`; identidad **no guarda** el `jti` (solo `sesion.revocada_en`), y revocar se hace por sesión. Supuesto: clave = `sesion_id` (claim `sesion_id` que ya firma `EmisorDeAcceso`); revocar la sesión invalida todos sus tokens. Un token sin `sesion_id` no es revocable por este mecanismo (hoy ningún flujo de producción lo emite así).
  - **A3 · «en el mismo commit».** ADR-030 §1 pide publicar en el mismo commit de la marca. Los puntos que revocan son al menos seis (dos en archivos del lote B y uno en un trigger de la base). Supuesto: un vigía que lee `sesion.revocada_en` y publica directo a Kafka (no outbox) cubre todos sin tocar archivos ajenos; se registra como desvío del ADR.
  - **A4 · arranque en frío.** ADR-030 §3 pide un endpoint de validez en identidad como respaldo; tocarlo exige `identidad.yaml` (lote B/C). Supuesto: mientras la lista no esté al día el servicio **falla cerrado** (401 a tokens con `sesion_id`) y su readiness queda DOWN; el endpoint queda pendiente y coordinado.
  - **A5 · actor en la bitácora.** Los eventos actuales de identidad no traen quién hizo el cambio. Supuesto: el consumidor lee `actorId`/`operadorId`/`otorgadoPor` si vienen (tolerante) y deja `actor_usuario_id` nulo si no; el lote B debe agregar el actor a sus eventos (se coordina en el PR).
  - **A6 · inactividad del servidor.** El servidor solo ve refrescos. Supuesto: el cliente refresca como «latido» mientras haya actividad humana (cada 5 min) y el servidor niega y revoca el refresco de un operador con más de 15 min sin refrescar; la actividad humana la decide el cliente.

## H1 — Revocación inmediata (GU-15, P-8)

**CA:** Dado un operador con token vigente, cuando identidad revoca su sesión, entonces cualquier servicio con `comun-web` rechaza ese token con 401 en cuanto recibe la revocación, y un token de otra sesión sigue entrando.
**DoD:** tests dirigidos de `comun-web` e identidad en verde en CI (`test`, `integrationTest`); compilación sin avisos; verificadores de `scripts/` en 0.
**Estado:** HECHO (peldaño TESTED: contra dobles de Kafka; broker real pendiente)

### H1.S1 — identidad publica la revocación

**CA:** Dada una sesión revocada por cualquier vía, entonces identidad publica `{sesionId, usuarioId, revocadaEn, venceEn}` en `aportaya.identidad.revocacion` con clave `sesion_id`, sin outbox.
**DoD:** prueba unitaria del vigía con dobles (correcto / límite / inválido) + DDL regenerado sin deriva.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Índice `ix_sesion_revocada_en` desde el `.puml` | `generar_ddl.py` produce el índice y no deja otro diff | `python3 scripts/generar_ddl.py && git diff --stat sql/` → solo `30_indices/01_identidad_usuarios.sql` (+ catálogo derivado si aplica) | HECHO |
| H1.S1.M2 | Contrato `RevocacionDeSesion` (record + JSON) en `comun-web/seguridad` | serializa/lee ida y vuelta; rechaza JSON sin `sesionId` o con fecha inválida | `./gradlew :plataforma:comun-web:test --tests '*RevocacionDeSesionTest'` (CI) | HECHO |
| H1.S1.M3 | `VigiaDeRevocaciones` en identidad: lee `sesion.revocada_en` con margen y publica directo (desvío: paginado por clave `(revocada_en, id)`, ver reporte) | publica una vez por sesión; repasa el margen; un fallo de Kafka no adelanta la marca | `./gradlew :servicios:identidad:test --tests '*VigiaDeRevocacionesTest'` (CI) | HECHO |
| H1.S1.M4 | Tema compactado en `crear-topicos.sh` | el script crea `aportaya.identidad.revocacion` con `cleanup.policy=compact,delete` | `bash -n despliegue/host/crear-topicos.sh` + `grep` de la línea | HECHO |

### H1.S2 — cada servicio rechaza el token revocado

**CA:** Dado un servicio con la lista al día, cuando llega un token cuya sesión está revocada, entonces 401; si la sesión no está revocada, entra; un servicio recién arrancado sin rehidratar no acepta tokens con `sesion_id`.
**DoD:** pruebas unitarias del validador, la lista y el lector con `MockConsumer` (rehidratación tras reinicio) en CI.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `ListaDeRevocaciones` en memoria con vencimiento | una revocación vence a `venceEn`; consulta O(1) | `--tests '*ListaDeRevocacionesTest'` (CI) | HECHO |
| H1.S2.M2 | `ValidadorDeRevocacion` en el decodificador | revocado → error `sesion_revocada`; vivo → ok; sin rehidratar → error | `--tests '*ValidadorDeRevocacionTest'` (CI) | HECHO |
| H1.S2.M3 | `LectorDeRevocaciones` (Kafka sin grupo, desde el inicio) | reconstruye la lista desde el tema al arrancar y marca «al día» al alcanzar el fin | `--tests '*LectorDeRevocacionesTest'` con `MockConsumer` (CI) | HECHO |
| H1.S2.M4 | Readiness y métrica `revocacion_propagacion_segundos` | readiness DOWN hasta estar al día; la métrica registra commit→recepción | `--tests '*SaludDeRevocacionesTest'` (CI) | HECHO |

## H2 — Cierre por 15 minutos de inactividad en el backoffice (P-8, ADR-038 §1)

**CA:** Dado un operador sin actividad, a los 13 min ve un aviso «tu sesión se cierra en 2 minutos» con «Seguir trabajando»; a los 15 min la sesión se cierra en el servidor y vuelve al ingreso con el motivo y con `volverA` a su pantalla. Del lado del servidor, un refresco de operador con más de 15 min sin actividad se niega y revoca la sesión.
**DoD:** `CU04InactividadTest` (integración) en CI; `yarn test:front` con la spec del vigilante; `yarn typecheck` y `yarn lint` en 0.
**Estado:** A MEDIAS (H2.S1.M1 espera la CI)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `RenovarSesion`: operador con `ultima_actividad_en` vieja → revoca `INACTIVIDAD` y rechaza | operador inactivo rechazado y sesión revocada; operador activo y participante inactivo renuevan | `./gradlew :servicios:identidad:integrationTest --tests '*CU04InactividadTest'` (CI) | A MEDIAS (espera CI: integrationTest) |
| H2.S2.M1 | `VigilanteDeInactividad` (actividad humana, latido, aviso, cierre, entre pestañas) | aviso a los 13 min, cierre a los 15, latido cada 5 min con actividad, reinicio con actividad | `yarn workspace @aportaya/backoffice test:front` (spec dirigida) | HECHO |
| H2.S2.M2 | Aviso en el shell + mensaje en el ingreso | el diálogo aparece, «Seguir trabajando» lo cierra; el ingreso explica el motivo | spec del shell/ingreso + `yarn typecheck` + `yarn lint` | HECHO |

## H3 — Bitácora de cambios de acceso (GU-07 consumidor, P-5)

**CA:** Dado un evento `identidad.usuario_interno_*` o `identidad.rol_*` publicado, cuando auditoría lo consume, entonces queda una fila en `comun.bitacora_evento` con entidad, acción, motivo, correlación y carga; el mismo evento dos veces deja una sola fila; un campo nuevo en la carga no rompe el consumo.
**DoD:** `CUBitacoraDeAccesosTest` (integración, PostgreSQL real) y prueba unitaria del lector del mensaje en CI.
**Estado:** A MEDIAS (H3.S1.M2 espera la CI)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Lector del mensaje (tipo → acción/entidad, tolerante a campos nuevos, rechaza sin cabeceras) | mapea los seis tipos actuales; ignora campos desconocidos; rechaza mensaje sin `event_id` | `./gradlew :servicios:auditoria:test --tests '*ConsumidorDeCambiosDeAccesoTest'` (CI) | HECHO |
| H3.S1.M2 | `RegistroDeCambiosDeAcceso` idempotente por `event_id` | duplicado → una fila | `./gradlew :servicios:auditoria:integrationTest --tests '*CUBitacoraDeAccesosTest'` (CI) | A MEDIAS (espera CI: integrationTest) |

## H4 — Cierre

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Verificadores de `scripts/` en 0 y generadores sin diff | todos rc=0 | comandos pegados en `evidencia/verificadores.txt` | HECHO |
| H4.S1.M2 | Push, PR a `dev`, CI en verde | checks verdes | enlace al PR + estado de checks | EN CURSO |
| H4.S1.M3 | REPORTE.md | tres secciones + evidencia | archivo en disco | HECHO |

## Simulación de los colaboradores (regla 65)

| Colaborador | Contrato | Doble | Correcto | Límite | Inválido |
|---|---|---|---|---|---|
| Kafka (publicación, vigía) | `KafkaTemplate.send(topic,key,value)` → futuro | `KafkaTemplate` mockeado | envío ok, marca avanza | misma sesión dos veces en el margen → se publica una vez | envío falla → la marca no avanza y se reintenta |
| Kafka (lectura, cada servicio) | tema con particiones y offsets | `MockConsumer` de kafka-clients | registros → lista | tema vacío / tema inexistente → al día sin entradas; reinicio → misma lista | valor ilegible → se descarta sin tumbar el lector |
| Eventos del lote B (CU-08) | sobre del outbox: cabeceras `event_id`, `type`, `correlation_id`, `occurred_at`, clave = agregado | registros construidos en la prueba | tipos actuales | campo nuevo desconocido; tipo nuevo `identidad.rol_x` | sin `event_id` → rechazo |

Lo verificado contra dobles queda pendiente de verificar contra Kafka real en el ensayo `e2eTest` (registrado en el reporte).

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Gradle local no resuelve dependencias (Maven Central 429) | No se puede compilar ni correr Java local | La CI es el juez; patrones vecinos copiados al pie de la letra; sin avisos (`-Werror`) |
| `CREATE INDEX` sobre `sesion` bloquea escrituras mientras se crea | Ingreso lento durante el despliegue | Tabla chica; el generador no admite `CONCURRENTLY`; se declara |
| Pisos de cobertura de `comun-web`/identidad/auditoría | CI roja | Pruebas unitarias de toda clase nueva |
| Conflicto con el lote B en identidad | Merge doloroso | Ningún archivo de B se toca |
