# Integración de origin/dev — generados, esquema y docs

## Esquema (fuente: docs/entidades/*.puml)
`01_identidad_usuarios.puml` auto-fusionó las columnas `url_perfil_izquierdo/derecho` de dev. `PYTHONUTF8=1 python scripts/generar_ddl.py` y `generar_semillas.py` regeneran sql/ sin diferencias contra lo fusionado (337 tablas, 1500 filas de semilla). `seeders/minimos/13-politicas-de-token.json` trae `REFRESCO_SESION` de dev junto a lo nuestro.

## Compose
`python scripts/generar_compose.py` y `--coolify`: el generador de dev agrega límites de CPU/RAM y pool Hikari a cada servicio; el servicio `inversiones` (nuestro, nuevo) no los tenía en el yml fusionado a mano. Se regeneró `docker-compose.coolify.yml` (sólo cambia `inversiones`).

## Clientes Dart (9 conflictos en clientes/dart/*)
Archivos generados: se tomó "ours" provisoriamente y se regeneran con `./gradlew generarClientes` (commit aparte, ver verificacion.md). Nunca se editan a mano.

## Postman (identidad.* en conflicto + el resto desfasado)
Generados por `scripts/generar_postman.py`. Se resolvió con "ours" y se regeneró todo (las colecciones de nuestra rama estaban atrasadas respecto de sus propios contratos).

## docs/CasosDeUso/CU-01
Se conservan los dos escenarios nuevos de nuestra rama (verificación de correo con código incorrecto/correcto/reutilizado) y los dos de dev (fecha de vencimiento futura se guarda; documento vencido da AP-CU01-07).
