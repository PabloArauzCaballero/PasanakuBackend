# H10.S1.M6 — revalidación de goldens de la app con Flutter 3.47.5

Fecha: 2026-10-13. Ejecución local en Windows; pruebas seriales, sin actualizar archivos de referencia.

```text
flutter --version
Flutter 3.47.5 • channel stable • https://github.com/flutter/flutter.git
Framework • revision 6a19cca564 (4 weeks ago) • 2026-09-17 14:13:22 -0400

flutter test --no-pub --concurrency=1 --reporter compact --timeout 60s test/goldens
02:05 +52 -216: Some tests failed.
EXIT_CODE=1
```

El runner recorrió 268 casos: 52 pasaron y 216 fallaron. Las salidas observadas son fallos de comparación golden; ejemplos de diferencias fueron 0,39 % y 78,67 % de píxeles. La matriz de bienvenida, ejecutada aparte con el mismo SDK, pasó 6/6; el test widget del paso de contrato pasó 2/2. No se usó `--update-goldens`.

**VEREDICTO:** FAIL  
**Peldaño:** WRITTEN (la regresión visual de la app no está verde)  
**Superficie:** Mobile · golden tests  
**Comando:** `flutter test --no-pub --concurrency=1 --reporter compact --timeout 60s test/goldens` desde `apps/movil`  
**Demuestra:** el conjunto de referencias actual no coincide con 216 renders de Flutter 3.47.5 en Windows.  
**No cubierto:** causa de cada diff, revisión individual de los PNG de fallo, comparación en runner canónico Linux y golden update aprobado. No se atribuyen las diferencias exclusivamente al rasterizador: pueden incluir cambios reales de UI.

Inspección de dos pares representativos bajo `apps/movil/test/goldens/failures/` confirmó que no todos los diffs son de rasterizador: el ingreso actual perdía «caracteres» del placeholder. Se corrigió a `Mínimo 8 caracteres` y una aserción exacta pasó; además, se alinearon los campos de nombres en dos columnas sin añadir espacio a la vista apilada. Esto reduce dos causas funcionales detectadas, no clasifica el conjunto.

Después de esos cambios, el test widget del ingreso pasó 4/4 y el test de campos del alta pasó 4/4; el análisis dirigido quedó limpio. La captura H10 de datos a 412 dp/130 % pasó 1/1. La regresión de transiciones sigue parcial: `transiciones_golden_test.dart` falló en tres referencias (aparece: 0,12 %; asienta: 7,50 %; final: 0,12 %); la referencia «asienta» muestra solo el celular mientras el render actual ya muestra el campo de contraseña. No se regeneró ninguna baseline Windows.

H10.S1.M6 permanece `A MEDIAS`. Próximo paso: volver a ejecutar el barrido completo después de estos retoques, agrupar las fallas restantes por imagen/tema/viewport, inspeccionar sus pares `masterImage`/`testImage` y validar las actualizaciones intencionales en el runner canónico.
