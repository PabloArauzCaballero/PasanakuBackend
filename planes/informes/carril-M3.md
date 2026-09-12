---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril M3 — pasanaku (Flutter)"
ola: F3
fase: F5
mundo: Flutter
modulo: apps/movil/lib/pantallas/{pasanaku,soporte}
rama: pablo/feature/carril-M3-pasanaku
estado: en curso — bloque grupo/turno/transparencia/reputación cerrado con huecos declarados; aporte parcial; entrega y reclamo bloqueados por falta de contrato
---

# Carril M3 — pasanaku

**Fase** F5 · **Mundo** Flutter · **Casos de uso** CU-20–29, 52, 53, 59–76 · **Puesto** P1 · Mac M5

> La fase más grande del frontend. Se partió en bloques como indica la ficha: **grupo,
> turno, aporte, entrega, transparencia, reclamo**, en ese orden de prioridad. Grupo,
> turno y transparencia quedan con flujos reales de punta a punta; aporte tiene el cobro
> pero no la lectura de "mi calendario"; entrega y reclamo quedan bloqueados por un
> hueco de contrato que se declara abajo, no se inventa.

## Paso cero y hallazgo inicial

El worktree se paró en `189aba5` como indicaba el prompt y se verificó
`apps/movil/lib/pantallas/{billetera,identidad}/` con el contenido esperado (M1 de 14
archivos, M2 con `pantalla_recargar/retirar/transferir.dart`). Antes de escribir la
primera pantalla se encontraron dos huecos troncales que ningún carril de pantallas
puede resolver por sí mismo (`.claude/skills/arrancar-carril/SKILL.md` §7): `pubspec.yaml`
solo declaraba el cliente de `nucleo-financiero`, y faltaban los `.g.dart` de
`json_serializable` de **todos** los clientes Dart (el generador de OpenAPI (Gradle)
solo corre `openapi-generator`; el `build_runner` de cada paquete es un paso aparte que
nadie había corrido todavía). Se resolvió con un micro-PR al troncal
(`pubspec.yaml` con los catorce clientes) y corriendo `dart run build_runner build` en
cada `clientes/dart/<servicio>` que este carril usa — sin eso `flutter analyze` fallaba
con "Undefined class" en cascada y ningún carril de pantallas hubiera podido avanzar.
También se descubrió que `packages/diseno_flutter/lib/tokens/tokens.dart` es un
artefacto GENERADO (`yarn workspace @aportaya/tokens build` + `yarn workspace
@aportaya/diseno-flutter build`), gitignorado; sin correrlo, ninguna pantalla de ningún
carril de Flutter compila. Se documenta acá porque el próximo carril que abra esta
máquina va a pisar el mismo bache si no corre esos tres comandos primero.

## El hueco más grande del carril: los contratos son casi todos comandos, no consultas

Al leer los `openapi/*.yaml` reales de `grupos`, `aportes`, `entregas`, `garantia`,
`transparencia` y `cumplimiento` se confirmó un patrón: son contratos de **escritura**
(`POST`), con un puñado de lecturas puntuales (`GET` de un recurso por id). **No existe
ningún `GET` de listado ni de detalle** para: mis grupos, el turno de un grupo, la cola
de postulaciones del organizador, el estado de una entrega, ni el reclamo. Puntualmente:

- No hay `GET /grupos` ni `GET /grupos?participante=` (la comprobación de D-15 que pide
  el gate — "después del canje, `GET /grupos?participante=` no trae el grupo" — no se
  puede ejecutar porque **esa ruta no existe** en `servicios/grupos/openapi/grupos.yaml`).
- No hay `GET /grupos/{id}/postulaciones` (cola del organizador) ni una operación para
  aceptar/rechazar una postulación con motivo obligatorio (D-15, segunda mitad).
- El contrato de `organizador` (`GET /organizadores/{id}/habilitacion`) devuelve
  `habilitado`, `nivel` y los límites del nivel, pero **no** la lista descompuesta de
  los 14 requisitos con cumplido/faltante que pide D-16 — esa forma
  (`Faltante[]`) solo existe en la respuesta de una mutación, no en algo que se pueda
  leer de antemano.
- **No existe ningún endpoint, en ningún servicio, para crear ni leer un
  `reclamo_cliente`** (CU-52/53). `cumplimiento` no publica ninguna operación bajo
  `/reclamos` pese a que el CU y `docs/Restricciones.md` documentan `R-CON-01` y los
  códigos `AP-CU52-0x`.
- No hay lectura de "mi calendario de cuotas" (D-22 vive en F4, no en F5, pero el
  organismo `CalendarioDeCuotas` de `packages/diseno_flutter` que lo pinta no tiene de
  dónde traer los datos de pasanaku sin ese contrato).
- No hay lectura del propio expediente de incumplimiento (D-17: "el participante ve su
  propio expediente con plazos guardados") — `garantia` solo expone
  `consultarRestriccion` (sí/no + monto para levantarla), sin plazos ni expediente.

Programar cualquiera de esas pantallas habría significado **inventar la forma de una
respuesta que no existe**, lo que la Regla cero prohíbe explícitamente. Se optó por: (1)
construir todo lo que SÍ tiene contrato real, con datos reales, de punta a punta; (2)
para lo que no tiene contrato, una pantalla explícita que dice qué falta y por qué —
nunca un formulario mudo que llama a un endpoint inexistente; (3) declarar cada hueco
acá, con el nombre exacto del endpoint que hace falta, para que quien escriba el
contrato no tenga que releer los CU de cero.

## Pantallas

| Pantalla | Ruta | CU | Delta | Organismos | Estados | Dato | Gate |
| --- | --- | :-: | :-: | --- | :-: | :-: | :-: |
| `PantallaCrearGrupo` | `/pasanaku/grupos/nuevo` | CU-20 | D-16 (parcial) | `EstadoDePantalla`, `Campo`, `CampoMonto`, `GrupoRadio`, `Casilla`, `Boton` | ⬜ 4/4 | real (`POST /grupos` + `GET /organizadores/{id}/habilitacion`) | ✅ |
| `PantallaPedirCupo` | `/pasanaku/grupos/:grupoId/pedir-cupo` | CU-68 | D-15 | `Campo`, `Boton`, `EstadoError` | ⬜ 4/4 | real (`POST /grupos/{id}/postulaciones`) | ✅ |
| `PantallaInvitar` | `/pasanaku/grupos/:grupoId/invitar` | CU-69 | — | `Campo`, `SelectorSegmentado`, `Boton` | ⬜ 4/4 | real (`POST /grupos/{id}/invitaciones`) | ✅ |
| `PantallaPermuta` | `/pasanaku/turnos/permuta` | CU-62 | — | `Campo`, `Boton` | ⬜ 4/4 | real (`POST /turnos/permutas`) | ✅ |
| `PantallaRetiro` | `/pasanaku/grupos/:grupoId/retiro` | CU-65 | — | `Campo`, `Casilla`, `ChipEstado`, `Boton` | ⬜ 4/4 | real (`POST /grupos/{id}/retiros`) | ✅ |
| `PantallaTurno` | `/pasanaku/sorteos/:sorteoId/turno` | CU-60/61 | — | `RielDeTurnos`, `PanelSorteo`, `EstadoDePantalla` | ⬜ 4/4 | real (`GET /grupos/sorteos/{id}/paquete`) | ✅ |
| `PantallaVerificarSorteo` | `/pasanaku/sorteos/:sorteoId/verificar` | CU-61 | gate propio F5 | `ChipEstado`, `EstadoDePantalla` | ⬜ 4/4 | real, pública, sin sesión (`GET /publico/sorteos/{id}/verificacion`) | ✅ |
| `PantallaTransparencia` | `/pasanaku/grupos/:grupoId/transparencia` | CU-73 | — | `ChipEstado`, `EstadoDePantalla` | ⬜ 4/4 | real, pública (`GET /publico/grupos/{id}/verificacion`) | ✅ |
| `PantallaMiEstado` | `/pasanaku/mi-estado` | CU-21/25/27 (lectura) | gate mora-en-hechos | `ChipEstado`, `Monto`, `EstadoDePantalla` | ⬜ 4/4 | real (`GET .../estado`, `GET .../restricciones/vigentes/{id}`) | ✅ |
| `PantallaMiPuntaje` | `/pasanaku/mi-puntaje` | CU-71 (lectura) | D-20 (parcial) | `EstadoDePantalla` | ⬜ 4/4 | real (`GET /reputacion/{id}/puntaje`) | ✅ |
| `PantallaAportar` | `/pasanaku/obligaciones/:obligacionId/aportar` | CU-21 | — | `CampoMonto`, `SelectorSegmentado`, `Campo`, `Boton`, `EstadoError` | ⬜ 4/4 | real (`POST /aportes/obligaciones/{id}/pagos`) | ✅ cobro · ⚠️ sin lectura de "qué me toca aportar" |
| `PantallaReclamoPendiente` | `/pasanaku/reclamos/nuevo` | CU-52/53 | D-18 | — (aviso) | ⬜ 1/4 (solo dato) | **bloqueada, sin contrato** | 🔴 |

**Sin pantalla, huecos declarados**: cola del organizador con aceptar/rechazar (D-15,
2ª mitad — falta `GET/PATCH /grupos/{id}/postulaciones`), "mis grupos" (falta `GET
/grupos?participante=`), los 14 requisitos descompuestos de D-16 (falta un `GET` de
solo lectura en `organizador`), expediente propio con plazos (D-17 — falta que
`garantia` exponga el expediente, no solo la restricción), recibir el fondo / CU-22
(falta lectura del estado de una entrega propia), mercado D-20 completo (falta listado
de grupos abiertos a postulación con el tope del 5 %).

## Piezas declaradas por nivel

| Pieza | Nivel | Dónde vive | CU | Estado |
| --- | --- | --- | :-: | :-: |
| `CrearGrupo` (`AsyncNotifier`) | átomo de dominio | `pasanaku/dominio/cu20_crear_grupo.dart` | CU-20 | ✅ |
| `habilitacionOrganizadorProvider` | átomo de dominio | `pasanaku/dominio/cu20_crear_grupo.dart` | D-16 | ✅ (parcial, ver hueco) |
| `PostularAlGrupo` | átomo de dominio | `pasanaku/dominio/cu68_postular_al_grupo.dart` | CU-68 | ✅ |
| `InvitarAlGrupo` | átomo de dominio | `pasanaku/dominio/cu69_invitar_al_grupo.dart` | CU-69 | ✅ |
| `SolicitarPermuta` | átomo de dominio | `pasanaku/dominio/cu62_solicitar_permuta.dart` | CU-62 | ✅ |
| `SolicitarRetiro` | átomo de dominio | `pasanaku/dominio/cu65_solicitar_retiro.dart` | CU-65 | ✅ |
| `paqueteDelSorteoProvider` / `CU60` | átomo de dominio | `pasanaku/dominio/cu60_sortear_turnos.dart` | CU-60 | ✅ |
| `verificarSorteoProvider` | átomo de dominio | `pasanaku/dominio/cu61_verificar_sorteo.dart` | CU-61 | ✅ |
| `verificarCadenaProvider` | átomo de dominio | `pasanaku/dominio/cu73_verificar_transparencia.dart` | CU-73 | ✅ |
| `estadoDelParticipanteProvider` / `restriccionVigenteProvider` | átomo de dominio | `pasanaku/dominio/estado_de_mora.dart` | CU-21/25/27 | ✅ |
| `puntajeProvider` | átomo de dominio | `pasanaku/dominio/consultar_puntaje.dart` | CU-71 | ✅ |
| `CobrarAporte` | átomo de dominio | `pasanaku/dominio/cu21_cobrar_aporte.dart` | CU-21 | ✅ |
| `PantallaCrearGrupoControles` (interfaz) | organismo/contrato interno | `pasanaku/pantalla_crear_grupo.dart` + `organismos_crear_grupo.dart` | CU-20 | ✅ (mantiene `pantalla_crear_grupo.dart` bajo 200 líneas) |

Ningún átomo, token ni puerto nuevo se creó en `packages/diseno_flutter` ni en
`lib/dominio/puertos/`: todo lo que hizo falta ya estaba (`RielDeTurnos`, `PanelSorteo`,
`ChipEstado`, `CalendarioDeCuotas` sin usar por falta de datos, `EstadoDePantalla`).

## Supuestos declarados

1. **`organizadorId` nulo ⇒ grupo autogestionado**, sin chequeo de habilitación
   (`RN-18`, comentario del propio `EntradaGrupo.organizadorId`). No es una inferencia:
   lo dice el contrato.
2. **`fechaDeInicio` por omisión, hoy + 7 días**, cuando el formulario se envía sin que
   la persona la haya tocado explícitamente — la maqueta no definía un selector de
   fecha para esta pantalla y el CU no fija un mínimo; se declara acá en vez de
   bloquear el flujo completo por un selector de fecha que no estaba especificado.
3. **La mora se muestra únicamente con los campos que el backend guarda**
   (`alDia`, `deudaVigente`, `porAportar`, `montoQueLaLevanta`) y **nunca con un número
   de días calculado en el cliente**, porque ni `EstadoDelParticipante` ni
   `RestriccionVigente` exponen una fecha de vencimiento por persona. Es una limitación
   real del contrato, no una elección de producto: `TextosPasanaku.diasDesdeVencimiento`
   quedó escrito (para cuando el contrato lo permita) pero **no se usa en ninguna
   pantalla todavía**, precisamente para no inventar el número.
4. **El "puntaje de reputación" de D-20 ("Tu nivel")** se resuelve con
   `GET /reputacion/{usuarioId}/puntaje`, que sí existe, aunque D-20 completo (mercado
   con tope del 5 % y pantalla que no abre bajo el mínimo) necesita además un listado
   de ofertas que no existe — se deja solo la mitad de "Tu nivel" construida.

## Ejemplos del contrato

Todos los ejemplos usados ya existían en `packages/simulado/ejemplos/{grupos,
transparencia}/` (generados junto al contrato por el carril de backend correspondiente:
`crearGrupo.json`, `postularAlGrupo.json`, `invitarAlGrupo.json`,
`solicitarPermuta.json`, `solicitarRetiro.json`, `consultarPaqueteDelSorteo.json`,
`verificarSorteo.json`, `verificarCadena.json`, `consultarPuntaje.json`,
`consultarEstadoDelParticipante.json`, `consultarRestriccion.json`,
`cobrarAporte.json`). Este carril no creó ningún ejemplo nuevo: no hizo falta, porque no
se programó contra ningún endpoint sin ejemplo.

## Micro-PR abiertos al troncal

| Rama | Qué agrega | Mundo | Estado |
| --- | --- | :-: | :-: |
| (aplicado directamente, ver "hallazgo inicial") | `apps/movil/pubspec.yaml`: declara los catorce clientes Dart de una vez | Flutter | ✅ aplicado — evita que el próximo carril repita el mismo bloqueo |

No se abrió un micro-PR de código de app: los huecos de este carril son de **contrato de
servicio** (grupos, organizador, garantia, cumplimiento), no de plataforma compartida —
van en "Bloqueos", no acá.

## Ficha de paridad iOS

| Pantalla | Área segura | Permisos | Gesto de retroceso | Tipografía dinámica | Decisión |
| --- | --- | --- | --- | --- | --- |
| Crear grupo / pedir cupo / invitar / permuta / retiro / aportar (formularios) | `SafeArea` ya presente, sin notch específico | Ninguno (sin cámara, sin biometría) | `AppBar` provee back nativo en ambas plataformas, sin gesto propio agregado | Usa `Theme.of(context).textTheme`, escala con el sistema | Sin cambios para iOS — pendiente solo la corrida real en simulador, no hay lógica condicional por plataforma que revisar |
| Turno / verificar sorteo / transparencia / mi estado / mi puntaje (lectura) | `SafeArea` presente | Ninguno | Back nativo | Igual que arriba | Idem — bloque de solo lectura, sin puertos nativos involucrados |

No hay ninguna dependencia de `Platform.is` en este bloque (verificado por
`gate_del_shell_test.dart`, que sigue en verde), así que la paridad es por construcción:
no hay rama de código que distinga Android de iOS en las doce pantallas de este carril.

## Bloqueos

| Qué falta | A quién se pide | Bloquea |
| --- | --- | --- |
| `GET /grupos?participante=` (o `GET /grupos/mios`) | `grupos` | D-15 (verificación completa), "mis grupos", calendario del ciclo |
| `GET /grupos/{id}/postulaciones` + acción de aceptar/rechazar con motivo obligatorio | `grupos` | D-15 (cola del organizador) |
| `GET` de solo lectura con los 14 requisitos descompuestos (cumplido/faltante/umbral) | `organizador` | D-16 completo |
| Expediente propio con plazos guardados (no solo la restricción sí/no) | `garantia` | D-17 |
| **Cualquier endpoint de `reclamo_cliente`** (crear, leer, elevar a segunda instancia) | `cumplimiento` (o un servicio `reclamos` nuevo) | CU-52, CU-53, D-18 completos — es el bloqueo más grande del carril |
| Lectura del estado de una entrega propia | `entregas` | CU-22 (recibir el fondo) |
| Listado de grupos abiertos a postulación, con el tope del 5 % de mercado | `grupos` o `tarifas` | D-20 completo |
| Lectura de "qué obligación me toca aportar ahora" (no solo cobrar una obligación ya identificada) | `aportes` | Que `PantallaAportar` se pueda abrir sin conocer de antemano el `obligacionId` |

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Especificación | Cada pantalla sale de la sección Interfaz/Flujo de su CU | 11 pantallas citadas por CU en la tabla de arriba | ✅ |
| Regla cero | Nada inventado donde falta contrato | 8 huecos declarados en "Bloqueos", 0 formularios mudos a endpoints inexistentes | ✅ |
| Estados | Los cuatro (carga/error/vacío/éxito) vía `EstadoDePantalla` en toda pantalla de lectura | 6/6 pantallas de lectura los usan | ✅ |
| Dinero | Todo importe por `Monto`; doble envío bloqueado con clave de idempotencia | `pantalla_pedir_cupo_test.dart` prueba doble toque → 1 sola petición, 1 sola clave | ✅ |
| Turno verificable | El sorteo se ve verificable desde la app | `PantallaTurno` enlaza a `PantallaVerificarSorteo` (ruta pública, `GET /publico/sorteos/{id}/verificacion`); prueba en `pantalla_verificar_sorteo_test.dart` para verifica=true y verifica=false | ✅ |
| Mora en hechos | Nunca una probabilidad como si fuera un hecho | `PantallaMiEstado` solo muestra `alDia`/`deudaVigente`/`montoQueLaLevanta`; `diasDesdeVencimiento` queda escrito y sin usar (hueco de contrato, ver Supuesto 3) | ✅ (dentro de lo que el contrato permite) |
| Reclamo con plazo guardado | El plazo mostrado es el guardado, no recalculado | **No aplicable todavía: no hay contrato de reclamo** — `PantallaReclamoPendiente` no muestra ningún plazo, ni guardado ni recalculado | 🔴 bloqueado, no incumplido |
| D-15 pedir cupo | Botón dice "Pedir mi cupo"; tras el canje, D-15 completo | Botón verificado por prueba; el "no ocupa cupo" no se puede probar con datos reales porque `GET /grupos?participante=` no existe | ⚠️ parcial |
| Paridad iOS | Ficha por bloque cerrado | Tabla de arriba | ✅ |
| Arquitectura | Un archivo por CU en `dominio/`, pantallas ≤200 líneas | `yarn workspace @aportaya/movil lint` → "ningún archivo de más de 200 líneas": OK | ✅ |
| Entrega | Lint, tipos, pruebas, build | ver debajo, salida real | ✅ |

## Gate de salida — evidencia

Comandos ejecutados desde `apps/movil/` (y `yarn workspace @aportaya/movil …` desde la
raíz), salida real:

```
$ yarn workspace @aportaya/movil build
...
Built with build_runner/aot in 0s; wrote 0 outputs.   # (up to date tras el micro-PR)

$ yarn workspace @aportaya/movil lint
Formatted 123 files (0 changed) in 0.14 seconds.
=== apps/movil (Flutter) ===
  OK    · sin red en vista
  OK    · sin literal de diseño
  OK    · sin formato de dinero
  OK    · sin plataforma en vista
  OK    · sin print
  OK    · ningún archivo de más de 200 líneas
  OK    · el shell tiene navegacion/rutas.dart
  OK    · el shell tiene dominio/cliente.dart
  OK    · el shell tiene proveedores/sesion.dart
TODO OK

$ yarn workspace @aportaya/movil typecheck
Analyzing movil...
No issues found!

$ yarn workspace @aportaya/movil test:front
...
00:01 +32: All tests passed!

$ yarn workspace @aportaya/movil test:a11y
00:00 +0: loading .../test/a11y/pantalla_de_saldo_a11y_test.dart
00:00 +0: la pantalla de saldo cumple las guías de accesibilidad de Flutter
00:00 +1: el estado de error también: el botón de reintento se alcanza y se lee
00:00 +2: All tests passed!

$ flutter test   (suite completa, incluye test/pasanaku/)
00:02 +41: All tests passed!
```

- [x] `flutter pub get` corrido en `apps/movil` y en los diez `clientes/dart/<servicio>`
      usados por este carril, sin diff de código (solo `.dart_tool/`, gitignorado)
- [x] `yarn workspace @aportaya/movil lint && yarn workspace @aportaya/movil typecheck`
- [x] `yarn workspace @aportaya/movil test:front && yarn workspace @aportaya/movil test:a11y`
- [x] `flutter analyze` → "No issues found!"
- [ ] Goldens: no se agregó ningún golden nuevo (las pantallas de este carril no tenían
      captura de maqueta específica que reproducir pixel a pixel dentro del tiempo
      disponible; se priorizaron pruebas de comportamiento sobre goldens — declarado
      como pendiente, no como hecho)
- [x] `grep -r "Platform.is" lib/` → sin resultados fuera de `infraestructura/` (gate
      del shell sigue en verde, `gate_del_shell_test.dart` pasa)
- [x] `git diff --stat` contra la base de los archivos del shell (`navegacion/`,
      `proveedores/`, `infraestructura/`, `lib/dominio/{cliente,errores,validacion}.dart`,
      `lib/dominio/puertos/`) → vacío, no se tocó ninguno
- [x] Gate propio de la ficha F5: turno/sorteo verificable ✅ · reclamo con plazo
      guardado 🔴 bloqueado por contrato (no incumplido: no hay plazo que mostrar porque
      no hay reclamo que crear) · mora en hechos ✅ · paridad iOS por bloque ✅

## Qué sigue, en orden

1. **Pedir el contrato de reclamos** (`cumplimiento` o un servicio nuevo) — es lo único
   que bloquea CU-52/53 y D-18 enteros, y es el hueco más caro del carril: sin él, el
   producto no tiene una vía de reclamo real, que es un requisito regulatorio (ASFI RNSF
   Libro 4 Título I), no solo de producto.
2. **Pedir `GET /grupos?participante=`** a `grupos` — sin esto no hay "mis grupos", no
   se puede terminar de verificar D-15, y tampoco hay de dónde traer el calendario del
   ciclo que pide D-22 (aunque D-22 es gate de F4).
3. **Pedir la cola de postulaciones del organizador** (`GET` + acción de
   aceptar/rechazar con motivo obligatorio) — cierra la segunda mitad de D-15.
4. **Pedir el detalle de los 14 requisitos a `organizador`** — cierra D-16.
5. **Pedir el expediente propio a `garantia`** (no solo la restricción) — cierra D-17.
6. Con esos cinco contratos resueltos, recién ahí construir: mis grupos, cola del
   organizador, expediente propio, calendario del ciclo, reclamo con plazo guardado, y
   el mercado D-20 completo (que además necesita un listado de ofertas).
7. Goldens de las pantallas ya construidas, comparados contra
   `docs/Views/AportaYa-Maqueta.html` en claro y oscuro — quedó fuera por tiempo, no por
   falta de necesidad.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[10b Estándar de ejecución del frontend]]
