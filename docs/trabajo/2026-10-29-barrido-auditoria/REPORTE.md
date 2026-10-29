# Reporte — barrido de reglas propias de `servicios/auditoria`

> **AVANCE: 3 / 4 — 75,0 %.** Las tres microtareas de código están hechas contra una réplica local del barrido; la cuarta (que `auditoria:testBarrido` pase en el CI real) todavía no se ejecutó.

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `claude/awesome-turing-5wmr4r` (la misma del PR #266: es la única rama de trabajo autorizada, así que este arreglo viaja en ese PR)
- Peldaño de evidencia alcanzado: `WRITTEN`, con la regla reproducida localmente (no compila ni corre Gradle aquí). No se afirma `RUNS` ni `TESTED`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | `SaludDeServicios` ya no lleva el `99.5` en el código: se retiró `OBJETIVO_POR_OMISION`, el `:99.5` del `@Value` y el constructor de conveniencia de `Salida` (que solo usaban dos pruebas) | `python3 -I replica_barrido.py be-dev/servicios/auditoria` | PASS contra la réplica: 0 hallazgos ([evidencia](./evidencia/01-replica-del-barrido.txt)) |
| H1.M2 | Los dos `BigDecimal.valueOf(100)` desaparecen: la base del porcentaje es `BigDecimal.TEN.pow(2)` y la validación `(0, 100)` del SLO está en un solo método, `DisponibilidadDelServicio.exigirObjetivoValido` | ídem | PASS contra la réplica |
| H1.M3 | `IndicadoresController` baja de 379 a 264 líneas; el bloque del catálogo de datos (6 operaciones) vive en `RutasDelCatalogoDeDatos` (150 líneas) | `wc -l` + comparación de los 11 métodos públicos con sus anotaciones | PASS: 11 = 11, anotaciones idénticas, ningún archivo ≥ 300 |

## A medias
### H1.M4 — que el CI real pase `auditoria:testBarrido`
- Qué anda: la réplica de las dos reglas del barrido da exactamente los 4 hallazgos que el CI dio en `dev` (run 40878468838) antes del cambio y 0 después.
- Qué no anda: no sé si compila ni si las pruebas de `auditoria` siguen verdes; Gradle no corre en este entorno.
- Qué falta exactamente: el CI de la rama (push y run nuevo); si aparece un diff de `spotless`, aplicarlo; confirmar que `auditoria:test`/`webTest` pasan y que el job llega por fin a `grupos:webTest` y a `BandejaDeAdmisionesRepositorioTest`.
- Dónde quedó: archivos tocados en [Evidencia](#evidencia); commits 3fe73702 y el de formato, en la rama del PR #266.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| (fuera de este plan) `GET /grupos/solicitudes-escaladas` | DECISION_REQUIRED | Pablo: qué significa «escalada» |
| (fuera de este plan) bóveda en rojo en `dev` | en curso en otra sesión (PR #262/#263) | su merge |

## Evidencia
- [01-replica-del-barrido.txt](./evidencia/01-replica-del-barrido.txt): salida literal antes/después y la comparación de métodos; el script que la produce, [replica_barrido.py](./evidencia/replica_barrido.py), copia las regex y el límite de `SinUmbralLiteral` y `TamanoDeArchivo`.
- Archivos: `servicios/auditoria/src/main/java/bo/aportaya/auditoria/{aplicacion/SaludDeServicios,dominio/DisponibilidadDelServicio,web/IndicadoresController}.java`, nuevo `web/RutasDelCatalogoDeDatos.java`, y `src/test/.../web/SaludDeServiciosWebTest.java` (un helper `soloElEstado` en lugar del constructor retirado; las aserciones no cambian).

## No cubierto
- Compilación, `spotlessCheck` y todas las pruebas de `auditoria`: no se ejecutaron. Los puntos de mayor riesgo son que el generador de OpenAPI exponga `IndicadoresApi` tal como lo asume la clase abstracta y que `@Permiso` se resuelva sobre los métodos heredados de una clase de paquete (mismo patrón que `grupos/web/RutasDe*`, en producción, pero no ejercitado aquí).
- El arranque real de `auditoria` sin `aportaya.salud.objetivo-slo`: ahora falla, a propósito (regla 90.3.4); el `application.yml` trae `${SALUD_OBJETIVO_SLO:99.5}`, así que el arranque normal no cambia. No lo arranqué.
- Los otros reds de `dev` (f5, bóveda, `AdmisionDelGrupoSustitucionTest`).

## Desvíos del plan
- CI del commit 3fe73702 (run 40885070706): `spotlessCheck` (paso 2) falló en `:servicios:auditoria` por dos diffs de formato míos, tal como anticipaba el riesgo del PLAN: una línea en blanco de más antes de `mapear` en `IndicadoresController` y un `when(...).thenReturn(...)` que cabe en una línea en `SaludDeServiciosWebTest.auditor()`. Se aplicaron literal en el commit siguiente. `testBarrido` y `compileJava` no llegaron a ejecutarse en ese run (el paso 2 corta).
- Hay un arreglo paralelo del mismo barrido en el PR #262 (commit 088547a6, otra sesión); se comentó en el PR #266 y queda a decisión de Pablo cuál de los dos aterriza.
- Código: ninguno más. El PLAN prevía «`wc -l` + réplica» para M3 y se añadió la comparación automática de firmas y anotaciones.

## Riesgos residuales
- Quien lance `auditoria` fuera de Compose/CI sin `application.yml` de recursos y sin la propiedad lo verá fallar al arrancar (era silencioso antes). Es el comportamiento pedido por la regla 90.3.4.
- `BigDecimal.TEN.pow(2)` satisface la forma de la regla; la regla no distingue una unidad (la base del 100 %) de un umbral regulatorio y no se le añadió una excepción en este cambio.
- `RutasDelCatalogoDeDatos` es el primer corte en cadena de `auditoria`; si el controlador vuelve a crecer, el siguiente corte natural es la salud/outbox/incidentes de Sistemas.

## Decisiones y ambigüedades
- Que el 99,5 % salga solo de `application.yml` (y no de una tabla de catálogo sembrada): es la lectura mínima de «umbrales son catálogo» que no inventa tabla ni migración, porque el valor ya vive en configuración desde la decisión de Pablo del 2026-10-29. Si se prefiere una tabla `auditoria.parametro`, es un cambio de esquema aparte. A confirmar con **Pablo**.
- Que `BigDecimal.TEN.pow(2)` es aceptable para la base 100. A confirmar con quien mantiene el barrido (`plataforma/comun-pruebas`).
