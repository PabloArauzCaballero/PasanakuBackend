# Reporte — F1: la identidad sale del token y el acceso es real

> **AVANCE: 11 / 11 — 100 %** de las microtareas de este carril. Peldaño alcanzado: **TESTED**, no VERIFIED. En el plan maestro, F1.S3.M1 queda **A MEDIAS** porque el token de acceso sigue sirviendo hasta 15 min (ver A medias).

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `correcciones/f1-identidad-token-acceso-2026-11-21` (base `origin/dev` 850e0f51)
- Commits: `7d2fc896` F1.S2.M1 · `7c40ea59` F1.S3.M1 · `c21797e9` F1.S3.M2
- Peldaño por área:
  - comun-web: TESTED (unitarias, web y mutante).
  - identidad: TESTED (unitarias, web e integración contra PostgreSQL real local; RLS ejercitada con `psql` y `SET ROLE svc_identidad`).
  - No llega a VERIFIED: no se levantó el servicio entero ni se probó por HTTP con un token real firmado.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 (F1.S2.M1) | `ActorAutenticado`, `SegundaFirma` y `CuatroOjos` en `comun-web/seguridad`. Se rechaza: misma persona (aun desde otra sesión), sesión compartida, rol `sistema` y token sin `sesion_id`. | `./gradlew :plataforma:comun-web:test --tests '*CuatroOjos*'` | PASS 6/6. Al quitar la regla, fallan 3 pruebas. `evidencia/h1-cuatro-ojos.txt` |
| H1.S1.M2 | Matriz web de la segunda firma. Otra persona → 200. Misma sesión → 422. Misma persona en otra sesión → 422. Aprobador en el cuerpo → 400 `AP-VAL-02`. Token sin `sesion_id` → 401. Sin token → 401. Sin permiso → 403. | `./gradlew :plataforma:comun-web:webTest --tests '*CuatroOjos*'` | PASS 7/7 |
| H1.S1.M3 | Guía de uso `plataforma/comun-web/docs/cuatro-ojos.md` | archivo en el commit 7d2fc896 | PASS |
| H2.S1.M1 (F1.S3.M1) | Átomo `PuedeAutenticarse`: lista blanca de los 8 estados de `ck_usuario_estado`. Un estado desconocido no entra. | `./gradlew :servicios:identidad:test --tests '*PuedeAutenticarse*'` | PASS 13/13 |
| H2.S1.M2 | El login rechaza SUSPENDIDO, BLOQUEADO_SEGURIDAD, BAJA_VOLUNTARIA y ELIMINADO con `AP-CU04-02`, sin abrir sesión. Con clave equivocada responde `AP-CU04-01` y no revela el estado. | `integrationTest` contra la Postgres local (`CU04CuentaNoHabilitadaTest`) | PASS 8/8. Al quitar el chequeo, fallan los 4 estados. `evidencia/h2-h3-integracion-postgres-local.txt` |
| H2.S1.M3 | El refresco de una cuenta no habilitada no renueva y revoca la sesión (`CUENTA_NO_HABILITADA`). Reactivar la cuenta no resucita esa sesión. | ídem + `psql` con `SET ROLE svc_identidad` | PASS. `evidencia/h2-psql.txt` (la revocación es idempotente: `UPDATE 1`, después `UPDATE 0`) |
| H2.S1.M4 | Desbloquear un interno libera `bloqueo_cuenta` (con `liberada_por` del token), que es lo que lee el login | ídem | PASS |
| H3.S1.M1 (F1.S3.M2) | `VerificadorTotp`: vectores RFC 6238, ventana ±1 paso, un solo uso por paso, comparación en tiempo constante. `000000` no entra. | `./gradlew :servicios:identidad:test --tests '*Totp*'` | PASS 14/14 + 3/3 |
| H3.S1.M2 | `LlavesDeFactor` obligatorias, sin valor por omisión, con versión de llave. Una llave corta o una versión ausente hace fallar el arranque. | `--tests '*LlavesDeFactor*'` | PASS 15/15 |
| H3.S1.M3 | Se borra `DesafioDeDesarrollo`. `DesafioPorTipo` es el desafío primario en todos los perfiles. | `grep -rn '"000000"' servicios/identidad/src/main` → sin resultados · `CableadoDelSegundoFactorTest` | PASS (2/2) |
| H3.S1.M4 | Enrolamiento del titular con `POST /sesion/factores/totp` y su `/confirmacion`. Pruebas: el operador entra con un código real, `000000` y un código reusado no entran, un enrolamiento ajeno → `AP-CU04-08`, no se reemplaza un TOTP confirmado (`AP-CU04-09`) y el secreto queda cifrado. | `EnrolamientoTotpWebTest` + `CU04TotpTest` contra la Postgres local | PASS 8/8 web y 6/6 integración. Al aceptar `000000`, el mutante falla. |

Regresión (`evidencia/regresion.txt`):

| Módulo | Pruebas | Resultado |
|---|---|---|
| comun-web | test 24/24 · webTest 53/53 | verde |
| identidad | test 87/87 · webTest 160/160, con la barrida `SeguridadWebTest` | verde |
| identidad | integrationTest de las 9 clases `BaseDeCU04` (61/61) | verde |
| nucleo-financiero (consumidor de `ContextoWebDePrueba`) | webTest 82/82 | verde |

## A medias

### F1.S3.M1 del plan maestro: «su token vigente deja de servir»
- **Qué anda:** una cuenta suspendida, bloqueada o de baja no obtiene token ni lo renueva, y su sesión queda revocada al intentar renovar. Suspender a un interno ya revocaba sus sesiones (`UsuariosInternosRepositorio.actualizar`), y eso no se tocó.
- **Qué no anda:** el **token de acceso** ya emitido (JWT de 15 min, `EmisorDeAcceso.VIGENCIA`) sigue siendo válido en los otros servicios hasta que vence, porque validan la firma sin consultar la sesión.
- **Qué falta exactamente:** F1.L4.M15, que pide verificar la sesión vigente (`sesion_id`) en las operaciones de dinero. Depende de F0.L4.M05 (pactos) y toca el decodificador compartido.
- **Dónde quedó:** sin cambios para esto. La rama compila y sus pruebas están en verde.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Migración de endpoints a `CuatroOjos` (tabla abajo) | TODO | Próxima tanda, por lote (fuera de este carril por pedido). |
| F1.L4.M01, resto: retirar `usuario_interno.bloqueado_hasta` y que la vista de interno muestre el bloqueo de `bloqueo_cuenta` | TODO | Cambio de esquema F0.L4.M04 en `docs/entidades` → `generar_ddl.py`, que está fuera de mi alcance de archivos. |
| F1.L4.M03: arranque fail-closed de `JWT_CLAVE_FIRMA` e `INVITACIONES_CLAVE`, y `${…:?}` en el compose | TODO | No se tomó en esta sesión. Toca `EmisorDeAcceso` y `docker-compose.coolify.yml`, y el compose está fuera de alcance. |
| F1.L4.M04: alta de operador por invitación (fija la contraseña y enrola el TOTP) | TODO | Depende de esta M02, ya hecha. Incluye pantalla de backoffice (`apps/`), fuera de alcance. |
| F1.L4.M14: tope de intentos fallidos sin carreras | TODO | No tomada. |
| F1.L4.M15: `iss`/`aud`, refresh con HMAC y pepper, sesión vigente en dinero | TODO | Depende de F0.L4.M05. |
| F1.L4.M16: desafíos y step-up persistidos entre réplicas | TODO | No tomada. El TOTP ya no depende de memoria, pero el código por correo y los desafíos de step-up sí. |
| Códigos de respaldo (`RESPALDO`) y rotación del TOTP con step-up | TODO | Diseño en F1.L4.M02 y M16. **DECISION_REQUIRED** sobre cómo se recupera un TOTP perdido (ver Decisiones). |

### Endpoints que hoy toman un aprobador o un actor del cuerpo (para la próxima tanda)

Inventario hecho por lectura de código. Verifiqué cada fila en la línea citada (`grep get<Campo>` en los controladores).

| Servicio | Endpoint | Dónde | Campo del cuerpo | Uso actual | Anexo |
|---|---|---|---|---|---|
| nucleo-financiero | `POST /billetera/reversos` | `web/BilleteraController.java:232`, `web/MapeoDeBilletera.java:155` | `autorizadaPor` | Se guarda como autorizador. `CU14ReversarTransaccion.java:72` solo rechaza el propio UUID, así que cualquier otro UUID pasa. | L1 ANF-SEG-005 (F1.L1.M03) |
| garantia | `POST /incumplimientos/{expedienteId}/cobertura` | `web/IncumplimientosController.java:145,153` | `aprobadaPor` | Si no es nulo, la cobertura salta a APLICADA sin aprobación (`CU23CubrirIncumplimiento.java:126`). Sin cuatro ojos. | L2 SEG-003 / APROB-002 (F1.L2.M02) |
| cumplimiento | `POST /cumplimiento/usuarios/{usuarioId}/diligencia` | `web/AdhesionController.java:155,164-165` | `aprobadaPor`, `segundaRevisionPor` | Los dos se guardan. Para PEP solo se exige que el segundo esté presente, no que sea distinto. | L4 IDCU-SEG-119 (F1.L4.M05) |
| cumplimiento | `POST /cumplimiento/casos` | `web/CumplimientoController.java:80,84` | `analistaId` | Quien abre el caso nombra a un analista inventado y después lo decide él mismo (`CU44DecidirComoRevisor` compara contra ese UUID del cuerpo). | L4 IDCU-SEG-120 (F1.L4.M06) |
| tarifas | `POST /comisiones/{devengoId}/devoluciones` | `web/ComisionesController.java:163,175` | `autorizadaPor` | Se guarda como autorizador. `CU33DevolverComision` no lee `ctx.usuarioId()`. | L5 TYA-DINERO-002 (F1.L5.M13) |
| tarifas | `POST /tarifas/tarifarios` | `web/TarifasController.java:114,123` | `aprobadoPor` | Se guarda como aprobador del cambio de tarifa. Sin cuatro ojos. | sin ID (hallazgo nuevo) |
| cumplimiento | `POST /cumplimiento/continuidad/pruebas` | `web/AccionesDeCumplimientoWeb.java:90` | `ejecutadaPor` | Atestación de quién ejecutó; no es una aprobación, pero el actor sale del cuerpo. | sin ID (hallazgo nuevo) |

Ya toman el aprobador del token, según el mismo inventario: retiros (`ResolucionDeRetiro`), entregas (CU22), reembolsos (CU19), CU43, CU47, CU48, cierre de billetera (CU16) y las aprobaciones de ERP y publicidad. Al migrarlos a `CuatroOjos` hay que confirmar que usan **dos actos con dos tokens** y no solo `ctx ≠ campo`.

## Evidencia

Todo en [`evidencia/`](./evidencia/):

| Archivo | Qué contiene |
|---|---|
| `h1-cuatro-ojos.txt` | Pruebas dirigidas y mutante de F1.S2.M1 |
| `h2.sql` + `h2-psql.txt` | Revocación por refresco y liberación del bloqueo con `SET ROLE svc_identidad` (datos sintéticos, `ROLLBACK`) |
| `h3-rls.sql` + `h3-rls-psql.txt` | RLS de `factor_mfa`. El titular inserta y confirma; el sistema lee y marca el uso. Mismo paso o uno anterior → `UPDATE 0`; paso siguiente → `UPDATE 1`. Otro participante ve 0 filas. |
| `h2-h3-integracion-postgres-local.txt` | 61/61 de integración con el código final, más los mutantes |
| `regresion.txt` | Suites completas, spotless y consumidor |

```text
$ ./gradlew :plataforma:comun-web:test :plataforma:comun-web:webTest :servicios:identidad:test :servicios:identidad:webTest
plataforma/comun-web test: tests=24 skipped=0 failures=0 errors=0
plataforma/comun-web webTest: tests=53 skipped=0 failures=0 errors=0
servicios/identidad test: tests=87 skipped=0 failures=0 errors=0
servicios/identidad webTest: tests=160 skipped=0 failures=0 errors=0
```

## No cubierto

- **NO COMPROBADO — requiere Docker (máquina de Pablo).** Las pruebas con `BaseDePrueba.contenedor()` directo, entre ellas `ArranqueTest`, `ArranquePuertaDeEntradaTest` y `ArranqueEntradaMalFormadaTest`, que confirman que el contexto Spring arranca con `MFA_LLAVES` y `MFA_LLAVE_VIGENTE`, y `UsuariosInternosRepositorioTest`. Comando:
  `./gradlew :servicios:identidad:integrationTest` y después `./gradlew :plataforma:comun-web:integrationTest`.
- La integración local se conectó como `pasanaku`, que es superusuario, así que **no ejercitó RLS desde Java**. Las políticas se probaron aparte con `psql` y `SET ROLE svc_identidad`.
- El servicio no se levantó entero: no hubo llamada HTTP real con un JWT firmado por identidad, ni prueba con dos réplicas.
- El step-up (`CU04StepUp.verificar`) con TOTP real tiene cubierto el cableado; no tiene prueba de integración propia.
- No hay prueba de concurrencia real de dos usos simultáneos del mismo código. La garantía es la precondición en el `UPDATE`, probada en secuencia con `psql`.
- Contrato: no corrí `openApiValidate` ni ninguna prueba de contrato contra los clientes Angular y Dart, y esos clientes no se regeneraron.

## Desvíos del plan

1. **Un solo PR con tres commits, uno por microtarea** del plan maestro (F1.S2.M1, F1.S3.M1, F1.S3.M2), en lugar de tres PR. La sesión tiene autorizada una sola rama de push. Cada commit se revisa por separado.
2. Los ajustes de `CU04CuentaNoHabilitadaTest` (cast `::timestamptz`, fila de `usuario_interno`, consulta igual a la del login) quedaron en el commit de F1.S3.M2.
3. Spotless reformateaba 4 archivos ajenos que ya venían mal en `dev`: `EnmascaradorPii`, `ConfirmarVerificacionCorreo`, `SolicitarVerificacionCorreo` y `EmisorDeAcceso`. **No se tocaron**; `spotlessJavaCheck` queda en rojo solo por ellos.

## Riesgos residuales y deuda

- **Operación de TEST.** Al quitar `000000`, los operadores sin TOTP real no pueden entrar al backoffice. Pueden enrolar su TOTP mientras son participantes, antes de recibir el rol de operador, o esperar F1.L4.M04. Las semillas de `sql/61_dev` (`enc:v1:demo-totp-admin-dev`) no son descifrables: el verificador las trata como factor inválido y no da error 500.
- **Despliegue.** `MFA_LLAVES` y `MFA_LLAVE_VIGENTE` son obligatorias: identidad **no arranca** sin ellas. Hay que agregarlas al almacén de secretos y al compose, que está fuera de mi alcance, antes de desplegar.
- `VerificadorTotp` usa una transacción propia (`REQUIRES_NEW`): el código queda gastado aunque la operación que lo pidió se revierta. Es intencional.
- Evento nuevo `identidad.factor_enrolado` en el outbox, todavía sin consumidor.

## Decisiones y ambigüedades

| # | Tema | Supuesto tomado | A quién confirmar |
|---|---|---|---|
| A1 | El PLAN maestro pide 422 para un cuerpo con aprobador; la plataforma (`CuerposEstrictos`) y el anexo L4 dicen 400 `AP-VAL-02` | Se conserva 400, que es el contrato vigente | Pablo |
| A2 | Qué estados de `usuario.estado` pueden autenticarse | Entran ACTIVO, PRE_REGISTRO, INCOMPLETO_KYC y PENDIENTE_VERIFICACION. Todo lo demás, y cualquier estado desconocido, no. | Pablo |
| A3 | Secreto TOTP «como hash con pepper» (PLAN) | No es posible: verificar exige el secreto en claro. Se guarda cifrado AES-256-GCM con `version_llave`, como ya modela `factor_mfa`. | Pablo (informativo) |
| D1 | **DECISION_REQUIRED:** cómo se recupera una cuenta que perdió su TOTP (soporte, códigos de respaldo, identidad + step-up) | No se implementó ningún camino. El mensaje dice «pedilo a soporte». | Pablo / Seguridad |
| D2 | Textos nuevos para usuarios (P-16): «Tu cuenta no esta habilitada para ingresar. Escribinos a soporte.», «Ya tenes un segundo factor activo. Para cambiarlo, pedilo a soporte.», «Ese codigo no coincide. Revisa la hora del telefono y proba de nuevo.», «Ese enrolamiento no existe, no es tuyo o ya se confirmo.», «La segunda firma tiene que ser de otra persona.» y el emisor «AportaYa» en la app de autenticación | Redactados por Claude | Pablo los aprueba |
| — | Dependencia nueva para TOTP | **Ninguna**: se reusó `CodigoTotp` (JDK `HmacSHA1`, con los vectores de la RFC) y `CifradorSimetrico` (AES-GCM) | — |

## Estado del entorno al cerrar

La base PostgreSQL local (puerto 5433) se apaga al cerrar. No queda ningún proceso de Gradle en background.

## Candado `blocker_gate` al cerrar

Al cerrar, `blocker_gate.py` bloqueó por 9 microtareas viejas en BLOQUEADO o EN CURSO de otros
trabajos (`2026-10-03-alta-fecha-actividad-autocaptura`, `2026-10-14-resolver-conflictos-prs-60-63`,
`2026-10-16-ci-formato-test`, `2026-10-27-rediseno-backoffice-al-repo-desplegado`,
`2026-10-28-admision-directa-backoffice`, `2026-10-30-arreglos-test-y-espejo`). Ninguna es de
este carril. Según DECISIONES P-20, se marcan DESCARTADO en **F0.M1**, en un PR propio que revisa
Pablo. No se tocaron porque quedan fuera del alcance de archivos de esta sesión.
Este carril no tiene microtareas en BLOQUEADO ni en EN CURSO.
