# Runbook — Backup y restore

## Síntomas (cuándo se usa este runbook)

- Pérdida o corrupción confirmada del volumen de Postgres (ver
  [[postgres-down]] primero — si el contenedor solo está caído, sin datos
  perdidos, ese runbook alcanza).
- Necesidad de un punto de restauración para investigar un incidente sin
  tocar producción.
- Ejercicio de restore programado (verificar que el backup sirve, no solo
  que existe — un backup nunca probado no es un backup).

## Qué se respalda

| Qué | Mecanismo | Dónde |
|---|---|---|
| Datos de Postgres | `pg_dump` (formato *custom*, `-F c`) hoy; PITR (base + WAL continuo) es la decisión tomada (AMB-8) para producción, **sin implementar todavía en este repo** | `DECISION_REQUIRED` — proveedor gestionado |
| Objetos de MinIO (fotos, comprobantes) | **Sin mecanismo de backup propio todavía** — el bucket tiene versionado activado (`despliegue/compose/base.yml`, `mc version enable`), que protege contra sobrescritura/borrado accidental, pero versionado no es backup fuera del volumen | `DECISION_REQUIRED` |
| Kafka | No necesita backup propio: la fuente de verdad de cada evento es la tabla `evento_dominio` (patrón outbox — ver [[outbox-backlog]]), Kafka es el canal, no el registro | — |
| Secretos | Ver [[secret-rotation]] — no se respaldan junto con los datos | — |

## Backup — cómo se hace hoy (dev/local)

```bash
docker exec aportaya-postgres pg_dump -U pasanaku -d pasanaku -F c -f /tmp/respaldo.dump
docker cp aportaya-postgres:/tmp/respaldo.dump ./respaldo-$(date +%Y%m%d-%H%M).dump
```

**Producción:** depende del backup gestionado del proveedor (Coolify u
otro) — este repo no tiene un cron ni un job propio de backup todavía.
`DECISION_REQUIRED`: quién opera esa infraestructura confirma la cadencia
real.

## Restore — probado de verdad, no solo documentado

```bash
# 1. Base de destino, NUNCA sobre la base viva sin confirmar primero que es
#    el destino correcto — un restore sobre la base equivocada es
#    irreversible.
docker exec aportaya-postgres psql -U pasanaku -d pasanaku -c "CREATE DATABASE <destino>;"

# 2. Restore
docker exec aportaya-postgres pg_restore -U pasanaku -d <destino> --no-owner --role=pasanaku /tmp/respaldo.dump

# 3. Verificación (ver "Validación" abajo)
```

Ejecutado el 2026-09-21 contra una base de desarrollo real (401 tablas, sin
datos de negocio todavía — sin semillas corridas): restore exitoso, 401/401
tablas, mismo tamaño (±1 MB, por overhead de índices reconstruidos).
Evidencia completa: [evidencia/H5-restore.txt](../auditoria-produccion/evidencia/H5-restore.txt).

## Validación

```bash
# Conteo de tablas, origen vs restaurado — tienen que coincidir
psql -d <origen> -c "SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema');"
psql -d <destino> -c "SELECT count(*) FROM information_schema.tables WHERE table_schema NOT IN ('pg_catalog','information_schema');"

# La bóveda no diverge contra el restaurado
python3 scripts/generar_ddl.py && git diff --exit-code -- sql/
```

## RPO / RTO

**`DECISION_REQUIRED`** (AMB-8, decidido 2026-09-21): se implementa PITR y
el RPO/RTO se **miden** en el restore real, no se prometen de antemano. Lo
medido hoy (`evidencia/H5-restore.txt`) es el RTO de un restore **manual**
(`pg_dump`/`pg_restore`, sin WAL continuo) sobre una base de desarrollo sin
datos de negocio: bajo un minuto de punta a punta. **Esto no es el número de
producción** — PITR con WAL continuo y volumen real todavía no está
configurado; el número real depende del proveedor gestionado y del tamaño
real de la base el día del incidente.

## Escalamiento

- Restore que falla o da un conteo de tablas distinto al esperado: no
  reintentar a ciegas — el backup puede estar corrupto. Escalar antes de
  intentar un segundo backup más viejo sin registrar qué pasó con el
  primero.
- Necesidad de restaurar producción: siempre con dos personas, nunca en
  solitario — un restore sobre la base equivocada no tiene vuelta atrás.

## Ver también

[[postgres-down]] · [[outbox-backlog]] · [[secret-rotation]]
