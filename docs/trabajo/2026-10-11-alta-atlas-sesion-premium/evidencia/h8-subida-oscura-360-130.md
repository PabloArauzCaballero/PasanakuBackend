# H8.S4.M4 — subida del expediente en oscuro

Revisión visual local del 13 de octubre de 2026. Capturas: [subida en curso, 360 × 740 dp/130 %](./h8-subida-oscura-360-130.png), [fallo de red con reintento, 360 × 740 dp/130 %](./h8-subida-fallida-oscura-360-130.png) y [fallo en ancho compacto, 320 × 640 dp/200 %](./h8-subida-error-oscura-320-200.png).

La captura en curso muestra cinco documentos sintéticos: el anverso del carnet en subida y las otras cuatro caras pendientes. Los rótulos y estados se leen completos, «Continuar» permanece deshabilitado y «Cancelar la subida» es visible.

La captura de fallo muestra el error sintético de falta de conexión en el anverso, «Reintentar» y «Repetir foto del anverso»; las otras cuatro caras siguen pendientes y «Continuar» está habilitado. No se muestran rutas locales ni datos personales. En 360 × 740/130 %, el error y ambas acciones se leen completos sin recorte ni superposición. La fixture encontró un `RenderFlex overflowed by 21 px` entre las acciones cuando estaban en `Row`; `FilaDeSubida` ahora las distribuye con `Wrap`.

La captura compacta confirma que el error sigue legible a 320 × 640/200 %. El título de la barra se trunca con la escala predeterminada de una línea, así que `PantallaDeSubidaDelExpediente` lo presenta en dos líneas desde 180 %; el test comprueba el título completo, ausencia de excepción y que «Repetir foto» se puede llevar a la zona visible desplazando la lista. A este tamaño las dos acciones no están simultáneamente en pantalla sin scroll; la vista conserva la acción secundaria accesible.

El golden `apps/movil/test/goldens/h8_subida_oscura_test.dart` usa proveedores y datos falsos; no accede a red, cámara, archivos reales ni backend. Comprueba además el desmontaje de la pantalla durante una subida pendiente, que se cancele/resetee el estado sin excepción y que la acción de reintento de la vista invoque al notifier. Estas dos combinaciones de viewport/escala no sustituyen los demás tamaños/temas/estados, navegación global ni dispositivo.

Verificación dirigida, ejecutada en serie desde `apps/movil`:

- `flutter test --no-pub --concurrency=1 --reporter expanded test/goldens/h8_subida_oscura_test.dart --update-goldens`: 2/2 PASS con las tres capturas; repetición final sin actualización: 2/2 PASS.
- `flutter test --no-pub --concurrency=1 --reporter expanded test/widget/pantalla_subida_del_expediente_test.dart`: 2/2 PASS.
- `flutter test --no-pub --concurrency=1 --reporter expanded test/unidad/subida_del_expediente_test.dart`: 10/10 PASS.
- `dart analyze --fatal-infos test/goldens/h8_subida_oscura_test.dart lib/pantallas/identidad/fila_de_subida.dart lib/pantallas/identidad/pantalla_subida_del_expediente.dart`: sin incidencias.
- `python ..\..\scripts\verificar_frontend.py movil`: TODO OK, 11/11.

La prueba encontró que `dispose()` leía providers durante el desmontaje y podía lanzar una excepción; se corrigió la pantalla para conservar referencias/estado observados y diferir la limpieza del seguimiento hasta después del teardown. Los tests dirigidos anteriores pasan con esa corrección.

H8.S4.M4 continúa `A MEDIAS`: esta captura cubre únicamente una vista oscura y no demuestra el criterio para todas las pantallas del alcance.
