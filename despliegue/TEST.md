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

**Fuera de Coolify**, en `/opt/aportaya/` del VPS: PostgreSQL, PgBouncer, MinIO y Kafka.
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

## La API en 504 con todo sano: el proxy no está en la red del backend (2026-09-18)

Síntoma engañoso: los quince contenedores `healthy`, el backoffice y el sitio en 200, y
**toda** ruta de `https://api.aportaya…` devolviendo `504` a los 31 segundos. Parece el
backend caído y no lo está.

Coolify le pone al gateway la etiqueta `traefik.docker.network =
n3wymuuo076w9prwo5i293bx` —la red del compose— pero **`coolify-proxy` solo está en la red
`coolify`**. Traefik entonces marca hacia la IP de esa red (10.0.26.x), nadie contesta, y
a los 30 s corta con 504.

Cómo se prueba en diez segundos, desde el propio proxy:

```bash
docker exec coolify-proxy wget -qO- --timeout=8 http://10.0.26.4:8080/api/v1/sesiones   # agota el tiempo
docker exec coolify-proxy wget -qO- --timeout=8 http://10.0.1.31:8080/api/v1/sesiones   # 401 al instante
```

Las dos IP son del **mismo** contenedor: la primera es su red de compose y la segunda la
red `coolify`. Que la segunda conteste es la prueba de que el backend está bien y de que
el problema es por dónde lo busca Traefik.

**El arreglo** —aditivo, sin reiniciar nada:

```bash
docker network connect n3wymuuo076w9prwo5i293bx coolify-proxy
```

Si Traefik no lo toma solo con el evento de Docker: `docker restart coolify-proxy`.

Hay que rehacerlo si la red del compose se recrea con otro nombre (cambia con el uuid del
recurso, no con el despliegue).

## Lo que este entorno NO es

No es producción ni se le parece: una réplica por servicio, sin respaldo
automático de la base y con el segundo factor de desarrollo encendido.
