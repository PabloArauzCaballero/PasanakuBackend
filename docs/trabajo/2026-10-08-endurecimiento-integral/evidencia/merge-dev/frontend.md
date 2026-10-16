# Integración de origin/dev — apps (backoffice, web/ui, móvil)

Regla: se conservan los dos comportamientos; donde chocan en semántica prima lo que ya está en dev (corre en el PR), salvo que lo nuestro corrija un defecto demostrable.

## backoffice
- `e2e/sistemas-sin-contrato.e2e.ts`: nuestro `prepararArranque(page)` (fija el meta del gateway y responde el refresco) MÁS el `page.route('**/sesion/refrescar')` de dev con el código real `AP-SES-01` y su aserción estricta de URL `/ingreso?volverA=%2Fsistemas%2Fservicios`. Dev prima en la aserción (más estricta que nuestra regex).
- `layout/shell-financiero.ts`: se toma el estilo de dev para el menú activo (`--brand-bg` + `--text`, AA documentado en claro y oscuro) y `minmax(0, 1fr)` en móvil; se conserva nuestra `grid-template-rows: auto auto 1fr` (el cabecera/derecha con `flex-wrap` ya estaba en común).
- `nucleo/errores.spec.ts` (add/add): los dos `describe` se conservan (CU-68 admisión y CU-01 decisión del expediente). `errores.ts` ya trae los códigos AP-CU68-* y AP-CU01-08..11.
- `rutas/cumplimiento/verificaciones/pantalla-de-expedientes.ts`: se adopta la estructura de dev (`ap-decision-de-expediente`, `ap-vencimiento-del-documento`, motivo por tarjeta, decisión en vuelo por expediente) y se conserva nuestro `ap-error-de-envio` (distingue "no pasó" de "no sabemos si pasó", muestra el código de seguimiento). Para no duplicar el mensaje, el error se guarda traducido entero por expediente (`errores: Record<id, ErrorTraducido>`) y se pinta con `ap-error-de-envio` dentro de la tarjeta; se conserva el texto de respaldo `decisionFallo` de dev. Se retira el `motivo` único compartido (era el defecto que dev corrigió).

## packages/ui
- `estilos.css`: se conservan las dos líneas (nuestros `--borde-control`/`--esqueleto` y los tokens de movimiento de dev).

## móvil
- `dominio/configuracion.dart`: sólo el comentario; se conserva el nuestro (más completo; mismo comportamiento).
- `configuracion_test.dart`: se conservan nuestra prueba de `10.0.2.2` en debug Y las dos de dev (debug sin lista, release rechaza).
- `enchufe_de_rutas_test.dart`, `gate_del_shell_test.dart`: se toma la versión de dev (misma lógica con comentario de Windows).
- Subida de fotos del alta (3 archivos): dev reemplazó la subida tras el alta (tres fotos) por la subida en lote de cinco capturas con máquina de estados (`SubidaNotifier`, `CaraDelCarril`, plazo y cancelación). Se adopta la de dev entera. Lo nuestro que se conserva: el `Token-De-Alta` (sin sesión, el servidor responde 403 sólo con `usuarioId`). Se hace viajar `tokenDeAlta`: `ResultadoDelAlta` → `AltaEnSeguimiento` (sólo memoria, no va en la URL) → `subirPendientes`/`reintentar` → `SubidaDeFotos.subir` (cabecera). `tokenDeAlta` es opcional, así que las pruebas de dev siguen igual. Se elimina `subirFotosDelExpediente` (nuestra versión de tres fotos), sin pruebas que la usaran.
