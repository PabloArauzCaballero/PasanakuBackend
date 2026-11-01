# Reporte — Cerrar los pendientes de Sistemas

> **AVANCE: 19 / 21 — 90,5 %** (1 `DESCARTADO` y 1 `A MEDIAS`: la verificación en TEST con sesión real). Mergeado en `test` (#243) y `dev` (#244).

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/sistemas-pendientes-20261029`
- Peldaño alcanzado: `TESTED`. Cada endpoint tiene tests unitarios, web (incluida la autorización negativa) y de integración contra Postgres real, más la regresión de los cuatro servicios y del backoffice completo. No es `VERIFIED`, porque falta ejercitarlo en TEST con una sesión real (ver A medias).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1 | Revisión de accesos: una fila por persona y rol, con estado de la cuenta y segundo factor, desde identidad `GET /usuarios-internos` | `ng test …/sistemas/**` | PASS · `evidencia/H1-accesos.txt` |
| H2 | Incidentes: `GET /indicadores/operacion/incidentes` sobre `auditoria.incidente_operativo`, paginado y en orden determinista | `gradlew :servicios:auditoria:{test,webTest,integrationTest}` | PASS 58/82/73 · `evidencia/H7-regresion-servicios.txt` |
| H3 | Webhooks: `GET /pagos/webhooks-recibidos` (aportes) y `GET /notificaciones/webhooks-recibidos`, sin payload, firma ni destinatario | ídem en aportes y notificaciones | PASS 49/50/121 y 5/46/74 |
| H4 | Proveedores: `GET /pagos/proveedores`, `GET /notificaciones/proveedores` y `GET /tarifas/costos-de-proveedor` (suma DECIMAL exacta, una fila por moneda) | ídem en tarifas | PASS 7/37/108 |
| H5 | Outbox por servicio: `GET /indicadores/operacion/outbox` sobre las métricas del relevo; con Prometheus caído responde `fuenteDisponible: false` | auditoria + `ng test` | PASS · `evidencia/H5-*.txt` |
| H6 | Despliegues y Respaldos dicen qué falta en concreto; la propuesta está en `entregables/brecha-observabilidad.md` | `ng test …/sistemas/**` | PASS |
| H7.S1.M1 | Regresión: backoffice 1363/1363 y `ng build` OK, con los cuatro servicios en verde | ver evidencia | PASS · `evidencia/H7-*.txt` |
| H7.S1.M3 | Se retiraron `PUERTO_BASE_DE_DATOS` y `PUERTO_OUTBOX`, que ya no usaba nadie | `ng test` | PASS |
| H7.S1.M4 | `nucleo/menu.ts` volvió a 200 líneas: lo había pasado a 202 en #224 y `verificar_frontend` fallaba en `test` | `python3 scripts/verificar_frontend.py` | TODO OK · `evidencia/verificadores-ci.txt` |

## A medias
### H7.S1.M2 — TEST con sesión real
- **Qué anda:** el código está mergeado en `test` y desplegado: el host está en `c3fd625`, que contiene `310124e` (`git merge-base --is-ancestor`), y esa tanda terminó con «listo en c3fd625» (2026-10-30 00:36). Sin credenciales, los siete endpoints nuevos responden 401 (`evidencia/H7-test-sin-sesion.txt`).
- **Qué no anda:** el 401 no prueba que el endpoint exista: una ruta inventada bajo el mismo prefijo (`/pagos/no-existe`) también da 401, porque la seguridad corre antes que el ruteo. Además, no ingresé a TEST con un usuario `ADMIN_PLATAFORMA`. El clasificador de permisos bloqueó leer el archivo de credenciales de prueba del host, y no le busqué la vuelta.
- **Qué falta exactamente:** entrar al backoffice de TEST como `ADMIN_PLATAFORMA` y abrir Accesos, Incidentes, Webhooks, Proveedores, Outbox y Base de datos. Confirmar en Outbox que `fuenteDisponible` es `true`, porque los nombres de las métricas en Prometheus son una hipótesis (ver Riesgos).
- **Dónde quedó:** `test` en `310124ed`, `dev` en `af660515`; compila y los tests están en verde.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Despliegues y Respaldos con datos | `DECISION_REQUIRED` | Pablo decide quién registra los despliegues y respaldos: el script de despliegue (propuesta en `entregables/brecha-observabilidad.md`) o Coolify. |
| Interruptores (Despliegues) | `DECISION_REQUIRED` | No hay modelo de banderas en ningún servicio; la doble confirmación del lado servidor es un caso de uso nuevo. |
| Receptor de avisos de entrega | TODO (otro carril) | Ningún código escribe `notificaciones.evento_entrega_mensaje` (`CU83EnviosTest.java:32`). Hasta que exista, la mitad de mensajería de Webhooks va a salir vacía en TEST. |

## Descartado
| ID | Por qué |
|---|---|
| H5.S1.M1 (medidor del outbox) | Ya existe: `Relevo.java` publica `aportaya.outbox.pendientes`, `fallidos`, `edad_mas_viejo_segundos` y `publicaciones_total`. Mi descubrimiento solo había leído un rango del archivo. |

## Evidencia
```text
$ ./gradlew :servicios:<s>:test :servicios:<s>:webTest :servicios:<s>:integrationTest --rerun
== auditoria exit=0       test 58 · webTest 82 · integrationTest 73 · fallas 0
== aportes exit=0         test 49 · webTest 50 · integrationTest 121 · fallas 0
== notificaciones exit=0  test 5  · webTest 46 · integrationTest 74 · fallas 0
== tarifas exit=0         test 7  · webTest 37 · integrationTest 108 · fallas 0

$ npx ng test --include='src/**/*.spec.ts'
 Test Files  224 passed (224)
      Tests  1363 passed (1363)
$ npx ng build            → OK, sin aviso de presupuesto

$ python3 scripts/verificar_{seguridad,criterios,pruebas_web,contratos_limites,frontend,boveda,narrativas}.py
todos exit=0
```
Todo dato de prueba es sintético: códigos `P…`, `M…`, `T…`, fechas de 2099 y personas «PersonaN Prueba».

## No cubierto
- Sesión real en TEST (A medias).
- Nombres de las métricas del outbox en Prometheus: los deduje de la traducción OTLP→Prometheus del colector y no los verifiqué contra un Prometheus vivo. Son configurables (`aportaya.prometheus.metrica-outbox-*`).
- Revisión de accesos no muestra «otorgado el»: ningún contrato publica cuándo se asignó cada rol.
- Webhooks y Proveedores piden los últimos 100 registros por fuente; no hay paginación hacia atrás.

## Desvíos del plan
- **Tests con contenedor mal nombrados (`TEST_BUG`, mío):** los cuatro tests de integración caían en el corredor `test`, que tiene un plazo de 5 s y no levanta contenedor, y pasaban solo con el contenedor caliente. Se renombraron a `*RepositorioTest`, la convención de `aportaya.base.gradle.kts:105`. Ningún plazo se tocó.
- **Conteo engañoso:** mi script contaba fallas leyendo los XML, y un `initializationError` no queda en el XML de la clase. Desde que lo detecté, el criterio es el `BUILD SUCCESSFUL` y el exit code de gradle.
- **Costos solo de proveedores de pago:** `costo_proveedor_operacion.proveedor_id` tiene FK a `aportes.proveedor_pago`. Mi primer grep no la encontró y lo descubrió el test de integración. Se corrigieron el contrato y el adaptador.
- **Webhooks:** si cualquiera de las dos fuentes falla, el puerto rechaza. El plan decía «mostrar la otra con aviso», pero el puerto no puede llevar ese aviso. Elegí rechazar antes que mostrar media lista como si fuera toda.
- **Outbox** cambió de «lista de mensajes» a «por servicio»; sus specs se reescribieron y el de accesibilidad se fusionó en el spec nuevo. El menú pasó a decir «Outbox por servicio» (5 specs actualizados).
- Se integró `origin/test` a mitad del trabajo. El único conflicto fue el ícono de cabecera de Outbox, que #235 quitó de todas las pantallas. Se regeneraron el catálogo de operaciones y Postman.

## Riesgos residuales
- **Secreto en el log del host (fuera de alcance, no lo toqué):** `/opt/aportaya/logs/autodespliegue.log` muestra en claro la clave de base `BD_CLAVE_ADMIN`, que se pasa como argumento a un `RUN` del Dockerfile. Infringe la regla 90.3.3. Hay que rotar esa clave y pasarla como secreto de build.
- **TEST no construye `grupos` desde 2026-10-30 01:33:** `AcuerdoRepositorio.java` da `cannot find symbol` en las líneas 54, 68, 81 y 173. Viene de otra sesión, no de este trabajo; las tandas siguientes van a fallar hasta que se corrija.
- **Dinero sin moneda en el modelo:** `auditoria.incidente_operativo.impacto_monetario` y `notificaciones.proveedor_mensajeria.costo_por_mensaje` no tienen columna de moneda (regla 91.1.2). No se publican; hay que corregir el modelo.
- `incidente_operativo` no tiene índice sobre `detectado_en`. Con pocas filas no importa, pero hay que agregarlo si crece.
- El paquete `@aportaya/simulado` conserva datos de ejemplo de migraciones y outbox que ya no usa nadie.

## Decisiones y ambigüedades (confirmar con Pablo)
1. «Incidentes» de Sistemas = `incidente_operativo` (operación), no `incidente_seguridad` (cumplimiento).
2. Outbox por servicio, a partir de métricas: auditoria no puede leer el esquema de otros servicios.
3. Detectado y Mitigado cuentan como abiertos; Post mortem y Resuelto, como cerrados.
4. El costo de Proveedores es el del último mes cerrado en hora de La Paz.
