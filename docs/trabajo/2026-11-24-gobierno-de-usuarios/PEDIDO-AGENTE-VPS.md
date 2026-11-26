# Pedido para el agente local con acceso al VPS (TEST, Contabo)

Fecha: 2026-11-26. Contexto: `dev` ya tiene los lotes A–H del gobierno de usuarios (C, F y G
entraron hoy: #522, #520, #525, más el arreglo de CI #530). La promoción `dev → test` es el
PR #531. Cuando se mergee, el temporizador `aportaya-autodespliegue.timer` reconstruye y
despliega solo (ver `despliegue/TEST.md`). Desde la nube no hay SSH al VPS; todo lo de abajo
hay que hacerlo desde la máquina local de Pablo.

## Reglas para quien ejecute esto

- **Nada de secretos ni datos de personas en lo que devuelvas.** Ni contraseñas, ni
  `MFA_LLAVES`, ni semillas TOTP, ni contenido de filas. Solo estados, conteos y nombres de objetos.
- Cada paso marcado SOLO LECTURA no cambia nada. Los marcados CAMBIA los ejecutás solo si el
  paso anterior dio lo esperado, y copiás la salida literal (recortada, sin secretos).
- Si algo no coincide con lo esperado, **parate y reportá**; no improvises arreglos ni toques
  la base a mano (regla 97.1).
- No toques la rama `main` ni reinicies PostgreSQL, PgBouncer, MinIO, Redis ni Kafka.

## Paso 0 — Foto inicial (SOLO LECTURA)

1. `systemctl status aportaya-autodespliegue.timer` y `systemctl list-timers | grep aportaya`.
2. `tail -n 40 /opt/aportaya/logs/autodespliegue.log`.
3. `docker ps --format '{{.Names}}\t{{.Status}}'` (los catorce servicios, el gateway y los fronts).
4. `df -h /` y `free -h` (la máquina es de 23 GB y comparte con otro proyecto).
5. El comando `estado` que Pablo usa en TEST, si existe: pegá su salida con secretos enmascarados.

**Devolver:** las salidas de arriba.

## Paso 1 — El tema de Kafka de revocaciones (ANTES de que test avance)

El broker tiene la creación automática de temas apagada. Sin este tema, `identidad` queda con
su readiness `revocacion` en DOWN y rechaza con 401 los tokens que traen `sesion_id`.

1. SOLO LECTURA: leé `despliegue/host/crear-topicos.sh` del repo (rama `dev`) y confirmá que es
   idempotente (segunda corrida no falla ni duplica).
2. CAMBIA: ejecutalo en el VPS.
3. SOLO LECTURA: describí el tema `aportaya.identidad.revocacion` y confirmá
   `cleanup.policy=compact,delete`.

**Devolver:** salida de la descripción del tema (particiones, réplicas, `cleanup.policy`).

## Paso 2 — Que `test` avance y se despliegue (lo dispara el temporizador)

1. Avisale a Pablo que el PR #531 está listo para mergear (o que ya se mergeó).
2. Una vez avanzada la rama `test`, seguí `tail -f /opt/aportaya/logs/autodespliegue.log` hasta
   que diga que reconstruyó y disparó el despliegue en Coolify. Si no arranca en ~10 minutos,
   revisá el temporizador (Paso 0.1) y reportá; no lances builds en paralelo.
3. Un cambio en `plataforma/` rehace los quince servicios **de a uno**: puede tardar mucho.
   Vigilá `uptime`/`free -h`; si la carga se dispara, reportá antes de que llegue al OOM.

**Devolver:** hora de inicio y de fin del autodespliegue y el commit desplegado
(`git -C <repo> rev-parse --short HEAD`).

## Paso 3 — Esquema y semillas aplicados (SOLO LECTURA)

Los lotes añadieron tablas y semillas en `identidad` (por ejemplo `politica_medida_cuenta` del
lote G). Tras el despliegue:

1. Confirmá que el trabajo de esquema (la imagen del esquema) terminó sin error: su log y estado.
2. Listá en la base `pasanaku` las tablas del esquema de identidad cuyo nombre contenga
   `medida`, `invitacion`, `revocacion` o `rol`, y devolvé solo nombres y **conteos de filas**.
3. Verificá que la marca `app.entorno = 'dev'` **sigue sin estar puesta** (las semillas de
   desarrollo no deben entrar en TEST).
4. Si algún objeto esperado no existe, reportá cuál. No lo crees a mano.

**Devolver:** lista de tablas con conteos y el resultado de la comprobación de la marca.

## Paso 4 — Salud tras el despliegue (SOLO LECTURA)

1. `docker ps`: ¿los catorce servicios y los fronts están `healthy`/`Up`?
2. Readiness de `identidad` desde dentro del host (sin exponer tokens): devolvé solo el estado
   de cada componente, en especial `revocacion` (debe ser UP).
3. Los cuatro dominios responden HTTPS 200/redirección correcta:
   `aportaya`, `backoffice.aportaya`, `app.aportaya`, `api.aportaya` en `.161.97.85.216.sslip.io`.
   Devolvé solo el código HTTP de cada uno.
4. Últimas 50 líneas de log de `identidad`, `grupos` y el gateway **filtradas a ERROR/WARN**,
   con cualquier dato personal enmascarado.

**Devolver:** tabla servicio → estado, y los errores si los hay.

## Paso 5 — Lo que NO hagas (lo hace Pablo)

- Iniciar sesión como operador (pide TOTP real).
- Probar la app en un dispositivo real.
- Crear el keystore de Android (`docs/operacion/firma-android-release.md`).

## Formato de la respuesta

Un solo mensaje con: Paso 0 a 4, cada uno con **PASS / FAIL / BLOQUEADO**, la salida recortada y,
al final, una línea **"No cubierto"** con lo que no pudiste comprobar.
