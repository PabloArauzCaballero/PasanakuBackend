# Reporte — Cerrar los pendientes del 2026-10-29

> **AVANCE: 5 / 6 — 83,3 %.** Script instalado en el host (09:56:50); H1.S1.M3 se cierra con la corrida que dispara este commit.

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/cerrar-pendientes-20261029` (PRs #239/#240 y #241/#242, mergeados en dev y test)
- Peldaño: `VERIFIED` para la pantalla de SLO y para el arreglo de `auditoria` (runtime en TEST, con captura); `TESTED` para el catálogo; `WRITTEN` + `bash -n` para el script del host.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | El script repone cualquier imagen faltante y verifica las 20 antes de desplegar | `bash -n despliegue/host/reconstruir-si-cambio.sh` | PASS, exit 0 · `evidencia/M1-sintaxis.txt` |
| H1.S2.M1 | Captura real de Sistemas › Servicios en TEST, en escritorio (claro y oscuro), tablet y móvil | Playwright con login real | PASS: «Operativos 14 · Caídos 0», con disponibilidad, presupuesto e interrupción · `evidencia/slo-*.png` (revisadas) |
| H1.S3.M1 | Catálogo de operaciones regenerado (estaba atrasado desde el 20-10) | `ng test` sobre rutas/sistemas + `auditoria` test/webTest | PASS, 171/171, 55 y 75 · `evidencia/S3-*.txt` |
| H1.S1.M2 | Script instalado en el host por Pablo | `sha256sum /opt/aportaya/bin/reconstruir-si-cambio.sh` | PASS, `3d985970db014fe1` = versión de `origin/test` |
| H1.S4.M1 | `auditoria` ya no se ve CAIDO a sí misma | 3 × `GET /indicadores/operacion/servicios` real, sin caché | PASS: `[]` (ningún servicio fuera de ARRIBA) · `evidencia/S4-no-arriba.txt` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S1.M3 | BLOQUEADO | Depende de M2: la siguiente corrida del temporizador después de instalarlo. |

## Evidencia
```text
causa de «Caídos 1» (medida en TEST):
  log de auditoria: "salud de auditoria: sin respuesta (TimeoutException)" solo en las peticiones HTTP
  readiness de auditoria pedido desde afuera: {"status":"UP"} 10ms · NanoCpus=500000000 (0,5 CPU → 1 portador) · java 21.0.12
  SaludDeServicios.consultar era synchronized + spring.threads.virtual.enabled=true → portador fijado
después de ReentrantLock (952e606 desplegado):
  [] [] []   ← servicios fuera de ARRIBA, en tres consultas separadas por 11 s
  0          ← "salud de auditoria: sin respuesta" en los últimos 3 minutos
```

## No cubierto
- La limpieza nocturna real de Coolify sobre las imágenes ya etiquetadas: corre hoy a las 20:00
  (hora de La Paz). Se replicó su regla, pero no se observó la corrida real.
- La rama «imagen faltante» del script nuevo no se ejercitó: no está instalado.
- No se escribió un test que reproduzca la fijación del portador; haría falta lanzar la JVM con
  `jdk.virtualThreadScheduler.parallelism=1`. La evidencia es de runtime en TEST.

## Desvíos del plan
- El primer intento de instalación se corrió desde la copia local atrasada y dejó una versión vieja (hash `023c8364…`). Se reinstaló desde `git show origin/test:…`. El respaldo `reconstruir-si-cambio.sh.bak-20261029` guarda el script que corría antes.
- Se agregó H1.S4.M1. La captura mostró «Caídos 1» con `auditoria` sana: era un bug real, el
  mismo de la primera captura de Pablo.

## Riesgos residuales
- `LimiteDeConsultas` de identidad y notificaciones usa `synchronized`, pero no bloquea adentro, así
  que no fija el portador. Queda anotado, sin tocar.
- Los presupuestos negativos de hoy salen de los reinicios de los despliegues de esta mañana.
  Es lo que se midió.

## Decisiones y ambigüedades
- La copia local `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18` no se tocó. Tiene un acordeón
  sin commitear que, según la memoria de Pablo, no se sube, y otras sesiones la están usando.
- Siguen en pie los supuestos del SLO: «última interrupción» incluye DEGRADADO, y los minutos sin
  medición no cuentan.
