# Carril L2 (8.ª vuelta) — Sandbox sintético de licencia en TEST y repetición del recorrido A/B

Peldaño por área (regla 30):
* **Sandbox de licencia GRUPO_PASANAKU en TEST: VERIFIED** (aplicado en TEST por ssh; `GET /licencia/alcance?servicio=GRUPO_PASANAKU` con sesión real de A pasó de `habilitado:false` a `habilitado:true, via:SANDBOX`; BILLETERA/RECARGA/RETIRO/TRANSFERENCIA_P2P siguen `false`). Probado antes en local dos veces (idempotencia) y en TEST dos veces (`cambios: 0`).
* **Recorrido A crea grupo → invita → B acepta/rechaza: BLOQUEADO, NO ocurrió.** Tras habilitar el servicio, `POST /grupos` de A responde `500 AP-INT-01` por un segundo bloqueo de datos (R-ORG-02, abajo). No hubo grupo, invitación, aceptación ni rechazo; no se abrió ningún emulador (sin grupo no hay qué mostrar que no esté ya en `carril-H3`).
* **Contrato sintético de organizador (seeder 17 ampliado): TESTED en local, NO aplicado en TEST** (requiere autorización de la persona dueña: es un `INSERT` más en su base, fuera de «ese script»).

Fecha: 2026-10-11 · Rama `justin/feature/cierre-endurecimiento-2026-10-08`. Cuentas: las de `Temp\aportaya-s\cuentas-test.txt` (fuera del repo; teléfonos, claves y tokens enmascarados).

## Tarea 1 — el sandbox
Por qué `entorno_prueba_regulado` y no la licencia: `cumplimiento` habilita un servicio por licencia OTORGADA que lo cubra **o** por un entorno de prueba ACTIVO de ese servicio (R-LIC-01, CU-46 flujo 2; `HabilitacionDeServicio.java`, `fn_lic_servicio_habilitado`). Con la segunda puerta **no se toca la licencia** (sigue ASFI/CERTIFICADO_ADECUACION/EN_TRAMITE, sin número de resolución): no se inventa número ni autoridad. La fila lleva «sandbox de TEST: no es una licencia ASFI» en `alcance`, 50 usuarios / 50000.00 de tope (necesarios por `ck_sandbox_limites`, R-LIC-02) y 90 días de ventana.

| Pieza | Archivo |
|---|---|
| Seeder (dev, sintético, `apto_produccion=false`, id estable, `ON CONFLICT (id) DO NOTHING`) | `seeders/dev/18-licencia-sandbox-test.json` (+ manifiesto, `seeders/README.md`) |
| Guion para ssh: `--estado`, `--retirar` (FINALIZADO), reactiva si venció; guardas | `despliegue/ensayos/sandbox_licencia_test.sh` |
| SQL embebido, generado (no a mano) | `scripts/generar_semillas.py` (ahora sincroniza dos guiones), `sql/61_dev/18-…sql` |
| Documentación | `despliegue/TEST.md` («Sandbox de licencia para GRUPO_PASANAKU en TEST») |

Guardas (todas probadas en local, `02`–`04`): sin contenedores de servicio → 3; cumplimiento con perfil `production` → 5; con perfil `default` → 5; `app.entorno='production'` → 5; licencia OTORGADA → 5. Ninguna escribe (estado igual antes y después).
Local (`00-prueba-local-corrida-completa.log`, Postgres 16 desechable en 5452, esquema actual): aplicar → `cambios: si`; segunda → `cambios: 0`; `--retirar` ×2; aplicar reactiva; ventana vencida se renueva; orquestador `sembrar_dev.sql` ×2 sin duplicar. Hallazgo de la propia prueba: `ck_sandbox_limites` obliga `fecha_fin > fecha_inicio` (una ventana «vencida» exige mover también el inicio).
Prueba de humo del esquema con dev + sandbox: 1 FALLA (`R-AUD-01 47 de 31 tablas selladas`), **idéntica sin la fila** (`07b`): preexistente, no del sandbox; `R-LIC-01` sigue OK.
TEST (`10`, `11`): antes `sandbox_filas=0 … fn_lic_GRUPO_PASANAKU=false`; aplicar `cambios: si`; segunda corrida `cambios: 0`; después `sandbox_activo_vigente=1 fn_lic_GRUPO_PASANAKU=true fn_lic_BILLETERA=false`. API (`12`): `{"habilitado":true,"via":"SANDBOX","limitesSandbox":{"usuarios":50,"montoOperacion":"50000.00"}}`.

## Tarea 2 — API real: se detiene en crear grupo (defecto de backend + bloqueo de datos)
`12-test-api-hasta-el-bloqueo.txt`: login A/B con segundo factor 200; `GET /licencia/alcance` ya habilitado; `POST /grupos` (A, 100.00 BOB, MENSUAL, 3 cupos) → **`500 AP-INT-01`**.
Causa demostrada (`13-…`, solo lectura de logs): Postgres `ERROR: R-ORG-02: el organizador … no tiene contrato firmado y vigente` (trigger `grupos.fn_org_validar_contrato_grupo`, `sql/40_reglas/restricciones.sql:2667`), que `grupos` registra como «fallo no previsto» (`CreacionRepositorio.crear:87`, `CU20CrearGrupo:95`). Clasificación: DATA (la cuenta A sintética de la 6.ª vuelta se sembró habilitada pero sin contrato) **y** PRODUCT_BUG (el rechazo de una regla de negocio llega al cliente como 500 «Volvé a intentar» en vez de un 422 con código propio; el cliente reintentaría para siempre). El bug de `grupos` no se tocó (otro servicio): hay que precomprobar el contrato o traducir el SQLSTATE `P0001` de R-ORG-02 a `AP-CU20-02`/código nuevo.
Emitir/firmar el contrato por la API no es posible con las cuentas sintéticas: `emitirContrato` exige `ADMIN_PLATAFORMA` y `firmarContrato` un `tokenFirmaId` real (FK `ck_contrato_org_firma`).

## Lo preparado para destrabarlo (probado en local, sin aplicar)
`seeders/dev/17-usuarios-e2e-sinteticos.json` ahora agrega, para A, un token de firma sintético ya CONSUMIDO y un `contrato_organizador` firmado y vigente (ids estables; `--estado` cuenta `contratos_vigentes`; `--retirar` lo rescinde; re-sembrar lo reactiva). `20-contrato-sintetico-local.log`: control negativo → R-ORG-02 rechaza el grupo; sembrar ×2 → `cambios: 0`; con el contrato el `INSERT` de un grupo pasa el trigger; `--retirar` → vuelve a rechazarse; re-sembrar → vuelve a pasar. `21-…`: orquestador de dev ×2 sin duplicar. Para aplicarlo en TEST (necesita el visto bueno de la persona dueña):
`cat "$TEMP/aportaya-s/cuentas-test.txt" despliegue/ensayos/sembrar_test.sh | ssh -i ~/.ssh/id_ed25519_contabo -o IdentitiesOnly=yes root@161.97.85.216 'bash -s'`
Después hay que repetir `POST /grupos`: pueden aparecer más compuertas aguas abajo (tarifas congela el precio y nucleo-financiero abre la cuenta del grupo por evento; invitar exige cupos libres) que hoy no se pueden ver.

## D4 (billetera por cuenta fija) — no hecho
`GET /billetera/cuentas/propia?moneda=BOB` existe y para A y B responde `422 AP-CU13-01 «No tenes una billetera activa en BOB»` (no tienen billetera). Arreglar `_cuentaPorOmision` (`pantallas/billetera/rutas.dart`, archivo del carril M2) toca cinco pantallas y exige un estado «sin billetera»; no es chico y sin grupo no se puede ver el efecto en el recorrido. Sin cambios en `apps/movil`; no se recompiló APK (`aportaya-test-5-TEST.apk` sigue siendo el vigente).

## Recursos
Guardián: con 2,5–3,0 GB libres y ningún otro proceso pesado mío (sin java/emulador/docker de otros en `tasklist`), `heavy.sh` venció a los 3000 s (`libres: 2488 MB`); reintenté con `MIN_FREE_MB=2500` (3 corridas, solo Postgres 16 + dos contenedores alpine de juguete, todos bajados por `trap`). Docker Desktop ya estaba arriba al llegar; no lo toqué. Sin emuladores ni daemons míos al terminar.

## No cubierto
Crear grupo exitoso, invitación, canje/aceptación/rechazo/revocación, persistencia en `/grupos/participaciones`, UI en emulador, efecto en A, D4, el contrato sintético en TEST, `git push` (denegado por el entorno; commits locales).
