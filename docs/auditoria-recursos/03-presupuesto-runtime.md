# Presupuesto de recursos TEST

Los límites se generan desde los descriptores para servicios Java; los valores de gateway y del trabajo de esquema siguen el presupuesto operativo del entorno TEST. Fronts y backend conservan archivos Compose independientes en Coolify.

| Servicio | Réplicas TEST | RAM límite | Heap JVM máximo aproximado | CPU límite | Hikari max/min idle |
|---|---:|---:|---:|---:|---:|
| gateway | 1 | 512 MiB | 333 MiB | 0,5 | N/A |
| identidad | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| aportes | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| grupos | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| nucleo-financiero | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| tarifas | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| entregas | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| garantia | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| notificaciones | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| organizador | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| auditoria | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| cumplimiento | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| erp | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| publicidad | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| transparencia | 1 | 512 MiB | 333 MiB | 0,5 | 5 / 0 |
| esquema (una sola ejecución) | 1 | 256 MiB | N/A | 0,25 | N/A |
| backoffice | 1 | 256 MiB | N/A | 0,25 | N/A |
| web (SSR) | 1 | 512 MiB | N/A | 0,5 | N/A |
| móvil web | 1 | 256 MiB | N/A | 0,25 | N/A |

Heap aproximado: 65 % de 512 MiB = 332,8 MiB. `JAVA_TOOL_OPTIONS` deja margen de contenedor para metaspace, buffers, threads y memoria nativa. El cálculo es un techo inicial de configuración, no una medición RSS.

La suma máxima de límites declarados es 8.960 MiB (8,75 GiB) para los servicios de Pasanaku TEST. No equivale a memoria reservada y Docker solo puede aplicar esa suma si el host tiene capacidad. El VPS comparte memoria con Atlas, PostgreSQL, PgBouncer, Kafka, MinIO, Coolify y el sistema operativo; por falta de acceso SSH no se validó que el agregado sea compatible con su presión real de memoria.

El pool TEST queda en hasta 70 conexiones cliente (14 servicios × 5), con un mínimo inicial de cero conexiones ociosas por servicio. PgBouncer declara `max_client_conn: 1000`; PostgreSQL declara `max_connections: 400` y margen reservado de 80. La capacidad del pooler de servidor y la carga concurrente real deben revisarse con sus métricas tras desplegar.

## Implementación y evidencia local

- `scripts/generar_compose.py --coolify` lleva límites coherentes en `deploy.resources.limits` y en `mem_limit`/`cpus`, con una réplica para workloads de aplicación TEST.
- Coolify recibe `maximumPoolSize=5` y `minimumIdle=0` en cada microservicio.
- `despliegue/Dockerfile` limita el build de cada imagen Java a dos workers, sin build paralelo y con heap de Gradle de 1,5 GiB y metaspace de 384 MiB.
- La JVM de runtime usa `MaxRAMPercentage=65`, `InitialRAMPercentage=20`, G1 y salida explícita ante OOM.
- Docker Compose 5.4.0 aceptó `config --quiet` en los cuatro archivos; el JSON expandido mostró `mem_limit=536870912`, CPU `0.5`, una réplica y Hikari `5` para servicios revisados.
- `scripts/auditoria/verificar_limites_compose.py` y el gate de CI comprueban todos los servicios y detectan si las salidas generadas divergen.

Rollback: revertir el cambio del generador/Dockerfile y regenerar los cuatro compose. Las imágenes TEST existentes conservan los límites anteriores hasta que Coolify despliegue los archivos nuevos; no se hizo despliegue remoto.
