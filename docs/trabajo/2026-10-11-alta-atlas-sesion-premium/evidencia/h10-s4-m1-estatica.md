# H10.S4.M1 — revisión estática de logs (parcial)

Fecha: 2026-10-12. Se buscó en todo `apps/movil/lib` cualquier salida por `print`, `log(`, `debugPrint`, y después se revisaron expresamente las llamadas ejecutables:

```text
rg -n -i 'print|log\(|debugPrint' apps/movil/lib
```

La búsqueda amplia también coincide con palabras de documentación. Al revisar llamadas Dart reales, el único logger es `developer.log` en `apps/movil/lib/pantallas/identidad/dominio/cu04_autenticar.dart:83`; su mensaje solo indica que el servidor no entregó `tokenRefresco`. No incluye token, refresh, contraseña, CI, archivo ni valor de sesión. No se encontraron `print`, `debugPrint` ni otros loggers ejecutables.

```text
rg -n -i -g '*.dart' 'developer\.log|print\(|debugPrint\(|logger\.' apps/movil/lib
apps/movil/lib\pantallas\identidad\dominio\cu04_autenticar.dart:83:        developer.log(
```

La verificación de runtime no se da por hecha. `flutter devices` detectó solo Windows y Chrome; `flutter emulators` enumeró Pixel 2 y Samsung A06-L, pero no se arrancó ninguno. La máquina reportó 2,487,888 KiB de memoria física libre (~2.37 GiB); para respetar la restricción de RAM no se inició una VM móvil durante este barrido. Por tanto aún falta observar `flutter logs` durante un recorrido de alta en Android. Estado honesto: evidencia estática completada, criterio total pendiente; H10.S4.M1 queda `A MEDIAS`.
