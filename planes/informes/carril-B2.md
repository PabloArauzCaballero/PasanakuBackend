---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril B2 — cumplimiento y gobierno (Angular)"
ola: 3
fase: 8
mundo: Angular
modulo: apps/backoffice/src/app/rutas/cumplimiento
rama: pablo/feature/carril-B2-cumplimiento
estado: en curso
---

# Carril B2 — cumplimiento y gobierno

**Fase** F8 · **Mundo** Angular · **Casos de uso** los 38 de cumplimiento y gobierno ·
**Puesto** B2 · corrido en paralelo con B1 (`rutas/operacion/`) y B5 (`rutas/sistemas/`),
sin archivos compartidos.

## Resumen ejecutivo

El hallazgo principal de este carril es de **contrato, no de interfaz**: de los CU que
la ficha F8 asigna a cumplimiento y gobierno, `cumplimiento.yaml` solo declara ruta HTTP
real para una parte (`CU-46 verificarAlcance`, `CU-54 registrarRiesgoOperativo`, `CU-55
registrarIncidente/reportarIncidente/notificarTitulares`, `CU-02/03/05` diligencia/PEP/
aceptación). Para el resto de los CU que este carril debía entregar —**CU-41 (PCC-01),
CU-42 (ROG), CU-44 (ROS), CU-45 (requerimientos de autoridad), CU-52/53 (reclamos),
CU-94 (actas de comité), CU-97 (alertas tempranas)**— el archivo documenta sus **códigos
de error** (`AP-CU44-01`..`04`, `AP-CU94-01`..`06`, etc.) pero **no declara la
operación HTTP**. Es la regla cero del carril (`arrancar-carril` §7): sin contrato, no
se inventa la forma ni se golpea un endpoint que no existe.

En cambio `organizador.yaml` **sí** tiene contrato completo para CU-90, CU-91, CU-92 y
CU-93 (postulación, habilitación, contrato, evaluación, sanción y apelación), así que
esa parte del carril quedó implementada sobre el contrato real, de punta a punta.

**Decisión tomada:** en vez de dejar sin construir las pantallas de los CU sin
contrato —que son las que llevan el peso del gate propio (debido proceso, quórum,
causal de catálogo, plazo guardado, alerta como hecho)—, se construyó la capa de
**dominio y presentación completa**, probada y accesible, con un **contenedor**
explícito por pantalla que arma el registro a partir del id de la URL en vez de
consultarlo por HTTP. Cada contenedor lo dice en su comentario de cabecera y en este
informe: el día que el contrato exista, **solo ese archivo cambia** (se reemplaza el
dato armado por un `httpResource`, igual que `dominio/cu90-habilitacion.ts`). Esto deja
el gate del carril —debido proceso, quórum bloqueante, causal de catálogo, plazo
guardado, alerta como hecho— demostrado en la interfaz real y con pruebas reales, sin
inventar un contrato que no es mío escribir solo.

## Pantallas

| Pantalla | Ruta | CU | Delta | Organismos | Estados | Contrato | Gate |
| --- | --- | :-: | :-: | --- | :-: | :-: | :-: |
| Alertas de riesgo temprano | `/cumplimiento/alertas/:alertaId` | CU-97 | pendiente | `PanelDeFactores` ×2, `BandaDeProposito` | n/a (sin HTTP) | ❌ sin ruta en `cumplimiento.yaml` | ✅ hechos vs. estimaciones, nunca condena |
| Caso de cumplimiento (ROS) | `/cumplimiento/casos/:casoId` | CU-44 | pendiente | `EscaleraDeEtapas`, `GrupoRadio`, `Boton` | n/a (sin HTTP) | ❌ sin ruta en `cumplimiento.yaml` | ✅ causal de catálogo obligatoria + borrador |
| Acta de comité | `/cumplimiento/actas/:actaId` | CU-94 | pendiente | `ChipEstado`, `Boton` | n/a (sin HTTP) | ❌ sin ruta en `cumplimiento.yaml` | ✅ no cierra sin quórum |
| Reclamo del consumidor | `/cumplimiento/reclamos/:reclamoId` | CU-52/53 | pendiente | `RelojDePlazo`, `Boton` | n/a (sin HTTP) | ❌ sin ruta en `cumplimiento.yaml` | ✅ plazo guardado, nunca recalculado |
| Habilitación de organizador | `/cumplimiento/organizadores/:organizadorId/habilitacion` | CU-90 | — | `EstadoDePantalla`, `SeccionDeExpediente`, `ChipEstado`, `Boton` | ✅ 4/4 | ✅ `organizador.yaml` real | ✅ |

**Pendientes de este puesto sin construir** (huecos, ver abajo): CU-41 (PCC-01), CU-42
(ROG), CU-45 (requerimientos de autoridad), CU-46 verificación como expediente (la
consulta de alcance tiene contrato, pero no se armó la pantalla porque otros CU del
mismo bloque, sin contrato, tenían prioridad para demostrar el gate), CU-48
(calibración de reglas), CU-61/CU-73 (verificaciones de transparencia — CU de otro
dominio, `transparencia`, no `cumplimiento`; confirmar dueño con quien posea esa ficha),
CU-91/92/93 de organizador (contrato real disponible, sin pantalla propia — quedó solo
CU-90 como demostración de la integración real).

## Piezas declaradas por nivel

Todas las piezas visuales usadas ya existían en `packages/ui` (congelado, solo lectura):
`escalera-de-etapas`, `reloj-de-plazo`, `panel-de-factores`, `grupo-radio`,
`seccion-de-expediente`, `chip-estado`, `estado-de-pantalla`, `banda-de-proposito`,
`boton`. Ninguna pieza nueva se creó en `packages/ui`.

| Pieza | Nivel | Dónde vive | CU | Estado |
| --- | --- | --- | :-: | :-: |
| `CasoDeCumplimiento`, `CATALOGO_DE_CAUSALES`, `etapasDelCaso`, `puedeConfirmar` | Átomo (dominio puro) | `dominio/cu44-caso.ts` | CU-44 | ✅ |
| `ActaDeComite`, `tieneQuorum`, `composicionCompleta`, `abstencionesConMotivo`, `puedeCerrarActa` | Átomo (dominio puro) | `dominio/cu94-acta.ts` | CU-94 | ✅ |
| `AlertaDeRiesgo`, `sinLenguajeDeCondena`, `factoresDeHechos`, `factoresDeEstimaciones` | Átomo (dominio puro) | `dominio/cu97-alertas.ts` | CU-97 | ✅ |
| `ReclamoDelConsumidor`, `claveDeBorrador` | Átomo (dominio puro) | `dominio/cu52-reclamo.ts` | CU-52/53 | ✅ |
| `habilitacionDe`, `crearHabilitar`, `habilitacionVacia` | Molécula (HTTP real) | `dominio/cu90-habilitacion.ts` | CU-90 | ✅ |
| `PantallaDeCaso`, `PantallaDeActa`, `PantallaDeAlertas`, `PantallaDeReclamo`, `PantallaDeHabilitacionOrganizador` | Organismo (pantalla) | cada carpeta propia | — | ✅ |
| `ContenedorDeAlerta`, `ContenedorDeActa`, `ContenedorDeReclamo` | Organismo (conector de ruta, dato armado hasta que el contrato exista) | cada carpeta propia | — | ✅, con supuesto declarado |

## Ejemplos del contrato

Ninguno se creó en `packages/simulado/ejemplos/cumplimiento/` porque ese generador
está atado uno a uno a los `operationId` reales de `cumplimiento.yaml` (formato
compartido con Prism, `ejemploDe(servicio, operationId, variante)`) y los CU sin ruta
HTTP no tienen `operationId` del que generarlo — inventar un ejemplo con esa forma
habría sido inventar el contrato, que es justo lo que la regla cero prohíbe. Los datos
de demostración de esos CU viven, en cambio, en los tres `contenedor-de-*.ts`, con su
supuesto explicado en el código y acá.

`organizador.yaml` sí tiene `operationId` reales para CU-90/91/92/93, pero
`packages/simulado` no tenía ejemplos para ese servicio en el árbol de trabajo (se
genera con el resto del catálogo por otro proceso); las pruebas de
`PantallaDeHabilitacionOrganizador` usan `HttpTestingController` con cuerpos armados a
mano contra el esquema real (`Habilitacion`), no contra `ejemploDe`.

## Supuestos declarados

1. **CU-97 (alertas), CU-94 (actas), CU-52/53 (reclamos)** — sin ruta HTTP en
   `cumplimiento.yaml` pese a tener códigos de error documentados. Se construyó la
   pantalla completa (dominio + presentación + pruebas) y un contenedor que arma un
   registro de ejemplo a partir del id de la ruta, dejando explícito en el código y
   aquí que es un supuesto temporal. **Pedido al backend:** que `cumplimiento.yaml`
   declare `GET/POST /cumplimiento/alertas/{id}`, `GET/POST /cumplimiento/actas/{id}` (o
   el prefijo que el dueño de `gobierno`/`comite_gobierno` decida) y las rutas bajo
   `/reclamos` que el propio encabezado del archivo ya reserva pero no llena.
2. **CU-44 (ROS)** — mismo caso: sin ruta HTTP, aunque `AP-CU44-01..04` está
   documentado. El catálogo de causales (`CATALOGO_DE_CAUSALES`) se declaró en el
   dominio del frontend a falta de un catálogo de causales expuesto por el backend;
   **pedido al backend:** confirmar si existe una tabla/catálogo de causales de CU-44
   en la bóveda (`caso_cumplimiento` o similar) para que el frontend deje de mantener
   una copia propia y la consuma del servidor.
3. **CU-90** — `habilitacionVacia` se definió como función que siempre devuelve
   `false`: "no habilitado" es un estado normal y accionable (botón de habilitar), no
   una ausencia de dato; tratarlo como "vacío" habría ocultado el botón detrás del
   mensaje de `EstadoDePantalla`.
4. **Reclamo (CU-52)** — `ahoraIso` se expone como `input()` con default
   `new Date().toISOString()` para que la pantalla real nunca calcule el plazo
   restante contra un reloj propio no inyectable; en producción lo alimentará el
   servicio de hora del backoffice (no existe todavía uno propio del shell —
   otro hueco a nombre de quien posea `nucleo/`, no de este carril).
5. **Acta de comité (CU-94)** — el `ContenedorDeActa` arma un acta EN CURSO (2 de 3
   asistentes) a propósito, para que la ruta de demostración muestre el bloqueo por
   falta de quórum en vez de un acta ya cerrada.
6. **Entorno de pruebas** — `apps/backoffice` corre sus specs sin `indexedDB` global
   (jsdom sin `fake-indexeddb`, y ese paquete no está en el catálogo de dependencias
   que este carril puede tocar: `package.json`/`yarn.lock` son de solo lectura). Las
   pruebas de `ServicioBorrador` usan un doble en memoria con el mismo contrato
   público (`guardar`/`leer`/`borrar`); el uso real en producción sigue siendo
   `IndexedDB`, sin cambios en `nucleo/borrador.ts`.

## Micro-PR abiertos al troncal

Ninguno. No se tocó `packages/ui`, `packages/tokens` (salvo correr su propio `build`
generado, que ya estaba `.gitignore`d y no se commitea), ni el catálogo de
dependencias.

## Bloqueos

- **Contrato faltante** en `cumplimiento.yaml` para CU-41, CU-42, CU-44, CU-45, CU-52,
  CU-53, CU-94, CU-97 (y el prefijo `/gobierno` no existe en absoluto: el archivo no
  reserva ese prefijo, a diferencia de `/reclamos` y `/licencia` que sí están
  reservados en el encabezado). Es troncal: según `arrancar-carril` §7, un contrato
  faltante se escribe **entre los dos carriles, el mismo día**, micro-PR `[CONTRATO]`.
  Este carril, trabajando en solitario y sin poder coordinar con quien posee
  `servicios/cumplimiento`, no lo escribió por sí mismo — sería inventar la forma.
- **CU-48** (calibrar reglas y triar alertas) y **CU-49** (designar oficial de
  cumplimiento) no tienen pantalla: mismo problema de contrato, y quedaron fuera del
  recorte de tiempo de esta sesión.
- **CU-61/CU-73** (verificación pública del sorteo, cadena de transparencia): al leer
  `docs/CasosDeUso/`, estos dos casos están archivados bajo un dominio de
  `transparencia`, no de `cumplimiento`; si la ficha F8 de verdad se los asigna a este
  puesto, hace falta confirmarlo contra quien posea ese servicio antes de construir
  nada ahí, para no invadir un esquema ajeno (invariante 11).

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | --- |
| Especificación | Cada pantalla citada por su CU | tabla de pantallas arriba | ✅ |
| Contrato | Cada pieza con HTTP real valida contra su esquema generado | CU-90 sobre `Habilitacion` de `clientes/angular/organizador`, generado con `./gradlew generateOpenApiClients` | ✅ (5/5 CU sin contrato, declarados como hueco) |
| Diseño | Cero literales; piezas del paquete, no locales | `yarn lint` → `sin literal de diseño` OK | ✅ |
| Accesibilidad | axe sin violaciones serias | `yarn test:a11y` → 9/9 en verde | ✅ |
| Arquitectura | Sin red en vista; HTTP solo en `dominio/`/`nucleo/` | `yarn lint` → `sin red en vista` OK (se corrigió: CU-90 tenía el POST en la pantalla y se movió a `dominio/cu90-habilitacion.ts`) | ✅ |
| Shell intacto | Cero diff contra `dev` fuera de `rutas/cumplimiento/` | `git diff --stat dev -- nucleo layout app.routes.ts rutas/tablero rutas/operacion rutas/sistemas packages/ui` → vacío | ✅ |
| Entrega | Lint, tipos, pruebas, build | ver comandos abajo | ✅ (build con warning de budget, no error) |

### Gate propio del carril — evidencia pegada

**1. El plazo que se muestra es el guardado, con su fecha de inicio visible — nunca uno
recalculado en el cliente.**
`PantallaDeReclamo` recibe `reclamo().plazoVenceEn` literal y lo pasa a `RelojDePlazo`
sin transformarlo; la prueba lo verifica leyendo el input del componente:
```
✓ src/app/rutas/cumplimiento/reclamos/pantalla-de-reclamo.spec.ts (2 tests)
  ✓ el plazo mostrado es el guardado en el reclamo, con su fecha visible — nunca uno recalculado en el cliente
```

**2. Toda decisión que perjudique muestra el estado del debido proceso completo.**
`PantallaDeCaso` renderiza `EscaleraDeEtapas` con las seis etapas de `debido-proceso`
(causal, notificación, plazo, descargo, decisión motivada, apelación), calculadas por
`etapasDelCaso()` a partir de qué campos ya se llenaron.

**3. El acta de comité NO SE CIERRA SIN QUÓRUM — probado con un intento real.**
```
✓ src/app/rutas/cumplimiento/actas/pantalla-de-acta.spec.ts (3 tests)
  ✓ intentar cerrar una acta simulada sin quórum: la interfaz lo bloquea
  ✓ con quórum, composición completa y abstenciones con motivo: el acta se puede cerrar
  ✓ una abstención sin motivo bloquea el cierre igual que la falta de quórum
```
El botón queda `disabled` (`puedeCerrarActa()`), y el intento de click no cambia el
estado interno de "cerrada" ni el bloqueo dice "No hay quórum...".

**4. Rechazar u observar SIN CAUSAL DEL CATÁLOGO es imposible.**
```
✓ src/app/rutas/cumplimiento/casos/pantalla-de-caso.spec.ts (2 tests)
  ✓ rechazar u observar SIN CAUSAL DEL CATÁLOGO es imposible: el botón de confirmar queda deshabilitado sin selección
```
`GrupoRadio` solo ofrece las cuatro causales de `CATALOGO_DE_CAUSALES`; no hay campo de
texto libre, y `puedeConfirmar()` exige que el valor elegido pertenezca al catálogo.

**5. Los formularios largos guardan borrador con `ServicioBorrador` y lo recuperan tras
una sesión caída.**
```
✓ el formulario largo guarda borrador con ServicioBorrador y lo recupera tras una sesión caída
✓ la respuesta larga guarda borrador y lo recupera tras una sesión caída
```
Ambas pruebas destruyen el `fixture` (simulan la sesión caída) y vuelven a montar la
pantalla con el mismo id, verificando que el texto vuelve.

**6. Una alerta de riesgo se muestra como razón para acompañar, nunca como condena —
hechos vs. estimaciones, claramente separados.**
```
✓ src/app/rutas/cumplimiento/alertas/pantalla-de-alertas.spec.ts (2 tests)
  ✓ muestra los hechos y las estimaciones en secciones separadas, con el mismo aviso de que no es un veredicto
  ✓ la guarda de dominio rechaza cualquier alerta redactada como condena
```
`PantallaDeAlertas` renderiza dos `PanelDeFactores` en secciones tituladas "Hechos
registrados" y "Estimaciones del modelo"; `sinLenguajeDeCondena()` es una guarda de
dominio reutilizable que rechaza frases como "probablemente incumplirá".

**7. `yarn workspace @aportaya/backoffice build/lint/typecheck/test:front/test:a11y`.**

```
$ yarn typecheck
(sin salida — 0 errores)

$ yarn lint
Linting "backoffice"... All files pass linting.
=== apps/backoffice (Angular) ===
  OK    · sin red en vista
  OK    · sin literal de diseño
  OK    · sin formato de dinero
  OK    · sin console
  OK    · ningún archivo de más de 200 líneas
  OK    · sin literal de diseño en @aportaya/ui
TODO OK

$ yarn test:front
 Test Files  13 passed (13)
      Tests  41 passed (41)

$ yarn test:a11y
 Test Files  7 passed (7)
      Tests  9 passed (9)

$ yarn build
Application bundle generation complete. [1.884 seconds]
▲ [WARNING] bundle initial exceeded maximum budget. Budget 300.00 kB was not met by
  18.97 kB with a total of 318.97 kB.
```
El build **completa** (no falla) con una advertencia de presupuesto de bundle inicial
(`angular.json`, de solo lectura para este carril); no se investigó si el excedente ya
existía en el shell de F6 o lo introdujo este carril, porque tocar el presupuesto es
decisión de quien posee `angular.json`. Se deja como hueco para quien lo posea.

**Nota sobre `./gradlew generateOpenApiClients`:** se corrió una vez al empezar el
carril para generar `clientes/angular/organizador` (usado por CU-90); es un artefacto
`.gitignore`d, no se commitea, y hay que volver a correrlo en cualquier máquina nueva
que retome esta rama.

## Qué queda abierto

- Escribir junto a quien posea `servicios/cumplimiento` los ocho micro-PR `[CONTRATO]`
  que faltan (CU-41, 42, 44, 45, 52, 53, 94, 97) y reemplazar cada
  `contenedor-de-*.ts` por su `httpResource`/`crearX()` real — el mismo patrón que ya
  usa `dominio/cu90-habilitacion.ts`.
- Construir las pantallas de CU-48 (calibración de reglas y triage de alertas) y CU-49
  (oficial de cumplimiento), y las tres pantallas de organizador que sí tienen
  contrato real y quedaron sin construir por tiempo (CU-91 contrato/firma/rescisión,
  CU-92 evaluación de desempeño, CU-93 sanción y apelación) — son la extensión directa
  del patrón de `PantallaDeHabilitacionOrganizador`.
- Confirmar el dueño de CU-61/CU-73 antes de tocar nada de `transparencia`.
- Revisar el presupuesto de bundle de `angular.json` con quien lo posea: cinco
  pantallas nuevas lo llevaron de (presumiblemente) por debajo de 300 kB a 318,97 kB.

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[22 Mapa de la maqueta · pantalla, carril y mundo]] · [[10b Estándar de ejecución del frontend]]
