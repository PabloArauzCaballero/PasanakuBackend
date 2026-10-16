# El entorno TEST (Contabo)

Un despliegue de la rama `test` para probar con datos reales, en el VPS
`161.97.85.216`, sobre el mismo Coolify donde vive el TEST de Atlas.

```
https://aportaya.161.97.85.216.sslip.io             el sitio público
https://backoffice.aportaya.161.97.85.216.sslip.io  el portal de operación
https://app.aportaya.161.97.85.216.sslip.io         la app móvil, en el navegador
https://api.aportaya.161.97.85.216.sslip.io         el gateway
```

HTTPS con certificado de Let's Encrypt, y `http://` redirige. Tiene que ser HTTPS: los
navegadores prueban `https://` primero y, con el certificado por defecto de Traefik,
bloqueaban la página antes de llegar a la app. Y la cámara —las fotos del carnet— solo
funciona en un contexto seguro.

## Qué corre dónde, y por qué

**En Coolify** (aplicación `aportaya-api`, rama `test`): el gateway, los catorce
servicios, el trabajo de esquema y los tres fronts. El compose es
`docker-compose.coolify.yml`, que **no se edita a mano**: lo genera
`python3 scripts/generar_compose.py --coolify` del mismo barrido de `servicios/` que el
compose local, para que no diverjan.

**Fuera de Coolify**, en `/opt/aportaya/` del VPS: PostgreSQL, PgBouncer, MinIO, Redis y Kafka.
Coolify recrea la aplicación entera en cada despliegue, y la base, el pool de conexiones,
el almacén de archivos y el broker no pueden reiniciarse cada vez que alguien empuja.

**Coolify no construye.** El compose arranca imágenes `aportaya/*:test` ya construidas en
el host, de a una. Construyendo desde Coolify se lanzaban los quince servicios a la vez
—reescribe los Dockerfile para inyectar sus `ARG`, así que la caché no sirve— y con
`org.gradle.jvmargs=-Xmx3g` eso son 45 GB de heap pedidos en una máquina de 23 GB que
además sostiene otro proyecto. Medido: 16 JVM, carga 49, rumbo al OOM.

## Dos cosas de Coolify que cuestan caro

**No pongas `fqdn` en esta aplicación.** Es de tipo compose, y sus cuatro dominios viven
en `docker_compose_domains`, uno por servicio. Si además se le llena el campo `fqdn` —por
ejemplo para que la columna *Domain* de la lista deje de mostrar `-`—, Coolify genera las
etiquetas de Traefik **solo para un servicio** y los otros tres quedan sin ruta: los
contenedores sanos y el navegador recibiendo `503 no available server`. Medido: con
`fqdn` puesto, 16 etiquetas de Traefik en el compose generado; con `fqdn` en nulo, 64 y
un router por servicio.

Que la lista muestre `-` es el precio de tener los cuatro dominios en un solo recurso.

**Sacar un servicio del compose le borra su dominio.** Al desplegar un compose donde un
servicio ya no está, Coolify poda su entrada de `docker_compose_domains`. Volver a
agregarlo al compose **no** devuelve el dominio: hay que reponerlo a mano. Pasó separando
los fronts en aplicaciones propias antes de crearlas — el orden correcto es crear primero
los destinos y mover después.

## Un push a `test`

El temporizador `aportaya-autodespliegue.timer` mira cada dos minutos si la rama avanzó.
Si avanzó, `/opt/aportaya/bin/reconstruir-si-cambio.sh` reconstruye **solo lo que ese
commit toca** —de a una imagen— y recién entonces dispara el despliegue en Coolify por su
webhook. El registro está en `/opt/aportaya/logs/autodespliegue.log`.

El autodespliegue propio de Coolify está **apagado a propósito**, y esa es la única
manera de que el orden sea correcto: encendido, el webhook de GitHub encolaba el
despliegue al instante —con las imágenes VIEJAS, porque Coolify no construye— y cuando
la reconstrucción terminaba, el disparo llegaba tarde y Coolify lo descartaba con
«Deployment already queued for this commit». Se midió con el commit `858bdaf`. El
disparador es uno solo: primero se construye, después se despliega.

Un cambio en `plataforma/`, en el Dockerfile o en Gradle rehace los quince servicios;
uno en `servicios/x/` rehace solo esa imagen; uno en `sql/` rehace la imagen del esquema.

## La base

Limpia a propósito: tiene el esquema completo y los catálogos mínimos —los mismos que van
a producción— y **nada de las semillas de desarrollo**. La marca `app.entorno = 'dev'` no
está puesta, así que `sql/61_dev/sembrar_dev.sql` se niega a entrar; es la misma guarda
que protege producción, y acá se comprueba que funciona.

Los catorce roles `svc_*` nacen `NOLOGIN` (lo correcto) y reciben credencial por
`/opt/aportaya/postgres/roles-test.sql`, que lee la clave de un archivo fuera del
repositorio — el «gestor de secretos» que el propio esquema describe.

## Entrar

Hay un operador de plataforma; sus credenciales están en
`/opt/aportaya/credenciales-admin-test.txt` del VPS, nunca en el repositorio.

TEST corre con el perfil `local`, que trae el segundo factor de desarrollo: **el código es
`000000` para cualquiera**. Es lo único que hace entrable el backoffice mientras no haya
SMS ni correo de verdad, y es una decisión consciente para un entorno de pruebas. El día
que haya un canal real, se cambia la variable `PERFIL_SPRING` en Coolify y este párrafo
deja de ser cierto.

## Cuentas sintéticas de prueba en TEST (2026-10-11)

El alta normal no sirve para probar en TEST: exige el código de seis dígitos que llega por
correo, y no hay buzón alcanzable (`evidencia/carril-H2`). Para poder recorrer «A organiza e
invita, B acepta o rechaza» con login real hay **dos cuentas sembradas desde el seeder**, sin
correos personales y sin tocar la base a mano:

| Código | Qué es | Teléfono (en la app, 8 dígitos) | Roles |
| --- | --- | --- | --- |
| `SINT-E2E-A` | organizador e invitante | `+59170009101` (`70009101`) | `ORGANIZADOR` (trae `GRUPO_CREAR` y `GRUPO_ADMINISTRAR`) y `PARTICIPANTE` |
| `SINT-E2E-B` | invitado | `+59170009102` (`70009102`) | `PARTICIPANTE` |

Más un otorgante `SINT-E2E-0` (suspendido, sin credencial) que solo firma las asignaciones de rol.
Son **sintéticas y declaradas** (`seeders/dev/17-usuarios-e2e-sinteticos.json`:
`apto_produccion=false`, procedencia «inventado para este repositorio», correos `@example.invalid`).
Lo que simulan sin que haya pasado: el nivel de verificación `BASICO` de A y B (no hay documento ni
decisión de una persona detrás) y la habilitación de A como organizador `APRENDIZ` (no hubo
postulación ni capacitación) y su **contrato de organizador** (una fila firmada y vigente, apoyada en un token
de firma sintético ya consumido: sin emisión ni firma reales; emitirlo por la API exige `ADMIN_PLATAFORMA`). `grupos`
consulta la habilitación antes de crear un grupo y, además, el trigger `R-ORG-02` exige ese contrato: sin él, `POST /grupos`
responde `500 AP-INT-01` (medido en TEST, `evidencia/carril-L2/12-…` y `13-…`). Re-correr `sembrar_test.sh` lo agrega
(idempotente) y `--retirar` lo rescinde.
No se sembró billetera: ni crear, ni invitar, ni aceptar la piden.

**Cómo sembrarlas.** La contraseña de cada cuenta se genera fuera del repositorio, en
`%TEMP%\aportaya-s\cuentas-test.txt` (asignaciones de shell: teléfono, clave y segundo factor). Se
antepone al guion, así viaja por la entrada estándar del `ssh` y no queda en el servidor ni en
`ps`:

```bash
cat "$TEMP/aportaya-s/cuentas-test.txt" despliegue/ensayos/sembrar_test.sh | ssh root@161.97.85.216 'bash -s'
```

`sembrar_test.sh` aplica **solo** esa semilla contra la base `pasanaku` de `aportaya-postgres`, calcula el
Argon2id de `contraseña + pimienta` con la pimienta que lee del contenedor de identidad
(`docker exec … printenv`, nunca impresa), y termina con una sonda que no abre sesión: sin segundo
factor debe responder `422 AP-CU04-03` (credencial correcta). Es idempotente (ids estables,
`ON CONFLICT DO NOTHING`, la credencial solo si falta): la segunda corrida dice `cambios: 0`. Aborta si
identidad no corre con el perfil `local`, que es el que acepta `000000` como segundo factor. El SQL que
lleva el guion lo escribe `scripts/generar_semillas.py` entre las marcas `SQL-GENERADO`; no se edita.

**Cómo comprobarlas y retirarlas.**

```bash
ssh root@161.97.85.216 'bash -s -- --estado'  < despliegue/ensayos/sembrar_test.sh   # solo cuenta
ssh root@161.97.85.216 'bash -s -- --retirar' < despliegue/ensayos/sembrar_test.sh   # baja lógica
```

`--retirar` revoca sesiones y roles, desactiva el factor, quita la credencial, deshabilita al organizador
y marca a los usuarios eliminados, todo en una transacción. Los grupos e invitaciones que se hayan creado
durante la prueba quedan como historial (no se borran). Volver a correr el guion las reactiva.

**Ojo con el bloqueo por intentos.** `CU04Autenticar` cuenta como «fallido» todo intento sin éxito de los
últimos 15 minutos, **incluido el primer paso de un ingreso correcto** (el que termina en `AP-CU04-03`
porque falta el segundo factor). Con cinco ingresos seguidos, un solo error de contraseña bloquea la
cuenta 30 minutos (`AP-CU04-05` y después `AP-CU04-02`). El guion deja un intento así por cuenta y por
corrida (la sonda): si una cuenta queda bloqueada, se espera la media hora; no se destraba tocando la base.
Medido contra la imagen real de identidad (`evidencia/carril-S/21-login-clave-equivocada.log`). Es
comportamiento del servicio, no de las cuentas, y queda anotado, no corregido.

**No existen en producción.** Viven en `seeders/dev/` (nunca en `minimos/`): `sembrar_dev.sql` aborta
sin `app.entorno = 'dev'`, y `sembrar_test.sh` aborta si identidad no corre con el perfil `local` (el de
producción no acepta el segundo factor fijo). Producción no corre ninguno de los dos.

## Sandbox de licencia para GRUPO_PASANAKU en TEST (sintético)

**Por qué.** Con las cuentas de arriba A pasa todas las precondiciones de «crear grupo», pero TEST
responde `422 AP-CU20-04` («El servicio no está habilitado en este momento»): `GET
/licencia/alcance?servicio=GRUPO_PASANAKU` dice `habilitado:false, via:NINGUNA` porque la licencia se siembra
`EN_TRAMITE` a propósito (R-LIC-01, CU-46; `seeders/minimos/08-gobierno-y-licencia.json`). No es un defecto:
es la regla funcionando, y sin licencia el recorrido «A crea grupo e invita, B acepta» no se puede hacer.

**Qué se hace (y qué NO).** Se abre la **segunda puerta** del propio modelo: un `entorno_prueba_regulado`
(sandbox) `ACTIVO` solo para `GRUPO_PASANAKU`, colgado de la licencia sembrada. `fn_lic_servicio_habilitado()`
y `cumplimiento` habilitan un servicio si hay licencia OTORGADA que lo cubre **o** un sandbox activo dentro
de su ventana (CU-46, flujo principal 2). Por eso **no se toca la licencia**: sigue `ASFI /
CERTIFICADO_ADECUACION / EN_TRAMITE`, sin número de resolución, y no se inventa ningún número ni autoridad.
La respuesta pasa a `habilitado:true, via:SANDBOX`, con los límites de la fila. BILLETERA, RECARGA, RETIRO y
TRANSFERENCIA_P2P siguen denegados.

**Es sintético y declarado** (`seeders/dev/18-licencia-sandbox-test.json`: `apto_produccion=false`,
procedencia «sandbox de TEST: no es una licencia ASFI», el campo `alcance` de la fila lleva la misma
leyenda). Lo que simula sin que haya pasado: que cumplimiento haya autorizado un entorno de prueba regulado
(no hay solicitud, resolución, garantía ni informes). Los límites (50 usuarios, 50000.00) están para cumplir
`ck_sandbox_limites` (R-LIC-02); no son un límite regulatorio. La persona dueña de TEST lo autorizó
expresamente («sí, sandbox de licencia»). **Nunca en producción.**

**Cómo aplicarlo, comprobarlo y retirarlo** (el SQL del guion lo escribe `scripts/generar_semillas.py` entre
las marcas `SQL-GENERADO`; no se edita):

```bash
ssh root@161.97.85.216 'bash -s'              < despliegue/ensayos/sandbox_licencia_test.sh   # aplica (idempotente)
ssh root@161.97.85.216 'bash -s -- --estado'  < despliegue/ensayos/sandbox_licencia_test.sh   # solo lee
ssh root@161.97.85.216 'bash -s -- --retirar' < despliegue/ensayos/sandbox_licencia_test.sh   # lo da de baja
```

Idempotente (id estable, `ON CONFLICT (id) DO NOTHING`): la segunda corrida dice `cambios: 0`. Si el sandbox
se retiró o su ventana (90 días) venció, correrlo de nuevo lo reactiva. `--retirar` pasa la fila a
`FINALIZADO`: el alcance vuelve a `habilitado:false, via:NINGUNA` y la licencia no cambió. Se comprueba con
una sesión real: `GET /licencia/alcance?servicio=GRUPO_PASANAKU`.

**Guardas** (abortan sin escribir): (1) identidad, cumplimiento y grupos deben correr con el perfil `local`
y ninguno con un perfil de producción; (2) si la base trae `app.entorno`, no puede ser de producción; (3) si
ya existe una licencia `OTORGADA` no se mezcla un sandbox sintético con una real; (4) debe existir la
licencia sembrada de la que cuelga. Además vive en `seeders/dev/`: `sembrar_dev.sql` (que lo incluye) aborta
sin `app.entorno = 'dev'` y `minimos/` no lo contiene, así que producción no lo recibe.

## Invitar por ENLACE y abrir el grupo a inscripciones en TEST (carril P, 2026-10-11)

Cuatro defectos de código impedían que un invitado aceptara una invitación (hallados por el carril L3 contra TEST).
Los tres de código viajan en el despliegue; falta una acción en el VPS **que no ejecuta ningún agente**:

1. **Política de token versionada.** `canal=ENLACE` (el de la app) daba `500 AP-INT-01` porque la fila de
   `identidad.politica_token` de TEST no lo permitía y la siembra es `DO NOTHING`. La corrección es una **versión
   nueva** de la política (`seeders/minimos/13-politicas-de-token.json`, `vigente_desde` 2026-10-11), no un `UPDATE`.
   Las bases nuevas la reciben solas; **TEST ya poblada la recibe con**
   `ssh root@161.97.85.216 'bash -s' < despliegue/ensayos/politica_invitacion_test.sh` (idempotente; `--estado` solo lee;
   falla si la política vigente no permite ENLACE).
2. **Tópico nuevo en Kafka.** `tarifas` responde en `aportaya.tarifas.tarifa_congelada`; el broker de TEST no crea
   tópicos solo: correr `despliegue/host/crear-topicos.sh` (la lista sale del código) **antes o junto con** el despliegue.
3. **Apertura del grupo.** `tarifas` congela el precio al consumir `grupos.grupo_creado` y `grupos` pasa el grupo de
   `BORRADOR` a `ABIERTO_A_INSCRIPCION` al consumir la respuesta. Los consumidores nacen con `auto-offset-reset: earliest`
   (grupo de consumidores nuevo): al arrancar leen los `grupo_creado` que ya estaban y abren también los grupos creados
   antes (los de prueba de L3 incluidos). Interruptores, **encendidos** por omisión: `CONGELAMIENTO_CONSUMO_HABILITADO`
   (tarifas) y `APERTURA_CONSUMO_HABILITADO` (grupos).

**Decisiones adoptadas por delegación del dueño del proyecto (2026-10-11, «lo más recomendado»):**
- El grupo se abre **automáticamente** al confirmarse el congelamiento del precio, sin interruptor apagado: es lo que la app
  ya promete («en un momento se abre para inscripciones»). No espera la cuenta del grupo (`nucleo-financiero` todavía no la abre):
  **supuesto A1, a confirmar con producto y con el dueño de `nucleo-financiero`**.
- **Admisión humana:** producción debe fijar `ADMISION_HUMANA_OBLIGATORIA=true` (es además el valor por omisión de la aplicación;
  el compose de TEST la deja en `false` para conservar «aceptar por enlace»).
- **Sustitución automática del administrador:** `SUSTITUCION_AUTOMATICA_ADMIN=false` (manual) por omisión.

## El corpus de identidad

TEST se usa además para juntar un corpus de verificación de identidad: por cada persona
que se da de alta, las tres fotos —anverso, reverso y prueba de vida— y la decisión que
tomó una persona sobre ellas.

**Una carpeta por usuario.** Las fotos van a MinIO, al bucket de archivos, bajo:

```
identidad/<usuarioId>/anverso-<uuid>.jpg
identidad/<usuarioId>/reverso-<uuid>.jpg
identidad/<usuarioId>/selfie-<uuid>.jpg
```

La carpeta es el `usuarioId` —un UUID opaco, nunca un nombre ni un número de documento:
la ruta no lleva datos personales adentro (ADR-034)— y el nombre empieza por la cara, así
que el expediente de alguien se ve listando una carpeta. El UUID del final está porque los
objetos **no se sobrescriben**: sacarse la foto de nuevo agrega una al lado de la anterior,
y la última es la que apunta la fila. La carpeta de cada expediente se muestra en el
backoffice, arriba de las fotos.

**Todo lo decide una persona.** No hay proveedor biométrico, ni puntaje, ni regla que
apruebe sola: el alta abre la verificación en `EN_REVISION` y lo único que la resuelve es
`POST /identidad/verificaciones/{id}/decision`, que exige el permiso
`VERIFICACION_RESOLVER` y guarda en `revisada_por` quién decidió. Rechazar sin motivo no
se admite. Eso es exactamente lo que hace útil al corpus: cada expediente queda con una
etiqueta puesta por alguien con nombre.

Se trabaja en **Cumplimiento → Verificación de identidad** del backoffice. La pantalla abre
en la cola **Por decidir** —los dos estados sin resolver, `PENDIENTE` y `EN_REVISION`— y al
abrir un expediente muestra las tres fotos juntas, porque cotejar la cara contra el
documento es mirarlas a la vez. Los enlaces son temporales (diez minutos) y cada uno se
pide al abrir, no al listar: cada lectura de una cédula queda registrada.

Un expediente al que le falta una foto **no se puede aprobar**, y la pantalla dice cuál
falta. Pasa cuando la cámara del teléfono falla y el alta sigue por la salida manual.

## La imagen del esquema tiene que existir SIEMPRE (2026-09-17)

El compose pide `aportaya/esquema:test` con `pull_policy: never`, y **los quince
servicios dependen de ella** con `condition: service_completed_successfully`. Si el tag
no está en el host, Coolify para los contenedores viejos, no puede crear los nuevos
—`No such image: aportaya/esquema:test`— y **el backend queda en cero**. Los tres
fronts siguen arriba, así que desde afuera parece que solo «no carga nada».

`reconstruir-si-cambio.sh` solo la rehacía cuando el commit tocaba `sql/`,
`despliegue/aplicar-esquema.sh` o `despliegue/Dockerfile.esquema`. Con el tag perdido
—una poda, un build que lo deja colgando— ningún commit la reconstruía, así que el
despliegue fallaba para siempre y el motivo no estaba a la vista: hay que ir a leer el
log del despliegue fallido en la base de Coolify.

Medido el 2026-09-17: pasó de verdad y dejó TEST sin backend media hora.

**El arreglo**, en `/opt/aportaya/bin/reconstruir-si-cambio.sh` (el guión vive en el
VPS, no en el repositorio) — reemplazar:

```bash
echo "$CAMBIOS" | grep -qE '^(sql/|despliegue/aplicar-esquema.sh|despliegue/Dockerfile.esquema)' && \
  construir docker build -f despliegue/Dockerfile.esquema -t aportaya/esquema:test .
```

por:

```bash
if echo "$CAMBIOS" | grep -qE '^(sql/|despliegue/aplicar-esquema.sh|despliegue/Dockerfile.esquema)' \
   || ! docker image inspect aportaya/esquema:test >/dev/null 2>&1; then
  construir docker build -f despliegue/Dockerfile.esquema -t aportaya/esquema:test .
fi
```

Y para levantarlo a mano cuando ya pasó:

```bash
cd /opt/aportaya/repo && docker build -f despliegue/Dockerfile.esquema -t aportaya/esquema:test .
/opt/aportaya/bin/disparar-despliegue.sh
```

**Cómo se ve el síntoma.** `docker ps` muestra solo `backoffice-*`, `web-*` y `movil-*`,
y ningún contenedor con el uuid de la aplicación del backend. El motivo está en el log
del despliegue, no en los logs de Docker:

```bash
docker exec coolify php artisan tinker --execute='$d=\App\Models\ApplicationDeploymentQueue::where("application_id",13)->orderByDesc("id")->first(); echo $d->status;'
```

> Ojo con `disparar-despliegue.sh`: su salida pasa por `tail -2`, así que el registro
> solo muestra las dos últimas aplicaciones encoladas. Que `aportaya-api` no aparezca
> en el log **no** significa que no se encoló.

## La API en 504 con todo sano: el enrutado quedó viejo (2026-09-18)

Síntoma engañoso: los quince contenedores `healthy`, el backoffice y el sitio en 200, y
**toda** ruta de `https://api.aportaya…` devolviendo `504` a los 31 segundos. Parece el
backend caído y no lo está: desde adentro contesta en milésimas.

```bash
G=$(docker ps --format '{{.Names}}' | grep '^gateway-')
docker exec "$G" curl -s -o /dev/null -w '%{http_code} %{time_total}s\n' \
  http://localhost:8080/api/v1/sesiones        # 401 en 0,2 s
```

Si el gateway contesta por dentro y el borde no, **lo que quedó viejo es el enrutado**:
Traefik sigue apuntando a un extremo que ya no existe. Pasó después de un despliegue
fallido —el de la imagen del esquema, acá arriba— que destruyó los contenedores y los
recreó a mano: quedó un estado intermedio en el que el proxy no alcanzaba la IP del
contenedor nuevo en la red del compose.

**El arreglo es un despliegue limpio**, que es lo que lo resolvió de verdad:

```bash
/opt/aportaya/bin/disparar-despliegue.sh
```

Como diagnóstico sirve comparar, desde el proxy, las **dos IP del mismo contenedor** —la
de la red del compose y la de la red `coolify`—:

```bash
docker inspect $(docker ps --format '{{.Names}}' | grep '^gateway-') \
  --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}={{$v.IPAddress}}{{println}}{{end}}'
docker exec coolify-proxy wget -qO- --timeout=8 http://<ip-del-compose>:8080/api/v1/sesiones
docker exec coolify-proxy wget -qO- --timeout=8 http://<ip-coolify>:8080/api/v1/sesiones
```

Que una agote el tiempo y la otra conteste 401 confirma que el backend está bien y que el
problema es por dónde lo busca el proxy. Cuando el enrutado está sano, **las dos
contestan**.

> **No es** que a `coolify-proxy` le falte la red del backend. Está en ella —y en las de
> los otros ~38 recursos— y Coolify la conecta sola en cada despliegue. Comprobarlo con
> `grep` pide cuidado: el nombre de la red es el uuid del recurso seguido de dígitos, así
> que un patrón como `^n3wymuuo ` (con espacio) no calza nunca y hace leer «no está»
> donde dice que sí:
>
> ```bash
> docker inspect coolify-proxy \
>   --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}={{$v.IPAddress}}{{println}}{{end}}' \
>   | grep -i n3wymuuo
> ```

## El esquema sólo se crea; la nulabilidad ahora converge (2026-09-18)

`sql/aplicar.sql` levanta las tablas con `CREATE TABLE IF NOT EXISTS`. Sobre una base
**ya creada** —como la de TEST— eso significa que un cambio en el `.puml` no llega
nunca: el esquema se aplica entero, no pasa nada, y el modelo y la base quedan diciendo
cosas distintas hasta que un `INSERT` falla.

Desde ahora, `sql/15_infra/nulabilidad.sql` —generado— corre en cada aplicación y
**afloja** el `NOT NULL` de toda columna que el modelo declara opcional. Sobre una base
recién creada no hace nada.

**No es un sistema de migraciones.** Hace una sola cosa y en una sola dirección. No
agrega columnas, no cambia tipos, no aprieta nada y no borra nada, así que no puede
perder datos ni fallar por datos existentes. Todo lo demás —una columna nueva, un tipo
distinto, un `NOT NULL` que se quiere imponer— sigue necesitando decidirse a mano, y con
datos de por medio eso es un trabajo aparte.

Probado sobre PostgreSQL 16: base limpia → esquema → prueba de humo **166 OK, 0 FALLA**;
después se le devolvió el `NOT NULL` a mano para imitar a TEST, se reaplicó el esquema y
las columnas volvieron a admitir nulos.

> **En producción va en ventana.** `aplicar.sql` corre todo en una transacción y cada
> `ALTER` toma un `ACCESS EXCLUSIVE` sobre su tabla. Son cambios de catálogo —no
> reescriben la tabla, son instantáneos— pero mientras dure la transacción nadie más
> toca esas tablas.

## Secretos que hay que cargar en Coolify antes de subir (2026-10-10)

Solo NOMBRES; los valores no viven en el repositorio. Cada uno se genera una vez, en una
terminal propia, y se pega en las variables de la aplicacion `aportaya-api`:

```bash
openssl rand -hex 32     # 64 caracteres hex; el servicio exige al menos 32
```

| Variable | La lee | Sin ella | Como se ve el fallo |
| --- | --- | --- | --- |
| `QR_INTERNO_CLAVE` | `nucleo-financiero` | **no arranca** (`ContenidoDeQr`: «necesita al menos 32 bytes», bean `qrInterno`) | contenedor en `restarting`; la ola 3 del compose no queda sana y **las olas siguientes (publicidad, tarifas, transparencia) nunca arrancan**; las rutas `/api/v1/billetera`, `/contabilidad`, `/custodia` dan 503 |
| `INVITACIONES_CLAVE` | `identidad` | arranca (UP) pero **toda invitacion falla en runtime** con 500: `SecretoDeInvitacion.firmar` lanza `IllegalStateException` si la clave tiene menos de 32 caracteres | readiness verde: el problema solo aparece al emitir/validar/consumir una invitacion |

Ninguna de las dos tiene valor por omision a proposito (regla 90.3.4). **`INVITACIONES_CLAVE`
no puede rotarse a la ligera**: las invitaciones ya emitidas se firman con esa clave y dejarian
de validar.

Siguen siendo las de antes y ya deben estar cargadas: `BD_CLAVE`, `BD_USUARIO_ADMIN`,
`BD_CLAVE_ADMIN`, `ARCHIVOS_USUARIO`, `ARCHIVOS_CLAVE`, `SEGURIDAD_PIMIENTA`,
`WEBHOOK_SECRETO`, `CERTIFICADOS_CLAVE_FIRMA`, `CUENTA_PUENTE_CUSTODIA`, `BASE_URL_PUBLICA`,
`SIN_NIT_EMISOR`, `JWT_CLAVE_FIRMA`, `PERFIL_SPRING`, `WEB_HOSTS_PERMITIDOS`, `URL_APP`, y
las `GMAIL_*` / `NOTIFICATION_EMAIL_PROVIDER`.

`inversiones` no suma ningun secreto: su aliado queda `deshabilitado` (`INVERSIONES_ALIADO_*`
y `INVERSIONES_LIBRO_CREDENCIAL_SERVICIO` van fijos y vacios en el compose; el servicio los
declara con valor por omision). El unico modo que acepta un perfil productivo es
`deshabilitado` (`GuardiaDeProduccion`).

**`APORTAYA_CORS_ORIGENES`** (nueva en el compose, gateway y servicios): solo la leen los perfiles
`staging`/`production`; con `local` no tiene efecto. Hoy no hay que cargarla. Pero **TEST no puede pasar a
`production` todavia**: `nucleo-financiero` no arranca (no existe un `ProveedorDeRetiro` fuera de `local`/`test`,
`ProveedorDeRetiroLocal.java:38`; `evidencia/carril-D3/FASE2.md`). Se queda en `PERFIL_SPRING=local`.

**Credencial de `svc_inversiones`.** El rol nace `NOLOGIN` y el servicio no puede conectarse
hasta que tenga clave. El compose ahora le pasa al trabajo de esquema `ROLES_CLAVE: ${BD_CLAVE}`
y `despliegue/aplicar-esquema.sh` da esa clave **solo** a los `svc_*` que no tienen login o no
tienen clave (no rota los que ya la tienen). No hay variable nueva que cargar.

## El esquema ahora expande antes de aplicar (2026-10-10)

El 503 del intento anterior tuvo una causa mas profunda que las claves: **el trabajo de esquema
fallaba sobre la base de TEST** y, por `service_completed_successfully`, no arrancaba ningun
servicio (`evidencia/carril-D3/FASE1.md`):

```
psql:/sql/10_tablas/02_grupos_turnos/invitacion.sql:34: ERROR:  column "consumidor_id" of relation "grupos.invitacion" does not exist
esquema exited with code 3
```

`CREATE TABLE IF NOT EXISTS` no agrega a una tabla existente las columnas que el modelo sumo, y
el primer `COMMENT ON COLUMN` que las nombra aborta toda la transaccion. Ademas los `CHECK` en
linea no se refrescan (la semilla de la cuenta `CUSTODIA_INVERSION` violaba el `CHECK` viejo).

`aplicar-esquema.sh` ahora **expande** antes de aplicar: aplica `aplicar.sql` en una base de
referencia descartable, y por cada columna que le falta a una tabla real emite
`ADD COLUMN`, y por cada `CHECK` cuya definicion cambio lo reemplaza. Es derivado y aditivo (ver
el comentario del script); sobre una base vacia no hace nada; sin permiso para `CREATE DATABASE`
avisa y sigue como antes. Una columna `NOT NULL` sin `DEFAULT` entra opcional.

Medido contra una base con los datos de `59370bbd` (402 tablas, 11 usuarios, 25 movimientos):
esquema `exit 0`, 436 tablas, **0 tablas pierden filas**, y una segunda corrida no agrega nada.

La causa de fondo esta en el generador (`scripts/generar_ddl.py`, regla 97.1): lo correcto es que
emita el `ADD COLUMN IF NOT EXISTS` desde el modelo. Mientras no lo haga, esta expansion es la red.

## Procedimiento de subida segura a TEST, por etapas

Orden y verificacion de cada etapa. Si una etapa falla, **no se sigue**.

**Etapa 0 · antes de tocar nada (local).**
`bash despliegue/ensayos/arranque_completo.sh` con `D3_ESCENARIO=actualizacion` (aplica `sql/`
de lo que corre hoy en TEST y despues el actual, levanta los servicios y corre el humo). Debe
terminar con `RESUMEN humo: 0 falla(s)`. Un `COMMIT` anterior distinto del de TEST se fija con
`D3_COMMIT_ANTERIOR=<sha>`.

**Etapa 1 · secretos.** Cargar `QR_INTERNO_CLAVE` e `INVITACIONES_CLAVE` en Coolify
(arriba). Verificar que **ambas** existen y tienen 64 caracteres, sin mostrarlas.

**Etapa 2 · respaldo.** `pg_dump -Fc pasanaku > /opt/aportaya/respaldos/pre-<sha>.dump` en el
VPS y `pg_restore --list` sobre el archivo (un respaldo que no se lee no es respaldo, regla
97.3.6). Anotar el sha desplegado hoy: es el destino del revert.

**Etapa 3 · imagen del esquema y del gateway.** Construir `aportaya/esquema:test` y
`aportaya/gateway:test` (el timer lo hace; o `docker build -f despliegue/Dockerfile.esquema`).
Verificar `docker image inspect aportaya/esquema:test`. Correr el trabajo de esquema **solo**, a mano,
contra la base real y leer su salida: debe terminar `esquema: listo`, con la lista de columnas
agregadas y `roles svc_* con credencial: 15 de 15`.

```bash
docker run --rm --network aportaya-interna -e PGHOST=postgres -e PGUSER="$BD_USUARIO_ADMIN"   -e PGPASSWORD="$BD_CLAVE_ADMIN" -e PGDATABASE=pasanaku -e ROLES_CLAVE="$BD_CLAVE" aportaya/esquema:test
```

Como es aditivo, **las imagenes viejas siguen funcionando contra el esquema nuevo** (columnas nuevas
opcionales o con default, tablas nuevas ignoradas). Esta es la etapa «expandir» de la regla 97.3.5.

**Etapa 4 · servicios.** Reconstruir las quince imagenes (de a una; el timer) y desplegar. Orden
que impone el compose: `esquema` -> `identidad` -> olas de a cuatro. Verificar:

```bash
docker ps --format '{{.Names}} {{.Status}}' | grep -v healthy   # nada debe quedar unhealthy/restarting
for s in identidad nucleo-financiero inversiones; do
  docker exec "$(docker ps --format '{{.Names}}' | grep "^$s-")"     wget -qO- http://127.0.0.1:8080/actuator/health/readiness; echo; done
```

**Etapa 5 · humo por el borde.** `GET https://api.aportaya…/api/v1/sesiones` sin token debe dar 401
(no 503/504); `GET /api/v1/inversiones/productos` sin token, 401; un login con el operador de plataforma.
Es lo que hace `despliegue/ensayos/arranque/humo.sh` apuntando al gateway local.

**Marcha atras, sin perdida de datos.** El esquema solo se expandio: no hay nada que deshacer en
la base. Se revierte el codigo con un **revert-commit** sobre `test` (el mismo mecanismo que ya se
uso: `git revert <sha-de-la-subida>`, push, y el timer reconstruye y despliega el estado anterior).
Si ademas fallo el trabajo de esquema, no se tocó la base (la transaccion es una sola). El
respaldo de la etapa 2 solo se restaura si hubo corrupcion, no para volver atras.

Dos cosas que un revert **no** deshace, a saber: (1) las columnas y tablas agregadas siguen ahi
—inofensivas para el codigo viejo—, y (2) las restricciones `CHECK` ya ensanchadas.

## Lo que este entorno NO es

No es producción ni se le parece: una réplica por servicio, sin respaldo
automático de la base y con el segundo factor de desarrollo encendido.
