# H8.S1.M3 — barrido break-ui en alta, billetera y perfil

Fecha: 2026-10-12. Barrido automatizado con Flutter 3.44.8 / Dart 3.12.2. Viewport lógico 360×740, DPR 1, texto del sistema al 200 %. Se comprobó el factor con `MediaQuery` y se exigió `tester.takeException() == null` en los cinco casos.

Comando desde `apps/movil`:

```text
flutter test --concurrency=1 --reporter expanded test/goldens/h8_break_ui_test.dart --timeout 60s
00:01 +5: All tests passed!
EXIT_CODE=0
```

| Pantalla / datos | Captura | Resultado visible | Hallazgo y propuesta |
|---|---|---|---|
| Alta — primer nombre `Áñé` repetido hasta 60 caracteres, apellidos `Peña` / `Álvarez` | [alta-datos-60-200.png](./h8-break-ui/alta-datos-60-200.png) | Sin excepción de layout; tildes y `ñ` se conservan. El campo de una línea muestra solo el tramo visible del nombre largo. | No hay desborde. Para revisar/edititar un dato tan largo con más seguridad, considerar foco/cursor que revele el extremo editado o un resumen accesible del valor completo. |
| Alta — correo de 150 caracteres | [alta-cuenta-email-150-200.png](./h8-break-ui/alta-cuenta-email-150-200.png) | Sin excepción; el correo completo llega al controlador. Campo de una línea enseña un fragmento y el texto auxiliar termina en elipsis a 200 %. | Elipsar el texto auxiliar o darle más espacio vertical; al editar, mantener visible el tramo del correo bajo el cursor y exponer el valor completo a lector de pantalla. |
| Billetera — saldo disponible `9999999.99`, 0 pasanakus y 0 movimientos | [billetera-saldo-alto-vacios-200.png](./h8-break-ui/billetera-saldo-alto-vacios-200.png) | Hallazgo inicial confirmado y corregido: las acciones ahora se apilan al 150 % o más y muestran sus verbos completos; el saldo se reduce solo lo necesario para mantener los grupos de miles juntos en una línea. No hay excepción de layout. | Fix aplicado en `AccionesDeSaldo` y `EncabezadoDeSaldo`; la captura regenerada muestra «Aportar», «Recargar», «Retirar» y `Bs 9.999.999,99` íntegros. |
| Billetera — saldo cero, tema oscuro | [billetera-saldo-cero-200.png](./h8-break-ui/billetera-saldo-cero-200.png) | El estado vacío de movimientos es legible, cabe y no produce excepción. | No se observó rotura en este estado. |
| Billetera — 40 movimientos, texto al 200 % | [claro](./h8-break-ui/billetera-40-movimientos-200.png) / [oscuro](./h8-break-ui/billetera-40-movimientos-200-oscuro.png) | La prueba confirma que están las 40 filas; ambas capturas al extremo inferior muestran tres filas simultáneas, montos, fecha y glosas truncadas sin overflow. En la nueva captura oscura, etiquetas e importes contrastan y se leen completos. | Se conserva la tarjeta desplazable de la pantalla; verificar cada glosa completa requiere abrir una fila/detalle en trabajo futuro. |
| Perfil — correo de 150 caracteres | [perfil-email-150-200.png](./h8-break-ui/perfil-email-150-200.png) | Campo desplazado al extremo del cursor; guardar, contraseña, guía, cierre y baja permanecen legibles, sin excepción. | El valor completo se conserva y puede recorrerse en el campo; no se detectó desborde. |

Actualización posterior, 2026-10-12: el feed real ya está conectado a `PantallaDeSaldo`. Se añadió una regresión con 40 movimientos y texto al 200 %: comprueba la primera y última fila, las 40 filas y la ausencia de excepciones; captura el extremo inferior de la lista. Las cinco capturas originales tenían UI obsoleta de billetera, así que se regeneraron las seis capturas de esta suite después de revisar visualmente sus resultados. Ejecución con actualización y repetición sin actualización: PASS 6/6 en ambas.

Las cinco capturas se generaron con `--update-goldens` únicamente para crear estos artefactos propios; después de corregir la etiqueta de alta al 200 %, se regeneró y volvió a inspeccionar `alta-datos-60-200.png`. La suite repetida sin `--update-goldens` pasa 5/5. No se regeneraron las baselines preexistentes de H10.

Ampliación H8.S4.M4, 2026-10-13: se agregó una variante oscura del feed sintético de 40 movimientos al 200 %. Captura revisada manualmente: tres filas visibles en el extremo inferior, importes y fecha legibles, sin recorte ni overflow. Desde `apps/movil`, `flutter test --no-pub --concurrency=1 --reporter expanded --update-goldens test/goldens/h8_break_ui_test.dart --timeout 60s` y repetición sin actualización pasaron 6/6; análisis dirigido limpio y gate frontend TODO OK 11/11. Se generó solo el PNG nuevo; no se regeneraron las demás imágenes.
