# Observabilidad y validaciones que requieren infraestructura

## Hallazgos confirmados

- Los servicios backend y el gateway declaran Micrometer Prometheus como dependencia.
- Los 14 servicios Spring no exponen explícitamente `/actuator/prometheus`; la cadena de seguridad del módulo web deniega acceso anónimo fuera de health/info y JWKS.
- El gateway sí exponía `prometheus` en el puerto de API y no incluye Spring Security. Quité esa exposición del puerto 8080; se conserva health/info.
- No hay configuración versionada de Prometheus, exporter PgBouncer/PostgreSQL, dashboard o reglas de alerta.
- La conexión saliente ya aplica 1 s de connect y 3 s de lectura. Gateway declara 2 s y 10 s. El cotizador de tarifas ya configura retry/circuit breaker con un fallback que rechaza sin cotización.
- Los logs, spans y configuración de trazas requieren revisión por servicio; no se cambió el formato sin verificar los consumidores actuales.

## Pendiente de operación

No pude confirmar límites activos, consumo RSS, heap real, CPU, imágenes locales, pool PgBouncer, `pg_stat_statements`, Kafka lag, latencias, errores, disco, rotación de logs ni dashboards en el VPS. La consulta SSH de solo lectura fue rechazada con `Permission denied (publickey)`. No se aplicó ningún cambio remoto.

Antes de abrir `/actuator/prometheus`, desplegar un scraper en red privada y autenticarlo con una credencial de servicio de rotación definida; el gateway no debe publicar métricas a través del puerto público. Después se puede habilitar el endpoint en el puerto de management privado y crear paneles/alertas desde series reales. PgBouncer y PostgreSQL viven fuera de este repo según `despliegue/TEST.md`, así que sus exporters/config deben instalarse en el host y no se inventaron aquí.

## Revisiones necesarias tras desplegar

1. `docker inspect` por servicio: `HostConfig.Memory`, `HostConfig.NanoCpus` y número de réplicas.
2. Muestreo de `docker stats` en arranque, idle y carga real; verificar OOM/restarts.
3. `SHOW POOLS` en PgBouncer: `cl_active`, `cl_waiting`, `sv_active`, `sv_idle`, `sv_used`, `maxwait`.
4. Activar `pg_stat_statements` con ventana definida y exportar consultas agregadas sin valores/PII.
5. Medir top queries, heap/RSS/GC, threads, latencia p50/p95/p99, 5xx y Kafka lag.
6. Comparar RSS p95 y uso agregado contra los presupuestos antes de subir cualquier límite.
