# Anexo L3 — Correcciones por hallazgo

Fuente: `AUDITORIA-grupos-organizador.md` (lote L3, `origin/test` 07fc6971). Hay 55 fichas y las 55 están trazadas (ver la tabla del final).
Decisiones que se aplican, todas de `2026-11-20-auditoria-consolidado/DECISIONES.md`:
- P-11: el organizador profesional se elimina, junto con sus postulaciones, la habilitación, el contrato y la evaluación de desempeño.
- Tanda 5: se entra a un grupo **solo por invitación** de un miembro. Los requisitos son KYC aprobado, reglamento aceptado y no tener deudas vivas. No hay grupos públicos.
- Tanda 4: el inicio y el sorteo son **automáticos** cuando se completan los cupos.
- Tanda 5: el orden de turnos va **por nivel de puntualidad**. Dentro de cada nivel decide un sorteo con compromiso y revelación.
- P-12: el sistema declara el incumplimiento, Soporte o Riesgos resuelven el descargo y otra persona resuelve la apelación.
- P-01/P-02 modelo A: hay un fondo del grupo con subrogación.
- P-18: se abre un PR por cambio hacia `dev`.

Todos los hallazgos de origen están en el peldaño `DISCOVERED`. Ninguna microtarea de este anexo se ejecutó.

## Resumen

**Microtareas por fase** (27 en total)

| Fase | Microtareas |
|---|---:|
| F0 | 2 |
| F1 | 7 |
| F2 | 6 |
| F3 | 7 |
| F4 | 5 |

**Hallazgos por fase de su microtarea principal**

| Fase | S1 | S2 | S3 | S4 | Total |
|---|---:|---:|---:|---:|---:|
| F0 | 0 | 0 | 2 | 0 | 2 |
| F1 | 2 | 3 | 3 | 0 | 8 |
| F2 | 0 | 11 | 7 | 0 | 18 |
| F3 | 0 | 8 | 3 | 0 | 11 |
| F4 | 0 | 3 | 10 | 3 | 16 |
| **Total** | **2** | **25** | **25** | **3** | **55** |

Cada hallazgo se cuenta una sola vez, en la fase de la microtarea principal que figura en la tabla de trazabilidad.

**Hallazgos «se resuelve con»** (la corrección de otro cambio los hace desaparecer; cada uno conserva su fila):

| Hallazgo | Se resuelve con |
|---|---|
| COHERENCIA-001 | F2.L3.M10 |
| FRAGIL-001 | F2.L3.M10 |
| HUECO-001 | F2.L3.M10 |
| FRAGIL-003 | F2.L3.M11 y F2.L3.M12 |
| REESCRITURA-001 | F2.L3.M12 |
| DEFECTO-001 | F2.L3.M12 |
| HUECO-005 | F2.L3.M12 |
| DINERO-001 | F2.L3.M12 |
| DEBIDO-PROCESO-001 | F2.L3.M12 |
| DEBIDO-PROCESO-002 | F2.L3.M12 |
| DOC-002 | F2.L3.M12 |
| RESILIENCIA-001 | F2.L3.M12 y F2.L3.M13 |
| SEG-003 | F2.L3.M13 |
| SEG-002 | F2.L3.M14 |
| HUECO-008 | F2.L3.M14 |
| PRUEBAS-002 | F2.L3.M10, F2.L3.M12, F3.L3.M17 y F3.L3.M18 |

- **DEBIDO-PROCESO-001 y DEBIDO-PROCESO-002:** al desaparecer el organizador no hay a quién sancionar como organizador. El debido proceso de los participantes (P-12) sigue vigente y lo cubre la microtarea de incumplimiento del lote L2.
- **Parte (3) y (4) de DATOS-001:** se resuelven con F2.L3.M12 y F2.L3.M13.

**Hallazgos «sin acción»:** ninguno. Todos piden un cambio, aunque sea retirar código.

**Ambigüedades abiertas** (no se resuelven por conveniencia; regla 00 §1.7):

| ID | Pregunta | Supuesto que se toma | Afecta a | A quién confirmar |
|---|---|---|---|---|
| D-L3-1 | Sin organizador y con el modelo A, ¿qué tipos de acuerdo del grupo sobreviven: condonación, expulsión, disolución, cambio de reglamento, repetir sorteo, permuta? | Ninguno | F3.L3.M21 | Pablo |
| D-L3-2 | ¿La subasta o venta de turnos y cupos cae con el modelo B («turno con precio»), que está descartado? | Se mantiene | F4.L3.M25 | Pablo |
| D-L3-3 | Cuando se retira a mitad de ciclo un participante que todavía no cobró, ¿qué se le devuelve y cuándo? | Se implementan solo las ramas que están definidas: antes del inicio se devuelve todo; un deudor queda subrogado al fondo | F3.L3.M19 | Pablo |
| D-L3-4 | ¿Cuál es el plazo máximo de inscripción antes de cancelar un grupo incompleto? | Es un parámetro obligatorio, sin valor por defecto | F3.L3.M18 | Pablo y Cumplimiento |

---

## F0 — Preparación

### F0.L3.M01 — ADR que reemplaza a ADR-054 y registra la regla sin respaldo de la empresa sobre ADR-051
- Hallazgos: RESPALDO-001 (S3)
- Causa de raíz: las decisiones nuevas no están escritas en un ADR. Las que siguen vigentes son ADR-051 D-02/D-03/D-15 (la empresa cubre el faltante) y ADR-054 (organizador opcional, ingreso por enlace o emparejamiento, inicio al 75 %). El catálogo de flujos las valida como vigentes.
- Corrección de raíz:
  - Escribir un ADR nuevo en `docs/Arquitectura/` que deje constancia de cuatro reglas:
    - no hay organizador;
    - se entra solo por invitación, con KYC, reglamento aceptado y sin deudas vivas;
    - el inicio y el sorteo son automáticos al completarse los cupos;
    - el orden de turnos va por nivel de puntualidad y, dentro de cada nivel, por sorteo con compromiso y revelación.
  - Marcar en ADR-054 qué secciones quedan superadas. Hacer lo mismo con ADR-051 D-02/D-03/D-15, apuntando al ADR del modelo A.
  - En `docs/catalogo-de-flujos/aporte-mora-cobertura.yaml`, reemplazar los pasos `cubrir-faltante` y `recuperar-respaldo` por los del fondo del grupo.
  - Esto resuelve la causa porque `validar_flujos.py` deja de validar el flujo retirado.
- CA: Dado el repo, cuando se busca `cubrirConRespaldo` o «la empresa aporta el faltante» en `docs/Arquitectura/*.md` vigentes y en `docs/catalogo-de-flujos/`, entonces no hay coincidencias fuera de las secciones marcadas como superadas.
- DoD:
  - `python scripts/validar_flujos.py` en verde con el catálogo nuevo;
  - `grep -rn 'cubrirConRespaldo\|recuperar-respaldo' docs/catalogo-de-flujos` vacío;
  - revisión de Pablo en el PR.
- Depende de: ninguna. Coordina con el ADR del modelo A del lote L2.
- Esfuerzo: medio

### F0.L3.M02 — Verificación de pactos del lado productor en `grupos`
- Hallazgos: PRUEBAS-003 (S3)
- Causa de raíz: los pactos `identidad-grupos-pertenencia`, `transparencia-grupos-pertenencia` y `grupos-aportes-grupo-iniciado` solo se verifican del lado consumidor. Si `grupos` cambia el payload, rompe a sus consumidores sin aviso (regla 98.1.4).
- Corrección de raíz: agregar en `servicios/grupos/src/test/.../contrato/` una prueba de proveedor que reproduzca cada interacción de `contratos/pactos/*grupos*.json` contra el controlador real y el publicador real de `grupo_iniciado`, con el serializador de outbox. Conectarla al gate de contratos.
- CA: Dado un cambio que quita un campo de `grupo_iniciado`, cuando corre la verificación de proveedor, entonces falla y nombra el pacto roto.
- DoD:
  - `./gradlew :servicios:grupos:test --tests '*PactoProveedor*'` en verde;
  - prueba de mutación: quitar un campo a mano, ver el rojo, revertir.
- Depende de: F0, contratos base. Es la base para F3.L3.M16 y F3.L3.M17.
- Esfuerzo: medio

---

## F1 — Seguridad e integridad

### F1.L3.M03 — Destino de transferencia por identificador único y fuera de la URL
- Hallazgos: DINERO-002 (S1) y OBSERV-001 (S2)
- Causa de raíz: un dato de presentación, el nombre visible del participante, se usa como identificador del destino del dinero. Se resuelve con `LIMIT 1` sin orden y viaja en el path `/grupos/participantes/alias/{alias}`, así que el nombre de una persona entra en los logs de cada salto.
- Corrección de raíz:
  - Definir un alias de destino elegido por su titular y con índice `UNIQUE`. El cambio entra por el modelo (`sql/10_tablas/02_grupos_turnos/participante.sql` o, mejor, en `identidad` si el alias es de la persona) y sigue el pipeline de la regla 97.1.
  - Reemplazar `GET /grupos/participantes/alias/{alias}` por un `POST` con la búsqueda en el cuerpo. Si hay 0 o más de una coincidencia, la respuesta es 422 con código propio. Va con versión nueva del contrato en `grupos.yaml` y la vieja se retira con fecha.
  - `ResolverDestino` de `nucleo-financiero` deja de aceptar un destino ambiguo.
  - La traza registra solo un identificador opaco.
  - Así se elimina la ambigüedad de origen en lugar de ordenar el `LIMIT 1`.
- CA:
  - Dados dos participantes con el mismo nombre visible, cuando se transfiere por nombre, entonces la API responde 422 y no se mueve dinero.
  - Cuando se transfiere por alias único, entonces el destino es ese titular y el alias no aparece en ninguna línea de log de acceso.
- DoD:
  - integración contra PostgreSQL real con dos homónimos;
  - prueba de contrato `nucleo-financiero`↔`grupos` del endpoint nuevo;
  - prueba de logs: captura del log de la petición sin el alias;
  - autorización negativa: sin token y con otro usuario.
- Depende de: F0 (contratos base).
- Esfuerzo: grande

### F1.L3.M04 — El retiro de un grupo se pide solo para quien llama
- Hallazgos: SEG-001 (S1)
- Causa de raíz: `CU65Retirarse` confía la autorización por objeto a RLS, y `solicitud_retiro` no tiene columna de titular que RLS recorra. Por eso el creador del grupo puede pedir el retiro de otro integrante.
- Corrección de raíz: en `GruposController.java:179-196` y `CU65Retirarse.java:48-78`, el participante se deriva de `ctx.usuarioId()` y del `grupoId` de la ruta. El cuerpo no aporta identidad. Agregar la columna de titular (`usuario_id`) a `solicitud_retiro` y una política RLS que la use, por el pipeline del modelo, para que la base también lo impida.
- CA: Dado el creador del grupo, cuando pide el retiro indicando a otro integrante, entonces recibe 403 o 404 y no se crea ninguna fila.
- DoD:
  - prueba web negativa «otro usuario» y «creador sobre otro integrante»;
  - integración con RLS activa contra base real;
  - `verificaciones.sql` sin deriva.
- Depende de: ninguna.
- Esfuerzo: medio

### F1.L3.M05 — El traspaso de cupo verifica el KYC real del entrante
- Hallazgos: SEG-004 (S3)
- Causa de raíz: en `GruposController.java:234-240` quedó un marcador que reemplaza el KYC del entrante por el mínimo del grupo, así que el control de debida diligencia siempre aprueba.
- Corrección de raíz: consultar `PersonasPorHttp` (`GET /usuarios/{u}/kyc`) con timeout explícito y circuito. Si `identidad` no responde, el traspaso falla cerrado (degradación declarada, regla 98.2.4). Los requisitos son los mismos que para la admisión por invitación (F3.L3.M17).
- CA:
  - Dado un entrante con KYC no aprobado, cuando se pide el traspaso, entonces 422 con código de KYC.
  - Con `identidad` caído, entonces 503 y no hay traspaso.
- DoD:
  - unitaria con doble de `PersonasPorHttp` en tres niveles: aprobado, límite (nivel exacto) e inválido (500 o timeout);
  - integración con `identidad` apagado.
- Depende de: ninguna.
- Esfuerzo: chico

### F1.L3.M06 — Un único conjunto de estados vivos del grupo para «grupos activos» y el cierre de billetera
- Hallazgos: DEFECTO-002 (S2)
- Causa de raíz: `ConsultasRepositorio.java:46-58` cuenta grupos vivos con estados que no existen en `grupo.sql:37`. Así, una persona en mora sale con 0 grupos activos y puede cerrar su billetera debiendo al grupo.
- Corrección de raíz:
  - Crear un enumerado de dominio `EstadoDeGrupo` en `servicios/grupos/.../dominio` con su conjunto `TERMINALES` alineado con el `CHECK` de `grupo.sql`.
  - La consulta pasa los estados por parámetro y cuenta solo participantes no retirados.
  - `RespuestasAOtrosServicios.java:50-53` responde con ese cálculo.
  - Agregar una prueba que compare el enum con el `CHECK` de la base, para que no vuelvan a divergir.
- CA: Dado un participante con cuota vencida en un grupo `CONFORMADO` o `EN_CURSO`, cuando `nucleo-financiero` consulta sus grupos activos, entonces la respuesta es ≥ 1 y el cierre de billetera se rechaza.
- DoD:
  - integración contra base real con un grupo en cada estado;
  - prueba de paridad enum↔`CHECK`;
  - prueba de contrato del endpoint interno.
- Depende de: ninguna.
- Esfuerzo: chico

### F1.L3.M07 — Resolver un acuerdo con la precondición en la escritura
- Hallazgos: DEFECTO-003 (S2)
- Causa de raíz: `AcuerdoRepositorio.java:133-141` escribe el estado sin condición, y la precondición vive en un `if` de `CU63Acordar` (regla 96.3.2). Con dos votos simultáneos el acuerdo se resuelve dos veces, y uno ya `EJECUTADO` puede volver a usarse en otro traspaso.
- Corrección de raíz: `UPDATE … SET estado = ? WHERE id = ? AND estado = 'ABIERTO'`. Si actualiza 0 filas, «ya resuelto»: responde idempotente y no emite un segundo `acuerdo_resuelto`. El consumo de un acuerdo por un traspaso exige `estado = 'APROBADO'` en la misma escritura y pasa a `EJECUTADO` atómicamente.
- CA: Dados dos votos decisivos concurrentes, cuando se procesan, entonces hay exactamente un `acuerdo_resuelto` en outbox, y un acuerdo `EJECUTADO` no autoriza un segundo traspaso.
- DoD:
  - prueba de concurrencia real (dos hilos y dos conexiones) contra PostgreSQL;
  - conteo en outbox = 1.
- Depende de: ninguna. Si D-L3-1 retira los acuerdos, se aplica solo a los tipos que sobrevivan.
- Esfuerzo: chico

### F1.L3.M08 — Importes del módulo con moneda, sin negativos y sin segunda fuente
- Hallazgos: DINERO-003 (S3)
- Causa de raíz:
  - El modelo de `grupos` y `organizador` no tiene un objeto de valor Dinero: los importes no llevan moneda, no tienen `CHECK >= 0` y uno se guarda como texto.
  - `periodo.monto_recaudado` es una segunda fuente para un número que pertenece al mayor (regla 91.1.5).
- Corrección de raíz:
  - En el modelo (`.puml` → `sql/10_tablas/02_grupos_turnos/{periodo,turno,solicitud_retiro,traspaso_cupo,solicitud_permuta,alta_grupo,…}.sql`), agregar `moneda CHAR(3)` junto a cada importe y `CHECK (monto >= 0)`, y convertir a `DECIMAL(14,2)` el que hoy es texto.
  - Retirar `periodo.monto_recaudado`. Quien lo necesite lo lee del mayor por el contrato de `nucleo-financiero`.
  - En `grupos.yaml:1368-1374,2002-2008`, cada importe pasa a ser `{monto, moneda}`, con versión.
  - Las tablas de `organizador` caen con F2.L3.M12.
- CA:
  - Dado un `INSERT` con importe negativo o sin moneda en cualquiera de esas tablas, entonces la base lo rechaza.
  - Ningún endpoint de `grupos` expone un recaudado que no venga del mayor.
- DoD:
  - aplicación del esquema con salida pegada;
  - `verificaciones.sql` y verificación de deriva en verde (97.2);
  - prueba que intenta violar cada `CHECK`;
  - prueba de contrato con el formato `{monto, moneda}`.
- Depende de: F2.L3.M12 (para no migrar las tablas que se borran).
- Esfuerzo: medio

### F1.L3.M09 — El secreto del enlace de invitación va en el fragmento y no en el path ni en el login
- Hallazgos: SEG-005 (S3)
- Causa de raíz: el enlace web de la app reutiliza el path para el secreto, y el token de invitación termina como parámetro de login. Esa invitación es ahora la única credencial para entrar a un grupo, y así queda en logs, proxies e historial.
- Corrección de raíz:
  - En `apps/movil/lib/pantallas/pasanaku/dominio/enlace_del_grupo.dart:15-21`, poner el secreto en el fragmento (`#`), como ya hace `apps/web`.
  - En `vistas_de_unirse.dart:62,69`, guardar el token en almacenamiento local durante el login, no en la URL.
  - El servicio recibe el secreto en el cuerpo de un `POST`.
- CA: Dado un enlace de invitación, cuando se abre y se completa el login, entonces ni el path ni la query de ninguna petición registrada contienen el secreto.
- DoD:
  - prueba de widget o de integración de la app que inspecciona las URL generadas;
  - prueba web del endpoint con el secreto en el cuerpo;
  - revisión de log de acceso.
- Depende de: F3.L3.M17 (la puerta única de invitación).
- Esfuerzo: chico

---

## F2 — Retiro de lo que contradice el flujo nuevo

### F2.L3.M10 — Retirar la admisión humana (propuesta y resolución de backoffice, interruptor y escalamiento)
- Hallazgos: APROB-001 (S2), COHERENCIA-001 (S3), FRAGIL-001 (S2) y HUECO-001 (S2). Contribuye a PRUEBAS-002.
- Causa de raíz: una decisión de negocio sin tomar se modeló como el interruptor de despliegue `ADMISION_HUMANA_OBLIGATORIA`, con valor distinto en TEST y en producción. Además, sobre la admisión humana se diseñó una cola de escalamiento a 48 h que el servicio no tiene. La decisión ya está tomada (tanda 5): sin aprobación humana.
- Corrección de raíz:
  - En `servicios/grupos`, retirar:
    - `RutasDeAdmision` (`POST /grupos/solicitudes/{id}/resoluciones`), `proponerAdmision` y `BandejaDeAdmisiones`;
    - el `@Value` `humana-obligatoria` de `application.yml:73-78` y `docker-compose.coolify.yml:377`;
    - los eventos `admision_propuesta` y `admision_resuelta`, con versión y fecha de retiro en el contrato.
  - En `apps/backoffice`, retirar `rutas/admisiones` y `rutas/operacion/solicitudes-escaladas`, y su entrada en `scripts/verificar_rutas_frontend.py:27`.
  - Reescribir las pruebas que afirmaban la aprobación (`GruposAdmisionHumanaWebTest`, `InvitacionesAdmisionHumanaWebTest`) como pruebas de que la ruta ya no existe, dejando el registro de qué se reemplaza por qué (regla 80.5.5).
  - Con esto desaparece la fuente de la divergencia entre entornos en lugar de igualar valores.
- CA: Dado cualquier entorno, cuando se busca `humana-obligatoria` o `ADMISION_HUMANA` en código y despliegue, entonces no hay coincidencias, y `POST /grupos/solicitudes/{id}/resoluciones` responde 404.
- DoD:
  - `grep` vacío;
  - `./gradlew :servicios:grupos:test` en verde;
  - `yarn lint` y `typecheck` del backoffice;
  - `python scripts/verificar_rutas_frontend.py` en verde.
- Depende de: F0.L3.M01, y F3.L3.M17 en el mismo orden de merge (la puerta por invitación tiene que existir antes).
- Esfuerzo: medio

### F2.L3.M11 — Alta de grupo sin organizador: el creador es miembro y el sistema gestiona el grupo
- Hallazgos: COHERENCIA-002 (S2). Contribuye a FRAGIL-003 y a ACOPLE-001 (parte del disparador).
- Causa de raíz: `CU20CrearGrupo.java:86-88` exige un organizador habilitado, que consulta a `organizador` por HTTP en el camino crítico. El disparador `grupos.fn_org_validar_contrato_grupo` (`restricciones.sql:2686-2701`) lee el esquema `organizador`.
- Corrección de raíz:
  - Quitar de `CU20CrearGrupo` y de `CreacionRepositorio.java:116` la precondición y el campo organizador.
  - El creador queda registrado como primer miembro y como emisor de invitaciones, sin ningún rol de gestión. `es_autogestionado` deja de ser una rama: el sistema gestiona todos los grupos.
  - Retirar del modelo, por el pipeline, el disparador y la FK `grupo → organizador`.
  - En la app, quitar el requisito de `pantalla_crear_grupo.dart:42-44`.
- CA: Dada una persona con KYC aprobado y sin organizador, cuando crea un grupo, entonces se crea con ella como miembro, y ninguna llamada sale hacia `organizador`.
- DoD:
  - integración de CU-20 contra base real con `organizador` apagado;
  - verificación de deriva;
  - prueba de la app (widget) sin la pantalla de habilitación previa.
- Depende de: F0.L3.M01.
- Esfuerzo: medio

### F2.L3.M12 — Retirar el organizador profesional: postulación, habilitación, contrato, evaluación y sanción de organizador
- Hallazgos: REESCRITURA-001 (S3), DEFECTO-001 (S2), HUECO-005 (S3), DINERO-001 (S2), DEBIDO-PROCESO-001 (S2), DEBIDO-PROCESO-002 (S2) y DOC-002 (S3). Contribuye a FRAGIL-003, RESILIENCIA-001, DATOS-001 (parte 3) y PRUEBAS-002.
- Causa de raíz: P-11 elimina la figura. Mientras el código exista, siguen vivos sus defectos: dos caminos de habilitación, rehabilitación por apelación, suspensiones que no vencen, un tope de dinero que tipea el operador, sanciones sin descargo previo, apelaciones reescritas por quien resuelve y un comentario que habla de comisiones de organizador.
- Corrección de raíz: retirar en este orden, de expansión a contracción (regla 97.3.5):
  1. Clientes y pantallas:
     - app: `contrato_de_organizador.dart` y habilitación básica;
     - backoffice: `rutas/postulaciones`, `rutas/cumplimiento/organizadores`;
     - cliente `HechosPorHttp.organizadorHabilitado*` de `grupos`.
  2. Endpoints del contrato `organizador.yaml`: postulaciones, habilitación, contrato, evaluación, sanciones y apelaciones, con versión y fecha de retiro.
  3. Casos de uso, eventos y consumidores, y el servicio `servicios/organizador` entero si no queda nada vivo, después de F2.L3.M13.
  4. Esquema `sql/10_tablas/07_organizador_automatizacion`, sus `20_claves`, `40_reglas` (R-ORG-*) y el comentario de `sql/60_semillas/24-metadata-comentarios.sql:25`.
  5. Pruebas y pactos asociados, con registro de qué se retira y por qué.

  Antes de borrar, exportar lo que haya asentado en algún entorno (P-14: no hay dinero real).

  El debido proceso de **participantes** (sanción, descargo y apelación por Soporte o Riesgos) no se retira: lo implementa la microtarea de incumplimiento del lote L2.
- CA: Dado el repo, cuando se busca `organizador` en `servicios/`, `apps/`, `contratos/` y `sql/`, entonces solo quedan referencias históricas en ADR o migraciones de retiro, y el despliegue de TEST arranca sin el servicio.
- DoD:
  - `grep -rni 'organizador' servicios apps contratos sql` revisado con la lista de excepciones;
  - build de todos los servicios y de las apps;
  - verificación de deriva;
  - `docker compose` de integración en la máquina de Pablo (P-19) arriba sin `organizador`.
- Depende de: F2.L3.M11, F2.L3.M13, F2.L3.M14 y F0.L3.M01.
- Esfuerzo: grande

### F2.L3.M13 — Retirar la automatización que confirma un organizador (CU-96, reglas y tareas)
- Hallazgos: ninguno propio. Resuelve SEG-003 (S2) y contribuye a RESILIENCIA-001 y DATOS-001 (parte 4).
- Causa de raíz: CU-96 crea tareas `REQUIERE_APROBACION` que confirma cualquier persona con rol `ORGANIZADOR`, sin comprobar la propiedad del grupo (BFLA). Sin organizador, ese control no tiene sujeto. Además, sus acciones (`EJECUTAR_ENTREGA`, `APLICAR_MORA`) pasan a ser trabajos programados del servicio dueño: la entrega automática es de `entregas` y el incumplimiento declarado por el sistema, de `garantia`.
- Corrección de raíz:
  - Retirar `AutomatizacionController`, `CU96EjecutarTarea`, las tablas `regla_automatizacion` y `tarea_automatizada`, la FK `tarea_automatizada → grupos.grupo`, la regla R-ORG-06 de `restricciones.sql:2772-2778` y el evento `tarea_aprobada`, que no tiene consumidor.
  - Comprobar antes que no hay planificador ni consumidor: `grep -rn 'tarea_aprobada\|CU96' servicios`.
- CA: Dado el repo, cuando se busca `tarea_automatizada` o `REQUIERE_APROBACION`, entonces no quedan rutas ni tablas vivas.
- DoD: `grep` vacío; build y verificación de deriva en verde.
- Depende de: que existan las microtareas de entrega automática e incumplimiento declarado por el sistema (lote L2, F3). Esta retira código sin uso, así que puede ir antes.
- Esfuerzo: medio

### F2.L3.M14 — Retirar la postulación y el emparejamiento (no hay grupos públicos)
- Hallazgos: HUECO-008 (S3) y SEG-002 (S2)
- Causa de raíz: CU-68 «postular» (ruta `pedir-cupo`, `MotorDeRecomendacion`) sirve a grupos abiertos a desconocidos y a una decisión humana. Para eso guarda el puntaje exacto de reputación en la evidencia y en un evento de Kafka, y se lo muestra al organizador. Con la entrada solo por invitación (tanda 5), la puerta deja de existir.
- Corrección de raíz:
  - Retirar `CU68Postular`, `CU68AceptarIngreso` (la decisión del organizador), `MotorDeRecomendacion`, `PrecondicionesDePostulacion`, `AdmisionDelGrupo.java:102`, `postulacion_emparejamiento`, las rutas del contrato y la ruta de la app `pedir-cupo` (`rutas.dart:47-52`, `pantalla_pedir_cupo.dart`).
  - Retirar del evento cualquier puntaje. Si quedan eventos históricos con puntaje en el tópico, purgarlos según la política de retención.
- CA: Dado el contrato `grupos.yaml`, cuando se listan las rutas, entonces no hay postulación ni emparejamiento, y ningún payload de evento de `grupos` contiene un puntaje de reputación.
- DoD:
  - `grep -rn 'puntaje\|postul' servicios/grupos contratos apps/movil/lib/pantallas/pasanaku` revisado;
  - tests de `grupos` en verde;
  - prueba de contrato de eventos.
- Depende de: F0.L3.M01 y F3.L3.M17.
- Esfuerzo: medio

### F2.L3.M15 — Retirar el inicio y el sorteo manuales
- Hallazgos: APROB-002 (S2), parte manual
- Causa de raíz: `POST /grupos/{id}/inicio`, `/sorteo` y `/sorteo/revelacion` (permiso `GRUPO_ADMINISTRAR`), y las pantallas de la app `inicio_del_grupo.dart` y `pantalla_sorteo_*` con sus acciones, ponen una persona en el circuito. La tanda 4 decidió que sea automático.
- Corrección de raíz: retirar esas rutas de `RutasDelGrupo.java:94-95` y `GruposController.java:164-173`, el permiso `GRUPO_ADMINISTRAR` sobre ellas, las acciones de la app (dejando la vista del resultado verificable del sorteo) y `ComprometerSorteo`/`RevelarSorteo` de `cu60_sortear_turnos.dart`. Va con versión del contrato.
- CA: Dado un grupo lleno, cuando un usuario llama `POST /grupos/{id}/inicio`, entonces 404, y el grupo se inicia solo por F3.L3.M18.
- DoD: pruebas web de 404; prueba de widget de la app sin botones de iniciar ni sortear.
- Depende de: F3.L3.M18. Se mergea después del trabajo programado: es la única excepción al orden de fases y se declara en el PR.
- Esfuerzo: chico

---

## F3 — Flujo nuevo

### F3.L3.M16 — Una sola conformación del grupo que siempre publica `grupo_iniciado` por outbox, más reparación
- Hallazgos: FLUJO-QR-001 (S2)
- Causa de raíz: hay dos caminos de conformación, `CU69FirmarReglamento.java:193-216` y `CU69IniciarGrupo`, y solo uno publica el hecho que abre la primera cuota y el QR. El grupo que se llena por firmas queda detenido sin error.
- Corrección de raíz:
  - Crear un método de dominio `Grupo.conformar()` en un repositorio de `infraestructura`. Hace el `UPDATE … WHERE estado = 'ABIERTO_A_INSCRIPCION'` y el `INSERT` en outbox de `grupo_iniciado` en la misma transacción. Los dos casos de uso lo llaman.
  - Agregar un trabajo de reparación, idempotente por `grupo_id`, que publica el evento para los grupos `CONFORMADO` que no lo tienen.
- CA: Dado un grupo que se completa por firmas, cuando firma el último, entonces hay exactamente un `grupo_iniciado` en outbox y `aportes` abre la cuota 1.
- DoD:
  - integración `grupos` + outbox contra base real;
  - prueba de duplicado del consumidor de `aportes` (el mismo evento dos veces, una sola cuota);
  - pacto de F0.L3.M02 en verde.
- Depende de: F0.L3.M02.
- Esfuerzo: medio

### F3.L3.M17 — Admisión única por invitación de un miembro, con requisitos verificados en el servicio dueño
- Hallazgos: DUP-INT-001 (S2), CONTRATO-001 (S2) y FRAGIL-002 (S3)
- Causa de raíz:
  - Hay cuatro implementaciones de «admitir»: `OrganizadorDecideIngreso`, `AdmisionRepositorio`, `CU69Enlace` y `SubastaDeCupoRepositorio`. Tienen efectos y eventos distintos.
  - La app usa puertas que producción cerraba.
  - El estado aceptado tiene dos nombres, `APROBADA` y `ACEPTADA`.
- Corrección de raíz:
  - Crear una sola operación de dominio `admitir(grupoId, invitacionId, usuarioId)`:
    - valida, con timeout y fallando cerrado, que el KYC esté aprobado en `identidad`, que no haya deudas vivas en `aportes`/`garantia` y que esté el reglamento firmado;
    - valida que la invitación la haya emitido un miembro y que siga vigente;
    - inserta el participante con precondición de cupo libre en el `UPDATE`;
    - publica un único evento `participante_admitido` por outbox.
  - Las puertas («canjear invitación» y «enlace de invitación») solo cambian el disparador.
  - Definir un solo conjunto de estados de la invitación en el modelo, el contrato y los clientes (Dart y backoffice), con versión.
  - En la app, `solicitudes_del_grupo.dart` y `cu69_unirse_por_enlace.dart` pasan a usar esta puerta.
- CA:
  - Dada una persona invitada que cumple los tres requisitos, cuando acepta, entonces entra sin intervención humana.
  - Si le falta uno, recibe 422 con el código del requisito.
  - Sin invitación de un miembro, no hay ruta para entrar.
- DoD:
  - integración con dobles de `identidad` y `aportes` en tres niveles (cumple, límite: deuda que vence hoy, inválido: servicio caído);
  - concurrencia en el último cupo (dos aceptaciones simultáneas: una entra y la otra recibe 409);
  - pacto del evento;
  - prueba de la app.
- Depende de: F0.L3.M01 y F0.L3.M02.
- Esfuerzo: grande

### F3.L3.M18 — Inicio y sorteo automáticos con orden por nivel de puntualidad, y cancelación con devolución si el grupo no se completa
- Hallazgos: APROB-002 (S2), parte automática, y HUECO-002 (S2)
- Causa de raíz: ADR-054 D3 y D4 se aceptaron sin código. No hay ningún `@Scheduled` en `grupos`, así que el grupo depende de un humano o queda abierto para siempre con cuotas cobradas sin destino.
- Corrección de raíz: un trabajo programado en `grupos`, con lock distribuido e idempotente por grupo:
  1. Cuando se completan los cupos, llama a `Grupo.conformar()` de F3.L3.M16.
  2. Cuando todas las cuotas 1 están acreditadas, porque `aportes` lo confirma por evento originado en el webhook del QR, compromete el sorteo.
  3. Revela el sorteo y fija el orden. El orden se arma agrupando por nivel de puntualidad, que se lee de `transparencia` (pieza D) al momento del compromiso y queda congelado en la evidencia del compromiso. Los niveles más altos van primero y dentro de cada nivel decide el sorteo con compromiso y revelación. En el primer ciclo todos son «Semilla» y el orden sale entero del sorteo.
  4. Al vencer el plazo de inscripción (parámetro obligatorio sin valor por defecto, D-L3-4), cancela el grupo (`ABIERTO_A_INSCRIPCION → CANCELADO`) y publica `grupo_cancelado` por outbox. `aportes`/`nucleo-financiero` ejecutan la saga de devolución de lo cobrado (lote L1).

  La evidencia del sorteo publica solo el nivel, nunca el puntaje.
- CA:
  - Dado un grupo que completa cupos y cuotas 1, cuando corre el trabajo, entonces queda con turnos asignados sin acción humana, y el orden respeta el nivel y es reproducible desde la evidencia publicada.
  - Dado un grupo incompleto al vencer el plazo, entonces queda `CANCELADO` y cada cuota cobrada tiene su devolución.
- DoD:
  - integración con reloj inyectado, para los dos caminos;
  - prueba de verificación del sorteo: recalcular el orden desde la semilla revelada;
  - doble de `transparencia` en tres niveles (niveles mixtos, todos iguales, servicio caído: el trabajo no compromete y reintenta);
  - prueba de lock (dos instancias, una ejecución);
  - el servicio no arranca sin el parámetro del plazo.
- Depende de: F3.L3.M16, F3.L3.M17, F3.L5.M39 y F3.L5.M41 (niveles de puntualidad, pieza D) y la de devolución del lote L1.
- Esfuerzo: grande

### F3.L3.M19 — Retiro voluntario sin aprobación, con liquidación según la posición y el modelo A
- Hallazgos: HUECO-004 (S2)
- Causa de raíz: `CU65Retirarse` deja la solicitud `PENDIENTE` esperando un `aprobar` que nadie llama. La aprobación es ELIMINABLE y falta la regla de liquidación.
- Corrección de raíz:
  - El caso de uso ejecuta el retiro en la misma transacción. Así se retira la aprobación (APR-06).
  - Antes del inicio: el participante sale y se devuelve íntegra su cuota cobrada, si la hubo, con una saga en el servicio dueño del dinero.
  - Si ya cobró el pozo y se retira: queda como deudor del fondo del grupo, con subrogación según el modelo A (lote L2), y su cupo queda vacante para una invitación.
  - Si se retira a mitad de ciclo sin haber cobrado: queda DECISION_REQUIRED (D-L3-3). La API responde 422 `RETIRO_NO_DISPONIBLE` hasta que Pablo decida, en vez de inventar un monto.
- CA:
  - Dado un participante antes del inicio, cuando se retira, entonces sale y su cuota se devuelve.
  - Dado uno que ya cobró, entonces queda con deuda al fondo y sin cupo.
  - En ningún caso queda una solicitud `PENDIENTE`.
- DoD:
  - integración por rama;
  - idempotencia con `Idempotency-Key` (el mismo pedido dos veces, un solo retiro);
  - pacto del evento hacia `garantia`.
- Depende de: F1.L3.M04, la microtarea de subrogación al fondo del lote L2 y D-L3-3 para la rama abierta.
- Esfuerzo: medio

### F3.L3.M20 — El ciclo de vida del grupo y del turno avanza con los hechos de cobro
- Hallazgos: HUECO-006 (S2)
- Causa de raíz: después del sorteo, el estado del grupo no tiene dueño en el código. `entregas` no puede escribir en `grupos` (regla 98.1) y `grupos` no consume ningún hecho de cobro ni de cierre. Por eso las guardas «turno ya cobrado» y la posición al retirarse están muertas.
- Corrección de raíz:
  - `grupos` consume `entrega_acreditada`, que viene del webhook del proveedor de desembolso (lote L2), y el cierre de periodo.
  - Mueve `turno → COBRADO`, `grupo → EN_CURSO/FINALIZADO` con precondición en el `UPDATE`, con deduplicación por `message_id` y DLQ.
  - Con esto, `TraspasoRepositorio.java:27-28` y `PosicionAlRetirarse.java:29-31` pasan a ver estados reales.
- CA: Dado un turno con entrega acreditada, cuando llega el evento (incluso duplicado), entonces el turno queda `COBRADO` una sola vez y ese cupo ya no se puede traspasar.
- DoD:
  - prueba de duplicado de mensaje;
  - pacto consumidor `entregas→grupos`;
  - integración del traspaso rechazado;
  - prueba con `entregas` caído: el estado queda atrasado y se expone «procesando» (98.4).
- Depende de: la microtarea de entrega automática con webhook del lote L2.
- Esfuerzo: medio

### F3.L3.M21 — Acuerdos del grupo: solo los tipos con efecto definido
- Hallazgos: HUECO-007 (S2) y CONTRATO-002 (S3)
- Causa de raíz: `CU63Acordar` registra votaciones sin efecto. El enum del contrato (`grupos.yaml:443,1409`) acepta tipos que el servicio no sabe leer y campos que ignora, como la modalidad de turnos, que ahora fija el sistema por puntualidad.
- Corrección de raíz:
  - Quedan solo los tipos que Pablo confirme (D-L3-1). Cada uno tiene su efecto en el servicio dueño, ejecutado por evento outbox al resolverse.
  - Los demás se retiran del modelo (`TipoDeAcuerdo`), del contrato con versión y de la app.
  - Supuesto mientras D-L3-1 está abierta: `REPETIR_SORTEO` y el campo de modalidad se retiran ya, porque contradicen el sorteo verificable y el orden por nivel.
  - `EXPULSION` y `CONDONACION_MORA` quedan en DECISION_REQUIRED, porque chocan con el incumplimiento declarado por el sistema (P-12) y con el reparto de incobrables del modelo A.
- CA: Dado un tipo de acuerdo aceptado por el contrato, cuando se aprueba, entonces su efecto ocurre. Un tipo retirado responde 422 del contrato, no 500.
- DoD:
  - prueba por tipo sobreviviente (efecto observable en el servicio dueño);
  - prueba de contrato del enum;
  - prueba 422.
- Depende de: D-L3-1 y F1.L3.M07.
- Esfuerzo: medio

### F3.L3.M22 — Textos de admisión por invitación en un solo catálogo
- Hallazgos: DOC-001 (S3)
- Causa de raíz: la app (`textos.dart:35-39`), la web (`invitacion.ts:53`) y el backoffice (`admisiones/textos.ts:4-5`) prometen tres trámites distintos, porque cada superficie escribió su propio supuesto.
- Corrección de raíz: un solo texto de producto sobre la entrada por invitación y sus requisitos, en el catálogo de textos compartido, con la marca AportaYa. Lo redacta Claude y lo aprueba Pablo (P-16). Se retiran los textos de admisión del backoffice con F2.L3.M10.
- CA: Dado el trámite de ingreso, cuando se lee en la app y en la web, entonces el texto es idéntico y menciona invitación, KYC, reglamento y deudas.
- DoD:
  - prueba que compara las claves del catálogo en las dos superficies;
  - aprobación de Pablo registrada en el PR.
- Depende de: F3.L3.M17 y P-16.
- Esfuerzo: chico

---

## F4 — Calidad y deuda

### F4.L3.M23 — Esquema: restricciones que nunca se disparan, historial mutable y acoples entre esquemas
- Hallazgos: DATOS-001 (S2), DATOS-002 (S3) y ACOPLE-001 (S2)
- Causa de raíz: las reglas de `sql/40_reglas` se escriben a mano con otro vocabulario que el DDL generado, y nada las ejercita. `historial_estado_grupo` falta en `35_append_only`. El modelo único genera FK y disparadores entre esquemas de servicios distintos.
- Corrección de raíz:
  - En `docs/Restricciones.md` → `40_reglas`:
    - corregir `uq_acuerdo_abierto` al estado real (`'ABIERTO'`);
    - corregir R-GRP-12 a `'APROBADA'`, o al estado que deje F3.L3.M19.
  - Las partes (3) y (4) caen con F2.L3.M12 y F2.L3.M13.
  - Agregar `historial_estado_grupo` a `35_append_only` (disparador y `REVOKE UPDATE, DELETE`).
  - Retirar las FK de `sql/20_claves/02_grupos_turnos.sql` hacia `identidad`, `aportes` y `garantia`, con un plan de expansión y contracción. La validación pasa a hacerse por contrato del servicio dueño.
  - Agregar a `verificaciones.sql` una prueba que intente violar cada regla.
- CA:
  - Dada cada regla de `40_reglas` del esquema `grupos`, cuando se intenta violarla, entonces la base la rechaza.
  - Ningún `UPDATE` sobre `historial_estado_grupo` prospera.
  - No queda FK de `grupos` a otro esquema.
- DoD:
  - `sql/aplicar.sql` + `verificaciones.sql` con salida pegada;
  - deriva en verde;
  - consulta a `information_schema` sin FK cruzadas.
- Depende de: F2.L3.M11, F2.L3.M12 y F2.L3.M13.
- Esfuerzo: grande

### F4.L3.M24 — Enumerados de dominio, SQL fuera de la capa de aplicación y cliente HTTP único
- Hallazgos: FRAGIL-004 (S3), ARQ-001 (S3) y DUP-X-001 (S3)
- Causa de raíz:
  - Faltan enumerados de dominio para estados y roles.
  - 13 casos de uso escriben jOOQ en `aplicacion/`.
  - `ClienteConCircuito` está copiado en cinco servicios.
  - Las tres cosas vienen de reescrituras en paralelo sin un patrón común.
- Corrección de raíz:
  - Extender el enumerado de F1.L3.M06 a roles y estados de turno y solicitud, pasándolos por parámetro al SQL.
  - Mover el SQL de los 13 casos de uso a repositorios detrás de `dominio/puertos`, como hacen los otros 22.
  - Extraer `ClienteConCircuito` a `plataforma/comun-web` y borrar las copias de `grupos`, `aportes`, `entregas` y `garantia`. La de `organizador` cae con F2.L3.M12.
  - Es refactor sin cambio de comportamiento: la suite existente es la red.
- CA: Dado `servicios/grupos/.../aplicacion`, cuando se busca `DSLContext`, entonces no hay coincidencias, y existe una sola clase `ClienteConCircuito` en el repo.
- DoD:
  - `grep` vacíos;
  - `./gradlew test` de los cuatro servicios en verde antes y después;
  - prueba de caída del dependido sobre el cliente común.
- Depende de: F2.L3.M12.
- Esfuerzo: grande

### F4.L3.M25 — Cola de muertos y compensación para los consumidores de `grupos`
- Hallazgos: RESILIENCIA-002 (S2)
- Causa de raíz: `ConsumidorDeCesionesKafka` y sus pares descartan el evento tras los reintentos, sin degradación declarada (98.2.4). Una cesión ya cobrada puede perderse.
- Corrección de raíz:
  - DLQ con alerta y reproceso en el consumidor común.
  - Si la cesión es definitivamente inválida, `grupos` publica `cesion_rechazada` y `entregas` compensa de forma idempotente.
  - Si Pablo confirma que la subasta o venta de turnos cae con el modelo B (D-L3-2), esta microtarea se reemplaza por el retiro de esos consumidores.
- CA: Dada una cesión rechazada por `grupos`, cuando se agotan los reintentos, entonces queda en la DLQ con alerta, y la compensación devuelve el dinero una sola vez.
- DoD:
  - prueba de duplicado;
  - prueba de mensaje envenenado que termina en la DLQ;
  - saga con compensación ejercitada contra `entregas` real en integración.
- Depende de: D-L3-2.
- Esfuerzo: medio

### F4.L3.M26 — Idempotencia transversal en las mutaciones de `grupos`
- Hallazgos: RESILIENCIA-003 (S3)
- Causa de raíz: el contrato declara `Idempotency-Key`, pero el patrón no es transversal. Solo CU-20 la guarda y la devuelve.
- Corrección de raíz: usar el componente de idempotencia de `plataforma` corregido en F1.L5.M08, en todas las mutaciones que sobrevivan: retiro, permuta, acuerdos y traspaso por subasta. Se guarda la respuesta en una tabla con `UNIQUE` y se devuelve en el reintento. Sorteo e inicio dejan de ser rutas con F2.L3.M15.
- CA: Dada la misma petición con la misma clave, cuando se repite, entonces se devuelve la respuesta original y no hay un segundo efecto ni un 500.
- DoD: prueba de idempotencia por endpoint, contra base real.
- Depende de: F1.L5.M08.
- Esfuerzo: medio

### F4.L3.M27 — Fechas, estado de la app, catálogo de errores, verificador de rutas, interfaz, rastro de lectura y limpieza
- Hallazgos: DEFECTO-004 (S3), DEFECTO-005 (S3), HUECO-003 (S3), PRUEBAS-001 (S4), UX-A11Y-001 (S4), TRAZA-001 (S3), ACOPLE-002 (S3) y CLEAN-001 (S4)
- Causa de raíz: cada hallazgo tiene su propia causa (ver la ficha de origen). Se agrupan solo por fase. **Se ejecutan como subtareas separadas, una por hallazgo, cada una con su propio PR.**
- Corrección de raíz:
  - (a) DEFECTO-004:
    - `Reloj.hoyEnLaPaz()` único en `plataforma`;
    - fecha fin calculada con la misma función del calendario;
    - `CanjearInvitacion` usa el reloj inyectado.
  - (b) DEFECTO-005: proveedor `family(grupoId).autoDispose` en `cu69_invitar_al_grupo.dart`.
  - (c) HUECO-003: generar `apps/movil/lib/dominio/errores.dart` desde la tabla de errores de los contratos.
  - (d) PRUEBAS-001: el verificador empareja por plantilla de ruta completa.
  - (e) UX-A11Y-001: vacíos con explicación, tokens tipográficos y la marca AportaYa en lugar de «Aporteo».
  - (f) TRAZA-001: registrar la lectura de backoffice en la bitácora de lectura (`comun.registro_acceso_datos`, como en F3.L5.M40).
  - (g) ACOPLE-002: el formulario de alta toma los campos del contrato (cupos, día de cobro, moneda), sin constantes en pantalla.
  - (h) CLEAN-001: retirar `EstadoDeSorteo`, `factor-fijo` «SOLO TEST» de la configuración principal y lo demás sin referencias que no haya caído en F2.
- CA, por subtarea:
  - (a) Un grupo iniciado a las 22:00 en La Paz tiene su periodo 1 en la fecha local.
  - (b) Invitar en A no se ve en B.
  - (c) Cada código del contrato tiene su texto.
  - (d) El verificador queda en verde sin entradas «conocidas».
  - (e) Ningún vacío con `''`.
  - (f) Cada lectura de backoffice deja una fila con quién y qué.
  - (g) Se puede crear un grupo en USD si el contrato lo admite.
  - (h) `grep` de cada símbolo vacío.
- DoD:
  - (a) unitaria con reloj fijo;
  - (b) prueba de widget;
  - (c) prueba que compara el catálogo con el contrato;
  - (d) `python scripts/verificar_rutas_frontend.py`;
  - (e) prueba visual en tres viewports (95.7);
  - (f) integración con `auditoria`;
  - (g) prueba de widget y de contrato;
  - (h) build y tests.
- Depende de: F2.L3.M10, F2.L3.M12 y F3.L5.M40 (bitácora de lectura).
- Esfuerzo: medio

---

## Trazabilidad hallazgo → microtarea (55 de 55)

| # | Hallazgo | Sev. | Microtarea | Tipo |
|---:|---|---|---|---|
| 1 | grupos-organizador-APROB-001 | S2 | F2.L3.M10 | corrección |
| 2 | grupos-organizador-APROB-002 | S2 | F3.L3.M18 + F2.L3.M15 | corrección |
| 3 | grupos-organizador-RESPALDO-001 | S3 | F0.L3.M01 | corrección |
| 4 | grupos-organizador-FLUJO-QR-001 | S2 | F3.L3.M16 | corrección |
| 5 | grupos-organizador-COHERENCIA-002 | S2 | F2.L3.M11 | corrección |
| 6 | grupos-organizador-COHERENCIA-001 | S3 | F2.L3.M10 | se resuelve con |
| 7 | grupos-organizador-DUP-INT-001 | S2 | F3.L3.M17 | corrección |
| 8 | grupos-organizador-DUP-X-001 | S3 | F4.L3.M24 | corrección |
| 9 | grupos-organizador-REESCRITURA-001 | S3 | F2.L3.M12 | se resuelve con |
| 10 | grupos-organizador-FRAGIL-001 | S2 | F2.L3.M10 | se resuelve con |
| 11 | grupos-organizador-FRAGIL-003 | S2 | F2.L3.M11 + F2.L3.M12 | se resuelve con |
| 12 | grupos-organizador-FRAGIL-002 | S3 | F3.L3.M17 | corrección |
| 13 | grupos-organizador-FRAGIL-004 | S3 | F4.L3.M24 | corrección |
| 14 | grupos-organizador-DEFECTO-001 | S2 | F2.L3.M12 | se resuelve con |
| 15 | grupos-organizador-DEFECTO-002 | S2 | F1.L3.M06 | corrección |
| 16 | grupos-organizador-DEFECTO-003 | S2 | F1.L3.M07 | corrección |
| 17 | grupos-organizador-DEFECTO-004 | S3 | F4.L3.M27 (a) | corrección |
| 18 | grupos-organizador-DEFECTO-005 | S3 | F4.L3.M27 (b) | corrección |
| 19 | grupos-organizador-HUECO-001 | S2 | F2.L3.M10 | se resuelve con |
| 20 | grupos-organizador-HUECO-002 | S2 | F3.L3.M18 | corrección |
| 21 | grupos-organizador-HUECO-004 | S2 | F3.L3.M19 | corrección |
| 22 | grupos-organizador-HUECO-006 | S2 | F3.L3.M20 | corrección |
| 23 | grupos-organizador-HUECO-007 | S2 | F3.L3.M21 | corrección (D-L3-1) |
| 24 | grupos-organizador-HUECO-003 | S3 | F4.L3.M27 (c) | corrección |
| 25 | grupos-organizador-HUECO-005 | S3 | F2.L3.M12 | se resuelve con |
| 26 | grupos-organizador-HUECO-008 | S3 | F2.L3.M14 | se resuelve con |
| 27 | grupos-organizador-RESILIENCIA-002 | S2 | F4.L3.M25 | corrección (D-L3-2) |
| 28 | grupos-organizador-RESILIENCIA-001 | S3 | F2.L3.M12 + F2.L3.M13 | se resuelve con |
| 29 | grupos-organizador-RESILIENCIA-003 | S3 | F4.L3.M26 | corrección |
| 30 | grupos-organizador-DINERO-002 | S1 | F1.L3.M03 | corrección |
| 31 | grupos-organizador-DINERO-001 | S2 | F2.L3.M12 | se resuelve con |
| 32 | grupos-organizador-DINERO-003 | S3 | F1.L3.M08 | corrección |
| 33 | grupos-organizador-DEBIDO-PROCESO-001 | S2 | F2.L3.M12 | se resuelve con |
| 34 | grupos-organizador-DEBIDO-PROCESO-002 | S2 | F2.L3.M12 | se resuelve con |
| 35 | grupos-organizador-ARQ-001 | S3 | F4.L3.M24 | corrección |
| 36 | grupos-organizador-CLEAN-001 | S4 | F4.L3.M27 (h) | corrección |
| 37 | grupos-organizador-ACOPLE-001 | S2 | F4.L3.M23 (+F2.L3.M11) | corrección |
| 38 | grupos-organizador-ACOPLE-002 | S3 | F4.L3.M27 (g) | corrección |
| 39 | grupos-organizador-CONTRATO-001 | S2 | F3.L3.M17 | corrección |
| 40 | grupos-organizador-CONTRATO-002 | S3 | F3.L3.M21 | corrección |
| 41 | grupos-organizador-DATOS-001 | S2 | F4.L3.M23 (+F2.L3.M12, F2.L3.M13) | corrección |
| 42 | grupos-organizador-DATOS-002 | S3 | F4.L3.M23 | corrección |
| 43 | grupos-organizador-PRUEBAS-002 | S3 | F2.L3.M10, F2.L3.M12, F3.L3.M17, F3.L3.M18 | se resuelve con |
| 44 | grupos-organizador-PRUEBAS-003 | S3 | F0.L3.M02 | corrección |
| 45 | grupos-organizador-PRUEBAS-001 | S4 | F4.L3.M27 (d) | corrección |
| 46 | grupos-organizador-UX-A11Y-001 | S4 | F4.L3.M27 (e) | corrección |
| 47 | grupos-organizador-OBSERV-001 | S2 | F1.L3.M03 | corrección |
| 48 | grupos-organizador-DOC-001 | S3 | F3.L3.M22 | corrección |
| 49 | grupos-organizador-DOC-002 | S3 | F2.L3.M12 | se resuelve con |
| 50 | grupos-organizador-SEG-001 | S1 | F1.L3.M04 | corrección |
| 51 | grupos-organizador-SEG-002 | S2 | F2.L3.M14 | se resuelve con |
| 52 | grupos-organizador-SEG-003 | S2 | F2.L3.M13 | se resuelve con |
| 53 | grupos-organizador-SEG-004 | S3 | F1.L3.M05 | corrección |
| 54 | grupos-organizador-SEG-005 | S3 | F1.L3.M09 | corrección |
| 55 | grupos-organizador-TRAZA-001 | S3 | F4.L3.M27 (f) | corrección |

Verificación: 55 filas, que coinciden con las 55 fichas `### grupos-organizador-*` del informe. Hay 2 S1, 25 S2, 25 S3 y 3 S4, igual que la fila «Total» del informe.
