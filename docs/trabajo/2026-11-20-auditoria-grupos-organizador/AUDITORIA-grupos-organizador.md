# Auditoría grupos-organizador (lote L3) — solo lectura

> Documento en construcción: los hallazgos se catalogan en el momento en que se confirman y la rama se
> actualiza por tandas. Las secciones marcadas «(pendiente)» se completan antes del cierre.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | grupos-organizador — `servicios/grupos`, `servicios/organizador`, alta, admisión, inicio, sorteo, postulaciones |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — `git diff --quiet` sin diferencias en `servicios/grupos`, `servicios/organizador`, `sql/10_tablas/02_grupos_turnos`, `sql/10_tablas/07_organizador_automatizacion`, `apps/backoffice/src/app/rutas`, `apps/movil/lib/pantallas/pasanaku`, `contratos`, `clientes/dart/grupos` |
| Historial | El clon era superficial; se aplicó `git fetch --unshallow` → `git rev-parse --is-shallow-repository` = `false` (1878 commits). 119 commits tocan `servicios/grupos` y 28 `servicios/organizador` |
| Archivos / líneas cubiertos | (pendiente) |

## 2. Resumen

(pendiente)

## 3. Huella y mapa de conexiones

(pendiente)

## 4. Distancia al flujo nuevo

### 4a. Inventario de aprobaciones

| ID | Dónde | Qué aprueba y quién | Estado · endpoint · permiso · pantalla · evento | Clasificación | Qué se rompe aguas abajo si se quita |
|---|---|---|---|---|---|
| APR-01 | `servicios/grupos/src/main/resources/application.yml:73-78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:141-150`; `.../web/InvitacionesWeb.java:127-133`; `.../web/RutasDeAdmision.java:25-37` | Ingreso de un integrante: el organizador **propone** y backoffice (`ADMIN_PLATAFORMA`) **resuelve** | Solicitud `PENDIENTE` → decisión `PROPUESTA` → `RESOLUCION`; `POST /grupos/solicitudes/{id}/propuestas` (`GRUPO_ADMINISTRAR`), `POST /grupos/solicitudes/{id}/resoluciones` (`ADMIN_PLATAFORMA`); pantallas backoffice `rutas/admisiones`, `rutas/operacion/solicitudes-escaladas`; eventos `aportaya.grupos.admision_propuesta`, `aportaya.grupos.admision_resuelta` | ELIMINABLE desde producto (el propio `application.yml:74` la declara «DECISION PENDIENTE (negocio y cumplimiento)»); queda DECISIÓN REQUERIDA solo si Cumplimiento la exige (A-1) | Las puertas «aceptar por enlace» y «decisión directa del organizador» hoy devuelven 422 (`CodigoError 69-10` y `68-15`) con el interruptor encendido; apagarlo reabre esos caminos, que crean participante sin expediente; la bandeja de backoffice y sus pruebas quedan sin uso |
| APR-02 | `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDelGrupo.java:94-95`; `.../aplicacion/CU69IniciarGrupo.java:89-91`; `.../web/GruposController.java:164-173` | Inicio del grupo y compromiso/revelación del sorteo: los dispara el administrador del grupo | `ABIERTO_A_INSCRIPCION → CONFORMADO`; `POST /grupos/{id}/inicio`, `POST /grupos/{id}/sorteo`, `POST /grupos/{id}/sorteo/revelacion`, todos `GRUPO_ADMINISTRAR`; app `inicio_del_grupo.dart`, `pantalla_sorteo_*`; eventos `grupo_iniciado`, `sorteo_comprometido`, `sorteo_revelado` | ELIMINABLE (ADR-054 D3 ya decidió que sean automáticos) | Sin el disparo manual hoy no hay ningún otro camino (0 `@Scheduled`): quitarlo sin el trabajo programado deja todo grupo detenido |
| APR-03 | `servicios/organizador/src/main/java/bo/aportaya/organizador/web/OrganizadoresController.java:116-129`; `.../web/RutasDeHabilitacion.java:106-129`; backoffice `apps/backoffice/src/app/rutas/postulaciones/`, `apps/backoffice/src/app/rutas/cumplimiento/organizadores/pantalla-de-habilitacion.ts` | Habilitación del organizador «profesional»: backoffice aprueba o rechaza la postulación, la confirma o revoca y habilita tras capacitación | `POST /organizadores/postulaciones/{id}/aprobacion`, `.../resolucion`, `.../revision`, `POST /organizadores/{id}/habilitacion`, todos `ADMIN_PLATAFORMA`; eventos `organizador.postulacion_aprobada`, `habilitacion_resuelta`, `habilitado` | DECISIÓN REQUERIDA (A-1): ADR-054 D6 la deja fuera del camino principal, pero sigue siendo puerta para organizar si no existe el grupo autogestionado (COHERENCIA-002) | Nadie llega a organizador profesional; la vía básica (APR-05) sigue |
| APR-04 | `servicios/organizador/src/main/java/bo/aportaya/organizador/web/AutomatizacionController.java:89-93`; `.../aplicacion/CU96EjecutarTarea.java:30-31,79-80,108-124` | Tarea automatizada «sensible»: nace en `REQUIERE_APROBACION` y la confirma una persona con rol `ORGANIZADOR` | `REQUIERE_APROBACION → PROGRAMADA`; `POST /automatizacion/tareas/{id}/aprobacion`; evento `organizador.tarea_aprobada` | CONSERVAR POR NORMA interna: R-ORG-06 «Las acciones sensibles exigen confirmación humana» (`docs/Restricciones.md:2667`) y regla 91.4 (sanciones). Confirmar con Pablo que R-NUEVA-1 no la alcanza | Una regla configurada que sanciona o mueve dinero se ejecutaría sin confirmación humana |
| APR-05 | `servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/CU90HabilitacionBasica.java:28-43`; app `contrato_de_organizador.dart` | Habilitación «Aprendiz»: el propio participante acepta y firma el contrato; **no** aprueba nadie | `POST /organizadores/yo/habilitacion-basica` (`PARTICIPANTE`) | No es aprobación de un tercero; queda como puerta previa al alta mientras exista COHERENCIA-002 | Sin ella, hoy nadie puede crear un grupo (`AP-CU20-02`) |

### 4b. Inventario de respaldo y cobertura

(pendiente)

### 4c. Ambigüedades A-1 a A-4

(pendiente)

## 5. Hallazgos

### grupos-organizador-APROB-001 — La admisión humana de backoffice está encendida por omisión y bloquea todo ingreso sin aprobación
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:73-78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:30-31,141-150`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/InvitacionesWeb.java:55-56,127-133`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeAdmision.java:25-37`
- Evidencia:
  ```yaml
  # DECISION PENDIENTE (negocio y cumplimiento): toda entrada al grupo pasa por admision humana de backoffice?
  # true (produccion): las dos puertas heredadas de TEST ... se cierran con 422 ...
  humana-obligatoria: ${ADMISION_HUMANA_OBLIGATORIA:true}
  ```
  ```java
  if (admisionHumanaObligatoria) {
      throw new ErrorDeNegocio(CodigoError.de(68, 15),
          "Este grupo exige admision humana: tu decision es una propuesta (proponerAdmision) y la resuelve backoffice.");
  ```
  ```java
  @Permiso("ADMIN_PLATAFORMA")
  public ResponseEntity<DecisionIngreso> resolverAdmision(...) { ... admision.decidir(..., true)); }
  ```
- Cómo se comprobó: lectura de las cuatro ubicaciones; `grep -rn 'admisionHumanaObligatoria\|humana-obligatoria' servicios/grupos/src/main` → solo esas dos clases leen el interruptor.
- Impacto: con el valor por omisión, ningún participante entra a un grupo sin que una persona de backoffice resuelva su solicitud. Contradice R-NUEVA-1 de frente: el flujo no avanza solo por pago QR. Para el grupo: inscripción detenida hasta que operación actúe.
- Causa de fondo: la decisión de negocio quedó modelada como interruptor de despliegue en vez de política explícita del producto, y su valor por omisión es el más restrictivo.
- Solución de fondo: que Pablo decida (A-1) si la admisión necesita intervención humana. Si no la necesita, retirar el camino propuesta/resolución y dejar un único camino de admisión automática por reglas (KYC, restricción, reputación, cupo). Si Cumplimiento la exige, documentar la norma y sacarla de un `@Value` de despliegue.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: backoffice (`rutas/admisiones`, `rutas/operacion/solicitudes-escaladas`), app móvil (unirse/pedir cupo), eventos `admision_propuesta`/`admision_resuelta`.
- Estándares y normas: ISO/IEC 25010 adecuación funcional (corrección funcional); reglas 00 §1.7 (ambigüedad resuelta por configuración), 96.4.
- Esfuerzo estimado: medio

### grupos-organizador-HUECO-001 — No existe el escalamiento de solicitudes vencidas que la pantalla de backoffice promete
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/solicitudes-escaladas/cola-de-solicitudes-escaladas.ts:14-22`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:58,141,163,211,1095` (rutas de solicitudes publicadas); `scripts/verificar_rutas_frontend.py:27`
- Evidencia:
  ```ts
  * D-15 · solicitudes de ingreso que el organizador dejó vencer (48 horas).
  * **Sin contrato (2026-10-26, franja B).** Ni `grupos.yaml` ni el simulado ... publican una cola de solicitudes escaladas
  ```
- Cómo se comprobó: `grep -rniE 'escalad|PT48H|48 ?h|vencid' servicios/grupos/src/main` → ningún trabajo programado ni estado que venza una solicitud `PENDIENTE`; las únicas coincidencias son de invitaciones, feriados y traspasos. `grupos.yaml` no publica `/grupos/solicitudes-escaladas`.
- Impacto: una solicitud que el organizador nunca propone queda `PENDIENTE` sin plazo; con APROB-001 encendido, el participante no entra nunca y nadie se entera. Para el grupo: el inicio al 75 % (CU-69) puede no alcanzarse.
- Causa de fondo: la regla de 48 h se diseñó en la pantalla y en el texto, nunca en el servicio dueño.
- Solución de fondo: depende de A-1. Si la admisión deja de ser humana, el hueco desaparece con APROB-001. Si se conserva, el vencimiento va en `grupos` como trabajo programado con estado propio y evento.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: backoffice `operacion/solicitudes-escaladas`; menú `apps/backoffice/src/app/nucleo/menu.ts:91`.
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 00 §5.4 (implementación sustituida por texto), 95.2.
- Esfuerzo estimado: medio

### grupos-organizador-ARQ-001 — 13 de los 35 casos de uso de `grupos` escriben SQL directo con jOOQ desde la capa de aplicación
- Clase / Severidad / Confianza: ARQ / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/{CU60Sortear,CU69Enlace,CU69EnlaceDeGrupo,CU69FirmarReglamento,CU69IniciarGrupo,ConsultarDetalleDelGrupo,GrupoAbierto,IndicadoresComerciales,IndicadoresDeTurnos,LecturasDelDetalle,OrganizadorDecideIngreso,PrecondicionesDePostulacion,ReglamentoAlIniciar}.java`; ejemplo `CU69IniciarGrupo.java:22-24,83,122-128,219-224`
- Evidencia:
  ```java
  import org.jooq.DSLContext;
  ...
  UPDATE grupos.grupo SET estado = 'CONFORMADO', cupos_totales = ?, ...
   WHERE id = ? AND estado = 'ABIERTO_A_INSCRIPCION'
  ```
- Cómo se comprobó: `grep -l 'import org.jooq' servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/*.java | wc -l` → 13 sobre 35 archivos.
- Impacto: la regla de transición de estado del grupo vive en un `UPDATE` dentro del caso de uso. Cualquier cambio de esquema rompe la capa de aplicación, y las pruebas unitarias del caso de uso no se pueden hacer sin base. Encarece cada cambio.
- Causa de fondo: los puertos de persistencia (`infraestructura/*Repositorio`) existen pero no se usan de forma uniforme; conviven dos estilos en la misma capa.
- Solución de fondo: mover el SQL a repositorios de `infraestructura` detrás de puertos de `dominio/puertos`, como ya hacen los otros 22 casos de uso.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno fuera de `grupos`.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (modularidad, modificabilidad); ADR-033 (puertos y adaptadores), ADR-016 (jOOQ en infraestructura); regla 96.1.3.
- Esfuerzo estimado: grande

### grupos-organizador-PRUEBAS-001 — El verificador de rutas del frontend falla por una ruta de grupos marcada como conocida
- Clase / Severidad / Confianza: PRUEBAS / S4 / confirmado
- Ubicaciones: `scripts/verificar_rutas_frontend.py:27`
- Evidencia (salida literal en `evidencia/verificar_rutas_frontend.txt`):
  ```text
  FALLA · la lista de conocidas se pudrió: estas rutas YA tienen contrato (sacalas de CONOCIDAS_SIN_CONTRATO):
     - GET /grupos/solicitudes-escaladas
  [exit 1]
  ```
- Cómo se comprobó: `python3 scripts/verificar_rutas_frontend.py` → exit 1. Pero `grep -n 'solicitudes-escaladas' servicios/grupos/src/main/resources/openapi/grupos.yaml` no devuelve nada: la ruta **no** tiene contrato. El script decide «tiene contrato» por otra regla, probablemente el prefijo `/grupos/solicitudes`, y se equivoca.
- Impacto: un gate de CI en rojo por un falso positivo acostumbra a ignorar el rojo.
- Causa de fondo: el emparejamiento de rutas del script es más laxo que el contrato.
- Solución de fondo: emparejar por plantilla de ruta completa, sin prefijo. Al resolverse HUECO-001, la entrada se retira de la lista.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: herramienta transversal `scripts/`.
- Estándares y normas: ISO/IEC 25010 fiabilidad (madurez del proceso de verificación); regla 80.5.
- Esfuerzo estimado: chico

### grupos-organizador-APROB-002 — El inicio del grupo y las dos fases del sorteo solo ocurren si el administrador las dispara a mano
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDelGrupo.java:94-95`; `.../web/GruposController.java:164-173`; `.../aplicacion/CU69IniciarGrupo.java:89-91`; `.../web/SorteoDelGrupo.java:88-92`; `.../infraestructura/AdmisionRepositorio.java:40-46`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:77`
- Evidencia:
  ```java
  if (g == null || !admision.esAdministrador(dsl, grupoId, ctx.usuarioId())) {
      throw new ErrorDeNegocio(CodigoError.de(69, 7), "Solo quien organiza el grupo puede iniciarlo.");
  ```
  ```java
  @Permiso("GRUPO_ADMINISTRAR")
  public ResponseEntity<CompromisoDeSorteo> comprometerSorteo(
  ```
  ADR-054 D3 (aceptada 2026-10-31): «**Inicio y sorteo automáticos.** El grupo se conforma al firmarse el último cupo, o en la **fecha límite** …».
- Cómo se comprobó: lectura de las ubicaciones; `grep -rn '@Scheduled' servicios/grupos servicios/organizador` → **0 resultados**. No hay trabajo programado que conforme el grupo ni que comprometa o revele el sorteo. `esAdministrador` exige un participante con `es_organizador`.
- Impacto: un grupo lleno con las primeras cuotas cobradas por QR no avanza si quien lo administra no aprieta «iniciar», «sortear» y «revelar». Es un paso humano en el circuito, contra R-NUEVA-1, y una decisión ya aceptada (ADR-054 D3) sin implementar. Los participantes ven su dinero cobrado sin turno asignado.
- Causa de fondo: ADR-054 se aceptó sin que su carril «`grupos` — inicio y sorteo» (ADR-054 §«Lo que falta en el código», fila 2-3) llegara al código.
- Solución de fondo: trabajo programado en `grupos`, con lock distribuido, que conforme el grupo al completarse o en `fecha_inicio`, y comprometa o revele el sorteo cuando todas las primeras cuotas estén acreditadas. El endpoint manual queda, como mucho, de respaldo operativo.
- Relación con el flujo nuevo: APROB / FLUJO-QR (el pago confirmado no mueve el grupo por sí solo)
- Conexiones y módulos afectados: `aportes` (primera cuota, `PrimerAportePorHttp`), app móvil (`inicio_del_grupo.dart`, `pantalla_sorteo_*`), evento `aportaya.grupos.grupo_iniciado` → `aportes` (pacto `grupos-aportes-grupo-iniciado.json`).
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 00 §6.2; ADR-054 D3.
- Esfuerzo estimado: grande

### grupos-organizador-HUECO-002 — No existe la cancelación con devolución de un grupo que no llega al 75 % (ADR-054 D4)
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:78`; ausencia en `servicios/grupos/src/main/java/**`
- Evidencia: ADR-054 D4: «Si no llega al 75 % en la fecha límite, se cancela y devuelve solo: `estado = CANCELADO` … evento `grupo.cancelado_sin_inicio`».
- Cómo se comprobó: `grep -rln 'cancelado_sin_inicio' --include=*.java --include=*.yaml --include=*.sql --include=*.json .` → 0 archivos; `grep -rn '@Scheduled' servicios/grupos` → 0. Las menciones de `CANCELADO` en `grupos` son solo filtros de lectura (`ConsultasRepositorio.java:55`, `ConsultarMisLimites.java:43`, `CU20CrearGrupo.java:174`).
- Impacto: un grupo que nunca alcanza el 75 % queda `ABIERTO_A_INSCRIPCION` para siempre. Si alguno ya pagó su primera cuota por QR (la regla de inicio la exige), ese dinero no tiene devolución automática.
- Causa de fondo: la misma de APROB-002, el carril de ADR-054 sin implementar.
- Solución de fondo: transición `ABIERTO_A_INSCRIPCION → CANCELADO` en `grupos`, con evento por outbox, y saga de devolución en el servicio dueño del dinero (L1). No requiere decisión nueva: D4 ya la tomó.
- Relación con el flujo nuevo: FLUJO-QR (dinero cobrado sin destino)
- Conexiones y módulos afectados: `aportes`/`nucleo-financiero` (devolución), notificaciones.
- Estándares y normas: ISO/IEC 25010 completitud funcional; regla 91.3, 98.3.2.
- Esfuerzo estimado: grande

### grupos-organizador-COHERENCIA-001 — El interruptor de admisión humana es global; ADR-054 lo quiere por tipo de conformación y sin admisión en el grupo de confianza
- Clase / Severidad / Confianza: COHERENCIA / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:78`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:30-31`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:58-59,76,79,93`
- Evidencia: ADR-054 D2 «El enlace es la admisión … Nadie aprueba a nadie»; D5 «El interruptor pasa de global a **por `tipo_conformacion`**». En el código, `@Value("${aportaya.grupos.admision.humana-obligatoria}") private boolean admisionHumanaObligatoria;` es un único booleano.
- Cómo se comprobó: `grep -rn 'tipo_conformacion' servicios/grupos/src/main` → 0 usos en lógica de admisión.
- Impacto: tres fuentes dicen tres cosas sobre la admisión: ADR-054 (sin aprobación en el grupo de confianza), `application.yml` (aprobación humana en todos) y R-NUEVA-1 (sin aprobación en ninguno). Manda el código, que es la más restrictiva.
- Causa de fondo: decisiones de producto aceptadas sin el cambio de código que las materializa.
- Solución de fondo: Pablo confirma R-NUEVA-1 sobre ADR-054 D5 (A-1). Después se actualiza el ADR y el interruptor desaparece.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: ninguno adicional a APROB-001.
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 00 §8 (prioridad de evidencia).
- Esfuerzo estimado: chico (si se decide quitar)

### grupos-organizador-COHERENCIA-002 — El alta de un grupo exige un organizador habilitado; ADR-054 D1 admite grupos sin organizador
- Clase / Severidad / Confianza: COHERENCIA / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU20CrearGrupo.java:86-88`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java:115`; `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java:116`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:75`; `apps/movil/lib/pantallas/pasanaku/pantalla_crear_grupo.dart:42-44`
- Evidencia:
  ```java
  if (entrada.organizador().isEmpty() || !entrada.organizadorHabilitado()) {
      throw new ErrorDeNegocio(CodigoError.de(20, 2), "Ese organizador no esta habilitado.");
  ```
  ```java
  .set(GRUPO.ES_AUTOGESTIONADO, organizador.isEmpty())   // nunca true: la línea 86 ya rechazó el vacío
  ```
  ADR-054 D1: «Existen grupos sin organizador humano … el organizador humano habilitado es una opción, no un requisito».
- Cómo se comprobó: lectura de las ubicaciones. `CreacionRepositorio.java:116` solo se alcanza después de `CU20CrearGrupo.java:86`, así que `es_autogestionado` siempre queda `false`. La app comenta lo contrario («Nulo … lo administra la plataforma (RN-18)», informado por el subagente de frontend y abierto por mí).
- Impacto: toda persona que quiere crear un pasanaku tiene que hacerse organizador primero. Hoy pasa por un autoservicio (`CU90HabilitacionBasica`: KYC + contrato firmado, sin aprobación humana; ver APR-05). El grupo autogestionado de ADR-054 no se puede crear. Además, la creación queda acoplada a `organizador` por HTTP en el camino crítico.
- Causa de fondo: ADR-054 aceptado sin su carril de código; quedaron rama muerta (`ES_AUTOGESTIONADO`) y comentario que miente.
- Solución de fondo: decidir si R-NUEVA-1 alcanza a la habilitación del creador (A-1). Si no la alcanza, implementar ADR-054 D1: sin organizador, `es_autogestionado = true`, creador como anfitrión.
- Relación con el flujo nuevo: APROB (puerta previa al circuito)
- Conexiones y módulos afectados: `organizador` (`GET /organizadores/usuarios/{id}/habilitacion`), app móvil (`contrato_de_organizador.dart`, `pantalla_crear_grupo.dart`).
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 00 §8; ADR-054 D1.
- Esfuerzo estimado: medio

### grupos-organizador-CONTRATO-001 — La app móvil usa dos puertas de admisión que el contrato cierra en producción y no tiene las que las reemplazan
- Clase / Severidad / Confianza: CONTRATO / S2 / confirmado (código); probable (el valor real de producción no se pudo ver)
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/dominio/solicitudes_del_grupo.dart:24-47`; `apps/movil/lib/pantallas/pasanaku/dominio/cu69_unirse_por_enlace.dart:38-56`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:144-150`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/InvitacionesWeb.java:127-133`; `servicios/grupos/src/main/resources/openapi/grupos.yaml:886`; `docker-compose.coolify.yml:377`; `apps/movil/lib/dominio/errores.dart:76`
- Evidencia:
  ```dart
  await DefaultApi(ref.read(dioProvider)).decidirSolicitudDeIngreso(
    ... motivo: aceptar ? null : 'El organizador no aceptó la solicitud.',
  ```
  ```dart
  final r = await dio.post<Map<String, dynamic>>('/grupos/invitaciones/enlace/aceptar', data: {...});
  ```
  ```yaml
  ADMISION_HUMANA_OBLIGATORIA: ${ADMISION_HUMANA_OBLIGATORIA:-false}   # docker-compose.coolify.yml:377 (TEST)
  humana-obligatoria: ${ADMISION_HUMANA_OBLIGATORIA:true}              # application.yml:78 (por omisión)
  ```
- Cómo se comprobó: `grep -rnE 'proponerAdmision|/propuestas|canjearInvitacion|/canje' apps/movil/lib` → 0 resultados. `grep -nE "AP-CU68|AP-CU60" apps/movil/lib/dominio/errores.dart` → 0; solo existe `'AP-CU69-10'` en la línea 76.
- Impacto: en TEST el interruptor está en `false` y la app funciona. Donde rija el valor por omisión `true`, la aceptación del organizador responde 422 `AP-CU68-15` (sin texto propio: genérico) y la invitación por número responde 422 `AP-CU69-10`. El participante invitado no puede entrar desde la app. Se considera discrepancia latente (S2) y no S1 porque en el entorno vivo auditado el camino funciona.
- Causa de fondo: el contrato cambió de comportamiento por un interruptor de entorno sin versión ni cliente actualizado (regla 98.1.3); ver también FRAGIL-001.
- Solución de fondo: resolver A-1. Si la admisión queda sin aprobación, retirar el interruptor y conservar las puertas que la app usa. Si no, la app implementa `proponerAdmision` y `canjearInvitacion` y su catálogo de errores.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: app móvil, `grupos`.
- Estándares y normas: ISO/IEC 25010 compatibilidad (interoperabilidad); RFC 9110 (422); reglas 96.4.1, 98.1.3, 95.2.3.
- Esfuerzo estimado: medio

### grupos-organizador-FRAGIL-001 — El mismo interruptor tiene valor por omisión distinto en TEST y en producción
- Clase / Severidad / Confianza: FRAGIL / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:78`; `docker-compose.coolify.yml:377`
- Evidencia: `humana-obligatoria: ${ADMISION_HUMANA_OBLIGATORIA:true}` frente a `ADMISION_HUMANA_OBLIGATORIA: ${ADMISION_HUMANA_OBLIGATORIA:-false}`.
- Cómo se comprobó: `grep -rn 'ADMISION_HUMANA_OBLIGATORIA' --include=*.yml --include=*.yaml .` → solo esas dos definiciones.
- Impacto: todo lo que se prueba en TEST (E2E, pruebas manuales, app) ejercita las puertas que producción cierra. Si cambia una variable de entorno en el despliegue, se rompe la admisión en la app (CONTRATO-001) y ninguna prueba de TEST lo detecta.
- Causa de fondo: una decisión de negocio pendiente (`application.yml:74`) resuelta por configuración, distinta por entorno.
- Solución de fondo: la de APROB-001. Mientras exista el interruptor, el mismo valor en todos los entornos de prueba que el de producción.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: despliegue (L5), app móvil.
- Estándares y normas: ISO/IEC 25010 fiabilidad (madurez); regla 90.3.4.
- Esfuerzo estimado: chico

### grupos-organizador-HUECO-003 — El catálogo de errores de la app no tiene los códigos de admisión ni de sorteo
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `apps/movil/lib/dominio/errores.dart:54-78`; consumidores `apps/movil/lib/pantallas/pasanaku/pantalla_pedir_cupo.dart:136`, `pantalla_sorteo.dart:127-131`
- Evidencia: `grep -nE "AP-CU68|AP-CU60" apps/movil/lib/dominio/errores.dart` → sin resultados; el archivo solo tiene `'AP-CU69-10'` del módulo.
- Cómo se comprobó: búsqueda citada; las pantallas consumidoras las listó el subagente de frontend y se abrió `errores.dart`.
- Impacto: rechazos de negocio de admisión y sorteo (`AP-CU68-*`, `AP-CU60-*`) se muestran con texto genérico; la persona no sabe qué hacer (regla 95.2.3).
- Causa de fondo: el catálogo de errores del cliente se escribe a mano y no se genera del contrato (que sí lista los códigos, p. ej. `grupos.yaml:886,1167`).
- Solución de fondo: generar el catálogo desde la tabla de errores del contrato.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: app móvil.
- Estándares y normas: ISO/IEC 25010 usabilidad (protección frente a errores); RFC 9457; regla 95.2.3.
- Esfuerzo estimado: chico

### grupos-organizador-DOC-001 — Tres superficies dicen tres cosas distintas sobre quién decide la admisión, y ninguna coincide con R-NUEVA-1
- Clase / Severidad / Confianza: DOC / S3 / confirmado
- Ubicaciones: `apps/movil/lib/pantallas/pasanaku/textos.dart:35-39`; `apps/web/src/app/paginas/invitacion/invitacion.ts:53`; `apps/backoffice/src/app/rutas/admisiones/textos.ts:4-5`
- Evidencia: app «Decide el organizador»; web «Una persona del equipo de AportaYa revisa tu solicitud y te avisa.»; backoffice «El administrador propone y el algoritmo solo recomienda: la decisión final es de backoffice.»
- Cómo se comprobó: `grep` de cada texto en su archivo.
- Impacto: el participante recibe promesas contradictorias sobre el mismo trámite; con R-NUEVA-1 las tres quedan obsoletas.
- Causa de fondo: la decisión de admisión (A-1) nunca se tomó; cada superficie escribió su supuesto.
- Solución de fondo: después de A-1, un solo texto de producto para el trámite, en el catálogo de textos compartido.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: app, web, backoffice.
- Estándares y normas: ISO/IEC 25010 usabilidad (reconocibilidad de la adecuación); regla 95.2.
- Esfuerzo estimado: chico

### grupos-organizador-FRAGIL-002 — El estado aceptado de una solicitud tiene dos nombres (`APROBADA`/`ACEPTADA`) en el mismo contrato
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/openapi/grupos.yaml:1703,1720`; `sql/10_tablas/02_grupos_turnos/solicitud_ingreso.sql:21`; `servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/BandejaDeAdmisiones.java:35,61`; `apps/backoffice/src/app/rutas/admisiones/pantalla-de-admision.ts:52`; `apps/backoffice/src/app/rutas/admisiones/entrada-de-admision.ts:152`
- Evidencia:
  ```yaml
  SolicitudDeIngreso:  estado: { enum: [PENDIENTE, APROBADA, RECHAZADA, EXPIRADA] }
  SolicitudDeAdmision: estado: { enum: [PENDIENTE, ACEPTADA, RECHAZADA, EXPIRADA] }
  ```
  ```java
  String enBase = estado == null ? null : "ACEPTADA".equals(estado) ? "APROBADA" : estado;
  ```
- Cómo se comprobó: lectura; además el filtro del backoffice ofrece `'ESCALADA'` (`entrada-de-admision.ts:152`), que no está en ningún enum del contrato ni en el `CHECK` de la tabla: ese filtro siempre devuelve vacío.
- Impacto: si alguien agrega un estado o cambia la traducción de `BandejaDeAdmisiones`, la bandeja o la pantalla de admisión muestran estados falsos. El backoffice ya compara contra los dos nombres. Ninguna prueba de contrato lo detecta (no hay pacto para estas operaciones).
- Causa de fondo: lenguaje ubicuo roto entre esquema y contrato; la traducción vive en un caso de uso.
- Solución de fondo: un solo nombre de estado en esquema, contrato y clientes, con versión del contrato si cambia.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: backoffice.
- Estándares y normas: ISO/IEC 25010 mantenibilidad (modificabilidad); reglas 96.4.1, 98.1.3.
- Esfuerzo estimado: chico

## 6. Matriz de contratos

(pendiente)

## 7. Evidencia obligatoria del módulo

(pendiente)

## 8. Cobertura por clase

(pendiente)

## 9. Lo que está bien

(pendiente)

## 10. Observado fuera de alcance

- `scripts/auditar_backend.py` informa «`grupos`: `canjearInvitacion`, `revocarInvitacionDelGrupo`, `listarBandejaDeAdmisiones`, `listarGruposDeBackoffice`, `consultarGrupoDeBackoffice`, `proponerAdmision`, `resolverAdmision` declarada sin implementar» (`evidencia/auditar_backend.txt`). Es un falso positivo: las siete están implementadas en `servicios/grupos/src/main/java/bo/aportaya/grupos/web/{RutasDeInvitacion,RutasDeSolicitudes,RutasDelGrupo,RutasDeAdmision}.java`, y `scripts/inventario_endpoints.py --check` dice «0 hallazgos».
- `servicios/publicidad/src/main/java/bo/aportaya/publicidad/infraestructura/CreativaRepositorio.java:89` y `.../aplicacion/CU110AltaDeAnunciante.java:158` consultan el esquema `organizador`, una «divergencia declarada» de `auditar_backend.py`. Viola la regla 98.1.1 desde publicidad (lote L5); en este lote se registra como conexión entrante de organizador (sección 3).

## 11. Límites de esta auditoría

- Gradle no corrió: `./gradlew :servicios:grupos:test` → «Received status code 429 from server: Too Many Requests» (Maven Central) y `BUILD FAILED` (`evidencia/gradle_grupos_test.txt`). Todo lo que exige compilar o ejecutar pruebas Java queda NO COMPROBADO, incluidos el barrido de tamaño, el de umbrales y el de secretos de Gradle. El tamaño de archivos se midió a mano: ningún `.java` de main supera 300 líneas (`evidencia/barrido_tamano_manual.txt`).
- `yarn typecheck`/`yarn lint` no corrieron: el repo pide `yarn@4.18.0`, el entorno tiene 1.22.22 y no hay `node_modules` (`evidencia/yarn_typecheck.txt`). Flutter no está instalado.
- No se usó base de datos ni Docker: nada se ejecutó contra TEST, producción ni entornos compartidos.

## 12. Anexo de cobertura de archivos

(pendiente)
