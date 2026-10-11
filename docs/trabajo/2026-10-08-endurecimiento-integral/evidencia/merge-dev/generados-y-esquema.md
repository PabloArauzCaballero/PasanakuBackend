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

## Verificadores (scripts/) que se desfasaron por la integración
- `scripts/auditoria/verificar_limites_compose.py` (dev) fijaba «16 servicios» en el compose de Coolify: con `inversiones` (nuestro, nuevo)
  son 17. La cuenta pasa a salir de los `descriptor.yml` más el job de esquema y el gateway; el resto del verificador (límites, una réplica,
  Hikari) no se toca.
- `scripts/verificar_rutas_frontend.py`: `POST /sesion/refrescar` figuraba como «conocida sin contrato»; dev le dio operación en
  `identidad.yaml`. El verificador exige sacarla de la lista (se pudrió), y se saca.

## clientes/angular/identidad.ts (doble temporal de 2026-09-21)
Archivo versionado a pesar de que `clientes/angular/` es generado. Declara a mano un subconjunto del cliente y, por resolución de módulos,
SOMBREA al cliente real (`clientes/angular/identidad/index.ts`): el código de dev (`CaraDelExpediente`, `fechaExpiracionDocumento`,
`documentoVigente`) no tipa contra él (`tsc` del backoffice: 5 errores, que ya estaban en dev y no los introduce el merge). Con el
cliente real generado `tsc` de app y spec del backoffice pasa limpio, y el CI ya exige que `identidad/index.ts` exista. Se elimina el doble
(como pedía su propio encabezado: «se borra cuando se publique el cliente real»). Quien trabaje en local corre `./gradlew generarClientes`.
