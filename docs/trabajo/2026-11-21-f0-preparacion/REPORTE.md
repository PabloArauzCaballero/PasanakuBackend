# Reporte — F0 Preparación del plan de correcciones (hito H0)

> **AVANCE: 11 / 11 — 100,0 %** de las microtareas del carril (`plan_status.py`). **El hito H0 del plan maestro NO
> está cerrado:** ningún PR está mergeado, F0.M2 (Docker) no se ejecutó, y la línea base no incluye `integrationTest`
> ni Flutter. Línea base en `dev`: Gradle `test` 15/15 servicios en verde (559 pruebas, 0 fallidas); 1 verificador en
> rojo (`verificar_rutas_frontend.py`); los 2 paquetes Flutter, NO COMPROBADO.

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 850e0f51
- Ramas y PR (todos hacia `dev`, ninguno mergeado):
  | PR | Rama | Microtarea |
  |---|---|---|
  | [#400](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/400) | `correcciones/f0-preparacion-2026-11-21` | H0.S1.M1 — plan y DECISIONES en `dev` |
  | [#401](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/401) | `…-m1` | H0.S1.M2 — F0.M1 `blocker_gate` |
  | [#402](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/402) | `…-adr055` | H0.S2.M1 — F0.M3 ADR-055 |
  | [#403](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/403) | `…-adr056` (apilada sobre `-adr055`) | H0.S2.M2 — F0.M4 ADR-056 |
  | [#404](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/404) | `…-adr058` (apilada sobre `-adr056`) | H0.S2.M3 — PX0.M01 ADR-058 |
  | [#405](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/405) | `…-flujos` | H0.S3.M1 — F0.M5 catálogo de flujos |
  | [#413](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/413) | `…-linea-base` | H0.S4 — F0.M6 línea base + este plan y reporte |
- Peldaño de evidencia: **documentación `VERIFIED`** contra sus verificadores (`verificar_boveda.py`,
  `validar_flujos.py`, `blocker_gate.py`, con salida pegada). **Línea base `RUNS`/`TESTED`** según la tabla.
  Nada de esto es `REGRESSION_VERIFIED`: Pablo revisa y mergea.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H0.S1.M1 | El plan de correcciones y DECISIONES quedan en `dev` (PR #400), copiados sin cambios | `diff -r /tmp/plan/docs/trabajo/<carpeta> docs/trabajo/<carpeta>` | PASS: sin diferencias (en el PR) |
| H0.S1.M2 | F0.M1: las 9 microtareas viejas pasan a `DESCARTADO` (P-20); el candado ya no bloquea | `python3 .claude/hooks/blocker_gate.py --self-test` + corrida con evento | PASS: 11/11, exit 0, 0 bloqueantes (antes 9, exit 2) · `evidencia/F0.M1-blocker-gate.txt` |
| H0.S2.M1 | F0.M3: ADR-055; ADR-049 y ADR-054 «reemplazado parcialmente», ADR-052 DD-2 reemplazada, R-SEG-04/R-ORG-06 en `Cumplimiento.md` | `python3 scripts/verificar_boveda.py` + `grep "Reemplazado parcialmente por ADR-055"` | PASS: `TODO OK`, 53 ADR, 2 líneas · `evidencia/F0.M3-adr055.txt` |
| H0.S2.M2 | F0.M4: ADR-056 (modelo A + D); ADR-051 `superada por ADR-056` | `verificar_boveda.py` + `grep "^estado" ADR-051` | PASS: `TODO OK`, 54 ADR · `evidencia/F0.M4-adr056.txt` |
| H0.S2.M3 | PX0.M01: ADR-058 con RM-01..RM-20; ADR-035 enmendada (sin WhatsApp) | `verificar_boveda.py` + `grep -c "^| RM-"` | PASS: `TODO OK`, 55 ADR, 20 filas · `evidencia/PX0.M01-adr058.txt` |
| H0.S3.M1 | F0.M5: catálogo vivo sin respaldo ni fondo viejo; flujo objetivo en `objetivo-modelo-A.md` | `python3 scripts/validar_flujos.py` + `grep` de pasos de respaldo | PASS: «Sin fallas.», grep vacío · `evidencia/F0.M5-flujos.txt` |
| H0.S4.M1 | F0.M6: 16 verificadores de solo lectura + 4 self-tests de candados corridos una vez y guardados | ver tabla de la línea base | PASS como ejecución (19 rc=0, 1 rc=1 registrado) · `evidencia/linea-base/verificadores/` |
| H0.S4.M2 | F0.M6: `./gradlew :servicios:<s>:test` en los 15 servicios, de a uno, contra PostgreSQL local | ver tabla de Gradle | PASS: 15/15 rc=0, 559 pruebas, 0 fallidas · `evidencia/linea-base/gradle/` |
| H0.S4.M3 | F0.M6: `yarn lint` y `yarn typecheck` con dependencias instaladas | ver §`yarn` | PASS como ejecución: los fallos son de entorno (Flutter) y quedan NO COMPROBADO · `evidencia/linea-base/yarn/` |
| H0.S4.M4 | F0.M2: procedimiento Docker para la máquina de Pablo escrito (abajo), sin ejecutar por instrucción | lectura de §«F0.M2» | PASS (documento) |
| H0.S5.M1 | Las 18 `F0.Lx.Mnn` de los anexos clasificadas con destino | `grep -c "F0.L" REPORTE.md` | PASS (ver §«Anexos») |

## A medias

### H0 (hito del plan maestro) — Preparación
- **Qué anda:** las 11 microtareas del carril tienen su DoD ejecutado y su PR abierto (#400 a #405 y #413).
- **Qué no anda:** nada está en `dev` todavía; mientras #401 no se mergee, cualquier rama que salga de `dev` sigue
  trabada por `blocker_gate` (pasó en esta sesión). F0.M2 no se ejecutó.
- **Qué falta exactamente:** (1) que Pablo revise y mergee #402 → #403 → #404 (en ese orden), #405 y #413 (#400 y #401 ya
  están en `dev`); (2) correr F0.M2 en la máquina de Pablo con el procedimiento de abajo; (3) sumar a la línea base
  `webTest` + `integrationTest` por servicio y `flutter analyze`, en esa misma máquina.
- **Dónde quedó:** ramas `correcciones/f0-preparacion-2026-11-21*`; solo documentación y evidencia, nada que
  compilar.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| F0.M2 (ejecución) | TODO — por instrucción no se ejecuta en la nube (Docker sin demonio) | La máquina de Pablo por Remote Control (P-19), con el procedimiento de abajo |
| Pruebas `integrationTest` (Testcontainers) de los 15 servicios | NO COMPROBADO — requiere Docker | Ídem; comando exacto abajo |
| `flutter analyze` (pedido por F0.M6 del plan maestro) | NO COMPROBADO — no hay Flutter en el contenedor (`flutter` no está en el PATH) | Máquina de Pablo |
| Microtareas F0 de código de los anexos | TODO — próxima tanda | Ver §«Anexos» |

## Línea base (F0.M6)

Contra `origin/dev` 850e0f51, el 2026-11-21. **No se arregló nada**: solo se registra.

### Verificadores (sin Docker, sin red)

| Verificador | Resultado | Nota |
|---|---|---|
| `verificar_boveda.py` | PASS | `TODO OK`, 52 ADR |
| `verificar_carriles.py` | PASS | |
| `verificar_contratos_limites.py` (+ `--self-test`) | PASS / PASS | |
| `verificar_criterios.py` | PASS | 3 CU pendientes de implementar (CU-05, CU-51, CU-57), informados, no fallan |
| `verificar_frontend.py todo` | PASS | |
| `verificar_maqueta.py` | PASS | |
| `verificar_narrativas.py` | PASS | |
| `verificar_pruebas_web.py` | PASS | |
| `verificar_rutas_frontend.py` | **FAIL** (rc=1) | «la lista de conocidas se pudrió»: `GET /grupos/solicitudes-escaladas` ya tiene contrato y sigue en `CONOCIDAS_SIN_CONTRATO`. Fuera de alcance; anotado |
| `verificar_seguridad.py` | PASS | `TODO OK · 7 aviso(s)` |
| `validar_flujos.py` | PASS | 1 aviso `sin-cablear` (H8-01) |
| `inventario_endpoints.py --check` | PASS | |
| `listar_topicos.py` | PASS | |
| `cobertura.py` | NO COMPROBADO | «Sin informes»: necesita `test webTest integrationTest` + `jacocoTestReport` |
| `pytest scripts/test_catalogo_de_datos.py` | PASS | 13 passed |
| Self-tests de `plan_gate`, `report_gate`, `blocker_gate`, `plan_status` | PASS ×4 | 11, 14, 11 y 31 PASS |
| `matriz_de_pruebas.py` (generador, no se cuenta como verificador) | — | `docs/catalogo-de-flujos/matriz.md` en `dev` está desfasada: dice 323 endpoints, el generador da 350 (se regenera en #405) |

Leí cada script antes de correrlo: ninguno escribe archivos en el modo usado (`inventario_endpoints.py` solo con
`--check`, que no escribe).

### Gradle `test` por servicio (PostgreSQL 16 local, puerto 5433, `sql/aplicar.sql` rc=0)

| Servicio | rc | BUILD | Clases | Pruebas | Fallidas | Errores | Omitidas 
|---|---|---|---|---|---|---|---|
| aportes | 0 | BUILD SUCCESSFUL in 2m 13s | 11 | 53 | 0 | 0 | 0 |
| auditoria | 0 | BUILD SUCCESSFUL in 20s | 13 | 58 | 0 | 0 | 0 |
| cumplimiento | 0 | BUILD SUCCESSFUL in 17s | 2 | 14 | 0 | 0 | 0 |
| entregas | 0 | BUILD SUCCESSFUL in 26s | 9 | 40 | 0 | 0 | 0 |
| erp | 0 | BUILD SUCCESSFUL in 13s | 1 | 5 | 0 | 0 | 0 |
| garantia | 0 | BUILD SUCCESSFUL in 25s | 8 | 43 | 0 | 0 | 0 |
| grupos | 0 | BUILD SUCCESSFUL in 30s | 18 | 114 | 0 | 0 | 0 |
| identidad | 0 | BUILD SUCCESSFUL in 36s | 9 | 46 | 0 | 0 | 0 |
| inversiones | 0 | BUILD SUCCESSFUL in 15s | 4 | 36 | 0 | 0 | 0 |
| notificaciones | 0 | BUILD SUCCESSFUL in 11s | 1 | 5 | 0 | 0 | 0 |
| nucleo-financiero | 0 | BUILD SUCCESSFUL in 26s | 19 | 94 | 0 | 0 | 0 |
| organizador | 0 | BUILD SUCCESSFUL in 15s | 2 | 15 | 0 | 0 | 0 |
| publicidad | 0 | BUILD SUCCESSFUL in 10s | 1 | 5 | 0 | 0 | 0 |
| tarifas | 0 | BUILD SUCCESSFUL in 14s | 2 | 7 | 0 | 0 | 0 |
| transparencia | 0 | BUILD SUCCESSFUL in 11s | 3 | 24 | 0 | 0 | 0 |
| **Total** | | | 103 | 559 | 0 | 0 | 0 |

`identidad` necesitó un segundo intento por un 429 de Maven Central (salida del primero en
`identidad-intento1-429.txt`). La primera tanda completa se cortó al primer servicio por el mismo 429 (en
`_primer-intento/`), así que se repitió con `--max-workers=1 -Dorg.gradle.parallel=false` y hasta tres intentos
con espera. Los conteos salen de los XML de JUnit de `servicios/<s>/build/test-results/test/`.

`webTest` (MockMvc, sin contenedor) e `integrationTest` (Testcontainers) **no** se corrieron en esta línea base: el
primero no lo pedía la instrucción y alargaba la sesión; el segundo necesita Docker. Comando exacto para la
máquina de Pablo, de a un servicio (regla 70.1.4):

```bash
for s in aportes auditoria cumplimiento entregas erp garantia grupos identidad inversiones notificaciones \
         nucleo-financiero organizador publicidad tarifas transparencia; do
  ./gradlew :servicios:$s:webTest :servicios:$s:integrationTest --console=plain \
    > docs/trabajo/2026-11-21-f0-preparacion/evidencia/linea-base/gradle/$s-integracion.txt 2>&1
done
```

### `yarn lint` y `yarn typecheck`

No había `node_modules`. `corepack` no pudo bajar Yarn 4.18.0: primero salía sin proxy (`ENOTFOUND`) y, con
`NODE_USE_ENV_PROXY=1`, el proxy del entorno respondió **403** para `repo.yarnpkg.com`. Se usó el mismo Yarn 4.18.0
publicado en npm (`@yarnpkg/cli-dist`, en una carpeta temporal fuera del repo). `yarn install --immutable`: rc=0,
lockfile intacto.

| Tarea | Resultado | Detalle |
|---|---|---|
| `yarn lint --continue` | 8/10 PASS | Fallan `@aportaya/movil` y `@aportaya/diseno-flutter`: exit 127, no hay Flutter/Dart en el contenedor → **NO COMPROBADO** (`ENVIRONMENT`) |
| `yarn typecheck --continue` sin clientes generados | 9/14 | `web` y `backoffice` con 104 `error TS`, casi todos `Cannot find module 'clientes/angular/…'`: los clientes están en `.gitignore` y la CI los genera antes (`ci.yml:259-288`). No es un defecto del producto, es un paso previo |
| `./gradlew generarClientes` (paso previo de la CI) | PASS | Regenerarlo modifica 3 fragmentos Dart **versionados** (`clientes/dart/{aportes,notificaciones,tarifas}/lib/…`): confirma F0.L6.M02. Restaurados con `git checkout`, no se commitean |
| `yarn typecheck --continue` con clientes generados | 11/14 PASS | `web` y `backoffice` en verde; fallan solo los 2 paquetes Flutter (exit 127) → **NO COMPROBADO** |

**Efecto secundario encontrado:** el `build` de `@aportaya/simulado` (dependencia de `typecheck`) **escribe en el
repo** un archivo nuevo, `packages/simulado/ejemplos/aportes/consultarPrimeraCuota.json` («1 creados · 349
conservados»). Lo borré después de cada corrida. Es un desfase entre el contrato de `aportes` y los ejemplos del
simulado en `dev`; queda anotado, no se arregló.

## F0.M2 — Procedimiento Docker para la máquina de Pablo (no ejecutado)

Fuente: `README.md` §«Levantar el backend», `despliegue/compose/base.yml` y `scripts/generar_compose.py`. Todo con
datos sintéticos; ningún dato real (regla 97.6). Desde la raíz del repo, en `dev` actualizada:

```bash
# 0 · puertos libres (regla 70.1.6): 5432, 6432, 9092, 6379, 9000 y el de NGINX
docker ps --format '{{.Names}}\t{{.Ports}}'

# 1 · infraestructura: postgres (pg_isready), pgbouncer, minio (mc ready), redis (ping), kafka
#     (kafka-broker-api-versions), gateway y nginx. --wait espera los healthchecks del compose.
docker compose -f despliegue/compose/base.yml --profile base up -d --wait
docker compose -f despliegue/compose/base.yml ps          # todos «healthy»

# 2 · compose de los servicios, generado (no se edita a mano; está en .gitignore)
python3 scripts/generar_compose.py
python3 scripts/generar_gateway.py

# 3 · imágenes, DE A UNA (README: catorce Gradle en paralelo se pisan el lock del caché)
docker buildx create --name aportaya --driver docker-container \
  --driver-opt network=aportaya-interna --use              # una sola vez
IP=$(docker inspect aportaya-postgres --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}')
for s in aportes auditoria cumplimiento entregas erp garantia grupos identidad inversiones notificaciones \
         nucleo-financiero organizador publicidad tarifas transparencia; do
  docker buildx build --load --allow network.host --network=host -f despliegue/Dockerfile \
    --build-arg SERVICIO="$s" --build-arg BD_URL_ADMIN="jdbc:postgresql://$IP:5432/pasanaku" \
    -t "aportaya/${s}:local" .
done

# 4 · stack completo, esperando los healthchecks (/actuator/health/readiness de cada servicio)
docker compose -f despliegue/compose/base.yml -f despliegue/compose/servicios.yml --profile todo up -d --no-build --wait
docker compose -f despliegue/compose/base.yml -f despliegue/compose/servicios.yml --profile todo ps

# 5 · DoD de F0.M2: health de cada servicio. Los servicios NO publican puerto (descriptor.yml:
#     puerto_publicado: false), así que se consulta desde adentro del contenedor:
for s in aportes auditoria cumplimiento entregas erp garantia grupos identidad inversiones notificaciones \
         nucleo-financiero organizador publicidad tarifas transparencia; do
  printf '%s ' "$s"; docker compose -f despliegue/compose/base.yml -f despliegue/compose/servicios.yml \
    exec -T "$s" wget -q -O - http://127.0.0.1:8080/actuator/health/readiness; echo
done
```

Notas: el README construye solo siete imágenes; para el DoD hacen falta las quince. Si un servicio no queda
`healthy`, se clasifica (regla 80.4) antes de tocar nada. La salida va a
`docs/trabajo/2026-11-21-f0-preparacion/evidencia/F0.M2/`.

## Anexos — las 18 microtareas F0

| Microtarea | Tipo | Destino |
|---|---|---|
| F0.L2.M01 ADR del modelo A + catálogo + verificador | Mixta | **Documental hecha**: ADR-056 (#403) y catálogo (#405). **Código, próxima tanda**: endurecer `scripts/validar_flujos.py` (fallar con cuentas 9.x o pasos de respaldo; admitir pasos pendientes con su microtarea); reescribir CU-22/CU-23 y las descripciones de `fondearPozo` y `garantia.yaml` (van con F2.M1/F2.M4 porque describen código vivo) |
| F0.L2.M02 ADR que reemplaza R-SEG-04 y R-ORG-06 para entrega, cobro y cobertura | Documental | **Hecha**: ADR-055 §4 y `docs/Cumplimiento.md` (#402). `docs/Restricciones.md` no se tocó (es fuente del DDL; va con F2.M8). La «issue enlazada» a Cumplimiento se reemplazó por las preguntas escritas en `Cumplimiento.md` (no abro issues sin pedido) |
| F0.L2.M03 Pactos para los saltos que mueven dinero | Código (pactos + pruebas de productor y consumidor) | **Próxima tanda.** Depende de la forma del evento de cobro (H3.S1) y de coordinar con L1 |
| F0.L2.M04 Verificación de productor de `nucleo-financiero-entregas.json` | Código (prueba Java) | **Próxima tanda** |
| F0.L3.M01 ADR que reemplaza a ADR-054 | Documental | **Hecha**: ADR-055 E4–E6 y §3 (#402), orden por nivel en ADR-056 §2 (#403), catálogo en #405 |
| F0.L3.M02 Pactos de productor en `grupos` | Código | **Próxima tanda** |
| F0.L4.M01 ADR que reemplaza a ADR-049 | Documental | **Hecha** (parcial, ver ambigüedad A3): ADR-055 E9 y banner en ADR-049 (#402) |
| F0.L4.M02 ADR del KYC automático | Documental | **Hecha**: ADR-055 E10 (#402). El cambio de `identidad.yaml` («la aprobación sigue siendo manual») es de F3.L4.M02 |
| F0.L4.M03 Decisión de Cumplimiento sobre LFT | Documental, externa | **`DECISION_REQUIRED` (Cumplimiento)**: ya está en `PREGUNTAS-LEGALES.md` como L-07. No se redacta la tabla acción → decisión sin la respuesta |
| F0.L4.M04 Migraciones versionadas y chequeo de deriva | Código grande + Docker | **Próxima tanda** |
| F0.L4.M05 Pactos base de identidad y cumplimiento | Código (pactos con tres niveles + verificación de productor) | **Próxima tanda** |
| F0.L5.M01 ADR de comisión | Mixta | **Documental hecha**: ADR-056 §5 con «porcentaje único» (manda la tanda 7, no «fija»). **Código/semilla, próxima tanda**: regla `BOLSA_COMPLETA` en `sql/60_semillas/09-reglas-operativas.sql` (vía su fuente) y `scripts/generar_catalogo_operaciones.py:30` |
| F0.L5.M02 Suite de integración en la máquina de Pablo | Código + Docker | **Próxima tanda** (el comando base queda arriba) |
| F0.L5.M03 Integración con el rol del servicio y semillas reales | Código | **Próxima tanda**; depende de F0.L5.M02 |
| F0.L5.M04 Verificación de deriva | Código | **Próxima tanda**; se une con F0.L4.M04 (misma causa) |
| F0.L5.M05 Pactos de productor y campos de destino obligatorios | Código (contratos + pruebas) | **Próxima tanda**; depende de F0.L3.M02 |
| F0.L6.M01 Catálogo de códigos de error en el contrato | Código | **Próxima tanda** |
| F0.L6.M02 Clientes generados fuera del índice | Código (git rm + CI) | **Próxima tanda** |

## Evidencia

Todo en [`evidencia/`](./evidencia/):
- `F0.M1-blocker-gate.txt`, `F0.M3-adr055.txt`, `F0.M4-adr056.txt`, `PX0.M01-adr058.txt`, `F0.M5-flujos.txt`.
- `linea-base/verificadores/*.txt` (uno por verificador, con comando, HEAD y `rc`).
- `linea-base/gradle/*.txt` (uno por servicio) y `_resumen.txt`; `linea-base/postgres-local.txt`.
- `linea-base/yarn/` (`install`, `lint`, `typecheck` con y sin clientes, `generarClientes`, fragmentos desfasados).

```text
$ python3 .claude/hooks/blocker_gate.py --self-test
blocker_gate self-test: 11 PASS, 0 FAIL
$ echo {"session_id":"f0-m1-verificacion",…} | python3 .claude/hooks/blocker_gate.py
exit=0           (antes, en origin/dev: «Hay 9 microtarea(s)…», exit=2)
$ python3 scripts/verificar_boveda.py   (rama -adr058, con 055, 056 y 058)
  OK · 55 decisiones registradas · TODO OK
$ python3 scripts/validar_flujos.py     (rama -flujos)
Sin fallas.
```

## No cubierto

- **Ningún ADR tiene prueba de comportamiento**: son documentos. Lo que describen se verifica en F1–F3.
- **El catálogo vivo** ya no presenta el respaldo, pero **el código del respaldo sigue existiendo** hasta F2.M1–M3.
  Dos casos de `entrega-del-turno.yaml` tienen una fixture que hoy arma el pozo con el adelanto de la empresa.
- **`objetivo-modelo-A.md` no está verificado**: describe pasos sin código.
- **Línea base:** sin `integrationTest`, sin `webTest`, sin `cobertura.py`, sin `flutter analyze`, sin E2E.
- **Mientras #401 no se mergee**, cualquier rama que salga de `dev` sigue trabada por los 9 bloqueantes viejos
  (lo vi en esta misma sesión al trabajar en la rama de línea base).

## Desvíos del plan

1. **Ramas:** un PR por microtarea exige una rama por PR. Usé la rama asignada (`correcciones/f0-preparacion-2026-11-21`)
   para #400 y ramas con sufijo para las demás.
2. **ADR apilados:** #402 → #403 → #404 salen uno de otro, porque los tres tocan el índice y la cifra de ADR de
   `planes/00 Plan maestro.md` que valida `verificar_boveda.py`. Se mergean en ese orden.
3. **F0.M5:** el plan pedía el catálogo «con los pasos del modelo A» y en verde. No es posible sin inventar
   `operationId` o sin tocar el validador (ver #405). El flujo objetivo quedó en `objetivo-modelo-A.md`, y el
   catálogo vivo, en verde, sin respaldo.
4. **Banners agregados** en ADR-052 (DD-2) y ADR-035 (WhatsApp), que la instrucción no nombraba, porque DECISIONES
   también los cambia. Declarados en #402 y #404.
5. **`matriz.md` regenerada** en #405: el diff incluye un desfase que ya existía en `dev`.
6. **Línea base:** se corrió `test` (lo pedido); no `webTest`. Para `typecheck` se corrió además `generarClientes`, el
   paso previo que hace la CI, porque sin él el resultado no medía nada. Yarn se tomó de npm porque el proxy niega
   `repo.yarnpkg.com`.

## Riesgos residuales y deuda

- Conflictos de merge si alguien más toca `_Arquitectura.md` o la cifra de ADR antes que la pila.
- `verificar_rutas_frontend.py` en rojo en `dev` (lista de conocidas vieja): cualquier gate que lo exija ya falla.
- `matriz.md` desfasada en `dev` hasta que se mergee #405.
- El `build` de `@aportaya/simulado` ensucia el árbol (crea un ejemplo nuevo); tres fragmentos de `clientes/dart`
  versionados están desfasados del contrato (F0.L6.M02).
- La red del entorno niega `repo.yarnpkg.com`: `corepack` no puede bajar Yarn. Si se quiere `yarn` sin el rodeo, hay
  que permitir ese host en el entorno (no lo propuse: la instrucción pedía yarn solo «si hay dependencias»).
- El texto «Diez ADR quedaron superados» de `_Arquitectura.md` ya era falso (hay 13, 14 con ADR-051).
- El `PLAN.md` del plan maestro no tiene IDs que `plan_lib` reconozca (lo marca «malformado»); no bloquea, pero su
  avance no se puede calcular con `plan_status.py`.
- `docs/Cumplimiento.md` afirmaba que declarar un incumplimiento exige una persona; P-12 lo automatiza. Quedó
  como pregunta para Cumplimiento.

## Decisiones y ambigüedades

Actualizado tras la tanda 9 de `DECISIONES.md` (mergeada en `dev` con #414): A1, A2, A5, A6 y A7 quedaron
resueltas y aplicadas en #402, #403 y #405. Siguen abiertas A3, A8, A9 y A10.

| # | Ambigüedad | Supuesto tomado | A quién confirmar |
|---|---|---|---|
| A1 | Inicio del grupo: «al completarse los cupos» vs la regla del 75 % de ADR-054 D3/D4 | **Resuelta (tanda 9):** a la fecha límite decide el creador (achicar, extender o cancelar); reemplaza el 75 %. Aplicado en ADR-055 E6 y §1-bis (#402) | Pablo (decidido el 2026-11-21) |
| A2 | Disolución automática por mora generalizada (ADR-054 D7) vs disolución votada (tanda 7) | **Resuelta (tanda 9):** la disolución automática sigue, con umbral; convive con la votada. ADR-055 E14 (#402) | Pablo (decidido el 2026-11-21) |
| A3 | ADR-049: «reemplace» (P-09) vs «reemplazado parcialmente» (instrucción) | Parcial: sobrevive la máquina de estados para la orden retenida por alerta | Pablo (en #402) |
| A4 | Comisión «fija» (anexo L5) vs «porcentaje único» (tanda 7) | Manda la tanda 7 | — (decidido) |
| A5 | Quién es «el perjudicado» si el fondo cubrió a tiempo o si son varios | **Resuelta (tanda 9):** el perjudicado es el fondo del grupo; si el fondo no alcanzó, quien esperó. ADR-056 R2 (#403) | Pablo (decidido el 2026-11-21) |
| A6 | Reparto de lo incobrable: ¿entre todos o solo entre quienes no deben? ¿se compensa la devolución del moroso? | **Resuelta (tanda 9):** primero se compensa con la devolución del moroso; el resto, solo entre los que cumplieron. ADR-056 A5 (#403) | Pablo (decidido el 2026-11-21) |
| A7 | «Grupo perfecto»: el fondo se devuelve antes, ¿de qué? | **Resuelta (tanda 9):** el fondo se devuelve igual; el ciclo siguiente del mismo grupo aporta un porcentaje menor. ADR-056 D4 (#403) | Pablo (decidido el 2026-11-21) |
| A8 | Plan de cuentas del modelo A | Transcrito de H3.S3, «a validar con el contador» | Contador |
| A9 | RM-01: ruta `infraestructura/proveedores/` (plan) vs `infraestructura/adaptadores/<puerto>/` (ADR-033) | Patrón existente (ADR-033) | Pablo (en #404) |
| A10 | ¿Alguna norma exige segregación humana en la entrega o el cobro? ¿Basta con persona en descargo y apelación? | Preguntas escritas en `Cumplimiento.md`; sugerido sumarlas a `PREGUNTAS-LEGALES.md` (no lo edité: se copió tal cual) | Cumplimiento |
