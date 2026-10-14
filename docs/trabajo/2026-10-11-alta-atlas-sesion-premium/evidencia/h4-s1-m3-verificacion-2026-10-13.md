# H4.S1.M3 — ejemplos en los campos del alta y del ingreso

Fecha: 2026-10-13. Se comparó la referencia de ingreso con la captura actual y se detectó que el hint de contraseña era más corto que el ejemplo definido en el plan.

Prueba roja, antes de corregir el texto:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --timeout 60s test/identidad/pantalla_sesion_prefijo_test.dart
Expected: 'Mínimo 8 caracteres'
  Actual: 'Mínimo 8'
00:03 +3 -1: Some tests failed.
EXIT_CODE=1
```

Se restauró el ejemplo completo en `TextosDelAlta.ejemploContrasena`. La prueba del ingreso comprueba ahora el valor exacto; la prueba de datos se alineó con los cuatro `TextField` actuales del paso, sin contar como campo de texto el selector de fecha.

Verificación final, desde `apps/movil`:

```text
flutter test --no-pub --concurrency=1 --reporter expanded --timeout 60s test/widget/campos_del_alta_test.dart
00:02 +4: All tests passed!
EXIT_CODE=0

flutter test --no-pub --concurrency=1 --reporter expanded --timeout 60s test/identidad/pantalla_sesion_prefijo_test.dart
00:03 +4: All tests passed!
EXIT_CODE=0

dart analyze --fatal-infos lib/pantallas/identidad/textos_del_alta.dart lib/pantallas/identidad/pasos_alta/campos_de_nombre.dart test/identidad/pantalla_sesion_prefijo_test.dart test/widget/campos_del_alta_test.dart
No issues found!
EXIT_CODE=0
```

El componente de nombres conserva una columna a 360 dp; en dos columnas a 412 dp, las cajas quedan alineadas aunque «Segundo nombre (opcional)» envuelva. La reserva de altura solo aplica en dos columnas, para evitar espacio extra en la disposición compacta.

**VEREDICTO:** PASS para los hints y el layout simulado cubiertos por estas pruebas.  
**Peldaño:** TESTED.  
**No cubierto:** captura física del alta (registrada bajo H4.S2.M4); la suite global de goldens de la app sigue parcial bajo H10.S1.M6.
