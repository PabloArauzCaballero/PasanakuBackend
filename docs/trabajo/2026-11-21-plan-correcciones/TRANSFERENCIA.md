# Transferencia — coordinación del plan de correcciones de Pasanaku (2026-11-21, 23:20 UTC)

> Para la sesión que retome la coordinación. Leé esto entero antes de actuar. Todo lo que dice está en GitHub;
> nada vive solo en el chat.

## 1. Qué es esto y cómo se trabaja

- Repo: `PabloArauzCaballero/PasanakuBackend` (monorepo Java/Spring + Flutter + Angular). Fuentes de verdad, en orden:
  `CLAUDE.md`, `AGENTS.md`, `.claude/rules/`, `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md`
  (tandas 1–11), `docs/trabajo/2026-11-21-plan-correcciones/PLAN.md` + `PLAN-PROVEEDORES.md` + `anexos/` + `EJECUCION.md`.
- **Definición de hecho de Pablo:** mergeado en `dev` **y promovido a `test`**, sin regresiones. La sesión
  coordinadora mergea (Pablo lo autorizó: «mergealo tenés permiso»). Nunca a `main` sin su OK explícito.
- **No detenerse:** orden de Pablo. Lo único que se escala es decisión de negocio/legal, acción destructiva sobre datos
  compartidos, o algo que el sistema de permisos bloquee.
- **Tope de 3 PR abiertos por sesión** (pedido de Pablo: «el backend está demasiado lleno»).
- Reportes de avance: **cuando Pablo pregunte, un HTML con barras por sesión** (generador en el scratchpad; rehacelo
  si se perdió: una barra por sesión con microtareas HECHO/total y PR mergeados/total).
- Nunca imprimir secretos. Nunca `UPDATE`/`DELETE` sobre datos de dinero. Nunca editar `sql/` a mano (se genera con
  `scripts/generar_ddl.py`, salvo `50_verificacion/prueba_humo.sql` y `60_semillas/`).

## 2. Sesiones de ejecución (tanda 2), todas vivas, en la nube

| Lote | Sesión | Alcance | Estado |
|---|---|---|---|
| L1 aportes + núcleo financiero | `session_01N9Xy4qW5RRVTwrzXYbHbTY` | `servicios/aportes`, `servicios/nucleo-financiero` (+ autorizaciones puntuales abajo) | 10/20. Abiertos #480, #483, #488. Sigue: M08 opción A (partición mensual de `webhook_pasarela`). |
| L2 garantía + entregas | `session_01N9Xy4pzCpE4gU2Kbyq6CJu` | `servicios/garantia`, `servicios/entregas` | 6/15. Abierto #484. Modelo A **espera Cumplimiento** (T11-2). |
| L3+L5 grupos y apoyo | `session_01N9Xy4sWCvMUeVf3UfpytJo` | grupos, organizador, auditoría, notificaciones, publicidad, tarifas, transparencia, inversiones, erp | 10/27. Abiertos #448, #486. Sigue: registro de parámetros en `tarifas` (M2b API, M6 congelamiento por evento, M4 pantalla). |
| L4+L6 identidad, cumplimiento, frontends | `session_01N9Xy4s2eUR6YsKRFvs7P5v` | identidad, cumplimiento, `apps/`, `packages/`, `clientes/` | Abiertos #460, #487. Lista en local: pantalla de bloqueos del backoffice (PR cuando entre #488). Pendiente: retirar `*_SERVICIO_FACTOR_FIJO` (comun-web, yml de grupos/entregas/inversiones, `despliegue/ensayos/arranque/lib.sh`). |

Cada sesión tiene su `docs/trabajo/2026-11-21-tanda2-<lote>/PLAN.md` y `REPORTE.md`. Para hablarles:
`mcp__claude-code-remote__send_message` con su `session_id`.

## 3. PR abiertos y qué hacer con cada uno

| PR | Qué | Acción |
|---|---|---|
| #489 | DECISIONES tanda 11 (docs) | Mergear cuando esté verde. |
| #460 | App: sin subasta + goldens billetera regenerados + **M11 pago con saldo** | Mergear cuando esté verde. **Bloquea la promoción** (desde #478, la app vieja falla al pagar con canal ≠ billetera). |
| #487 | Fix `ng serve` del backoffice (regresión de #426) | Mergear cuando esté verde. |
| #483 | M07 identidad de servicio (grupos/entregas/garantia → aportes; rol `SERVICIO_GARANTIA`) | Mergear verde. **No promover** sin que TEST tenga las cuentas de servicio (ver §5). |
| #480 | M01 `Importe` sin signo en nucleo-financiero (incluye 3 líneas de `apps/movil`, autorizado) | Mergear verde. |
| #488 | `GET /billetera/bloqueos` | Mergear verde; después avisar a L4L6 para abrir la pantalla. |
| #486 | Modelo del registro de parámetros en `tarifas` | Mergear verde. |
| #484 | Parámetros del modelo A obligatorios (interruptor `MODELO_A_HABILITADO`, apagado por omisión) | Mergear verde. |
| #448 | L3L5: notificaciones/transparencia/tarifas (autorización, webhook, certificado) | Mergear verde. |
| #468 | `dependabot.yml` → `target-branch: dev`, **contra `main`** | El sistema de permisos bloqueó mergearlo. Necesita que Pablo lo mergee o lo autorice explícitamente. Después cerrar #394–#397 (apuntan a `main`) para que dependabot los rehaga contra `dev`. |
| #394–#397 | Dependabot contra `main` | Ver #468. |

Herramientas que usé (scratchpad, pueden haberse perdido; son triviales de rehacer):
- `ci.sh <n…>`: estado de checks por PR (`gh api repos/…/commits/<sha>/check-runs`).
- `candidatos.sh` / `vigia.sh`: lista PR `correcciones/tanda2-*` o `claude/*` con todos los checks completos, sin
  fallas y `mergeable_state=clean`; el vigía hace bucle cada 90 s y sale cuando hay uno.
- Antes de mergear: mirar tests borrados (`files` con `status=removed` en `src/test`) y que estén justificados en el PR.
- PR apilados: si la punta está verde y contiene a los anteriores (`git merge-base --is-ancestor`), se mergean en orden.
- «Release iOS (macOS)» tiene un intermitente conocido (`deep_link_test 02`, ver §6); si es el único rojo y el PR no
  toca `apps/movil`, se mergea dejando un comentario.

## 4. `dev` y `test`

- `test` = a9819e0f (promoción #469). `dev` = c1f0cc95, **137 commits adelante**.
- **La próxima promoción (`dev` → `test`) NO puede salir hasta** que: (1) entre #460; (2) Pablo cargue los secretos y
  cree las cuentas de servicio en TEST; (3) `chequear` dé `RESULTADO: PASA`. Sin eso, `aportes` (#458), `entregas`
  (#463) e `identidad` (#442) no arrancan.
- Cómo se promueve: PR `head=dev` → `base=test`, esperar CI de la punta de `dev` (no mergear nada a `dev` mientras
  tanto, porque cancela la corrida), mergear. Coolify despliega en ~2 min.

## 5. Servidor de TEST (Pablo, por SSH; la nube no llega: el proxy bloquea 161.97.85.216)

- Instrucciones completas para la sesión local de Pablo: `/home/user/PASANAKU-SERVIDOR-TEST.md` (fuera del repo; copia
  en este mismo directorio: `SERVIDOR-TEST.md`). Incluye el guion `aportaya-promocion.sh` (modos `chequear`, `estado`,
  `llaves-mfa`) y el orden de pasos.
- Secretos que TEST necesita en Coolify (app `aportaya-api`): `MFA_LLAVES`, `MFA_LLAVE_VIGENTE`,
  `ENTREGAS_LLAVE_CIFRADO_CUENTAS`, `APORTES_WEBHOOK_SECRETO`, `JWT_CLAVE_FIRMA` (confirmar), y por cuenta de servicio
  `GRUPOS_/ENTREGAS_/INVERSIONES_SERVICIO_{TELEFONO,CLAVE,TOTP_SEMILLA}` (y `GARANTIA_…` cuando entre #483), con
  `*_SERVICIO_FACTOR_FIJO` vacía.
- **Hoy en TEST sorteo, entrega e inversiones fallan cerrados** por falta de credencial de servicio (causa doble:
  compose con vacíos fijos, arreglado en #481; y `000000` eliminado en #412). Se arregla con
  `despliegue/ensayos/aprovisionar_cuentas_servicio_test.sh` (#482) + cargar variables + promover.
- Precondiciones de datos que `chequear` verifica (todas en 0): coberturas en `SOLICITADA` (#477), entregas en
  `EFECTIVO_ORGANIZADOR` (#463), reservas/coberturas/planes vivos del respaldo (#479), importes negativos (#411).
- Los TOTP de operadores humanos de prueba los siembra Pablo en su propia terminal
  (`despliegue/ensayos/arrancar_totp_test.sh`), nunca por un chat.

## 6. Abierto / riesgos

- **Intermitente iOS** `deep_link_test 02` (2 de 3 corridas): instrumentado (#460 + #466 sube el log del simulador como
  artefacto `diagnostico-release-ios-<sha>`). Cuando vuelva a fallar, pasarle el artefacto a L4L6.
- **Spring Boot 4 / Spring 7 antes del 2026-12-21**: vencen las excepciones de Trivy/OSV (`.github/.trivyignore`,
  `.github/osv-scanner.toml`). Tarea sin asignar; toca `build.gradle*` y `buildSrc` de todos: hacerla con las sesiones
  en pausa o en una sesión dedicada.
- **Cola de CI saturada** (7 runners). Cancelar corridas viejas lo bloqueó el sistema de permisos: Pablo lo hace a mano
  o lo autoriza.
- **PasanakuFrontend** (repo aparte, copia vieja del monorepo): #53, #56–#58 de dependabot con rebase pedido; su `dev`
  ya tiene rojos propios (OSV y Frontend). Preguntar a Pablo si ese repo sigue en uso.
- Decisiones pendientes de Pablo: `politica_cobertura` y tablas del aval (borrar o no, consultas en
  `docs/trabajo/2026-11-21-tanda2-l2/evidencia/`); valores de los parámetros financieros (backoffice, cuatro ojos);
  opinión de Cumplimiento L-01..L-03 (`PREGUNTAS-LEGALES.md`) para el modelo A.

## 7. Decisiones tomadas por la coordinación (para ratificar)

- Respaldo y aval retirados por partes (expandir → 410 → contraer), sin borrar tablas con datos.
- `RecaudoPorHttp` de garantía se conserva para F3 (cobertura del fondo del grupo).
- Parámetros del modelo A validados solo con `MODELO_A_HABILITADO=true`.
- Mockito como `-javaagent` en toda JVM de prueba (#444); aliado simulado en HTTP/1.1 (#453); credenciales del
  compose desplegado como `${VAR:-}` con guarda (#481).
