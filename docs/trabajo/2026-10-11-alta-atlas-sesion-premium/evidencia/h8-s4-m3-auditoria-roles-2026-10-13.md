# H8.S4.M3 — cotejo estático de roles de color

Fecha: 2026-10-13. Rama: `codex/alta-atlas-sesion-premium-2026-10-11`.

## Alcance y método

Se cotejaron los usos de color de cuatro grupos de acciones descritos en la revisión color-focalizada de `h8-s4-m3.md` con los widgets Flutter que los implementan y con la asignación de color del sistema de diseño. Esta nota es una auditoría estática complementaria: no vuelve a inspeccionar píxeles ni reemplaza las capturas citadas.

## Hallazgos cotejados

| Superficie observada en evidencia previa | Implementación y rol efectivo | Lectura frente al criterio literal |
|---|---|---|
| Billetera: «Recargar» y «Retirar» aparecen verdes como acciones secundarias; «Aportar» es naranja. | `AccionesDeSaldo` asigna `BotonVariante.primario` a Aportar y `fantasma` a Recargar/Retirar (`apps/movil/lib/pantallas/billetera/acciones_de_saldo.dart`). `Boton` asigna `brandTexto` y borde `brand` a la variante fantasma (`packages/diseno_flutter/lib/atomos/boton.dart`). | El verde identifica acciones secundarias, no un estado. El comentario del enum dice «Verde = estructura, naranja = acción» (`boton_variante.dart`), que no coincide literalmente con «verdes/rojos solo para estado» del plan. |
| Perfil: fila para cambiar contraseña y fila para volver a la guía; cierre de sesión y baja de cuenta. | Las dos filas de navegación usan texto neutral `t.text` y chevrón `t.text3` (`fila_de_accion_de_perfil.dart`). Cerrar sesión usa `fantasma` y por tanto `brandTexto`; baja usa `peligro` y por tanto `rojoSolido` (`pantalla_perfil.dart`, `boton.dart`). | La nota visual previa que agrupaba cambio de contraseña y guía como acciones verdes no coincide con su código actual. Sí se confirma verde en cerrar sesión y rojo en la acción destructiva de baja; son acciones, no estados. |
| Ingreso: «¿Olvidaste tu contraseña?» y «Crear mi cuenta». | Ambos son `TextButton` (`sesion_pie.dart`); el tema global asigna `brandTexto` a `TextButton` habilitados (`tema.dart`, `tema_de_controles.dart`). | Enlaces accionables con color de marca, no indicadores de estado. |
| Subida fallida: «Reintentar» y «Repetir foto», junto al mensaje de error. | Los dos controles son `TextButton`; el error se presenta con `Alerta(tono: Tono.error)` (`fila_de_subida.dart`) y el tema da `brandTexto` a esos botones. | El rojo comunica el error con alerta; el verde sigue identificando las acciones de recuperación, no el estado. |

## Resultado y límites

El código confirma usos de verde en acciones secundarias/enlaces y de rojo en una acción destructiva. La implementación declara una política semántica propia («verde = estructura, naranja = acción») para varias variantes; el requisito de H8.S4.M3 es más estricto y no documenta excepciones para enlaces, acciones secundarias o destrucción. No corresponde cambiar paleta ni declarar conformidad sin aplicar esa regla literal o contar con una decisión de producto/diseño registrada.

VEREDICTO: BLOCKED para decidir conformidad global; la inspección estática de los cuatro grupos quedó completada.
Peldaño: DISCOVERED (widgets y asignaciones de tokens localizados; sin modificación de producto).
Superficie: Mobile, revisión estática.
Comando: `rg -n 'variante: BotonVariante\\.(fantasma|peligro|primario)|TextButtonThemeData|botonTextoDesde|TextButton\\(' apps/movil/lib/pantallas/billetera/acciones_de_saldo.dart apps/movil/lib/pantallas/identidad/pantalla_perfil.dart apps/movil/lib/pantallas/identidad/sesion_pie.dart apps/movil/lib/pantallas/identidad/fila_de_subida.dart packages/diseno_flutter/lib/tema.dart packages/diseno_flutter/lib/tema_de_controles.dart packages/diseno_flutter/lib/atomos/boton.dart`
Salida: coincidencias localizadas en los cuatro widgets y en los mapeos globales; no se ejecutaron pruebas.
Demuestra: el color verde/rojo señalado por la revisión tiene usos accionables explícitos y la observación previa del perfil necesita precisión.
No cubierto: inspección visual directa en esta sesión; resto de pantallas/estados; grupos/detalle sin ruta local; gate Vitest de bóveda (Vitest no instalado según evidencia previa); decisión de política y correcciones resultantes.

## Siguiente acción

Aplicar el criterio literal del plan a los usos accionables identificados —o registrar una decisión de producto/diseño que defina excepciones— y después ejecutar pruebas visuales/contraste de regresión. Hasta entonces H8.S4.M3 permanece `A MEDIAS`.

## Continuación local 2026-10-13 — primer lote de neutralización

Se aplicó el criterio literal del plan, sin cambiar los tokens fuente: controles secundarios y datos financieros que aún comunicaban con verde ahora usan el acento naranja o texto/superficie neutrales. Cambios en `BotonIcono`, `Avatar`, `CampoDeFecha`, `AccionesRapidas`, `TarjetaSaldo`, `FilaDeMovimiento`, `TarjetaDeOferta`, `ListaDeMovimientos`, `DesgloseDeCobro`, `ResumenDePeriodo`, `MedidorDeRango`, `PanelSorteo`, `NotificacionEmergente`, `Acordeon` y `TarjetaDelGrupo` (`apps/movil/lib/pantallas/pasanaku/tarjetas_de_unirse.dart`). En la app también `GarantiasDePortada`, `LaminaDelTour`, `ConsejosParaLaFoto` y el título de `ResumenDeAvance` usan roles neutrales. La barra del medidor estimado usa el mismo naranja de acción; no representa un estado de éxito.

### Pruebas y resultados

- Rojo previo reproducido: `flutter test --no-pub --concurrency=1 --reporter compact test/widget/roles_de_color_no_estado_test.dart` detectó primero verde en el botón de ícono y, al ampliar los casos, en la compensación de una oferta (ambos temas).
- Verde actual: ese test focalizado pasa **2/2** (claro/oscuro) y comprueba controles, superficies e importes de saldo/movimiento/oferta/extracto/cobro/resumen/rango, además de numeración de sorteo e icono de notificación.
- `roles_de_color_accion_test.dart`: **2/2 PASS**; `campo_de_fecha_test.dart`: **7/7 PASS**; `movimiento_test.dart`: **11/11 PASS**; app `pantalla_unirse_test.dart`: **3/3 PASS**.
- App `roles_color_acento_test.dart`: **4/4 PASS** en claro/oscuro; comprueba garantías, titular de Tour, consejos, título de avance, tarjeta de captura (icono, marco y degradado) con roles neutrales, el fondo normal/expandido de acordeón en `surface2` y la categoría «marca» neutral de la bandeja. `pantalla_camara_de_respaldo_test.dart`: **5/5 PASS**. Análisis dirigido del app: **No issues found**.
- `dart analyze --fatal-infos` en `packages/diseno_flutter`: **No issues found**. Contraste: `node --test pruebas/roles-contraste.test.mjs pruebas/texto-de-ejemplo-contraste.test.mjs` en `packages/tokens`: **3/3 PASS**.
- Regresiones visuales sin `--update-goldens`: el catálogo del diseño detectó diferencias en Átomos/botones y Moléculas (ambos temas); H10 Tour **24/24** celdas y Portada **4/4** celdas dieron diferencias pixelarias después de los cambios de color; H10 Cámara **6/6** también antes del ajuste final de superficie del acordeón, y una repetición actual focalizada sigue detectando diferencia esperada. No se regeneró ninguna baseline. Abrí los `_testImage.png` de portada, Tour y cámara: los textos y las acciones quedan íntegros, y el acordeón cerrado ya usa `surface2` (rol de superficie); el arte de marca permanece verde. Falta terminar el barrido de color y revalidar el conjunto antes de actualizar exclusivamente las referencias revisadas.

### Límite y próximo trabajo

Este lote no cierra la microtarea. `TarjetaDeCaptura` ya usa superficie y controles neutrales, verificados en ambos temas. Siguen pendientes el cotejo de usos de verde en logotipo/ilustraciones y otros resultados de `rg 'brandTexto|brandBg|verdeSolido|sobreVerdeSolido' packages/diseno_flutter/lib apps/movil/lib`. Selección/foco/progreso y éxito explícitos se conservan como semánticos; la identidad corporativa en logotipo/arte queda como excepción por resolver sin rediseñar la marca a ciegas. La galería de goldens del catálogo no sustituye el cotejo de cada pantalla del alcance; H8.S4.M3 sigue `A MEDIAS`.

## Continuación local 2026-10-13 — identidad y arte

Se cotejó el rol de color de la marca y el fondo de apertura. El contorno exterior del isotipo y la palabra «Aporta» dejaron de duplicar el verde como segundo acento; el detalle naranja se conserva. El halo y los arcos decorativos del fondo usan superficies neutrales, conservando su destello de acento. La portada mantiene un único sello naranja con texto de contraste. En las ilustraciones de pautas, el carnet y su silueta dejaron de usar verde y la boca decorativa dejó de usar rojo; el rostro mantiene el acento naranja compartido con la acción principal.

- TDD del diseño: `roles_de_color_no_estado_test.dart` falló inicialmente para el trazo exterior verde del isotipo; tras corregirlo, ambos temas pasan **4/4** (isotipo/fondo y acciones/valores).
- Los goldens de marca/apertura fallaron antes de actualizarse por el cambio intencional del trazo; tras revisar las cinco imágenes (isotipo en claro/oscuro y apertura en tres cuadros), se actualizaron solo esas referencias y la prueba pasó **2/2**.
- App: `roles_color_acento_test.dart` y `pautas_publicas_test.dart` pasan **6/6** (ambos temas y rutas públicas). `dart analyze --fatal-infos` dirigido en ambos paquetes: **No issues found**.
- La captura `pautas_publicas_360.png` detectó inicialmente **14.216 píxeles (4,94 %)** respecto a su referencia. Se inspeccionaron lado a lado base y captura actual: estructura y textos permanecen íntegros, estados «Correcto/Incorrecto» conservan verde/rojo semánticos y carnet/silueta quedan neutrales. Se actualizó solo ese golden después de esa inspección; luego la prueba sin update pasó **1/1** y se volvió a inspeccionar el PNG final. La regresión visual global y la comparación canónica siguen pendientes.
- Una primera invocación con Flutter 3.44.8 no llegó a compilar el test porque cargó fuentes Flutter 3.47.5 desde `C:\src\flutter`; se descartó ese resultado como fallo de SDK. La verificación válida se ejecutó con `C:\src\flutter\bin\flutter.bat`.

Este cotejo resuelve los usos identificados de marca y arte, pero no prueba todas las superficies del alcance, los umbrales de contraste por pantalla ni la regresión visual completa. H8.S4.M3 permanece `A MEDIAS`.

### Continuación local — ilustraciones del tour

La auditoría de las capturas H10 encontró verde decorativo en las personas, la rueda del sorteo, el paraguas y el aro del pedestal. Las formas ahora usan tonos neutrales y el acento naranja; se conserva verde solo en el tilde del comprobante verificado. Los tokens de foco, selección, pasos y progreso no se cambiaron porque describen estados.

- Se inspeccionaron las cuatro celdas H8 oscuras (360 × 740, texto 130 %) y las 21 PNG H10 nuevos/recreados —cuatro láminas × tres anchos × dos temas, reutilizando H8 para las tres primeras celdas oscuro/360— sin clipping, solapamiento ni texto ilegible.
- `h8_tour_oscuro_test.dart`: 1/1 captura (cuatro láminas) tras actualización y 1/1 sin actualización. `h10_tour_visual_test.dart`: 24/24 tras actualización y 24/24 sin actualización.
- Verificación complementaria vigente: roles de color y acción del paquete de diseño **6/6**, goldens de marca/apertura **2/2**; app roles+pautas+golden público **7/7**; análisis dirigido Dart de ambos paquetes limpio; pruebas de contraste de tokens **3/3**; gate móvil **TODO OK (11/11)**.

El lote del tour queda cotejado visualmente, pero la tarea H8.S4.M3 sigue `A MEDIAS`: falta completar el inventario visual/color de las otras superficies del alcance y el análisis de diferencias pendientes en Portada/Cámara.

### Continuación local — portada y cámara

Se inspeccionaron los cuatro renderizados H10 de Portada (412/600 dp, claro/oscuro) y los dos H8 de Portada (360 dp, claro/oscuro) después de neutralizar el lockup, el sello y garantías. La estructura y textos siguen completos; las garantías y las acciones secundarias usan roles neutrales, y las acciones primarias/enlaces comparten naranja. Se actualizaron esos seis PNG; `h10_portada_visual_test.dart` pasa **4/4** y `h8_portada_mfa_oscuro_test.dart` **4/4**, ambas pruebas sin update (las dos celdas MFA no cambiaron).

También se inspeccionaron los seis estados sintéticos de cámara concedida (360/412/600 dp en ambos temas). Se actualizaron las seis referencias porque los botones ghost/enlace ahora siguen los roles neutrales/naranja. El visor, el copy, marco y acciones no se solapan; tests H10 **6/6** con y sin update, `pantalla_camara_de_respaldo_test.dart` previo **5/5**. Sigue sin ejecutarse captura real ni prueba física de permiso.

H8.S4.M3 sigue `A MEDIAS`: este lote corrige Portada/Cámara y prueba capturas concretas, pero aún falta cerrar la tabla de roles de todas las superficies y su contraste por tema; las demás diferencias de golden de la suite global no se presumen conformes.

No se actualizó el estado del plan ni se publicaron cambios. No se regeneraron goldens.

## Continuación local 2026-10-13 — tono de categoría en bandeja

El barrido de tokens encontró que la cadena de notificación `marca` se convertía en `Tono.marca`, que pinta verde aunque identifica una novedad de producto y no un estado. Se añadió una regresión de widget para la bandeja; primero falló en oscuro y claro al recibir el frente blanco sobre verde oscuro y el frente verde sobre fondo verde claro. `PantallaDeBandeja._tonoDesde` ahora resuelve esa categoría a `Tono.neutro`, conservando los mapeos de éxito, aviso y error.

- `apps/movil/test/widget/roles_color_acento_test.dart`: **4/4 PASS** (categoría «marca» y contenido no-estado, cada uno en claro/oscuro).
- `dart analyze --fatal-infos lib/pantallas/notificaciones/pantalla_de_bandeja.dart test/widget/roles_color_acento_test.dart`: **No issues found**.
- No se modificaron baselines ni el estado oficial de H8.S4.M3. El barrido aún debe determinar la política para logotipo/ilustraciones y verificar de forma visual el conjunto de pantallas; H8.S4.M3 continúa `A MEDIAS`.
