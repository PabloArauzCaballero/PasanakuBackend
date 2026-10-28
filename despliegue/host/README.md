# Guiones del host de TEST

Los guiones que orquestan TEST viven en `/opt/aportaya/bin/` del VPS Contabo
(`161.97.85.216`) y **no estaban versionados en ningun lado**. El 2026-09-21 las tres
fallas del dia estaban en uno de ellos y nadie podia verlas en una revision: el arreglo
del guard del esquema llevaba cuatro dias escrito en `TEST.md` sin que nadie notara que
jamas se habia aplicado al archivo real.

Aca se versiona lo que se pudo auditar linea por linea. Los demas (`construir.sh`,
`construir-todo.sh`, `construir-fronts.sh`, `construir-movil-web.sh`,
`disparar-despliegue.sh`, `desplegar-una.sh`, `generar-clientes.sh`) siguen solo en el
host: pasan credenciales de base por `--build-arg` y no entran a un repositorio sin
separarlas antes. **Eso queda pendiente.**

## Avisa cuando la copia del host quedo vieja (desde 2026-10-28)

`reconstruir-si-cambio.sh` **no se instala solo**: corre como root y `test` no exige
revision, asi que instalarse desde la rama seria una consola root para cualquiera que
empuje ahi. Al final de cada corrida compara su copia con la del repo y, si difieren,
deja `AVISO: el guion del host difiere…` en `/opt/aportaya/logs/autodespliegue.log`.
Ademas etiqueta las imagenes de backoffice y web con `aportaya.commit=<sha>` y no despliega
si la etiqueta no es la del commit.

Ver que esta al dia:

```bash
ssh root@161.97.85.216 'grep -c actualizar_guion /opt/aportaya/bin/reconstruir-si-cambio.sh; \
  docker image inspect -f "{{index .Config.Labels \"aportaya.commit\"}}" aportaya/backoffice:test aportaya/web:test'
```

## Instalar

```bash
scp despliegue/host/reconstruir-si-cambio.sh root@161.97.85.216:/tmp/
ssh root@161.97.85.216 'install -m 755 /tmp/reconstruir-si-cambio.sh /opt/aportaya/bin/'
```

No hace falta reiniciar nada: `aportaya-autodespliegue.timer` lo invoca cada dos minutos
y toma la version nueva en la corrida siguiente.

## Que cambia respecto de lo que corre hoy en el host

| # | Antes | Ahora |
| --- | --- | --- |
| 1 | El esquema se reconstruia solo si el commit tocaba `sql/` | Tambien si la imagen no esta en el host (`docker image inspect`) |
| 2 | El movil se construia con `docker build` pelado | Va por `construir-movil-web.sh`, que regenera `clientes/dart` primero |
| 3 | Una falla marcaba el log y **desplegaba igual** | Con una falla no se despliega, y el log dice cuales fueron |
| 4 | Los fronts no conocian `packages/tutoriales` | Entra en el disparador, junto con `ui` y `tokens` |
| 5 | Un cambio de contrato no tocaba el movil | Un cambio en `servicios/*/openapi/` lo reconstruye |

Los cinco salen de fallas medidas, no de prolijidad. El detalle de cada una esta en
`despliegue/TEST.md` y en los comentarios del propio guion.

## Lo que sigue sin estar cubierto

- Los otros nueve guiones del host, por lo de las credenciales.
- `clientes/dart` y `clientes/angular` no se versionan (ADR-016) y se generan en el host:
  si el VPS se pierde, se regeneran, pero nada avisa cuando quedan viejos salvo que el
  build falle.
- Nada vigila que la imagen desplegada corresponda al commit desplegado. Hoy se compara
  a mano: `docker images --filter reference=aportaya/*:test` contra la hora del despliegue.
