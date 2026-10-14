# Informe final de la intervención

Fecha: 2026-10-07. Alcance: snapshots locales de Backend `main` (`4e7bb51f47a948f38adc2598452f24492f4c1b07`) y Frontend `dev` (`1f4c01d9fc19e781bd17d97eb9912f0598c2d8ff`). Los cambios están en el árbol de trabajo y no se han publicado ni desplegado.

## Resumen ejecutivo

Se confirmó una duplicación extensa entre repositorios, se fijaron límites de recursos generados desde los descriptores, se restringió la JVM/build, se añadió detección de impacto para CI y se reutilizan clientes OpenAPI como artifacts dentro de cada ejecución. Se corrigió además una incompatibilidad existente del plugin de generación OpenAPI con Gradle 9.8.

No se retiraron directorios duplicados: hacerlo ahora eliminaría código/tests que solo existen en Backend y rompería la CI de Frontend, que aún depende de Gradle, SQL y scripts backend. Se documentó la frontera objetivo y el orden de migración. La separación física, despliegue y métricas reales quedan pendientes; por eso el éxito general del plan aún no puede declararse completo.

## Arquitectura antes

- Backend tenía 4.705 archivos versionados y Frontend 4.561. De las rutas comunes, 4.520 eran idénticas, 41 divergían, 144 solo estaban en Backend y ninguna solo en Frontend.
- Ambos repositorios incluían aplicaciones web/móvil y, a la vez, los 14 servicios, 26 proyectos Gradle, SQL, `buildSrc`, wrapper y despliegue backend.
- Compose no aplicaba límites duros uniformes; Gradle podía paralelizar workers con heap de 3 GiB; Hikari declaraba 115 conexiones por proceso sumando los 14 servicios.
- La CI de Frontend repetía generación/validación backend y compilación aunque el cambio no afectara esos componentes.

## Arquitectura después

La frontera documentada asigna servicios, SQL, OpenAPI fuente, Gradle y despliegue backend a Backend; aplicaciones, paquetes Yarn/Turborepo y despliegue web/móvil a Frontend. Clientes Angular/Dart se generan desde contratos backend y viajan como artifacts identificados por SHA a los jobs dependientes. Esta es una transición: el contenido duplicado permanece hasta migrar consumidores y diferencias exclusivas.

## Problemas encontrados

1. La duplicación es real y cuantificada; una limpieza por coincidencia de hash no es segura.
2. El workflow Frontend consume `servicios/`, `plataforma/`, `sql/`, Gradle y scripts, por lo que borrar esas rutas dejaría gates rotos.
3. `spotlessCheck` detecta formato preexistente en muchos archivos. `spotlessApply` habría reformateado 292 archivos; se descartó ese cambio masivo.
4. Gradle muestra conflictos entre locks y versiones del catálogo; al actualizar locks, numerosas dependencias del gateway siguen resolviendo como `FAILED`. No se guardaron locks parciales.
5. El VPS no es observable desde esta sesión: SSH devolvió `Permission denied (publickey)`.

## Causas raíz

- Se mezclaron dos productos/repositorios con historial y ownership compartidos, sin contrato reproducible de distribución de clientes.
- El camino de despliegue permitía construir múltiples servicios Java en paralelo y los budgets declarados no se trasladaban consistentemente a Coolify/Compose.
- Los jobs CI dependían del tipo de cambio solo de forma parcial y repetían generación de clientes.
- Hay divergencia entre el catálogo Gradle actual, locks existentes y formato aplicado históricamente; se requiere reconciliación de dependencias por PR acotado.

## Cambios ejecutados

- Inventario reproducible, comparación SHA-256 por ruta, lista de candidatos de remoción, presupuesto runtime, mapa de llamadas síncronas, observabilidad, fronteras y mapa de impacto CI.
- Generador Compose desde descriptores: 1 réplica para cada workload TEST; límites consistentes en `deploy.resources`, `mem_limit` y `cpus`; Hikari TEST max 5/minIdle 0. Gateway 512 MiB/0,5 CPU, schema 256 MiB/0,25 CPU, backoffice 256 MiB/0,25 CPU, SSR web 512 MiB/0,5 CPU y web móvil 256 MiB/0,25 CPU.
- Build de imagen Java secuencial, dos workers, heap Gradle 1,5 GiB y metaspace 384 MiB. Runtime JVM: MaxRAM 65%, inicial 20%, G1 y salida ante OOM.
- Pipeline CI con clasificación de rutas, jobs selectivos, clientes OpenAPI/Postman generados en el job de contratos y artifacts para frontend/macOS. Se conserva la versión v6 de setup-gradle en el workflow Frontend.
- Se quitó `/actuator/prometheus` de la superficie pública del gateway, que no configura Spring Security. No se abrió un endpoint alternativo sin scraper autenticado y red privada.
- El plugin OpenAPI ahora asigna `Directory` al `outputDir`, requerido por la versión usada. La generación de 14 clientes Angular y 14 Dart terminó correctamente.
- Se sincronizaron los cambios transicionales de infraestructura y CI en ambos repositorios. No se borró contenido backend del repositorio Frontend.

## Recursos antes

- Gradle declaraba `-Xmx3g`, metaspace 768 MiB y paralelismo sin máximo de workers.
- Compose no trasladaba budgets de forma uniforme a límites efectivos.
- Hikari permitía 115 conexiones por proceso en conjunto y TEST no fijaba una réplica.
- No hay una captura confiable de RSS/CPU del VPS anterior; no se inventa una medición.

## Recursos después

- Compose generado declara un agregado máximo de 8.960 MiB (8,75 GiB) para el stack Pasanaku TEST, incluyendo frontends y tarea de schema; no es memoria reservada ni consumo medido.
- 14 servicios × máximo Hikari 5 = hasta 70 conexiones cliente por réplica agregadas; mínimo ocioso cero.
- Heap aproximado máximo de una JVM de 512 MiB: 333 MiB, dejando margen nativo.
- El host local de verificación tiene ~7,7 GiB disponibles para Docker. El stack no se levantó completo ni se desplegó en el VPS; el agregado declarado supera la memoria local y debe validarse en el host objetivo junto a Atlas antes de activar todos los servicios.

## CI antes

Cambios frontend podían disparar validaciones y generación backend; cada job generaba/transportaba clientes de forma poco explícita y los jobs macOS seguían atados a la suite frontend completa.

## CI después

`detectar_cambios.py` clasifica rutas contra `dependency-impact.yaml`. Los jobs backend, base, frontend, contratos, imágenes, mobile/macOS, seguridad y vulnerabilidades se filtran por grupos de impacto. Contratos genera OpenAPI una vez y publica artifact por SHA; Frontend descarga ese artifact. Los cambios documentales comunes no activan PostgreSQL. La clasificación es deliberadamente conservadora para rutas compartidas.

## Seguridad

No se removieron controles de dominio, contratos, autorización ni pruebas intencionalmente. Se cerró la exposición pública de métricas del gateway. `verificar_seguridad.py` pasó con tres avisos no bloqueantes existentes: diferencia de permisos CU/catálogo, flujos de token sin canal activo y advertencia de campo DSL dinámico. Antes de activar Prometheus hace falta un scraper privado autenticado.

## Riesgos restantes

- `yarn install --immutable` completó con advertencias de peers. Los gates Frontend no quedaron verdes: lint requiere Node 22.22.3 (local 22.19.0); typecheck reporta `baseUrl` eliminado/no relativo bajo TypeScript 7; test:front y test:a11y se detienen en `contrato-de-adhesion.md` sin frontmatter; build invoca `cp`, no disponible en Windows.
- `spotlessCheck` y `test` Java no pasan por formato preexistente y locks/dependencias no resueltas. No se deshabilitó ninguna prueba ni se conservó el `spotlessApply` masivo.

- Remoción de duplicados pospuesta: 144 archivos Backend-only y workflow Frontend todavía acoplado. No iniciar PR-02 de borrado hasta migrar diferencias y consumers.
- 8,75 GiB de límites agregados no está cotejado con memoria disponible del VPS, Atlas, PostgreSQL, Kafka, MinIO y Coolify.
- Falta inspección post-deploy (`docker inspect`, `docker stats`, reinicios/OOM y réplicas reales).
- SSH denegado; no se pudieron medir pool/queries de PgBouncer/PostgreSQL, lag Kafka, latencias, RSS, logs, imágenes instaladas o alertas.
- Suite Java no queda verde por locks/dependencias; `spotlessCheck` tampoco por divergencia de formato preexistente.
- No se corrió carga real, build limpio/cacheado comparativo ni medición de imágenes Flutter/Angular.

## Deuda técnica

- Separar PR-01/PR-03/PR-04/PR-05/PR-06/PR-07: los cambios locales están agrupados en dos worktrees y requieren commits/PRs pequeños antes de integrar.
- Migrar del Frontend los 144 archivos/backend-only y los consumidores de SQL, scripts y generación.
- Resolver catálogo/locks de Spring y formato Gradle/Java sin aplicar reformateo masivo ni desactivar tests.
- Incorporar dashboard, alertas y exporters solo después de confirmar el plano privado de management.
- Medir bundle Angular, builds limpios/cacheados, imágenes y carga móvil.

## Próximos pasos

1. Revisar y dividir los cambios por PR según el plan; correr CI en GitHub para detectar diferencias de runner.
2. Resolver conflictos de dependencias en un PR aparte y repetir `spotlessCheck`, compilación, tests y security scan.
3. Completar migración de diferencias Frontend y sustituir acceso directo a fuentes backend por artifact/package versionado por SHA; luego eliminar rutas duplicadas con gate de frontera.
4. Obtener acceso de solo lectura al VPS, medir RAM real/Atlas y ajustar budgets por evidencia antes de desplegar.
5. Desplegar primero un servicio canario, validar límites efectivos y métricas, luego ampliar gradualmente.
6. Añadir exporters/dashboards/alertas privados y ejecutar pruebas de carga con umbrales acordados.

## Score final

| Área | Antes | Después |
|---|---:|---:|
| Arquitectura | 2/5 | 3/5 |
| Runtime | 1/5 | 3/5 configuración; medición pendiente |
| Build | 1/5 | 3/5 configuración; suite pendiente |
| CI | 2/5 | 4/5 diseño; CI remota pendiente |
| Seguridad | 3/5 | 4/5 superficie gateway; revisión total pendiente |
| Base de datos | 2/5 | 3/5 configuración de pool; PgBouncer pendiente |
| Observabilidad | 1/5 | 2/5 exposición corregida; stack pendiente |
| Mantenibilidad | 2/5 | 3/5 |
| Eficiencia | 1/5 | 3/5 configurada; runtime no medido |

La evaluación indica avance verificable en configuración, no cumplimiento completo del criterio general. No hay deploy, merge, publicación de artifacts entre repositorios ni eliminación de archivos.


