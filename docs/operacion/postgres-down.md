# Runbook — PostgreSQL caído o inalcanzable

## Síntomas

- Los 14 servicios devuelven `503`/`500` en `/actuator/health/readiness`, con
  causa `Connection refused` o `Connection to pgbouncer:6432 refused` en el
  log.
- `aportaya-postgres` o `aportaya-pgbouncer` no aparecen `Healthy` en
  `docker ps` (dev) o en el dashboard de pods (prod).
- Picos de `pg_locks` o de conexiones sin liberar antes de la caída (revisar
  `carga/k6/README.md` §baseline para la consulta).

## Dashboards y consultas

```bash
# Estado del contenedor (dev)
docker ps --filter "name=aportaya-postgres" --format "{{.Names}}: {{.Status}}"

# Log de los últimos minutos
docker logs --since 10m aportaya-postgres

# Conexiones activas, si el motor responde
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c \
  "SELECT count(*), state FROM pg_stat_activity GROUP BY state;"
```

En producción: el panel de Coolify/el proveedor gestionado (`PITR`, ver
`backup-recovery.md`) reporta CPU, IOPS y conexiones — es la primera parada
antes de tocar nada.

## Diagnóstico

1. **¿El contenedor/instancia existe y corre?** `docker ps -a` (dev) — si
   salió, `docker logs aportaya-postgres` dice por qué (OOM, `FATAL`, disco
   lleno).
2. **¿Corre pero no acepta conexiones?** `pg_isready` desde dentro de la red
   interna (`docker exec aportaya-postgres pg_isready -U pasanaku`). Un `no
   response` con el proceso vivo suele ser saturación de `max_connections`
   (`despliegue/compose/base.yml:36`, hoy 300) o un `VACUUM`/`autovacuum`
   bloqueante.
3. **¿Es PgBouncer el que no responde, no Postgres?** Probar directo contra
   Postgres (`docker exec aportaya-postgres psql ...`) y contra PgBouncer
   (`docker exec aportaya-pgbouncer psql -h 127.0.0.1 -p 6432 ...`) por
   separado — aíslan cuál de los dos falló.
4. **¿Disco lleno?** `docker exec aportaya-postgres df -h /var/lib/postgresql/data`.

## Acciones seguras

- Reiniciar PgBouncer primero (más barato, no toca datos):
  `docker restart aportaya-pgbouncer`.
- Si Postgres está vivo pero saturado de conexiones colgadas:
  `SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE state = 'idle in transaction' AND state_change < now() - interval '5 minutes';`
  — mata solo transacciones abandonadas, nunca las activas.
- Si el contenedor murió por OOM: revisar `docker stats` de los vecinos
  (regla 70, máquina compartida) antes de asumir que Postgres es el
  problema.

## Recuperación

- **Contenedor caído, datos intactos (volumen `aportaya-datos-pg` sano):**
  `docker compose -f despliegue/compose/base.yml --profile base up -d postgres pgbouncer`
  (con `APORTAYA_PG_PORT` si esta máquina lo necesita — ver `baseline.md`).
- **Volumen corrupto o disco perdido:** restaurar desde el último backup —
  ver `backup-recovery.md`, sección "Restore". No se reconstruye el esquema
  a mano; `bd:aplicar` es idempotente pero NO trae los datos de vuelta.

## Validación

```bash
docker exec aportaya-postgres pg_isready -U pasanaku -d pasanaku
python3 scripts/generar_ddl.py && git diff --exit-code -- sql/   # boveda no divergio
docker exec -i aportaya-postgres psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku \
  -f /repo/sql/50_verificacion/verificaciones.sql   # 0 FALLA
```

Y que al menos un servicio real vuelva a `readiness` en verde antes de dar
el incidente por cerrado.

## Escalamiento

- Si el volumen se perdió y el backup más reciente tiene más de RPO
  (`DECISION_REQUIRED`, ver `backup-recovery.md`): escalar a quien tenga la
  decisión de negocio sobre pérdida de datos aceptable — no es una decisión
  técnica.
- Si la causa es un ataque (conexiones desde una IP no reconocida, patrón de
  `DROP`/`DELETE` fuera de lo esperado): escalar como incidente de
  seguridad, no como caída de infraestructura — congelar accesos antes de
  seguir el resto de este runbook.

## Ver también

[[backup-recovery]] · [[kafka-down]] · [[branch-protection]]
