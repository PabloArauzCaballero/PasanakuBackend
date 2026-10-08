# Tamaño de imágenes y bundles

Captura: 2026-10-07. El checkout no contiene imágenes de aplicación construidas con los Dockerfiles actuales, y no se desplegó el stack. Por ello no existe una comparación válida de antes/después; el tamaño del filesystem de una imagen base no representa el tamaño final de cada servicio.

| Imagen/artefacto | Antes | Después | Resultado |
|---|---:|---:|---|
| Backend Java (14 servicios + gateway) | Sin imagen de aplicación disponible | Sin imagen construida | Pendiente de build reproducible |
| Backoffice | Sin imagen de aplicación disponible | Sin imagen construida | Pendiente |
| Web SSR | Sin imagen de aplicación disponible | Sin imagen construida | Pendiente |
| Web móvil | Sin imagen de aplicación disponible | Sin imagen construida | Pendiente |
| Flutter Android/iOS | No medido | No medido | Pendiente de build release |
| Angular inicial/lazy chunks | No medido | No medido | Pendiente de build y análisis de bundle |

El Dockerfile backend ahora limita la concurrencia de Gradle y su memoria durante el build; esto reduce presión del host, pero no demuestra que la imagen final sea menor. Medir después de builds limpios equivalentes con `docker image inspect`, registrar tiempo y tamaño y comparar los mismos targets/plataformas.
