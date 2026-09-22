---
tags:
  - arquitectura
  - adr
  - frontend
  - producto
titulo: "ADR-045 — Tutoriales interactivos como datos, con el progreso detrás de un puerto"
estado: aceptada
fecha: 2026-09-17
---

# ADR-045 — Tutoriales interactivos como datos, con el progreso detrás de un puerto

> Cómo se enseña a usar AportaYa desde adentro de AportaYa: **un motor compartido entre
> las dos superficies web y otro en la app**, un catálogo declarativo por producto, y el
> avance detrás de un puerto que hoy escribe en el dispositivo y mañana en el servidor.

## Contexto

Dos productos con dos públicos que no se parecen: el **backoffice**, con seis dominios,
tablas densas, doble control y plazos legales, operado por gente que entra por primera
vez a una herramienta que decide sobre la plata y la identidad de otros; y la **app del
participante**, usada por gente que nunca tuvo una billetera digital y que necesita
entender qué es «retenido» antes de que le importe.

Hasta ahora la única ayuda era el tour de bienvenida de la app (cuatro láminas, antes de
abrir cuenta) y los textos de propósito de cada pantalla. Nada que acompañe **sobre la
interfaz de verdad**, y nada en el backoffice.

Tres restricciones marcaron el diseño:

1. **No hay dónde guardar el progreso.** `identidad` solo tiene
   `preferencia_notificacion`; no existe una tabla de preferencias de usuario, y agregar
   una al esquema regulado por una funcionalidad de ayuda es una decisión propia.
2. **Los dos productos no comparten runtime.** Angular y Dart; ADR-044.
3. **El presupuesto del paquete inicial del backoffice es bloqueante.** Una funcionalidad
   de ayuda no puede hacer más lento el arranque de quien viene a mirar un saldo.

## Decisión

**Un motor compartido por las dos superficies web (`packages/tutoriales`) y otro en la
app, con el mismo modelo; los tutoriales son datos; los elementos se marcan con un
atributo estable; el progreso vive detrás de un puerto; y todo el motor viaja perezoso.**

| Decisión | Backoffice | Sitio público | App |
| --- | --- | --- | --- |
| Motor | `packages/tutoriales` (señales) | el mismo paquete | `dominio/tutoriales/motor.dart` (Riverpod) |
| Catálogo | `rutas/ayuda/catalogo/` | `tutoriales/catalogo/` | `pantallas/soporte/catalogo/` |
| Marca del elemento | `data-tutorial-id="…"` | igual | `MarcaDeTutorial(id: …)` sobre una `GlobalKey` |
| Vista | `@aportaya/ui/{foco,globo}-de-tutorial` | igual | `aportaya_diseno/moviles/{foco,globo}_de_tutorial` |
| Centro | sección `Ayuda` (`/ayuda`) | pestaña `Guía` (`/tutoriales`) | quinta pestaña `Ayuda` (`/soporte/ayuda`) |
| Quién aprende | `Sesion` (permisos del token) | `visitanteAnonimo` | capacidades (`sesion`, `organizador`) |
| Progreso | puerto `AlmacenDeProgreso` → `localStorage` | igual | mismo puerto → almacén seguro |

**Lo que cambia por producto entra por token**, no por código: el catálogo, las rutas
válidas, el almacén, quién aprende y cómo se llama el centro. Agregar el motor a un
producto web nuevo son cinco `provide` y un `@defer`.

### Las cinco reglas

1. **Un tutorial enseña, no opera.** Lo único automático es desplazar y enfocar.
2. **El motor no conoce ningún tutorial.** Agregar uno no toca el motor.
3. **El filtro oculta, no protege.** El servidor sigue decidiendo en cada llamada.
4. **Nunca se traba.** Un objetivo que no aparece es un problema declarado con reintento.
5. **La ayuda no pesa en el arranque.** El shell solo conoce una señal de «hay un
   tutorial corriendo»; el resto llega por `import()`.

## Motivo

**Porque un tutorial escrito dentro de un componente muere con ese componente.** La
alternativa obvia —un `if (esPrimeraVez)` en cada pantalla— reparte la misma lógica por
sesenta archivos y hace imposible responder «¿qué le falta aprender a esta persona?».

**Porque el progreso es un dato del usuario y hoy no hay dónde ponerlo.** El puerto
permite entregar algo que funciona sin tocar el esquema regulado, y deja el cambio en un
`provide`. El contrato ya está escrito (`docs/Frontend/Motor de tutoriales.md` §11).

**Porque el presupuesto del paquete es una regla, no una sugerencia.** La primera versión
sumaba 50 kB al arranque del backoffice y rompía el presupuesto de 340 kB. Aislar el
motor detrás de una señal mínima lo dejó en +15 kB y bajo el límite.

**Porque marcar con `data-tutorial-id` sobrevive al rediseño.** Un selector de clase
CSS o una jerarquía de nodos se rompe en el próximo retoque visual, y se rompe en
silencio.

## Alternativas descartadas

| Alternativa | Por qué no |
| --- | --- |
| Una librería de *product tour* (Shepherd, Intro.js, `flutter_intro`) | Dos librerías distintas, dos modelos distintos, tokens propios que hay que pelear, y ninguna sabe de permisos ni de rutas del proyecto |
| Duplicar el motor en el sitio público en vez de extraerlo | Mil quinientas líneas repetidas que iban a divergir en el primer arreglo. La extracción a `packages/tutoriales` costó un día y dejó al sitio con cinco `provide` |
| Tutoriales en video o en el sitio público | No acompañan sobre la pantalla real; envejecen sin que nadie se entere |
| Un solo motor en el backoffice, ayuda estática en la app | El público que más necesita la ayuda es el de la app |
| Progreso solo en el dispositivo, sin puerto | Contradice el requisito de continuar en otro dispositivo y deja el cambio futuro como una reescritura |
| Agregar ya la tabla `progreso_tutorial` a la bóveda | Tocar el esquema regulado merece su propio tramo; el puerto hace que esperar no cueste nada |

## Consecuencias

**A favor**

- Agregar un tutorial es agregar una constante; el núcleo se congela.
- El catálogo se valida en el build: ids duplicados, rutas inexistentes, anclas sin
  marcar, orden, ciclos y permisos incoherentes.
- La ayuda no pesa en el arranque de ninguno de los dos productos.
- Accesibilidad verificada: `vitest-axe` en web, guías de Flutter en la app.

**En contra, y hay que asumirlo**

- **Dos implementaciones.** El riesgo de divergencia se acota con un modelo idéntico y
  un documento único, no desaparece.
- **El avance no viaja entre dispositivos** hasta que exista el endpoint.
- **`Capacidad.organizador` está declarada y siempre en falso** en la app hasta que la
  habilitación del CU-90 se exponga fuera de la pantalla de crear grupo.
- **Cada pantalla que se quiera enseñar hay que marcarla.** Es una línea por elemento, y
  una prueba falla si un tutorial señala algo que nadie marcó.

## Cómo se verifica

- [x] `yarn workspace @aportaya/backoffice lint && typecheck && test:front && test:a11y`
- [x] `yarn workspace @aportaya/web lint && typecheck && test:front && test:a11y && build`
- [x] `apps/movil`: `dart analyze`, `flutter test test/unidad/tutoriales test/widget/tutoriales test/a11y`
- [x] El paquete inicial del backoffice queda bajo el presupuesto de 340 kB.
- [x] `/tutoriales` se prerrenderiza **con su lista**: el catálogo se espera con
      `PendingTasks`, así la página indexable no llega vacía.
- [x] La prueba del catálogo de cada producto verifica que toda ancla referida esté
      marcada en una pantalla de ese producto.
- [x] `yarn workspace @aportaya/backoffice test:e2e`: seis recorridos en verde contra
      `yarn dev:mock`, incluidos teclado, salida con confirmación y retomar a medias.
- [ ] `identidad` expone el progreso (§11) y los dos adaptadores remotos se enchufan.

## Ver también

`docs/Frontend/Motor de tutoriales.md` · [[ADR-044 Frontend en Angular y Flutter]] ·
[[ADR-033 Puertos y adaptadores]] · [[ADR-009 Composición atómica]] ·
[[ADR-036 Android primero]]
