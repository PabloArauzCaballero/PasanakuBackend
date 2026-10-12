# Carril P — ESTADO al cierre (2026-10-11). Peldaño: WRITTEN. NADA compilado ni probado.

Causa: la máquina nunca llegó a 2500 MB libres (600-2400); `heavy.sh` venció 2 veces (3000 s). Cero corridas Gradle/Docker.
Lo único ejecutado: `generar_semillas.py`, `generar_compose.py` (+`--coolify`), `verificar_criterios.py` (OK), `verificar_seguridad.py` (OK tras corregir un `+` en SQL de test), `verificar_boveda.py` (OK), `verificar_pruebas_web.py` (OK). Plan: `PLAN-P.md`.

## Cambios (todo SIN verificar por Gradle)
- H1 apertura (CU-20): tarifas `aplicacion/CongelarTarifaDelGrupo`, `infraestructura/TarifaCongeladaRepositorio`, `consumo/ConsumidorDeGruposCreadosKafka` (grupo_creado -> snapshot+hash SHA-256, UNIQUE grupo, emite `tarifas.tarifa_congelada`). grupos `aplicacion/AbrirInscripcionDelGrupo`, `consumo/ConsumidorDePreciosCongeladosKafka` (BORRADOR->ABIERTO_A_INSCRIPCION con `AND estado='BORRADOR'`, historial, idempotente). application.yml de ambos: `auto-offset-reset: earliest` + interruptores ENCENDIDOS (`CONGELAMIENTO_/APERTURA_CONSUMO_HABILITADO`); `generar_compose.py` + `docker-compose.coolify.yml` regenerado.
- H2: `HechosPorHttp` (250 lín.) delega en nuevo `TokensDeInvitacionPorHttp` (422 de identidad -> `ErrorDeNegocio` con su código; otros 4xx/5xx no se traducen). Política: `seeders/minimos/13` (fila vieja restaurada + versión `2026-10-11` con ENLACE), `sql/60_semillas/13` regenerado, `scripts/generar_semillas.py` (SIEMBRAS_DE_TEST con carpeta), nuevo `despliegue/ensayos/politica_invitacion_test.sh` (NO ejecutado; ni siquiera probado local).
- H3: `PrecondicionesDePostulacion` (extraído de `CU68Postular.postular`), `CU68Postular.comprobar`, `CanjearInvitacion.comprobarAntesDeConsumir`, `RutasDeInvitacion.canjearInvitacion` comprueba ANTES de consumir + log si falla tras consumir. Token huérfano 69-09: ya se compensaba (`InvitacionesWeb.invitar`, test existente); solo se añadió log en `revocar`.
- H4: `TokensDeInvitacionPorHttp.esValido` loguea clase/HTTP (sin teléfono/secreto).
- Delegado: CU-18 rate limit identidad: `web/LimiteDeConsultas` (copia del de notificaciones), `UsuariosController.verificarTitularidad` -> 429+Retry-After, `aportaya.titularidad.consultas-por-minuto: 120`, 429 en `identidad.yaml`.
- Docs: CU-20, CU-69, CU-18, READMEs grupos/tarifas, `despliegue/TEST.md` (sección carril P + decisiones delegadas), `grupos.yaml` descripción de `POST /grupos`, javadoc CU20.
- Tests escritos (sin correr): grupos `CU20AperturaTest`, `ConsumidorDePreciosCongeladosKafkaTest`, `CU69CanjeTest` (+3), `web/CU69CanjeOrdenWebTest`, `HechosEmisionDeInvitacionTest`; tarifas `CongelarTarifaDelGrupoTest`, `ConsumidorDeGruposCreadosKafkaTest`; identidad `CU69PoliticaVersionadaTest`, `web/UsuariosTitularidadLimiteWebTest`.

## Falta (en orden)
1. Compilar/formatear: `:servicios:{grupos,tarifas,identidad}:spotlessApply` y `compileTestJava`. Riesgos probables: INSERT..SELECT con parámetros en `AbrirInscripcionDelGrupo`; imports sobrantes; DisplayName largos.
2. Correr `test webTest integrationTest testBarrido` de los 3 servicios; conteos desde `build/test-results/*.xml`.
3. `:servicios:grupos:generarClientes :servicios:identidad:generarClientes` y `yarn build` en `packages/simulado` (cambiaron `grupos.yaml` y `identidad.yaml`); compilar web/backoffice.
4. Ensayo de `politica_invitacion_test.sh` contra Postgres desechable 5453 (`PG_CONTENEDOR=… PG_ENV=/x PG_USUARIO=pasanaku PG_CLAVE=pasanaku`), dos corridas ("cambios: 0").
5. Si la cobertura (`pisoDeCobertura`) baja, revisar. Prueba end-to-end con Kafka real (cadena grupo_creado -> tarifa_congelada -> abierto) no existe.
Comando: `bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash <scratchpad>/p/p_full.sh --continue <tareas>` (Postgres 5453, JAVA_HOME Adoptium 21).

## Supuestos / pendientes de dueño
- A1: abre con el precio congelado sin esperar la cuenta del grupo (nucleo-financiero no la abre) — producto + dueño de nucleo.
- A2: snapshot = tarifario + conceptos activos + reglas; hash = SHA-256 del texto jsonb — producto/finanzas.
- Límite 120/min de titularidad es SINTÉTICO (Seguridad). Residual canje: carrera entre comprobación y consumo deja enlace consumido (queda log). Cuota diaria cuenta emisiones compensadas.
- VPS (no ejecutado): correr `politica_invitacion_test.sh` y `despliegue/host/crear-topicos.sh` (tópico `aportaya.tarifas.tarifa_congelada`).
- Docker Desktop quedó abierto: no lo inicié yo; no hay contenedores míos ni candado.
