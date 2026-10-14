# H10.S1.M6 — barrido global móvil, revalidación 2026-10-13

Comando ejecutado desde `apps/movil` en Windows con Flutter 3.47.5:

```text
flutter test --no-pub --concurrency=1 --timeout 60s --reporter=compact test/goldens
```

## Resultado vigente — 2026-10-13

El último barrido completo dio **262 aprobados / 268; 6 fallidos**, exit 1. Se ejecutó en Windows con Flutter 3.47.5, serialmente, sin `--update-goldens`. Las cuatro suites dirigidas de alta responsive, estrés de texto, billetera/perfil y transiciones pasan **11/11** sin update después de revisar y regenerar sus referencias afectadas.

Los únicos seis fallos actuales son las capturas de **billetera cargando** (claro/oscuro × 360/412/600 dp). El diff aislado inspeccionado está en el icono QR de cabecera; no hay diferencia visible en el esqueleto o el layout. No actualicé estas referencias: el job oficial `app-goldens` de [ci.yml](../../../../.github/workflows/ci.yml) compara en `macos-latest` con Flutter 3.44.8, mientras esta corrida usa Windows/Flutter 3.47.5. Aceptar aquí esos píxeles produciría baselines no canónicas. Próximo paso para cerrar M6: ejecutar las seis celdas y la suite completa en el runner macOS/Flutter 3.44.8; mantener `A MEDIAS` hasta ese gate.

Cambios intencionales inspeccionados y revalidados: filas vacías de cotejo pasan a “Pendiente de completar” (H8 oscuro); CTA/enlaces y acciones de billetera usan los roles naranja/neutro (incluida perfil a 200 %); movimientos conservan iconografía neutral; el ingreso mantiene el texto “Mínimo 8 caracteres”; y las capturas de transición muestran el CTA naranja. Se corrigió además el test de ingreso que buscaba solo “Mínimo 8” y los tests extremos que exigían localizar secciones de una lista perezosa antes de desplazarla.

Referencias regeneradas en esta revalidación focalizada (Windows/Flutter 3.47.5; inspeccionadas por las diferencias descritas, no aprobadas como baselines canónicas):

| Grupo | Archivos exactos | Diferencia intencional revisada |
|---|---|---|
| Alta H8, paso 4 oscuro | `h8-alta-oscuro-paso-4-abajo-360-130-scroll-restablecido.png`; `h8-alta-oscuro-paso-4-fecha-responsive-360-130.png`; `h8-alta-oscuro-paso-4-final-responsive-360-130.png` | Tres filas vacías ahora expresan “Pendiente de completar”; fecha, botón, textos y scroll siguen completos. |
| Estrés H10, texto 200 % | `h10-texto-200/ingreso-360-200.png`; `h10-texto-200/ingreso-360-200-desplazado.png`; `h10-texto-200/alta-360-200.png`; `h10-texto-200/alta-fecha-360-200.png`; `h10-texto-200/alta-360-200-desplazado.png`; `h10-texto-200/billetera-360-200.png`; `h10-texto-200/billetera-360-200-desplazado.png` | El enlace de ingreso y el icono de fecha usan acento naranja; billetera prioriza Aportar y apila las tres acciones sin truncar etiquetas. |
| Ruptura H8, texto 200 % | `h8-break-ui/alta-datos-60-200.png`; `h8-break-ui/billetera-saldo-alto-vacios-200.png`; `h8-break-ui/billetera-saldo-cero-200.png`; `h8-break-ui/billetera-40-movimientos-200.png`; `h8-break-ui/billetera-40-movimientos-200-oscuro.png`; `h8-break-ui/perfil-email-150-200.png` | Acentos/acciones reflejan roles naranja/neutro; filas de movimiento neutrales; perfil y textos extremos conservan legibilidad y scroll. |
| Transición portada → ingreso | `apps/movil/test/goldens/imagenes/transicion_ingreso_aparece.png`; `apps/movil/test/goldens/imagenes/transicion_ingreso_asienta.png`; `apps/movil/test/goldens/imagenes/transicion_ingreso_final.png` | El CTA “Crear mi cuenta” pasa de verde decorativo a naranja semántico; no cambia la secuencia del fundido. |

Todas las suites que escriben estas referencias pasaron después **sin `--update-goldens`**. El job macOS/Flutter 3.44.8 debe comprobarlas antes de considerarlas aprobadas para integración.

H10.S1.M6 continúa `A MEDIAS`; el porcentaje oficial del plan sigue **183/213 (85,9 %)**. No se publicaron cambios.

## Historial de la revalidación previa (193/268; superada)

Inspeccioné los pares `masterImage`/`testImage` actuales para `h1-candado-oscuro-360`, `h10-ayuda-inicio-oscuro-412-130`, `h10-perfil-inicial-oscuro-360-130` y `h10-subida-error-red-claro-360-130`.

- En las referencias anteriores, controles activos y acciones de navegación aparecen verdes/mint; la acción «Dar de baja tu cuenta» aparece roja.
- En las capturas actuales, las acciones interactivas usan los roles naranja/neutro y la baja de cuenta usa acento naranja; el criterio vigente de H8.S4.M3 reserva rojo para estados, no para esa acción. El contenido, la geometría y el contraste siguen legibles.
- Estas diferencias coinciden con la actualización semántica de color de H8.S4.M3; son candidatas a regeneración tras revisar cada celda, no regresiones por sí mismas. Un caso de candado llegó a 94,67 % de píxeles distintos porque cambian marca/acción secundaria, no porque se haya recortado la pantalla.
- Hay además diferencias pequeñas (por ejemplo 0,09 % en `alta-datos-60-200`) y tres fallos de transición; aún no se atribuyen a color ni a rasterizador. No se deben regenerar sin inspección específica.

La corrección reciente de `FilaDeCotejo` evita mostrar éxito al comparar dos valores vacíos. Sus pruebas de widget pasan 6/6 en claro/oscuro; la suite visual `h10_alta_datos_visual_test.dart` pasó 6/6 con actualización aprobada y 6/6 en repetición sin update. La suite de goldens de `packages/diseno_flutter` pasa 18/18 en Flutter 3.47.5 Windows. El último barrido global previo registró 132 fallos, antes de la actualización focalizada de Candado; el total global vigente queda desconocido hasta repetirlo.

Antes de repetir el barrido, inspeccioné y actualicé 57 referencias semánticas: Candado 6/6, Ayuda 12/12, Perfil 7/7, Subida 12/12 y Verificación 20 celdas (suite 24/24 al incluir cuatro referencias H8 reutilizadas). Cada suite volvió a pasar sin update. El nuevo resultado global queda arriba: 193/268 PASS, 75 FAIL. Los artefactos pendientes se agrupan en: H10 grupos/mi estado (20), H10 ingreso (16), H10 billetera cargando (6), H10 alta/documento (6), H10 prueba de vida (5), H1 candado (6), escaneo denegado (6), alta responsive H8 (3), extremos de alta/billetera/perfil (4) y transiciones (3). Estas 75 imágenes siguen pendientes de inspección/clasificación; no se regeneraron en bloque.

**Estado:** H10.S1.M6 sigue `A MEDIAS`. Próximo trabajo: inspeccionar los artefactos por familia, regenerar solo referencias con diferencia intencional probada, corregir regresiones reales y repetir el barrido completo. La validación del runner canónico Linux sigue pendiente por la incompatibilidad documentada del entorno Windows/rasterizador.
