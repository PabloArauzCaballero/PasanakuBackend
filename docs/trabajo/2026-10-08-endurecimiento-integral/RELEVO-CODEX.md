# RELEVO A CODEX — Endurecimiento integral de Pasanaku (2026-10-08 a 2026-10-11)

> **AVANCE: 69 / 86 — 80,2 %** (microtareas HECHO / total de `PLAN.md`; `A MEDIAS` cuenta como no hecha).
> Escrito para quien NO vio la sesión. El dueño (Pablo) cerró la sesión el 2026-10-11 ~20:30 (hora de La Paz) y entregó todo a Codex.

Léelo en este orden: §1 estado → §2 ramas → §3 TEST → §4 reglas y RAM → §5 trabajo en vuelo → §6 pendiente priorizado → §7 decisiones → §8 abiertos con dueño → §9 deuda y riesgos → §10 comandos.

---

## 1. Estado en una pantalla

| Cosa | Estado (verificado el 2026-10-11) |
|---|---|
| Plan | `docs/trabajo/2026-10-08-endurecimiento-integral/PLAN.md`: 69 HECHO, 5 A MEDIAS, 10 BLOQUEADO, 2 TODO (86). `REPORTE.md` y `evidencia/carril-*` tienen el detalle |
| Backend | 15 servicios + `inversiones` (nuevo) + gateway. Última corrida completa: 3.396 pruebas / 81 suites / 0 fallas (conteo desde `build/test-results/*.xml`, script `relevo-herramientas/xml_conteo.py`). Re-verificados después por Gradle: aportes 174, nucleo 549, identidad 227+, entregas 212, grupos 362, erp, comun-web 67 |
| TEST (Contabo) | **Desplegado y sano** con el commit `aa272849` (= árbol de `justin/feature/cierre-endurecimiento-2026-10-08`@`b099e7e9`+). API 200, gateway rutea `inversiones` (401 sin token) |
| PR a `dev` | **#53** (https://github.com/PabloArauzCaballero/PasanakuBackend/pull/53), `MERGEABLE` sin conflictos, **CI de GitHub en rojo** (ya estaba rojo en `dev` antes: formato y `verificar_criterios`; mirar los checks antes de pedir merge). El #52 se cerró (tenía conflictos) |
| App móvil | APK contra TEST: `C:\Users\Usuario\Downloads\APK-Pasanaku\aportaya-test-5-TEST.apk`. Script de TestFlight para el Mac del dueño: `scripts/testflight_ios.sh` (NO probado, no hay Mac aquí) |
| Lo que NO se logró | El recorrido de dos usuarios (A organiza/invita, B acepta/rechaza) contra TEST **no se completó**: hay defectos de código (ver §5, carril P) |

---

## 2. Ramas, worktrees y commits

Repo: `https://github.com/PabloArauzCaballero/PasanakuBackend`. Trabajo local en `C:\Users\Usuario\Downloads\Entrypoint-GitHUb\Pasanaku\`:

| Carpeta (worktree) | Rama | Para qué |
|---|---|---|
| `PasanakuBackend-cierre` | `justin/feature/cierre-endurecimiento-2026-10-08` | **Rama de trabajo y fuente de lo desplegado en TEST.** Sale de `origin/test` (`59370bbd`) + todo el cierre |
| `PasanakuBackend-dev` | `justin/merge/dev-en-cierre` | La misma rama con `origin/dev` (31 commits, 27 conflictos) integrado. Es la cabeza del PR #53. Tiene además el fix del IDOR de `consultarEstadoDeVerificacion` y `pubspec.lock` (Flutter 3.44.8) |
| `PasanakuBackend` y `PasanakuFrontend` | (del usuario) | **Árboles originales del dueño: NO tocar** (tienen cambios sin commitear suyos) |

- `origin/test` = `aa272849` (commit de subida creado con `git commit-tree <árbol-de-la-rama> -p origin/test`, sin `--force`). **Subir a test = esto**, no un merge: el commit `0c2e1488` (revert) que dejó la subida fallida del 2026-10-10 habría deshecho el trabajo. Script: `relevo-herramientas/subir_a_test.sh`.
- `origin/dev` = `7b467d8e`.
- Para llevar lo nuevo de la rama de cierre al PR #53: en `PasanakuBackend-dev`, `git merge origin/justin/feature/cierre-endurecimiento-2026-10-08`, resolver (la última vez: `errores.dart`, `autenticar_test.dart`, `campo_de_fecha.dart`; se tomó la versión de `dev` y se agregaron las claves que faltaban), push de `justin/merge/dev-en-cierre`.
- Commits relevantes (de más nuevo a más viejo): `0924c56d` ADR-051/052 · `7a917f03` L3 evidencia · `fec8d391` script TestFlight · `172db3c4`/`adf743e1` sandbox licencia y contrato sintético · `a1228d32` H3 · `4281d509` cuentas sintéticas · `28ce6f70` H2 · `011abf73` cierre carril I · `dfce2739` nucleo/identidad · `5d3c7437` G · `0b2de6a6` tercera vuelta.
- Hay trabajo **sin verificar o a medias** de los carriles P, Q, R y T (§5): al cerrar la sesión se les pidió commitear todo con prefijo `WIP:` y escribir un `ESTADO.md` en su carpeta de evidencia. **Mirá `git log` de la rama y `evidencia/carril-{P,Q,R,T}/ESTADO.md` primero.**

---

## 3. TEST (servidor Contabo)

**Acceso:** `ssh -i ~/.ssh/id_ed25519_contabo -o IdentitiesOnly=yes root@161.97.85.216` (la clave está en el equipo del dueño; las otras claves de `~/.ssh` NO entran). El clasificador de permisos de Claude Code bloqueó varias veces el `ssh` desde agentes: el dueño dio permiso explícito y reiterado («tenés permiso pa todo»); con ese permiso el `ssh` funcionó. Si te bloquean, pedile al dueño que corra el comando con `!`.

**URLs:** API `https://api.aportaya.161.97.85.216.sslip.io` (`/actuator/health`, `/api/v1/...`) · backoffice `https://backoffice.aportaya.161.97.85.216.sslip.io` · web `https://aportaya.161.97.85.216.sslip.io` · app web móvil `https://app.aportaya.161.97.85.216.sslip.io`.

**Cómo se despliega:** Coolify app `aportaya-api` (rama `test`, compose `docker-compose.coolify.yml` generado con `python scripts/generar_compose.py --coolify`). Las imágenes `aportaya/*:test` NO las construye Coolify: las construye en el host `/opt/aportaya/bin/reconstruir-si-cambio.sh` (timer `aportaya-autodespliegue.timer`, cada 2 min) y recién si TODAS compilan dispara `disparar-despliegue.sh`. Log: `/opt/aportaya/logs/autodespliegue.log`. Documentación: `despliegue/TEST.md` y `despliegue/host/README.md`.

**Lecciones del despliegue (costaron horas, leelas):**
1. **El esquema se expande ANTES de construir.** La construcción de cada servicio genera jOOQ introspeccionando la base VIVA: sin las tablas nuevas, no compila. Orden real: (a) `docker run aportaya/esquema:test` contra la base real (aditivo, una transacción; `despliegue/aplicar-esquema.sh` agrega columnas/CHECK faltantes) → (b) timer reconstruye servicios → (c) Coolify despliega. Script que lo hizo: `relevo-herramientas/preparar_host_test.sh`.
2. `reconstruir-si-cambio.sh` hacía `git checkout FETCH_HEAD` que **fallaba en silencio** (clientes generados modificados en el clon del host) y construía código VIEJO. Se corrigió en el host con `checkout -f`; **el fix está en el host pero verificá que `despliegue/host/reconstruir-si-cambio.sh` del repo lo tenga** (`grep 'checkout -f'`).
3. Si una corrida falla, el script marca el sha como visto y no reintenta: forzar con `echo <sha-viejo> > /opt/aportaya/ultimo-construido; rm -f /opt/aportaya/ultimo-fallido`.
4. El timer solo reconstruye lo que cambió. Los frontends/gateway que no dispara hay que reconstruirlos a mano (`relevo-herramientas/faltantes_host.sh`).
5. `movil-web` falló por `pubspec.lock` resuelto con otro Flutter: el Dockerfile y `ci.yml` fijan **Flutter 3.44.8**. Arreglado en `b099e7e9`.
6. Kafka no autocrea tópicos: `bash /opt/aportaya/repo/despliegue/host/crear-topicos.sh` (244 tópicos `aportaya.*`; idempotente). Hay que repetirlo si se agregan eventos nuevos (p.ej. `aportaya.nucleo_financiero.aporte_aplicado`).
7. `nucleo-financiero` NO arranca con `PERFIL_SPRING=production`/`staging` (pide un bean `ProveedorDeRetiro` que solo existe en perfil `local`: `ProveedorDeRetiroLocal`). **TEST debe seguir en `PERFIL_SPRING=local`.**

**Secretos en Coolify (solo nombres; los valores los tiene el dueño):** `QR_INTERNO_CLAVE` (≥32 bytes, nucleo; sin default) e `INVITACIONES_CLAVE` (identidad). Opcionales/futuros: `INVERSIONES_SERVICIO_*` (credencial de servicio de inversiones → identidad; **vacías**), `JWT_CLAVE_FIRMA_ANTERIOR` (rotación). `APORTAYA_CORS_ORIGENES` solo lo lee el perfil production. En producción: `ADMISION_HUMANA_OBLIGATORIA=true` (el compose de TEST la deja en `false` a propósito), `CUENTAS_LOGICAS_APTO_PRODUCCION` (guarda `GuardiaDeCuentasLogicas`), `INVERSIONES_ALIADO_MODO=deshabilitado` (único modo que arranca en prod).

**Respaldo previo a la subida:** `/opt/aportaya/respaldos/pre-subida-pasanaku.dump` en el VPS (2,6 MB, `pg_restore --list` OK). La base se llama `pasanaku`, el contenedor `aportaya-postgres` (postgres:16; **ojo: hay otros Postgres de otros proyectos en el mismo VPS** — `gymsheet`, atlas, etc.; el primer filtro por nombre agarró uno ajeno). Usuario/clave en `/opt/aportaya/postgres/.env`.

**Marcha atrás:** `git revert` del commit de subida en `origin/test` (el esquema solo se expandió: no hay nada que deshacer en la base). Si hace falta, el dump.

**Datos sintéticos que SE SEMBRARON en TEST (todo `apto_produccion=false`, retirable):**
- 3 usuarios `SINT-E2E-0/-A/-B` (`+59170009100/101/102`, `@example.invalid`), segundo factor `000000` (perfil local). A = organizador (roles ORGANIZADOR+PARTICIPANTE), B = invitado. Script: `despliegue/ensayos/sembrar_test.sh` (`--estado`, `--retirar`). **Las contraseñas están SOLO en `C:\Users\Usuario\AppData\Local\Temp\aportaya-s\cuentas-test.txt`** (fuera del repo; no las pegues en chats/logs).
- **Sandbox de licencia** del alcance `GRUPO_PASANAKU` (`entorno_prueba_regulado` ACTIVO; la licencia ASFI real sigue `EN_TRAMITE`, sin número): `despliegue/ensayos/sandbox_licencia_test.sh` (`--estado`, `--retirar`). Autorizado por el dueño («sí, sandbox de licencia»).
- **Contrato de organizador sintético firmado** para A (R-ORG-02): dentro de `sembrar_test.sh`. Autorizado por el dueño («sí, contrato sintético»).
- Residuos de la prueba L3 (API): 2 grupos de A («L3 API uno/dos»), una postulación pendiente de B en el primero, 1 token de invitación consumido, 1 huérfano, 1 invitación SMS vigente a B en el segundo.
- Para retirar todo: `--retirar` de ambos scripts. Producción NO debe recibir nada de esto (los scripts se niegan si identidad no corre en perfil `local`).

**Estado funcional real en TEST (probado por API real, evidencia en `evidencia/carril-L3/`):**
| Flujo | Resultado |
|---|---|
| Login A/B con 2.º factor | OK |
| Crear grupo (A) | OK (202), tras sandbox + contrato |
| Invitar por canal `SMS` | OK (201) |
| Invitar por `ENLACE` (default de la app) | **500 AP-INT-01** (defecto, ver §5 P) |
| Revocar / reemitir | OK, idempotente, tercero 403 |
| B consulta/acepta el enlace | **422 AP-CU69-05**: el grupo nace `BORRADOR` y nadie lo abre a inscripciones (defecto, §5 P) |
| Aporte → billetera/mayor | **No mueve plata**: nadie abre billeteras USUARIO/GRUPO (carril Q) |

---

## 4. Reglas de la casa, RAM y entorno (OBLIGATORIO)

- **Reglas:** `.claude/rules/` (00 no negociables, 20 plan, 30 escalera de evidencia, 40 reporte, 65 aislar y simular, 70 recursos, 90 seguridad, 91 dinero, 95/96/97 capas, 98 microservicios). Candados en `.claude/hooks/` (`plan_gate`, `report_gate`, `blocker_gate`; `python .claude/hooks/plan_status.py` calcula el %).
- **Nunca** `float` para dinero; mayor append-only; `clave_idempotencia` con UNIQUE; esquema SOLO desde `docs/entidades/*.puml` + `docs/Restricciones.md` + `scripts/modelo.py` → `PYTHONUTF8=1 python scripts/generar_ddl.py` → `sql/` (nunca editar `sql/` a mano; borrar `servicios/*/build/generated/jooq` tras cambiar el esquema). Semillas: `seeders/**/*.json` + `scripts/generar_semillas.py`. Pruebas: no borrar/skip/debilitar; nombres ↔ escenarios del CU (`scripts/verificar_criterios.py`; prefijos permitidos: rechaza / reintento / concurrencia / cuadre / compensa / evento duplicado / fuera de orden / rls — **«limite:» NO**). Archivos de código ≤300 líneas (`BarridoTest`), movil ≤200 (`verificar_frontend.py movil`). SQL en pruebas siempre parametrizado.
- **Gates:** `scripts/verificar_criterios.py`, `verificar_seguridad.py`, `verificar_pruebas_web.py`, `verificar_boveda.py`, `verificar_maqueta.py`, `verificar_contratos_limites.py`, `verificar_rutas_frontend.py`, `verificar_carriles.py` (este último falla desde siempre por «sin carril dueño: inversiones»: declarar el dueño en `DUENO_DE_SERVICIO`).
- **RAM — la máquina del dueño se cayó una vez por esto.** Todo Gradle/Docker/Testcontainers/Flutter/Playwright va **de a uno** por el guardián: `bash C:/Users/Usuario/.claude/ram-guard/heavy.sh <comando>` (copias en `relevo-herramientas/ram-guard-*.sh`; espera ≥3500 MB libres y un candado `C:/Users/Usuario/.claude/heavy.lock`; >30 min = huérfano; `MAX_WAIT_S`, `MIN_FREE_MB`). Con el stack Docker arriba solo quedan ~2–3 GB: bajalo (`bash despliegue/ensayos/bajar_entorno.sh`) antes de correr suites. Docker Desktop de Windows tiene 4 GB (`.wslconfig`): los 16 JVM juntos no entran, arrancar en tandas (`despliegue/ensayos/arranque_completo.sh`, modos completo/tandas/conjunto/individual).
- **Gradle:** `./gradlew.bat --max-workers=2 -Dorg.gradle.parallel=false`; `generateJooq` necesita Postgres vivo (`sql/aplicar.sql`); `./gradlew generarClientes` regenera clientes (TS/Dart) y hay que compilar web/backoffice. Tareas de prueba: `test`, `webTest`, `integrationTest`, `contractTest`, `sagaTest`, `testBarrido`.
- **JAVA_HOME del usuario está roto** (apunta a un Android Studio inexistente): usar Eclipse Adoptium JDK 21 (`C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`).
- **Flutter:** versión **3.44.8** (la del CI y del Dockerfile). Hay SDKs en `C:\Users\Usuario\flutter-sdk` (3.47.5 local — NO usar para resolver `pubspec.lock`) y `C:\fl3448` (3.44.8, 1,2 GB; el dueño puede borrarlo). `build_runner` AOT lo bloquea el Control de aplicaciones de Windows: usar `--force-jit`. Generados gitignored: `*.g.dart`, `packages/diseno_flutter/lib/tokens/tokens.dart`.
- **Scripts de apoyo** (copias en `relevo-herramientas/`): `subir_a_test.sh`, `preparar_host_test.sh`, `faltantes_host.sh`, `vigilar_*.sh`, `xml_conteo.py` (cuenta pruebas desde XML), `TERCERA_VUELTA.md`/`CUARTA_VUELTA.md`/`GATES_COMUN.md` (briefs que se daban a cada agente: reglas + honestidad).
- Honestidad: nunca «listo» sin salida pegada; peldaño por área; «No cubierto» siempre. Datos personales/secretos: enmascarar. **No uses los correos personales de `seeders/dev/15-usuarios-dev.json`.**

---

## 5. Trabajo en vuelo al cerrar la sesión (carriles P, Q, R, T)

Se lanzaron en paralelo el 2026-10-11 y se les pidió parar, commitear con `WIP:` y dejar `ESTADO.md`. **Verificá cada uno antes de confiar.**

**P — grupos / identidad / tarifas (defectos hallados por L3 contra TEST)**
1. Grupo nace `BORRADOR` (`grupos/CreacionRepositorio:73`) y `CU69Enlace:157` exige `CONFORMADO`/`ABIERTO_A_INSCRIPCION` → todo enlace da 422. Falta quien abra el grupo. Decisión adoptada (ADR-052): **se abre automáticamente al confirmarse el precio congelado** (evento de tarifas). P empezó a tocar `servicios/tarifas`.
2. Canal `ENLACE` da 500: identidad lo rechaza (AP-CU69-05) y grupos lo traduce a 500 (`HechosPorHttp:185`); la política `INVITACION_GRUPO.canales_permitidos` en TEST conserva la fila vieja porque el seed usa `ON CONFLICT DO NOTHING` (confirmar con un SELECT en `identidad.politica_token`). Corrección = **nueva versión de la política** + script idempotente de siembra para el VPS (NO se ejecutó).
3. `canje` (`RutasDeInvitacion:103`) consume el token en identidad ANTES de validar localmente: si falla, el token queda quemado. Hacerlo atómico/compensable (regla 98).
4. `enlaceDeInvitacionValido` traga excepciones sin log.
5. CU-18: límite de consultas por actor de servicio (429 + Retry-After) para `SERVICIO_ENTREGAS`.

**Q — nucleo-financiero / aportes**
1. Billeteras USUARIO/GRUPO: consumidores de `usuario_registrado` y `grupo_creado` (ADR-052 DD-8; `grupo_activado` nunca se emite). Sin billetera de grupo el consumidor de `aporte_cobrado` rechaza (AP-CU12-02) y `GET /billetera/cuentas/propia` da 422 AP-CU13-01.
2. `nucleo/HechosPorHttp` consulta a aportes por `usuarioId` pero aportes indexa por `participante_id` («tiene obligaciones abiertas» siempre falso).
3. Aporte PAGADO sin saldo no se compensa (`aporte_no_aplicado` / AP-CU21-03).
4. Precio de venta = **precio − cargos**, cada deducción con su línea (ADR-052 DD-1; `pagarAlVendedor`/CU-130).
5. Renombrar 2 pruebas con prefijo `limite:` (`CU13MercadoTest.java:261`, una de CU-21 en aportes) para que `verificar_criterios.py` dé RC 0.

**R — erp / publicidad:** `clave_idempotencia` UNIQUE en `pago_a_proveedor` (aditivo; `aplicar-esquema.sh` la agrega), `aprobadaPor` de la factura desde el token (fuera del contrato), quitar `QR` de las formas de pago del contrato (la restricción solo admite CHEQUE/EFECTIVO/TRANSFERENCIA). Las 11 rutas de frontend erp/publicidad sin contrato requieren construir listados (decisión de columnas/filtros/permisos: ABIERTO).

**T — app móvil:** D4 (billetera consulta cuenta de ID fijo `_cuentaPorOmision` en `pantallas/billetera/rutas.dart`: usar `GET /billetera/cuentas/propia` y mostrar «tu billetera todavía no está activa» en AP-CU13-01), pantalla de 2.º factor sin explicación, tutorial que muestra `billetera.saldo`, texto «custodia en Banco Unión» sin fuente, `estado_alta.dart` >200 líneas, selector de fecha en inglés (`flutter_localizations`; **toca `pubspec.lock`: resolver con Flutter 3.44.8 y verificar `flutter pub get --enforce-lockfile` RC 0**), botón Compartir (plugin nuevo = decisión), APK `aportaya-test-6-TEST.apk`.

---

## 6. Pendiente priorizado (qué haría yo, en este orden)

1. **Congelar el estado:** `git fetch`; revisar commits `WIP:` y los `ESTADO.md` de P/Q/R/T; correr `git status` del worktree cierre (hay cambios de varios carriles mezclados: el carril W ya arrastró cambios de otros en CU-20/CU-69 al commitear).
2. **Cerrar P** (sin P nadie acepta una invitación) y verificar por Gradle: grupos, identidad, tarifas (`spotlessApply`, `test`, `webTest`, `integrationTest`, `testBarrido`) + los 4 `verificar_*.py` + conteos desde XML.
3. **Cerrar Q** (sin Q el aporte no mueve plata) y R, T.
4. **Un solo redespliegue a TEST** (sin pisar lo que corre): `subir_a_test.sh` (commit-tree sobre `origin/test`) → el timer reconstruye lo que cambió (servicios tocados + esquema + movil-web) → verificar 17 contenedores `healthy` y que `GET /api/v1/inversiones/productos` dé 401. Si cambia el esquema (R) expandir antes: `docker run aportaya/esquema:test` contra la base real (§3.1). Si hay eventos nuevos: `crear-topicos.sh`. Aplicar en el VPS los scripts de siembra que P deje (política de canales) — **el dueño autorizó operar su TEST; los scripts de siembra se corrieron con `ssh ... 'bash -s' < script`.**
5. **Repetir el recorrido A/B** (primero por API con `curl`, luego dos emuladores `aportaya_a`/`aportaya_b`, recetas y capturas en `evidencia/carril-H`, `-H2`, `-H3`, `-L3`; los AVD ya existen en `~/.android/avd`; los dos emuladores no entran a la vez con poca RAM). Si pasa con persistencia real en TEST: cerrar H3.S1.M4, H14.S1.M1, H12.S1.M2 (+2 a +3 microtareas). Compilar APK nuevo (define `API=https://api.aportaya.161.97.85.216.sslip.io/api/v1` y **`HOSTS_PERMITIDOS=api.aportaya.161.97.85.216.sslip.io`**, sin el cual la app rechaza todo host que no sea loopback).
6. **Actualizar `PLAN.md` y `REPORTE.md`** con lo verificado (esta tarea la dejé a propósito en manos de quien consolide) y commitear; luego traer la rama a `PasanakuBackend-dev`, empujar y revisar el CI del PR #53.
7. Deuda menor (§9).

---

## 7. Decisiones de negocio adoptadas POR DELEGACIÓN del dueño (2026-10-11)

El dueño dijo «lo que no se pueda cerrar con código, hacé lo más recomendado pero cerralo». Quedó escrito en `docs/Arquitectura/ADR-051` (aceptado: un solo camino de entrega con comisión como cargo separado y sin comisión del organizador; un solo camino de cobertura con reserva empresarial; sin grupos sin organizador humano) y `ADR-052` (ocho decisiones: precio − cargos; admisión humana obligatoria en producción `true` y `false` en TEST; sustitución automática de administrador `false` = manual; CU-18 sí/no con límite de consultas; apertura automática del grupo al confirmar precio; sin `QR` en pagos a proveedor; autorizante/aprobador desde el token en erp; billeteras al evento). Constancia en ambos: «aceptado por delegación expresa de Pablo; **pendiente de ratificación formal por Producto/Finanzas/Legal**». No hay firmas inventadas. Mantené esa honestidad.

---

## 8. Abierto: lo que un cambio de código NO cierra (dueño)

| Qué | Dueño |
|---|---|
| H15.M1–M5 (mapa jurídico, proveedores, capital/cobertura, fiscal, certificación de adaptadores reales) y H16.M1–M4 (piloto) — **evidencia externa; NO se inventa** (dictámenes, contratos, fondos, autorización ASFI) | Dirección, Legal, Finanzas, Cumplimiento, Tributario |
| Licencia ASFI real de `GRUPO_PASANAKU` (hoy solo sandbox de TEST) | Cumplimiento/Legal |
| Capital y reserva (D-03), calificación jurídica del respaldo (D-02), tope por operación billetera/QR (D-16), tarifa/impuestos/factura de comisión y cargos (D-04) | Finanzas, Legal, Producto, Tributario |
| Aliado, tasas, custodia y fiscal de inversiones (DR-INV-01…07); hoy `INVERSIONES_ALIADO_MODO=deshabilitado` | Dirección, Legal, Fiscal, Tesorería |
| Cifras del límite de consultas de CU-18 y qué roles verifican titularidad; actor con `GRUPO_ADMINISTRAR`+`RECLAMO_VER` para que garantía consulte reclamos | Seguridad |
| Feriados bancarios vs Gaceta Oficial (el calendario es sintético) | Tesorería/Legal |
| H4.S1.M6: proveedor de QR interoperable no existe; H12.S1.M4: lector de pantalla real y otros navegadores | Producto / persona con la herramienta |
| ADR-051/052: ratificación formal | Producto, Finanzas, Legal |
| Rotación real de claves en Coolify (JWKS con clave anterior) y restauración probada de backups | Quien opere el servidor |
| Pre-existente (no es nuestro): bucle `SECURITY DEFINER` de `sql/00_base/03_permisos.sql` (~l.241), documentado en `evidencia/carril-S2/DEUDA-security-definer.md` | Seguridad/DBA |

---

## 9. Deuda técnica y riesgos conocidos

- **PR #53 con CI rojo** (formato, reglas propias, contratos/clientes, esquema/semillas, OSV…); ya estaba rojo en `dev`. No verificado en esa rama tras el último merge: `flutter analyze` / `flutter test` de `apps/movil` y `packages/diseno_flutter`, y las suites Gradle de los otros 14 servicios (solo compilan).
- 8–9 *goldens* de Flutter fallan por rasterizado de otra plataforma (clasificado ENVIRONMENT, el CI corre en macOS; NO regenerar en Windows).
- `ClienteConCircuito` copiado en 4 servicios (consolidar en plataforma). 19 rutas del frontend sin contrato (`verificar_rutas_frontend.py`). `bundle` inicial del backoffice +9,66 kB sobre presupuesto (WARNING).
- `CU04Autenticar` cuenta como fallido el primer paso de cada ingreso correcto: tras ~5 ingresos en 15 min, un error de contraseña bloquea la cuenta 30 min (las sondas de los scripts de siembra dejan 1 intento por corrida).
- Defecto de código: `AP-CU69-05` se usa para causas distintas (revocada, vencida, usada, reglamento cambiado) y el cliente no las distingue; `CU20CrearGrupo` convierte un rechazo de negocio (R-ORG-02) en 500 `AP-INT-01` (debe ser 422 con código propio).
- `identidad`: token de alta vale 30 min sin rate limit propio; `solicitarVerificacionCorreo/confirmar…` sin revisar el límite de intentos (fuerza bruta del OTP de 6 dígitos).
- Con admisión humana obligatoria (valor de producción) la app (que usa `enlace/aceptar`, no el canje) no podría entrar por invitación (`AP-CU69-10`).
- ERP: pago parcial repetido se duplica hasta que R cierre la idempotencia.
- Seguridad automática del plugin reportó el IDOR del estado de verificación: **cerrado** en `justin/merge/dev-en-cierre` (commit `ebe7982a`); verificar que esté también en la rama de cierre/TEST si no se integra `dev` primero.

---

## 10. Comandos útiles

```bash
# RAM libre / procesos
powershell -c "[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1024)"
# Conteo de pruebas desde XML
python docs/trabajo/2026-10-08-endurecimiento-integral/relevo-herramientas/xml_conteo.py
# Avance del plan
python .claude/hooks/plan_status.py
# Suite de un servicio (Postgres desechable + Gradle + parar daemon), por el guardián
MAX_WAIT_S=3000 bash C:/Users/Usuario/.claude/ram-guard/heavy.sh bash <script> ...
# Estado de TEST (solo lectura)
curl -s -o /dev/null -w '%{http_code}\n' https://api.aportaya.161.97.85.216.sslip.io/actuator/health
ssh -i ~/.ssh/id_ed25519_contabo -o IdentitiesOnly=yes root@161.97.85.216 'tail -n 8 /opt/aportaya/logs/autodespliegue.log; docker ps --format "{{.Names}}|{{.Status}}" | grep -E "^(gateway|identidad|nucleo|inversiones|grupos|aportes|entregas)-"'
# Cuentas y sandbox de TEST
ssh ... 'bash -s -- --estado' < despliegue/ensayos/sembrar_test.sh
ssh ... 'bash -s -- --estado' < despliegue/ensayos/sandbox_licencia_test.sh
# Subir a test (commit nuevo encima de origin/test, sin --force)
bash docs/trabajo/2026-10-08-endurecimiento-integral/relevo-herramientas/subir_a_test.sh
# TestFlight (EN EL MAC del dueño; no probado)
bash scripts/testflight_ios.sh            # --solo-compilar para no subir
```

TestFlight: bundle `bo.aportaya.aportayaMovil`, equipo `7A42TVN3Y6`; la receta es la del 2026-09-28 (`docs/trabajo/2026-09-28-testflight-ios/REPORTE.md`). Requiere Flutter 3.44.8, Xcode con la cuenta del equipo, Node 22 + corepack, CocoaPods.

**Cosas que el dueño debe hacer o decidir (no las puede hacer un agente):** correr `scripts/testflight_ios.sh` en su Mac; borrar `C:\fl3448` si no lo quiere; ratificar ADR-051/052 con Producto/Finanzas/Legal; conseguir H15/H16; cargar secretos reales en producción (nunca en el repo).
