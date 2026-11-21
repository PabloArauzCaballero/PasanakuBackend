# Anexo L4 — Correcciones por hallazgo

Fuente: informe de auditoría L4 identidad-cumplimiento (`origin/test` `07fc6971`, 176 fichas `IDCU-*`).
Decisiones aplicadas: `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md` (2026-11-21).
Peldaño de este anexo: `DISCOVERED` (regla 30). Nada de lo que sigue está escrito ni probado: son microtareas.

**Abreviaturas de ruta** (todas relativas a la raíz del monorepo):
`ID/` = `servicios/identidad/src/main/java/bo/aportaya/identidad/` ·
`CU/` = `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/` ·
`NF/` = `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/` ·
`MOV/` = `apps/movil/lib/pantallas/identidad/` ·
`BO/` = `apps/backoffice/src/app/rutas/` ·
`SQL/` = `sql/`.

**Lo que se conserva por norma y se vuelve real** (nota del encargo): los cuatro ojos de la diligencia PEP
(F1.L4.M05), el caso LFT analista ≠ revisor (F1.L4.M06), la segregación del reporte UIF (F1.L4.M07,
F4.L4.M55, F4.L4.M68) y la atención de requerimientos de autoridad (F4.L4.M52, F4.L4.M73). En todos, la
identidad de cada actor sale del token de su propia sesión, nunca del cuerpo de la petición.

## Resumen

**Microtareas por fase:** F0 = 5 · F1 = 26 · F2 = 2 · F3 = 12 · F4 = 89 · F5 = 0. Total = 134.

**Hallazgos por fase y severidad** (cada ficha cuenta una sola vez, en la microtarea donde se traza):

| Fase | S1 | S2 | S3 | S4 | Total |
|---|---|---|---|---|---|
| F0 | 0 | 0 | 3 | 0 | 3 |
| F1 | 3 | 20 | 11 | 2 | 36 |
| F2 | 0 | 1 | 4 | 2 | 7 |
| F3 | 0 | 13 | 6 | 1 | 20 |
| F4 | 0 | 27 | 57 | 26 | 110 |
| F5 | 0 | 0 | 0 | 0 | 0 |
| **Total** | **3** | **61** | **81** | **31** | **176** |

**Hallazgos «se resuelve con»** (el arreglo es consecuencia de otra microtarea; igual tienen fila):
- IDCU-DOC-006, IDCU-CONTRATO-057, IDCU-APROB-060, IDCU-PRUEBAS-073, IDCU-DINERO-146 → se resuelven con F2.L4.M01 (se retira la doble aprobación de retiros, P-09).
- IDCU-SEG-109, IDCU-APROB-159 → se resuelven con F2.L4.M02 (se retira el organizador profesional, P-11).
- IDCU-ACOPLE-002 e IDCU-DEFECTO-003 → se resuelven con F4.L4.M01 (al retirar la copia muerta de CU-40).
- IDCU-CLEAN-133 → se resuelve con F1.L4.M06 (al hacer real la separación analista ≠ revisor se borra la versión insegura).
- IDCU-HUECO-068 → se resuelve con F1.L4.M05 (mismo CHECK de los cuatro ojos PEP).
- IDCU-UX-A11Y-173 → se resuelve con F1.L4.M04 (el camino de enrolamiento existe).
- IDCU-RESILIENCIA-062 → se resuelve con F3.L4.M07 (despacho único del retiro).
- IDCU-DEFECTO-029 → se resuelve con F3.L4.M03 (KYC automático con aviso; ya no se sondea una revisión humana).
- Partes de otras fichas que desaparecen con el retiro del organizador (F2.L4.M02): la porción de organizador de IDCU-TRAZA-087, IDCU-RESILIENCIA-108, IDCU-FRAGIL-101, IDCU-DINERO-150, IDCU-FRAGIL-160, IDCU-DOC-162 e IDCU-SEG-169. Esas fichas se trazan a su microtarea principal, que arregla el resto.

**Hallazgos «sin acción»:** ninguno. Las decisiones de Pablo cambian el *cómo* de varias fichas
(KYC por proveedor, retiros automáticos, organizador eliminado), pero ninguna convierte un hallazgo en correcto.
En particular IDCU-RESPALDO-007 no queda «correcto» con el modelo A: el texto actual habla de un fondo
genérico que «cubre», y el modelo A exige decir que el fondo es **del grupo**, en cuenta segregada, y que
**vuelve** al cierre; se reescribe (F3.L4.M11).

**Ambigüedades que este anexo deja registradas** (no se resuelven por conveniencia, regla 00 §1.7):
- AMB-L4-1 — Bloqueos y retenciones automáticas LFT sin plazo de descargo (IDCU-DEBIDO-PROCESO-066): ¿excepción normativa al 91.4? Confirmar con Cumplimiento (F0.L4.M03).
- AMB-L4-2 — Tipos de documento admitidos en el alta (IDCU-HUECO-019): ¿solo CI boliviano o también CEX y pasaporte? Confirmar con Pablo y con lo que soporte el proveedor KYC.
- AMB-L4-3 — Parámetros normativos (umbrales UIF, plazos LFT, plazos de incidente, límites de retiro por ventana, enfriamiento): configurables y obligatorios sin valor por defecto (DECISIONES, fila «Parámetros financieros»); los valores los fijan Pablo y Cumplimiento.
- AMB-L4-4 — Aplicabilidad a una ETF del «Instructivo EIF» citado en las semillas UIF (IDCU-REG-046): confirmar con Cumplimiento/Legal.

## F0 — Preparación

### F0.L4.M01 — ADR que reemplaza a ADR-049: retiros automáticos con controles
- Hallazgos: ninguno propio (prerrequisito de F2.L4.M01, F3.L4.M07 y F3.L4.M08; contexto en IDCU-APROB-043, IDCU-FLUJO-FALTANTE-036)
- Causa de raíz: ADR-049 fija la doble aprobación humana por operación; P-09 la reemplaza por controles automáticos.
- Corrección de raíz: nuevo ADR en `docs/Arquitectura/` que declara ADR-049 «reemplazado»: segundo factor ligado a la operación, cuenta destino verificada, enfriamiento tras cambio de cuenta, límites por ventana con moneda (parámetros obligatorios sin valor por defecto), monitoreo UIF; Cumplimiento revisa solo lo que dispara alerta. Cita `docs/Cumplimiento.md:170-176` (la norma pide niveles de autorización aprobados, no aprobación por operación).
- CA: Dado el repositorio, cuando se lee ADR-049, entonces su estado es «Reemplazado por ADR-0NN» y el ADR nuevo enumera los cinco controles y quién revisa las alertas.
- DoD: `grep -n "Reemplazado" "docs/Arquitectura/ADR-049 Doble aprobación de retiros.md"` → 1 línea; PR revisado y aprobado por Pablo.
- Depende de: P-09.
- Esfuerzo: chico

### F0.L4.M02 — ADR del KYC automático por proveedor con revisión humana solo de excepciones y PEP
- Hallazgos: ninguno propio (prerrequisito de F3.L4.M01–M06; contexto en IDCU-APROB-008 y APR-01/APR-12 del informe)
- Causa de raíz: `identidad.yaml:350-352,458` declara «la aprobación sigue siendo manual» y toda la compuerta de dinero depende de una persona.
- Corrección de raíz: ADR que define el puerto `VerificadorDeIdentidad` (proveedor), los desenlaces (APROBADA, RECHAZADA, EXCEPCION, PEP), qué cae en la cola de Cumplimiento, el evento `identidad.verificacion_resuelta` y el actor de sistema que asigna el rol PARTICIPANTE. Mantiene la diligencia PEP con cuatro ojos (`docs/Cumplimiento.md` §2.3).
- CA: Dado el ADR, cuando se busca qué casos revisa una persona, entonces solo figuran EXCEPCION y PEP.
- DoD: ADR en `docs/Arquitectura/` revisado por Pablo; `grep -n "manual" servicios/identidad/src/main/resources/openapi/identidad.yaml` actualizado en F3.L4.M02.
- Depende de: P-08.
- Esfuerzo: chico

### F0.L4.M03 — Registrar la decisión de Cumplimiento sobre bloqueos y retenciones LFT automáticas
- Hallazgos: IDCU-DEBIDO-PROCESO-066 (S3)
- Causa de raíz: las reglas LFT sembradas (`SQL/10_tablas/12_cumplimiento_asfi/regla_monitoreo_lft.sql:22`, `SQL/60_semillas/08-gobierno-y-licencia.sql:39`, `19-reportes-y-retencion.sql:61-64`) ordenan `RETENER_OPERACION`/`BLOQUEAR_*` sin plazo de descargo, y la posible excepción normativa al 91.4 no está registrada.
- Corrección de raíz: ADR o decisión registrada (AMB-L4-1) que diga, con norma citada, qué acciones LFT son medidas cautelares inmediatas (sin descargo previo, con revisión humana posterior obligatoria y plazo) y cuáles pasan por expediente. Las semillas se ajustan en F4.L4.M25 según lo decidido.
- CA: Dado el catálogo de acciones LFT, cuando se consulta cada acción automática, entonces tiene una decisión registrada que la habilita o la manda a expediente.
- DoD: documento en `docs/Arquitectura/` firmado por Pablo y Cumplimiento; tabla acción → decisión sin filas vacías.
- Depende de: Cumplimiento (externa).
- Esfuerzo: chico

### F0.L4.M04 — Camino de migraciones y verificación de deriva modelo ↔ base ↔ jOOQ
- Hallazgos: IDCU-ARQ-070 (S3)
- Causa de raíz: ADR-032 quedó a medias: no existe `sql/migraciones/`, cada aplicación hace DROP/ADD de todas las FK (`SQL/20_claves/*.sql:6-10`) y no hay chequeo de deriva (`SQL/50_verificacion/verificaciones.sql`).
- Corrección de raíz: completar ADR-032: directorio de migraciones versionadas generadas desde el `.puml`, índices `CONCURRENTLY` con nombre, FK `NOT VALID` + `VALIDATE`, y un script de deriva que compare modelo, catálogo de la base y clases jOOQ (tabla/columna ausente, NOT NULL no mapeada, nulabilidad divergente). Es prerrequisito de todas las microtareas de esquema de este anexo.
- CA: Dada una columna agregada al modelo y no aplicada, cuando corre el chequeo de deriva, entonces falla con el nombre de la columna.
- DoD: ejecutar el chequeo contra la base de integración (Docker en la máquina de Pablo) en verde y luego con una deriva provocada en rojo; pegar ambas salidas.
- Depende de: entorno Docker de F0 (P-19).
- Esfuerzo: grande

### F0.L4.M05 — Pactos base de identidad y cumplimiento con verificación del productor
- Hallazgos: IDCU-PRUEBAS-058 (S3)
- Causa de raíz: `contratos/pactos/identidad-grupos-pertenencia.json` tiene solo dos 200 y nadie lo verifica contra el proceso real; no hay pactos para nivel KYC, step-up ni alcance de licencia.
- Corrección de raíz: verificación del productor en el CI de identidad; pactos con los tres niveles (correcto, límite, inválido, regla 65) para `identidad→grupos` (pertenencia), `identidad→nucleo-financiero` (nivel KYC, evidencia step-up) y `cumplimiento→grupos` (alcance de licencia).
- CA: Dado un cambio que quita un campo del contrato de pertenencia, cuando corre la verificación del productor, entonces falla.
- DoD: correr la verificación del productor en verde; romper un campo a propósito y pegar el rojo.
- Depende de: ninguna.
- Esfuerzo: medio

## F1 — Seguridad e integridad del mayor

### F1.L4.M01 — Regla única «puede autenticarse» en login y refresco; un solo modelo de bloqueo
- Hallazgos: IDCU-SEG-075 (S1), IDCU-SEG-089 (S2)
- Causa de raíz: el estado de cuenta y el bloqueo de acceso viven en tres modelos (`usuario.estado`, `bloqueo_cuenta`, `usuario_interno.bloqueado_hasta`) y `CU04Autenticar`/`RenovarSesion` solo miran `bloqueo_cuenta`; `UsuarioRepositorio.estadoDe` no tiene llamador; desbloquear un interno limpia un campo que el login no lee.
- Corrección de raíz: átomo de dominio `PuedeAutenticarse(estado, bloqueosVigentes)` en `ID/dominio/`, invocado en `ID/aplicacion/CU04Autenticar.java` y `ID/aplicacion/RenovarSesion.java` (el refresco de una cuenta SUSPENDIDA/BAJA revoca la familia). Retirar `usuario_interno.bloqueado_hasta` y que suspender/desbloquear (`ID/infraestructura/UsuariosInternosRepositorio.java:221-268`) escriba el mismo modelo que lee el login. Así no hay dos verdades sobre quién entra.
- CA: Dada una cuenta suspendida, cuando intenta iniciar sesión o refrescar, entonces recibe el error de cuenta no habilitada y no obtiene token; dado un interno desbloqueado, cuando ingresa, entonces entra.
- DoD: prueba de integración contra base real «suspendido no entra», «baja no entra», «suspendido no refresca», «desbloqueado entra»; `grep -rn "bloqueado_hasta" servicios/identidad sql` → 0.
- Depende de: F0.L4.M04 (cambio de esquema).
- Esfuerzo: medio

### F1.L4.M02 — Segundo factor real (TOTP) con enrolamiento, y prohibir el factor fijo fuera de entornos descartables
- Hallazgos: IDCU-HUECO-077 (S1)
- Causa de raíz: el adaptador de desarrollo `ID/infraestructura/DesafioDeDesarrollo.java` (`CODIGO = "000000"`) quedó como implementación por omisión en TEST y nunca se construyó el enrolamiento.
- Corrección de raíz: adaptador TOTP (RFC 6238) que lee el secreto cifrado de `factor_mfa`; endpoints de enrolamiento, confirmación y códigos de respaldo en `identidad.yaml`; `DesafioDeDesarrollo` solo activo con un perfil explícitamente descartable y fallo de arranque si ese perfil aparece junto a una base con datos (ver F1.L4.M03).
- CA: Dado un operador con TOTP enrolado, cuando presenta `000000` u otro código inválido, entonces el factor se rechaza; cuando presenta el código vigente, entonces se acepta.
- DoD: unitaria con vectores de RFC 6238; integración «código inválido rechazado», «código reutilizado rechazado», «sin enrolamiento → AP-CU04-06»; arranque con perfil de desarrollo y base de integración → falla de arranque (pegar salida).
- Depende de: F1.L4.M03.
- Esfuerzo: grande

### F1.L4.M03 — Arranque fail-closed: secretos y modo de desarrollo atados a «entorno descartable», no a nombres de perfil
- Hallazgos: IDCU-SEG-004 (S2), IDCU-CONFIG-100 (S3)
- Causa de raíz: `ID/infraestructura/EmisorDeAcceso.java:73-91` falla cerrado solo con `staging`/`production`; TEST corre `local` (`despliegue/TEST.md:86-88`); `SecretoDeInvitacion` valida la clave en el primer uso; pools incoherentes.
- Corrección de raíz: invertir la lógica (todo falla salvo un perfil `descartable` explícito); `@ConfigurationProperties @Validated` para `JWT_CLAVE_FIRMA`, `INVITACIONES_CLAVE` y el pool (alineado con `descriptor.yml`); `${JWT_CLAVE_FIRMA:?}` en `docker-compose.coolify.yml:448`; perfil propio para TEST sin factor fijo.
- CA: Dado un arranque sin `JWT_CLAVE_FIRMA` o sin `INVITACIONES_CLAVE` y sin perfil descartable, cuando arranca el servicio, entonces termina con error de configuración antes de aceptar tráfico.
- DoD: prueba de arranque (contexto Spring) con cada variable faltante → falla; con todas → arranca; `docker compose config` sin la variable → error.
- Depende de: ninguna.
- Esfuerzo: chico

### F1.L4.M04 — Alta de operador por invitación de un solo uso: fijar contraseña y enrolar el segundo factor
- Hallazgos: IDCU-HUECO-149 (S2), IDCU-UX-A11Y-173 (S4)
- Causa de raíz: el alta de usuario interno genera una contraseña provisional que se hashea y se descarta (`ID/infraestructura/UsuariosInternosRepositorio.java:181-183`) y no hay primer ingreso ni enrolamiento; el backoffice muestra AP-CU04-06 sin camino.
- Corrección de raíz: invitación con token de propósito único (hash con pepper, vencimiento) en `token_verificacion`; endpoint y pantalla de primer ingreso (`BO/ingreso/`) que fija la contraseña y enrola TOTP; borrar `generarContrasenaProvisional`; el error AP-CU04-06 lleva al enrolamiento.
- CA: Dado un operador recién dado de alta, cuando abre su enlace, entonces fija contraseña y enrola TOTP; cuando reutiliza el enlace, entonces recibe «enlace usado».
- DoD: integración «token de un solo uso», «token vencido», «token de otro propósito»; prueba de UI del primer ingreso; captura en tres anchos (regla 95.7).
- Depende de: F1.L4.M02.
- Esfuerzo: medio

### F1.L4.M05 — Diligencia PEP con cuatro ojos reales (dos actos, dos sesiones)
- Hallazgos: IDCU-SEG-119 (S1), IDCU-HUECO-068 (S3)
- Causa de raíz: la aprobación está modelada como dato de entrada: `CU/web/AdhesionController.java:155-166` toma `aprobadaPor` y `segundaRevisionPor` del cuerpo; R-UIF-10 (`SQL/40_reglas/restricciones.sql:1110-1116`) acepta NULL.
- Corrección de raíz: dos operaciones (`aprobar`, `segundaRevision`) cuyo actor es `ctx.usuarioId()`; estado intermedio `PENDIENTE_SEGUNDA_REVISION`; CHECK `aprobada_por IS NOT NULL AND segunda_revision_por IS NOT NULL AND segunda_revision_por <> aprobada_por` para PEP COMPLETA; el contrato (versión nueva de `cumplimiento.yaml:1220-1234`) rechaza esos campos en el cuerpo con 400, como CU-43.
- CA: Dado un analista, cuando aprueba y luego intenta la segunda revisión con su propia sesión, entonces recibe 409/422 y el nivel no sube; dado otro analista, cuando hace la segunda revisión, entonces el nivel sube.
- DoD: integración con base real y `SET ROLE svc_cumplimiento`: «mismo actor rechazado», «cuerpo con aprobadaPor → 400», «sin segunda revisión no sube topes»; prueba SQL directa del CHECK con NULL → error.
- Depende de: F0.L4.M04.
- Esfuerzo: medio

### F1.L4.M06 — Caso LFT: el analista es quien actúa, y decide otra persona con otro permiso
- Hallazgos: IDCU-SEG-120 (S2), IDCU-CLEAN-133 (S3)
- Causa de raíz: `analista_id` es un UUID libre del cuerpo (`CU/web/CumplimientoController.java:78-85`) y abrir y decidir usan `CUMPLIMIENTO_CASOS`; además sigue viva la versión insegura `CU/aplicacion/CU44InvestigarYReportar.java:145-225`.
- Corrección de raíz: el analista es `ctx.usuarioId()` al tomar el caso (o se valida contra identidad con rol ANALISTA); estado `EN_REVISION` con informe registrado por él; la decisión exige permiso distinto (`CUMPLIMIENTO_CASOS_DECIDIR`) y actor ≠ analista; borrar `CU44InvestigarYReportar.decidir` y reescribir sus pruebas contra `CU44DecidirComoRevisor`.
- CA: Dado un caso tomado por A, cuando A intenta decidirlo, entonces recibe 403/422; cuando B (con permiso de decisión) decide, entonces se registra B.
- DoD: autorización negativa (mismo actor, sin permiso, sin token); `grep -n "decidir(" CU/aplicacion/CU44InvestigarYReportar.java` → 0.
- Depende de: ninguna.
- Esfuerzo: medio

### F1.L4.M07 — Segregación del reporte UIF sin hueco por NULL
- Hallazgos: IDCU-DEFECTO-039 (S2)
- Causa de raíz: `ck_reporte_segregacion` (`SQL/40_reglas/restricciones.sql:1512-1516`) evalúa NULL como aceptado y `generado_por` es `ON DELETE SET NULL` (`SQL/20_claves/12_cumplimiento_asfi.sql:485`); no exige `revisado_por`.
- Corrección de raíz: `generado_por` obligatorio (actor de sistema explícito cuando lo genera un trabajo); CHECK para ENVIADO: `revisado_por` y `aprobado_por` no nulos y distintos de `generado_por` y entre sí; FK sin `SET NULL`. Los actores salen del token en CU-43.
- CA: Dado un reporte con `generado_por` nulo o con aprobador igual al generador, cuando se intenta pasar a ENVIADO, entonces la base lo rechaza.
- DoD: prueba SQL directa con los tres casos (nulo, igual, revisado faltante) → violación del CHECK con su nombre.
- Depende de: F0.L4.M04.
- Esfuerzo: chico

### F1.L4.M08 — Políticas de fila separadas por operación: el titular lee, no escribe, sus roles, KYC, reputación y PEP
- Hallazgos: IDCU-SEG-038 (S2)
- Causa de raíz: el generador de políticas (`SQL/40_reglas/restricciones.sql:1362-1474`) decide el régimen por lista de visibilidad: `cond_escritura := cond` da escritura al titular.
- Corrección de raíz: políticas `FOR SELECT` y `FOR INSERT/UPDATE/DELETE` separadas; mover `asignacion_rol`, `verificacion_kyc`, `reputacion_usuario`, `perfil_financiero`, `factor_mfa`, `token_verificacion`, `aceptacion_contrato` a `lectura_por_titular`; en `usuario`, `GRANT UPDATE (col)` solo de columnas editables por el titular.
- CA: Dada una sesión de participante, cuando intenta `UPDATE verificacion_kyc SET estado='APROBADA'` sobre la suya, entonces RLS lo rechaza.
- DoD: prueba de integración con `SET ROLE` del servicio y `app.rol=PARTICIPANTE`, un caso por tabla (escritura rechazada, lectura permitida).
- Depende de: F0.L4.M04.
- Esfuerzo: medio

### F1.L4.M09 — Rol de sesión derivado del ámbito real; vigencia de la asignación respetada
- Hallazgos: IDCU-SEG-080 (S2)
- Causa de raíz: todo rol GLOBAL recibe `rol=BACKOFFICE` (`ID/web/SesionesController.java:55,201`) y `fn_seg_rol_privilegiado()` lo trata como privilegiado; `perfilDe` no filtra `vigente_hasta`.
- Corrección de raíz: `app.rol` derivado del rol/departamento real (o política por permiso en vez de por rol de sesión); `fn_seg_rol_privilegiado` solo para roles con acceso de datos justificado; `UsuarioRepositorio.perfilDe` con la misma vigencia que `PermisosEfectivos`.
- CA: Dado un usuario con rol `ANUNCIANTE`, cuando consulta datos de otro participante vía RLS, entonces no los ve; dado un rol vencido, cuando inicia sesión, entonces no lo lleva en el token.
- DoD: integración con RLS real: matriz rol → tabla sensible (permitido/denegado); prueba de vigencia.
- Depende de: F1.L4.M08.
- Esfuerzo: medio

### F1.L4.M10 — Personal interno acotado a `usuario_interno`, roles asignables en lista blanca y una sola implementación de asignación
- Hallazgos: IDCU-SEG-076 (S2), IDCU-DUP-INT-103 (S3)
- Causa de raíz: `PATCH /usuarios-internos/{id}` resuelve cualquier `usuario.id` (`ID/infraestructura/UsuariosInternosRepositorio.java:200-255`) y permite `estado=ACTIVO` sin KYC; la ruta expuesta usa `RotacionDeRolesInternos` (sin ámbito, SoD parcial) y no `CU08AsignarRol`.
- Corrección de raíz: JOIN obligatorio con `usuario_interno`; quitar `estado` del contrato de actualización (la activación de participantes solo la hace el KYC, F3.L4.M02; la suspensión, el caso de uso dueño); lista blanca de roles asignables por quien asigna; borrar `RotacionDeRolesInternos` y exponer `CU08AsignarRol`.
- CA: Dado un administrador, cuando hace PATCH sobre el id de un participante no interno, entonces recibe 404; cuando asigna un rol fuera de su lista blanca, entonces 403.
- DoD: autorización negativa (participante como objetivo, rol no permitido, sin permiso, sin token); `grep -rn RotacionDeRolesInternos servicios/identidad` → 0.
- Depende de: F1.L4.M11.
- Esfuerzo: medio

### F1.L4.M11 — Pares de funciones incompatibles como dato de catálogo con FK a `permiso`
- Hallazgos: IDCU-DEFECTO-079 (S2)
- Causa de raíz: `ID/dominio/SegregacionDeFunciones.java:16-22` compara códigos escritos a mano (`ENTREGAS_AUTORIZAR`, `REVERSOS_AUTORIZAR`) que la semilla no tiene; la prueba usa permisos sintéticos.
- Corrección de raíz: tabla `par_incompatible(permiso_a, permiso_b)` con FK a `permiso`, sembrada; `SegregacionDeFunciones` lee el catálogo. Incluye los pares que exigen los cuatro ojos que siguen vivos (P-13: reembolsos, reversos y cierre de billetera; PEP; LFT; UIF). Los pares de entrega y de aprobación de retiro desaparecen con F2.
- CA: Dado un usuario con `REVERSO_AUTORIZAR`, cuando se le asigna el permiso incompatible, entonces la asignación se rechaza.
- DoD: integración contra la semilla real: cada par de la tabla produce rechazo; FK impide un código inexistente.
- Depende de: F0.L4.M04.
- Esfuerzo: chico

### F1.L4.M12 — OTP de correo: el reintento sobre un código consumido cuenta intentos y no devuelve el borrador
- Hallazgos: IDCU-SEG-081 (S2)
- Causa de raíz: la rama de «reintento idempotente» (`ID/infraestructura/VerificacionCorreoRepositorio.java:183-187`) no comparte el contador y devuelve el borrador con PII.
- Corrección de raíz: contar intentos también sobre CONSUMIDO; tras el consumo, el borrador se entrega una sola vez con un token de un solo uso, nunca por reintento del OTP.
- CA: Dado un OTP consumido, cuando se prueban N+1 códigos, entonces la ruta bloquea y no devuelve el borrador.
- DoD: integración «fuerza bruta sobre consumido → bloqueo», «reintento legítimo → misma respuesta sin PII».
- Depende de: ninguna.
- Esfuerzo: chico

### F1.L4.M13 — Sin enumeración de cuentas en rutas públicas ni por tiempo del login
- Hallazgos: IDCU-SEG-091 (S2)
- Causa de raíz: el alta y la verificación de correo responden con el `campo` que ya existe; el login no ejecuta Argon2 si el usuario no existe.
- Corrección de raíz: respuesta uniforme en `SolicitarVerificacionCorreo` y `CU01RegistrarUsuario` con aviso al dueño por el canal verificado; hash señuelo en `CU04Autenticar` para usuario inexistente.
- CA: Dado un teléfono ya registrado y otro libre, cuando se intenta el alta con cada uno, entonces las respuestas HTTP son idénticas.
- DoD: integración comparando cuerpo y estado; medición del tiempo de login existente vs inexistente con diferencia dentro de un margen declarado (pegar medición).
- Depende de: ninguna.
- Esfuerzo: medio

### F1.L4.M14 — Controles de seguridad sin carreras (lectura y luego escritura)
- Hallazgos: IDCU-FRAGIL-101 (S3)
- Causa de raíz: conteo de intentos fallidos, «último administrador» y código público se resuelven con SELECT-then-INSERT sin bloqueo ni unicidad.
- Corrección de raíz: bloqueo por usuario (`FOR UPDATE` o advisory) en `AccesoRepositorio`/`CU04Autenticar`; precondición en la escritura para el último administrador (`CU08RevocarRol`); código público más largo con índice UNIQUE y reintento acotado. La porción de `FirmaDeOrganizadorRepositorio` desaparece con F2.L4.M02.
- CA: Dadas N+1 peticiones paralelas con clave errónea, cuando terminan, entonces la cuenta queda bloqueada tras exactamente el tope.
- DoD: prueba de concurrencia real (hilos paralelos contra base real) para el tope de fallidos y para el último administrador.
- Depende de: F1.L4.M01.
- Esfuerzo: medio

### F1.L4.M15 — Token de acceso con `iss`/`aud` validados, refresco con HMAC y revocación verificada en operaciones de dinero
- Hallazgos: IDCU-SEG-005 (S3), IDCU-SEG-099 (S3)
- Causa de raíz: `EmisorDeAcceso.java:112-121` no emite `iss`/`aud` y el decodificador compartido (`plataforma/comun-web/.../ConfiguracionDelDecodificador.java`) no los valida; el refresh se guarda con SHA-256 sin pimienta; el acceso sigue 15 min tras logout/suspensión.
- Corrección de raíz: emitir y validar `iss` y `aud` por servicio; refresh con HMAC y pepper (90.2.9); `sesion_id` en el token y verificación de sesión vigente en operaciones de dinero (retiro, transferencia).
- CA: Dado un token con `aud` de otro servicio, cuando llega, entonces 401; dada una sesión cerrada, cuando su token intenta un retiro, entonces 401.
- DoD: pruebas del decodificador (iss/aud faltante o ajeno); integración «logout → retiro rechazado»; pacto actualizado (F0.L4.M05).
- Depende de: F0.L4.M05.
- Esfuerzo: medio

### F1.L4.M16 — Estado de seguridad persistido: desafíos, step-up y topes compartidos entre réplicas
- Hallazgos: IDCU-HUECO-078 (S2), IDCU-RESILIENCIA-094 (S2), IDCU-OBSERV-116 (S4)
- Causa de raíz: el adaptador «local» en memoria (`ID/infraestructura/DesafioLocal.java`, mapa `abiertos` de `ID/aplicacion/CU04StepUp.java:42`, `ID/web/LimiteDeConsultas.java`) se usa con `replicas.min: 4`; el step-up llama la sobrecarga que no entrega el código; `Instant.now()` en lugar de `Reloj`.
- Corrección de raíz: desafíos en `token_verificacion` (hash con pepper, intentos, vencimiento); una sola resolución de factor y entrega para login y step-up; topes en almacenamiento compartido; `Reloj` inyectado; sin mapas que crecen.
- CA: Dado un step-up emitido por una réplica, cuando el código se verifica en otra, entonces se acepta; el titular recibe el código.
- DoD: integración con dos instancias contra la misma base; prueba de entrega (doble del canal de correo); `grep -n "ConcurrentHashMap" ID/aplicacion/CU04StepUp.java ID/infraestructura/DesafioLocal.java` → 0.
- Depende de: F1.L4.M02.
- Esfuerzo: medio

### F1.L4.M17 — Evidencia step-up ligada al retiro concreto
- Hallazgos: IDCU-SEG-059 (S3)
- Causa de raíz: la evidencia valida propósito y `jti`, no monto, destino ni clave (`NF/infraestructura/SegundoFactorStepUp.java:85-122`).
- Corrección de raíz: claim con hash de (cuenta, destino, monto, moneda, clave de idempotencia) emitido por identidad y verificado al solicitar el retiro. Es uno de los controles del ADR de F0.L4.M01.
- CA: Dada una evidencia obtenida para un retiro de 10 BOB, cuando se usa para uno de 1000 BOB o a otro destino, entonces se rechaza.
- DoD: integración en nucleo-financiero con evidencia real: mismo retiro OK, monto distinto, destino distinto, reutilizada → rechazo.
- Depende de: F1.L4.M16; coordinar con lote L1 (nucleo-financiero).
- Esfuerzo: medio

### F1.L4.M18 — `VerificarTitularidad` coteja contra un KYC aprobado vigente
- Hallazgos: IDCU-SEG-092 (S2)
- Causa de raíz: `ID/aplicacion/VerificarTitularidad.java:45-75` confunde «declaró un documento» con «documento verificado» y puede devolver 500.
- Corrección de raíz: cotejar solo contra la verificación APROBADA vigente; sin verificación, respuesta de contrato «no verificable». Es la base de «entrega a cuenta verificada» (P-10, 91.3.3).
- CA: Dado un usuario sin KYC aprobado, cuando se consulta titularidad, entonces la respuesta es «no verificable» y nunca «coincide».
- DoD: integración: aprobado coincide, aprobado no coincide, pendiente, rechazado, vencido; ninguno → 500.
- Depende de: ninguna.
- Esfuerzo: chico

### F1.L4.M19 — Invitar y buscar por teléfono solo como miembro del grupo y servicio a servicio
- Hallazgos: IDCU-SEG-093 (S2)
- Causa de raíz: autorización por rol sin propiedad del recurso: `EmitirTokenDeInvitacion` no verifica que el emisor pertenezca al grupo; `BuscarPorTelefono` corre `deSistema` sin tope.
- Corrección de raíz: la invitación (único camino de entrada, decisión de la tanda 5) exige que el emisor sea miembro vigente del grupo (consulta a grupos con token de servicio o claim firmado); `BuscarPorTelefono` solo servicio a servicio con tope persistente por actor; quitar `ContextoSesion.deSistema(ctx.usuarioId())`.
- CA: Dado un participante que no es miembro del grupo X, cuando emite una invitación a X, entonces 403.
- DoD: autorización negativa (no miembro, miembro de otro grupo, sin token); prueba de tope; pacto con grupos (F0.L4.M05).
- Depende de: F0.L4.M05.
- Esfuerzo: medio

### F1.L4.M20 — Evidencia del expediente inmutable después de la decisión
- Hallazgos: IDCU-SEG-086 (S2)
- Causa de raíz: `ID/infraestructura/ExpedienteRepositorio.java:47-75` reemplaza fotos sin precondición sobre el estado de la verificación.
- Corrección de raíz: aceptar fotos solo si la verificación vigente está PENDIENTE/EN_REVISION (precondición en el UPDATE/INSERT); fotos como filas append-only; una recaptura abre una verificación nueva.
- CA: Dada una verificación APROBADA, cuando se sube una foto, entonces 409 y la foto anterior queda intacta.
- DoD: integración con base real: subir en PENDIENTE OK, en APROBADA/RECHAZADA rechazado; trigger append-only probado con UPDATE directo.
- Depende de: F0.L4.M04.
- Esfuerzo: chico

### F1.L4.M21 — Retención de un retiro gobernada solo por su orden
- Hallazgos: IDCU-DINERO-037 (S2), IDCU-FRAGIL-064 (S3)
- Causa de raíz: el endpoint genérico de retenciones (`NF/web/BilleteraController.java:186-200`, `CU13RetenerSaldo.java:180-245`) solo exige titularidad y no mira a qué agregado pertenece la retención; la del retiro vence por política (`SolicitudDeRetiro.java:34,126-136`).
- Corrección de raíz: prohibir por API cerrar una retención con `referencia_tipo`; solo la orden la cierra; retención del retiro sin vencimiento mientras la orden esté viva; trigger en `retencion_saldo` que impida LIBERADA si la orden referida no está RECHAZADA/PAGADA.
- CA: Dada una orden de retiro viva, cuando el titular llama a cerrar su retención, entonces 409 y el saldo sigue retenido.
- DoD: integración: titular intenta liberar (rechazo), orden rechazada libera, UPDATE directo bloqueado por trigger; prueba de doble gasto (retiro vivo + transferencia del mismo saldo → falla).
- Depende de: coordinar con lote L1.
- Esfuerzo: medio

### F1.L4.M22 — El encaje solo frena altas y autorizaciones, y por moneda
- Hallazgos: IDCU-DEFECTO-041 (S2)
- Causa de raíz: `tg_retiro_encaje` (`SQL/40_reglas/restricciones.sql:530-555`) corre en todo `UPDATE OF estado` y sobre todas las custodias, distinto de Java (`OrdenRetiroRepositorio.java:264-278`).
- Corrección de raíz: aplicar solo a INSERT y a la transición hacia AUTORIZADA, filtrando por la moneda de la orden; Java y base con la misma regla.
- CA: Dado un encaje roto en USD, cuando una orden BOB ya EN_PROCESO pasa a PAGADA, entonces la transición se acepta.
- DoD: prueba SQL con encaje roto: alta rechazada, EN_PROCESO→PAGADA aceptada, otra moneda no afectada.
- Depende de: F0.L4.M04; coordinar con lote L1.
- Esfuerzo: chico

### F1.L4.M23 — El pago del retiro asienta la línea del costo
- Hallazgos: IDCU-DINERO-042 (S2)
- Causa de raíz: `NF/aplicacion/ResolucionDeRetiro.java:103-115` asienta dos patas por el bruto; la diferencia bruto−neto no tiene cuenta (91.3.4).
- Corrección de raíz: asiento de tres patas en la misma transacción (usuario→puente por neto, usuario→ingreso por comisión fija del servicio), con moneda y clave de idempotencia. La comisión es la fija e igual para todos (DECISIONES, «Ingreso de la empresa»; P-17).
- CA: Dado un retiro de X con costo c, cuando se paga, entonces el asiento tiene débitos = créditos = X y una línea de ingreso por c.
- DoD: integración contra base real con el asiento pegado, cuadre desde el mayor, reintento con la misma clave sin duplicar (91.6).
- Depende de: coordinar con lote L1.
- Esfuerzo: chico

### F1.L4.M24 — Bitácoras y evidencias de identidad y cumplimiento append-only
- Hallazgos: IDCU-DATOS-047 (S2)
- Causa de raíz: `SQL/00_base/03_permisos.sql:17,42` da UPDATE a los servicios y `SQL/35_append_only/append_only.sql` no incluye intentos, consentimientos, aceptaciones, envíos regulatorios ni casos LFT.
- Corrección de raíz: agregar esas tablas a la lista APPEND_ONLY del modelo (REVOKE UPDATE/DELETE + trigger); estados terminales sellados por trigger (`reporte_regulatorio` post-ENVIADO, `caso_investigacion_lft` cerrado).
- CA: Dado el rol del servicio, cuando intenta `UPDATE consentimiento`, entonces la base lo rechaza.
- DoD: prueba SQL por tabla con `SET ROLE svc_*` (UPDATE y DELETE rechazados); chequeo de deriva (F0.L4.M04) en verde.
- Depende de: F0.L4.M04.
- Esfuerzo: medio

### F1.L4.M25 — `GET /licencia/alcance` no escribe y no acepta el usuario del query
- Hallazgos: IDCU-SEG-137 (S3)
- Causa de raíz: `CU/web/LicenciaController.java:38-45` toma `usuarioId` del query y escribe en el outbox en cada rechazo; `usuariosRegistrados` siempre 0.
- Corrección de raíz: lectura sin efectos; el usuario sale del token (o, para consultas servicio a servicio, de un token de servicio con alcance); el rechazo se registra una vez por operación real, no por GET; contador real de usuarios del sandbox.
- CA: Dado un participante, cuando consulta el alcance con el id de otra persona, entonces 403 o se ignora el parámetro; el outbox no crece.
- DoD: autorización negativa; conteo de filas de outbox antes y después de 10 GET → igual.
- Depende de: ninguna.
- Esfuerzo: chico

### F1.L4.M26 — Controles regulatorios que derivan sus hechos del servidor, no de booleanos de entrada
- Hallazgos: IDCU-SEG-136 (S3)
- Causa de raíz: CU-47, CU-48, CU-49, CU-52, CU-53 y CU-41 reciben `comiteConQuorum`, `hayNoObjecion`, `exento`, etc. como entrada.
- Corrección de raíz: cada caso de uso consulta el hecho a su dueño (acta con quórum, no objeción registrada, roles del designado en identidad); los booleanos salen de las firmas; regla de ArchUnit/prueba que impida exponer estos CU sin esa derivación.
- CA: Dado un comité sin quórum registrado, cuando se evalúa el riesgo de producto, entonces la evaluación no se aprueba aunque nadie haya enviado el booleano.
- DoD: unitarias e integración por caso de uso con el hecho ausente/presente; `grep -n "comiteConQuorum\|hayNoObjecion" CU/aplicacion` sin parámetros de entrada.
- Depende de: ninguna.
- Esfuerzo: medio

## F2 — Retiro de lo que contradice el flujo nuevo

### F2.L4.M01 — Retirar la doble aprobación humana de retiros (endpoint, bandeja, permiso, pantallas, pruebas, CHECK)
- Hallazgos: IDCU-DOC-006 (S4), IDCU-CONTRATO-057 (S3), IDCU-APROB-060 (S3), IDCU-PRUEBAS-073 (S4), IDCU-DINERO-146 (S2)
- Causa de raíz: la aprobación por operación (ADR-049) es una aprobación ELIMINABLE según P-09; sus defectos (aprobación a ciegas, bandeja sin datos, contrato 404/409 vs 422, matriz negativa incompleta, comentario obsoleto) desaparecen con ella.
- Corrección de raíz: quitar `GET /billetera/retiros` (bandeja) y `POST /billetera/retiros/{id}/aprobacion` de `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml`; la rama de aprobación de `NF/aplicacion/ResolucionDeRetiro.java:188-290`, `NF/infraestructura/AprobacionDeRetiroRepositorio.java`, `BandejaDeRetirosRepositorio.java`; permiso `RETIRO_APROBAR` de `SQL/60_semillas/10-roles-y-permisos.sql`; `ck_retiro_doble_aprobacion`, columna `requiere_doble_aprobacion` y clave `doble-aprobacion-desde` de `application.yml`; pantallas `BO/operacion/retiros/` (bandeja, aprobación, dominios `cu11-*`); pruebas `BilleteraAprobacionWebTest`/`BilleteraBandejaWebTest`; paso del catálogo `docs/catalogo-de-flujos/recarga-y-retiro.yaml`. Los controles que lo reemplazan son F3.L4.M08.
- CA: Dado el sistema, cuando se busca una ruta o pantalla de aprobación de retiro, entonces no existe; un retiro dentro de los controles avanza sin intervención humana.
- DoD: `grep -rn "RETIRO_APROBAR\|doble-aprobacion\|aprobarRetiro\|requiere_doble_aprobacion" servicios sql apps docs/catalogo-de-flujos` → 0; build de backoffice y nucleo-financiero; chequeo de deriva en verde; `python3 scripts/validar_flujos.py` sin el paso.
- Depende de: F0.L4.M01; coordinar con lote L1 y L6.
- Esfuerzo: medio

### F2.L4.M02 — Retirar el organizador profesional del alcance de identidad y backoffice de cumplimiento
- Hallazgos: IDCU-SEG-109 (S3), IDCU-APROB-159 (S3)
- Causa de raíz: P-11 elimina al organizador; la firma de contrato de organizador (evidencia débil) y la pantalla de habilitación (sin guardia ni doble envío) pertenecen a ese circuito.
- Corrección de raíz: borrar `ID/aplicacion/ActivarRolOrganizador.java`, `ID/infraestructura/FirmaDeOrganizadorRepositorio.java`, `OrganizadorPorHttp.java`, la ruta `POST /usuarios/{id}/roles/organizador` de `identidad.yaml`, el rol ORGANIZADOR y `GRUPO_ADMINISTRAR` autoconcedido de la semilla, `BO/cumplimiento/organizadores/` y `dominio/cu90-habilitacion.ts` con su ruta en `cumplimiento.routes.ts:117-121`, y la política de token `FIRMA_CONTRATO`. Con esto caen también las porciones de organizador de IDCU-TRAZA-087, IDCU-RESILIENCIA-108, IDCU-FRAGIL-101, IDCU-DINERO-150, IDCU-FRAGIL-160, IDCU-DOC-162 e IDCU-SEG-169.
- CA: Dado el sistema, cuando se busca cómo obtener el rol ORGANIZADOR, entonces no hay ruta, permiso ni pantalla.
- DoD: `grep -rni "organizador" servicios/identidad/src apps/backoffice/src/app/rutas/cumplimiento sql/60_semillas` → solo referencias históricas documentadas; build y pruebas de identidad y backoffice.
- Depende de: retiro del servicio organizador en lote L3.
- Esfuerzo: medio

## F3 — Flujo nuevo

### F3.L4.M01 — Puerto del proveedor KYC con doble de tres niveles; la prueba de vida la decide el proveedor
- Hallazgos: IDCU-SEG-015 (S2)
- Causa de raíz: no hay proveedor de OCR/prueba de vida; el cliente simula la prueba de vida y el cotejo (`MOV/dominio/ciclo_de_pose.dart`, `MOV/pasos_alta/paso_cotejo.dart`) y lo compensaba la revisión humana.
- Corrección de raíz: puerto `VerificadorDeIdentidad` en identidad con adaptador real (cuando se contrate) y doble declarado con firma real en tres niveles (regla 65): correcto (APROBADA), límite (EXCEPCION: documento borroso, vencimiento al día; PEP), inválido (firma inválida del callback, timeout, 5xx). La app deja de rotular el cotejo como verificación («Revisá tus datos») y envía la captura al proveedor; el texto del contrato (`MOV/texto_del_contrato.dart:76`) se ajusta.
- CA: Dada una captura que el doble califica como EXCEPCION, cuando termina la verificación, entonces la cuenta queda en revisión de Cumplimiento y no se activa.
- DoD: pruebas de contrato del puerto con el doble en los tres niveles; callback con firma inválida → rechazado; timeout → estado reintentable sin activar.
- Depende de: F0.L4.M02; P-08.
- Esfuerzo: grande

### F3.L4.M02 — Decisión KYC por evento (outbox) y asignación automática con actor de sistema y ámbito correcto
- Hallazgos: IDCU-FLUJO-FALTANTE-085 (S2), IDCU-TRAZA-087 (S2)
- Causa de raíz: la decisión KYC no emite evento (`ID/infraestructura/RevisionRepositorio.java:174-235`) y el rol PARTICIPANTE se otorga «a nombre del revisor» con ámbito GLOBAL por falta de un actor de sistema.
- Corrección de raíz: eventos `identidad.verificacion_iniciada` y `identidad.verificacion_resuelta` (nivel resultante, desenlace) por outbox; actor de sistema explícito para asignaciones automáticas; asignación con el ámbito declarado del rol; regla «nadie resuelve su propio expediente» para las excepciones humanas; consumidor idempotente en cumplimiento. `identidad.yaml:350-352,458` deja de decir «manual».
- CA: Dado un desenlace APROBADA del proveedor, cuando se procesa, entonces se publica una vez `verificacion_resuelta`, el rol se otorga por el actor de sistema y cumplimiento lo registra una sola vez aunque el mensaje llegue dos veces.
- DoD: integración con outbox real; prueba de duplicado de mensaje (mismo `message_id` dos veces → un efecto); prueba de caída de cumplimiento (el evento queda en outbox y se entrega al volver).
- Depende de: F3.L4.M01.
- Esfuerzo: medio

### F3.L4.M03 — App: la compuerta de dinero refleja el estado del servidor y avisa el cambio
- Hallazgos: IDCU-APROB-008 (S2), IDCU-DEFECTO-029 (S4)
- Causa de raíz: el cliente codifica «una persona los revisa» (`MOV/textos_de_verificacion.dart`, `MOV/pantalla_identidad_requerida.dart`, `apps/movil/lib/proveedores/sesion.dart:149-159`) y sondea 40 s una revisión humana.
- Corrección de raíz: la app solo mapea `estadoDeCuenta` del servidor (APROBADA, EN_VERIFICACION, EN_REVISION_CUMPLIMIENTO, RECHAZADA con motivo codificado); aviso por notificación/evento en vez de sondeo, con respaldo de reintento con backoff y aviso cuando se detiene; textos nuevos redactados por Claude y aprobados por Pablo (P-16).
- CA: Dado un KYC resuelto por el proveedor, cuando la app vuelve al frente o recibe el aviso, entonces muestra el estado nuevo sin texto de «revisión de una persona».
- DoD: pruebas de widget por estado; `grep -rn "una persona" apps/movil/lib/pantallas/identidad` → 0; capturas en tres anchos y ambos temas.
- Depende de: F3.L4.M02.
- Esfuerzo: medio

### F3.L4.M04 — Recaptura de las caras rechazadas del expediente
- Hallazgos: IDCU-HUECO-014 (S2)
- Causa de raíz: `pendientesDeKyc` modela solo faltantes «nuevos» y la app borra copias locales de caras fallidas.
- Corrección de raíz: derivar faltantes de `EstadoDeVerificacion.fotos` para las cinco caras (incluido lo que el proveedor rechace) y ofrecer recaptura; conservar la copia local hasta que el servidor acepte.
- CA: Dado un reverso rechazado, cuando el titular abre su expediente, entonces ve «volvé a sacar el reverso» y puede subirlo.
- DoD: pruebas de widget y de dominio para cada cara; prueba de que la copia local persiste tras fallo.
- Depende de: F3.L4.M01, F1.L4.M20.
- Esfuerzo: medio

### F3.L4.M05 — Perfil transaccional enviado a cumplimiento, con importe decimal y moneda
- Hallazgos: IDCU-HUECO-009 (S2), IDCU-DINERO-016 (S3), IDCU-DINERO-097 (S3)
- Causa de raíz: `EntradaRegistro` (`identidad.yaml:1415-1418`) nunca incorporó el perfil y la prueba fija la ausencia; el monto es `double` en la app y texto libre en el borrador.
- Corrección de raíz: contrato del perfil transaccional con dueño cumplimiento (endpoint o evento) usando `Dinero{monto decimal, moneda}`; la app usa el `Dinero` del cliente generado; borrador con DECIMAL + moneda; la prueba `apps/movil/test/identidad/estado_alta_test.dart:135-160` pasa a exigir el envío. Alimenta el monitoreo de desvío UIF y la clasificación del proveedor KYC.
- CA: Dado un alta con perfil transaccional, cuando se completa, entonces cumplimiento tiene el perfil con moneda y el monto exacto.
- DoD: prueba de contrato app→identidad→cumplimiento; integración que lea el perfil persistido; `grep -n "double" MOV/pasos_alta/paso_perfil_transaccional.dart` → 0.
- Depende de: F3.L4.M02.
- Esfuerzo: medio

### F3.L4.M06 — Declaración PEP persistida en cumplimiento, con idempotencia y acuse
- Hallazgos: IDCU-HUECO-010 (S2)
- Causa de raíz: la pantalla (`MOV/pantalla_verificacion_profunda.dart:104-117`) se construyó antes del endpoint y guarda en memoria.
- Corrección de raíz: endpoint de declaración PEP en cumplimiento (con `Idempotency-Key`), consumido por la app con estados de carga/éxito/error; una declaración PEP abre la revisión de Cumplimiento con cuatro ojos (F1.L4.M05). Hasta tenerlo, la ruta `/identidad/verificacion-profunda` queda oculta.
- CA: Dado un titular que se declara PEP, cuando envía, entonces queda registrada en el servidor y la cuenta pasa a revisión reforzada.
- DoD: integración con base real; prueba de reintento con la misma clave → una sola fila; prueba de widget del error.
- Depende de: F1.L4.M05 y F4.L4.M53 (esta última se adelanta: es el arreglo del repositorio que el endpoint usa).
- Esfuerzo: medio

### F3.L4.M07 — Despacho único y automático del retiro al proveedor de desembolso, confirmado por webhook firmado
- Hallazgos: IDCU-FLUJO-FALTANTE-036 (S2), IDCU-RESILIENCIA-062 (S3)
- Causa de raíz: hay dos despachos (`NF/aplicacion/CU11RetirarSaldo.java:107-136` `instruirPago`, y `CU11InstruirRetiro` + `RetirosConProveedor`) y ninguno está cableado a un disparador; `instruirPago` además corre sin transacción y llama al proveedor dentro de la que el comentario dice evitar.
- Corrección de raíz: borrar `instruirPago`; consumidor idempotente de `retiro_autorizado` (outbox) que instruye al puerto del proveedor de desembolso (P-15: doble con firma real en tres niveles) fuera de la transacción; la orden pasa a PAGADA solo por el webhook firmado del proveedor; timeout por paso y compensación (RECHAZADA libera la retención por la orden, F1.L4.M21).
- CA: Dado un retiro autorizado, cuando el doble del proveedor confirma por webhook válido, entonces la orden queda PAGADA y el asiento existe una vez; con webhook de firma inválida, entonces nada cambia.
- DoD: integración con el doble: correcto (pago confirmado), límite (mismo webhook dos veces → un asiento; webhook al borde de la ventana), inválido (firma inválida, monto distinto, orden inexistente, proveedor caído → reintento con backoff y sin doble pago); `python3 scripts/validar_flujos.py` sin aviso «SIN CABLEAR».
- Depende de: F0.L4.M01, F1.L4.M21, F1.L4.M23; coordinar con lote L1.
- Esfuerzo: grande

### F3.L4.M08 — Controles automáticos del retiro con decisión registrada y un solo esquema de evento de rechazo
- Hallazgos: IDCU-APROB-043 (S2), IDCU-TRAZA-048 (S3), IDCU-CONTRATO-049 (S3)
- Causa de raíz: el único «control» era un umbral de 5000.00 sin moneda ni procedencia, por operación y decidido por el controlador; la decisión no deja registro y `retiro_rechazado` sale con dos formas.
- Corrección de raíz: controles del ADR (F0.L4.M01) en el dominio de nucleo-financiero: límites por ventana acumulada y por moneda desde catálogo versionado con procedencia (parámetros obligatorios sin valor por defecto: el servicio no arranca sin ellos), enfriamiento tras cambio de cuenta, cuenta verificada (F1.L4.M18), step-up ligado (F1.L4.M17), alerta UIF; tabla append-only `decision_retiro(orden_id, control, desenlace, motivo_codificado, decidida_en)`; esquema único versionado de `retiro_rechazado` con motivo codificado. Cumplimiento revisa solo las alertas.
- CA: Dados dos retiros que juntos superan el límite de la ventana, cuando se solicita el segundo, entonces se rechaza con motivo codificado y queda una fila en `decision_retiro`; sin parámetros configurados, el servicio no arranca.
- DoD: integración por control (correcto, límite exacto, superado, otra moneda); prueba de arranque sin parámetros → falla; prueba de contrato del evento con los consumidores.
- Depende de: F0.L4.M01, F2.L4.M01; coordinar con lote L1.
- Esfuerzo: grande

### F3.L4.M09 — Detección UIF alimentada por los cobros y retiros confirmados, con tipo de cambio de catálogo
- Hallazgos: IDCU-FLUJO-FALTANTE-117 (S2), IDCU-DINERO-135 (S3)
- Causa de raíz: CU-41/CU-42 esperan que «quien posee las transacciones» les pase acumulado y tipo de cambio, pero nadie publica ni consume nada; el tipo de cambio llega del llamador sin fecha ni semántica.
- Corrección de raíz: consumidor idempotente en cumplimiento (deduplicación por `message_id`) de los eventos de aporte cobrado por QR (webhook confirmado), recarga y retiro pagado; acumulado por proyección local con desfase declarado; tipo de cambio desde catálogo con fuente, fecha y sentido declarado; importes con moneda en `EntradaLimite`/eventos; CU-43 bloquea `generar` si el consumidor tiene desfase mayor al tolerado (98.4.3).
- CA: Dado un aporte cobrado que supera el umbral PCC-01, cuando llega su evento (aunque llegue dos veces), entonces se registra una operación relevante una sola vez; con desfase, el reporte mensual no se genera.
- DoD: integración con mensajes reales: correcto, duplicado, umbral exacto, otra moneda con tipo de cambio de catálogo, consumidor caído y recuperado; reporte generado con datos.
- Depende de: flujo QR → webhook → cobro de F3 (lotes L1/L2); F4.L4.M25.
- Esfuerzo: grande

### F3.L4.M10 — Consulta del efecto de reclamos con token de servicio (descargo y apelación)
- Hallazgos: IDCU-COHERENCIA-122 (S2)
- Causa de raíz: garantía consulta el efecto de reclamos con el token del usuario final y la ruta exige `RECLAMO_VER`: la respuesta es 403 y se traduce a «FRENA» siempre.
- Corrección de raíz: token de servicio con permiso dedicado `RECLAMO_EFECTO_LEER` para garantía (el consumidor de organizador desaparece con F2.L4.M02); prueba de contrato consumer-driven; el 403 deja de traducirse a un efecto. Es la pieza que permite que el incumplimiento declarado por el sistema respete un reclamo abierto (P-12, 91.4).
- CA: Dado un participante sin reclamos, cuando garantía consulta el efecto, entonces recibe «NO FRENA»; con reclamo abierto, «FRENA».
- DoD: pacto garantía→cumplimiento verificado por el productor; prueba de caída de cumplimiento con fallback declarado.
- Depende de: F0.L4.M05; coordinar con lote L2.
- Esfuerzo: medio

### F3.L4.M11 — Lámina del tour reescrita para el modelo A «Tu garantía vuelve»
- Hallazgos: IDCU-RESPALDO-007 (S2)
- Causa de raíz: el texto del tour (`MOV/textos.dart:42-46`, `MOV/laminas_del_tour.dart:44-50`) se escribió para un fondo genérico que «cubre», sin trazabilidad a la regla vigente.
- Corrección de raíz: texto nuevo según P-01/P-02 (modelo A + D): aporte chico extra a un fondo **del grupo** en cuenta segregada que nunca es de la empresa; si alguien no paga, el fondo cubre su cuota y esa persona queda debiendo al fondo; al cierre se devuelve lo aportado menos lo incobrable en partes iguales; la puntualidad da beneficios. Arte `ArteFondoDeGarantia` ajustado. Redacta Claude, aprueba Pablo (P-16); alinear con el contrato de participación.
- CA: Dada la lámina 4, cuando se lee, entonces nombra el fondo del grupo, su devolución al cierre y no atribuye la cobertura a la empresa.
- DoD: aprobación escrita de Pablo en el PR; prueba de golden del tour; capturas en tres anchos y ambos temas.
- Depende de: P-16; validación con Cumplimiento del fondo segregado (DECISIONES).
- Esfuerzo: chico

### F3.L4.M12 — Invitación web → app: el secreto llega a la app y la admisión no la resuelve una persona
- Hallazgos: IDCU-FLUJO-FALTANTE-174 (S2)
- Causa de raíz: la web retira el token de la URL y lo guarda en memoria, pero no diseñó el traspaso a la app; el texto dice que «la resuelve una persona de AportaYa» (`apps/web/src/app/paginas/invitacion/invitacion.ts:15,52-53,97`).
- Corrección de raíz: enlace universal/deep link `aportaya://invitacion#t=…` construido desde memoria en `apps/web/src/app/layout/enlace-a-la-app.ts`; la app canjea el token; texto nuevo: la entrada es por invitación de un miembro y se admite automáticamente si se cumplen KYC aprobado, reglamento aceptado y sin deudas vivas (tanda 5; P-16).
- CA: Dado un invitado que abre el enlace en la web, cuando toca «Abrir en la app», entonces la app recibe el token y muestra el grupo; el texto no menciona revisión humana.
- DoD: prueba E2E web→enlace (Playwright, sin `networkidle`); prueba de la app del canje con token válido, vencido y usado.
- Depende de: F1.L4.M19; admisión automática en lote L3.
- Esfuerzo: medio

## F4 — Calidad y deuda

### F4.L4.M01 — Un único CU-40 en nucleo-financiero; retirar la copia muerta de cumplimiento
- Hallazgos: IDCU-REESCRITURA-001 (S2), IDCU-ACOPLE-002 (S3), IDCU-DEFECTO-003 (S3)
- Causa de raíz: CU-40 se implementó en dos servicios; la copia de cumplimiento no bloquea (`FOR UPDATE` solo en el comentario), lee el esquema `nucleo_financiero` y calcula la fecha en UTC.
- Corrección de raíz: borrar `CU/aplicacion/CU40EvaluarLimites.java`, `CU/infraestructura/LimiteRepositorio.java`, `CU/dominio/EvaluacionDeLimites.java` y sus pruebas (`CU40Test`, `CU40RechazosTest`, armados); quitar el GRANT de cumplimiento sobre `nucleo_financiero`; si cumplimiento necesita el rastro, consume `nucleo_financiero.limite_rechazo`. La regla viva usa `Reloj.hoy()` en La Paz (verificar en nucleo-financiero).
- CA: Dado el repositorio, cuando se busca CU-40, entonces existe solo en nucleo-financiero y cumplimiento no tiene permisos sobre ese esquema.
- DoD: `grep -rn "CU40EvaluarLimites\|nucleo_financiero" servicios/cumplimiento/src` → 0; build y suite de cumplimiento en verde; prueba de rol: `SET ROLE svc_cumplimiento; SELECT … FROM nucleo_financiero.consumo_limite` → permiso denegado.
- Depende de: coordinar con lote L1.
- Esfuerzo: chico

### F4.L4.M02 — App: llamadas de identidad por el cliente generado
- Hallazgos: IDCU-CONTRATO-017 (S3)
- Causa de raíz: deriva entre spec y cliente: seis operaciones por Dio crudo con casteos sin validar.
- Corrección de raíz: regenerar el cliente y usar `registrarUsuario`, `consultarMiPerfil`, `actualizarFechaExpiracionDocumento`, `iniciarVerificacion`, `subirDocumento`, `cambiarMiFotoDePerfil`; un solo provider de `MiPerfil`.
- CA: Dado un cambio de forma en el spec, cuando se compila la app, entonces falla la compilación en vez de fallar en ejecución.
- DoD: `grep -rn "dio.post\|dio.get" MOV/dominio` → 0 para esas operaciones; `flutter analyze` y pruebas de dominio en verde.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M03 — Un único catálogo codificado de tipo de documento, nivel y estado KYC
- Hallazgos: IDCU-COHERENCIA-018 (S3), IDCU-COHERENCIA-051 (S3), IDCU-COHERENCIA-105 (S3)
- Causa de raíz: el mismo concepto tiene tres vocabularios (`CEX`, `CARNET_EXTRANJERO`, `CARNET_EXTRANJERIA`), dos para nivel (`COMPLETO`/`AVANZADO`), dos regex de teléfono, y las conversiones tragan errores (`ID/web/MapeoDelBorrador.java:24,39`; coerción a CI en `MOV/dominio/cu01_registrar.dart:105-109`).
- Corrección de raíz: catálogo codificado (97.4.7) usado por contrato, borrador, tablas y cliente; mapeos que fallan ante un valor desconocido; una regex de teléfono en el spec.
- CA: Dado un borrador retomado con tipo de documento extranjero, cuando se registra, entonces el valor se acepta sin conversión; un valor desconocido devuelve error de contrato.
- DoD: prueba de contrato y de mapeo con todos los valores; chequeo de deriva (F0.L4.M04).
- Depende de: F0.L4.M04.
- Esfuerzo: medio

### F4.L4.M04 — Tipos de documento del alta según la decisión registrada
- Hallazgos: IDCU-HUECO-019 (S3)
- Causa de raíz: alcance recortado a CI sin decisión registrada.
- Corrección de raíz: registrar AMB-L4-2; si se admiten otros tipos, selector y validación por tipo en `MOV/pasos_alta/paso_documento.dart`; si no, el contrato y el texto del contrato lo dicen.
- CA: Dada la decisión, cuando un extranjero intenta el alta, entonces la app hace exactamente lo decidido.
- DoD: pruebas de widget por tipo admitido; decisión enlazada en el PR.
- Depende de: AMB-L4-2 (Pablo), F4.L4.M03.
- Esfuerzo: medio

### F4.L4.M05 — App: errores manejados por resultado, sin estados colgados
- Hallazgos: IDCU-FRAGIL-020 (S3)
- Causa de raíz: manejo por tipo de excepción esperado (`catch (_) {}`, solo `on DioException`, sin `finally`).
- Corrección de raíz: `on Object` con estado `fallida` accionable en `verificacion_correo.dart`, `subida_del_expediente.dart`, `pantalla_camara.dart`, `pantalla_identidad_requerida.dart`, `pantalla_perfil.dart`; registrar sin PII el fallo de borrador.
- CA: Dado un fallo de lectura del archivo, cuando se sube una cara, entonces la cara queda en «falló, reintentá» y el botón no se bloquea.
- DoD: pruebas de dominio inyectando `FileSystemException` y `TypeError`.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M06 — Rotar la clave de idempotencia al repetir la foto en «completar KYC»
- Hallazgos: IDCU-DEFECTO-021 (S3)
- Causa de raíz: el patrón de rotación (`rotarClaveParaRepetir`) no se aplicó en `MOV/pantalla_completar_kyc.dart:80-88`.
- Corrección de raíz: usar la misma rotación.
- CA: Dada una foto repetida, cuando se envía, entonces lleva clave nueva y el servidor no devuelve la respuesta anterior.
- DoD: prueba de dominio de la clave antes/después de repetir.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M07 — Credenciales fuera del backup de Android y candado ligado al Keystore
- Hallazgos: IDCU-SEG-022 (S3)
- Causa de raíz: configuración por defecto (`apps/movil/android/app/src/main/AndroidManifest.xml:10`).
- Corrección de raíz: `android:allowBackup="false"` o `dataExtractionRules` que excluyan el almacén seguro; evaluar refresh token ligado a una clave del Keystore con autenticación de usuario.
- CA: Dado un backup de la app, cuando se restaura en otro equipo, entonces no contiene tokens.
- DoD: inspección del manifiesto fusionado (`aapt dump xmltree`) pegada; prueba manual documentada.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M08 — Un solo refrescador de sesión con timeouts
- Hallazgos: IDCU-RESILIENCIA-023 (S3)
- Causa de raíz: `apps/movil/lib/dominio/renovar_acceso.dart` copió `BaseOptions` sin timeouts y hay dos refrescadores.
- Corrección de raíz: compartir el `Refrescador` single-flight y heredar timeouts.
- CA: Dadas dos peticiones con token vencido, cuando se refresca, entonces hay una sola llamada de refresco.
- DoD: prueba con servidor simulado contando llamadas y con timeout provocado.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M09 — Motivos de rechazo codificados en el contrato; vacíos que orientan
- Hallazgos: IDCU-UX-A11Y-024 (S3)
- Causa de raíz: catálogo de motivos no codificado en el contrato; se muestra el texto libre del servidor; `mensajeVacio: ''`.
- Corrección de raíz: enum de motivos en `identidad.yaml` y en el desenlace del proveedor; texto genérico accionable para desconocidos; vacíos con texto que explica y orienta.
- CA: Dado un motivo desconocido, cuando se muestra, entonces la persona ve un texto accionable, no el código.
- DoD: pruebas de widget por motivo y vacío.
- Depende de: F3.L4.M01.
- Esfuerzo: chico

### F4.L4.M10 — Rutas de la app que validan sus parámetros
- Hallazgos: IDCU-FRAGIL-026 (S3)
- Causa de raíz: sin validación de parámetros en `MOV/rutas_de_entrada.dart:96,104,113,120`.
- Corrección de raíz: validar y redirigir a una pantalla de estado.
- CA: Dado un enlace profundo sin `extra`, cuando se abre, entonces la app muestra la pantalla de estado y no revienta.
- DoD: prueba de navegación por ruta sin parámetros.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M11 — Nombre accesible del botón «Volver» de la cámara
- Hallazgos: IDCU-UX-A11Y-027 (S3)
- Causa de raíz: botón de solo icono sin nombre (`MOV/pasos_alta/cabecera_de_camara.dart:24-27`).
- Corrección de raíz: `tooltip: 'Volver'` (y `Semantics`).
- CA: Dado un lector de pantalla, cuando enfoca el botón, entonces anuncia «Volver».
- DoD: prueba de semántica del widget.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M12 — Pruebas de la app que exigen el envío en vez de fijar el hueco
- Hallazgos: IDCU-PRUEBAS-028 (S3)
- Causa de raíz: las pruebas fijan la ausencia del perfil, solo prueban «cancelar» en la baja y omiten goldens con `skip`.
- Corrección de raíz: pruebas de contrato que exijan el envío del perfil y la declaración PEP; pruebas de contraseña, baja y dispositivo de confianza cuando existan sus endpoints; quitar los `skip` de los goldens.
- CA: Dada la suite, cuando se quita el envío del perfil, entonces una prueba falla.
- DoD: correr la suite y pegar la salida; `grep -rn "skip:" apps/movil/test/goldens/h10_alta_*` → 0.
- Depende de: F3.L4.M05, F3.L4.M06, F4.L4.M19, F4.L4.M20.
- Esfuerzo: medio

### F4.L4.M13 — Borrar código muerto y duplicado de la app (identidad)
- Hallazgos: IDCU-DUP-INT-030 (S4)
- Causa de raíz: copias divergentes y funciones sin uso (`modelo_del_alta.dart`, `carnet_de_prueba.dart`, `errores_paso_datos.dart`, providers sin uso).
- Corrección de raíz: borrar; `carnet_de_prueba.dart` fuera del binario de release.
- CA: Dado el binario de release, cuando se busca `generarCapturasDePrueba`, entonces no está.
- DoD: `flutter analyze` sin «unused»; build de release y búsqueda del símbolo.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M14 — App: tamaños por tokens y textos en archivos de textos
- Hallazgos: IDCU-CLEAN-031 (S4)
- Causa de raíz: literales de tamaño y texto en widgets.
- Corrección de raíz: tokens de `aportaya_diseno` y archivos de textos.
- CA: Dado el módulo, cuando se busca un literal numérico de tamaño, entonces no aparece.
- DoD: `grep` de literales sobre las ubicaciones del informe → 0; goldens sin cambio visual.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M15 — Movimiento condicionado a «reducir movimiento»
- Hallazgos: IDCU-UX-A11Y-032 (S4)
- Causa de raíz: `Scrollable.ensureVisible` e indicador indeterminado sin `duracion(context, …)`.
- Corrección de raíz: usar `duracion(context, …)` y un indicador estático cuando se pide reducir movimiento.
- CA: Con «reducir movimiento» activo, cuando se navega el alta, entonces no hay animación.
- DoD: prueba de widget con `MediaQuery(disableAnimations: true)`.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M16 — La cámara devuelve un único tipo de resultado
- Hallazgos: IDCU-FRAGIL-033 (S4)
- Causa de raíz: dos formas de resultado y documentación de un valor que ya no existe.
- Corrección de raíz: un tipo sellado de resultado en `pantalla_camara.dart` y `sacar_captura.dart`.
- CA: Dado cualquier camino de la cámara, cuando vuelve, entonces el llamador recibe el mismo tipo.
- DoD: `flutter analyze` + prueba de dominio.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M17 — Usar el vencimiento del token de alta e invalidar el provider KYC por sesión
- Hallazgos: IDCU-TRAZA-034 (S4)
- Causa de raíz: `tokenDeAltaVenceEn` ignorado y `FutureProvider.family` sin `autoDispose` ni generación de sesión.
- Corrección de raíz: ofrecer «ingresá para continuar» al vencer; `autoDispose` + `generacionDeSesionProvider`.
- CA: Dado un cierre de sesión, cuando entra otro titular, entonces no ve el estado KYC anterior.
- DoD: prueba de provider con cambio de sesión.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M18 — Edad por fecha civil y política de clave expuesta por el servidor
- Hallazgos: IDCU-REG-035 (S4)
- Causa de raíz: reglas de negocio duplicadas en el cliente, edad por `inDays / 365.25`.
- Corrección de raíz: comparar por fecha civil; la política de clave la valida el servidor y el cliente solo la refleja.
- CA: Dado alguien que cumple 18 hoy, cuando se valida, entonces es mayor de edad.
- DoD: unitarias en el borde (día anterior, mismo día, bisiestos).
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M19 — Cambio de clave, recuperación de acceso y dispositivo de confianza con endpoint real
- Hallazgos: IDCU-HUECO-011 (S2), IDCU-HUECO-025 (S3), IDCU-HUECO-088 (S2)
- Causa de raíz: CU-08/CU-09 implementados sin su ruta y pantallas publicadas sin endpoint (`MOV/pantalla_contrasena.dart`, `pantalla_dispositivos.dart`).
- Corrección de raíz: exponer en `identidad.yaml` cambio de clave (con clave actual, rate limit, revocación de sesiones), recuperación con token de un solo uso y enfriamiento (`VentanaDeEnfriamiento`), y confiar en dispositivo; con autorización negativa e idempotencia. Hasta tenerlos, ocultar las filas en la app.
- CA: Dada una clave cambiada, cuando se usa la anterior o una sesión previa, entonces se rechaza.
- DoD: integración de cada endpoint con autorización negativa; prueba «recuperación sin enumeración» (misma respuesta para cuenta existente e inexistente).
- Depende de: F1.L4.M13, F1.L4.M16.
- Esfuerzo: grande

### F4.L4.M20 — Baja de cuenta con precondiciones validadas en el servidor
- Hallazgos: IDCU-HUECO-012 (S2)
- Causa de raíz: el contrato promete cerrar la cuenta «desde la app» y no hay endpoint (`CU09SolicitarBaja` sin ruta).
- Corrección de raíz: endpoint de baja con precondiciones en la escritura (saldo cero, sin cupos activos, sin deudas vivas con el fondo del grupo); pantalla conectada; prueba de confirmación.
- CA: Dado un titular con saldo o cupo activo, cuando pide la baja, entonces recibe el motivo; sin ellos, la cuenta queda en BAJA y no puede ingresar (F1.L4.M01).
- DoD: integración con cada precondición; prueba de widget del envío.
- Depende de: F1.L4.M01.
- Esfuerzo: medio

### F4.L4.M21 — Catálogo de contratos de adhesión versionado: servido, aceptado y persistido con su hash real
- Hallazgos: IDCU-COHERENCIA-013 (S2), IDCU-DATOS-072 (S4), IDCU-REG-084 (S2)
- Causa de raíz: no hay catálogo de documentos versionados conectado: el registro escribe versión «1» y hash de ceros, la app muestra un texto fijo y las semillas tienen relleno.
- Corrección de raíz: cumplimiento sirve el texto (o URL firmada + hash) de la versión vigente; la app pinta eso y bloquea aceptar si el hash difiere; el registro persiste versión y hash reales y el contrato aceptado; semillas con documento real y una sola política por propósito de token; `licenciaHabilitaBilletera` consultado, no fijo.
- CA: Dada una versión nueva del contrato, cuando un titular acepta, entonces queda registrada esa versión con su hash.
- DoD: integración del alta con el catálogo; prueba de hash distinto → rechazo; `grep -n 'repeat(64)\|"0".repeat' servicios/identidad sql/60_semillas` → 0.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M22 — Exigir contrato vigente y diligencia vigente en las operaciones de dinero
- Hallazgos: IDCU-HUECO-040 (S2)
- Causa de raíz: `fn_uif_exigir_ddd` y `fn_con_exigir_contrato` (`SQL/40_reglas/restricciones.sql:1089-1100,1197-1210`) solo los llaman pruebas.
- Corrección de raíz: invocarlas desde los triggers de las operaciones de dinero (o desde `fn_lim_evaluar`), con contratos reales sembrados (F4.L4.M21).
- CA: Dado un titular sin contrato aceptado vigente, cuando intenta un aporte o un retiro, entonces la base lo rechaza.
- DoD: prueba SQL e integración en nucleo-financiero con y sin contrato/diligencia.
- Depende de: F4.L4.M21; coordinar con lote L1.
- Esfuerzo: medio

### F4.L4.M23 — Borrador del alta sin número de documento en claro y con importe tipado
- Hallazgos: IDCU-SEG-044 (S2), IDCU-DATOS-082 (S2)
- Causa de raíz: el borrador se diseñó como espejo del formulario (`SQL/15_infra/borradores.sql`), fuera del `.puml`, con PII como PK.
- Corrección de raíz: no persistir el número (o cifrarlo con la llave versionada de `documento_identidad`); PK UUID; DECIMAL + moneda; retención mínima; modelar en el `.puml`.
- CA: Dado un borrador guardado, cuando se lee la tabla, entonces no hay número de documento en claro.
- DoD: integración del borrador; consulta de verificación sobre la tabla; deriva en verde.
- Depende de: F0.L4.M04, F4.L4.M30.
- Esfuerzo: medio

### F4.L4.M24 — Semillas de desarrollo sintéticas
- Hallazgos: IDCU-DATOS-045 (S2)
- Causa de raíz: `SQL/61_dev/15-usuarios-dev.sql` y su JSON fuente tienen datos de apariencia real.
- Corrección de raíz: datos sintéticos (`*.invalid`, teléfonos de rango reservado); purga del historial si Pablo lo decide.
- CA: Dada la semilla dev, cuando se revisa, entonces todo correo termina en `.invalid`.
- DoD: script de verificación sobre el JSON; segunda corrida del seeder sin inserciones (97.4.3).
- Depende de: decisión de Pablo sobre la purga del historial.
- Esfuerzo: chico

### F4.L4.M25 — Semillas regulatorias con procedencia, vigencia real y activas solo tras aprobación
- Hallazgos: IDCU-REG-046 (S2), IDCU-DATOS-128 (S3)
- Causa de raíz: umbrales UIF, plazos, matriz y tipologías sembrados `activa TRUE` con `vigente_desde = current_date` y fuentes «confirmar con legal».
- Corrección de raíz: procedencia por fila (URL, fecha de obtención, responsable), fecha de vigencia de la norma en `seeders/minimos/*.json`, `activa=FALSE` hasta aprobación de Cumplimiento; ajuste de acciones LFT según F0.L4.M03.
- CA: Dado un entorno nuevo, cuando se siembra, entonces las vigencias coinciden con la norma y nada queda activo sin aprobación registrada.
- DoD: verificador de semillas (sin filas sin fuente); segunda corrida idempotente; consulta de vigencias pegada.
- Depende de: AMB-L4-3, AMB-L4-4, F0.L4.M03.
- Esfuerzo: medio

### F4.L4.M26 — Parámetros normativos de código y configuración a catálogo con procedencia
- Hallazgos: IDCU-DATOS-127 (S2)
- Causa de raíz: plazos LFT, de incidente, periodicidades, desvío y requisitos viven en `CU/.../ConfiguracionDeCumplimiento.java` y `application.yml` sin fuente.
- Corrección de raíz: catálogo con `base_normativa`, fuente, fecha y aprobación (como `umbral_reporte_uif`); obligatorios sin valor por defecto (el servicio no arranca sin ellos).
- CA: Dado un arranque sin el catálogo cargado, cuando arranca cumplimiento, entonces falla con el parámetro faltante.
- DoD: prueba de arranque; `grep -n "aportaya.desvio\|caso-lft.dias-plazo" servicios/cumplimiento/src/main/resources/application.yml` → 0.
- Depende de: AMB-L4-3.
- Esfuerzo: medio

### F4.L4.M27 — `debe_fallar` exige el SQLSTATE o el constraint esperado
- Hallazgos: IDCU-PRUEBAS-050 (S3)
- Causa de raíz: `EXCEPTION WHEN others THEN RETURN 'OK'` en `SQL/50_verificacion/prueba_humo.sql:25-33`.
- Corrección de raíz: parámetro de constraint esperado; datos válidos salvo la condición bajo prueba (R-UIF-06 y R-UIF-07).
- CA: Dado un CHECK eliminado, cuando corre la prueba de humo, entonces falla.
- DoD: correr la prueba de humo; quitar un CHECK a propósito y pegar el rojo.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M28 — Moneda junto a cada importe en identidad, cumplimiento y sus contratos
- Hallazgos: IDCU-DATOS-052 (S3), IDCU-DINERO-150 (S3)
- Causa de raíz: tablas y contratos sin `moneda CHAR(3)` y tipo de cambio sin fecha.
- Corrección de raíz: agregar moneda en las tablas listadas en el informe y en `cumplimiento.yaml` (`montoTotal`, `perdidaCuantificada`); fecha del tipo de cambio en `registro_operacion_relevante`; mostrar con `ap-monto`. El campo `limiteDeMonto` de organizador desaparece con F2.L4.M02.
- CA: Dado cualquier importe en estos esquemas, cuando se lee, entonces tiene su moneda.
- DoD: consulta al catálogo de la base: columnas de importe sin columna de moneda hermana → 0; deriva en verde.
- Depende de: F0.L4.M04.
- Esfuerzo: medio

### F4.L4.M29 — `token_verificacion` con catálogos cerrados, idempotencia única y FK completas
- Hallazgos: IDCU-DATOS-053 (S3)
- Causa de raíz: sin CHECK de estado/propósito/canal, `clave_idempotencia` sin UNIQUE, índice duplicado y FK faltantes.
- Corrección de raíz: CHECK o catálogo codificado; índice UNIQUE con nombre; borrar `ix_usuario_telefono_e164`; FK de `alcance_invitacion` y `sesion.refresco_familia_id`.
- CA: Dados dos inserts con la misma clave, cuando se ejecutan, entonces el segundo viola el UNIQUE.
- DoD: prueba SQL de unicidad y de CHECK; deriva en verde.
- Depende de: F0.L4.M04.
- Esfuerzo: chico

### F4.L4.M30 — Cifrado real del documento, HMAC de búsqueda único y claves derivadas por propósito
- Hallazgos: IDCU-SEG-054 (S3), IDCU-DATOS-083 (S2), IDCU-SEG-098 (S3)
- Causa de raíz: `numero_cifrado` guarda `"cifrado:" + hash` (`ID/web/MapeoDelAlta.java:59-62`), el hash es SHA-256 con pimienta y no el HMAC que la regla declara, y una sola pimienta sirve para todo.
- Corrección de raíz: cifrado real con llave versionada (KMS) y HMAC-SHA256 de búsqueda en un único componente compartido; claves derivadas por propósito (HKDF) para clave, documento, OTP y token de alta; migración de los datos existentes (re-captura si el número es irrecuperable).
- CA: Dado un documento registrado, cuando Cumplimiento necesita el número, entonces se descifra con la versión de llave; el hash de búsqueda coincide con `fn_seg_hash_busqueda`.
- DoD: unitarias de ida y vuelta y de rotación de llave; integración del alta; consulta que verifica que ninguna fila empieza con `cifrado:`.
- Depende de: decisión de gestión de llaves (Pablo).
- Esfuerzo: grande

### F4.L4.M31 — Esquemas de seguridad declarados en los OpenAPI
- Hallazgos: IDCU-CONTRATO-055 (S3)
- Causa de raíz: `bearerAuth` referenciado sin `components.securitySchemes`; cumplimiento no declara seguridad.
- Corrección de raíz: declarar esquemas (sesión y token de alta) y la seguridad de cada operación.
- CA: Dado el spec, cuando se valida, entonces es válido y cada operación declara su seguridad.
- DoD: validador OpenAPI en verde para ambos specs.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M32 — Retirar la búsqueda por teléfono en la URL
- Hallazgos: IDCU-CONTRATO-056 (S3), IDCU-SEG-112 (S3)
- Causa de raíz: la ruta deprecada sigue publicada sin fecha de retiro aunque el consumidor ya usa el POST.
- Corrección de raíz: borrar la operación GET de `identidad.yaml`, `ID/web/UsuariosController.java:151-162`, la colección Postman y el ejemplo simulado.
- CA: Dado un GET `/usuarios/por-telefono`, cuando llega, entonces 404.
- DoD: prueba web; `grep -rn "por-telefono" servicios packages postman` → solo el POST.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M33 — Rechazar una orden de retiro no le pone fecha de pago
- Hallazgos: IDCU-DEFECTO-061 (S3)
- Causa de raíz: `pasarA` siempre escribe `pagada_en` (`NF/infraestructura/OrdenRetiroRepositorio.java:171-185`).
- Corrección de raíz: `pagada_en` solo en la transición a PAGADA; CHECK `pagada_en IS NULL OR estado='PAGADA'`.
- CA: Dada una orden rechazada, cuando se lee, entonces `pagada_en` es nulo.
- DoD: integración del rechazo; prueba SQL del CHECK.
- Depende de: coordinar con lote L1.
- Esfuerzo: chico

### F4.L4.M34 — Un solo catálogo de umbrales y reglas de monitoreo
- Hallazgos: IDCU-DUP-INT-063 (S3)
- Causa de raíz: `regla_monitoreo_lft` y `regla_cumplimiento` describen la misma tipología con otros parámetros; `umbral_operativo` duplica `limite_operativo_billetera`.
- Corrección de raíz: una sola tabla por concepto; las reglas refieren `umbral_reporte_uif` por FK.
- CA: Dada una tipología, cuando se busca su parámetro, entonces existe en un solo lugar.
- DoD: consulta de verificación de duplicados; deriva en verde.
- Depende de: F4.L4.M25.
- Esfuerzo: medio

### F4.L4.M35 — CU-11 reescrito para el retiro automático
- Hallazgos: IDCU-COHERENCIA-065 (S3)
- Causa de raíz: el documento promete disponible ≥ monto + costo, antifraude y `factorMfa` que el código no hace.
- Corrección de raíz: reescribir `docs/CasosDeUso/CU-11 Retirar saldo.md` según F3.L4.M07/M08 y el ADR de F0.L4.M01.
- CA: Dado el CU-11, cuando se compara con el código, entonces cada regla tiene su prueba citada.
- DoD: revisión cruzada documentada en el PR.
- Depende de: F3.L4.M08.
- Esfuerzo: chico

### F4.L4.M36 — ADMIN_PLATAFORMA sin funciones incompatibles
- Hallazgos: IDCU-SEG-067 (S3)
- Causa de raíz: la semilla concentra acceso, KYC, reinscripción de factor, fondo, tarifario y catálogo en un rol.
- Corrección de raíz: repartir en roles con pares incompatibles del catálogo (F1.L4.M11); `FONDO_ADMINISTRAR` se redefine o se retira según el modelo A (fondo del grupo, lote L2).
- CA: Dado el rol ADMIN_PLATAFORMA, cuando se validan sus permisos contra `par_incompatible`, entonces no hay conflicto.
- DoD: prueba de integración que recorre todos los roles sembrados contra los pares.
- Depende de: F1.L4.M11.
- Esfuerzo: chico

### F4.L4.M37 — PII de cumplimiento e identidad cifrada o seudonimizada
- Hallazgos: IDCU-DATOS-069 (S3)
- Causa de raíz: `beneficiario_final.documento`, `intento_autenticacion.identificador_usado`, `alcance_invitacion` y `referencia_personal` guardan PII en claro.
- Corrección de raíz: cifrado + HMAC con el componente de F4.L4.M30; el identificador de intento por HMAC; la columna de aval de `referencia_personal` se retira con el aval (P-03, lote L2/L3).
- CA: Dadas esas tablas, cuando se leen, entonces no hay documento ni teléfono en claro.
- DoD: consultas de verificación; deriva en verde.
- Depende de: F4.L4.M30.
- Esfuerzo: medio

### F4.L4.M38 — Documentación generada al día
- Hallazgos: IDCU-DOC-071 (S4)
- Causa de raíz: conteos y matrices escritos a mano que no se regeneran.
- Corrección de raíz: regenerar `seeders/README.md` y `docs/catalogo-de-flujos/matriz.md` desde la fuente; corregir comentarios duplicados de tablas; agregar los flujos KYC y UIF al catálogo.
- CA: Dado el README de seeders, cuando se compara con la semilla, entonces los conteos coinciden.
- DoD: script de regeneración ejecutado y diff vacío en una segunda corrida.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M39 — Bitácora de lectura de datos personales y financieros como componente de plataforma
- Hallazgos: IDCU-SEG-090 (S2), IDCU-SEG-130 (S2)
- Causa de raíz: no hay componente de lectura auditada; ningún endpoint escribe `comun.registro_acceso_datos` aunque el contrato lo promete.
- Corrección de raíz: anotación/interceptor en `plataforma/comun-web` que inserte en `comun.registro_acceso_datos` con justificación para endpoints marcados sensibles; aplicado a expedientes, contacto en grupo, titularidad, reclamos, UIF y casos.
- CA: Dada una lectura de foto del expediente, cuando ocurre, entonces hay una fila de acceso con actor, recurso y justificación.
- DoD: integración por endpoint marcado; prueba de que sin justificación la lectura se rechaza.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M40 — Catálogo de códigos de error por caso de uso en el contrato
- Hallazgos: IDCU-CONTRATO-095 (S3), IDCU-CONTRATO-163 (S4)
- Causa de raíz: no hay catálogo de códigos: AP-CU01-07..11 significan cosas distintas y el backoffice mapea códigos no declarados.
- Corrección de raíz: catálogo de códigos por CU en los specs; prueba que impida duplicados; los clientes mapean desde el contrato.
- CA: Dados dos usos del mismo código con distinto significado, cuando corre la prueba, entonces falla.
- DoD: prueba de unicidad de códigos; build de backoffice con mapeo generado.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M41 — `Idempotency-Key` aplicada con el componente de plataforma en identidad, cumplimiento y el cliente de usuarios internos
- Hallazgos: IDCU-CONTRATO-096 (S3), IDCU-CONTRATO-132 (S3), IDCU-CONTRATO-158 (S3)
- Causa de raíz: el contrato exige la clave y los controladores la reciben y la descartan; el componente `plataforma/comun-web/.../idempotencia/Idempotencia.java` no se usa; el cliente de usuarios internos no la manda.
- Corrección de raíz: usar el componente en todas las mutaciones con clave; el cliente del backoffice la envía; `403` tipado correctamente. La paginación de la colección se trata en F4.L4.M43.
- CA: Dado un reintento con la misma clave, cuando llega, entonces la respuesta es la misma y no hay segunda fila ni segundo evento.
- DoD: prueba de idempotencia por mutación (alta, subida, decisión, alta de interno, desafío, CU-02, CU-54, aceptación de contrato, incidentes).
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M42 — Capas limpias en identidad y sin «contexto de sistema» con el usuario de la sesión
- Hallazgos: IDCU-ARQ-102 (S3)
- Causa de raíz: aplicación importa modelos de `web.generado`; SQL crudo en aplicación; `ContextoSesion.deSistema(ctx.usuarioId())` apaga RLS con identidad del usuario.
- Corrección de raíz: puertos de lectura acotados; regla ArchUnit `aplicacion` ↛ `web`; contexto de sistema solo con actor de sistema.
- CA: Dada una importación de `web.generado` en `aplicacion`, cuando corren las pruebas, entonces ArchUnit falla.
- DoD: prueba ArchUnit en verde y en rojo con una violación provocada.
- Depende de: F1.L4.M19.
- Esfuerzo: medio

### F4.L4.M43 — Listados paginados y consulta por id en identidad
- Hallazgos: IDCU-RENDIMIENTO-104 (S3), IDCU-DATOS-148 (S2)
- Causa de raíz: el contrato no tiene `GET /identidad/verificaciones/{id}` ni cursor/límite; la ficha KYC descarga la cola completa; N+1 en usuarios internos; `orElseThrow` → 500.
- Corrección de raíz: cursor/límite y filtro multi-estado en servidor; consulta por id; consultas sin N+1; respuesta de la decisión desde el propio resultado (sin relectura). También la colección de usuarios internos.
- CA: Dada una cola de 10 000 expedientes, cuando se abre una ficha, entonces se hace una sola consulta por id.
- DoD: integración de paginación con orden determinista; prueba de cantidad de consultas.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M44 — Sin valores inventados presentados como reales
- Hallazgos: IDCU-DATOS-106 (S3)
- Causa de raíz: fecha de nacimiento 1990-01-01, locale distinto, `nivel_diligencia = "SIMPLIFICADA"` fijo en todo token, `"desconocido"` en dispositivo.
- Corrección de raíz: campos opcionales nulos cuando no se conocen; nivel de diligencia leído de cumplimiento (o fuera del token); un único locale.
- CA: Dado un usuario con diligencia reforzada, cuando inicia sesión, entonces su token no dice SIMPLIFICADA.
- DoD: integración de sesión con distintos niveles; `grep -n "LocalDate.of(1990" servicios/identidad` → 0.
- Depende de: F3.L4.M02.
- Esfuerzo: chico

### F4.L4.M45 — La IP registrada es la del cliente, no la del gateway
- Hallazgos: IDCU-TRAZA-107 (S3)
- Causa de raíz: sin `server.forward-headers-strategy`; se usa `getRemoteAddr()`.
- Corrección de raíz: estrategia de cabeceras reenviadas con lista de proxies confiables en `plataforma/comun-web`, aplicada a identidad y cumplimiento.
- CA: Dada una petición con `X-Forwarded-For` desde el gateway confiable, cuando se registra un intento, entonces la IP es la del cliente; desde otro origen, la cabecera se ignora.
- DoD: prueba web con y sin proxy confiable.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M46 — Efectos externos después del commit
- Hallazgos: IDCU-RESILIENCIA-108 (S3)
- Causa de raíz: el correo del login se envía dentro de `@Transactional` (`CU04Autenticar.java:165`) y `ClienteDeServicio.blindar` registra cuerpos 4xx; la saga de `ActivarRolOrganizador` desaparece con F2.L4.M02.
- Corrección de raíz: envío por outbox después del commit; log de errores sin cuerpo.
- CA: Dado un rollback del login, cuando termina, entonces no salió ningún correo.
- DoD: integración con rollback forzado y doble del correo.
- Depende de: F2.L4.M02.
- Esfuerzo: chico

### F4.L4.M47 — README, descriptor y OpenAPI que dicen lo que existe
- Hallazgos: IDCU-DOC-110 (S3), IDCU-DOC-142 (S3)
- Causa de raíz: documentación escrita como intención y no regenerada.
- Corrección de raíz: corregir descripciones de `registrarUsuario`, cabecera, eventos emitidos/consumidos y estado de los CU en los README y descriptores de identidad y cumplimiento, después de F3.
- CA: Dado el README, cuando se compara con los temas de outbox reales, entonces coinciden.
- DoD: script que lista temas publicados y consumidos y los compara con el README (diff vacío).
- Depende de: F3.L4.M02, F4.L4.M54.
- Esfuerzo: chico

### F4.L4.M48 — Pruebas de identidad que ejercitan los controles reales
- Hallazgos: IDCU-PRUEBAS-111 (S3)
- Causa de raíz: casos de uso con `@MockitoBean`, step-up con doble, solo felices + 401/403, pruebas que escriben en esquemas ajenos.
- Corrección de raíz: integración con base real, rol del servicio y RLS; matriz negativa completa en usuarios internos; R-SEG-09 probado con datos, no con la existencia del constraint.
- CA: Dado un control roto (por ejemplo, login de suspendido), cuando corre la suite, entonces una prueba falla.
- DoD: suite en verde y en rojo con el control roto a propósito.
- Depende de: F1.L4.M01.
- Esfuerzo: medio

### F4.L4.M49 — `actualizarFechaExpiracionDocumento` coherente y acotado a un documento
- Hallazgos: IDCU-OTRO-113 (S4)
- Causa de raíz: marcado `@Publico` pero exige sesión; actualiza todos los documentos del titular.
- Corrección de raíz: seguridad declarada coherente; actualizar solo el documento de la verificación vigente.
- CA: Dado un titular con dos documentos, cuando actualiza la fecha, entonces cambia uno solo.
- DoD: integración.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M50 — `ConsultarNivelKyc` autoriza por permiso, no por un rol que el token nunca trae
- Hallazgos: IDCU-CLEAN-114 (S4)
- Causa de raíz: compara contra `"ADMIN_PLATAFORMA"` como string de rol.
- Corrección de raíz: `@Permiso` con un permiso del catálogo.
- CA: Dado un operador con el permiso, cuando consulta, entonces accede; sin él, 403.
- DoD: autorización negativa.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M51 — Envío de correo con token OAuth cacheado y cabecera saneada
- Hallazgos: IDCU-RENDIMIENTO-115 (S4)
- Causa de raíz: `GmailApiCorreoDeVerificacion` pide token en cada envío y arma `To:` sin sanear.
- Corrección de raíz: cache del token hasta su vencimiento; validación de la dirección y armado con la API de MIME.
- CA: Dados dos envíos seguidos, cuando se mandan, entonces hay un solo pedido de token; una dirección con salto de línea se rechaza.
- DoD: unitaria con servidor simulado.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M52 — Orden de bloqueo de autoridad ejecutada como comando idempotente y lectura bajo oficio auditada
- Hallazgos: IDCU-FLUJO-FALTANTE-118 (S2)
- Causa de raíz: `CU/aplicacion/CU45AtenderRequerimiento.java:104-137` delega en eventos sin consumidor y sin los datos mínimos; `anotarBloqueo` no tiene llamador.
- Corrección de raíz: comando a nucleo-financiero con clave = id del requerimiento por canal autenticado entre servicios (saga con estado y compensación); escribir `comun.registro_acceso_datos` en cada lectura bajo oficio. Se conserva por norma (APR-11).
- CA: Dado un oficio con orden de bloqueo, cuando se registra, entonces el saldo del afectado queda bloqueado una sola vez aunque el comando se repita.
- DoD: integración entre servicios: duplicado, nucleo-financiero caído y recuperado, oficio inexistente; consulta del bloqueo.
- Depende de: F4.L4.M39; coordinar con lote L1.
- Esfuerzo: grande

### F4.L4.M53 — Declaración PEP sin DELETE: beneficiarios versionados y escritura permitida por RLS
- Hallazgos: IDCU-DEFECTO-121 (S2)
- Causa de raíz: modelo «foto completa» con borrado y pruebas que no usan el rol de producción (`BaseDeCumplimiento.java:131-135`).
- Corrección de raíz: beneficiarios con vigencia (append-only); política RLS de escritura del titular sobre su declaración (o recalificación como sistema); pruebas con `SET ROLE svc_cumplimiento` + `app.rol`.
- CA: Dada una declaración nueva, cuando se guarda, entonces la anterior queda con fin de vigencia y no se borra nada.
- DoD: integración con el rol de producción.
- Depende de: F1.L4.M08, F1.L4.M24.
- Esfuerzo: medio

### F4.L4.M54 — Consumidores de los eventos de cumplimiento en sus dueños
- Hallazgos: IDCU-FLUJO-FALTANTE-123 (S2)
- Causa de raíz: ~30 eventos sin consumidor ni prueba de contrato; payloads con la traza en vez del agregado.
- Corrección de raíz: por evento, decidir dueño y consumidor idempotente (limitación de cuenta y nivel de diligencia → nucleo-financiero; consentimientos → notificaciones; casos LFT → solo cumplimiento) o dejar de publicarlo; payload con usuario/cuenta; pacto por evento.
- CA: Dado un evento de limitación de cuenta, cuando se publica dos veces, entonces la cuenta queda LIMITADA una vez.
- DoD: prueba de duplicado y de caída del consumidor por evento conservado; lista de eventos retirados.
- Depende de: F0.L4.M05.
- Esfuerzo: grande

### F4.L4.M55 — Archivo del reporte regulatorio generado y guardado; envío con constancia
- Hallazgos: IDCU-HUECO-124 (S2)
- Causa de raíz: la integración con la UIF no existe y se simula con datos del operador (URL derivada del dominio público).
- Corrección de raíz: generar el archivo en almacenamiento privado con hash; registrar el envío con la constancia del canal real o un estado `PENDIENTE_CONSTANCIA` separado.
- CA: Dado un reporte aprobado, cuando se envía, entonces existe el archivo con su hash y el estado no es ENVIADO sin constancia.
- DoD: integración con almacenamiento local de prueba; hash verificado.
- Depende de: F1.L4.M07.
- Esfuerzo: medio

### F4.L4.M56 — El filtro del reporte sale del catálogo, no de la entrada
- Hallazgos: IDCU-DEFECTO-125 (S2)
- Causa de raíz: `catalogoCodigo` y `formulario` independientes en la entrada.
- Corrección de raíz: derivar el filtro de `catalogo_reporte_regulatorio`; quitar `formulario` del contrato (versión nueva).
- CA: Dado un PCC-01 de un mes, cuando se genera, entonces solo contiene registros PCC-01.
- DoD: integración con registros mixtos.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M57 — Hallazgos de gobierno con código único derivado e inserción atómica
- Hallazgos: IDCU-DEFECTO-126 (S2)
- Causa de raíz: SELECT-then-INSERT y códigos truncados que colisionan; dos repositorios de hallazgos.
- Corrección de raíz: código derivado del id de origen; `INSERT … ON CONFLICT DO NOTHING RETURNING`; un solo repositorio.
- CA: Dado un envío tardío de un reporte con un hallazgo cerrado previo, cuando se registra, entonces no hay 500 y no se revierte el envío.
- DoD: integración con el caso de colisión y concurrente.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M58 — Controles de vencimiento como trabajos programados con visibilidad
- Hallazgos: IDCU-HUECO-129 (S2)
- Causa de raíz: `vencidosSinEnviar`, requerimientos, reclamos, alertas sin analista y continuidad son consultas sin disparador.
- Corrección de raíz: trabajos idempotentes con ShedLock (como CU-54/55), última ejecución visible y alerta si no corren.
- CA: Dado un reporte UIF vencido sin enviar, cuando corre el trabajo diario, entonces se abre un hallazgo una sola vez.
- DoD: integración del trabajo corrido dos veces; prueba de visibilidad.
- Depende de: F4.L4.M57.
- Esfuerzo: medio

### F4.L4.M59 — Elevar el tipo de diligencia solo en la vigente
- Hallazgos: IDCU-DEFECTO-131 (S3)
- Causa de raíz: `DiligenciaRepositorio.elevarTipo` filtra solo por usuario.
- Corrección de raíz: filtrar por id; `vigenteDe` por estado y vigencia; historia append-only.
- CA: Dadas dos diligencias, cuando se eleva, entonces la histórica no cambia.
- DoD: integración.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M60 — Una sola implementación para cada cálculo regulatorio
- Hallazgos: IDCU-DUP-INT-134 (S3)
- Causa de raíz: CU-41 no usa `UmbralAlcanzado`; dos formas de abrir alertas y hallazgos; CU-94 aprueba sin la no objeción que exige CU-47.
- Corrección de raíz: CU-41 usa `UmbralAlcanzado`; borrar duplicados; CU-94 reutiliza la verificación de CU-47.
- CA: Dado un producto sin no objeción, cuando se eleva al comité, entonces no se aprueba.
- DoD: unitarias del umbral compartido; integración de CU-94.
- Depende de: F3.L4.M09.
- Esfuerzo: medio

### F4.L4.M61 — Evidencia y parseos robustos en cumplimiento
- Hallazgos: IDCU-FRAGIL-138 (S3)
- Causa de raíz: IP del gateway en la evidencia de aceptación, JSON por regex, `LocalDate.now()` de la JVM, objetos fuera de DI, NPE con `ventanaDias` nulo.
- Corrección de raíz: IP por F4.L4.M45; Jackson para JSON; `Reloj` en La Paz; inyección; validación de nulos en el catálogo.
- CA: Dada una aceptación de contrato, cuando se sella, entonces la IP es la del firmante y la fecha la de La Paz.
- DoD: unitarias por punto.
- Depende de: F4.L4.M45.
- Esfuerzo: chico

### F4.L4.M62 — Escalado de incidentes vencidos sin inanición
- Hallazgos: IDCU-DEFECTO-139 (S3)
- Causa de raíz: los incidentes ya escalados no se marcan y la ventana se reselecciona.
- Corrección de raíz: marcar el incidente escalado (como CU-54) y excluirlo.
- CA: Dados más incidentes vencidos que el tope, cuando corre el trabajo varios días, entonces todos se escalan.
- DoD: integración con tope chico.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M63 — Reserva: el titular no ve su riesgo ni la investigación
- Hallazgos: IDCU-DEBIDO-PROCESO-140 (S3)
- Causa de raíz: `nivelRiesgo` devuelto al participante, coincidencias de lista mostradas al analista KYC, evento `caso_lft_abierto` con `usuarioId`.
- Corrección de raíz: respuestas por vista sin nivel de riesgo; mensajes genéricos fuera del equipo LFT; evento sin dato personal.
- CA: Dado un participante, cuando consulta su adhesión, entonces no ve su nivel de riesgo.
- DoD: prueba de contrato de la respuesta; inspección del payload del evento.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M64 — Pruebas de cumplimiento con títulos ciertos y rol de producción
- Hallazgos: IDCU-PRUEBAS-141 (S2)
- Causa de raíz: pruebas plantilla («cuadre», «misma clave») que no verifican lo que dicen y conexión como dueño sin GRANT ni RLS.
- Corrección de raíz: reescribir o renombrar las 36 pruebas plantilla; `BaseDeCumplimiento` con `SET ROLE svc_cumplimiento` + `app.rol`.
- CA: Dada la suite, cuando se rompe el GRANT de una tabla, entonces una prueba falla.
- DoD: suite en verde y rojo provocado; `grep -rn "cuadre: la suma de debitos" servicios/cumplimiento/src/test` → 0.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M65 — Listados de cumplimiento con límite y alertas idempotentes
- Hallazgos: IDCU-RENDIMIENTO-143 (S4)
- Causa de raíz: `casosDelUsuario` sin paginación, alerta nueva por corrida, 500 por `IllegalArgumentException`, huella de 24 bits como unicidad.
- Corrección de raíz: paginación; alerta por clave natural (usuario, mes); error de contrato; clave de unicidad completa.
- CA: Dadas dos corridas del desvío del mismo mes, cuando terminan, entonces hay una sola alerta.
- DoD: integración.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M66 — Ficha del caso LFT contra el contrato real y sin datos de ejemplo
- Hallazgos: IDCU-DATOS-144 (S2)
- Causa de raíz: se mezcló el patrón de debido proceso de sanciones con el caso LFT y quedó un tipo de dominio con caso `demo` y causales inventadas.
- Corrección de raíz: `GET /cumplimiento/casos/{id}` (sin datos de la persona) en el contrato y la ficha lo usa; sin contrato, `ap-indisponible`; borrar `CATALOGO_DE_CAUSALES` y `etapasDelCaso`; ruta con `canMatch`.
- CA: Dado un id inexistente, cuando se abre la ficha, entonces muestra «no disponible», nunca un caso de ejemplo.
- DoD: prueba de componente; `grep -n "'demo'" BO/cumplimiento/casos` → 0; capturas.
- Depende de: ninguna.
- Esfuerzo: medio

### F4.L4.M67 — Narrativas LFT fuera del almacenamiento del navegador
- Hallazgos: IDCU-SEG-145 (S2)
- Causa de raíz: borrador genérico del shell reutilizado para datos reservados en IndexedDB.
- Corrección de raíz: no persistir narrativas LFT en el cliente (memoria de sesión o borrador en servidor con control de acceso); purga al cerrar sesión.
- CA: Dado un analista que escribe una narrativa y cierra sesión, cuando otro abre el navegador, entonces no hay rastro en IndexedDB.
- DoD: prueba de componente; inspección de IndexedDB en Playwright tras logout.
- Depende de: F4.L4.M66.
- Esfuerzo: chico

### F4.L4.M68 — Aprobar y enviar un reporte UIF por su id
- Hallazgos: IDCU-DEFECTO-147 (S2)
- Causa de raíz: el contrato identifica por (periodo, catálogo tecleado) y el componente no resetea su estado.
- Corrección de raíz: `POST /uif/reportes/{reporteId}/envios` (versión nueva); resetear las señales al cambiar el reporte. El aprobador sale del token (F1.L4.M07).
- CA: Dado un reporte elegido, cuando se aprueba, entonces se aprueba ese y no otro, sin datos del anterior.
- DoD: integración del endpoint; prueba de componente con cambio de reporte.
- Depende de: F1.L4.M07.
- Esfuerzo: chico

### F4.L4.M69 — Exportación con permiso por recurso y apagada por omisión
- Hallazgos: IDCU-SEG-151 (S3), IDCU-SEG-171 (S4)
- Causa de raíz: el kit exporta por omisión (`packages/ui/src/listado/listado.ts:50`) y cuatro colas comparten un permiso.
- Corrección de raíz: `exportable` por omisión `false`; `exportador.puedeExportar(recurso)` por cola y por pantalla de accesos/roles/permisos.
- CA: Dado un operador sin permiso de exportar accesos, cuando abre la revisión de accesos, entonces no ve el botón.
- DoD: pruebas de componente; la exportación en servidor también verifica el permiso (autorización negativa).
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M70 — Fotos de cédula sin suscripciones colgadas
- Hallazgos: IDCU-FRAGIL-152 (S3)
- Causa de raíz: `subscribe` sin `takeUntilDestroyed` en `BO/cumplimiento/verificaciones/tira-de-fotos.ts`.
- Corrección de raíz: `takeUntilDestroyed` + cancelar en `soltar()`; descartar respuestas si ya no está abierto.
- CA: Dado un cierre rápido de la tira, cuando llega la respuesta, entonces no se crea `blob:`.
- DoD: prueba de componente contando `createObjectURL`.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M71 — Decisión de excepción KYC con confirmación y error en el campo
- Hallazgos: IDCU-UX-A11Y-153 (S3)
- Causa de raíz: un clic decide; motivo sin tope; 422 como error general.
- Corrección de raíz: diálogo de confirmación con consecuencia; `maximo=160`; mapear 422 al campo (la pantalla sigue para las excepciones y PEP, P-08).
- CA: Dado un motivo de 161 caracteres, cuando se intenta, entonces el campo lo marca.
- DoD: prueba de componente; capturas en tres anchos.
- Depende de: F3.L4.M01.
- Esfuerzo: chico

### F4.L4.M72 — Cola KYC sin contacto personal en la fila
- Hallazgos: IDCU-SEG-154 (S3)
- Causa de raíz: la fila muestra teléfono y correo, que no se necesitan para decidir.
- Corrección de raíz: quitar o enmascarar en la lista y en la respuesta del listado; contacto solo en la ficha.
- CA: Dada la cola, cuando se ve, entonces no hay teléfono ni correo completos.
- DoD: prueba de contrato del listado; prueba de componente.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M73 — «Responder requerimiento» sin arrastrar el enlace de otro oficio
- Hallazgos: IDCU-DEFECTO-155 (S3)
- Causa de raíz: `respuestaUrl` solo se limpia tras éxito.
- Corrección de raíz: resetear al cambiar el requerimiento y al cancelar.
- CA: Dado un enlace escrito y cancelado, cuando se abre otro oficio, entonces el campo está vacío.
- DoD: prueba de componente.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M74 — Abrir caso con titular seudónimo y selector de analista
- Hallazgos: IDCU-HUECO-156 (S3)
- Causa de raíz: la fila de alertas no trae titular y el analista se pega como UUID.
- Corrección de raíz: identificador opaco de titular en la fila (seudónimo) o agrupación asistida por servidor; el analista es quien toma el caso (F1.L4.M06), sin campo libre.
- CA: Dadas alertas de dos titulares, cuando se intenta abrir un caso, entonces la pantalla lo impide antes de enviar.
- DoD: prueba de componente; contrato actualizado.
- Depende de: F1.L4.M06.
- Esfuerzo: chico

### F4.L4.M75 — Doble control para asignar roles críticos
- Hallazgos: IDCU-REG-157 (S3)
- Causa de raíz: un solo administrador otorga `OFICIAL_CUMPLIMIENTO` u otros roles críticos.
- Corrección de raíz: maker-checker para roles marcados críticos: propuesta y aprobación por dos actores distintos tomados del token.
- CA: Dado un administrador que propone un rol crítico, cuando él mismo intenta aprobarlo, entonces se rechaza.
- DoD: autorización negativa e integración.
- Depende de: F1.L4.M10, F1.L4.M11.
- Esfuerzo: medio

### F4.L4.M76 — Rutas de detalle del backoffice con la misma guardia que su bandeja
- Hallazgos: IDCU-FRAGIL-160 (S3)
- Causa de raíz: rutas de detalle sin `canMatch` (`BO/cumplimiento/cumplimiento.routes.ts`); la de organizadores desaparece con F2.L4.M02.
- Corrección de raíz: `canMatch: [guardiaDe(...)]` en cada detalle (no es seguridad; el servidor decide).
- CA: Dado un operador sin acceso a casos, cuando navega a `casos/:id`, entonces no se monta la pantalla.
- DoD: prueba de rutas.
- Depende de: F2.L4.M02.
- Esfuerzo: chico

### F4.L4.M77 — Borrar componentes muertos del backoffice de cumplimiento y sus pruebas
- Hallazgos: IDCU-CLEAN-161 (S4)
- Causa de raíz: componentes y funciones sin uso cubiertos por pruebas que inflan la cobertura.
- Corrección de raíz: borrar `alertas/pantalla-de-alertas.ts`, `actas/pantalla-de-acta.ts`, `reclamos/pantalla-de-reclamo.ts`, `vencimiento-del-documento.ts`, el componente de `compartido/navegacion.ts` y funciones sin uso, con sus specs.
- CA: Dado el backoffice, cuando se buscan esos símbolos, entonces no existen.
- DoD: build y suite de backoffice; `grep` → 0.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M78 — Comentarios del backoffice alineados con el contrato
- Hallazgos: IDCU-DOC-162 (S4)
- Causa de raíz: comentarios escritos antes de que existieran las rutas.
- Corrección de raíz: corregir o borrar los comentarios listados (los de organizador y retiros desaparecen con F2).
- CA: Dado cada comentario citado, cuando se compara con el spec, entonces no lo contradice.
- DoD: revisión en el PR con la lista.
- Depende de: F2.L4.M01, F2.L4.M02.
- Esfuerzo: chico

### F4.L4.M79 — Diccionarios de estados generados desde el contrato
- Hallazgos: IDCU-DUP-INT-164 (S4)
- Causa de raíz: enums copiados a mano.
- Corrección de raíz: textos indexados por los enums generados; prueba que exija texto para cada valor.
- CA: Dado un valor nuevo en el contrato, cuando se compila, entonces falla hasta tener su texto.
- DoD: build de backoffice; prueba de exhaustividad.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M80 — Menú «Más» de usuarios internos con el componente del kit
- Hallazgos: IDCU-UX-A11Y-165 (S3)
- Causa de raíz: overlay hecho a mano sin rol de menú ni restauración de foco.
- Corrección de raíz: usar el menú del kit (`packages/ui`) con `role="menu"`, flechas y foco devuelto.
- CA: Dado el teclado, cuando se abre y se elige una acción, entonces el foco vuelve al disparador.
- DoD: prueba de accesibilidad de teclado; capturas.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M81 — Ficha de usuario interno con textos y fechas legibles
- Hallazgos: IDCU-UX-A11Y-166 (S4)
- Causa de raíz: códigos, ISO y booleanos crudos; id por snapshot.
- Corrección de raíz: textos por enum, fecha local, «Sí/No»; id por observable de ruta.
- CA: Dado un usuario bloqueado, cuando se ve la ficha, entonces la fecha es legible.
- DoD: prueba de componente.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M82 — Backoffice: tipografía, tiempos y medidas por tokens
- Hallazgos: IDCU-UX-A11Y-167 (S4)
- Causa de raíz: literales en estilos.
- Corrección de raíz: tokens del sistema de diseño; animaciones con «reducir movimiento».
- CA: Dadas las ubicaciones citadas, cuando se buscan literales, entonces no aparecen.
- DoD: `grep` de literales → 0; capturas sin cambio visual.
- Depende de: F2.L4.M01 (parte de retiros desaparece).
- Esfuerzo: chico

### F4.L4.M83 — Sin `$any` ni `as` en el borde del contrato
- Hallazgos: IDCU-CLEAN-168 (S4)
- Causa de raíz: casteos que ocultan respuestas distintas.
- Corrección de raíz: tipos generados y validación de la respuesta.
- CA: Dado un 201 sin cuerpo, cuando se procesa, entonces se maneja como error tipado.
- DoD: lint con la regla `no-explicit-any` y sin `as` en esos archivos.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M84 — Parámetros codificados y enlaces con esquema validado
- Hallazgos: IDCU-SEG-169 (S4)
- Causa de raíz: interpolación de parámetros en URLs; `documentoUrl` acepta `javascript:`.
- Corrección de raíz: `HttpParams`/`encodeURIComponent`; validación de esquema `https:` en el cliente y en el servidor.
- CA: Dado un enlace `javascript:`, cuando se guarda, entonces el servidor lo rechaza.
- DoD: prueba de validación en servidor y cliente.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M85 — Sin importaciones cruzadas entre dominios del backoffice
- Hallazgos: IDCU-ACOPLE-170 (S4)
- Causa de raíz: piezas compartidas viven dentro de un dominio.
- Corrección de raíz: mover a un módulo compartido; regla de lint de fronteras.
- CA: Dada una importación cruzada, cuando corre el lint, entonces falla.
- DoD: lint en verde y rojo provocado.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M86 — Huella de dispositivo estable en el ingreso del backoffice
- Hallazgos: IDCU-OBSERV-172 (S4)
- Causa de raíz: huella aleatoria por pestaña.
- Corrección de raíz: identificador persistente por navegador (no por pestaña).
- CA: Dados dos ingresos desde el mismo navegador, cuando se registran, entonces es el mismo dispositivo.
- DoD: prueba de dominio.
- Depende de: ninguna.
- Esfuerzo: chico

### F4.L4.M87 — Pruebas del backoffice sin esperas fijas y con integración real marcada
- Hallazgos: IDCU-PRUEBAS-175 (S4)
- Causa de raíz: `setTimeout(r, 50)` y solo HTTP simulado.
- Corrección de raíz: espera por condición; agregar E2E Playwright contra el backend real para KYC de excepción, UIF y casos, etiquetando las de HTTP simulado como tales.
- CA: Dada la suite, cuando se busca `setTimeout` en specs, entonces no aparece.
- DoD: `grep -rn "setTimeout" BO/cumplimiento --include=*.spec.ts` → 0; E2E dirigido en verde.
- Depende de: entorno Docker de F0.
- Esfuerzo: medio

### F4.L4.M88 — Cálculos de plantilla precalculados
- Hallazgos: IDCU-RENDIMIENTO-176 (S4)
- Causa de raíz: `new Date()` y celdas calculadas por ciclo.
- Corrección de raíz: `computed` por fila; la bandeja de retiros desaparece con F2.L4.M01.
- CA: Dada una cola de 500 filas, cuando cambia la detección, entonces no se recalculan las celdas.
- DoD: prueba de componente contando llamadas.
- Depende de: F2.L4.M01.
- Esfuerzo: chico

### F4.L4.M89 — Cliente Dart: o se versiona entero o se genera entero
- Hallazgos: IDCU-CONFIG-074 (S4)
- Causa de raíz: barrel y pubspec versionados mientras `src/` está ignorado (`.gitignore:51`).
- Corrección de raíz: dejar de versionar el barrel (todo generado en el build) o versionar el cliente completo, con un chequeo de deriva barrel↔OpenAPI.
- CA: Dado un checkout limpio, cuando se compila la app tras el paso de generación, entonces no hay exportaciones rotas.
- DoD: build desde checkout limpio pegado.
- Depende de: ninguna.
- Esfuerzo: chico

## F5 — Piloto «Poder de compra»

Ningún hallazgo de L4 toca algo reutilizable por el piloto. Sin microtareas.

## Trazabilidad hallazgo → microtarea

Las 176 fichas del informe, en su orden. Verificado por script: 176 fichas en el informe, 176 trazadas, 0 duplicadas, 0 sin trazar.

| # | Hallazgo | Sev. | Título (abreviado) | Microtarea | Tipo |
|---|---|---|---|---|---|
| 1 | IDCU-REESCRITURA-001 | S2 | CU-40 «Evaluar límites» existe dos veces; la copia de cumplimiento está muerta y diverg… | F4.L4.M01 | corrección |
| 2 | IDCU-ACOPLE-002 | S3 | Cumplimiento lee tablas del esquema `nucleo_financiero` | F4.L4.M01 | se resuelve con F4.L4.M01 |
| 3 | IDCU-DEFECTO-003 | S3 | La fecha de vigencia de los límites se calcula en UTC, no en `America/La_Paz` | F4.L4.M01 | se resuelve con F4.L4.M01 |
| 4 | IDCU-SEG-004 | S2 | TEST corre con perfil `local`: segundo factor «000000» para cualquiera y clave JWT en m… | F1.L4.M03 | corrección |
| 5 | IDCU-SEG-005 | S3 | Los servidores de recurso no validan emisor ni audiencia; el token de acceso no los lleva | F1.L4.M15 | corrección |
| 6 | IDCU-DOC-006 | S4 | El cliente de aprobación de retiros dice que no existe la bandeja que el archivo vecino… | F2.L4.M01 | se resuelve con F2.L4.M01 |
| 7 | IDCU-RESPALDO-007 | S2 | El tour promete que «un fondo de garantía cubre la cuota» de quien no paga | F3.L4.M11 | corrección |
| 8 | IDCU-APROB-008 | S2 | Todo el circuito de identidad del cliente asume aprobación humana de KYC como compuerta… | F3.L4.M03 | corrección |
| 9 | IDCU-HUECO-009 | S2 | El perfil transaccional (origen de fondos, actividad, monto) se pide y nunca se registra | F3.L4.M05 | corrección |
| 10 | IDCU-HUECO-010 | S2 | La declaración PEP se guarda solo en memoria y se pierde | F3.L4.M06 | corrección |
| 11 | IDCU-HUECO-011 | S2 | «Cambiar contraseña» no cambia nada y no avisa | F4.L4.M19 | corrección |
| 12 | IDCU-HUECO-012 | S2 | «Confirmar baja» solo cierra la hoja; el contrato promete cerrar la cuenta «desde la app» | F4.L4.M20 | corrección |
| 13 | IDCU-COHERENCIA-013 | S2 | Se acepta por id un contrato del servidor pero se muestra un texto y un tarifario fijos… | F4.L4.M21 | corrección |
| 14 | IDCU-HUECO-014 | S2 | Fotos del expediente que fallaron se borran al salir y no hay camino para re-subir anve… | F3.L4.M04 | corrección |
| 15 | IDCU-SEG-015 | S2 | Prueba de vida y cotejo son escenografía en el cliente | F3.L4.M01 | corrección |
| 16 | IDCU-DINERO-016 | S3 | Monto mensual estimado como `double`, sin moneda en el contrato, 'Bs' literal | F3.L4.M05 | corrección |
| 17 | IDCU-CONTRATO-017 | S3 | Seis operaciones del spec se llaman por Dio crudo con comentarios obsoletos y casteos s… | F4.L4.M02 | corrección |
| 18 | IDCU-COHERENCIA-018 | S3 | Vocabularios de tipo de documento y teléfono divergentes; coerción silenciosa a CI | F4.L4.M03 | corrección |
| 19 | IDCU-HUECO-019 | S3 | El alta solo admite CI boliviano | F4.L4.M04 | corrección |
| 20 | IDCU-FRAGIL-020 | S3 | Catch que tragan y estados que quedan colgados | F4.L4.M05 | corrección |
| 21 | IDCU-DEFECTO-021 | S3 | Clave de idempotencia de «completar KYC» nunca rota | F4.L4.M06 | corrección |
| 22 | IDCU-SEG-022 | S3 | Credenciales: backup Android por defecto y candado biométrico solo visual | F4.L4.M07 | corrección |
| 23 | IDCU-RESILIENCIA-023 | S3 | Refresco de sesión sin timeouts y con dos refrescadores no coordinados | F4.L4.M08 | corrección |
| 24 | IDCU-UX-A11Y-024 | S3 | Errores crudos y estados vacíos mudos | F4.L4.M09 | corrección |
| 25 | IDCU-HUECO-025 | S3 | «Confiar en este dispositivo por 30 días» no se envía; recuperación de contraseña inexi… | F4.L4.M19 | corrección |
| 26 | IDCU-FRAGIL-026 | S3 | Rutas que revientan con enlace profundo o sin `extra` | F4.L4.M10 | corrección |
| 27 | IDCU-UX-A11Y-027 | S3 | Botón de solo icono sin nombre accesible | F4.L4.M11 | corrección |
| 28 | IDCU-PRUEBAS-028 | S3 | Los tests fijan los huecos como comportamiento correcto | F4.L4.M12 | corrección |
| 29 | IDCU-DEFECTO-029 | S4 | El sondeo del estado KYC se rinde a los 40 s frente a una revisión humana | F3.L4.M03 | se resuelve con F3.L4.M03 |
| 30 | IDCU-DUP-INT-030 | S4 | Código muerto y duplicado | F4.L4.M13 | corrección |
| 31 | IDCU-CLEAN-031 | S4 | Literales de tamaño y textos fuera de los archivos de textos | F4.L4.M14 | corrección |
| 32 | IDCU-UX-A11Y-032 | S4 | Movimiento no condicionado a «reducir movimiento» en dos puntos | F4.L4.M15 | corrección |
| 33 | IDCU-FRAGIL-033 | S4 | La cámara devuelve dos formas distintas | F4.L4.M16 | corrección |
| 34 | IDCU-TRAZA-034 | S4 | `tokenDeAltaVenceEn` ignorado y provider KYC sin invalidación por sesión | F4.L4.M17 | corrección |
| 35 | IDCU-REG-035 | S4 | Validaciones de negocio duplicadas en el cliente | F4.L4.M18 | corrección |
| 36 | IDCU-FLUJO-FALTANTE-036 | S2 | Un retiro AUTORIZADO nunca se instruye al proveedor en producción | F3.L4.M07 | corrección |
| 37 | IDCU-DINERO-037 | S2 | El titular puede liberar la retención de su propio retiro mientras la orden sigue viva | F1.L4.M21 | corrección |
| 38 | IDCU-SEG-038 | S2 | Las políticas de fila dan ESCRITURA al titular sobre sus roles, su KYC, su reputación, … | F1.L4.M08 | corrección |
| 39 | IDCU-DEFECTO-039 | S2 | `ck_reporte_segregacion` se salta con `generado_por` NULL y no exige `revisado_por` | F1.L4.M07 | corrección |
| 40 | IDCU-HUECO-040 | S2 | R-UIF-09 (diligencia vigente) y R-CON-06 (contrato aceptado) no los invoca ninguna oper… | F4.L4.M22 | corrección |
| 41 | IDCU-DEFECTO-041 | S2 | El trigger de encaje bloquea también el pago de órdenes ya en curso y no filtra por moneda | F1.L4.M22 | corrección |
| 42 | IDCU-DINERO-042 | S2 | El pago del retiro no registra la línea del costo | F1.L4.M23 | corrección |
| 43 | IDCU-APROB-043 | S2 | Umbral de doble aprobación sin moneda, sin procedencia, por operación y decidido por el… | F3.L4.M08 | corrección |
| 44 | IDCU-SEG-044 | S2 | `borrador_onboarding` guarda el número de documento en claro y el monto como texto | F4.L4.M23 | corrección |
| 45 | IDCU-DATOS-045 | S2 | Seeds de desarrollo con datos personales de apariencia real | F4.L4.M24 | corrección |
| 46 | IDCU-REG-046 | S2 | Parámetros regulatorios y de riesgo activos sin confirmación ni procedencia completa | F4.L4.M25 | corrección |
| 47 | IDCU-DATOS-047 | S2 | Bitácoras y evidencias de identidad/cumplimiento no son append-only | F1.L4.M24 | corrección |
| 48 | IDCU-TRAZA-048 | S3 | La aprobación de retiro no deja registro propio y el motivo de rechazo se pierde | F3.L4.M08 | corrección |
| 49 | IDCU-CONTRATO-049 | S3 | `nucleo_financiero.retiro_rechazado` sale con dos formas distintas | F3.L4.M08 | corrección |
| 50 | IDCU-PRUEBAS-050 | S3 | `debe_fallar` acepta cualquier excepción; dos pruebas de R-UIF fallan por otra causa | F4.L4.M27 | corrección |
| 51 | IDCU-COHERENCIA-051 | S3 | Tres vocabularios para el carnet de extranjería y dos para el nivel KYC | F4.L4.M03 | corrección |
| 52 | IDCU-DATOS-052 | S3 | Importes sin moneda en identidad y cumplimiento; tipo de cambio sin fecha | F4.L4.M28 | corrección |
| 53 | IDCU-DATOS-053 | S3 | Idempotencia y catálogos cerrados faltantes en `token_verificacion`; FK e índices | F4.L4.M29 | corrección |
| 54 | IDCU-SEG-054 | S3 | El hash de búsqueda del documento no usa el único camino que la regla declara | F4.L4.M30 | corrección |
| 55 | IDCU-CONTRATO-055 | S3 | OpenAPI de identidad y cumplimiento sin esquema de seguridad | F4.L4.M31 | corrección |
| 56 | IDCU-CONTRATO-056 | S3 | `buscarPorTelefono` deprecado sigue publicado con el teléfono en la URL | F4.L4.M32 | corrección |
| 57 | IDCU-CONTRATO-057 | S3 | `aprobarRetiro` declara 404 y 409 pero el código devuelve 422 | F2.L4.M01 | se resuelve con F2.L4.M01 |
| 58 | IDCU-PRUEBAS-058 | S3 | Pacto identidad→grupos sin verificación de productor ni casos de error | F0.L4.M05 | corrección |
| 59 | IDCU-SEG-059 | S3 | La evidencia step-up no está ligada al retiro concreto | F1.L4.M17 | corrección |
| 60 | IDCU-APROB-060 | S3 | La bandeja no da información para decidir | F2.L4.M01 | se resuelve con F2.L4.M01 |
| 61 | IDCU-DEFECTO-061 | S3 | Rechazar una orden le pone fecha de pago | F4.L4.M33 | corrección |
| 62 | IDCU-RESILIENCIA-062 | S3 | Dos despachos al proveedor; `instruirPago` no puede correr bien | F3.L4.M07 | se resuelve con F3.L4.M07 |
| 63 | IDCU-DUP-INT-063 | S3 | Catálogos paralelos de umbrales y reglas de monitoreo | F4.L4.M34 | corrección |
| 64 | IDCU-FRAGIL-064 | S3 | Retención del retiro con motivo y vencimiento contrarios a CU-11 | F1.L4.M21 | corrección |
| 65 | IDCU-COHERENCIA-065 | S3 | CU-11 promete cosas que el código no hace | F4.L4.M35 | corrección |
| 66 | IDCU-DEBIDO-PROCESO-066 | S3 | Bloqueos y retenciones automáticas sin expediente | F0.L4.M03 | corrección |
| 67 | IDCU-SEG-067 | S3 | ADMIN_PLATAFORMA concentra funciones incompatibles | F4.L4.M36 | corrección |
| 68 | IDCU-HUECO-068 | S3 | R-UIF-10 deja pasar PEP COMPLETA sin `aprobada_por` | F1.L4.M05 | se resuelve con F1.L4.M05 |
| 69 | IDCU-DATOS-069 | S3 | PII en claro en cumplimiento e identidad | F4.L4.M37 | corrección |
| 70 | IDCU-ARQ-070 | S3 | ADR-032 incompleto: no hay `sql/migraciones/` ni verificación de deriva | F0.L4.M04 | corrección |
| 71 | IDCU-DOC-071 | S4 | Documentación generada desactualizada | F4.L4.M38 | corrección |
| 72 | IDCU-DATOS-072 | S4 | Contratos de adhesión y políticas de token con valores de relleno o ambiguos | F4.L4.M21 | corrección |
| 73 | IDCU-PRUEBAS-073 | S4 | Matriz negativa incompleta en la aprobación de retiro | F2.L4.M01 | se resuelve con F2.L4.M01 |
| 74 | IDCU-CONFIG-074 | S4 | Barrel Dart versionado con `src/` ignorado | F4.L4.M89 | corrección |
| 75 | IDCU-SEG-075 | S1 | Suspender, bloquear o dar de baja una cuenta no impide volver a entrar | F1.L4.M01 | corrección |
| 76 | IDCU-SEG-076 | S2 | `/usuarios-internos` permite activar a cualquier participante saltando el KYC y darle c… | F1.L4.M10 | corrección |
| 77 | IDCU-HUECO-077 | S1 | El segundo factor del operador no existe: no hay TOTP ni enrolamiento; en TEST cualquie… | F1.L4.M02 | corrección |
| 78 | IDCU-HUECO-078 | S2 | Step-up para retiros inutilizable en producción | F1.L4.M16 | corrección |
| 79 | IDCU-DEFECTO-079 | S2 | La segregación de funciones compara códigos de permiso que no existen | F1.L4.M11 | corrección |
| 80 | IDCU-SEG-080 | S2 | Cualquier rol GLOBAL (anunciante, comercial, soporte, servicios) recibe `rol=BACKOFFICE… | F1.L4.M09 | corrección |
| 81 | IDCU-SEG-081 | S2 | Fuerza bruta ilimitada sobre un OTP ya consumido devuelve el borrador con PII | F1.L4.M12 | corrección |
| 82 | IDCU-DATOS-082 | S2 | El número de documento y el origen de fondos del borrador se guardan en texto plano 7 días | F4.L4.M23 | corrección |
| 83 | IDCU-DATOS-083 | S2 | `numero_cifrado` no está cifrado: guarda el hash con un prefijo, el número es irrecuper… | F4.L4.M30 | corrección |
| 84 | IDCU-REG-084 | S2 | Consentimientos con versión y hash de documento inventados; los contratos aceptados no … | F4.L4.M21 | corrección |
| 85 | IDCU-FLUJO-FALTANTE-085 | S2 | La decisión KYC no emite evento; la coreografía del alta está incompleta | F3.L4.M02 | corrección |
| 86 | IDCU-SEG-086 | S2 | Las fotos del expediente se pueden reemplazar después de aprobado (evidencia legal sobr… | F1.L4.M20 | corrección |
| 87 | IDCU-TRAZA-087 | S2 | Roles otorgados a nombre del revisor KYC que no los decidió, y con ámbito GLOBAL para r… | F3.L4.M02 | corrección |
| 88 | IDCU-HUECO-088 | S2 | CU-08 y CU-09 sin endpoint; no existe recuperación, cambio de clave, baja ni enrolamien… | F4.L4.M19 | corrección |
| 89 | IDCU-SEG-089 | S2 | Desbloquear un interno no desbloquea; la clave provisional del alta nunca se entrega | F1.L4.M01 | corrección |
| 90 | IDCU-SEG-090 | S2 | Lecturas de datos sensibles sin bitácora de lectura, aunque el contrato lo promete | F4.L4.M39 | corrección |
| 91 | IDCU-SEG-091 | S2 | Enumeración de cuentas en rutas públicas y por tiempo de respuesta del login | F1.L4.M13 | corrección |
| 92 | IDCU-SEG-092 | S2 | `VerificarTitularidad` responde "coincide" sin KYC aprobado y puede devolver 500 | F1.L4.M18 | corrección |
| 93 | IDCU-SEG-093 | S2 | `GRUPO_ADMINISTRAR` (autoconcedido vía CU-90) permite enumerar teléfonos sin tope e inv… | F1.L4.M19 | corrección |
| 94 | IDCU-RESILIENCIA-094 | S2 | Estado de seguridad en memoria con 4 réplicas mínimas | F1.L4.M16 | corrección |
| 95 | IDCU-CONTRATO-095 | S3 | Códigos de error reutilizados con significados distintos y fuera del patrón declarado | F4.L4.M40 | corrección |
| 96 | IDCU-CONTRATO-096 | S3 | `Idempotency-Key` obligatoria en el contrato pero ignorada en el código | F4.L4.M41 | corrección |
| 97 | IDCU-DINERO-097 | S3 | `montoMensualEstimado` es texto libre sin formato ni moneda | F3.L4.M05 | corrección |
| 98 | IDCU-SEG-098 | S3 | Una sola pimienta para todo; hash de documento sin HMAC | F4.L4.M30 | corrección |
| 99 | IDCU-SEG-099 | S3 | Refresh con SHA-256 sin pimienta; JWT de acceso sin `iss`/`aud`, no revocable al cerrar… | F1.L4.M15 | corrección |
| 100 | IDCU-CONFIG-100 | S3 | Configuración que no falla al arrancar y tamaños incoherentes | F1.L4.M03 | corrección |
| 101 | IDCU-FRAGIL-101 | S3 | Carreras en controles de seguridad | F1.L4.M14 | corrección |
| 102 | IDCU-ARQ-102 | S3 | Capas cruzadas, SQL crudo en aplicación y "contexto de sistema" con el usuario de la se… | F4.L4.M42 | corrección |
| 103 | IDCU-DUP-INT-103 | S3 | Dos implementaciones de asignación de roles divergentes | F1.L4.M10 | corrección |
| 104 | IDCU-RENDIMIENTO-104 | S3 | Listados sin paginación, N+1 y relectura total tras decidir | F4.L4.M43 | corrección |
| 105 | IDCU-COHERENCIA-105 | S3 | Tres nombres para el carné de extranjería y conversiones que tragan errores | F4.L4.M03 | corrección |
| 106 | IDCU-DATOS-106 | S3 | Valores inventados presentados como reales | F4.L4.M44 | corrección |
| 107 | IDCU-TRAZA-107 | S3 | La IP registrada es la del gateway | F4.L4.M45 | corrección |
| 108 | IDCU-RESILIENCIA-108 | S3 | Efectos externos dentro de transacción y saga sin compensación | F4.L4.M46 | corrección |
| 109 | IDCU-SEG-109 | S3 | Evidencia de firma de contrato de organizador débil | F2.L4.M02 | se resuelve con F2.L4.M02 |
| 110 | IDCU-DOC-110 | S3 | Documentación que contradice el código | F4.L4.M47 | corrección |
| 111 | IDCU-PRUEBAS-111 | S3 | Las pruebas no cubren los controles que fallan | F4.L4.M48 | corrección |
| 112 | IDCU-SEG-112 | S3 | Ruta deprecada con teléfono en la URL sigue publicada | F4.L4.M32 | corrección |
| 113 | IDCU-OTRO-113 | S4 | `actualizarFechaExpiracionDocumento` es `@Publico` pero exige sesión; actualiza todos l… | F4.L4.M49 | corrección |
| 114 | IDCU-CLEAN-114 | S4 | `ConsultarNivelKyc` autoriza por string de rol que nunca viene en el token | F4.L4.M50 | corrección |
| 115 | IDCU-RENDIMIENTO-115 | S4 | Gmail pide token OAuth en cada envío; cabecera `To:` sin sanear | F4.L4.M51 | corrección |
| 116 | IDCU-OBSERV-116 | S4 | Tope de titularidad por réplica y mapa `entregadosPorCorreo` que crece | F1.L4.M16 | corrección |
| 117 | IDCU-FLUJO-FALTANTE-117 | S2 | La detección UIF (PCC-01 y ROG, CU-41/CU-42) no tiene ningún disparador: los reportes m… | F3.L4.M09 | corrección |
| 118 | IDCU-FLUJO-FALTANTE-118 | S2 | Un oficio de autoridad con orden de bloqueo nunca bloquea nada y el acceso por oficio n… | F4.L4.M52 | corrección |
| 119 | IDCU-SEG-119 | S1 | Los "cuatro ojos" de la diligencia PEP (CU-02) los declara el cliente HTTP: un solo ana… | F1.L4.M05 | corrección |
| 120 | IDCU-SEG-120 | S2 | La separación "quien investiga no decide" es nominal: el analista es un UUID libre del … | F1.L4.M06 | corrección |
| 121 | IDCU-DEFECTO-121 | S2 | Declarar PEP (CU-03, endpoint expuesto) borra beneficiarios con DELETE sin privilegio y… | F4.L4.M53 | corrección |
| 122 | IDCU-COHERENCIA-122 | S2 | `garantia` y `organizador` consultan el efecto de reclamos con el token del usuario, pe… | F3.L4.M10 | corrección |
| 123 | IDCU-FLUJO-FALTANTE-123 | S2 | Ninguno de los ~30 eventos que publica cumplimiento tiene consumidor: consentimientos, … | F4.L4.M54 | corrección |
| 124 | IDCU-HUECO-124 | S2 | El "archivo" del reporte regulatorio no existe y el "envío" es una declaración manual d… | F4.L4.M55 | corrección |
| 125 | IDCU-DEFECTO-125 | S2 | Generar un reporte acepta `catalogoCodigo` y `formulario` independientes: se puede gene… | F4.L4.M56 | corrección |
| 126 | IDCU-DEFECTO-126 | S2 | `abrirHallazgo` compara solo hallazgos abiertos pero el índice UNIQUE es global, con có… | F4.L4.M57 | corrección |
| 127 | IDCU-DATOS-127 | S2 | Parámetros normativos sin procedencia en código y configuración (plazos LFT, plazos de … | F4.L4.M26 | corrección |
| 128 | IDCU-DATOS-128 | S3 | Semilla de umbrales UIF con `vigente_desde = current_date`: la vigencia depende del día… | F4.L4.M25 | corrección |
| 129 | IDCU-HUECO-129 | S2 | Controles de vencimiento escritos como consultas y nunca disparados | F4.L4.M58 | corrección |
| 130 | IDCU-SEG-130 | S2 | No hay bitácora de lectura de datos personales/financieros | F4.L4.M39 | corrección |
| 131 | IDCU-DEFECTO-131 | S3 | `DiligenciaRepositorio.elevarTipo` actualiza TODAS las diligencias del usuario, incluid… | F4.L4.M59 | corrección |
| 132 | IDCU-CONTRATO-132 | S3 | `Idempotency-Key` es obligatoria en el contrato y ningún endpoint la usa | F4.L4.M41 | corrección |
| 133 | IDCU-CLEAN-133 | S3 | La versión insegura de la decisión LFT (`CU44InvestigarYReportar.decidir`) sigue viva | F1.L4.M06 | se resuelve con F1.L4.M06 |
| 134 | IDCU-DUP-INT-134 | S3 | Duplicaciones internas con semántica divergente | F4.L4.M60 | corrección |
| 135 | IDCU-DINERO-135 | S3 | Importes sin moneda y tipo de cambio sin fuente | F3.L4.M09 | corrección |
| 136 | IDCU-SEG-136 | S3 | Controles regulatorios que aceptan autodeclaraciones booleanas (hoy sin entrada HTTP) | F1.L4.M26 | corrección |
| 137 | IDCU-SEG-137 | S3 | `GET /licencia/alcance` escribe en el outbox por cada rechazo, con un `usuarioId` arbit… | F1.L4.M25 | corrección |
| 138 | IDCU-FRAGIL-138 | S3 | Evidencia y parseos frágiles | F4.L4.M61 | corrección |
| 139 | IDCU-DEFECTO-139 | S3 | Escalado de incidentes vencidos puede quedar en inanición | F4.L4.M62 | corrección |
| 140 | IDCU-DEBIDO-PROCESO-140 | S3 | Pequeñas fugas hacia el titular o fuera del equipo LFT | F4.L4.M63 | corrección |
| 141 | IDCU-PRUEBAS-141 | S2 | Pruebas plantilla con nombres que no corresponden y afirmaciones que no se verifican; i… | F4.L4.M64 | corrección |
| 142 | IDCU-DOC-142 | S3 | README y descriptor afirman más de lo que existe | F4.L4.M47 | corrección |
| 143 | IDCU-RENDIMIENTO-143 | S4 | Listados sin límite y alertas no idempotentes | F4.L4.M65 | corrección |
| 144 | IDCU-DATOS-144 | S2 | Ficha de caso LFT enrutada con datos de ejemplo, catálogo de causales inventado y "debi… | F4.L4.M66 | corrección |
| 145 | IDCU-SEG-145 | S2 | Narrativa del caso LFT persistida en IndexedDB del navegador, por caso y no por usuario… | F4.L4.M67 | corrección |
| 146 | IDCU-DINERO-146 | S2 | Aprobación de retiro "a ciegas" por enlace profundo: se autoriza sin ver monto, destino… | F2.L4.M01 | se resuelve con F2.L4.M01 |
| 147 | IDCU-DEFECTO-147 | S2 | "Aprobar y enviar" un reporte UIF identifica el reporte por un código tecleado, no por … | F4.L4.M68 | corrección |
| 148 | IDCU-DATOS-148 | S2 | La ficha KYC descarga la cola completa (todos los estados y personas) para mostrar un e… | F4.L4.M43 | corrección |
| 149 | IDCU-HUECO-149 | S2 | Alta de usuario interno sin forma de entregar la contraseña provisional ni de enrolar e… | F1.L4.M04 | corrección |
| 150 | IDCU-DINERO-150 | S3 | Importes sin moneda en contratos consumidos por el lote | F4.L4.M28 | corrección |
| 151 | IDCU-SEG-151 | S3 | Revisión de accesos exportable sin permiso de exportación (lista de quién no tiene segu… | F4.L4.M69 | corrección |
| 152 | IDCU-FRAGIL-152 | S3 | Fotos de cédula: suscripciones sin cancelar crean `blob:` después de soltar/destruir | F4.L4.M70 | corrección |
| 153 | IDCU-UX-A11Y-153 | S3 | Decisión KYC sin confirmación, motivo sin tope del contrato y error del servidor no aso… | F4.L4.M71 | corrección |
| 154 | IDCU-SEG-154 | S3 | La bandeja KYC muestra teléfono y correo de cada persona en la fila | F4.L4.M72 | corrección |
| 155 | IDCU-DEFECTO-155 | S3 | "Responder requerimiento" conserva el enlace escrito para otro oficio | F4.L4.M73 | corrección |
| 156 | IDCU-HUECO-156 | S3 | Abrir caso: "marcá alertas de un mismo titular" pero la fila no trae titular; el analis… | F4.L4.M74 | corrección |
| 157 | IDCU-REG-157 | S3 | Asignación de roles privilegiados sin doble control | F4.L4.M75 | corrección |
| 158 | IDCU-CONTRATO-158 | S3 | Mutaciones de usuarios internos sin Idempotency-Key; listado sin paginación; 403 mal ti… | F4.L4.M41 | corrección |
| 159 | IDCU-APROB-159 | S3 | Habilitar organizador: botón para cualquiera, ruta sin guardia, sin bloqueo de doble en… | F2.L4.M02 | se resuelve con F2.L4.M02 |
| 160 | IDCU-FRAGIL-160 | S3 | Rutas de detalle sin `canMatch` mientras las bandejas sí lo tienen | F4.L4.M76 | corrección |
| 161 | IDCU-CLEAN-161 | S4 | Código muerto (componentes y funciones sin uso) cubierto por tests que inflan la cobertura | F4.L4.M77 | corrección |
| 162 | IDCU-DOC-162 | S4 | Comentarios que contradicen el contrato actual | F4.L4.M78 | corrección |
| 163 | IDCU-CONTRATO-163 | S4 | Mensajes para códigos que el contrato no declara o con otra semántica | F4.L4.M40 | corrección |
| 164 | IDCU-DUP-INT-164 | S4 | Diccionarios de estados/enums copiados a mano en varios lugares | F4.L4.M79 | corrección |
| 165 | IDCU-UX-A11Y-165 | S3 | Menú «Más» de usuarios internos hecho a mano | F4.L4.M80 | corrección |
| 166 | IDCU-UX-A11Y-166 | S4 | Ficha de usuario interno muestra códigos y fechas crudos; id por snapshot | F4.L4.M81 | corrección |
| 167 | IDCU-UX-A11Y-167 | S4 | Literales de tipografía, tiempos y medidas en lugar de tokens | F4.L4.M82 | corrección |
| 168 | IDCU-CLEAN-168 | S4 | `$any` y `as` en el borde del contrato | F4.L4.M83 | corrección |
| 169 | IDCU-SEG-169 | S4 | Parámetros de ruta y query sin codificar; enlaces sin validar esquema | F4.L4.M84 | corrección |
| 170 | IDCU-ACOPLE-170 | S4 | Importaciones cruzadas entre dominios del backoffice | F4.L4.M85 | corrección |
| 171 | IDCU-SEG-171 | S4 | Exportación con un único permiso para cuatro colas distintas | F4.L4.M69 | corrección |
| 172 | IDCU-OBSERV-172 | S4 | Huella de dispositivo aleatoria por pestaña en el ingreso | F4.L4.M86 | corrección |
| 173 | IDCU-UX-A11Y-173 | S4 | Ingreso: `AP-CU04-06` (sin factor enrolado) se muestra como texto del servidor, sin camino | F1.L4.M04 | se resuelve con F1.L4.M04 |
| 174 | IDCU-FLUJO-FALTANTE-174 | S2 | Web: el secreto de invitación se retira de la URL y se guarda en memoria, pero nunca se… | F3.L4.M12 | corrección |
| 175 | IDCU-PRUEBAS-175 | S4 | Espera fija y dobles declarados | F4.L4.M87 | corrección |
| 176 | IDCU-RENDIMIENTO-176 | S4 | Funciones en plantilla recalculadas por celda | F4.L4.M88 | corrección |
