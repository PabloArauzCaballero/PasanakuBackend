# Duplicación entre repositorios

Fecha de comparación: 2026-10-07. Referencias: Backend `main` en `4e7bb51f47a948f38adc2598452f24492f4c1b07`; Frontend `dev` en `1f4c01d9fc19e781bd17d97eb9912f0598c2d8ff`.

## Resultado

La hipótesis se confirma con una duplicación casi total. Al comparar los 4.705 archivos versionados de Backend con los 4.561 de Frontend:

- 4.520 rutas tienen contenido idéntico según SHA-256.
- 41 rutas comunes tienen contenido distinto.
- 144 rutas existen solo en Backend.
- No hay rutas exclusivas de Frontend: todas las rutas de Frontend existen en Backend.
- Los 144 archivos exclusivos de Backend incluyen código y pruebas recientes de web, backoffice y móvil, clientes generados, archivos de despliegue y evidencia. La separación debe conservar esos cambios antes de retirar contenido.

La comparación completa con ruta, estado, SHA-256 y tamaño por repo está en [01-comparacion-archivos.md](01-comparacion-archivos.md). El generador reproducible es [comparar_repos.py](../../scripts/auditoria/comparar_repos.py).

## Evidencia de carpetas cruzadas

Ambos repositorios contienen los 14 servicios backend, los 26 proyectos Gradle, `plataforma/`, `sql/`, `buildSrc/`, wrapper/configuración Gradle, descriptores, scripts de backend y archivos de despliegue. También tienen `apps/`, `packages/` y la configuración Yarn/Turborepo. `docker-compose.coolify.yml` coincide byte por byte (SHA-256 `c27ff96d5843543d760f658782298590a53323d52f1be408c1b6f51ab95c8ba6`).

No es seguro retirar carpetas todavía: el workflow Frontend `ci.yml` ejecuta Gradle, genera clientes Angular/Dart, aplica y valida `sql/`, y ejecuta pruebas backend. También usa scripts que leen `servicios/` y `plataforma/`. Las referencias concretas están enumeradas en el workflow y scripts de Frontend; el PR de separación debe primero mover esos gates al repo Backend o reemplazarlos con artifacts identificados por SHA.

## Conclusión de fuente de verdad

- Backend debe ser dueño de servicios, plataforma, SQL, descriptores, Gradle, OpenAPI fuente, generación de contratos y despliegue backend.
- Frontend debe ser dueño de `apps/`, `packages/`, Yarn/Turborepo, Dockerfiles y despliegues propios de web/móvil.
- Los clientes Angular/Dart deben ser artefactos generados desde OpenAPI del Backend. La opción inicial elegida es publicarlos como artifact versionado por SHA y consumir exactamente ese SHA desde Frontend; no se copiarán manualmente.
- `docs/`, scripts comunes y archivos compartidos requieren clasificación individual. La coincidencia de hash por sí sola no autoriza borrar ni escoger propietario.

## PR-01 propuesto

`docs: baseline de recursos y duplicación`

Incluye el inventario, el comparador y los resultados de esta auditoría. No cambia comportamiento de build, contratos, runtime o aplicaciones. La base de comparación está fechada y los SHAs están registrados.

## PR-02 propuesto

`chore: separar fuentes de verdad backend y frontend`

Debe migrar primero los cambios de Frontend que solo existen en Backend, establecer publicación/consumo reproducible de clientes generados por SHA, separar workflows y validar los dos repositorios. Luego retira duplicados clasificados y sin referencias. No debe borrar los 144 archivos exclusivos de Backend durante la limpieza de Frontend.
