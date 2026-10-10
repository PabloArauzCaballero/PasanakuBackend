# FASE 2 — los quince servicios + gateway con la configuracion que se despliega en TEST

Comando final (todo por `heavy.sh`): `D3_ESCENARIO=actualizacion D3_MODO=tandas bash despliegue/ensayos/arranque_completo.sh`.
Base: la de `59370bbd` con datos de dev (402 tablas, 11 usuarios, 25 movimientos) -> trabajo de esquema actual.
Compose: `docker-compose.coolify.yml` regenerado, sin editar; imagenes `aportaya/<servicio>:test` construidas con
`despliegue/Dockerfile` (17 imagenes, `evidencia/carril-D3/construccion-*.log`). Perfil `local`, secretos sinteticos.

## Resultado por servicio (readiness UP, `arranque-f4-final-tandas-actualizacion.txt`)

| Servicio | Arranca / readiness | RAM (MiB / limite 320) | Notas |
| --- | --- | --- | --- |
| gateway | UP | ~232 | rutas.yml con las 15 rutas; `inversiones` ruteada (401 sin token) |
| identidad | UP | ~216 | sin `INVITACIONES_CLAVE` arranca pero falla en runtime (FASE1) |
| aportes, auditoria, cumplimiento | UP | 189-198 | |
| entregas, erp, garantia, grupos | UP | 189-206 | |
| inversiones | UP | ~204 | aliado `deshabilitado`; con perfil `production` tambien UP |
| notificaciones, organizador | UP | 189-190 | |
| nucleo-financiero | UP | ~260 | **no arranca sin `QR_INTERNO_CLAVE`** (FASE1); **no arranca con perfil `production`/`staging`** (abajo) |
| publicidad, tarifas, transparencia | UP | 189-200 | |

Humo (`humo-f4-final-tandas-actualizacion.txt`): 0 fallas en las 4 tandas: readiness y `/actuator/health`
(sonda de base por PgBouncer con el rol `svc_*`), gateway liveness/readiness, `POST /api/v1/sesiones` rechaza por
contrato (422), cada prefijo de `rutas.yml` responde 401 sin token (`/verificar` 200 por contrato publico),
`/api/v1/inversiones/productos` 401, ruta inexistente 404.

**RAM pico de los contenedores: 1.813 MB** con 10 contenedores (5 de infraestructura + gateway + 4 servicios).
Con gateway + 8 servicios + infra: 2.357 MB. Los 16 JVM a la vez (~16 x 210 MiB + ~0,9 GB de infra) superan los
4 GB de Docker Desktop (.wslconfig): por eso se probo **en tandas de 4**, no los quince juntos. En el host de
TEST (23 GB, `MaxRAMPercentage=75`) no aplica. No se cambio `.wslconfig` ni `gradle.properties`.

## Perfiles (inversiones + nucleo-financiero + identidad + gateway, base limpia)

| Perfil | gateway | identidad | nucleo-financiero | inversiones | Causa raiz |
| --- | --- | --- | --- | --- | --- |
| `local` (TEST hoy), CORS vacio | UP | UP | UP | UP | el valor de `APORTAYA_CORS_ORIGENES` no se usa en local |
| `production`, sin `APORTAYA_CORS_ORIGENES` en el compose (antes de este trabajo) | **cae** | UP | **cae** | UP | `ConfiguracionCors`: «aportaya.cors.origenes vacio o con '*' en un perfil no local/test»; `log-f2-perfil-production-gateway.txt` |
| `production`, con la variable cargada (compose corregido) | UP | UP | **cae** | UP | `CU11RetirarSaldo` (parametro 9) pide un bean `ProveedorDeRetiro` y solo existe `ProveedorDeRetiroLocal` (`@Profile({"local","test"})`, `ProveedorDeRetiroLocal.java:38`); `log-f2-perfil-production-cors-nucleo-financiero.txt` |

Conclusion: **TEST tiene que seguir en `PERFIL_SPRING=local`** (o `test`). Pasar a `production` exige codigo de
nucleo-financiero (un adaptador real de retiro, o decision de negocio): NO es un cambio de despliegue. Diagnostico
para el dueno del servicio: `servicios/nucleo-financiero/.../infraestructura/ProveedorDeRetiroLocal.java:38`,
`aplicacion/CU11RetirarSaldo.java` (constructor, parametro 9).

## Cambios de despliegue hechos

- `scripts/generar_compose.py`: `INVERSIONES_LIBRO_CREDENCIAL_SERVICIO`, `NOTIFICATION_EMAIL_PROVIDER` y las `GMAIL_*`
  en `DE_DESARROLLO` (el generador de desarrollo se negaba a escribir: 5 variables sin valor); `variables_de()` lee
  tambien `application-<perfil>.yml` y declara `APORTAYA_CORS_ORIGENES` (servicios + gateway); `ROLES_CLAVE` en el
  trabajo de esquema. Regenerados: `docker-compose.coolify.yml`, `despliegue/coolify/*.yml` (sin cambios),
  `rutas.yml` (sin cambios: ya traia `inversiones`).
  `URL_ENTREGAS` aparece ahora en `nucleo-financiero` (el servicio la declara y la derivaba el generador).
- `despliegue/aplicar-esquema.sh`: expansion de columnas y CHECK antes de aplicar (FASE1) y credencial de los
  `svc_*` sin clave (`svc_inversiones` nace NOLOGIN: sin esto no conecta).
- `despliegue/ensayos/`: `arranque_completo.sh`, `bajar_entorno.sh`, `arranque/{infra.yml,servicios.override.yml,
  lib.sh,construir_imagenes.sh,humo.sh,comparar_esquemas.sh,sembrar_usuarios.sh}`.

## Reglas 97.3 / compatibilidad hacia atras

- Expandir antes de contraer: la expansion es aditiva; las imagenes viejas siguen sirviendo contra el esquema
  nuevo (columnas opcionales o con default; tablas nuevas ignoradas). Una columna `NOT NULL` sin `DEFAULT`
  entra opcional.
- **Cambios destructivos o incompatibles detectados en `git diff 59370bbd -- sql`**: ninguno destructivo. Se
  ensancharon 5 CHECK de enumeracion (agregar valores); se agrego `UQ` sobre `grupos.invitacion.token_id`
  (falla con claro error si ya hay tokens repetidos); el trigger `tg_grupo_contrato_organizador` se repunta a una
  funcion nueva y la vieja queda huerfana; `rol_migracion`/`rol_auditor` cambian su `search_path` (suman
  `inversiones`).
- Una segunda corrida del trabajo de esquema es idempotente (0 columnas, 0 restricciones, exit 0).

## No cubierto
- Flujo por eventos de punta a punta (Kafka sin topicos precreados); carga; perfiles `staging`.
- Los tres fronts y NGINX/Traefik (no estan en este entorno).
- El estado real de la base de TEST (solo se reprodujo con la version `59370bbd` y datos de dev): antes de subir,
  correr la etapa 3 del procedimiento (TEST.md) contra una copia del respaldo.
