# Reporte — Código de acceso por correo y correo repetido cortado en el paso del correo

> **AVANCE: 14 / 15 — 93,3 %.** Desplegado y verificado en TEST: el contenedor tiene el código nuevo y el servicio real devuelve `AP-CU01-03`. Usuarios finales de TEST borrados (ejecutado por Pablo, 2026-10-16 19:14). **No observado:** que el código de acceso llegue a una bandeja real.

- Fecha: 2026-10-16 · Plan: [PLAN.md](./PLAN.md)
- PRs: #85, #88 y #92 → `test` · #91 y #93 → `dev`. Todos MERGED.
- Peldaño de evidencia: backend identidad `VERIFIED` para #88 (camino real en TEST) · `TESTED` + desplegado para #85 (falta ver el correo llegar) · limpieza `BLOQUEADO`.
- Microtareas descartadas (no cuentan en el total): las 6 de H2, el camino de despacho de `notificaciones`. Ver Decisiones.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `factor_mfa.tipo` admite `CORREO` (modelo → DDL), con evolución idempotente del CHECK (`CK_AMPLIADOS`) | `python3 scripts/generar_ddl.py` ×2 | PASS · evidencia/01 |
| H1.S1.M2 | Aplicado en la base local y gate de deriva en verde | `psql -f aplicar.sql` ×2 + `EsquemaAlDiaRepositorioTest` | PASS · evidencia/02, 07 |
| H1.S2.M1 | Texto propio de "código para entrar" en el correo, reusando `CorreoDeVerificacion`/Gmail | `--tests '*GmailApiCorreoDeVerificacion*'` 5/5 | PASS · evidencia/03 |
| H1.S2.M2 | `DesafioLocal` envía `CORREO`; si el proveedor falla, no queda código vivo ni hay 500 | `--tests '*DesafioPorCorreo*'` 5/5 (contra un doble) | PASS · evidencia/04 |
| H1.S2.M3 | Perfil `local` (el de TEST): el correo sale de verdad y `000000` sigue entrando | `--tests '*DesafioDeDesarrollo*'` 4/4 · `CableadoDelSegundoFactorTest` 2/2 | PASS · evidencia/05, 07 |
| H1.S2.M4 | Correo confirmado en el alta = token `VERIFICACION_CORREO` consumido, con la huella del correo actual | `CU04CodigoPorCorreoTest` | PASS · evidencia/06 |
| H1.S2.M5 | CU-04: participante sin factor y con correo confirmado → código por correo; el operador sigue con TOTP | `integrationTest --tests '*CU04*'` 38/38 | PASS · evidencia/06 |
| H1.S2.M6 | Regresión de identidad | `check` + `integrationTest` | PASS (con rojos previos, ver Riesgos) · evidencia/07 |
| H3.S1.M1 | `identidad` reconstruida y desplegada en Contabo con #85 y #88 | `docker ps` + clases dentro del contenedor | PASS · evidencia/09 |
| H4.S1.M1 | El correo repetido se corta al pedir el código: 422 `AP-CU01-03`, `campo=correo`, sin envío | `CU01VerificacionCorreoTest` 2/2 + `curl` en TEST | PASS · evidencia/08, 09 |
| H4.S1.M2 | OpenAPI lista `AP-CU01-03` en el 422 de la ruta | `grep` | PASS · evidencia/08 |
| H5.S1.M1 | PRs mergeados en `test` y `dev` | `gh pr view` | MERGED |
| H5.S1.M2 | Redespliegue en Contabo | `docker ps` / Coolify | PASS · evidencia/09 |

## A medias
### H3.S1.M2 — Login real en TEST con el código llegando por correo
- **Qué anda:** el contenedor de TEST tiene el código de #85: `correoVerificadoDe` está en las clases, el perfil es `local` y el proveedor `gmail_api`. El esquema tiene `CORREO` y el arranque no dio errores. Gmail ya entrega en TEST el código del alta.
- **Qué no anda / no se vio:** nadie inició sesión desde el despliegue (0 intentos entre 21:49 y 22:08 UTC), así que no se observó un correo de acceso llegando.
- **Qué falta exactamente:** que un participante **sin factor y con el correo confirmado en el alta** entre desde un dispositivo nuevo y reciba el correo "Tu código para entrar a AportaYa". Después, revisar `intento_autenticacion` (EXITOSO) y que en el log no aparezca `por correo no se pudo enviar`.
- **Dónde quedó:** `origin/test` (`2444abf5`), desplegado. Compila y los tests pasan.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Limpieza de usuarios finales en TEST | HECHO (2026-10-16 19:14, ver abajo) | — |

**Resultado de la limpieza.** Pablo la autorizó y la ejecutó él con `!`, porque el clasificador de permisos de
Claude Code no dejaba correrla desde esta sesión. Salida, recortada:
```text
-rw------- 1 root root 2976080 Oct 16 19:14 /opt/aportaya/respaldos/pasanaku-antes-de-limpiar-20261016-1914.dump
NOTICE:  huerfanos: 0
BORRADO|usuario|9 · sesion|38 · intento_autenticacion|79 · token_verificacion|23 · dispositivo|23 · consentimiento|18
BORRADO|grupo|2 · cupo|7 · participante|2 · organizador|1 · cuenta_billetera|8 · comun.bitacora_evento|0
BORRADO|evento_dominio: identidad 44 · grupos 7 · nucleo_financiero 8 · tarifas 2 · grupos.alta_grupo 2
QUEDAN_USUARIOS|USR000000,USR000091
COMMIT
```
Corpus de MinIO intacto: quedan las carpetas de los usuarios borrados, sin cuenta asociada.

**Lo que había antes de que Pablo lo autorizara:**

**Limpieza de TEST — qué se hizo y por qué está bloqueada.** Alcance confirmado: borrar 9 cuentas finales; se conservan `USR000000` (sistema) y `USR000091` (operador); el corpus de MinIO queda intacto.
- Backup hecho y verificado restaurándolo (11 usuarios): `/opt/aportaya/respaldos/pasanaku-antes-de-limpiar-usuarios-20261016.dump`.
- Simulación del cierre por claves foráneas, dentro de una transacción revertida: aprox. 260 filas en identidad, grupos, organizador y billetera. No toca roles del operador. No hay asientos, transacciones ni pagos, así que el mayor no se descuadra.
- Ejecutar exige borrar filas de `comun.bitacora_evento` (append-only) y desactivar sus triggers. El clasificador de permisos de Claude Code lo bloqueó como **alteración de auditoría**. **No se ejecutó nada.**
- Lo destraba: que Pablo autorice explícitamente esa acción (regla de permisos para ese comando) o elija otra estrategia, por ejemplo reconstruir la base de TEST desde el esquema y las semillas.

## Evidencia
- `evidencia/01` … `evidencia/08`: generador, esquema, tests dirigidos y regresión, con su salida.
- `evidencia/09`: despliegue en Contabo y prueba de #88 contra el servicio real.

```text
POST /usuarios/verificaciones/correo  (correo de la cuenta sintética SINT-E2E-A, enmascarado)
HTTP 422  "codigo":"AP-CU01-03"  "campo":"correo"
POST /usuarios/verificaciones/correo  (sonda-<epoch>@example.invalid)
HTTP 202
```

## No cubierto
- **El correo real de acceso llegando a una bandeja** (H3.S1.M2).
- Los tests de `DesafioLocal` corren contra un doble (`CorreoEnMemoria`). Que Gmail entregue el correo se prueba solo con `MockRestServiceServer` y, de verdad, en TEST.
- En la rama de `dev` no se corrieron localmente los tests Flutter del arreglo del 2026-10-15. En el CI de #93 fallaron por formato de Dart en archivos ajenos (`553bd786`).
- Correos con distinta capitalización (`Ana@` y `ana@`) cuentan como distintos, igual que en `uq_usuario_correo` y en CU-01.
- Sobre la base de `origin/test` posterior a #90 no se pudieron correr los tests de identidad: no compilan. Ver Riesgos.

## Desvíos del plan
- **H2 descartado:** en vez de conectar el despacho de `notificaciones`, se reusó el enviador Gmail que identidad ya tiene encendido en TEST (pedido de Pablo: "reutilizá el servicio que ya está"). Además, pasar el código por `notificaciones` lo dejaba en texto plano en el outbox y en la bandeja (90.2.9).
- **"Perfil test" no creado:** TEST ya corre con `local`, que hace exactamente eso.
- **Correo confirmado:** se usa el token `VERIFICACION_CORREO` del alta, no `usuario.correo_verificado_en`, porque esa columna está vacía en todas las cuentas de TEST.
- **Trabajo agregado:** H4 (correo repetido) y H5 (publicación), por pedido de Pablo. Llevar a `dev` exigió portar también el arreglo del 2026-10-15 (`f18a3915`) y separar dos archivos de test por el límite de 300 líneas.
- **#88 y #91 se mergearon antes** de que llegaran los commits de CI. Sus seguimientos son #92 y #93.

## Riesgos residuales
- **El backoffice de TEST no se actualizó:** su build falla por `TS2339 'correo' en ExpedienteEnRevision` (viene de #90). Sigue la imagen anterior.
- **`origin/test` no compila los tests de identidad** desde `1a086b93` (#90): `VerificacionesControllerWebTest.java:61`. `ExpedienteDeIdentidad.java` tampoco pasa spotless.
- **`dev` tiene CI en rojo por cosas ajenas:** spotless de `RevisionRepositorio`/`CU02RevisionTest`, formato Dart (`553bd786`) y el barrido por `RevisionRepositorio`/`UsuariosControllerWebTest`.
- **Personas que siguen sin poder entrar desde un dispositivo nuevo:** participantes sin correo confirmado en el alta, y los que tienen factor `SMS` (el SMS sigue apagado por contrato).
- **TOTP fuera del perfil `local`:** `DesafioLocal` compara el código de la app autenticadora contra un número al azar. Si TEST deja `local`, los operadores no entran.
- `DesafioLocal` guarda los desafíos en memoria: con más de una réplica, el código no valida.
- `notificaciones` no despacha nada: no tiene consumidor, CU-80 y CU-83 no se invocan y el adaptador es simulado.

## Decisiones y ambigüedades
- **A1 (confirmada por Pablo):** el factor por correo es solo para participantes, no para operadores.
- **A2 (confirmada):** no hay enrolamiento por API. El correo confirmado en el alta actúa como factor implícito cuando no hay otro enrolado.
- **A3 (confirmada):** el código no se persiste en ningún lado.
- **Sin confirmar (a Pablo):** si un participante con factor `SMS` y correo confirmado debería recibir el código por correo. Hoy no lo recibe: el factor enrolado manda.
- **Criterios agregados a la bóveda:** CU-04 (5 escenarios) y CU-01 (1), derivados de lo pedido hoy. Hay que revisarlos como requisito.
