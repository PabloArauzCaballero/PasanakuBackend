# H10.S3.M4 — lectura al 200 %

Fecha: 2026-10-12. Flutter 3.44.8 / Dart 3.12.2. Viewport lógico 360×740, DPR 1, `TextScaler` verificado a 2×. Las pantallas largas se revisaron en posiciones superior, intermedia y desplazada: el contenido continúa en scroll, sin solapamientos ni overflow de renderizado.

Comando de verificación (desde `apps/movil`):

```text
flutter test --concurrency=1 --reporter expanded test/goldens/h10_texto_200_test.dart --timeout 60s
00:03 +3: All tests passed!
EXIT_CODE=0
```

La captura inicial mostró dos defectos y se corrigieron antes de aprobar la revisión:

- Ingreso: el hint de contraseña «Mínimo 8 caracteres» se truncaba en la caja al 200 %. Se acortó a «Mínimo 8», visible entero y conservando la indicación de longitud en el contexto de contraseña.
- Alta: el rótulo «Segundo nombre (opcional)» se cortaba al envolver porque `Campo.alturaMinimaEtiqueta` imponía una altura máxima fija. Se cambió a una altura mínima, de modo que el rótulo crece a dos líneas.
- Billetera: las etiquetas de acciones se truncaban y el monto alto se partía entre grupos de miles. Las acciones se apilan al escalar 150 % o más y `EncabezadoDeSaldo` ajusta solo el monto para mantenerlo en una línea.

Capturas generadas con `--update-goldens`, luego ejecutadas sin esa opción y revisadas visualmente:

| Pantalla/posición | Evidencia | Revisión |
|---|---|---|
| Ingreso, inicial | [ingreso-360-200.png](./h10-texto-200/ingreso-360-200.png) | Encabezado, celular y salida secundaria legibles; la contraseña queda en el tramo desplazable. |
| Ingreso, desplazada | [ingreso-360-200-desplazado.png](./h10-texto-200/ingreso-360-200-desplazado.png) | Celular, contraseña, hint, recuperación, custodia y acciones completos; sin superposición. |
| Alta, inicial | [alta-360-200.png](./h10-texto-200/alta-360-200.png) | Cuatro campos; el rótulo opcional ahora ocupa dos líneas sin cortarse. |
| Alta, fecha | [alta-fecha-360-200.png](./h10-texto-200/alta-fecha-360-200.png) | Día, mes, año y ayuda completos y distinguibles. |
| Alta, desplazada | [alta-360-200-desplazado.png](./h10-texto-200/alta-360-200-desplazado.png) | Cotejo, continuar y tarjeta de confianza visibles; scroll esperado. |
| Billetera, inicial | [billetera-360-200.png](./h10-texto-200/billetera-360-200.png) | `Bs 9.999.999,99` en una línea y acciones «Aportar / Recargar / Retirar» completas. |
| Billetera, desplazada | [billetera-360-200-desplazado.png](./h10-texto-200/billetera-360-200-desplazado.png) | Vacíos de pasanaku y movimientos y texto de custodia legibles. |

`tester.takeException()` fue nulo tras montar y desplazar cada superficie. Validación adicional: 5/5 casos H8 break-ui, 7/7 pruebas de saldo, `flutter analyze --fatal-infos` limpio en la app y paquete de diseño. H8 actualizó únicamente la captura propia del nombre largo que cambió por el rótulo arreglado; no se regeneraron goldens históricos de H10.
