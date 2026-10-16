# Entorno para pruebas de aceptacion de dos usuarios (agente E)

Estado actual: **abajo** (ver ESTADO-ENTORNO.md). Se levanta en ~2 minutos. Consume RAM: con el gateway y
8 servicios el conjunto usa ~2,4 GB de los 4 GB de Docker Desktop (.wslconfig: 4 GB). **Los quince servicios
juntos NO entran** en esta maquina (cada JVM ~200-260 MiB con `-Xmx144m`, mas ~0,9 GB de infraestructura):
se levanta el subconjunto que la prueba necesite.

## Levantar

Todo por el guardian de RAM (lo aplica el propio guion). Puerto base D = 5444.

```bash
# 1. imagenes aportaya/<servicio>:test ya construidas en esta maquina (17). Si faltan:
#    bash despliegue/ensayos/arranque/construir_imagenes.sh <servicio> ...   (de a una)
# 2. el subconjunto que QUEDA ARRIBA + usuarios sinteticos:
D3_MODO=conjunto D3_SEMBRAR=1 D3_MANTENER=1 \
D3_SOLO="grupos nucleo-financiero cumplimiento tarifas aportes organizador" \
  bash despliegue/ensayos/arranque_completo.sh
```

`identidad` y el gateway siempre estan. Para admision humana (aceptar/rechazar) bastan `identidad` y
`cumplimiento`; para grupos/aportes sumar `grupos`, `tarifas`, `aportes`, `nucleo-financiero`,
`organizador`. El perfil es `local` (el de TEST hoy): **el segundo factor es `000000` para cualquiera**.

## Bajar

```bash
bash despliegue/ensayos/bajar_entorno.sh                  # solo toca recursos aportaya-d3*
D3_DOCKER_APAGAR=1 bash despliegue/ensayos/bajar_entorno.sh   # ademas apaga Docker Desktop
```

## URLs y puertos (solo loopback)

| Que | Direccion |
| --- | --- |
| API (gateway) | `http://127.0.0.1:18080/api/v1/...` |
| PostgreSQL (admin) | `127.0.0.1:5444`, base `pasanaku`, usuario `pasanaku` |
| PgBouncer | `127.0.0.1:6444` |
| Salud de un servicio | `docker exec aportaya-d3-<servicio>-1 wget -qO- http://127.0.0.1:8080/actuator/health` |

La clave del admin de la base esta en `%TEMP%\aportaya-d3\entorno.env` (fuera del repo, generada al vuelo).
Los fronts (backoffice/web/movil) NO estan en este entorno: apuntarlos a `http://127.0.0.1:18080` con su
proxy `/api` de desarrollo.

## Usuarios sinteticos (D3_SEMBRAR=1, o `bash despliegue/ensayos/arranque/sembrar_usuarios.sh`)

Se escriben en `%TEMP%\aportaya-d3\credenciales-e2e.txt` (modo 600, **fuera del repo**; el guion se niega a
escribir dentro del clon). Contrasenas generadas en el momento (`secrets.token_urlsafe`), hash Argon2id
con la pimienta del entorno (identidad hashea `credencial + pimienta`, `Argon2Hasheador.sazonar`).

| Rol | Codigo | Para que |
| --- | --- | --- |
| `ADMIN_PLATAFORMA` | USRE2E01 | operador de plataforma |
| `OFICIAL_CUMPLIMIENTO` | USRE2E02 | backoffice: el que decide expedientes (aprobar/rechazar) |
| `PARTICIPANTE` | USRE2E03, USRE2E04 | los dos usuarios del ensayo |

Ingreso (verificado con las 4 cuentas, HTTP 200 con JWT): dos pasos contra `POST /api/v1/sesiones`.
1. `{"telefonoE164","credencial","huellaDispositivo","plataforma":"WEB"}` -> 422 `AP-CU04-03` (pide el codigo).
2. igual + `"factor":{"tipo":"OTP","valor":"000000"}` -> 200 con `tokenAcceso`.

Requisito del seed: cada cuenta lleva un factor enrolado (`TOTP` para operadores por R-SEG-10, `SMS` para
participantes); sin el, el segundo paso responde `AP-CU04-04` aunque el codigo sea `000000`.

## Lo que FALTA (decirselo al agente E)

- **No hay billeteras ni datos de negocio** para los usuarios sembrados: `nucleo-financiero` abre la cuenta
  al consumir `identidad.usuario_registrado`, y estos usuarios se insertaron directo. Para un flujo con dinero
  hay que registrarlos por `POST /api/v1/usuarios` (CU-01) con el servicio `nucleo-financiero` arriba, o
  sembrar `sql/61_dev` (marca `app.entorno='dev'`; trae datos personales del equipo, ver nota abajo).
- **Perfil `production`/`staging` no es ensayable**: `nucleo-financiero` no arranca (falta un
  `ProveedorDeRetiro` real; ver FASE2.md). Todo corre en `local`.
- Fotos de identidad: MinIO esta en la maquina pero el bucket `aportaya-archivos` se crea como carpeta sin
  versionado (no se pudo bajar `quay.io/minio/mc` con la red de esta maquina).
- Kafka no precrea topicos (`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, igual que en TEST): los eventos entre
  servicios dependen de lo que cada servicio cree al arrancar; no se verifico el flujo por eventos de punta a
  punta (solo arranque, readiness y humo por el gateway).
- Nota de privacidad (regla 90.2): `seeders/dev/15-usuarios-dev.json` lleva correos personales reales del
  equipo; **no se usaron** para este entorno. Conviene reemplazarlos por direcciones `@ejemplo.invalid`.
