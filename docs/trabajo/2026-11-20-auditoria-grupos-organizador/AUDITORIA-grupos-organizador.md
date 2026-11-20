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
| APR-01 | `servicios/grupos/src/main/resources/application.yml:79-84`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:141-150`; `.../web/InvitacionesWeb.java:127-133`; `.../web/RutasDeAdmision.java:25-37` | Ingreso de un integrante: el organizador **propone** y backoffice (`ADMIN_PLATAFORMA`) **resuelve** | Solicitud `PENDIENTE` → decisión `PROPUESTA` → `RESOLUCION`; `POST /grupos/solicitudes/{id}/propuestas` (`GRUPO_ADMINISTRAR`), `POST /grupos/solicitudes/{id}/resoluciones` (`ADMIN_PLATAFORMA`); pantallas backoffice `rutas/admisiones`, `rutas/operacion/solicitudes-escaladas`; eventos `aportaya.grupos.admision_propuesta`, `aportaya.grupos.admision_resuelta` | ELIMINABLE desde producto (el propio `application.yml:79` la declara «DECISION PENDIENTE (negocio y cumplimiento)»); queda DECISIÓN REQUERIDA solo si Cumplimiento la exige (A-1) | Las puertas «aceptar por enlace» y «decisión directa del organizador» hoy devuelven 422 (`CodigoError 69-10` y `68-15`) con el interruptor encendido; apagarlo reabre esos caminos, que crean participante sin expediente; la bandeja de backoffice y sus pruebas quedan sin uso |
| APR-02 | `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDelGrupo.java:94-95`; `.../aplicacion/CU69IniciarGrupo.java:89-91`; `.../web/GruposController.java:164-173` | Inicio del grupo y compromiso/revelación del sorteo: los dispara el administrador del grupo | `ABIERTO_A_INSCRIPCION → CONFORMADO`; `POST /grupos/{id}/inicio`, `POST /grupos/{id}/sorteo`, `POST /grupos/{id}/sorteo/revelacion`, todos `GRUPO_ADMINISTRAR`; app `inicio_del_grupo.dart`, `pantalla_sorteo_*`; eventos `grupo_iniciado`, `sorteo_comprometido`, `sorteo_revelado` | ELIMINABLE (ADR-054 D3 ya decidió que sean automáticos) | Sin el disparo manual hoy no hay ningún otro camino (0 `@Scheduled`): quitarlo sin el trabajo programado deja todo grupo detenido |

### 4b. Inventario de respaldo y cobertura

(pendiente)

### 4c. Ambigüedades A-1 a A-4

(pendiente)

## 5. Hallazgos

### grupos-organizador-APROB-001 — La admisión humana de backoffice está encendida por omisión y bloquea todo ingreso sin aprobación
- Clase / Severidad / Confianza: APROB / S2 / confirmado
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:79-84`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:30-31,141-150`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/InvitacionesWeb.java:55-56,127-133`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeAdmision.java:25-37`
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
- Ubicaciones: `servicios/grupos/src/main/resources/application.yml:84`; `servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeSolicitudes.java:30-31`; `docs/Arquitectura/ADR-054 Grupos sin organizador humano.md:58-59,76,79,93`
- Evidencia: ADR-054 D2 «El enlace es la admisión … Nadie aprueba a nadie»; D5 «El interruptor pasa de global a **por `tipo_conformacion`**». En el código, `@Value("${aportaya.grupos.admision.humana-obligatoria}") private boolean admisionHumanaObligatoria;` es un único booleano.
- Cómo se comprobó: `grep -rn 'tipo_conformacion' servicios/grupos/src/main` → 0 usos en lógica de admisión.
- Impacto: tres fuentes dicen tres cosas sobre la admisión: ADR-054 (sin aprobación en el grupo de confianza), `application.yml` (aprobación humana en todos) y R-NUEVA-1 (sin aprobación en ninguno). Manda el código, que es la más restrictiva.
- Causa de fondo: decisiones de producto aceptadas sin el cambio de código que las materializa.
- Solución de fondo: Pablo confirma R-NUEVA-1 sobre ADR-054 D5 (A-1). Después se actualiza el ADR y el interruptor desaparece.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: ninguno adicional a APROB-001.
- Estándares y normas: ISO/IEC 25010 corrección funcional; regla 00 §8 (prioridad de evidencia).
- Esfuerzo estimado: chico (si se decide quitar)

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
