# Lectura visual H10.S3.M2 — veinte superficies

Estado: HECHO para las 228 celdas existentes de veinte superficies. Entorno local Windows, Flutter test con fixtures sintéticos, texto 130 %. La unidad grupos/detalle no tiene ruta/contrato local ni captura y queda fuera de estas 228 celdas; la cobertura de la matriz corresponde a H10.S3.M1.

Tour: 24 celdas (21 PNG nuevos + 3 referencias H8 oscuras reutilizadas); Bienvenida: 6 PNG H10 nuevos; Verificación: 24 celdas (20 PNG H10 nuevos + 4 referencias H8 oscuras reutilizadas).

Abrí e inspeccioné cada celda: grupos / mi estado (20 nuevas + 4 reutilizadas), portada (4 nuevas + 2 reutilizadas), billetera (24 nuevas), ingreso (16 nuevas + 2 reutilizadas), MFA (16 nuevas + 2 reutilizadas), Tour (21 nuevas + 3 reutilizadas), Bienvenida (6 nuevas), Verificación (20 nuevas + 4 reutilizadas), Subida (11 nuevas + 1 reutilizada), Perfil (6), Candado (6), Ayuda (12), Cámara (6), Prueba de vida (5 nuevas + 1 reutilizada), Alta/cuenta (5 nuevas + 1 reutilizada), Alta/actividad (5 nuevas + 1 reutilizada), Alta/contrato (5 nuevas + 1 reutilizada), Alta/correo (5 nuevas + 1 reutilizada), Alta/datos (5 nuevas + 1 reutilizada) y Alta/documento (6 nuevas), **228 en total**. Cada fila deja una observación por imagen; la evidencia reproducible y los comandos están en [INDICE.md](./INDICE.md).

## Revalidación posterior de roles de color — 2026-10-13

La inspección original de 228 capturas precede algunos cambios semánticos de color. Como revisión adicional del diff global se reabrieron y revisaron las 6 capturas actuales de Candado, las 12 de Ayuda, las 7 de Perfil (incluido estado presionado), las 12 de Subida y las 20 imágenes nuevas de Verificación (cuatro estados, cinco celdas por estado). En Candado, marca, CTA y acción secundaria mantienen jerarquía; en Ayuda los CTA reflejan roles de acción y el filtro seleccionado sigue distinguible; en Perfil Cerrar sesión queda neutro y la baja de cuenta sigue el criterio naranja, no rojo; en Subida, error conserva rojo y acciones pasan a naranja; en Verificación, acciones pasan a naranja y los estados/textos siguen distinguibles. No hay clipping ni pérdida de jerarquía; se actualizaron esas 57 referencias tras inspección y las suites pasan sin update: 6/6, 12/12, 7/7, 12/12 y 24/24 (Verificación incluye cuatro referencias H8 reutilizadas). El barrido global posterior aún está pendiente.

| Captura | Resultado visual |
|---|---|
| `h10-grupos-mi-estado-principal-deuda-claro-360-130.png` | OK: deuda, monto y dos acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-principal-al-dia-claro-360-130.png` | OK: estado al día, monto y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-cargando-claro-360-130.png` | OK: skeleton y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-error-parcial-claro-360-130.png` | OK: mensaje, retry y restricción parcial legibles; sin recorte. |
| `../h8-grupos-mi-estado-deuda-oscuro-360-130-inicio.png` | OK según inspección H8: deuda y acciones visibles; sin recorte. |
| `../h8-grupos-mi-estado-al-dia-oscuro-360-130.png` | OK según inspección H8: estado al día y acciones visibles; sin recorte. |
| `../h8-grupos-mi-estado-cargando-oscuro-360-130.png` | OK según inspección H8: skeleton y acciones visibles; sin recorte. |
| `../h8-grupos-mi-estado-error-oscuro-360-130.png` | OK según inspección H8: error, retry y acciones visibles; sin recorte. |
| `h10-grupos-mi-estado-principal-deuda-claro-412-130.png` | OK: deuda, monto y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-principal-al-dia-claro-412-130.png` | OK: estado al día, monto y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-cargando-claro-412-130.png` | OK: skeleton y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-error-parcial-claro-412-130.png` | OK: mensaje, retry y restricción parcial legibles; sin recorte. |
| `h10-grupos-mi-estado-principal-deuda-oscuro-412-130.png` | OK: deuda, monto y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-principal-al-dia-oscuro-412-130.png` | OK: estado al día, monto y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-cargando-oscuro-412-130.png` | OK: skeleton y acciones legibles; sin recorte. |
| `h10-grupos-mi-estado-error-parcial-oscuro-412-130.png` | OK: mensaje, retry y restricción parcial legibles; sin recorte. |
| `h10-grupos-mi-estado-principal-deuda-claro-600-130.png` | OK: deuda y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-principal-al-dia-claro-600-130.png` | OK: estado al día y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-cargando-claro-600-130.png` | OK: skeleton y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-error-parcial-claro-600-130.png` | OK: mensaje, retry y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-principal-deuda-oscuro-600-130.png` | OK: deuda y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-principal-al-dia-oscuro-600-130.png` | OK: estado al día y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-cargando-oscuro-600-130.png` | OK: skeleton y acciones completas; botones usan todo el ancho disponible. |
| `h10-grupos-mi-estado-error-parcial-oscuro-600-130.png` | OK: mensaje, retry y acciones completas; botones usan todo el ancho disponible. |

| `../h8-portada-claro-360-130.png` | OK según inspección H8: marca, título y acciones visibles. |
| `../h8-portada-oscuro-360-130.png` | OK según inspección H8: marca, título y acciones visibles. |
| `h10-portada-principal-claro-412-130.png` | OK: título, marca, garantías y acciones completas. |
| `h10-portada-principal-oscuro-412-130.png` | OK: título, marca, garantías y acciones completas. |
| `h10-portada-principal-claro-600-130.png` | OK: título y acciones completos; garantías envuelven 2+1. |
| `h10-portada-principal-oscuro-600-130.png` | OK: título y acciones completos; garantías envuelven 2+1. |
| `h10-billetera-principal-claro-360-130.png` | OK: acciones completas apiladas; la lista sigue con scroll. |
| `h10-billetera-cargando-claro-360-130.png` | OK: cabecera y esqueletos completos. |
| `h10-billetera-vacio-claro-360-130.png` | OK: saldo cero, aviso vacío y custodia legibles. |
| `h10-billetera-error-claro-360-130.png` | OK: mensaje 503, referencia y reintento completos. |
| `h10-billetera-principal-oscuro-360-130.png` | OK: acciones completas apiladas; la lista sigue con scroll. |
| `h10-billetera-cargando-oscuro-360-130.png` | OK: cabecera y esqueletos completos. |
| `h10-billetera-vacio-oscuro-360-130.png` | OK: saldo cero, aviso vacío y custodia legibles. |
| `h10-billetera-error-oscuro-360-130.png` | OK: mensaje 503, referencia y reintento completos. |
| `h10-billetera-principal-claro-412-130.png` | OK: acciones completas apiladas; la lista sigue con scroll. |
| `h10-billetera-cargando-claro-412-130.png` | OK: cabecera y esqueletos completos. |
| `h10-billetera-vacio-claro-412-130.png` | OK: saldo cero, aviso vacío y custodia legibles. |
| `h10-billetera-error-claro-412-130.png` | OK: mensaje 503, referencia y reintento completos. |
| `h10-billetera-principal-oscuro-412-130.png` | OK: acciones completas apiladas; la lista sigue con scroll. |
| `h10-billetera-cargando-oscuro-412-130.png` | OK: cabecera y esqueletos completos. |
| `h10-billetera-vacio-oscuro-412-130.png` | OK: saldo cero, aviso vacío y custodia legibles. |
| `h10-billetera-error-oscuro-412-130.png` | OK: mensaje 503, referencia y reintento completos. |
| `h10-billetera-principal-claro-600-130.png` | OK: acciones en fila; movimientos completos y legibles. |
| `h10-billetera-cargando-claro-600-130.png` | OK: cabecera y esqueletos completos. |
| `h10-billetera-vacio-claro-600-130.png` | OK: saldo cero, aviso vacío y custodia legibles. |
| `h10-billetera-error-claro-600-130.png` | OK: mensaje 503, referencia y reintento completos. |
| `h10-billetera-principal-oscuro-600-130.png` | OK: acciones en fila; movimientos completos y legibles. |
| `h10-billetera-cargando-oscuro-600-130.png` | OK: cabecera y esqueletos completos. |
| `h10-billetera-vacio-oscuro-600-130.png` | OK: saldo cero, aviso vacío y custodia legibles. |
| `h10-billetera-error-oscuro-600-130.png` | OK: mensaje 503, referencia y reintento completos. |

| `h10-ingreso-principal-claro-360-130.png` | OK: campos y pie completos; botón de ingreso correctamente deshabilitado sin credenciales. |
| `h10-ingreso-cargando-claro-360-130.png` | OK: spinner y rótulo legibles; salida secundaria visible. |
| `h10-ingreso-error-claro-360-130.png` | OK: rechazo y referencia completos; ambos campos y salida al alta visibles. |
| `../h8-ingreso-oscuro-360-130.png` | OK: formulario y pie legibles; reutilizada desde H8 con mismo viewport/escala. |
| `h10-ingreso-cargando-oscuro-360-130.png` | OK: spinner y rótulo legibles; contraste claro sobre fondo oscuro. |
| `../h8-ingreso-error-oscuro-360-130.png` | OK: mensaje/referencia y salida visibles; reutilizada desde H8 con mismo viewport/escala. |
| `h10-ingreso-principal-claro-412-130.png` | OK: campos, custodia y pie completos; enlace de alta en la misma fila. |
| `h10-ingreso-cargando-claro-412-130.png` | OK: spinner y rótulo visibles; pie completo. |
| `h10-ingreso-error-claro-412-130.png` | OK: error, referencia, campos y pie legibles. |
| `h10-ingreso-principal-oscuro-412-130.png` | OK: contraste de campos y acciones; formulario y pie completos. |
| `h10-ingreso-cargando-oscuro-412-130.png` | OK: spinner y rótulo visibles; pie legible. |
| `h10-ingreso-error-oscuro-412-130.png` | OK: error, campos, foco y pie visibles con contraste. |
| `h10-ingreso-principal-claro-600-130.png` | OK: ancho completo, contenido alineado, pie completo. |
| `h10-ingreso-cargando-claro-600-130.png` | OK: spinner/rótulo y salida al alta visibles. |
| `h10-ingreso-error-claro-600-130.png` | OK: mensaje en una línea, campos y pie completos. |
| `h10-ingreso-principal-oscuro-600-130.png` | OK: ancho completo, contraste y alineación legibles. |
| `h10-ingreso-cargando-oscuro-600-130.png` | OK: spinner/rótulo y salida al alta visibles. |
| `h10-ingreso-error-oscuro-600-130.png` | OK: mensaje en una línea, campos y pie completos. |

### MFA — lectura de las 18 celdas

| Celda | Veredicto visual |
|---|---|
| 360 claro principal | OK: título, seis casillas y ayuda completas. |
| 360 claro cargando | OK: botón/rótulo visibles; casillas deshabilitadas. |
| 360 claro error | OK: seis dígitos; mensaje se envuelve en dos líneas sin corte. |
| 360 oscuro principal | Reutilización exacta de captura H8 ya revisada. |
| 360 oscuro cargando | OK: botón/rótulo visibles; casillas deshabilitadas. |
| 360 oscuro error | Reutilización exacta de captura H8 ya revisada. |
| 412 claro principal | OK: título, casillas y ayuda legibles. |
| 412 claro cargando | OK: botón/rótulo completos; no se superponen. |
| 412 claro error | OK: mensaje en una línea; casillas completas. |
| 412 oscuro principal | OK: contraste de título, casillas y ayuda. |
| 412 oscuro cargando | OK: botón/rótulo completos; contraste suficiente. |
| 412 oscuro error | OK: mensaje y casillas completos con contraste. |
| 600 claro principal | OK: casillas distribuidas en el ancho; no hay clipping. |
| 600 claro cargando | OK: botón ancho completo y rótulo legible. |
| 600 claro error | OK: dígitos y mensaje completos; sin overflow. |
| 600 oscuro principal | OK: casillas distribuidas; contraste legible. |
| 600 oscuro cargando | OK: botón ancho completo y rótulo legible. |
| 600 oscuro error | OK: dígitos y mensaje completos; sin overflow. |

No observé clipping, solapamiento ni texto ilegible en las 16 imágenes MFA nuevas; las dos oscuras 360 dp son capturas H8 exactas ya revisadas. El estado de carga evita edición repetida del código.

### Tour — lectura de las 24 celdas

| Lámina | Tema | 360 dp | 412 dp | 600 dp |
|---:|---|---|---|---|
| 1 | claro | OK: ilustración, título, texto, indicador y «Siguiente» completos. | OK: composición completa; controles visibles. | OK: composición completa; controles visibles. |
| 1 | oscuro | Reutilizada H8: contraste y «Volver», «Saltar», «Siguiente» visibles. | OK: composición y controles completos. | OK: composición y controles completos. |
| 2 | claro | OK: contenido, indicador y controles completos. | OK: composición completa; controles visibles. | OK: composición completa; controles visibles. |
| 2 | oscuro | Reutilizada H8: texto, indicador y controles visibles. | OK: composición y controles completos. | OK: composición y controles completos. |
| 3 | claro | OK: contenido, indicador y controles completos. | OK: composición completa; controles visibles. | OK: composición completa; controles visibles. |
| 3 | oscuro | Reutilizada H8: texto, indicador y controles visibles. | OK: composición y controles completos. | OK: composición y controles completos. |
| 4 | claro | OK: «Empezar» visible; «Saltar» desvanecido; composición completa. | OK: «Empezar» visible; sin clipping. | OK: «Empezar» visible; sin clipping. |
| 4 | oscuro | OK: baseline H10 propia; «Empezar» visible; sin clipping. | OK: «Empezar» visible; sin clipping. | OK: «Empezar» visible; sin clipping. |

Las 21 capturas H10 fueron inspeccionadas visualmente, más las tres referencias H8 reutilizadas. En esta revisión los verdes decorativos de las ilustraciones y del aro del pedestal se neutralizaron; se conserva el tilde verde como estado verificado y el naranja como acento. No se observó clipping, solapamiento ni texto ilegible; los controles superiores e inferiores están visibles en las tres H8 inspeccionadas. Carga, vacío y error no aplican al tour estático. Las cuatro capturas H8 oscuras se actualizaron y revisaron para reflejar el cambio de roles; H8 y H10 pasan sin update.

### Bienvenida — lectura de las 6 celdas

| Celda | Veredicto visual |
|---|---|
| 360 claro principal | OK: encabezado, tarjeta y ambas acciones completos; sin recorte. |
| 360 oscuro principal | OK: título y ambas acciones legibles; sin copy de bono no confirmado. |
| 412 claro principal | OK: tarjeta y acciones completas; espaciado uniforme. |
| 412 oscuro principal | OK: contraste del título y acciones; sin clipping. |
| 600 claro principal | OK: texto y acciones completos; tarjeta alineada al ancho. |
| 600 oscuro principal | OK: contenido íntegro y contraste legible. |

Las seis capturas nuevas se inspeccionaron; no observé texto ilegible, solapamiento ni recorte. El test comprueba que la cuenta se rotula «nueva cuenta» y que aparecen «Ir a tu billetera» y «Completar tu perfil» sin afirmar un monto de bono. La H8 oscura 360 no se reutiliza: el test da 190 px distintos (0,07 %), visibles en el contorno del panel; no se estableció la causa y se conserva la referencia H8 intacta.

### Estado de verificación — lectura de las 24 celdas

| Estado | Tema | 360 dp | 412 dp | 600 dp |
|---|---|---|---|---|
| pendiente | claro | OK: título, mensaje, caso y acciones completos. | OK: título, caso y botones completos. | OK: texto y acciones íntegros. |
| pendiente | oscuro | Reutilizada H8 exacta: contenido y acciones visibles. | OK: contraste y acciones completos. | OK: contraste y acciones íntegros. |
| en revisión | claro | OK: texto y acción completos. | OK: encabezado y controles visibles. | OK: contenido y controles completos. |
| en revisión | oscuro | Reutilizada H8 exacta: texto y acciones visibles. | OK: texto legible y controles completos. | OK: contenido y controles íntegros. |
| aprobada | claro | OK: estado aprobado y acciones completos. | OK: estado y acción completos. | OK: contenido íntegro. |
| aprobada | oscuro | Reutilizada H8 exacta: estado y acción visibles. | OK: contraste suficiente y acción visible. | OK: contenido íntegro. |
| rechazada | claro | OK: motivo envuelto en dos líneas, sin corte. | OK: motivo completo en una línea. | OK: motivo completo; acción visible. |
| rechazada | oscuro | Reutilizada H8 exacta: motivo y acción visibles. | OK: motivo completo y contraste legible. | OK: motivo y acciones completos. |

Se inspeccionaron las 20 imágenes H10 nuevas una a una y las cuatro referencias H8 ya verificadas; no observé clipping, solapamiento ni texto ilegible. Los cuatro estados usan respuestas sintéticas; el motivo de rechazo se localiza como «La cara no coincide con el documento». La suite H10 pasó 24/24 en repetición sin actualizar y H8 1/1 testWidgets cubre las referencias reutilizadas.

No observé errores de layout no intencionales en las **228 celdas de veinte superficies** revisadas. En Alta/documento se inspeccionaron seis capturas nuevas: el anverso y las acciones visibles caben; CI/fecha siguen bajo el primer viewport en 360/412 dp y se alcanzan por scroll; no se abre cámara ni permiso. El carrusel revela parcialmente el título de la tarjeta siguiente como affordance, sin recortar la tarjeta activa. Su H8 oscuro/360 difiere en 1.056 píxeles (0,40 %) con Flutter 3.47.5 y no se reutiliza ni modifica. En Ayuda, las seis capturas iniciales muestran la recomendación y en las otras seis la tarjeta y acción del último tutorial quedan en viewport; no se ejecutan acciones. H8 oscuro/360 difiere en la inicial (1.286 píxeles, 0,48 %) y sus PNG quedan intactos. En Candado, las seis capturas muestran la capa de bloqueo con marca, título, explicación y acciones completas; no se filtra contenido subyacente. Perfil muestra seis capturas con el campo vacío y cinco acciones completas, sin exponer correo ni activar ninguna acción; H8 oscuro/360 difiere en 444 píxeles (0,17 %) y permanece intacto. En subida, el error de red y sus acciones se leen en los tres anchos/temas; las cuatro fotos pendientes y «Continuar» caben. Cámara muestra cabecera/pista, consejos plegados, marco ID-1 y acciones sin solapamiento; el test usa un fake inerte y no permite tomar una foto. Prueba de vida muestra pose frontal, instrucciones, marco, miniaturas y acciones completas; el tema oscuro se aplica internamente para conservar contraste sobre el visor. Su H8 oscuro/360 pasa igualdad exacta y permanece intacto. Alta/cuenta y Alta/actividad se muestran inicialmente vacías, sin foco ni teclado; los valores grises de cuenta son placeholders y no datos precargados. La referencia H8 oscura/360 coincide exactamente en ambas. En actividad, los selectores se mantienen cerrados; los formularios completos caben a 412 y 600 dp, y en 360 dp la tarjeta de confianza queda bajo el primer viewport desplazable sin recortar la acción. Alta/contrato previo conserva el aviso y la acción completos en los tres anchos y temas; no se abre ni acepta el contrato. Alta/correo muestra seis OTP vacíos, sin teclado ni llamada de red; al faltar correo, la dirección se representa con puntos suspensivos como en H8, cuya referencia oscuro/360 coincide exactamente. Alta/datos no contiene nombres/fecha, teclado o campo enfocado; a 360/412 dp lo que queda bajo el viewport es accesible por scroll y a 600 dp se lee el resumen con «Continuar». La referencia H8 oscuro/360 coincide exactamente. La salida al alta del ingreso queda dentro del viewport incluso cuando el error expande el formulario a 360 dp. Las acciones de billetera se apilan bajo 420 dp para evitar truncamiento; 600 dp conserva la fila. En vistas compactas, los movimientos inferiores siguen fuera del primer viewport dentro de la lista desplazable. “Al día” sigue siendo una respuesta válida, no un `EstadoVacio`; portada, tour y bienvenida son estáticos y no aplican carga/vacío/error. La lectura individual de las 228 celdas queda registrada arriba y en las tablas por superficie: 156 filas de capturas identificadas por archivo y 72 observaciones en matrices con eje estado/tema/ancho. No hay captura física ni lectura del rasterizador de CI; esos límites pertenecen a H10.S3.M1 y a las tareas de dispositivo, no dejan lecturas pendientes en M2. El párrafo “Alcance de pantallas” de H8 en `PLAN.md` enumera 21 unidades visuales conceptuales, contando seis pasos de alta por separado. La cifra no es un total de rutas: el alta es un wizard con subestados, candado es una capa y detalle de grupo carece de ruta/contrato local según el inventario H8.

### Subida del expediente — 12 celdas

Fixture sintético de `PantallaDeSubidaDelExpediente`: anverso en curso con las otras cuatro caras pendientes, y fallo de red con reintento/repetición, a 360 × 740, 412 × 850 y 600 × 960 dp; ambos temas, texto 130 %, DPR 1. Solo se reutiliza la referencia H8 de subida en curso, oscuro/360, cuya prueba pasó exacta; el golden H8 de fallo de red difiere en 148 píxeles (0,06 %) con Flutter 3.44.8 y queda intacto. Los otros 11 PNG son baselines H10 propias.

| Estado | Tema | 360 dp | 412 dp | 600 dp |
|---|---|---|---|---|
| En curso | claro | OK: detalle, cancelar y cuatro pendientes completos | OK: contenido completo | OK: contenido completo |
| En curso | oscuro | Reutilizada H8 exacta | OK: contenido completo | OK: contenido completo |
| Error de red | claro | OK: mensaje envuelto y ambas acciones visibles | OK: mensaje y acciones en línea | OK: mensaje y acciones completos |
| Error de red | oscuro | OK: mensaje envuelto, contraste y acciones | OK: mensaje y acciones en línea | OK: mensaje y acciones completos |

Golden H10 con actualización y repetición sin actualización: 12/12 ambas. `dart analyze --fatal-infos` no reportó incidencias; tests widget + unidad de subida: 12/12; gate móvil: `TODO OK` (11/11). Las 11 capturas nuevas fueron inspeccionadas; sin recortes, solapamientos ni texto ilegible. No se usaron cámara, red, backend o datos reales.

### Perfil — 6 celdas

Perfil inicial sintético con correo vacío, 360 × 740, 412 × 850 y 600 × 960 dp, ambos temas, texto 130 %, DPR 1. Las seis capturas H10 propias se inspeccionaron: etiqueta/campo y las acciones Guardar, Cambiar contraseña, Ver la guía otra vez, Cerrar sesión y Dar de baja tu cuenta están completas; sin clipping ni solapamientos. No se escribe un correo ni se activa una acción. `h8_perfil_oscuro_test.dart` difiere en 444 píxeles (0,17 %) y su referencia queda intacta. Update y repetición H10: 6/6 cada una; test widget vecino del cierre: 1/1; análisis Dart limpio; gate móvil 11/11.

### Candado — 6 celdas

La capa de bloqueo al arranque se capturó con control de bloqueo y proveedor biométrico sintéticos, 360 × 740, 412 × 850 y 600 × 960 dp, ambos temas, texto 130 %, DPR 1. Las seis imágenes muestran la marca, «AportaYa está bloqueada», la explicación y las dos acciones dentro del viewport; no vi clipping ni solapamientos. El hijo privado está visualmente cubierto. No se simularon éxito biométrico, desbloqueo ni sesión vencida; las pruebas existentes de interacción cubren la absorción de toques y exclusión semántica del hijo. H1 no es referencia compatible (altos diferentes, sin escala fijada); no se reutilizó. H10 update/repetición 6/6; análisis sin incidencias; pruebas vecinas widget/unidad 20/20; gate 11/11. La primera ejecución transitoria del update falló porque el test dejó abierto un `SemanticsHandle`; se quitó ese manejo redundante y las dos matrices finales pasan.

### Centro de ayuda — 12 celdas

Tres tutoriales sintéticos con registro/progreso local. En el estado inicial, el contenido y la recomendación se muestran legibles; en la lista desplazada, la última tarjeta y su botón quedan dentro del viewport en 360/412/600, claro/oscuro. Se inspeccionaron las 12 capturas H10; sin texto ilegible, recortes ni solapamientos. No se lanzó ningún tutorial. No se reutilizaron las referencias H8: su golden inicial difiere en 1.286 píxeles (0,48 %); se conservan intactas. H10 update y repetición: 12/12 ambas; análisis limpio; widget/a11y/unidad 18/18; gate 11/11. Se capturaron solo estados existentes; carga/error remoto no aplican.

### Cámara de identidad — 6 celdas

`PantallaDeCamara` en permiso concedido, con cámara sintética inerte, sin tomar ni escribir fotos. Revisé las seis capturas a 360/412/600 dp, claro/oscuro, texto 130 % y DPR 1. Cabecera/pista, consejos plegados, marco ID-1 y controles aparecen completos; no hay solapamientos ni recortes. El hallazgo inicial de pie arriba y consejos superpuestos a 360 dp se corrigió poniendo el pie en `MainAxisSize.min` y fluyendo los consejos debajo de la cabecera. El test verifica geometría y falla si se solicita `tomarFoto`. No existe H8 compatible; no se modificó ninguna referencia. Update/repetición H10: 6/6 ambas; análisis limpio; prueba widget cámara de respaldo: 5/5; gate móvil: 11/11.

### Prueba de vida — 6 celdas

Estado inicial frontal sintético, con las tres miniaturas pendientes; cámara silenciosa sin cuadros faciales y reloj fijo para no avanzar la pose. En 360/412/600 dp, claro/oscuro, texto 130 %, el título, la instrucción, el óvalo, las miniaturas y el pie están dentro del viewport. El tema oscuro se fuerza dentro de la pantalla porque el fondo de visor es oscuro también cuando la app circundante usa tema claro; el ajuste conserva el contraste de las acciones. El test verifica que «Tomar ahora» esté habilitado, pero no lo acciona; `tomarFoto` falla si se llega a llamar. H8 oscuro/360 pasó comparación pixelaria exacta y queda reutilizado, intacto; las cinco capturas restantes son H10 nuevas. Update/repetición H10: 5/5; repetición H8: 1/1; widget/unidad de vida: 39/39; gate 11/11. Las seis imágenes fueron inspeccionadas sin texto ilegible, recorte ni solapamiento.

### Alta — cuenta inicial — 6 celdas

`PantallaDeRegistro` en `PasoAlta.cuenta`, sin datos ni campo enfocado, con teclado oculto. Las capturas de tres anchos × dos temas, texto 130 %, no tocan «Continuar»; Dio está simulado y se usa almacenamiento en memoria. Los textos `71234567`, `tu@correo.com` y similares son placeholders de ejemplo, no valores de controlador. El contenido que rebasa el primer viewport se desplaza verticalmente; no hay overflow. La referencia H8 oscura/360 pasó igualdad exacta y queda intacta; los otros cinco PNG H10 se revisaron sin clipping ni solapamientos. Update H10 5/5; repetición 6/6 incluida comparación H8; análisis limpio; gate 11/11.

### Alta — actividad/perfil transaccional — 6 celdas

`PantallaDeRegistro` en `PasoAlta.actividad`, con estado sintético inicial: origen, actividad y monto vacíos, sin selector abierto, foco o teclado; no se acciona «Continuar». Las cinco PNG H10 se inspeccionaron: campos, etiquetas, ayudas y acción legibles, sin clipping ni solapamiento; a 360 dp la tarjeta de confianza queda después del primer viewport desplazable y a 412/600 dp permanece completa. El candidato H8 oscuro/360 pasó comparación pixelaria exacta y quedó intacto. Fixture con Dio simulado y almacén en memoria; matriz H10 update y repetición verificada junto con los widgets del perfil (11/11); análisis y gate móvil limpios.

### Alta — paso previo al contrato — 6 celdas

`PasoAlta.contrato` muestra el mensaje de último paso y «Continuar», sin navegar a la ruta legal ni iniciar consentimientos o envío. En 360/412/600 dp y ambos temas, escala 130 %, texto/acción completos y sin overflow. La referencia H8 oscuro/360 pasó igualdad exacta, intacta; se inspeccionaron los cinco PNG H10 restantes. Test visual más widget existente: 8/8; análisis Dart limpio.

### Alta — correo inicial — 6 celdas

Se inspeccionaron las cinco capturas H10 nuevas: las seis celdas OTP y botones se leen completos en claro/oscuro a 360/412/600 dp. Ningún OTP recibe foco; teclado oculto, sin destino verificado, sin llamada de solicitud/verificación. La dirección no se especifica porque el fixture intencionalmente tiene el correo vacío; la UI muestra puntos suspensivos y coincide exactamente con H8 oscuro/360. No se utilizó información personal.

### Alta — datos personales iniciales — 6 celdas

Se inspeccionaron las cinco capturas H10: ningún campo recibe foco, y no se abre teclado ni fecha. Los placeholders son ejemplos estáticos, no datos introducidos. A 360/412 dp los campos inferiores y el CTA continúan por scroll; a 600 dp los campos, resumen y acción quedan completos. La referencia H8 oscuro/360 pasó comparación pixelaria exacta y permanece intacta.

### Alta — documento inicial — 6 celdas

No se ingresó número de CI, lugar ni fecha, ni se adjuntó captura. El botón de foto permanece sin activar; no se solicita permiso ni se instancia cámara durante la matriz. El anverso activo, carril y acciones son visibles en los tres anchos. CI/expedición/fecha empiezan debajo del primer viewport a 360/412 dp; el usuario puede desplazarse. Se inspeccionaron las seis imágenes; el título parcial de la siguiente tarjeta es un indicio de carrusel. El golden H8 oscuro/360 difiere 1.056 px (0,40 %) en el Flutter activo y queda intacto; se usan seis PNG H10 nuevos.

## Ampliación de lectura individual — celdas antes resumidas por grupo

Estas 84 celdas se abrieron individualmente para cumplir la regla de una observación por captura. En capturas de alta que terminan antes del final del formulario, indico si lo que sigue continúa por scroll; no lo tomo como clipping. Las referencias H8 reutilizadas se identifican con ruta `../`.

| Captura | Resultado observado |
|---|---|
| `h10-subida-en-curso-claro-360-130.png` | Anverso subiendo y cuatro pendientes; mensaje, cifrado y cancelar legibles; continuar completo y deshabilitado; sin solapamiento. |
| `h10-subida-en-curso-claro-412-130.png` | Mismo estado; cancelar y demás texto legibles; CTA deshabilitado completo; sin clipping. |
| `h10-subida-en-curso-claro-600-130.png` | Mismo estado; texto y CTA inferior de ancho completo visibles; sin solapamiento. |
| `../h8-subida-oscura-360-130.png` | Subida oscura; texto claro y cancelar legibles; CTA disabled de contraste intencionalmente bajo; sin clipping. |
| `h10-subida-en-curso-oscuro-412-130.png` | Texto claro y cancelar legibles; CTA disabled completo, de bajo contraste propio del estado; sin solapamiento. |
| `h10-subida-en-curso-oscuro-600-130.png` | Estado y CTA disabled completos; contraste textual suficiente a simple vista; sin clipping. |
| `h10-subida-error-red-claro-360-130.png` | Alerta rosa/roja con icono y texto completa; reintentar/repetir apilados; CTA naranja completo; sin solapamiento. |
| `h10-subida-error-red-claro-412-130.png` | Alerta y dos acciones en fila legibles; pendientes y CTA completos; sin clipping. |
| `h10-subida-error-red-claro-600-130.png` | Alerta en dos líneas, acciones horizontales y CTA completos; sin solapamiento. |
| `h10-subida-error-red-oscuro-360-130.png` | Alerta oscura con texto rosa/rojo legible; acciones apiladas y CTA completos; sin clipping. |
| `h10-subida-error-red-oscuro-412-130.png` | Alerta legible y acciones en fila; demás filas y CTA caben; sin solapamiento. |
| `h10-subida-error-red-oscuro-600-130.png` | Alerta, acciones y CTA completos; sin clipping ni contraste evidentemente insuficiente. |
| `h10-prueba-vida-inicial-claro-360-130.png` | Instrucción, marco, tres pendientes y acciones visibles; pose delineada en verde sobre visor oscuro; sin solapamiento. |
| `h10-prueba-vida-inicial-claro-412-130.png` | Mismo estado; instrucción, miniaturas y acciones caben; sin clipping. |
| `h10-prueba-vida-inicial-claro-600-130.png` | Mismo estado; paneles y botones completos a lo ancho; sin solapamiento. |
| `../h8-prueba-vida-oscura-frente-360-130.png` | Referencia H8 oscura; pose, pendientes y controles completos; texto/controles legibles; sin clipping. |
| `h10-prueba-vida-inicial-oscuro-412-130.png` | Estado inicial oscuro; silueta y guías contrastan con visor; tres pendientes y acciones completas. |
| `h10-prueba-vida-inicial-oscuro-600-130.png` | Estado inicial oscuro; instrucción, silueta y botones completos; sin solapamiento. |
| `h10-camara-concedida-claro-360-130.png` | Visor oscuro, consejos, marco y pie visibles; «Tomar foto» y «Cancelar» completos; sin recorte. |
| `h10-camara-concedida-claro-412-130.png` | Mismo estado; pie separado del marco y ambas acciones legibles; sin clipping. |
| `h10-camara-concedida-claro-600-130.png` | Marco y pie completos; texto de ayuda y acciones visibles; sin solapamiento. |
| `h10-camara-concedida-oscuro-360-130.png` | Visor oscuro con marco, CTA naranja y cancelar verde completos; título/ayuda legibles. |
| `h10-camara-concedida-oscuro-412-130.png` | Mismo estado; controles completos y con separación; sin clipping. |
| `h10-camara-concedida-oscuro-600-130.png` | Mismo estado; marco y dos acciones completos en el viewport; sin solapamiento. |
| `h10-alta-cuenta-vacia-claro-360-130.png` | Encabezado y tarjeta de contacto legibles; resto del formulario continúa por scroll; no hay clipping interno. |
| `h10-alta-cuenta-vacia-claro-412-130.png` | Contacto y comienzo de credenciales visibles; contenido restante desplazable; sin solapamiento. |
| `h10-alta-cuenta-vacia-claro-600-130.png` | Contacto, credenciales y aviso empiezan dentro del viewport; campos de ejemplo vacíos; sin overflow visible. |
| `../h8-alta-oscuro-paso-1-360-130.png` | Referencia H8 oscura; datos de ejemplo son placeholders; tarjeta/ayudas legibles; resto desplazable. |
| `h10-alta-cuenta-vacia-oscuro-412-130.png` | Etiquetas y campos vacíos visibles; contraste suficiente a simple vista; credenciales continúan por scroll. |
| `h10-alta-cuenta-vacia-oscuro-600-130.png` | Tarjetas y aviso completos en gran parte del viewport; campos no contienen datos; sin solapamiento. |
| `h10-alta-correo-inicial-claro-360-130.png` | OTP vacío; destino en elipsis envuelve a dos líneas; botones y tarjeta de custodia completos; sin clipping. |
| `h10-alta-correo-inicial-claro-412-130.png` | OTP vacío y acciones completas; texto de destino y custodia legibles; sin solapamiento. |
| `h10-alta-correo-inicial-claro-600-130.png` | Seis casillas, mensajes y acciones completos; amplio espacio bajo el contenido; sin recorte. |
| `../h8-alta-oscuro-paso-2-360-130.png` | Referencia H8 oscura; seis casillas vacías, elipsis y acciones legibles; sin clipping. |
| `h10-alta-correo-inicial-oscuro-412-130.png` | OTP y acciones completos; contraste textual legible; no hay teclado ni destino real. |
| `h10-alta-correo-inicial-oscuro-600-130.png` | Misma vista; OTP, custodia y botones completos; sin solapamiento. |
| `h10-alta-actividad-vacia-claro-360-130.png` | Selectores cerrados, ayudas y CTA visibles; la tarjeta de confianza sigue por scroll; sin clipping. |
| `../h8-alta-oscuro-paso-5-360-130-scroll-restablecido.png` | Referencia H8 oscura; selectores y ayuda legibles, CTA completo; confianza no visible en este viewport. |
| `h10-alta-actividad-vacia-claro-412-130.png` | Campos y CTA completos; aviso de cifrado en tarjeta separada abajo; sin solapamiento. |
| `h10-alta-actividad-vacia-oscuro-412-130.png` | Campos vacíos y CTA legibles; aviso visible debajo; sin clipping. |
| `h10-alta-actividad-vacia-claro-600-130.png` | Formulario, CTA y aviso de confianza caben; sin recorte ni solapamiento. |
| `h10-alta-actividad-vacia-oscuro-600-130.png` | Formulario, CTA y aviso completos; textos legibles; sin overflow visible. |
| `h10-alta-contrato-previo-claro-360-130.png` | Aviso de último paso y botón completos; gran parte inferior queda vacía, sin recortes. |
| `../h8-alta-oscuro-paso-6-360-130-scroll-restablecido.png` | Referencia H8 oscura; mensaje de contrato y CTA completos y legibles; sin clipping. |
| `h10-alta-contrato-previo-claro-412-130.png` | Mensaje y CTA completos; el texto envuelve en dos líneas; sin solapamiento. |
| `h10-alta-contrato-previo-oscuro-412-130.png` | Mensaje y CTA nítidos y completos en oscuro; sin clipping. |
| `h10-alta-contrato-previo-claro-600-130.png` | Mensaje en dos líneas y CTA de ancho completo; ambos dentro del viewport. |
| `h10-alta-contrato-previo-oscuro-600-130.png` | Mensaje y CTA completos; legibles contra superficie oscura; sin solapamiento. |
| `h10-alta-datos-inicial-claro-360-130.png` | Nombres y etiquetas legibles; fecha empieza al final del viewport y continúa por scroll; no hay overflow interno. |
| `../h8-alta-oscuro-paso-4-360-130-scroll-restablecido.png` | Referencia H8 oscura; campos de nombre completos y fecha inicia abajo; contenido desplazable. |
| `h10-alta-datos-inicial-claro-412-130.png` | Nombres y selectores Día/Mes visibles; año y CTA siguen por scroll; sin solapamiento. |
| `h10-alta-datos-inicial-oscuro-412-130.png` | Nombres y primeros selectores visibles; contenido restante desplazable; textos legibles. |
| `h10-alta-datos-inicial-claro-600-130.png` | Formulario de cuatro nombres, fecha, resumen y CTA completos; sin clipping. |
| `h10-alta-datos-inicial-oscuro-600-130.png` | Campos, resumen y CTA completos con contraste legible; sin solapamiento. |
| `h10-alta-documento-inicial-claro-360-130.png` | Anverso activo y acciones parcialmente en el borde inferior; tarjeta siguiente asoma como carrusel; CI sigue por scroll. |
| `h10-alta-documento-inicial-claro-412-130.png` | Anverso, pautas y «Tomar foto» completos; CI comienza debajo del primer viewport; carrusel indicado. |
| `h10-alta-documento-inicial-claro-600-130.png` | Carril, anverso y acciones completos; campos de CI/expedición comienzan al final; sin overflow interno. |
| `h10-alta-documento-inicial-oscuro-360-130.png` | Anverso y CTA continúan por scroll; asoma siguiente tarjeta como affordance; no se ve captura real. |
| `h10-alta-documento-inicial-oscuro-412-130.png` | Anverso y CTA completos; tarjeta siguiente asoma intencionalmente; CI continúa debajo. |
| `h10-alta-documento-inicial-oscuro-600-130.png` | Anverso, controles y primeros campos completos/visibles; carrusel parcial intencional. |
| `h10-ayuda-inicio-claro-360-130.png` | Encabezado, avance, buscador/filtros y comienzo de recomendación legibles; lista continúa por scroll. |
| `h10-ayuda-inicio-claro-412-130.png` | Recomendación, estado y CTA completos; tarjeta siguiente empieza bajo el viewport; sin clipping. |
| `h10-ayuda-inicio-claro-600-130.png` | Tarjetas y acciones empiezan dentro del viewport; lista continúa por scroll; textos legibles. |
| `h10-ayuda-inicio-oscuro-360-130.png` | Filtros envuelven a varias líneas; recomendación empieza debajo; títulos/etiquetas legibles. |
| `h10-ayuda-inicio-oscuro-412-130.png` | Recomendación completa con CTA y siguiente tarjeta parcial; sin solapamiento. |
| `h10-ayuda-inicio-oscuro-600-130.png` | Varias tarjetas/acciones visibles; la lista sigue por scroll; no se observa clipping. |
| `h10-ayuda-lista-claro-360-130.png` | Captura desplazada: tarjetas y CTAs completos; no se ve el encabezado, esperado por scroll. |
| `h10-ayuda-lista-claro-412-130.png` | Tres tarjetas con botones completos y etiquetas legibles; sin clipping/solapamiento. |
| `h10-ayuda-lista-claro-600-130.png` | Filtros y tres tarjetas completos; botones y textos caben en ancho amplio. |
| `h10-ayuda-lista-oscuro-360-130.png` | Captura desplazada; tarjetas inferiores y CTA completos; ausencia de encabezado se explica por scroll. |
| `h10-ayuda-lista-oscuro-412-130.png` | Tres tarjetas/acciones visibles completas; títulos envuelven sin recorte. |
| `h10-ayuda-lista-oscuro-600-130.png` | Filtros y tres tarjetas completas; contraste legible; sin solapamiento. |
| `h10-candado-inicial-claro-360-130.png` | Marca, explicación y dos acciones completas; todo centrado y sin contenido subyacente. |
| `h10-candado-inicial-claro-412-130.png` | Título y explicación caben; ambos botones completos y separados. |
| `h10-candado-inicial-claro-600-130.png` | Bloque centrado, texto y acciones completos; sin clipping ni solapamiento. |
| `h10-candado-inicial-oscuro-360-130.png` | Candado oscuro; explicación y botones visibles; textos legibles sobre fondo profundo. |
| `h10-candado-inicial-oscuro-412-130.png` | Marca y dos acciones completas con separación; sin recorte. |
| `h10-candado-inicial-oscuro-600-130.png` | Contenido centrado y completo en ancho amplio; no se filtra pantalla detrás. |
| `h10-perfil-inicial-claro-360-130.png` | Campo correo vacío y cinco acciones completas; baja de cuenta se distingue como destructiva; sin clipping. |
| `h10-perfil-inicial-claro-412-130.png` | Campo y cinco botones completos y separados; acción destructiva legible. |
| `h10-perfil-inicial-claro-600-130.png` | Perfil completo en viewport; las etiquetas de todas las acciones caben. |
| `h10-perfil-inicial-oscuro-360-130.png` | Campo y cinco acciones visibles; los roles se distinguen por texto/forma además de color. |
| `h10-perfil-inicial-oscuro-412-130.png` | Misma jerarquía en oscuro; etiquetas completas, sin solapamiento. |
| `h10-perfil-inicial-oscuro-600-130.png` | Acciones y campo completos; no se muestra correo real ni se activa acción. |
