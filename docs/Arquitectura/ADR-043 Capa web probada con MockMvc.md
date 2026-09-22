---
tags:
  - arquitectura
  - adr
titulo: "ADR-043 — La capa web se prueba con MockMvc, y @Permiso se hace cumplir"
estado: aceptada
fecha: 2026-09-04
---

# ADR-043 — Capa web probada con MockMvc

> Extiende a [[ADR-026 Pruebas de un sistema distribuido]], que ya listaba el nivel de
> API. Lo que cambia es **cómo** se implementa y **dónde** corre.

## Contexto

ADR-026 declaró seis niveles de prueba y uno de ellos —API, `@SpringBootTest` +
MockMvc— nunca se escribió. Al 2026-09-04, con 32 controladores y 153 endpoints en
producción, la capa web tenía **cero** pruebas.

No era un hueco cosmético. Escribir la suite encontró **cuatro defectos**, y ninguno
podía aparecer en otro nivel:

**Uno.** `@Permiso("ENTREGA_AUTORIZAR")` estaba en los 32 controladores, el token ya
traía los permisos que CU-08 calculaba, `TodoEndpointDecideSuAcceso` comprobaba al
arrancar que cada endpoint declarara algo… y **nadie leía el valor de la anotación en
una petición**. La cadena de seguridad exigía sesión y nada más. Cualquier participante
autenticado podía llamar `ENTREGA_AUTORIZAR`, `REVERSO_AUTORIZAR` o aprobar su propio
reembolso. La declaración existía; la autorización no.

**Dos.** Una cabecera obligatoria ausente salía como **500**. `Idempotency-Key` es
obligatoria en toda operación con efecto, y olvidarla devolvía un fallo del servidor por
algo que el cliente arregla solo — y escribía un ERROR en la bitácora por cada petición
mal armada, que es como una alerta real se pierde entre el ruido.

**Tres.** Una **cabecera obligatoria ausente** salía como `500`. `Idempotency-Key` es
obligatoria en toda operación con efecto, y olvidarla devolvía un fallo del servidor por
algo que el cliente arregla solo — y escribía un ERROR en la bitácora por cada petición
mal armada, que es como una alerta real se pierde entre el ruido.

**Cuatro.** Un **parámetro que viola su restricción del contrato** —un `pattern`, un
`minimum`— también salía como `500`. La violación no llega como excepción de Spring MVC
sino como `ConstraintViolationException` de Jakarta, porque la lanza el proxy de
`@Validated` de la interfaz generada, y el manejador global no la contemplaba. Afectaba
a **todo endpoint con parámetros restringidos**.

Los cuatro son invisibles desde abajo. Una prueba de caso de uso llama al caso de uso
**directamente**: nunca pasa por la guardia, ni por el deserializador, ni por el
manejador de errores. Una prueba de contrato compara esquemas, no comportamiento. El
E2E los habría encontrado, tarde y con el peor mensaje de error posible.

## Decisión

**El nivel de API se implementa como corte MVC (`@WebMvcTest`) con dobles del caso de
uso, en su propio corredor `webTest`, y sin contenedor.**

Y, como consecuencia inseparable: **`@Permiso` se hace cumplir en tiempo de petición**,
con `GuardiaDePermiso` en `comun-web`.

### Corte MVC, y no `@SpringBootTest`

ADR-026 decía `@SpringBootTest` sobre la base de Testcontainers. Se cambia por tres
razones:

| | `@SpringBootTest` + Testcontainers | `@WebMvcTest` |
| --- | --- | --- |
| Qué hace falta para correrlo | Docker | nada |
| Cuánto tarda | segundos por clase, más el arranque de PostgreSQL | milisegundos |
| Qué prueba de más | el caso de uso, que **ya** se prueba en `CU<NN>Test` | — |

El tercer punto es el que decide. Repetir el caso de uso en este nivel no agrega
garantía: la agrega `CU<NN>Test`, contra PostgreSQL real, que es donde viven las
restricciones. Lo que **solo** existe acá es el contrato HTTP —ruta, verbo, estado,
JSON, validación, manejador de errores, guardia— y para eso la base no aporta nada.

El primero es el que lo hace usable. Una suite que necesita Docker es una suite que no
corre en cada guardado, y una prueba de seguridad que nadie corre es una prueba que no
existe.

Se conserva `ArranqueTest` (`@SpringBootTest`, con Testcontainers) por servicio: es lo
único que comprueba que el proceso **levanta** con su configuración real, su
decodificador de token y su cableado. Los dos niveles son complementarios y ninguno
reemplaza al otro.

### La sábana de seguridad

Cada servicio hereda `SabanaDeSeguridadWeb` declarando sus dobles. Sin listar
controladores: el corte carga **todos** los del servicio, lee el mapeo de MVC y barre
cada ruta que encuentra.

De ahí salen seis afirmaciones que **no se escriben por endpoint** y por eso no se
pueden olvidar para uno nuevo:

1. sin sesión, toda ruta que no sea `@Publico` responde `401`;
2. autenticado sin el permiso, toda ruta con `@Permiso` responde `403`;
3. **el permiso de una operación no abre la de al lado**: cada ruta se pide con los
   permisos de *todas las demás del servicio* y tiene que seguir dando `403`. Es la
   diferencia entre «la guardia existe» y «la guardia distingue», y los permisos de al
   lado se leen del mapeo, así que un endpoint nuevo entra al barrido cruzado de todos
   los demás el día que se escribe;
4. **sin `Idempotency-Key`, ninguna operación que la declare obligatoria se ejecuta** —
   se pide *con* el permiso correcto, para que un `403` no tape que la clave no se
   estaba exigiendo;
5. la negativa no nombra el permiso que falta —decirlo ya cuenta que el recurso existe—;
6. ninguna respuesta de error filtra trazas, SQL, nombres de paquete ni secretos.

La sesión del `403` usa un rol que el catálogo **no tiene** y cero permisos. Con un rol
real, la sábana pasaría por casualidad justo en los endpoints de ese rol.

### `@Permiso` se resuelve contra dos catálogos

El valor de la anotación es un código del catálogo, y el catálogo tiene dos tablas:
`rol` (15 códigos) y `permiso` (32). De los 33 códigos usados, 9 son de `rol` y 24 de
`permiso`; **ninguno está en las dos**. La petición pasa si el token trae ese permiso
entre sus efectivos, o si su rol es exactamente ese código.

Que un `@Permiso` nombre un código inexistente dejaría el endpoint inalcanzable para
todo el mundo, con un `403` que nadie sabe explicar. El arranque no lo puede comprobar
—el catálogo está en la base, no en el classpath—, así que lo comprueba
`scripts/verificar_pruebas_web.py` contra las semillas.

### Qué se prueba en cada nivel, sin superposición

| | Dónde | Qué |
| --- | --- | --- |
| El cálculo, el plazo, el estado | `<Atomo>Test` | funciones puras |
| La regla, la restricción, el cuadre | `CU<NN>Test` | contra PostgreSQL real |
| **Ruta, estado, JSON, validación, guardia** | **`<Controlador>WebTest`** | **corte MVC** |
| Que el proceso levanta | `ArranqueTest` | contexto completo |

Un doble del caso de uso en `CU<NN>Test` está prohibido y lo sigue estando ([[ADR-026 Pruebas de un sistema distribuido]]). Un doble del caso de uso en la capa web es lo
correcto: lo que se prueba es que la petición **llega o no llega** a él.

## Motivo

**Porque la anotación sin guardia era peor que no tener anotación.** Treinta y dos
controladores declaraban su permiso y una revisión de código veía la declaración. Nadie
tenía motivo para dudar. Un sistema que parece autorizar y no autoriza es más peligroso
que uno que no lo intenta.

**Porque el contrato HTTP es la única parte del sistema que nadie más verifica.** El
compilador verifica los tipos, la base verifica las restricciones, el contrato verifica
los esquemas. Entre la petición y el caso de uso no había nadie.

**Porque una suite rápida es la que se corre.** `webTest` no levanta contenedor, no
comparte el límite de PostgreSQL y entra en `check`. Las 100+ pruebas de la capa web de
los quince módulos corren en menos de un minuto.

## Alternativas descartadas

| Alternativa | Por qué no |
| --- | --- |
| **`@SpringBootTest` + MockMvc**, como decía ADR-026 | Exige Docker para probar un `400`, y repite el caso de uso que ya se prueba contra la base. Se conserva solo en `ArranqueTest`, donde el objetivo sí es el contexto completo. |
| **`MockMvcBuilders.standaloneSetup`** | No carga la cadena de seguridad ni el manejador global. Habría dejado fuera exactamente los dos defectos que esto encontró. |
| **`@WithMockUser` de Spring Security** | Produce un `UsernamePasswordAuthenticationToken`, y `SesionDeLaPeticion` exige un `Jwt`. Probar con un principal que producción nunca ve es probar otro sistema. |
| **Firmar un JWT real en las pruebas** | Ataría la suite de trece servicios al emisor del catorceavo: cambiar el algoritmo de firma rompería suites que no tienen nada que ver con la firma. |
| **Hacer cumplir `@Permiso` con `@PreAuthorize`** | Exige SpEL en cada método y activar la seguridad por método en los catorce servicios. Un interceptor que lee la anotación que **ya existe** no agrega nada que mantener. |
| **Una prueba de seguridad escrita por endpoint** | 153 endpoints, y la 154 se olvida. La sábana barre lo que el mapeo tenga. |

## Consecuencias

**A favor**

- El `403` del autenticado sin permiso es una prueba, en los quince módulos.
- Un controlador nuevo entra a la sábana el día que se escribe, sin tocar ninguna prueba.
- La capa más expuesta se verifica sin Docker, así que corre en cada guardado.

**En contra, y hay que asumirlo**

- **`GuardiaDePermiso` cambia el comportamiento en producción.** Peticiones que antes
  pasaban ahora dan `403`. Es lo correcto y hay que decirlo: cualquier cliente que
  dependiera del agujero deja de funcionar. La colección de humo tiene que correr con
  tokens cuyos roles tengan los permisos que el endpoint declara.
- **La matriz por controlador es trabajo por controlador.** Los treinta y uno la tienen,
  y la lista de deuda de `scripts/verificar_pruebas_web.py` quedó vacía. Se conserva el
  mecanismo: un controlador nuevo sin prueba tiene que entrar ahí con nombre y apellido,
  en vez de que alguien afloje el gate.

- **Veinticinco operaciones devuelven un estado HTTP que su contrato no declara.** Casi
  todas son el mismo patrón: el código responde `201` cuando la operación produjo el
  efecto y `200` cuando un reintento devolvió el que ya existía —que es lo que hace que
  reintentar sea seguro sin adivinar— y el OpenAPI declara **uno** de los dos. El cliente
  generado solo conoce el declarado. **No se resolvió acá**: o el contrato declara los dos
  estados, o el controlador deja de distinguirlos, y lo segundo perdería información que
  hoy el cliente usa. Es una decisión de contrato. Hasta que se tome, viven en
  `DIVERGENCIA_DE_ESTADO` del gate, que impide que la lista crezca, y las matrices fijan
  la conducta **real** —la que ve el cliente— y no la declarada.

- **El código generado no aplica todo lo que el contrato declara.** Un `enum` en el
  cuerpo se rechaza con `400`; un `enum` en un parámetro de consulta **no** —el generador
  lo produce como `String`— y un `required` de objeto o arreglo tampoco, porque el modelo
  lo inicializa vacío y `@NotNull` nunca dispara. Esos dos los rechaza el caso de uso, y
  salen `422`. Las pruebas fijan ese reparto en vez de taparlo: si el generador cambiara,
  fallan, y la decisión de quién rechaza se toma a propósito.
- **Un doble puede divergir del caso de uso real.** Se acota: lo que el doble devuelve no
  decide nada de negocio, solo qué JSON sale. La regla la sigue probando `CU<NN>Test`.

## Cómo se verifica

- [ ] `./gradlew webTest` pasa en los quince módulos.
- [ ] `python3 scripts/verificar_pruebas_web.py` devuelve 0.
- [ ] Cada servicio con controladores tiene su `SeguridadWebTest`.
- [ ] Cada `@Permiso` nombra un código de `rol` o de `permiso` del catálogo sembrado.
- [ ] Cada controlador tiene su `<Controlador>WebTest`, y `DEUDA` está vacía.
- [ ] Apagar `GuardiaDePermiso` pone en rojo los quince módulos. *(Es la comprobación
      que dice que la sábana está viva; se hizo al escribirla.)*

## Ver también

[[ADR-026 Pruebas de un sistema distribuido]] · [[ADR-024 Autenticación y sesión distribuida]] · [[ADR-020 Contratos OpenAPI primero]] · [[ADR-019 Dinero con BigDecimal]] · `docs/Pruebas.md`
