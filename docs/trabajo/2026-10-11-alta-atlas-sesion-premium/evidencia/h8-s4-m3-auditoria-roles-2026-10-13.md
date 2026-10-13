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
