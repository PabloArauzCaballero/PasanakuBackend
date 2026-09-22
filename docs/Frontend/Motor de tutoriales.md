---
tags:
  - frontend
  - producto
  - accesibilidad
titulo: "Motor de tutoriales interactivos"
estado: vigente
fecha: 2026-09-17
---

# Motor de tutoriales interactivos

**Tres productos, dos motores, un solo concepto.**

| Producto | Motor | Dónde está el centro |
| --- | --- | --- |
| **Backoffice** (Angular) | `@aportaya/tutoriales` | sección **Ayuda** del menú (`/ayuda`) |
| **Sitio público** (Angular + SSR) | el mismo paquete | pestaña **Guía** (`/tutoriales`) |
| **App del participante** (Flutter) | `apps/movil/lib/dominio/tutoriales/` | quinta pestaña **Ayuda** (`/soporte/ayuda`) |

Las dos superficies web **comparten el motor entero**: el paquete `packages/tutoriales`
tiene el modelo, la validación, el registro, el motor, el velo, el globo y el centro de
tutoriales. Lo que cambia por producto entra por token: el catálogo, quién está mirando
y cómo se llama el centro.

La app tiene su propia implementación porque no comparte runtime, pero **comparte el
modelo, el vocabulario y las reglas**. Lo que se lee acá vale para los tres; donde
difieren, se dice cuál.

> **Regla cero.** Un tutorial **enseña, no opera.** Nunca envía un formulario, nunca
> confirma una operación, nunca aprueba ni borra nada. Lo único que hace por su cuenta
> es desplazar la pantalla y poner el foco en un campo. Todo lo demás lo hace una
> persona, y el servidor vuelve a decidir si puede.

## 1 · Las piezas

| Pieza | Web (`packages/tutoriales/src`) | App (`apps/movil/lib`) | Qué hace |
| --- | --- | --- | --- |
| Modelo | `tipos.ts` | `dominio/tutoriales/modelo.dart` | Los tipos. Sin DOM, sin framework |
| Validación | `validacion.ts` | `dominio/tutoriales/validacion.dart` | Revisa el catálogo entero y devuelve **todos** los problemas |
| Cuentas del avance | `avance.ts` | `dominio/tutoriales/avance.dart` | Estado, fracción, versión, requisitos pendientes |
| Registro | `registro.ts` | `dominio/tutoriales/registro.dart` | Qué tutoriales existen y cuáles se le ofrecen a quien mira |
| Motor | `motor.ts` | `dominio/tutoriales/motor.dart` | La máquina de estados del recorrido |
| Localizar el objetivo | `objetivo.ts` (`data-tutorial-id`) | `moviles/anclas_de_tutorial.dart` (`GlobalKey`) | Encontrar el elemento, incluso si todavía no existe |
| Vigilar la acción | `vigilar-accion.ts` | dentro de `capa_de_tutorial.dart` | Mirar si la persona hizo lo que el paso pedía |
| Puerto del progreso | `almacen.ts` | `dominio/puertos/almacen_de_progreso.dart` | Leer, guardar, reiniciar |
| Adaptador local | `almacen-local.ts` (`localStorage`) | `infraestructura/almacen_de_progreso_seguro.dart` (almacén seguro) | El de hoy |
| Adaptador remoto | `apps/backoffice/.../almacen-remoto.ts` | — (contrato documentado, §11) | El de mañana |
| **Quién aprende** | `identidad.ts` | `proveedores/tutoriales.dart` (`capacidadesProvider`) | El puerto que separa «operador con permisos» de «visitante sin cuenta» |
| Bitácora | `bitacora.ts` | — | Puerto de analítica, con adaptador en memoria |
| Señal de encendido | `encendido.ts` | (estado del motor) | Lo único que el shell conoce: si hay un tutorial corriendo |
| Velo | `@aportaya/ui/foco-de-tutorial` | `aportaya_diseno/moviles/foco_de_tutorial.dart` | Oscurece todo menos el elemento, con anillo |
| Globo | `@aportaya/ui/globo-de-tutorial` | `aportaya_diseno/moviles/globo_de_tutorial.dart` | Título, texto, progreso y controles |
| Anfitrión | `anfitrion-de-tutorial.ts` | `pantallas/soporte/capa_de_tutorial.dart` | Monta velo + globo, teclado/toque, foco |
| Lanzador contextual | `lanzador-de-tutorial.ts` | (pestaña Ayuda) | «Cómo funciona esta pantalla» |
| Centro de tutoriales | `centro/` | `pantallas/soporte/` | Listado, búsqueda, filtros, avance |
| Catálogo | `apps/backoffice/.../rutas/ayuda/catalogo/`<br>`apps/web/src/app/tutoriales/catalogo/` | `pantallas/soporte/catalogo/` | **Los tutoriales, como datos** |

### Lo que cada producto enchufa

Un producto web nuevo necesita exactamente cinco `provide` (ver
`apps/web/src/app/tutoriales/proveer.ts`, que es el ejemplo más corto):

| Token | Qué se le pasa |
| --- | --- |
| `CARGADORES_DE_TUTORIALES` (multi) | una función que hace `import()` del catálogo |
| `RUTAS_DEL_PRODUCTO` | las rutas contra las que se valida el catálogo |
| `ALMACEN_DE_PROGRESO` | `AlmacenLocal` hoy; el remoto cuando exista |
| `IDENTIDAD_DE_TUTORIALES` | la sesión del operador, o `visitanteAnonimo` |
| `PRESENTACION_DEL_CENTRO` | título y propósito del centro, en la voz del producto |

## 2 · Flujo de ejecución

1. Alguien pulsa **Comenzar** (centro de ayuda) o **Cómo funciona esta pantalla**
   (lanzador contextual).
2. El motor busca la definición en el registro. Si no existe o no tiene pasos, no
   arranca: no hay estado a medias.
3. Publica el progreso inicial y va al paso 0.
4. Por cada paso: **navega** si el paso vive en otra ruta → **espera** al elemento →
   **desplaza y enfoca** si el paso lo pide → **engancha la escucha** de la acción
   esperada → guarda «voy por acá».
5. Avanzar solo procede si la acción del paso está cumplida. Si no, el globo dice qué
   falta, con el texto del propio paso.
6. El último paso cierra el recorrido como **completado**; salir a la mitad lo cierra
   como **omitido**, con el paso donde se fue.

```
iniciar ──► ir(0) ──► [navegar] ──► [esperar objetivo] ──► paso activo
                                          │                    │
                                   no aparece            avanzar / atrás
                                          ▼                    ▼
                                 problema + reintentar     ir(n±1) … último ──► completado
                                                                  │
                                                            salir ──► ¿a la mitad? ─sí─► confirmar ──► omitido
```

## 3 · Estructura de un tutorial

```ts
export const resolverUnExpediente: TutorialDefinicion = {
  id: 'cumplimiento-verificaciones',   // único en todo el catálogo
  version: '1.0.0',                    // cambiarla vuelve a ofrecerlo (§7)
  titulo: 'Revisar un expediente de identidad',
  descripcion: 'La cola de verificación, cómo se filtra y qué mirar en las fotos.',
  categoria: 'Cumplimiento',           // agrupa en el centro de ayuda
  ruta: '/cumplimiento/verificaciones',// enciende el lanzador de esa pantalla
  permisos: ['ver:cumplimiento'],      // se muestra solo con TODOS estos
  minutos: 6,
  dificultad: 'intermedio',
  obligatorio: true,
  requisitos: ['intro-plataforma'],    // avisan, no bloquean
  siguiente: 'cumplimiento-casos',
  pasos: [
    {
      id: 'la-cola',
      titulo: 'Esta es la cola de trabajo',
      descripcion: 'Cada tarjeta es alguien esperando su cuenta.',
      objetivo: 'cumplimiento-cola',   // el data-tutorial-id del elemento
      posicion: 'arriba',
      ruta: '/cumplimiento/verificaciones',
      esperaMs: 6000,                  // llega después de la petición
      accion: { tipo: 'clic', objetivo: 'cumplimiento-filtros' },
      ayudaSiFalla: 'Pulsá cualquiera de los botones de filtro para seguir.',
      automatica: 'desplazar',         // 'desplazar' | 'enfocar'
      bloquea: false,                  // el hueco deja pulsar de verdad
      permiso: 'VERIFICACION_RESOLVER',// el paso se salta sin este permiso
    },
  ],
}
```

En Flutter es lo mismo con nombres de Dart: `ancla` en lugar de `objetivo`, `espera`
como `Duration`, y `requiere: {Capacidad.sesion}` en lugar de `permisos`.

## 4 · Cómo crear un tutorial nuevo

**Backoffice / sitio público**

1. Escribí la constante en el archivo de su módulo, dentro de
   `apps/backoffice/src/app/rutas/ayuda/catalogo/` o de
   `apps/web/src/app/tutoriales/catalogo/` (o creá uno nuevo: uno por módulo o tema).
2. Sumala al arreglo de `catalogo.ts` de ese producto.
3. Marcá en la pantalla los elementos que el tutorial señala:
   `data-tutorial-id="mi-elemento"`. Si el elemento se repite por fila, marcá el
   contenedor, no la fila.
4. `yarn workspace @aportaya/backoffice test:front` (o `@aportaya/web`) — la prueba del
   catálogo de cada producto verifica ids únicos, rutas existentes, anclas marcadas,
   orden y ciclos.

**App**

1. La constante va en `apps/movil/lib/pantallas/soporte/catalogo/`, sumada a
   `catalogoDeTutoriales`.
2. Marcá el widget: `MarcaDeTutorial(id: 'billetera.saldo', hijo: …)`.
3. Si la ruta es nueva, agregala a `rutasConocidas` del mismo archivo.
4. `flutter test test/unidad/tutoriales` corre las mismas verificaciones.

**El motor no se toca en ninguno de los dos casos.** Si para agregar un tutorial hace
falta editar `motor.*`, `registro.*` o el anfitrión, algo se diseñó mal.

## 5 · Asociar elementos de la interfaz

- **Backoffice:** atributo `data-tutorial-id`. Nunca una clase de CSS ni un selector
  estructural: una clase cambia en el próximo retoque visual y el tutorial se rompe sin
  que nadie se entere.
- **App:** `MarcaDeTutorial`, que presta una `GlobalKey` estable del registro de anclas.
  Sin `ProveedorDeAnclas` arriba (una prueba de una pantalla suelta, un *golden*)
  devuelve el hijo tal cual: **marcar nunca cambia cómo se ve una pantalla**.
- Elementos que llegan después de una petición: `esperaMs` / `espera`. El buscador
  observa el DOM (web) o reintenta por cuadros (app) hasta el plazo.
- Elemento que no aparece: el globo muestra el problema y ofrece **Reintentar**,
  **Atrás** y **Omitir**. El tutorial **nunca** se traba ni se cierra solo.

## 6 · Validar acciones

| Acción | Qué mira | Backoffice | App |
| --- | --- | --- | --- |
| `ninguna` | nada; se sigue con «Siguiente» | ✓ | ✓ |
| `clic` / `toque` | un clic sobre el elemento resaltado | escucha `click` | mira el puntero desde la capa |
| `escribir` | `value.length >= minimo` | escucha `input` | — |
| `elegir` | valor no vacío | escucha `change` | — |
| `navegar` | la URL llegó a la ruta | eventos del router | ubicación del `GoRouter` |
| `aparezca` | el objetivo existe | espera al objetivo | espera al ancla |

Ninguna de estas escuchas dispara nada: **observan lo que la aplicación ya emite.**

## 6 bis · Las reglas del listado

Tres decisiones del centro de tutoriales que no son obvias y que se tomaron mirando
capturas, no leyendo código:

1. **El recomendado no se repite abajo.** Está arriba con su propia tarjeta; verlo dos
   veces hacía dudar de si eran dos tutoriales distintos con el mismo nombre.
2. **El recomendado desaparece mientras se busca o se filtra.** Quien escribe «zzz» y no
   encuentra nada tiene que ver que no encontró nada, no una tarjeta que no pidió.
3. **Lo abandonado a la mitad se puede continuar.** Al salir, el diálogo promete
   «guardamos por qué paso ibas». Si después la tarjeta solo ofreciera empezar de cero,
   esa promesa sería mentira. Para empezar de cero está «Reiniciar».

## 6 ter · Dónde se planta el globo

- **En la web** lo decide `ubicacion.ts`, una función pura: se prueba el lado pedido, si
  no entra el opuesto, y si ninguno entra va **centrado**, sin flecha. «Entrar» se juzga
  por lo vertical arriba y abajo, y por lo horizontal de costado: mezclar las dos
  medidas dejaba pasar globos que después se recortaban contra el borde y terminaban
  encima de lo que señalaban.
- **El globo se mide solo.** Suponer un alto fijo era un error caro: con un texto de tres
  renglones el globo mide bastante más, no entraba donde se lo había calculado y tapaba
  el elemento. Ahora se mide con `ResizeObserver` apenas existe.
- **En el teléfono** el globo se apoya abajo, ancho completo, salvo que lo que señala
  esté en la mitad de abajo de la pantalla —como la barra de pestañas—: ahí sube. Un
  globo apoyado siempre abajo tapaba justo lo que más se señala en una app.

## 7 · Versionado

`version` es una cadena libre y su única regla es: **cambiarla invalida el progreso
anterior**. Un progreso con otra versión deja de contar como completado y el tutorial
vuelve a ofrecerse, conservando `repeticiones` y la fecha de inicio. Es deliberadamente
conservador: mostrar de más un tutorial que cambió molesta; esconder un cambio que
importa deja a alguien operando con instrucciones viejas.

Cambiá la versión cuando cambie **lo que hay que hacer**. No la cambies por corregir una
coma.

## 8 · Roles, permisos y capacidades

Todo pasa por un puerto, `IdentidadDeQuienAprende`, con dos preguntas: **quién sos**
(para no mezclar avances en la misma máquina) y **qué podés** (para no ofrecer lo que no
vas a poder hacer). Cada producto lo cumple a su manera.

- **Backoffice:** `permisos` del tutorial y `permiso` del paso se resuelven con
  `Sesion.puede()`, el mismo que usa el menú. Un tutorial se muestra si la sesión tiene
  **todos** sus permisos; un paso se salta si le falta el suyo.
- **Sitio público:** `visitanteAnonimo`. No hay cuenta, no hay sujeto y `puede()`
  **niega siempre**: ningún tutorial del sitio declara permisos, y la prueba del
  catálogo lo verifica. Deny por omisión, como el resto del proyecto.
- **App:** `requiere: {Capacidad.sesion, …}`. `sesion` sale de que haya token guardado.
  `organizador` **queda declarada y en falso** hasta que la app exponga la habilitación
  del CU-90 fuera de la pantalla de crear grupo — el hueco está a la vista y tiene un
  solo lugar donde llenarse (`capacidadesProvider`).
- El filtro **oculta, no protege**. El tutorial no abre nada: señala lo que la pantalla
  ya muestra, y el servidor sigue decidiendo en cada llamada. Un catálogo manipulado, en
  el peor caso, señala un botón que da 403.

## 9 · Rutas y modales

- Un paso con `ruta` distinta a la actual hace que el motor navegue antes de buscar el
  objetivo. Si la navegación falla —por ejemplo, el rol no abre esa sección— queda un
  problema declarado con el motivo.
- Dentro de un modal: marcá el contenedor del modal y usá `accion: { tipo: 'aparezca' }`
  con el mismo id. El paso anterior debe ser el que abre el modal, con
  `accion: { tipo: 'clic' }` y `bloquea: false` para que el botón se pueda pulsar.
- Estado de la tabla en la URL: el centro de ayuda del backoffice guarda su filtro en
  `?q=`, `?estado=` y `?categoria=`, igual que cualquier tabla.

## 10 · Accesibilidad

- El globo es un `dialog` con `aria-modal="false"`: se anuncia entero al cambiar de paso
  y **no aísla** la aplicación, porque el paso siguiente suele ser «pulsá ese botón».
- Teclado (backoffice): `Escape` sale —preguntando si va por la mitad—, `→` avanza, `←`
  retrocede. Si el foco está en un campo, las flechas son del campo.
- El foco se recuerda al abrir y se devuelve al cerrar.
- El elemento resaltado lleva **anillo además de oscurecimiento**: el estado no se
  comunica solo con color ni solo con contraste.
- `prefers-reduced-motion` / `MediaQuery.disableAnimations` apagan las transiciones.
- El velo va `aria-hidden` / `ExcludeSemantics`: quien usa lector escucha el globo.
- Pruebas: `vitest-axe` en el backoffice; `androidTapTargetGuideline`,
  `labeledTapTargetGuideline` y `textContrastGuideline` en la app.

## 11 · Contrato de backend (documentado, **todavía no implementado**)

Hoy el progreso vive en el dispositivo. Cuando `identidad` exponga estas operaciones, el
backoffice cambia `ALMACEN_DE_PROGRESO` a `AlmacenRemoto` —ya escrito— y la app escribe
su adaptador equivalente. **Ningún componente cambia.**

```yaml
# servicios/identidad/src/main/resources/openapi/identidad.yaml
/tutoriales/progreso:
  get:
    operationId: listarProgresoDeTutoriales
    responses: { '200': { content: { application/json: { schema: { type: array, items: { $ref: '#/components/schemas/ProgresoDeTutorial' } } } } } }
/tutoriales/progreso/{tutorialId}:
  put:
    operationId: guardarProgresoDeTutorial   # idempotente: la misma fila dos veces deja lo mismo
    parameters: [ { name: Idempotency-Key, in: header, required: true, schema: { type: string } } ]
    responses: { '200': { … }, '400': { … }, '403': { … } }
  delete:
    operationId: reiniciarProgresoDeTutorial # idempotente: borrar lo que no está es 204
    responses: { '204': { description: sin contenido } }

components:
  schemas:
    ProgresoDeTutorial:
      type: object
      required: [tutorialId, version, estado, indice, iniciadoEn, ultimaInteraccion]
      properties:
        tutorialId:        { type: string, maxLength: 64, pattern: '^[a-z0-9-]+$' }
        version:           { type: string, maxLength: 16 }
        estado:            { type: string, enum: [pendiente, en-progreso, completado, omitido] }
        pasoId:            { type: string, maxLength: 64, nullable: true }
        indice:            { type: integer, minimum: 0 }
        iniciadoEn:        { type: string, format: date-time }
        terminadoEn:       { type: string, format: date-time, nullable: true }
        ultimaInteraccion: { type: string, format: date-time }
        repeticiones:      { type: integer, minimum: 0 }
```

Reglas del lado servidor, no negociables:

- **El dueño sale del token, nunca del cuerpo.** El cliente no manda un id de usuario;
  así nadie escribe el progreso de otro cambiando un campo.
- Tabla `identidad.progreso_tutorial` con `PRIMARY KEY (usuario_id, tutorial_id)`, RLS
  por `usuario_id` igual que el resto del esquema de `identidad`.
- `PUT` idempotente (upsert por la clave primaria) y `DELETE` idempotente.
- No es dato personal sensible, pero sí es dato de la persona: entra en la exportación
  y en el borrado de cuenta (CU de baja).

**Decisión declarada:** no se agregó la tabla a la bóveda en esta entrega. Tocar el
esquema regulado por una funcionalidad de ayuda merece su propio ADR y su propio
tramo, y el puerto ya deja el cambio en una línea.

## 12 · Cómo ejecutar las pruebas

```bash
# Backoffice
yarn workspace @aportaya/backoffice typecheck
yarn workspace @aportaya/backoffice lint          # eslint + verificar_frontend.py
yarn workspace @aportaya/backoffice test:front    # unidad + integración
yarn workspace @aportaya/backoffice test:a11y     # vitest-axe
yarn workspace @aportaya/backoffice test:e2e      # Playwright: 6 recorridos, con dev:mock levantado

# Sitio público
yarn workspace @aportaya/web typecheck && yarn workspace @aportaya/web lint
yarn workspace @aportaya/web test:front && yarn workspace @aportaya/web test:a11y
yarn workspace @aportaya/web build                # prerrenderiza /tutoriales con su lista

# Sistema de diseño web (velo, globo y la ubicación del globo)
yarn workspace @aportaya/ui test:front && yarn workspace @aportaya/ui test:a11y

# El paquete compartido: eslint propio y el barrido de reglas. Las pruebas que lo
# ejercitan viven en los dos productos que lo consumen.
yarn workspace @aportaya/tutoriales lint

# App
cd apps/movil
dart analyze --fatal-infos
flutter test test/unidad/tutoriales test/widget/tutoriales
flutter test test/a11y
```

## 13 · Diagnóstico

| Síntoma | Dónde mirar |
| --- | --- |
| «No encontramos …» en el globo | el `data-tutorial-id` / `MarcaDeTutorial` no está, o llega después del plazo: subí `esperaMs` / `espera` |
| El tutorial no aparece en el centro de ayuda | permisos del tutorial vs. los de la sesión; o el catálogo no está enchufado en `catalogo.ts` |
| El botón «Cómo funciona esta pantalla» no sale | ningún tutorial declara esa `ruta`, o el catálogo todavía no cargó (llega por `import()`) |
| La alerta «Problemas en el catálogo» | `RegistroDeTutoriales.problemas()` lista cada uno con su código; la prueba del catálogo falla por lo mismo |
| El avance no se guarda | backoffice: `localStorage` bloqueado — el globo lo avisa; app: el almacén seguro del dispositivo |
| El globo tapa el elemento | `posicion` no entra en la ventana y cae centrado: es a propósito, antes tapar que cortar el texto |

## 14 · Decisiones tomadas

1. **Los tutoriales son datos, no código.** Agregar uno es agregar una constante y
   sumarla a una lista.
2. **Dos motores, un modelo.** Angular y Flutter no comparten runtime; comparten el
   modelo, los nombres y las reglas, para que un tutorial se piense una sola vez.
3. **El progreso detrás de un puerto.** Local hoy, remoto mañana, sin tocar una pantalla.
4. **El catálogo viaja perezoso.** En el backoffice, el motor, el velo, el globo y el
   catálogo entero están fuera del paquete inicial: el shell solo conoce una señal de
   «hay un tutorial corriendo». Nadie paga la ayuda para mirar un saldo.
5. **El hueco del velo deja pasar el dedo.** Un paso que dice «pulsá esto» tiene que
   dejar pulsarlo de verdad.
6. **Ninguna acción escribe.** El motor observa; la persona opera.

## 15 · Limitaciones conocidas

- **El avance no viaja entre dispositivos** hasta que exista el endpoint de §11. En el
  sitio público, además, **nunca va a viajar**: no hay cuenta donde guardarlo.
- **El e2e del backoffice necesita `Prefer: example=vacio`.** El escenario por omisión
  de `packages/simulado/ejemplos/identidad/autenticar.json` pide siempre segundo factor,
  también cuando la petición ya lo trae, y contra él la sesión nunca se abre. La prueba
  elige el escenario con esa cabecera y está explicado dentro del archivo; el ejemplo
  `ok` no se toca porque lo usan las pruebas de contrato de la app. Con esa cabecera,
  **los seis recorridos pasan**.
- **La sesión del e2e no tiene permisos de sección**: el token del simulado no trae
  claims, así que se ven los cuatro tutoriales abiertos. El filtrado por permiso se
  prueba en `registro.spec.ts`, no en el e2e.
- **`Capacidad.organizador` está declarada y siempre en falso** en la app: la app aún no
  expone esa habilitación fuera de la pantalla de crear grupo.
- **La bitácora de analítica no sale a ningún lado**: guarda los últimos 50 eventos en
  memoria, para diagnóstico. El puerto está listo para un destino real.
- **En el backoffice no hay acciones `escribir`/`elegir` en el catálogo actual** más que
  en el propio centro de ayuda; el motor las soporta y están probadas.
- **La app no valida acciones de escritura de campos**: en un teléfono el teclado tapa
  el globo, así que los pasos de la app piden tocar o navegar.

## Ver también

`web-angular` · `movil-flutter` · `disenar-frontend` · `arquitectura-atomica` ·
[[ADR-044 Frontend en Angular y Flutter]] · [[ADR-033 Puertos y adaptadores]] ·
[[ADR-009 Composición atómica]]
