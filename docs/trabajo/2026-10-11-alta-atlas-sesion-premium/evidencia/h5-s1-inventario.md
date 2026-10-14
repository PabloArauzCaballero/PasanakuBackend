# H5.S1 · Inventario y cambios de captura documental

Inventario estático del checkout antes de modificar:

```text
git grep -n -i -E "escribirAMano|camaraSinPermiso|camaraPocaLuz|sinCamara|elegirDeLasFotos" HEAD -- apps/movil/lib/pantallas/identidad
HEAD:apps/movil/lib/pantallas/identidad/textos.dart:122:  static const camaraSinPermiso =
HEAD:apps/movil/lib/pantallas/identidad/textos.dart:124:  static const camaraPocaLuz =
HEAD:apps/movil/lib/pantallas/identidad/textos.dart:127:  static const escribirAMano = 'Escribir a mano';
HEAD:apps/movil/lib/pantallas/identidad/textos.dart:128:  static const elegirDeLasFotos = 'Elegir de mis fotos';
HEAD:apps/movil/lib/pantallas/identidad/textos.dart:129:  static const sinCamara =
```

Lectura del runtime: esos cinco textos no tenían consumidores; el alta sí exponía
atajos de carnet ficticio solo en debug en el pie del carrusel y en la pantalla de
cámara. El puerto existente `Camara.elegirDeLasFotos()` ya era implementado por
`CamaraDelSistema` con `ImageSource.gallery`, aunque el alta no lo conectaba.

Cambios realizados:

- Retirados los textos heredados de escritura manual y ambos atajos de carnet de
  prueba que permitían evitar una captura real.
- El aviso de permiso de cámara muestra ahora «Permitir cámara» o «Abrir ajustes»
  según el estado y ofrece «Elegir de la galería». La selección vuelve a la cara
  activa; se conserva el validador de archivo y sus umbrales.
- Añadida prueba widget de los dos estados del permiso, el control de galería y la
  ausencia de la opción «Escribir a mano» en el aviso.

Verificación estática posterior: `rg` no encuentra los símbolos de copy manual ni
las callbacks/ramas del carnet ficticio retiradas. `dart format` se ejecutó sobre los
archivos Dart tocados y `git diff --check` terminó sin errores (solo mostró avisos
globales de conversión LF/CRLF en archivos ya modificados del checkout).

No se ejecutaron pruebas Flutter, análisis ni builds por el límite de recursos de
esta intervención. La prueba widget y el flujo nativo de `image_picker` quedan por
validar. «Usar esta foto igual» tras rechazo local tampoco está disponible: el
validador actual rechaza la imagen y esta tarea no modificó la calidad de captura.
