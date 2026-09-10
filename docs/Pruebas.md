---
tags:
  - pruebas
  - calidad
titulo: "Pruebas — cómo se prueba AportaYa"
---

# Pruebas

> La pregunta de este documento no es «¿qué porcentaje tenemos?». Es la de
> [[ADR-026 Pruebas de un sistema distribuido]]: **¿qué del dinero no está probado?**

Un porcentaje alto conseguido excluyendo lo difícil la contesta al revés.

---

## 1 · Los seis corredores

Cada uno tiene su comando, su velocidad y su motivo. Ninguno reemplaza a otro.

| Corredor | Qué prueba | Contra qué | Necesita Docker | Nombre del archivo |
| --- | --- | --- | :-: | --- |
| `test` | átomos: cálculo, plazos, objetos de valor | nada | no | `<Atomo>Test` |
| `webTest` | **contrato HTTP**: ruta, estado, JSON, validación, guardia | corte MVC, casos de uso doblados | **no** | `<Controlador>WebTest`, `SeguridadWebTest` |
| `integrationTest` | casos de uso, repositorios, arranque | PostgreSQL 16 real | sí | `CU<NN>Test`, `<Repo>Test`, `ArranqueTest` |
| `contractTest` | el par productor/consumidor | contratos verificados | sí | `<Servicio>ContratoTest` |
| `sagaTest` | fallo parcial y compensación | dobles de los servicios | sí | `<Saga>Test` |
| `e2eTest` | el stack entero | compose `--profile todo` | sí | `<Flujo>E2ETest` |

```bash
./gradlew test webTest          # la suite rápida: corre en cada guardado, sin Docker
./gradlew verificar             # lo mismo que corre el CI, en un comando
```

**La suite rápida es la que se corre.** Si probar el `403` de un endpoint exigiera
levantar PostgreSQL, nadie lo probaría — y una prueba de seguridad que no se corre no
existe. Por eso `webTest` no toca la base.

---

## 2 · Qué va en cada nivel, y por qué no en el otro

La regla es una: **cada cosa se prueba donde puede fallar de verdad.**

```
        petición HTTP
              │
              ▼
     ┌────────────────┐   ← webTest: ¿llega? ¿con qué forma? ¿se le permite?
     │    guardia     │
     │   validación   │
     │  controlador   │
     └────────┬───────┘
              │
              ▼
     ┌────────────────┐   ← integrationTest: ¿la regla se cumple? ¿cuadra?
     │  caso de uso   │
     └────────┬───────┘
              │
              ▼
     ┌────────────────┐   ← integrationTest: ¿la restricción RECHAZA?
     │   PostgreSQL   │
     └────────────────┘
```

| Se prueba | Dónde | Y **no** en |
| --- | --- | --- |
| El cálculo de una comisión, un plazo, una transición de estado | `<Atomo>Test` | ningún otro: es puro |
| Que la restricción `R-XXX-nn` rechaza | `CU<NN>Test`, contra la base | el corte MVC: un doble siempre acepta |
| Que dos aportes con la misma clave producen un pago | `CU<NN>Test` | — |
| Que el cuadre da `0.00` al centavo | `CU<NN>Test` | — |
| **Que sin sesión responde `401`** | `SeguridadWebTest` | integración: nunca pasa por la guardia |
| **Que sin el permiso responde `403`** | `SeguridadWebTest` | ídem |
| **Que un JSON roto es `400` y no `500`** | `<Controlador>WebTest` | ídem: no pasa por el `DispatcherServlet` |
| **Que el dinero sale como cadena decimal** | `<Controlador>WebTest` | ídem: no pasa por Jackson |
| **Que la respuesta no filtra la contraseña** | `SeguridadWebTest` | ídem |
| Que el proceso levanta con su configuración real | `ArranqueTest` | — |

> **Prohibido:** doblar el repositorio en `CU<NN>Test`. Lo que hay que probar ahí es que
> la restricción rechaza, y un doble siempre acepta ([[ADR-026 Pruebas de un sistema distribuido]]).
>
> **Correcto:** doblar el caso de uso en `<Controlador>WebTest`. Lo que se prueba ahí es
> si la petición **llega o no llega** a él ([[ADR-043 Capa web probada con MockMvc]]).

---

## 3 · Escribir una prueba de la capa web

### La sábana del servicio — se escribe una vez

```java
@PruebaWeb                                    // sin controladores: los carga TODOS
class SeguridadWebTest extends SabanaDeSeguridadWeb {
    @MockitoBean private CU22LiquidarEntrega cu22;
    @MockitoBean private CU28EmitirDesembolso cu28;
}
```

Hereda seis afirmaciones sobre **todas** las rutas del servicio, presentes y futuras:

1. sin sesión, lo que no sea `@Publico` responde `401`;
2. autenticado sin el permiso, lo que declare `@Permiso` responde `403`;
3. **el permiso de una operación no abre la de al lado** — barrido cruzado: cada ruta se
   pide con los permisos de *todas las demás* del servicio y tiene que seguir dando
   `403`. Es la diferencia entre «la guardia existe» y «la guardia distingue»;
4. **sin `Idempotency-Key`, ninguna operación que la declare se ejecuta** — se pide *con*
   el permiso correcto, para que un `403` no tape que la clave no se estaba exigiendo;
5. la negativa no nombra el permiso que falta;
6. ninguna respuesta de error filtra trazas, SQL, paquetes ni secretos.

Un controlador nuevo entra a la sábana **el día que se escribe**, sin tocar esta clase.

### La matriz de un controlador

```java
@PruebaWeb(EntregasController.class)
class EntregasControllerWebTest {
    @Autowired MockMvc mvc;
    @MockitoBean CU22LiquidarEntrega cu22;

    @Test
    @DisplayName("201 con la liquidacion, y el dinero como cadena decimal")
    void caminoFeliz() throws Exception {
        when(cu22.liquidar(any(), any())).thenReturn(...);

        mvc.perform(post("/entregas")
                .with(Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR"))
                .header("Idempotency-Key", CLAVE)
                .contentType(APPLICATION_JSON).content(CUERPO))
           .andExpect(status().isCreated())
           .andExpect(jsonPath("$.neto.monto").value("1150.00"));
    }
}
```

### Fabricar una sesión

```java
Sesiones.como("ORGANIZADOR", "ENTREGA_EJECUTAR")   // rol + permisos efectivos
Sesiones.comoOtro("PARTICIPANTE", "BILLETERA_VER") // otro usuario: probar IDOR
Sesiones.sinPermisos()                             // autenticado y sin poder nada
Sesiones.sinRol()                                  // token válido y contenido incompleto
// sin .with(...)                                  // sin sesión
```

**No se firma un JWT de verdad.** Hacerlo ataría la suite de trece servicios al emisor
del catorceavo. Que la firma se valide bien lo prueba `ArranqueTest`, contra el
decodificador real.

---

## 4 · Matriz endpoint → escenarios

Para **cada** endpoint. Las cuatro primeras las cubre la sábana automáticamente; el
resto se escribe en `<Controlador>WebTest`.

| Escenario | Esperado | Quién lo cubre |
| --- | :-: | --- |
| sin sesión | `401` | sábana |
| autenticado sin el permiso | `403` | sábana |
| con el permiso de OTRA operación del servicio | `403` | sábana |
| sin `Idempotency-Key` (si la declara) | `400` | sábana |
| el error no nombra el permiso | — | sábana |
| el error no filtra internos | — | sábana |
| petición válida | `200` / `201` / `202` / `204` | matriz |
| reintento con la misma clave | `200` (no `201`, no `409`) | matriz |
| JSON corrupto | `400` | plataforma |
| cuerpo vacío | `400` | plataforma |
| campo obligatorio ausente | `400` | matriz |
| enum fuera del contrato | `400` | matriz |
| fecha / UUID mal formados | `400` | plataforma |
| parámetro con restricción violada | `400` | plataforma |
| cabecera obligatoria ausente | `400` | plataforma |
| verbo equivocado | `405` | plataforma |
| `Content-Type` no soportado | `415` | plataforma |
| ruta inexistente | `404` | plataforma |
| regla de negocio | `422` con `AP-CU<NN>-<nn>` | matriz |
| restricción de la base | `409` con `R-XXX-nn` | plataforma |
| fallo no previsto | `500`, solo con la traza | plataforma |

«plataforma» = `ManejadorGlobalDeErroresWebTest` en `comun-web`. Se prueba **una vez**
para los catorce: el manejador es uno solo, y probarlo catorce veces sería mantener
catorce copias de la misma verdad.

### Lo que el borde NO valida, y hay que saber

El código generado desde el OpenAPI **no aplica todo lo que el contrato declara**. Las
tres diferencias están fijadas en pruebas para que nadie las descubra en producción:

| El contrato declara | ¿Lo rechaza el borde? | Dónde está escrito |
| --- | :-: | --- |
| `enum` en el **cuerpo** | sí, `400` | casi todas las matrices |
| `pattern` en un **parámetro** | sí, `400` | `PublicidadControllerWebTest` |
| `enum` en un **parámetro** | **no**, llega crudo | `IndicadoresControllerWebTest` |
| `required` de un objeto/arreglo | **no**, llega vacío | `ReportesControllerWebTest`, `ReputacionControllerWebTest` |

Las dos últimas las tiene que rechazar el caso de uso, y por eso salen `422` y no `400`.
Las pruebas fijan ese reparto: si el generador cambiara de comportamiento, fallan — y la
decisión de quién rechaza se toma a propósito en vez de descubrirse.

### Lo que además hay que probar cuando aplica

| Si el endpoint… | Probar |
| --- | --- |
| mueve dinero | que el monto viaja como **cadena** decimal, no como número |
| toca un recurso de alguien | que otro usuario recibe `403`/`404` (IDOR) |
| separa autorizar de ejecutar | que el permiso de uno **no** sirve para el otro |
| devuelve datos de una persona | que no salen contraseña, hash, pimienta ni tokens |
| acepta un permiso delegado | que sale del **token**, nunca del cuerpo |
| tiene efecto | que sin `Idempotency-Key` no se ejecuta |

---

## 5 · Matriz servicio → reglas

Cada `R-XXX-nn` citado por un caso de uso necesita su prueba de **rechazo**, contra la
base. Lo verifica `scripts/verificar_criterios.py`, no una revisión.

Cada `@Permiso("X")` necesita que `X` exista en el catálogo sembrado (`rol` o
`permiso`). Lo verifica `scripts/verificar_pruebas_web.py`: un código inexistente deja
el endpoint inalcanzable para todos, con un `403` que nadie sabe explicar, y el arranque
no lo puede detectar porque el catálogo está en la base.

---

## 6 · Matriz cobertura → criticidad

Los pisos de [[ADR-026 Pruebas de un sistema distribuido]]:

| Ámbito | Líneas | Ramas |
| --- | :-: | :-: |
| Global | 80 % | 70 % |
| `dominio/` de dinero y cumplimiento | 95 % | 95 % |
| `aplicacion/` de esos mismos | 90 % | — |
| Criterios de aceptación de cada CU | **100 %** | |
| Restricciones citadas | **100 %** | |

Se declaran por módulo, en su `build.gradle.kts`:

```kotlin
extra["pisoDeCobertura"] = 0.89   // líneas del módulo
extra["pisoDeRamas"]     = 0.71   // ramas del módulo
extra["pisoDelDominio"]  = 0.71   // líneas Y ramas de dominio/
```

**Los diecinueve módulos tienen su piso puesto**, fijado con evidencia y no copiado de
este documento: se midió con la suite completa, se redondeó hacia abajo al 1 % y se le
restó dos puntos de margen. Es un **trinquete**: bloquea la caída y no rompe el build por
un refactor legítimo.

> **El trinquete todavía no llega al objetivo de ADR-026, y hay que decirlo.** El piso
> global de líneas se cumple en catorce de diecinueve módulos, pero el de `dominio/` de
> los servicios de dinero está entre 0,60 y 0,75 — lejos del 95 % que fija el ADR, porque
> lo que baja el número son las **ramas**, no las líneas (`dominio` cubre 79–100 % de
> líneas y 62–78 % de ramas). El trinquete impide que empeore; subirlo hasta el ADR es
> trabajo de escribir los casos límite que hoy faltan, no de cambiar el número.

`pisoDelDominio` se aplica **solo al paquete `dominio`**, sin sus subpaquetes: la regla de
JaCoCo corre paquete por paquete y `dominio.puertos` son interfaces —cero líneas
ejecutables, cero por ciento siempre—. Exigirles el piso del cálculo del dinero sería
exigirle cobertura a una declaración.

Sin declaración no hay piso, y la regla no corre. Para revisarlos o subirlos:

```bash
./gradlew test webTest integrationTest
./gradlew jacocoTestReport
python3 scripts/cobertura.py
```

### Qué se excluye, y qué no

Se excluye **solo** el código generado (`**/generado/**`, `**/generated/**`): jOOQ
produce ~110 clases por servicio y el generador de OpenAPI otras tantas. Con ellas
adentro, `aportes` medía 12 % teniendo su dominio al 84 % — un número que mide cuántas
tablas tiene el esquema, no qué está probado.

**No se excluye** nada más. Ni servicios, ni controladores, ni validadores, ni cálculo
financiero, ni manejo de errores. Excluir lo difícil para subir el porcentaje contesta
la pregunta al revés.

---

## 7 · Reglas que no se negocian

| Regla | Por qué |
| --- | --- |
| Ninguna prueba `@Disabled` ni saltada | El build las cuenta y falla. Una prueba saltada miente mejor que una que falta |
| Ningún `Thread.sleep` | La cola se drena de forma determinista; el reloj se inyecta |
| Ningún `Instant.now()` en una prueba | `Reloj.fijo(...)`; en el corte MVC ya viene parado |
| Ninguna llamada a internet | Dobles que reproducen el fallo real: timeout, duplicado, fuera de orden |
| Ninguna prueba depende de otra | Cada una prepara su estado; corren en cualquier orden |
| Ningún `assertNotNull` como única afirmación | La prueba tiene que fallar cuando el comportamiento cambia |
| Ninguna base en memoria | El modelo usa `EXCLUDE`, `btree_gist`, RLS y `numeric` |
| Ninguna prueba se desactiva para que pase el CI | El número de pruebas activas nunca baja |

---

## 8 · Definición de terminado de una funcionalidad

```
[ ] Compila, y `./gradlew spotlessCheck check` pasa.
[ ] Cada criterio de aceptación de la bóveda tiene su prueba, con el mismo nombre.
[ ] Cada R-XXX-nn citado tiene su prueba de RECHAZO, contra la base.
[ ] Prueba de reintento: misma clave ⇒ misma respuesta, un solo efecto.
[ ] Prueba de concurrencia: dos transacciones ⇒ una gana, nunca doble efecto.
[ ] Prueba de cuadre si mueve dinero: débitos = créditos, al centavo.
[ ] Prueba de vencimiento si hay plazo legal, con el reloj inyectado.
[ ] El controlador tiene su matriz en <Controlador>WebTest.
[ ] El servicio tiene su SeguridadWebTest (la sábana lo cubre solo).
[ ] Evento duplicado y fuera de orden ⇒ un solo efecto, en todo consumidor.
[ ] Compensación probada en toda operación que cruza servicios.
[ ] `python3 scripts/verificar_criterios.py` y `verificar_pruebas_web.py` devuelven 0.
[ ] La cobertura del módulo no baja.
[ ] Todo bug corregido llega con la prueba que lo reproduce.
```

## 9 · La regla del bug

```
Bug → prueba ROJA que lo reproduce → arreglo → prueba VERDE → merge
```

La prueba se queda para siempre. Dos ejemplos en este repositorio, los dos de
[[ADR-043 Capa web probada con MockMvc]]:

- `GuardiaDePermisoWebTest` — `@Permiso` no autorizaba nada.
- `ManejadorGlobalDeErroresWebTest.cabeceraObligatoriaQueFalta` — una `Idempotency-Key`
  olvidada devolvía `500`.

## Ver también

[[ADR-026 Pruebas de un sistema distribuido]] · [[ADR-043 Capa web probada con MockMvc]] · [[ADR-019 Dinero con BigDecimal]] · [[ADR-024 Autenticación y sesión distribuida]] · [[Restricciones]]
