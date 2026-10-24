# Doble revisión de las capturas — pestaña «Operaciones» de la ficha del catálogo (regla 35)

- Fecha: 2026-10-24 · Alcance: `/tablero/catalogo-de-datos/<esquema>/<tabla>?vista=operaciones`
- Capturas: Playwright contra Prism (servidor simulado), **no** la API real; 3 anchos (1280, 768, 360) × 2 temas.
- **Veredicto final: ACEPTABLE CON RESERVAS** (18 de 18 capturas finales; 4 APROBADA, 14 ACEPTABLE CON RESERVAS; ninguna RECHAZADA; sin hallazgos BLOQUEANTE ni MAYOR abiertos).
- Quién revisó: primera pasada (verificación) quien implementó; segunda pasada (adversarial) el agente independiente `visual-reviewer`, tres rondas. La regla 35.1.6 prohíbe que la segunda pasada la haga quien implementó: no se hizo.

## Errores propios que se declaran
1. En el encargo de la ronda 1 se le pasó al revisor, como «contexto de diseño», una regla de otro producto (la de fichas «en UNA tarjeta con pestañas», del `CLAUDE.md` de `mantra-core-health`). No aplica a Pasanaku. El hallazgo H7 de la ronda 1 que se apoyaba en ella se **descartó**. Las rondas 2 y 3 se encargaron sin esa regla.
2. La primera captura se hizo en la carpeta de evidencia de otro trabajo (`2026-10-08-endurecimiento-integral`) y pisó PNG ajenos; se restauraron con `git checkout` y las capturas finales van en esta carpeta.

## Ronda 1 → RECHAZADA (móvil) · `capturas/`
Bloqueantes: pestaña activa recortada a 360 px («Opera»); chip «Activa» cortado. Mayores: vacío que no orienta y que afirmaba algo falso; sin título de sección; rutas partidas a mitad de palabra; pestaña «Gobierno» con aspecto de activa en tablet. **Correcciones:** acciones de cabecera y tira de pestañas segmentada envuelven (no hay scroll lateral que oculte); hover distinto de la activa; título y descripción en la sección de directas; copy del vacío con por qué y qué hacer; cortes `<wbr>` tras cada «/»; enlaces de endpoint a su ficha; método con ancho fijo; tarjetas alineadas arriba; sin textos internos («bóveda», «docs/CasosDeUso»).

## Ronda 2 → ACEPTABLE CON RESERVAS · (carpeta reemplazada por v3)
Hallazgos: contador «6» contradice «sin operaciones» (B1, MAYOR); no existía captura de la tabla con filas (B2, MAYOR); copy de infraestructura remitía a un vacío (B3); código `CU-NN` con el mismo estilo que un enlace (B4); píldora activa débil en oscuro (B5). **Correcciones:** contador retirado; captura sintética con 210 operaciones (la fuente empaquetada solo nombra `outbox_evento`, que no es una tabla del catálogo real); copy según haya o no casos de uso; `CU-NN` en gris, enlaces subrayados; borde fino en la píldora activa. Al capturar la tabla con filas aparecieron otros defectos reales, también corregidos: 210 filas renderizadas de golpe (axe agotó 30 s) → paginación de a 10; columna «Acciones» fijada a la derecha tapaba «Impacto» y duplicaba el enlace de la ruta → retirada; ruta e impacto recortados → envuelven.

## Ronda 3 (final) → ACEPTABLE CON RESERVAS · `capturas-v3/` (18 PNG)
| Captura | Nota | Reservas |
|---|---|---|
| `ficha-operaciones-{escritorio,tablet}-{light,dark}` (4) | APROBADA | — |
| `ficha-operaciones-movil-{light,dark}` (2) | ACEPTABLE CON RESERVAS | R8 |
| `ficha-operaciones-infra-*` (6) | ACEPTABLE CON RESERVAS | R3 |
| `ficha-operaciones-directas-escritorio-*` (2) | ACEPTABLE CON RESERVAS | R2, R4 |
| `ficha-operaciones-directas-tablet-*` (2) | ACEPTABLE CON RESERVAS | R2, R4, R7 |
| `ficha-operaciones-directas-movil-*` (2) | ACEPTABLE CON RESERVAS | R1, R5, R6, R10 + límite de resolución |

### Reservas abiertas (todas MENOR; se llevan a H4, no se ocultan)
- R1 Título de la tabla de directas a 360 px muy angosto (3 líneas).
- R2 Título repetido: el `h2` de la sección y la cabecera de la tarjeta de la tabla dicen lo mismo, con la jerarquía invertida.
- R3 El copy de infraestructura es condicional («Si es infraestructura…») y la ficha no muestra insignia de infraestructura.
- R4 Enlace inconsistente: en directas enlaza la ruta; en las tarjetas de casos de uso enlaza el `operationId`.
- R5 Cortes dentro de palabra en móvil para `operationId` largos (`recibirWebhookPasa/rela`).
- R6 «Impacto / Uso» alineado a la derecha, en columna estrecha, en móvil.
- R7 Columnas mal repartidas a 768 px en directas.
- R8 Separador «· servicio» huérfano en tarjetas de casos de uso en móvil.
- R9 Huecos en la grilla de tarjetas a 1280 px (alineadas arriba).
- R10 La página de directas mide ~4850 px en móvil con 10 filas.
- Conocidas, previas a esta entrega y fuera de ella: migas con «Ficha de tabla» repetido; banner de ayuda «Inicio y Tableros» en la ficha del catálogo; pestañas en 3 filas a 360 px (decisión tomada: envolver antes que esconder).

## No cubierto
Teclado, foco visible, orden de tabulación y destino de los enlaces; estados cargando/error/sin permiso de esta pestaña; paginación más allá de la página 1; anchos intermedios; lector de pantalla; zoom 200 %; **comportamiento contra la API real**. La barra lateral que termina a ~900 px en las capturas de página completa se trata como artefacto de la captura (no verificado en un navegador real).

## Hallazgo de datos que esta revisión destapó
El inventario de «operaciones directas» (`sistemas/operaciones/dominio/operaciones.json`, 1,4 MB empaquetado en el frontend) tiene 314 entradas y **una sola tabla distinta** referenciada, `outbox_evento` (210 veces, con servicio «src» y operación «INSERT»). Es dato de relleno, no un inventario: por eso «Operaciones directas» casi siempre sale vacía. Registrado en el plan como defecto de datos (H4).
