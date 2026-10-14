# H8.S4.M3 — revalidación de roles y contraste (2026-10-13)

Esta nota supersede los hallazgos accionables que quedaron desactualizados en la auditoría inicial y añade la regresión más reciente. No cambia el estado oficial de la microtarea.

## Corrección del hallazgo de contraste

El test `packages/diseno_flutter/test/a11y/catalogo_a11y_test.dart` detectó que la muestra «Retirar» fallaba con **1,04:1** en claro y oscuro: utilizaba `BotonVariante.sobreVerde` sobre una tarjeta que ya no era verde. La variante solo se usaba en esa muestra y el texto blanco no tenía contraste sobre la superficie neutral. Se retiró la variante del enum y de `Boton`; la muestra usa ahora `BotonVariante.secundario`. Esto elimina además una opción API obsoleta que permitía verde en una acción.

Verificación después de la corrección:

- `flutter test --no-pub --concurrency=1 --reporter compact test/a11y/catalogo_a11y_test.dart test/widget/roles_de_color_accion_test.dart`: **16/16 PASS** (14 a11y + 2 roles de acción).
- `dart analyze --fatal-infos lib test/widget/roles_de_color_accion_test.dart`: **No issues found**.
- `flutter test --no-pub --concurrency=1 --reporter compact test/goldens/catalogo_golden_test.dart`: **14/14 PASS** sin update, tras inspección y actualización de 10 PNG en botones, indicadores, ilustraciones, moléculas y organismos.
- `flutter test --no-pub --concurrency=1 --reporter compact test/goldens/ilustraciones_golden_test.dart`: **2/2 PASS** sin update, tras inspeccionar y actualizar las dos referencias de las ilustraciones grandes del tour.
- Suite completa de `packages/diseno_flutter`: PASS sin `--update-goldens`, Flutter 3.47.5/Windows, `--no-pub --concurrency=1 --timeout 60s` (124 pruebas: la corrida anterior mostró 122 PASS + las dos ilustraciones ya resueltas).

Las ilustraciones actualizadas conservan el verde solo en la marca de comprobante verificado; personas, disco de sorteo y paraguas son neutrales más el acento naranja. No se observaron cambios de geometría en las muestras inspeccionadas.

## Alcance y pendiente

Los cambios previos neutralizaron el verde decorativo/no semántico en marca, tour, portada, arte de pautas y controles/acciones revisados; se preservan verdes/rojos que codifican estados (selección, foco, progreso, éxito y error, con señal textual/forma cuando corresponde). Las suites H10 focalizadas de Tour (24/24), Portada (4/4), Cámara (6/6), y pauta pública (1/1) pasan sin update según sus evidencias.

No se cierra H8.S4.M3 todavía: falta terminar el cotejo cromático por pantalla y estado y confirmar explícitamente los pares de contraste usados en cada superficie del alcance. La última corrida global registrada de `apps/movil/test/goldens` dio **113 aprobados y 155 diferencias (268 celdas)**; no se inspeccionaron individualmente todas las diferencias ni se atribuyen exclusivamente a roles de color. Ver [revalidación global H10](./h10-app-goldens-flutter-3.47.5-2026-10-13.md).

No se publicó nada. El estado de `PLAN.md` continúa `A MEDIAS` para H8.S4.M3.

## Continuación de la auditoría — 2026-10-13

Se encontró otro uso no semántico: los iconos `+` y `−` de `TarjetaDeSolicitud` usaban respectivamente `okTexto` y `errTexto` para expresar polaridad de datos. Ahora ambos usan `text2` en claro y oscuro. Se añadió una regresión widget para proteger esa distinción.

Verificación actual, Flutter 3.47.5/Windows, sin actualizar referencias:

- `flutter test --no-pub --concurrency=1 --reporter compact test/widget/roles_de_color_no_estado_test.dart`: **6/6 PASS**; incluye signos de polaridad, marca y datos no-estado en ambos temas.
- `dart analyze --fatal-infos lib/organismos/tarjeta_de_solicitud.dart test/widget/roles_de_color_no_estado_test.dart`: **No issues found**.
- `flutter test --no-pub --concurrency=1 --reporter compact test/goldens/catalogo_golden_test.dart --plain-name "Organismos"`: **2/2 PASS** sin update.
- Suite completa del paquete `packages/diseno_flutter`, `flutter test --no-pub --concurrency=1 --timeout 60s --reporter compact`: **126/126 PASS**.
- `node --test pruebas/roles-contraste.test.mjs pruebas/texto-de-ejemplo-contraste.test.mjs` en `packages/tokens`: **3/3 PASS**.

El resultado global del plan permanece **182/213 (85,4 %)**. No se cambia el estado: aunque el hallazgo de `TarjetaDeSolicitud` está corregido, esta evidencia todavía no contiene una tabla completa de rol, contraste y estado para cada superficie/estado del alcance de H8.S4.M3.

## Cierre de H8.S4.M3 — 2026-10-13

Esta sección supersede los estados parciales anteriores de este archivo. Se completó el cotejo cromático de las **20 superficies disponibles** en ambos temas, usando la matriz visual color-focalizada de 40 capturas y las revalidaciones dirigidas de los estados modificados. La unidad conceptual «grupos/detalle» no tiene pantalla ni ruta implementada y queda fuera del inventario ejecutable; no se inventó una pantalla para cubrirla.

| Superficies revisadas | Rol de acción/acento actual | Verde/rojo conservado |
|---|---|---|
| Billetera, perfil, ingreso, subida, candado, cámara, MFA y verificación | Acciones principales/enlaces en naranja; acciones secundarias neutrales. Baja de cuenta ya no se pinta de rojo. | Errores solo en mensajes/estados de error; selección/foco/progreso según estado. Tipo de movimiento y polaridad de solicitud son neutrales. |
| Tour, bienvenida, apertura/portada y arte de pautas | Acento naranja para acción y énfasis; arte e identidad decorativa neutrales. | Verde/rojo solo en estados representados (p. ej., comprobación correcta/incorrecta). |
| Alta: cuenta, correo, documento, datos, actividad y contrato | CTA/enlaces en naranja; campos y acciones secundarias neutrales. | Verde solo en foco, selección y progreso; rojo solo en error. |
| Grupos/mi estado, ayuda y prueba de vida | Acción principal naranja; filtros y controles neutrales salvo la selección explícita. | Verde solo para selección, progreso o estado; rojo solo para error. |

Los pares cromáticos se verifican con los roles semánticos usados por estas superficies —texto normal/estado ≥4,5:1 y controles ≥3:1— en claro y oscuro. La suite de contraste se ejecutó de nuevo y dio 3/3; a11y de catálogo y los tests de roles comprueban el mapeo de componentes. Las capturas color-focalizadas H8 oscuras fueron revisadas tras los cambios, sus baselines aprobadas pasan sin update (16/16); los goldens de billetera afectados pasan por categoría y la regresión legacy de saldo pasa 2/2. La suite completa de diseño y el gate móvil también están verdes.

Ejecuciones más recientes:

- `flutter test --no-pub --concurrency=1 --reporter compact test/widget/roles_de_color_no_estado_test.dart`: **6/6 PASS**.
- `flutter test --no-pub --concurrency=1 --reporter compact test/goldens/catalogo_golden_test.dart --plain-name "Organismos"`: **2/2 PASS**, sin update.
- `flutter test --no-pub --concurrency=1 --timeout 60s --reporter compact` (`packages/diseno_flutter`): **126/126 PASS**.
- `node --test pruebas/roles-contraste.test.mjs pruebas/texto-de-ejemplo-contraste.test.mjs`: **3/3 PASS**.
- `flutter test --no-pub --concurrency=1 --reporter compact test/widget/roles_color_acento_test.dart test/goldens/pantalla_de_saldo_golden_test.dart` (`apps/movil`): **8/8 PASS**.
- `python ..\..\scripts\verificar_frontend.py movil`: **TODO OK (11/11)**.

El barrido global de goldens móviles sigue registrando **113 aprobados y 155 diferencias/errores visibles de 268**; estas quedan en H10.S1.M6 y no se atribuyen todas a color. La corrección de roles y contraste requerida por H8.S4.M3 está verificada en sus superficies incluidas, con sus capturas focalizadas. Estado propuesto/corroborado en `PLAN.md`: **HECHO**.
