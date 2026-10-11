# Deuda de resiliencia · falta circuit breaker en RecaudoPorHttp, RespaldoPorHttp y HechosPorHttp

Carril S2 (seguridad transversal y operación) · regla 98.2.3 («circuit breaker hacia toda dependencia
inter-servicio, con su fallback declarado») y 98.8.4 («prueba de caída del dependido»).
Estado: **DIAGNOSTICADO + PROPUESTA + PRUEBA ESCRITA COMO PLAN**. No se editó ningún servicio.

## 1 · Qué hay hoy (con archivo y línea)

| Cliente | Archivo | Llama a | Timeout | Reintento | Circuit breaker | Fallo de la llamada |
|---|---|---|---|---|---|---|
| `RecaudoPorHttp` (entregas) | `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RecaudoPorHttp.java:40` | aportes `GET /aportes/periodos/{id}/recaudo` | sí (1 s conexión, 3 s lectura, `ConfiguracionDeClientes`) | no | **no** (el propio Javadoc lo declara: «No hay circuit breaker todavía») | `Optional.empty()` → quien pregunta no avanza |
| `RecaudoPorHttp` (garantia) | `servicios/garantia/src/main/java/bo/aportaya/garantia/infraestructura/RecaudoPorHttp.java:43` | idem | idem | no | **no** | idem (AP-CU23-12 si aportes cae) |
| `RespaldoPorHttp` (entregas) | `servicios/entregas/src/main/java/bo/aportaya/entregas/infraestructura/RespaldoPorHttp.java:39` | garantia `POST /garantia/respaldo/coberturas` | sí | no (la clave de idempotencia sale del turno, el llamador reintenta) | **no** | `Optional.empty()` → «no se sabe si cubrió, se reintenta» |
| `HechosPorHttp` (grupos) | `servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java:64-224` | 8 servicios (organizador, tarifas, cumplimiento, aportes, transparencia, garantia, notificaciones, identidad) | sí | no | **no** | cada método traduce la falta a «lo que no deja pasar» (deniega), salvo reputación (`tieneHistorial=false`) |

Lo que sí está bien: el **tiempo máximo** existe en todos (un cliente colgado no arrastra al que pregunta) y
el fallo se traduce a denegar por omisión (invariante 9). Lo que falta es el **segundo escalón**: después de
N fallos seguidos, dejar de llamar un rato. Sin él, un `aportes` caído hace que cada pedido a `entregas`
espere 1 s (conexión rechazada) a 3 s (lectura) antes de fallar, ocupando un hilo, y todos los pedidos
nuevos pagan esa espera.

Precedentes en el repo (para seguir el patrón existente y no inventar otro):

- `servicios/nucleo-financiero/.../CotizadorPorHttp.java:6,37` — `@CircuitBreaker` de resilience4j con
  configuración en `application.yml:79-` y runbook `docs/operacion/provider-timeout.md`.
- `servicios/inversiones/.../LibroPorHttp.java:53` y `AliadoSimuladoHttp.java:64` — `CircuitBreaker.of(...)` programático.
- `resilience4j-spring-boot3` 2.3.0 ya está en `gradle/libs.versions.toml:17,88`; `entregas` ya lo declara
  (`servicios/entregas/build.gradle.kts:35`).

## 2 · Propuesta

**Un solo lugar, dos pasos.** `ClienteDeServicio` (`plataforma/comun-web/.../clientes/ClienteDeServicio.java`)
es el cliente de seis de los siete usos (RecaudoPorHttp ×2, HechosPorHttp de grupos, HechosDeGruposPorHttp de
aportes, PaquetePorHttp, HechosPorHttp de nucleo-financiero). Agregarle un `CircuitBreaker` por destino en
`blindar(...)` arregla esos seis sin tocar a sus dueños. Los que usan `RestClient` directo
(`RespaldoPorHttp`, `TitularPorHttp`, y el `identidad` crudo de `HechosPorHttp:~224`) hay que migrarlos a
`ClienteDeServicio` (o envolverlos) — eso sí es edición de cada dueño.

| Parámetro | Valor propuesto | Por qué |
|---|---|---|
| Ventana | 10 llamadas deslizantes | suficiente para distinguir una caída de un error aislado |
| Umbral de fallos | 50 % | una de cada dos falla → dejar de insistir |
| Espera con el circuito abierto | 30 s | tiempo de que un orquestador reinicie un contenedor |
| Llamadas de prueba en semiabierto | 3 | confirmar la recuperación sin inundar al recién levantado |
| Qué cuenta como fallo | `ResourceAccessException` (red, timeout) y 5xx; **no** 4xx (es respuesta del negocio) | un 404/422 no es una caída |

**Los valores son sintéticos** (marcarlos así en la configuración; fijarlos es decisión del dueño de cada
servicio con su tráfico real). Se leen de `application.yml` (`resilience4j.circuitbreaker.instances.<destino>`),
no del código (invariante 10).

**Fallback declarado por dependencia** (qué responde la API cuando el dependido está caído; regla 98.2.4):

| Dependido | Con el circuito abierto | ¿Aceptable? |
|---|---|---|
| aportes (recaudo del pozo) | `Optional.empty()` → entregas/garantia **no avanzan** (el pozo no se puede afirmar) | Sí: dinero, se deniega por omisión; el usuario ve «procesando» (98.4.1) |
| garantia (cobertura) | `Optional.empty()` → se reintenta con la **misma** clave de idempotencia del turno | Sí: el reintento no duplica |
| organizador, tarifas, cumplimiento, garantia (hechos para grupos) | deniega (no habilitado / sin tarifario / sin licencia / restringido) | Sí: es lo que ya hace con un timeout |
| transparencia (reputación) | `tieneHistorial=false`, y decide quien pregunta | Sí, ya declarado en el Javadoc |
| notificaciones (supresión) | «suprimido» (no se escribe) | Sí: ante la duda no se contacta |

## 3 · Prueba de caída del dependido — escrita como PLAN

Regla 98.8.4: se ejercita de verdad, no se mockea. La pieza reutilizable ya existe y está probada:
`plataforma/comun-pruebas/.../DependidoDeEnsayo.java` (un servidor en loopback que se **cae** y **vuelve** en el
mismo puerto y **cuenta las peticiones que llegaron**; `DependidoDeEnsayoTest` demuestra que se cae de verdad).
Cada dueño escribe la prueba contra SU cliente, en `src/test/java/.../infraestructura/<Cliente>CaidaTest.java`
(corre en `test`, sin Docker), con esta estructura de tres niveles (regla 65):

```java
// Correcto: el dependido vive y responde → el cliente devuelve el valor y el circuito sigue CERRADO.
try (var aportes = new DependidoDeEnsayo().responder(200, RECAUDO_JSON)) {
    var cliente = new RecaudoPorHttp(new ClienteDeServicio(RestClient.builder(), aportes.urlBase(), "aportes"));
    assertThat(cliente.consultar(PERIODO)).isPresent();
}

// Limite: se cae a mitad de camino. Las N primeras llamadas FALLAN (y llegan al socket), la N+1 ya NO llega.
try (var aportes = new DependidoDeEnsayo().responder(200, RECAUDO_JSON)) {
    var cliente = ...;
    aportes.caer();
    for (int i = 0; i < VENTANA; i++) assertThat(cliente.consultar(PERIODO)).isEmpty();   // fallan: connection refused
    long t0 = System.nanoTime();
    assertThat(cliente.consultar(PERIODO)).isEmpty();                                    // circuito ABIERTO
    assertThat(Duration.ofNanos(System.nanoTime() - t0)).isLessThan(Duration.ofMillis(50)); // sin red: falla al instante
    // el contador del dependido no crece con el circuito abierto (no se llamo)
}

// Invalido: 5xx y respuestas rotas cuentan como fallo; un 404/422 NO abre el circuito.
//   .responder(500, "{}")        → abre tras la ventana
//   .responder(200, "no es json") → vacio, cuenta como fallo
//   .responder(404, "{}")         → vacio, NO cuenta: el circuito sigue cerrado tras 3×VENTANA llamadas

// Recuperacion: tras la espera, UNA llamada de prueba (semiabierto) llega; si responde, el circuito cierra.
aportes.levantar();
Thread.sleep(ESPERA_ABIERTO + margen);       // la espera sale de la configuracion de la prueba, no de 30 s reales
assertThat(cliente.consultar(PERIODO)).isPresent();
assertThat(aportes.peticionesRecibidas()).isEqualTo(llamadasDePrueba);
```

Y para `RespaldoPorHttp`, además: **la caída no duplica la cobertura**: con el circuito abierto el llamador reintenta
con la misma `Idempotency-Key` (`nameUUIDFromBytes("cobertura-del-turno:"+turnoId)`), y al volver el dependido
una sola cobertura queda registrada (prueba de duplicado, 98.8.3).

## 4 · Estado de la evidencia

| Pieza | Estado |
|---|---|
| Diagnóstico con archivo:línea | HECHO (esta tabla; lectura de código, peldaño DISCOVERED) |
| Doble reutilizable `DependidoDeEnsayo` + su prueba | ver `gradle-comun-pruebas-*.log` (WRITTEN → TESTED según la corrida) |
| Circuit breaker en `ClienteDeServicio` | **NO implementado** (edita `comun-web` y los servicios ajenos) — dueño: plataforma + cada servicio |
| `RecaudoCaidaTest` / `RespaldoCaidaTest` / `HechosCaidaTest` | **NO escritos** (plan arriba); se escriben contra el cliente real cuando exista el breaker |

**Decisión de negocio que NO se cerró:** si ante un `aportes` caído conviene permitir a `entregas` seguir con el
último recaudo conocido (cache) en vez de bloquear. Hoy se bloquea (más seguro, 91.3). A confirmar con Tesorería/Dirección.
