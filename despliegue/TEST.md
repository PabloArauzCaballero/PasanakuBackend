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

## Lo que este entorno NO es

No es producción ni se le parece: una réplica por servicio, sin respaldo
automático de la base y con el segundo factor de desarrollo encendido.
