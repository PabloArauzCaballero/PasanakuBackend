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

## Lo que este entorno NO es

No es producción ni se le parece: una réplica por servicio, sin respaldo
automático de la base y con el segundo factor de desarrollo encendido.
