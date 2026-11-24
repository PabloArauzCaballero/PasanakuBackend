# H2.S1.M1 — semilla SUPERVISOR_CUMPLIMIENTO sobre PostgreSQL 16 local (base efímera, datos solo de catálogo)

```text
$ createdb pasanaku && (cd sql && psql -v ON_ERROR_STOP=1 -d pasanaku -q -f aplicar.sql)   # EXIT 0
$ psql ... -f <semilla de HEAD (dev 6729629d)>/sql/60_semillas/sembrar.sql                     # EXIT 0
-- estado con la semilla VIEJA:
OFICIAL_CUMPLIMIENTO|BLOQUEO_AUTORIDAD_LEVANTAR,BLOQUEO_AUTORIDAD_REGISTRAR
$ psql ... -f sql/60_semillas/sembrar.sql   (nueva, corrida 1)  # EXIT 0
OFICIAL_CUMPLIMIENTO|BLOQUEO_AUTORIDAD_REGISTRAR
SUPERVISOR_CUMPLIMIENTO|AUDITORIA_LEER,BLOQUEO_AUTORIDAD_LEVANTAR
roles|permisos|rol_permiso = 33|74|166
$ psql ... -f sql/60_semillas/sembrar.sql   (nueva, corrida 2)  # EXIT 0
OFICIAL_CUMPLIMIENTO|BLOQUEO_AUTORIDAD_REGISTRAR
SUPERVISOR_CUMPLIMIENTO|AUDITORIA_LEER,BLOQUEO_AUTORIDAD_LEVANTAR
roles|permisos|rol_permiso = 33|74|166   <- la segunda corrida no inserta ni borra nada
```

Consultas usadas:
```sql
SELECT r.codigo, string_agg(p.codigo, ',' ORDER BY p.codigo) FROM identidad.rol r JOIN identidad.rol_permiso rp ON rp.rol_id=r.id JOIN identidad.permiso p ON p.id=rp.permiso_id WHERE (r.codigo='OFICIAL_CUMPLIMIENTO' AND p.codigo LIKE 'BLOQUEO%') OR r.codigo='SUPERVISOR_CUMPLIMIENTO' GROUP BY r.codigo ORDER BY 1;
SELECT (SELECT count(*) FROM identidad.rol), (SELECT count(*) FROM identidad.permiso), (SELECT count(*) FROM identidad.rol_permiso);
```
