# Reporte — Auditoría de solo lectura L1: aportes + núcleo financiero

> **AVANCE: 8 / 9 — 88,9 %.** (H1.S1.M9, la entrega, se cierra con este mismo push; ver «Pendiente»)

- Fecha: 2026-11-20 · Plan: [PLAN.md](./PLAN.md) · Informe: [AUDITORIA-aportes-nucleo-financiero.md](./AUDITORIA-aportes-nucleo-financiero.md)
- Rama: `auditoria/aportes-nucleo-financiero-2026-11-20` (desde `origin/test` @ `07fc69719287b0dadd152d0293a054948df69ead`; `origin/dev` @ `850e0f51` sin diferencias en el módulo)
- Peldaño de evidencia alcanzado: `DISCOVERED` en todas las áreas (lectura del código). Verificadores Python del repo ejecutados. Pruebas Java, Flutter y Angular: `BLOCKED` por entorno. Ningún hallazgo se ejecutó.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Rama desde `origin/test` con historial completo | `git rev-parse --is-shallow-repository` | PASS → `false` |
| H1.S1.M2 | Huella y mapa de conexiones (sección 3) | `grep -c '^## 3' AUDITORIA-*.md` | PASS → 1 |
| H1.S1.M3 | Anexo con una fila por archivo (418) | `grep -c '^| \`servicios' AUDITORIA-*.md` | PASS → 418 |
| H1.S1.M4 | Inventarios de aprobaciones (14) y de respaldo (9 + 1 que no aplica); A-1 a A-4 tratadas | `grep -c '^| AP-\|^| RS-' AUDITORIA-*.md` | PASS → 24 |
| H1.S1.M5 | Vista de conjunto (duplicación, reescrituras, inconsistencias por fallo a mitad) | sección 4e y fila de clases en la sección 8 | PASS |
| H1.S1.M6 | Matriz de contratos y salidas de verificadores (Gradle, yarn y Flutter declarados NO COMPROBADO) | secciones 6 y 7 | PASS (con lo bloqueado declarado) |
| H1.S1.M7 | Cada hallazgo con característica ISO/IEC 25010 | `grep -c 'ISO/IEC 25010' AUDITORIA-*.md` ≥ 45 | PASS |
| H1.S1.M8 | Refutación de S1/S2: 4 hallazgos retirados o rebajados | sección 11, «Refutación» | PASS |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M9 | TODO hasta que el push termine | `git push -u origin auditoria/aportes-nucleo-financiero-2026-11-20`; el resultado se pega en el mensaje final de la sesión |

## Evidencia
```text
$ ./gradlew --no-daemon :servicios:aportes:test :servicios:aportes:webTest :servicios:nucleo-financiero:test :servicios:nucleo-financiero:webTest --continue
> Could not resolve org.postgresql:postgresql:42.7.12. … Received status code 429 from server: Too Many Requests
BUILD FAILED in 38s
$ docker ps
failed to connect to the docker API at unix:///var/run/docker.sock
$ yarn typecheck
error This project's package.json defines "packageManager": "yarn@4.18.0". However the current global version of Yarn is 1.22.22.
$ python3 scripts/auditar_backend.py
NOTA GLOBAL: 9.64/10   ·   con 0/393 pruebas verificadas
```
El resto de los verificadores, en la sección 7 del informe.

Resultado: **45 hallazgos — S1: 8 · S2: 20 · S3: 16 · S4: 1.**

## No cubierto
- Ninguna ejecución de pruebas del módulo. La evidencia de las reglas 91.6, 98.8 y 90.6 queda NO COMPROBADA, salvo el redondeo (por lectura).
- Pruebas: inventariadas por grep, no leídas línea a línea. OpenAPI: lectura parcial. Tablas de billetera y custodia: por grep.
- No hay prueba visual de ninguna pantalla (regla 95.7).

## Desvíos del plan
- Catalogación y push por tandas (indicación de Pablo, retransmitida por la sesión coordinadora).
- H1.S1.M6 cerrado con la parte de Gradle, yarn y Flutter en `BLOCKED` por entorno.

## Riesgos residuales
Los ocho S1 del informe. Los dos de mayor impacto son FLUJO-QR-001 (el aporte se cobra por declaración del cliente) y RESPALDO-003 (el registro contable de la cobertura empresarial está encendido en el despliegue de TEST).

## Decisiones y ambigüedades
- A-1 a A-4: registradas en la sección 4c del informe, sin resolver (Pablo; Cumplimiento para AP-08 y AP-11).
- Supuestos: TEST corre con el perfil `local` (`despliegue/TEST.md:458`); `garantia` publica la cobertura en TEST (lo confirma L2).
- El control de la regla 65 señala microtareas de **otros** planes (`2026-10-03-…` a `2026-10-30-…`). No se tocan por el alcance de solo lectura.
- Procesos en background: ninguno queda corriendo. Las corridas de Gradle y de los cuatro subagentes ya terminaron.
