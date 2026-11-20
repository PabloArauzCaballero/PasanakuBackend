# Lote C — packages/ui, packages/tutoriales, shell del backoffice, shell del sitio web

Auditoría de solo lectura sobre `origin/test @07fc697`. Peldaño: `DISCOVERED` (nada se ejecutó; todo hallazgo sale de lectura + grep con ruta:línea).
La tabla de cobertura está al final (sección 1, ubicada al final por su largo).

## 2) Hallazgos candidatos

Orden: severidad propuesta, después impacto.

### H-C01 · El sitio público promete que un fondo de garantía cubre al que no paga y que el turno se cobra completo
- Clase: RESPALDO · Severidad: **S1** (texto público que contradice R-NUEVA-2 y crea una expectativa de dinero que la empresa ya no va a poner)
- Ubicaciones y citas:
  - `apps/web/contenido/paginas/como-funciona.md:4` «recibís tu turno con el fondo completo»; `:15` «recibís el fondo completo del período»; `:20` «Un fondo de garantía cubre la falta de un participante para que quien ya cobró y quien todavía no, no se vean afectados.»
  - `apps/web/contenido/paginas/preguntas.md:20` «Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado.»
  - `apps/web/contenido/paginas/inicio.md:5` (description SEO) «…sorteo verificable y fondo de garantía.»
  - `apps/web/src/app/app.routes.ts:117` (JSON-LD FAQPage que leen los buscadores) «Un fondo de garantía cubre esa falta para que el resto del grupo no se vea afectado…»
  - `apps/web/src/app/paginas/inicio/secciones/hero.ts:21` «recibí tu turno completo»; `:31` pastilla «Fondo de garantía».
  - `apps/web/src/app/paginas/inicio/secciones/producto.ts:68` «el fondo del período completo entra a tu billetera»; `:75` «Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno. El grupo no se cae por uno».
  - `apps/web/src/app/paginas/inicio/secciones/cierre.ts:69` «el fondo completo va para uno»; `:71` «el fondo de garantía cubre la entrega para que quien tenía el turno cobre igual».
  - `apps/web/src/app/paginas/inicio/secciones/transparencia.ts:65` «el fondo de garantía cubrió el que faltó» y `:30` el ejemplo muestra 9 de 10 aportes y entrega de «Bs 2.500,00» (= 10 × 250): pozo completo con un faltante.
  - Backoffice: `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-tablero.ts:25` «…el fondo de garantía comunitario cuente con respaldo suficiente para cubrir cualquier falta»; `explicaciones-operacion.ts:74` «el ganador reciba su fondo completo».
  - Catálogo UI: `packages/ui/src/catalogo/muestras.ts:30` etapa «Garantía: A los 10 días se cubre con el fondo de garantía.»
  - Propagación: `apps/web/scripts/contenido.mjs:46-64` copia esos .md a `public/*.md`, `llms.txt` y `llms-full.txt` (espejo para motores generativos).
- Cómo se comprobó: lectura completa de cada archivo + `grep -n -i -E "garant|cubr|completo|faltante"` sobre el lote.
- Impacto: promesa pública (y en datos estructurados para buscadores/LLM) de cobertura empresarial; un participante puede reclamar el pozo completo apoyado en el texto publicado.
- Causa de fondo: el contenido se escribió para el modelo anterior (fondo de garantía) y vive en tres capas sin fuente única (Markdown, componentes de portada con texto fijo y JSON-LD escrito a mano).

### H-C02 · El sitio ofrece canales de aporte/recarga distintos del QR confirmado por webhook
- Clase: FLUJO-QR · Severidad: **S2**
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/producto.ts:67` «Desde tu saldo, con QR o en efectivo en un punto de atención.»; `:71` «Recargás por QR, transferencia o efectivo»; `apps/web/contenido/paginas/como-funciona.md:15` «…mostrada antes de confirmar» (confirmación de la persona como paso del flujo); `apps/web/src/app/paginas/inicio/secciones/telefono.ts:20` «Aporte confirmado» (ilustración).
- Cómo se comprobó: lectura completa.
- Impacto: el texto público describe efectivo/transferencia como vías de aporte; contradice «sólo el pago por QR confirmado por webhook firmado mueve el flujo».
- Causa: texto heredado de la landing previa a la regla.

### H-C03 · El shell del backoffice cablea rutas, menú, permisos y textos de aprobación humana dentro del circuito del pasanaku
- Clase: APROB · Severidad: **S2** (es el punto de contacto que mantiene vivas las pantallas de aprobación; las pantallas son de otros lotes)
- Rutas registradas (`apps/backoffice/src/app/app.routes.ts`): `:94-98` `admisiones` (`ver:admisiones`), `:99-103` `postulaciones-organizador` (`ver:postulaciones`), `:84-88` `operacion` (contiene retiros en revisión y solicitudes escaladas).
- Menú: `apps/backoffice/src/app/nucleo/menu.ts:76` «Retiros en revisión» `p('RETIRO_APROBAR')`; `:85-90` grupo «Grupos y admisiones» con «Admisiones a grupos» y «Postulaciones a organizador»; `:91` «Solicitudes escaladas» `SOLICITUD_INGRESO_VER`.
- Permisos de sección: `apps/backoffice/src/app/nucleo/secciones.ts:21-30` `'RETIRO_APROBAR', 'ENTREGA_AUTORIZAR', 'ENTREGA_EJECUTAR', 'REVERSO_AUTORIZAR', 'SOLICITUD_INGRESO_RESOLVER'`; `:53-55` `'ver:admisiones': ['ADMIN_PLATAFORMA']`, `'ver:postulaciones': ['ADMIN_PLATAFORMA']`.
- Textos de error: `apps/backoffice/src/app/nucleo/errores.ts:41` `'AP-CU68-15': 'Este grupo exige admisión humana: la decisión del organizador es una propuesta y la resuelve backoffice.'`; `:42-46` CU-90 «postulación… antes de aprobar»; `:52` `'AP-CU11-11': 'Esta orden ya no está en revisión…'`; `:11-13` «No se aprueba un expediente…».
- Explicaciones: `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-operacion.ts:30-38` (retiro «cuatro ojos», «un segundo responsable… autorice formalmente la transferencia»), `:48` «Permite al analista aprobar o rechazar», `:86-90` admisiones/postulación.
- UI compartida: `packages/ui/src/tarjeta-de-solicitud/tarjeta-de-solicitud.ts:8-30` «Aceptar en el grupo» / «Rechazar»; `packages/ui/src/lista-de-requisitos/lista-de-requisitos.ts:11` «Qué falta para habilitarse»; `packages/ui/src/catalogo/catalogo-organismos.ts:39` «Se aprueba con límite de Bs 500»; `packages/ui/src/catalogo/catalogo-backoffice.ts:104` `valor: 'APROBADA'`.
- Pruebas que fijan el flujo viejo: `apps/backoffice/src/app/nucleo/entrada-estricta.spec.ts:143-159` (resolución de admisión `{ decision, motivo, revisionEsperada, propuestaId }` y aprobación de retiro `desenlace: 'AUTORIZADA'`), `matriz-de-roles.spec.ts:103-111` (segregación «quien resuelve admisiones»), `menu.spec.ts:34`, `precarga-segun-sesion.spec.ts:25`.
- Cómo se comprobó: `grep -n -i -E "aprob|habilit|postulaci|admisi|autoriz|en revisi"` sobre el lote + lectura completa de app.routes/menu/secciones/errores.
- Nota: los retiros de billetera con revisión humana pueden quedar fuera del «circuito del pasanaku» según cómo se interprete R-NUEVA-1 → **ambigüedad a confirmar con el dueño**; admisiones a grupos y postulación a organizador sí son del circuito.

### H-C04 · El sitio web en SSR no emite ninguna cabecera de seguridad y su proxy `/api` reenvía todo sin timeout
- Clase: SEG · Severidad: **S2**
- Ubicaciones: `apps/web/src/server.ts:107-124` (respuesta HTML sin CSP, HSTS, `X-Frame-Options`, `nosniff`, `Referrer-Policy`); `apps/web/docker/Dockerfile.web:1` dice «servido por Node detrás de NGINX» pero la imagen (`:20-33`) es solo Node, sin NGINX; `grep -rn -i "Content-Security-Policy|helmet|X-Frame" apps/web` → sin resultados; `despliegue/coolify/web.yml` tampoco las agrega (grep, fuera de lote).
- Proxy: `apps/web/src/server.ts:56-90` reenvía al gateway **todas** las cabeceras del cliente salvo cuatro (`SIN_REENVIO`, `:54`), incluido un `X-Forwarded-For` arbitrario; el `fetch` (`:67`) no tiene timeout (regla 98.2.1); cualquier `/api/*` pasa.
- Backoffice, CSP divergente entre sus dos NGINX: `apps/backoffice/docker/nginx.conf:9` `style-src 'self'` (bloquearía los `<style>` que inyecta Angular) vs `nginx.desplegado.conf:23` `style-src 'self' 'unsafe-inline'` e `img-src … blob:`.
- Impacto: clickjacking/MIME sniffing en el sitio público; un cliente puede inyectar cabeceras hacia el gateway; un gateway lento cuelga hilos de Node.
- Causa: el server.ts es la plantilla del CLI con un proxy agregado; el set de cabeceras se resolvió solo en el NGINX del backoffice.

### H-C05 · Exportación CSV/JSON en el navegador, activa por omisión, sin rastro de auditoría y con valores crudos
- Clase: SEG · Severidad: **S2**
- Ubicaciones: `packages/ui/src/listado/listado.ts:98` `readonly exportable = input(true)`; `:50-51` monta `ap-exportar-listado` con `listado().filtradas()` (todas las páginas); `:118-120` columnas exportables por omisión = `{ clave, titulo }` sin `valor` → `packages/ui/src/exportar-listado/exportar.ts:17-22` exporta `fila[clave]` crudo (no lo que la celda muestra enmascarado). `packages/ui/src/tabla-de-datos/tabla-de-datos.ts:148-149` (`comoTexto`) copia al portapapeles `String(f[c.clave])` crudo.
- Contradicción interna: `apps/backoffice/src/app/nucleo/exportador.ts:23-30` «CU-58, y nada más: **nunca** un `CSV.stringify` acá… la huella y el tope de descargas los decide el servidor».
- Alcance medido: `grep -rl "<ap-listado" apps/backoffice/src/app/rutas | wc -l` → 32; `grep -rn "\[exportable\]"` → 2. Treinta listados exportan desde el navegador.
- Permiso inexistente: `exportador.ts:37-43` arma `exportar:${recurso}`; `grep -ic exportar sql/60_semillas/10-roles-y-permisos.sql` → 0, y `secciones.ts:96-100` solo traduce `ver:*`, así que `puedeExportar` siempre es `false`: el canal auditado nunca se ofrece y el no auditado está siempre encendido.
- Registro de lecturas: `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.ts:22-25` apagado por omisión; `ACCESO_A_DATOS` no lo usa ninguna pantalla (`grep -rn ACCESO_A_DATOS apps/backoffice/src --include=*.ts | grep -v spec` → solo el interceptor); su texto `:66` «El equipo de auditoría ya quedó avisado» es falso (solo muestra un aviso local).
- Impacto: un operador baja listados completos con PII sin huella ni tope (regla 90.2.7) y sin enmascarar.

### H-C06 · Al restaurar o refrescar la sesión se pierde el `sujeto`: el avance de tutoriales de distintos operadores se mezcla
- Clase: DEFECTO / COHERENCIA · Severidad: **S2**
- Ubicaciones: `apps/backoffice/src/app/nucleo/auth-bootstrap.ts:35` `sesion.abrir(resultado.acceso, resultado.permisos, resultado.rol)` y `sesion.interceptor.ts:27` `sesion.abrir(nueva.acceso, …)` → `sesion.ts:50` `sujeto: string | null = null`. Solo el login usa `abrirConToken` (`rutas/ingreso/pantalla-de-ingreso.ts:158`, punto de contacto). `packages/tutoriales/src/almacen-local.ts:70` `PREFIJO + huella(this.identidad.sujeto() ?? 'anonimo')`.
- Promesa incumplida: `sesion.ts:32-36` «para que dos operadores en la misma máquina no compartan el avance».
- Además hay dos fuentes de permisos: claims del JWT en el login (`sesion.ts:95-101`) y cuerpo de `SalidaRenovacion` en el refresco; `RespuestaRefresco` (`refresco-de-sesion.ts:6-10`) está tipado a mano en vez de usar el generado.
- Impacto: tras recargar la página, todos los operadores comparten la clave `…anonimo`.

### H-C07 · Clones divergentes del núcleo de red entre backoffice y web (y cuatro definiciones de `ErrorTraducido`)
- Clase: DUP-X · Severidad: **S2** (misma intención, comportamiento distinto)
- `diff apps/backoffice/src/app/nucleo/X apps/web/src/app/nucleo/X`:
  - `errores.ts`: el web no tiene `case 502: case 503: case 504` (backoffice `:97-98`), así que un 503 en el sitio dice «Algo salió mal de nuestro lado» en vez de «servicio no disponible»; catálogo de códigos 3 (web `:7-11`) vs ~75 (backoffice `:7-78`); `mensajeDeTiempoAgotado()` con firma distinta (web `:26` sin parámetro, backoffice `:104` `(escritura: boolean)`).
  - `timeout.interceptor.ts`: el web aplica 15 s a todo, también a escrituras (web `:12-14`).
  - `gateway.ts`: web lee `process.env.APORTAYA_GATEWAY` y luego la meta (`:14-16, :42`); backoffice solo la meta.
  - `errores.interceptor.ts` y `traza.interceptor.ts`: idénticos byte a byte (clon consistente).
  - `configuracion-invalida.ts`: mismo componente con textos distintos; **no lo usa nadie** en ninguna de las dos apps (ver H-C11).
- `ErrorTraducido` definido 4 veces: `apps/backoffice/src/app/nucleo/errores.ts:5`, `apps/web/src/app/nucleo/errores.ts:5`, `packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts:12` (sin `codigo`/`tiempoAgotado`), `packages/ui/src/error-de-envio/error-de-envio.ts:5` (`ErrorParaVista`).
- Constante `GW` duplicada con valores distintos: `apps/backoffice/src/app/nucleo/gateway.ts:8` `'http://gw/api/v1'` vs `cadena-de-red.apoyo.ts:11` `'https://gw.test/api/v1'`.
- Causa: `packages/ui` declarado «no es de este carril» (`apps/web/src/app/nucleo/configuracion-invalida.ts:6-7`): se copió en vez de compartir.

### H-C08 · La UI promete «si lo repetís igual, no se duplica» aunque la clave de idempotencia es opcional
- Clase: CONTRATO / FRAGIL · Severidad: **S2**
- Ubicaciones: `apps/backoffice/src/app/nucleo/errores.ts:104-108` «…si la repetís igual, no se duplica.»; `packages/ui/src/error-de-envio/error-de-envio.ts:27` «Si repetís exactamente lo mismo, no se duplica.» (para todo 0/5xx/timeout); `apps/backoffice/src/app/nucleo/idempotencia.interceptor.ts:58-81`: solo hay clave si la petición trae `CLAVE_POR_CONTENIDO` o `CLAVE_IDEMPOTENCIA`; el resto sale **sin** `Idempotency-Key`.
- Comentario que miente sobre el orden: `idempotencia.interceptor.ts:73` «El interceptor de errores corre más adentro» — en `interceptores.ts:19-26` `erroresInterceptor` está **más afuera** que `idempotenciaInterceptor`.
- Parches: `VIGENCIA_DE_CLAVE_MS = 15 * 60_000` (`:33`, decisión del cliente); el mapa `intentos` es global de módulo (`:36`), no por inyector; `AlmacenRemoto.guardar` (`nucleo/tutoriales/almacen-remoto.ts:42`) genera clave nueva por llamada, contra el criterio del propio interceptor.
- Impacto: una pantalla que no marque su petición recibe la promesa de no-duplicado sin garantía.

### H-C09 · Componentes de dinero de `packages/ui`: día UTC, suma en el cliente sin verificar moneda y comparaciones con cadenas mágicas
- Clase: DINERO / DEFECTO · Severidad: **S3** (hoy sin consumidores en apps; ver H-C11)
- `packages/ui/src/lista-de-movimientos/lista-de-movimientos.ts:39` `const dia = m.fechaIso.slice(0, 10)` y `:44` `this.hoyIso().slice(0, 10)`: agrupa por día UTC (un movimiento a las 22:00 en La Paz cae al día siguiente). `:44` `neto(movimientos.map((m) => m.monto))` suma en el cliente sin verificar que `m.moneda` (`fila-de-movimiento.ts:7`) coincida con `moneda()`; `:44` `saldo: movimientos[0]?.saldoCorrido` depende del orden de entrada.
- `packages/ui/src/tarjeta-saldo/tarjeta-saldo.ts:20` `[deshabilitado]="saldo() === '0.00'"`: con `'0'`, `'0.0'` o un saldo negativo, «Retirar» queda activo; la regla de negocio vive en la vista.
- `packages/ui/src/campo-monto/campo-monto.ts:12` prefijo `Bs` fijo y `aCadenaDelContrato` (`:24-30`) devuelve un importe sin moneda; acepta `0` y ceros a la izquierda.
- `packages/ui/src/listado/crear-listado.ts:45-47` ordena importes en texto con `Number(a)` (solo orden de presentación; S4 por sí solo).
- `packages/ui/src/reloj-de-plazo/reloj-de-plazo.ts:49-57` «Faltan N días» por `Math.floor(ms/86_400_000)`, no por días calendario de La Paz; `fechaLegible` (`:45`) sin uso.
- Bordes: `item-pasanaku.ts:23` `turnoActual() / turnos()` sin guarda de cero; `medidor-de-rango.ts:43` divide por `maximo-minimo`; `fecha.ts:33-39` con ISO inválido produce «NaN undefined NaN».

### H-C10 · Textos públicos contradictorios sobre custodia y notas internas de carril publicadas
- Clase: DOC / COHERENCIA · Severidad: **S3**
- Custodia: `apps/web/contenido/paginas/descargar.md:22`, `cierre.ts:72` y `confianza.ts:57` dicen «AportaYa no custodia fondos de terceros», mientras `seguridad.md:3-16` («Cómo custodia AportaYa el dinero… Encaje»), `producto.ts:74` («Custodia separada… El encaje se verifica todos los días») e `inicio.md:4-5` («con custodia») afirman lo contrario.
- `como-funciona.md:24` «El dinero que aportás es el mismo que vas a recibir» contra `:15` «con la comisión ya descontada».
- Notas internas visibles al público: `contenido/legal/contrato-de-adhesion.md:18` y `contenido/legal/estado-regulatorio.md:24` («**Nota técnica de este carril (F9):** … **Pedido al carril de backend…**»); se renderizan con `[innerHTML]` (`contrato-de-adhesion.ts:19`) y van a `llms-full.txt`.
- `apps/web/public/legal/estado-regulatorio.md` es un artefacto **generado** (por `scripts/contenido.mjs:48-50`) que quedó versionado: `.gitignore:7` `public/*.md` no cubre subcarpetas.
- Cifras fijas presentadas como medición: `cierre.ts:60-67` («99 casos de uso», «305 tablas», «141 restricciones», «14 servicios», «medidas el 2026-09-16»).
- `transparencia.ts:60` «El sistema revisa la historia del grupo todos los días»: promesa operativa sin fuente en el lote.

### H-C11 · Código muerto y reescrituras paralelas en el shell y en `packages/ui`
- Clase: REESCRITURA · Severidad: **S3**
- Sin consumidores (grep de imports fuera del propio archivo y de su spec):
  - `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts` (197 líneas; solo aparece en comentarios de `rutas/contabilidad/...`). Además su `cargar()` (`:143-153`) tiene `try/finally` sin `catch`: el `error()` nunca se llena y no hay control de respuestas fuera de orden.
  - `apps/backoffice/src/app/layout/pestanas-de-seccion.ts` (no está en la plantilla de `shell-financiero.ts:41-77`).
  - `apps/backoffice/src/app/nucleo/filtros/barra-de-filtros.ts` (duplica `packages/ui/src/barra-de-filtros`; las 10 pantallas importan la de ui).
  - `apps/backoffice/src/app/nucleo/evidencia/panel-de-evidencia.ts`.
  - `ConfiguracionInvalida` + `provideGateway` + `CONFIGURACION_GATEWAY` en ambas apps (`apps/backoffice/e2e/config-invalida.e2e.ts:32` lo reconoce «pendiente de cableado»).
  - `ExplicadorDeVistaComponent` (`nucleo/explicador-de-vista.component.ts`) no se monta; las ~560 líneas de `nucleo/explicaciones/*.ts` igual entran al bundle vía `explicador-de-vista.ts:5,35-37` y afirman cosas falsas: `explicaciones-operacion.ts:36` «`POST /operaciones/retiros/{id}/aprobar`» cuando el contrato es `/billetera/retiros/{ordenId}/aprobacion` (`servicios/nucleo-financiero/.../nucleo-financiero.yaml:517`); «puntaje crediticio», «prueba de vida biométrica» (`:84,90`); «peso» en vez de boliviano (`explicaciones-tablero.ts:25`).
  - `apps/backoffice/src/app/nucleo/tutoriales/almacen-remoto.ts` (declarado no enchufado).
  - `packages/ui`: usados solo por el catálogo: `tarjeta-saldo`, `lista-de-movimientos`, `fila-de-movimiento`, `tipo-de-movimiento`, `desglose-de-cobro`, `resumen-de-periodo`, `item-pasanaku`, `riel-de-turnos`, `tarjeta-de-oferta`, `tarjeta-de-solicitud`, `vale`, `verificador-de-sorteo`, `cuenta-enmascarada`, `fila-de-cotejo`, `lista-de-requisitos`, `medidor-de-rango`, `banda-de-proposito`, `nota-de-vista`, `estrellas`, `punto`, `acordeon`, `barra-de-pasos` (bucle `grep -rlE "@aportaya/ui/<d>|../<d>/"` sobre `apps` y `packages`).
- Pares paralelos: `apps/backoffice/src/app/layout/nota-de-la-vista.ts` vs `packages/ui/src/nota-de-vista/nota-de-vista.ts`; `nucleo/tabla` vs `packages/ui/src/tabla-de-datos`; tres modelos de estado de colección (`tabla-de-datos-tipos.ts:27-30` `EstadoColeccion`, `listado.ts:22` `CargaDeListado`, `estado-de-pantalla.ts:14-21` `RAMA_DE`).

### H-C12 · Guardias y menú como copia manual de la cadena de permisos; atajos `ver:*` y códigos de rol usados como permiso
- Clase: FRAGIL / SEG · Severidad: **S3** (el texto dice explícitamente que el servidor decide; el riesgo es de mantenimiento)
- `apps/backoffice/src/app/nucleo/menu.ts:36-37` «La cadena de cada sección, copiada de `app.routes.ts` y de cada `*.routes.ts`» (lo cubre `menu-y-guardias.spec.ts`); `secciones.ts:87-100` `ver:tablero` y `ver:ayuda` abren siempre; `secciones.ts:34-35, 46-49, 75` mezclan **códigos de rol** (`TESORERIA`, `CONTABILIDAD`, `OFICIAL_CUMPLIMIENTO`…) como si fueran permisos; `menu.ts:46-47` «el `rol` del token real es siempre `OPERADOR`», así que `menu-de-usuario.ts:60-63` muestra siempre «Operador».
- Punto ciego de prueba: `matriz-de-roles.spec.ts:138-141` solo detecta literales `puede('X')`; el permiso dinámico `exportar:${recurso}` (`exportador.ts:38`) escapa a la verificación «todo permiso pedido existe en el catálogo» (ver H-C05).
- Rutas registradas por el shell (`app.routes.ts`): `ingreso`, `arranque` (sin sesión); bajo `ShellFinanciero` con `requiereSesion()`: `tablero` (sin permiso), `tablero/financiero` `TABLERO_FINANCIERO_VER`, `tablero/sistemas` `TABLERO_SISTEMAS_VER`, `tablero/catalogo-de-datos` `TABLERO_SISTEMAS_VER`, `…/:esquema/:tabla/ficha` `CATALOGO_DATOS_EDITAR`, `…/:esquema/:tabla` `TABLERO_SISTEMAS_VER`, `…/:tabla` (redirección), `tablero/comercial` `TABLERO_COMERCIAL_VER`, `operacion` `ver:operacion`, `cumplimiento` `ver:cumplimiento`, `admisiones` `ver:admisiones`, `postulaciones-organizador` `ver:postulaciones`, `grupos` `ver:grupos`, `sistemas` `ver:sistemas`, `contabilidad` `ver:contabilidad`, `ayuda` (sin guardia), `publicidad` `ver:publicidad`, `configuracion` `ver:configuracion`. Todos los permisos literales existen en `sql/60_semillas/10-roles-y-permisos.sql` (verificado con `grep -c`).

### H-C13 · Fallos de UX/A11Y en el shell y en tutoriales
- Clase: UX-A11Y · Severidad: **S3**
- Tutorial que no puede resaltar: `apps/web/src/app/layout/cabecera-de-pagina.ts:11` `host: { style: 'display: contents' }` y el objetivo está en ese host (`paginas/como-funciona/como-funciona.ts:13`, `paginas/transparencia/transparencia.ts:13`); `packages/tutoriales/src/objetivo.ts:69-70` devuelve `null` si el recuadro mide 0×0, que es lo que mide un elemento `display: contents`. El catálogo (`tutoriales/catalogo/herramientas.ts:92`, `primeros-pasos.ts:64`) apunta a esos ids.
- Contraste: `apps/web/src/sitio/sitio.css:67-68` `.boton--claro` documenta «3.32:1» para texto normal (WCAG AA pide 4.5:1); se usa en `cierre.ts:51`.
- Literales de color: `apps/web/src/sitio/tokens-sitio.css:11-45` define hex (`#E7F5EC`, `#081A11`…) bajo un comentario que dice «acá no se redefine ninguno»; `sitio.css:1-9` «Cero hex sueltos»; `apps/web/src/index.html:8` `theme-color #1C5A3A`.
- Campana: `apps/backoffice/src/app/layout/campana-de-notificaciones.ts:136-142` solo carga al abrir: la insignia de no leídos no aparece nunca antes del primer clic; `:154-160` `error: () => undefined` traga fallos de «Marcar leído» sin aviso; `:167-173` hora con `toLocaleTimeString([])` sin zona La Paz ni fecha; `:103` `transition: background .15s ease` literal; `nucleo/avisos-internos.ts:56` mensaje de error fijo.
- Centro de ayuda: `packages/tutoriales/src/registro.ts:65-74` si un `import()` del catálogo falla, `pedido` queda como promesa rechazada para siempre; `centro/pantalla-centro-de-ayuda.ts:126-130` muestra entonces «Todavía no hay tutoriales para tu rol» (sin estado cargando ni error); `:161` `subscribe()` sin manejo de error al reiniciar.
- Motor: `packages/tutoriales/src/motor.ts:132-156` `ir()` asíncrono sin cancelación: avanzar rápido deja que una búsqueda vieja (`esperar`, hasta 4 s) fije el elemento de otro paso.
- Vacío mudo: `packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts:63` pasa `explicacion=""` (sin por qué ni qué hacer, regla 95.2.2).
- Búsqueda: `apps/backoffice/src/app/layout/shell-financiero.css:57` oculta el buscador global por debajo de 48rem, sin alternativa en el celular.
- `apps/backoffice/src/app/nucleo/configuracion-invalida.ts:20` pone el nombre de pila de una persona de guardia en la UI (dato frágil; no es PII de participantes).

### H-C14 · Configuración y comentarios de despliegue que se contradicen
- Clase: CONFIG / DOC · Severidad: **S3**
- `apps/backoffice/docker/nginx.desplegado.conf:6-8` y `apps/web/src/server.ts:44-46` dicen que sin la meta «`gatewayPorDefecto()` cae a http://localhost:4010»; `nucleo/gateway.ts:41-54` (ambas apps) dice lo contrario («nunca localhost»).
- La meta ya viene en el HTML: `apps/backoffice/src/index.html:12` y `apps/web/src/index.html:10`; `nginx.desplegado.conf:56` (`sub_filter`) y `server.ts:100-105` la agregan otra vez (duplicada). `apps/backoffice/src/index.desarrollo.html` dice «SOLO desarrollo» para la misma etiqueta que ya está en el index de producción.
- `apps/backoffice/docker/nginx.conf` no tiene `location /api/` pero el index apunta la API a `/api/v1`: con esa configuración un `GET` a la API devuelve `index.html` con 200 (`try_files … /index.html`, `:30`).
- READMEs del CLI sin adaptar: `apps/backoffice/README.md:1-59`, `apps/web/README.md:1-59`.
- Temporales personales: `apps/backoffice/tsconfig.spec.richard.json:2` («Se borra cuando…»); `apps/backoffice/src/tipos/clientes-angular.doble.d.ts:1-77` (módulos `any`, «DOBLE TEMPORAL», mientras `clientes/angular` se genera, `.gitignore:50`).
- `apps/web/src/server.ts:15-25` comentario de plantilla «Example Express Rest API endpoints…».

### H-C15 · Pruebas: reintentos, paralelismo sin tope, pruebas vacuas y comentarios obsoletos
- Clase: PRUEBAS · Severidad: **S3**
- `apps/backoffice/playwright.config.ts:30` `retries: 1` (regla 80.5.3).
- `apps/web/playwright.config.ts:7-45` sin `workers: 1` y `reuseExistingServer: true` también en CI (`:27,40`), al revés que el backoffice (`:36,42`).
- `apps/web/src/app/enchufe-de-paginas.spec.ts:19-30`: escribe un archivo en `src/app/paginas` durante la prueba y compara huellas que excluyen esa misma carpeta → nunca puede fallar.
- `apps/web/pruebas/e2e/web-verificacion.e2e.ts:45-47` y `:60` describen el estado viejo («`hydrate on viewport`», «hoy esto SÍ aparece, y es el bug») que el propio archivo da por resuelto en `:21-32`.
- `apps/web/pruebas/e2e/accesibilidad.e2e.ts:36` solo falla con `serious`/`critical`; los `moderate` pasan.
- `packages/tutoriales/package.json:22` `test:a11y` es un `echo`; el paquete tiene un solo spec (`src/validacion.spec.ts`) y sus pruebas viven en `apps/backoffice/src/app/nucleo/tutoriales/*.spec.ts` (el paquete depende de una app para probarse).
- Pruebas que fijan el flujo de aprobación: ver H-C03.

### H-C16 · Fragilidad de SEO/GEO: listas «compartidas» que no se comparten y FAQ duplicada a mano
- Clase: FRAGIL · Severidad: **S3**
- `apps/web/src/app/seo/sitemap.mjs:5-7` dice que `RUTAS_EXCLUIDAS_SITEMAP` «se comparte con RUTAS_NO_INDEXABLES… para que nunca diverjan», pero son tres literales distintos: `sitemap.mjs:7` (2 rutas), `geo/robots.mjs:5` (5), `geo/llms.mjs:10` (5).
- `https://aportaya.bo` repetido en `seo/json-ld.ts:54`, `seo/servicio-meta.ts:10`, `scripts/contenido.mjs:55,60,63,64`.
- FAQ JSON-LD escrita a mano (`app.routes.ts:114-122`) que ya diverge del Markdown (`preguntas.md:20` vs `app.routes.ts:117`) pese a que `json-ld.ts:83-86` exige que salga del texto de la página.
- Canonical sin barra vs sitemap con barra (lo documenta `web-seo.e2e.ts:13-19`).

### H-C17 · Duplicación visual entre apps y paquete
- Clase: DUP-X / DUP-INT · Severidad: **S3**
- Marca: `apps/web/src/app/layout/marca.ts:15-24` y `apps/backoffice/src/app/layout/simbolo-de-marca.ts:14-21` (mismos trazos).
- Tres sistemas de íconos: `apps/web/src/app/layout/trazos.ts` + `icono.ts`, `apps/backoffice/src/app/layout/icono-de-seccion.ts`, `packages/ui/src/icono/icono.ts`.
- `apps/web/src/app/layout/cabecera-de-pagina.ts` vs `packages/ui/src/cabecera-de-pagina/cabecera-de-pagina.ts`.
- Nueve páginas web con la misma plantilla copiada (solo cambian la etiqueta y la clave): `paginas/{como-funciona,contrato-de-adhesion,descargar,legal-estado-regulatorio,preguntas,privacidad,reclamos,seguridad,transparencia}/*.ts`.
- `MESES` duplicado en `packages/ui/src/fecha/fecha.ts:3` y `lista-de-movimientos.ts:67`.

### H-C18 · Datos sensibles que llegan enteros al cliente para enmascararse o compararse en la vista
- Clase: SEG / DDD · Severidad: **S3**
- `packages/ui/src/cuenta-enmascarada/cuenta-enmascarada.ts:12` recibe `numero` completo y enmascara en el navegador (`:18-21`): el número entero viaja al cliente (regla 90.1.8, respuesta mínima).
- `packages/ui/src/fila-de-cotejo/fila-de-cotejo.ts:27` decide en el cliente «Coincide/No coincide» entre lo declarado y el documento (decisión de KYC en la vista).
- (Hoy los dos solo se usan en el catálogo; el riesgo aparece cuando se adopten.)

### H-C19 · Parches e higiene
- Clase: PARCHE / CLEAN · Severidad: **S4**
- Comentario que miente: `apps/backoffice/src/app/layout/dominio/cerrar-sesion.ts:21` dice «→ «Admin plataforma»» y el código (`:23`) produce «Administración de plataforma».
- JSDoc duplicado: `apps/backoffice/src/app/layout/icono-de-seccion.ts:3-4`.
- `packages/ui/src/estilos.css:34-75` reglas globales que entran al interior de `ap-tabla-de-datos` «para no pasar su presupuesto de 4 kB».
- Literales de tiempo: `auth-bootstrap.ts:8` 5000; `timeout.interceptor.ts:47-48` 15 s/30 s; `packages/ui/src/toast/toast.ts:15` 5000 (los avisos se cierran solos, WCAG 2.2.1); `precarga-segun-sesion.ts:120` 3000; `nucleo/tabla/navegacion-por-teclado.ts:34` reintentos 5×16 ms.
- `catch` que tragan errores: `precarga-segun-sesion.ts:98-100`, `indice-de-busqueda.ts:48-50`, `campana-de-notificaciones.ts:140-160`, `shell-financiero.ts:129` `error: () => undefined`.
- Comentario huérfano: `apps/web/src/sitio/sitio.css:622-624` habla de una clase `js` que «pone una línea en el <head>» que no existe en `index.html`.
- `packages/ui/src/dialogo/dialogo.ts:134-136` usa `window.confirm` nativo para el descarte.
- `apps/backoffice/src/app/layout/buscador-global.ts:31` `$any($event.target)`; ídem `packages/ui` `area-de-texto.ts:20`, `lista-de-opciones.ts:25`, `paginacion.ts:22,28`.

### H-C20 · Resiliencia de arranque del SSR
- Clase: RESILIENCIA · Severidad: **S3**
- `apps/web/src/server.ts:67` `fetch(gatewayInterno + req.originalUrl, …)` sin `AbortSignal`/timeout ni circuit breaker; un error va a `next(error)` (`:87-89`) y queda en manos del manejador por omisión de Express.
- `apps/web/src/app/nucleo/gateway.ts:14-16` lee `APORTAYA_GATEWAY` y `server.ts:51` lee `APORTAYA_GATEWAY_INTERNO`: dos variables para el mismo destino que hay que mantener alineadas (`playwright.config.ts:31-38` explica que si difieren se rompe la hidratación, NG0502).

## 3) Lo que está bien (preservar)

- Token de acceso solo en memoria; refresco por cookie HttpOnly, sin `localStorage` (`apps/backoffice/src/app/nucleo/sesion.ts:22-29`, `refresco-de-sesion.ts:37`). En el lote no se guardan tokens en almacenamiento accesible por script (`grep localStorage|sessionStorage`: solo tutoriales, recientes del buscador y estado de notas).
- Refresco single-flight con `HttpBackend` y un solo reintento por petición (`refresco-de-sesion.ts:23-46`, `sesion.interceptor.ts:15-45`), con pruebas de concurrencia (`sesion.interceptor.spec.ts:50-140`).
- Clave de idempotencia por contenido canónico, conservada ante 0/5xx/408/425/429 y liberada ante 4xx definitivos (`idempotencia.interceptor.ts:43-56`), con pruebas (`cadena-de-red.spec.ts:44-130`, `cadena-de-red.timeout.spec.ts`).
- Ningún error crudo del backend llega a la pantalla: todo pasa por `mensajeDe` (`errores.interceptor.ts:9-23`); el 403 no confirma que el recurso exista.
- Dinero como cadena del contrato, sin `parseFloat`: `ap-monto` exige moneda (`packages/ui/src/monto/monto.ts:19-20`), formateo único en `@aportaya/tokens/dinero`, `neto()` con `BigInt` (`lista-de-movimientos.ts:49-60`), `desglose-de-cobro` no suma (`:7-8`).
- Las guardias declaran «el rol oculta, no protege» (`permisos.ts:9-12`, `menu.ts:64`, `shell-financiero.ts:34-35`) y el sitio público no tiene guardias ni escrituras (`sin-acciones-ajenas.spec.ts`).
- `volverA` validado como ruta interna (`restaurando-sesion.ts:9-11`); enlaces de avisos solo internos (`campana-de-notificaciones.ts:68`).
- CSV con defensa contra inyección de fórmulas (`exportar.ts:31-42`).
- Accesibilidad trabajada: diálogos con foco atrapado, Escape y restauración de foco (`packages/ui/src/dialogo/dialogo.ts`), menús con teclado (`menu-de-usuario.ts:83-96`), movimiento reducido global (`packages/ui/src/estilos.css:32`, `sitio.css:626-632`), «no solo color» explícito en chips y pestañas.
- SSR: accesos a `window/document/localStorage` con guarda; rutas de terceros con `noindex` en meta y en `X-Robots-Tag` (`app.routes.server.ts:28-38`); `robots.txt` separa citación de entrenamiento.
- Borradores en IndexedDB y no en `localStorage` (`nucleo/borrador.ts:6-16`).
- `innerHTML` sin `bypassSecurityTrust*`: Angular sanitiza el Markdown generado (grep `bypassSecurity` → 0).

## 4) Vistos fuera del lote (una línea cada uno)

- `apps/backoffice/src/app/rutas/**` — 30 de 32 `<ap-listado>` sin `[exportable]` (exportan PII en el navegador; ver H-C05).
- `apps/backoffice/src/app/rutas/ingreso/pantalla-de-ingreso.ts:158` — único llamador de `abrirConToken` (H-C06).
- `apps/backoffice/src/app/rutas/ayuda/catalogo/proveer.ts:49-56` — `identidadDelOperador()` depende de `sesion.sujeto()` (H-C06).
- `apps/backoffice/e2e/config-invalida.e2e.ts:32` — reconoce que `ConfiguracionInvalida` no está cableada.
- `apps/web/src/app/verificadores/verificador-de-cadena.ts:37` — según `web-verificacion.e2e.ts:12-19`, todavía no recalcula la cadena.
- `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:517` — `/billetera/retiros/{ordenId}/aprobacion` (APROB, retiro con revisión humana).
- `sql/60_semillas/10-roles-y-permisos.sql` — no siembra ningún `exportar:*`.
- `despliegue/coolify/web.yml:39,49` — define `APORTAYA_GATEWAY` y `NG_TRUST_PROXY_HEADERS`, pero ninguna cabecera de seguridad.
- `servicios/identidad/src/main/resources/openapi/identidad.yaml:1280-1294` — `SalidaRenovacion` no trae `sub`: el cliente no puede reponer el sujeto sin leer el JWT.

## 1) Tabla de cobertura

Total de archivos del lote: 422 (packages/ui + packages/tutoriales: 173; shell backoffice: 134; shell web: 115). Leídos completos: 235. El resto, por escaneo dirigido (declarado en cada fila). La descripción sale del primer comentario del archivo cuando existe.

| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| `apps/backoffice/.editorconfig` | 17 | sí | Configuración: .editorconfig |
| `apps/backoffice/.gitignore` | 44 | sí | Configuración: .gitignore |
| `apps/backoffice/.prettierrc` | 12 | sí | Configuración: .prettierrc |
| `apps/backoffice/README.md` | 59 | sí | Doc: Backoffice |
| `apps/backoffice/angular.json` | 145 | parcial: configs build (l.40-100) + grep | Configuración: angular.json |
| `apps/backoffice/docker/Dockerfile.backoffice` | 32 | sí | Configuración: Dockerfile.backoffice |
| `apps/backoffice/docker/nginx.conf` | 31 | sí | Configuración: nginx.conf |
| `apps/backoffice/docker/nginx.desplegado.conf` | 59 | sí | Configuración: nginx.desplegado.conf |
| `apps/backoffice/eslint.config.js` | 36 | sí | Configuración: eslint.config.js |
| `apps/backoffice/package.json` | 47 | sí | Configuración: package.json |
| `apps/backoffice/playwright.config.ts` | 49 | sí | Configuración: playwright.config.ts |
| `apps/backoffice/proxy.desarrollo.json` | 7 | sí | Configuración: proxy.desarrollo.json |
| `apps/backoffice/proxy.e2e.json` | 7 | sí | Configuración: proxy.e2e.json |
| `apps/backoffice/proxy.stack-local.json` | 7 | sí | Configuración: proxy.stack-local.json |
| `apps/backoffice/pruebas/preparar.ts` | 4 | sí | preparar.ts |
| `apps/backoffice/pruebas/vitest-axe.d.ts` | 11 | sí | vitest-axe.d.ts |
| `apps/backoffice/public/favicon.ico` | 1 | no: binario vacío (0 líneas) | favicon.ico |
| `apps/backoffice/src/app/app.config.ts` | 26 | sí | Configuración: app.config.ts |
| `apps/backoffice/src/app/app.routes.ts` | 138 | sí | El enchufe por dominio.** Lo escribe el shell (F6) UNA vez y se congela: cada |
| `apps/backoffice/src/app/app.ts` | 10 | sí | app.ts |
| `apps/backoffice/src/app/enchufe-de-rutas.spec.ts` | 42 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de enchufe-de-rutas |
| `apps/backoffice/src/app/layout/buscador-global.spec.ts` | 79 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de buscador-global |
| `apps/backoffice/src/app/layout/buscador-global.ts` | 144 | sí | El buscador global de la cabecera: un combobox sobre pantallas y tablas (`IndiceDeBusqueda`). |
| `apps/backoffice/src/app/layout/campana-de-notificaciones.a11y.spec.ts` | 58 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de campana-de-notificaciones |
| `apps/backoffice/src/app/layout/campana-de-notificaciones.spec.ts` | 180 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de campana-de-notificaciones |
| `apps/backoffice/src/app/layout/campana-de-notificaciones.ts` | 175 | sí | H5.S0 · Campana de notificaciones y avisos internos para operadores. |
| `apps/backoffice/src/app/layout/dominio/cerrar-sesion.spec.ts` | 70 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cerrar-sesion |
| `apps/backoffice/src/app/layout/dominio/cerrar-sesion.ts` | 25 | sí | CU-04 · cerrar la sesión. Contrato real: `DELETE /sesion` (`cerrarSesion`), que revoca |
| `apps/backoffice/src/app/layout/icono-de-seccion.ts` | 92 | parcial: l.1-25 y 89-92 | Trazos de 24×24, uno por sección del menú. Mismo grosor y remate que `ap-icono`. |
| `apps/backoffice/src/app/layout/menu-de-usuario.ts` | 101 | sí | El menú de quien opera, en la cabecera: iniciales en un círculo, el rol en texto legible, y un |
| `apps/backoffice/src/app/layout/menu-lateral.css` | 123 | no: sólo grep (hex/motion) | Estilos de menu-lateral |
| `apps/backoffice/src/app/layout/menu-lateral.spec.ts` | 110 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de menu-lateral |
| `apps/backoffice/src/app/layout/menu-lateral.ts` | 129 | sí | La navegación del backoffice: grupos **fijos** con rótulo en mayúsculas y, debajo de cada sección, **todas** sus |
| `apps/backoffice/src/app/layout/navegacion-del-shell.a11y.spec.ts` | 32 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de navegacion-del-shell |
| `apps/backoffice/src/app/layout/nota-de-la-vista.spec.ts` | 55 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de nota-de-la-vista |
| `apps/backoffice/src/app/layout/nota-de-la-vista.ts` | 43 | sí | «Sobre esta pantalla»: las notas de la vista detrás de un botón ⓘ en la cabecera fija, en un panel que flota. |
| `apps/backoffice/src/app/layout/pestanas-de-seccion.spec.ts` | 54 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de pestanas-de-seccion |
| `apps/backoffice/src/app/layout/pestanas-de-seccion.ts` | 119 | sí | Las pantallas de la sección actual, como las pestañas de sección del portal interno de Atlas (`section-tabs.tsx`): |
| `apps/backoffice/src/app/layout/shell-financiero.css` | 60 | sí | Estilos de shell-financiero |
| `apps/backoffice/src/app/layout/shell-financiero.spec.ts` | 100 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de shell-financiero |
| `apps/backoffice/src/app/layout/shell-financiero.ts` | 131 | sí | El shell del backoffice con la anatomía del portal interno de Atlas: barra lateral oscura con la |
| `apps/backoffice/src/app/layout/simbolo-de-marca.ts` | 34 | sí | El símbolo de AportaYa (`landing/assets/img/simbolo.svg`): las patas de la apacheta en |
| `apps/backoffice/src/app/layout/sistemas/shell-sistemas.ts` | 25 | sí | El marco del backoffice de sistemas: el banner de datos de ejemplo (es del shell, no de cada |
| `apps/backoffice/src/app/layout/tutoriales/anfitrion-de-tutorial.a11y.spec.ts` | 64 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de anfitrion-de-tutorial |
| `apps/backoffice/src/app/layout/tutoriales/anfitrion-de-tutorial.spec.ts` | 162 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de anfitrion-de-tutorial |
| `apps/backoffice/src/app/layout/url-actual.ts` | 21 | sí | La URL de la vista, como señal, **una sola vez para todo el shell**. Antes cabecera, menú y pestañas se suscribían |
| `apps/backoffice/src/app/nucleo/auth-bootstrap.spec.ts` | 115 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de auth-bootstrap |
| `apps/backoffice/src/app/nucleo/auth-bootstrap.ts` | 54 | sí | Cuánto se espera la respuesta del refresco antes de rendirse con `ERROR`. |
| `apps/backoffice/src/app/nucleo/avisos-internos.spec.ts` | 107 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de avisos-internos |
| `apps/backoffice/src/app/nucleo/avisos-internos.ts` | 107 | sí | H5.S0 · Avisos internos para operadores (la campana del backoffice). |
| `apps/backoffice/src/app/nucleo/borrador.ts` | 56 | sí | Borrador local de formularios largos (ROS, actas) en `IndexedDB`, **nunca** en |
| `apps/backoffice/src/app/nucleo/cadena-de-red.apoyo.ts` | 25 | sí | Montaje compartido de las pruebas de la cadena de red real (`INTERCEPTORES_DE_RED`). |
| `apps/backoffice/src/app/nucleo/cadena-de-red.spec.ts` | 134 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cadena-de-red |
| `apps/backoffice/src/app/nucleo/cadena-de-red.timeout.spec.ts` | 57 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cadena-de-red.timeout |
| `apps/backoffice/src/app/nucleo/configuracion-invalida.a11y.spec.ts` | 24 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de configuracion-invalida |
| `apps/backoffice/src/app/nucleo/configuracion-invalida.ts` | 31 | sí | Configuración: configuracion-invalida.ts |
| `apps/backoffice/src/app/nucleo/entrada-estricta.spec.ts` | 162 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de entrada-estricta |
| `apps/backoffice/src/app/nucleo/errores.interceptor.ts` | 23 | sí | Todo error sale de acá ya traducido: la pantalla nunca ve el mensaje del backend ni |
| `apps/backoffice/src/app/nucleo/errores.spec.ts` | 52 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de errores |
| `apps/backoffice/src/app/nucleo/errores.ts` | 109 | sí | Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso» |
| `apps/backoffice/src/app/nucleo/evidencia/panel-de-evidencia.ts` | 55 | sí | Bitácora, movimientos y trazas de un caso — reclamos, disputas y descargos la |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-cumplimiento.ts` | 106 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Explicaciones para el módulo Cumplimiento y Control Normativo de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-finanzas-publicidad.ts` | 111 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Explicaciones para los módulos de Finanzas (Contabilidad ERP) y Publicidad de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-operacion.ts` | 95 | parcial: l.25-95 + grep | Explicaciones para el módulo Operación y Admisiones de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-plataforma.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Explicaciones para los módulos de Configuración de Plataforma y Centro de Ayuda de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-sistemas.ts` | 137 | parcial: grep + l.54-58 | Explicaciones para el módulo Sistemas e Infraestructura de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-tablero.ts` | 58 | parcial: grep + l.20-28 | Explicaciones para el módulo Inicio y Tableros Ejecutivos de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/index.spec.ts` | 69 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de index |
| `apps/backoffice/src/app/nucleo/explicaciones/index.ts` | 102 | parcial: l.40-102 | Comprueba si `pathname` coincide con un patrón `prefijo`. |
| `apps/backoffice/src/app/nucleo/explicaciones/tipos.ts` | 42 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Tipos de datos para el explicador de vistas del backoffice de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.component.css` | 175 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Estilos de explicador-de-vista.component |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.component.spec.ts` | 91 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de explicador-de-vista.component |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.component.ts` | 141 | sí | Componente que presenta el explicador de la vista y del módulo en dos planos: |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.ts` | 54 | sí | Servicio central del explicador de vistas de AportaYa. |
| `apps/backoffice/src/app/nucleo/exportador.ts` | 48 | sí | CU-58, y nada más: **nunca** un `CSV.stringify` acá. Este servicio pide al backend que |
| `apps/backoffice/src/app/nucleo/filtros/barra-de-filtros.spec.ts` | 45 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de barra-de-filtros |
| `apps/backoffice/src/app/nucleo/filtros/barra-de-filtros.ts` | 57 | sí | Chips de filtro + búsqueda con **el estado en la `querystring`**: `?filtro=a,b&q=texto`. |
| `apps/backoffice/src/app/nucleo/gateway.spec.ts` | 87 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de gateway |
| `apps/backoffice/src/app/nucleo/gateway.ts` | 71 | sí | Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. |
| `apps/backoffice/src/app/nucleo/idempotencia.interceptor.ts` | 82 | sí | La clave viaja por el contexto de la petición: la genera el formulario al abrirse |
| `apps/backoffice/src/app/nucleo/indice-de-busqueda.ts` | 68 | sí | Un resultado del buscador global: a dónde lleva y cómo se describe. |
| `apps/backoffice/src/app/nucleo/interceptores.ts` | 26 | sí | La cadena de red del backoffice, en el orden en que se atraviesa HACIA el servidor (la respuesta vuelve en el orden inve… |
| `apps/backoffice/src/app/nucleo/matriz-de-roles.spec.ts` | 185 | parcial: l.1-30, 120-160 + nombres de tests | Pruebas unitarias/estructura de matriz-de-roles |
| `apps/backoffice/src/app/nucleo/menu-y-guardias.spec.ts` | 102 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de menu-y-guardias |
| `apps/backoffice/src/app/nucleo/menu.spec.ts` | 104 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de menu |
| `apps/backoffice/src/app/nucleo/menu.ts` | 197 | sí | Una condición de acceso: alcanza con **uno** de sus permisos (`Sesion.puede`) o con que el rol sea uno de sus `roles`. |
| `apps/backoffice/src/app/nucleo/navegacion.ts` | 81 | sí | Las preguntas que el shell le hace al árbol del menú (`menu.ts`): qué ve esta sesión, a dónde lleva cada entrada, |
| `apps/backoffice/src/app/nucleo/permisos.spec.ts` | 96 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de permisos |
| `apps/backoffice/src/app/nucleo/permisos.ts` | 58 | sí | `canMatch` funcional por permiso: si el rol no lo tiene, la ruta **no monta** (regla 4 |
| `apps/backoffice/src/app/nucleo/precarga-segun-sesion.spec.ts` | 66 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de precarga-segun-sesion |
| `apps/backoffice/src/app/nucleo/precarga-segun-sesion.ts` | 123 | sí | Precarga en reposo de **lo que esta sesión puede abrir** (F2, 2026-10-26). Sin estrategia, cada clic del menú |
| `apps/backoffice/src/app/nucleo/refresco-de-sesion.ts` | 47 | sí | Un solo refresco en vuelo por vez: N suscriptores concurrentes a `refrescar()` comparten |
| `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.spec.ts` | 117 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de registro-de-acceso.interceptor |
| `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.ts` | 74 | sí | Marca una petición como «vista datos personales» (CU-58, `R-SEG-02`). La pantalla que |
| `apps/backoffice/src/app/nucleo/restaurando-sesion.a11y.spec.ts` | 44 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de restaurando-sesion |
| `apps/backoffice/src/app/nucleo/restaurando-sesion.spec.ts` | 118 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de restaurando-sesion |
| `apps/backoffice/src/app/nucleo/restaurando-sesion.ts` | 83 | sí | Ruta interna verdadera: nunca una URL externa que el `volverA` pudiera traer. |
| `apps/backoffice/src/app/nucleo/secciones.spec.ts` | 60 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de secciones |
| `apps/backoffice/src/app/nucleo/secciones.ts` | 101 | sí | Qué permiso real abre cada sección del backoffice. |
| `apps/backoffice/src/app/nucleo/senal-de-red.ts` | 23 | sí | ¿El navegador dice que no hay red? Se actualiza con los eventos `online`/`offline` y se desuscribe |
| `apps/backoffice/src/app/nucleo/sesion.interceptor.spec.ts` | 180 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de sesion.interceptor |
| `apps/backoffice/src/app/nucleo/sesion.interceptor.ts` | 45 | sí | Marca la petición de refresco y el reintento, para no entrar en bucle. |
| `apps/backoffice/src/app/nucleo/sesion.spec.ts` | 67 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de sesion |
| `apps/backoffice/src/app/nucleo/sesion.ts` | 120 | sí | `UNKNOWN` es el único estado de arranque: todavía no se intentó el refresco. `RESTORING` |
| `apps/backoffice/src/app/nucleo/tabla/cursor.spec.ts` | 82 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cursor |
| `apps/backoffice/src/app/nucleo/tabla/cursor.ts` | 56 | sí | Bandejas paginadas por cursor (carril A, `convenciones-contrato.md`).** Todas las listas del backoffice responden |
| `apps/backoffice/src/app/nucleo/tabla/navegacion-por-teclado.spec.ts` | 29 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de navegacion-por-teclado |
| `apps/backoffice/src/app/nucleo/tabla/navegacion-por-teclado.ts` | 41 | sí | La parte pura de la navegación fila a fila: qué índice corresponde según la tecla. |
| `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.css` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Estilos de tabla-de-datos-virtualizada |
| `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.spec.ts` | 115 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tabla-de-datos-virtualizada |
| `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts` | 197 | sí | El organismo que van a usar las 64 pantallas de `F7`/`F8`: columnas configurables, |
| `apps/backoffice/src/app/nucleo/tabla/tipos.ts` | 18 | sí | Tipos compartidos de la tabla de datos virtualizada. Los usan las 64 pantallas de F7 y F8. |
| `apps/backoffice/src/app/nucleo/timeout.interceptor.ts` | 26 | sí | H12.S1.M3 · ninguna llamada espera para siempre. Una lectura que no responde deja la pantalla en «cargando» sin salida; |
| `apps/backoffice/src/app/nucleo/traza.interceptor.ts` | 11 | sí | `x-request-id` en cada petición: sin esto «me falló el reverso» no se puede seguir. |
| `apps/backoffice/src/app/nucleo/tutoriales/almacen-local.spec.ts` | 82 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de almacen-local |
| `apps/backoffice/src/app/nucleo/tutoriales/almacen-remoto.ts` | 49 | sí | El progreso en el servidor**, contra el contrato descrito en |
| `apps/backoffice/src/app/nucleo/tutoriales/avance.spec.ts` | 83 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de avance |
| `apps/backoffice/src/app/nucleo/tutoriales/motor-salida.spec.ts` | 98 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de motor-salida |
| `apps/backoffice/src/app/nucleo/tutoriales/motor.spec.ts` | 131 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de motor |
| `apps/backoffice/src/app/nucleo/tutoriales/objetivo.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de objetivo |
| `apps/backoffice/src/app/nucleo/tutoriales/preparar-motor.ts` | 84 | sí | El andamiaje que comparten las pruebas del motor y del anfitrión: dos pantallas de |
| `apps/backoffice/src/app/nucleo/tutoriales/registro.spec.ts` | 91 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de registro |
| `apps/backoffice/src/app/nucleo/tutoriales/validacion.spec.ts` | 85 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de validacion |
| `apps/backoffice/src/app/nucleo/tutoriales/vigilar-accion.spec.ts` | 117 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de vigilar-accion |
| `apps/backoffice/src/index.desarrollo.html` | 19 | sí | index.desarrollo.html |
| `apps/backoffice/src/index.html` | 17 | sí | index.html |
| `apps/backoffice/src/main.ts` | 5 | sí | main.ts |
| `apps/backoffice/src/styles.css` | 14 | sí | Estilos de styles |
| `apps/backoffice/src/tipos/clientes-angular.doble.d.ts` | 77 | sí | DOBLE TEMPORAL (regla 65) — no es el contrato real. |
| `apps/backoffice/tsconfig.app.json` | 10 | sí | Configuración: tsconfig.app.json |
| `apps/backoffice/tsconfig.json` | 51 | sí | Configuración: tsconfig.json |
| `apps/backoffice/tsconfig.spec.json` | 14 | sí | Configuración: tsconfig.spec.json |
| `apps/backoffice/tsconfig.spec.richard.json` | 20 | sí | Configuración: tsconfig.spec.richard.json |
| `apps/backoffice/vitest-base.config.ts` | 9 | sí | Configuración: vitest-base.config.ts |
| `packages/tutoriales/eslint.config.js` | 36 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: eslint.config.js |
| `packages/tutoriales/package.json` | 38 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: package.json |
| `packages/tutoriales/src/almacen-local.ts` | 104 | sí | Una clave por operador: dos personas en la misma máquina no comparten avance. |
| `packages/tutoriales/src/almacen.ts` | 23 | sí | El puerto del progreso.** El motor no sabe si esto termina en `localStorage` o en |
| `packages/tutoriales/src/anfitrion-de-tutorial.ts` | 153 | sí | El anfitrión**: lo único que el shell monta para que los tutoriales existan. |
| `packages/tutoriales/src/avance.ts` | 102 | sí | Las cuentas del avance, sin estado y sin IO.** Todo lo que el centro de ayuda |
| `packages/tutoriales/src/bitacora.ts` | 38 | sí | Lo que vale la pena saber de un recorrido, para mejorarlo después. |
| `packages/tutoriales/src/centro/filtros-de-ayuda.ts` | 46 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Buscador y filtros del centro de ayuda. No filtra: emite qué se eligió. |
| `packages/tutoriales/src/centro/pantalla-centro-de-ayuda.ts` | 163 | sí | El centro de tutoriales.** Lista lo que esta persona puede aprender, con su avance, |
| `packages/tutoriales/src/centro/resumen-de-avance.ts` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Cuánto llevás hecho, arriba de todo. Dice el número además de pintar la barra: una |
| `packages/tutoriales/src/centro/tarjeta-de-tutorial.ts` | 83 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un tutorial en la lista: qué enseña, cuánto lleva y qué botón corresponde. |
| `packages/tutoriales/src/centro/textos.ts` | 61 | sí | Cómo se presenta el centro en cada producto.** Es lo único del centro que cambia |
| `packages/tutoriales/src/centro/vista-de-tutoriales.ts` | 94 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Los filtros del centro de ayuda, tal como viven en la dirección. |
| `packages/tutoriales/src/encendido.ts` | 16 | sí | Lo único que el shell necesita saber de los tutoriales: si hay uno corriendo. |
| `packages/tutoriales/src/identidad.ts` | 34 | sí | Quién está aprendiendo.** Es lo único que el motor necesita saber de la sesión, y |
| `packages/tutoriales/src/lanzador-de-tutorial.ts` | 72 | sí | El tour contextual**: un botón en la cabecera que abre el tutorial de la pantalla |
| `packages/tutoriales/src/motor.ts` | 199 | sí | El motor: dónde estoy, cómo sigo y qué hago si el paso no se puede dar. |
| `packages/tutoriales/src/objetivo.ts` | 98 | sí | El atributo con el que la interfaz se deja señalar. Nunca una clase de CSS. |
| `packages/tutoriales/src/progreso.ts` | 50 | sí | La única copia viva del avance.** El motor escribe por acá y el centro de tutoriales |
| `packages/tutoriales/src/registro.ts` | 98 | sí | Trae un pedazo del catálogo. Es una función para que el `import()` sea perezoso. |
| `packages/tutoriales/src/textos-del-recorrido.ts` | 13 | sí | Los textos del recorrido en curso. Los del centro de ayuda viven en `rutas/ayuda/textos.ts`. |
| `packages/tutoriales/src/tipos.ts` | 114 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El contrato del motor de tutoriales.** Solo tipos: ni una importación de Angular, |
| `packages/tutoriales/src/validacion.spec.ts` | 99 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de validacion |
| `packages/tutoriales/src/validacion.ts` | 116 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El catálogo se valida, no se confía.** Un tutorial mal escrito no se descubre |
| `packages/tutoriales/src/vigilar-accion.ts` | 87 | sí | Todo lo que hace falta para mirar sin tocar. |
| `packages/tutoriales/tsconfig.json` | 23 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.json |
| `packages/ui/angular.json` | 56 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: angular.json |
| `packages/ui/eslint.config.js` | 36 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: eslint.config.js |
| `packages/ui/ng-package.json` | 7 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: ng-package.json |
| `packages/ui/package.json` | 50 | sí | Configuración: package.json |
| `packages/ui/pruebas/preparar.ts` | 4 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | preparar.ts |
| `packages/ui/pruebas/vitest-axe.d.ts` | 11 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | vitest-axe.d.ts |
| `packages/ui/src/acciones-de-fila/acciones-de-fila.ts` | 66 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Las acciones de una fila de tabla, siempre **ícono + nombre** (nunca un ícono solo): Ver, Editar y |
| `packages/ui/src/acordeon/acordeon.ts` | 30 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un bloque que se abre y cierra. `details`/`summary` nativos: teclado y lector sin código. |
| `packages/ui/src/alerta/alerta.ts` | 34 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Aviso en línea. Dice qué pasó y qué hacer; el ícono acompaña al color. `error` se anuncia como alerta. |
| `packages/ui/src/area-de-texto/area-de-texto.spec.ts` | 59 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de area-de-texto |
| `packages/ui/src/area-de-texto/area-de-texto.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Área de texto con la misma anatomía que `ap-campo`: etiqueta (con tooltip opcional), ayuda o |
| `packages/ui/src/avatar/avatar.spec.ts` | 8 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de avatar |
| `packages/ui/src/avatar/avatar.ts` | 31 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Iniciales sobre fondo de marca. Una imagen opcional, siempre con el nombre como alternativa. |
| `packages/ui/src/banda-de-proposito/banda-de-proposito.ts` | 20 | sí | «Para qué sirve»: toda pantalla del backoffice lleva arriba una frase de negocio. |
| `packages/ui/src/barra-de-filtros/barra-de-filtros.spec.ts` | 74 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de barra-de-filtros |
| `packages/ui/src/barra-de-filtros/barra-de-filtros.ts` | 80 | sí | Un selector de la barra: su nombre (la clave en la URL), su etiqueta, sus opciones y el valor vigente. |
| `packages/ui/src/barra-de-pasos/barra-de-pasos.ts` | 32 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | «Paso 3 de 8: Documento». Progreso por segmentos, el actual con relleno de marca y |
| `packages/ui/src/boton-copiar/boton-copiar.ts` | 59 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Copia un texto al portapapeles y lo dice («Copiado») en una región viva, además de cambiar el |
| `packages/ui/src/boton/boton.ts` | 79 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El único botón. **El color de acción (`--accent`) = acción** y hay uno por pantalla: el |
| `packages/ui/src/cabecera-de-pagina/cabecera-de-pagina.spec.ts` | 58 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cabecera-de-pagina |
| `packages/ui/src/cabecera-de-pagina/cabecera-de-pagina.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | La cabecera de toda pantalla del backoffice: título, descripción y un carril de |
| `packages/ui/src/cabecera-de-seccion/cabecera-de-seccion.ts` | 36 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El encabezado de un bloque dentro de una pantalla: `h2` con ícono en el acento y una línea de |
| `packages/ui/src/cajon-lateral/cajon-lateral.spec.ts` | 69 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cajon-lateral |
| `packages/ui/src/cajon-lateral/cajon-lateral.ts` | 92 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un panel que entra desde la derecha para una acción sobre una fila («Revisar columna X»), sin |
| `packages/ui/src/campo-busqueda/campo-busqueda.ts` | 35 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Búsqueda con lupa y borrado. `type=search` para que el teclado móvil ofrezca «Buscar». |
| `packages/ui/src/campo-contrasena/campo-contrasena.ts` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Contraseña con «ver». El botón dice lo que hará, y el campo nunca se autocompleta con la actual. |
| `packages/ui/src/campo-monto/campo-monto.spec.ts` | 15 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de campo-monto |
| `packages/ui/src/campo-monto/campo-monto.ts` | 30 | sí | Monto con prefijo «Bs», teclado decimal y coma como separador. Guarda lo que la |
| `packages/ui/src/campo-otp/campo-otp.ts` | 45 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Código de un solo uso en celdas. Un único `input` real (`one-time-code`, numérico): |
| `packages/ui/src/campo/campo.ts` | 82 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Campo de texto con etiqueta, ayuda y error. El error **sustituye** a la ayuda y se |
| `packages/ui/src/casilla/casilla.ts` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Casilla nativa vestida: el `input` real queda, así el teclado y el lector la ven. |
| `packages/ui/src/catalogo/catalogo-atomos.ts` | 86 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Los átomos, cada uno con sus variantes y estados. Es datos: no tiene lógica propia. |
| `packages/ui/src/catalogo/catalogo-backoffice.ts` | 106 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | La anatomía de una pantalla del backoffice (la del portal interno de Atlas, con las piezas de |
| `packages/ui/src/catalogo/catalogo-moleculas.ts` | 85 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Las moléculas con datos de muestra fijos. |
| `packages/ui/src/catalogo/catalogo-organismos.ts` | 84 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Los organismos con datos de muestra fijos. |
| `packages/ui/src/catalogo/catalogo.a11y.spec.ts` | 39 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de catalogo |
| `packages/ui/src/catalogo/catalogo.spec.ts` | 46 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de catalogo |
| `packages/ui/src/catalogo/catalogo.ts` | 32 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El catálogo vivo de `@aportaya/ui`. Lo monta `/catalogo` en el sitio (sin indexar) y |
| `packages/ui/src/catalogo/muestras.ts` | 66 | sí | Datos fijos del catálogo. «Hoy» es fijo para que las capturas no cambien con el calendario. |
| `packages/ui/src/catalogo/seccion.ts` | 19 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un bloque del catálogo: nombre de la pieza y una grilla con sus variantes. |
| `packages/ui/src/chip-elegible/chip-elegible.ts` | 63 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un chip que se elige. **Elegido = relleno de marca**, no un cambio sutil de fondo |
| `packages/ui/src/chip-estado/chip-estado.ts` | 33 | sí | Un estado con nombre. Un color de estado en una superficie chica lleva **relleno y |
| `packages/ui/src/chips-de-filtro/chips-de-filtro.spec.ts` | 22 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de chips-de-filtro |
| `packages/ui/src/chips-de-filtro/chips-de-filtro.ts` | 31 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Filtros de varias líneas, con ícono por chip y **sin «otros»**: cada tipo tiene su chip |
| `packages/ui/src/codigo-copiable/codigo-copiable.ts` | 19 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un identificador o código en monoespaciado con su botón de copiar al lado (esquemas, tablas, trazas, claves). |
| `packages/ui/src/codigo-qr/codigo-qr.ts` | 37 | sí | QR como SVG en línea (se ve en SSR y se imprime nítido). El contenido no se lee en |
| `packages/ui/src/cuenta-enmascarada/cuenta-enmascarada.spec.ts` | 8 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cuenta-enmascarada |
| `packages/ui/src/cuenta-enmascarada/cuenta-enmascarada.ts` | 21 | sí | Cuenta bancaria mostrando solo los últimos cuatro. El lector oye «terminada en 1234». |
| `packages/ui/src/desglose-de-cobro/desglose-de-cobro.ts` | 41 | sí | Las líneas de un cobro y el total. **El total viene del servidor**: acá no se suma |
| `packages/ui/src/dialogo-de-confirmacion/dialogo-de-confirmacion.spec.ts` | 61 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo-de-confirmacion |
| `packages/ui/src/dialogo-de-confirmacion/dialogo-de-confirmacion.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | La confirmación antes de una escritura, como en el portal de Atlas: ícono de advertencia, |
| `packages/ui/src/dialogo/dialogo.foco.spec.ts` | 45 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo.foco |
| `packages/ui/src/dialogo/dialogo.frontend.spec.ts` | 174 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo.frontend |
| `packages/ui/src/dialogo/dialogo.spec.ts` | 184 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo |
| `packages/ui/src/dialogo/dialogo.ts` | 138 | parcial: grep foco/Escape/confirm | Confirmación modal sobre `<dialog>` nativo: foco atrapado, Escape cierra, fondo |
| `packages/ui/src/dialogo/politica-de-descarte.ts` | 14 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Política de descarte única del organismo `Dialogo` (H3.S2, `entregables/contrato-dialogo.md`). |
| `packages/ui/src/error-de-envio/error-de-envio.spec.ts` | 42 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de error-de-envio |
| `packages/ui/src/error-de-envio/error-de-envio.ts` | 45 | sí | Lo que la capa de red entrega ya traducido (mismo contorno que `ErrorTraducido` de las apps). |
| `packages/ui/src/escalera-de-etapas/escalera-de-etapas.ts` | 44 | sí | Las etapas de cobranza en orden, con la actual marcada y las pasadas con su fecha. |
| `packages/ui/src/esqueleto/esqueleto.ts` | 38 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Silueta de carga. `lineas`: tantas líneas como `lineas`, con anchos que no se repiten (texto). |
| `packages/ui/src/estado-de-error/estado-de-error.spec.ts` | 74 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-error |
| `packages/ui/src/estado-de-error/estado-de-error.ts` | 49 | sí | Un error accionable, como en el portal de Atlas: ícono en loseta, título, qué pasó, el código |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.exhaustividad.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-pantalla.exhaustividad |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.frontend.spec.ts` | 201 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-pantalla.frontend |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.spec.ts` | 153 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-pantalla |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts` | 126 | sí | Lo que el interceptor de errores adjunta: el mensaje ya traducido y la traza. |
| `packages/ui/src/estado-vacio/estado-vacio.spec.ts` | 42 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-vacio |
| `packages/ui/src/estado-vacio/estado-vacio.ts` | 56 | sí | Vacío con **por qué** y **qué hacer**. Vacío por filtro ofrece quitar el filtro; vacío |
| `packages/ui/src/estilos.css` | 75 | sí | Estilos de estilos |
| `packages/ui/src/estrellas/estrellas.ts` | 33 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | De una a cinco. Solo lectura muestra; con `editable` es un `radiogroup` con teclado. |
| `packages/ui/src/exportar-listado/exportar-listado.ts` | 49 | sí | «Descargar CSV» y «Descargar JSON» de las filas **filtradas** de un listado (todas las páginas, no solo la |
| `packages/ui/src/exportar-listado/exportar.spec.ts` | 60 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de exportar |
| `packages/ui/src/exportar-listado/exportar.ts` | 79 | sí | Exportación en el navegador de **lo que el listado muestra ya filtrado**: mismas filas, mismas columnas y los |
| `packages/ui/src/fecha/fecha.spec.ts` | 25 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de fecha |
| `packages/ui/src/fecha/fecha.ts` | 40 | sí | Fecha del contrato (ISO) en hora de Bolivia, **con la zona visible**: un plazo se |
| `packages/ui/src/fila-de-cotejo/fila-de-cotejo.spec.ts` | 11 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de fila-de-cotejo |
| `packages/ui/src/fila-de-cotejo/fila-de-cotejo.ts` | 33 | sí | Lo que dijo la persona contra lo que dice el documento. Coincide o no, con palabra y color. |
| `packages/ui/src/fila-de-movimiento/fila-de-movimiento.ts` | 43 | sí | Una línea del extracto: ícono tipado, concepto, hora, monto con signo y saldo corrido. |
| `packages/ui/src/foco-de-tutorial/foco-de-tutorial.ts` | 61 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El recuadro del elemento señalado, en coordenadas de la ventana. |
| `packages/ui/src/girador/girador.ts` | 19 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Actividad indeterminada. Siempre se anuncia (`role=status`); respeta *reduced motion*. |
| `packages/ui/src/globo-de-tutorial/globo-de-tutorial.ts` | 129 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Lo que se supone que mide el globo **antes de haberlo pintado una vez**. Se usa para |
| `packages/ui/src/globo-de-tutorial/ubicacion.spec.ts` | 66 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de ubicacion |
| `packages/ui/src/globo-de-tutorial/ubicacion.ts` | 92 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Aire entre el elemento y el globo, y entre el globo y el borde de la ventana. |
| `packages/ui/src/grupo-radio/grupo-radio.ts` | 39 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Una entre varias, con `fieldset`/`legend`. El elegido lleva relleno de marca en el aro. |
| `packages/ui/src/icono/icono.ts` | 132 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Trazos de 24×24 (stroke), uno por nombre. El ícono dice **qué pasó**, nunca si sube o baja. |
| `packages/ui/src/interruptor/interruptor.ts` | 27 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Sí/no inmediato (`role=switch`). Activo en verde **ok**: es un estado, no una acción. |
| `packages/ui/src/item-pasanaku/item-pasanaku.ts` | 44 | sí | Un grupo en una lista: nombre, aporte, avance del ciclo y el estado de mi cuota. |
| `packages/ui/src/lista-de-movimientos/lista-de-movimientos.spec.ts` | 22 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de lista-de-movimientos |
| `packages/ui/src/lista-de-movimientos/lista-de-movimientos.ts` | 68 | sí | El extracto **agrupado por día** (regla 2), con el neto del día y el saldo al cierre. |
| `packages/ui/src/lista-de-opciones/lista-de-opciones.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de lista-de-opciones |
| `packages/ui/src/lista-de-opciones/lista-de-opciones.ts` | 64 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Lista desplegable nativa (teclado y lector de pantalla de serie) con la anatomía de `ap-campo`: |
| `packages/ui/src/lista-de-requisitos/lista-de-requisitos.ts` | 44 | sí | Qué falta para habilitarse, con el avance arriba y una acción por requisito pendiente. |
| `packages/ui/src/listado/crear-listado.spec.ts` | 125 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de crear-listado |
| `packages/ui/src/listado/crear-listado.ts` | 148 | parcial: grep + l.40-48 | Un criterio de filtro del listado. Sin `opciones`, se arman con los valores distintos que traen las filas |
| `packages/ui/src/listado/listado.spec.ts` | 117 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de listado |
| `packages/ui/src/listado/listado.ts` | 142 | parcial: l.1-140 + grep | La columna que agrega `#acciones`: no choca con un campo real porque ningún contrato usa este nombre. |
| `packages/ui/src/listado/singular.spec.ts` | 27 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de singular |
| `packages/ui/src/listado/singular.ts` | 24 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El singular del nombre de registro de un listado, para no escribir «1 anunciantes» (2026-10-27). La mayoría solo |
| `packages/ui/src/medidor-de-rango/medidor-de-rango.ts` | 45 | sí | Dónde cae un valor dentro de un rango con zonas (bajo/medio/alto). Un `meter` nativo |
| `packages/ui/src/migas/migas.spec.ts` | 44 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de migas |
| `packages/ui/src/migas/migas.ts` | 55 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Migas de pan: «Inicio › Sección › Vista». Las migas con `ruta` son enlaces; la última es la |
| `packages/ui/src/monto/formatear.ts` | 2 | sí | Reexporta la referencia: el formateo vive UNA vez, en @aportaya/tokens/dinero. |
| `packages/ui/src/monto/monto.spec.ts` | 34 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de monto |
| `packages/ui/src/monto/monto.ts` | 25 | sí | El único lugar de la web donde se dibuja dinero: `tabular-nums`, prefijo `Bs`, coma |
| `packages/ui/src/nota-de-vista/nota-de-vista.spec.ts` | 63 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de nota-de-vista |
| `packages/ui/src/nota-de-vista/nota-de-vista.ts` | 102 | sí | «Notas de la vista»: qué muestra esta pantalla y cómo se usa, en una línea plegada que solo |
| `packages/ui/src/paginacion/paginacion.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de paginacion |
| `packages/ui/src/paginacion/paginacion.ts` | 78 | sí | «Página 2 de 7 · 134 registros», filas por página, ir a una página y Anterior/Siguiente, como |
| `packages/ui/src/panel-de-factores/panel-de-factores.ts` | 41 | sí | Por qué el veredicto fue el que fue, en palabras. Cada factor dice si sumó, restó o |
| `packages/ui/src/pestanas/pestanas.ts` | 50 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Pestañas WAI-ARIA con flechas, Inicio y Fin. El panel lo dibuja quien las usa, con `aria-labelledby`. |
| `packages/ui/src/posiciones-de-saldo/posiciones-de-saldo.a11y.spec.ts` | 27 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de posiciones-de-saldo |
| `packages/ui/src/posiciones-de-saldo/posiciones-de-saldo.spec.ts` | 85 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de posiciones-de-saldo |
| `packages/ui/src/posiciones-de-saldo/posiciones-de-saldo.ts` | 112 | sí | Las posiciones de una billetera ya separadas (las calcula `separarPosiciones` de |
| `packages/ui/src/progreso/progreso.ts` | 32 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Avance determinado de 0 a 1. Se lee como barra de progreso con su valor en porcentaje. |
| `packages/ui/src/public-api.ts` | 4 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Sin barril a propósito (conflicto nº 2 de planes/16): cada componente se importa por su ruta, |
| `packages/ui/src/punto/punto.ts` | 17 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Marca de «hay algo nuevo». Sin texto no dice nada al lector: por eso `etiqueta` es obligatoria. |
| `packages/ui/src/reloj-de-plazo/reloj-de-plazo.spec.ts` | 14 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de reloj-de-plazo |
| `packages/ui/src/reloj-de-plazo/reloj-de-plazo.ts` | 57 | sí | Un plazo con fecha, restante y **de qué norma sale**. El vencimiento viene guardado del |
| `packages/ui/src/resumen-de-periodo/resumen-de-periodo.ts` | 34 | sí | Entradas, salidas y neto de un período. Tres cifras, todas por `ap-monto`. |
| `packages/ui/src/riel-de-turnos/riel-de-turnos.ts` | 44 | sí | El orden del grupo en una tira: entregados, el actual con relleno de marca, y «vos» marcado. |
| `packages/ui/src/seccion-clave-valor/seccion-clave-valor.spec.ts` | 62 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de seccion-clave-valor |
| `packages/ui/src/seccion-clave-valor/seccion-clave-valor.ts` | 95 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Una fila: rótulo y valor. `mono` lo muestra como código copiable; `tono` colorea el valor según lo que significa; |
| `packages/ui/src/seccion-de-expediente/seccion-de-expediente.ts` | 42 | sí | Un bloque del expediente: título, quién y cuándo, y una lista de datos con nombre. |
| `packages/ui/src/selector-segmentado/selector-segmentado.spec.ts` | 25 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de selector-segmentado |
| `packages/ui/src/selector-segmentado/selector-segmentado.ts` | 39 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Dos a cuatro vistas equivalentes (lista/calendario, QR/código). Es un `radiogroup` |
| `packages/ui/src/tabla-de-datos/tabla-de-datos-tipos.ts` | 30 | sí | `fijaDerecha`: la columna (de acciones) queda pegada al borde derecho al desplazar; `mono`: identificadores y códigos. |
| `packages/ui/src/tabla-de-datos/tabla-de-datos.css` | 65 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Estilos de tabla-de-datos |
| `packages/ui/src/tabla-de-datos/tabla-de-datos.spec.ts` | 242 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tabla-de-datos |
| `packages/ui/src/tabla-de-datos/tabla-de-datos.ts` | 182 | parcial: l.1-40 y 100-182 | El organismo canónico de tabla de datos (único; ver `tabla-de-datos-virtualizada` en |
| `packages/ui/src/tarjeta-de-oferta/tarjeta-de-oferta.ts` | 45 | sí | Alguien ofrece cambiar de turno. Se ve quién, qué turno da y cuál pide, y **cuánto |
| `packages/ui/src/tarjeta-de-solicitud/tarjeta-de-solicitud.spec.ts` | 28 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tarjeta-de-solicitud |
| `packages/ui/src/tarjeta-de-solicitud/tarjeta-de-solicitud.ts` | 57 | sí | Una persona pide entrar a un grupo. Aceptar es un toque; **rechazar exige motivo**: |
| `packages/ui/src/tarjeta-kpi/tarjeta-kpi.ts` | 53 | sí | Un número con nombre, como la tarjeta de métrica del portal de Atlas: ícono en loseta con |
| `packages/ui/src/tarjeta-saldo/tarjeta-saldo.ts` | 39 | sí | Encabezado verde con el saldo. «Recargar» es el único naranja; «Retirar» se pinta |
| `packages/ui/src/tarjeta/tarjeta.ts` | 22 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Superficie con borde. Con `titulo` es una `section` con encabezado; sin él, un `div`. |
| `packages/ui/src/tipo-de-movimiento/tipo-de-movimiento.ts` | 22 | sí | Los tipos que el extracto distingue. **El ícono dice qué pasó** (regla 1), el signo lo dice el monto. |
| `packages/ui/src/toast/toast.ts` | 49 | sí | Cola de avisos efímeros: uno a la vez, el siguiente espera. Un error no se va solo. |
| `packages/ui/src/tono/tono.ts` | 31 | sí | Los tonos semánticos que un componente puede pedir. Cada uno resuelve a un par |
| `packages/ui/src/tooltip/tooltip.spec.ts` | 47 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tooltip |
| `packages/ui/src/tooltip/tooltip.ts` | 55 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Una ayuda corta que aparece al pasar el mouse o al enfocar lo que envuelve, y se va con |
| `packages/ui/src/vale/vale.ts` | 58 | sí | El vale de un partner: muesca de ticket, QR **rotativo** (el contenido cambia y la |
| `packages/ui/src/verificador-de-sorteo/verificador-de-sorteo.ts` | 53 | sí | El sorteo se puede reproducir: semilla comprometida, semilla revelada, orden |
| `packages/ui/tsconfig.json` | 44 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.json |
| `packages/ui/tsconfig.lib.json` | 18 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.lib.json |
| `packages/ui/tsconfig.lib.prod.json` | 9 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.lib.prod.json |
| `packages/ui/tsconfig.spec.json` | 14 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.spec.json |
| `packages/ui/vitest-base.config.ts` | 16 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: vitest-base.config.ts |
| `apps/web/.editorconfig` | 17 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: .editorconfig |
| `apps/web/.gitignore` | 9 | sí | Configuración: .gitignore |
| `apps/web/.prettierrc` | 12 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: .prettierrc |
| `apps/web/README.md` | 59 | sí | Doc: Web |
| `apps/web/angular.json` | 114 | sí | Configuración: angular.json |
| `apps/web/contenido/legal/contrato-de-adhesion.md` | 18 | sí | Texto público: Contrato de adhesión |
| `apps/web/contenido/legal/estado-regulatorio.md` | 24 | sí | Texto público: Estado regulatorio |
| `apps/web/contenido/paginas/como-funciona.md` | 24 | sí | Texto público: Cómo funciona AportaYa |
| `apps/web/contenido/paginas/descargar.md` | 22 | sí | Texto público: Usar AportaYa |
| `apps/web/contenido/paginas/inicio.md` | 16 | sí | Texto público: El pasanaku de siempre, sin el cuaderno |
| `apps/web/contenido/paginas/preguntas.md` | 36 | sí | Texto público: Preguntas frecuentes |
| `apps/web/contenido/paginas/privacidad.md` | 26 | sí | Texto público: Privacidad y tratamiento de datos |
| `apps/web/contenido/paginas/reclamos.md` | 26 | sí | Texto público: Reclamos |
| `apps/web/contenido/paginas/seguridad.md` | 28 | sí | Texto público: Seguridad y custodia de tu dinero |
| `apps/web/contenido/paginas/transparencia.md` | 27 | sí | Texto público: Transparencia: cómo se sella y se verifica |
| `apps/web/docker/Dockerfile.web` | 33 | sí | Configuración: Dockerfile.web |
| `apps/web/eslint.config.js` | 36 | sí | Configuración: eslint.config.js |
| `apps/web/lighthouserc.cjs` | 32 | sí | Configuración: lighthouserc.cjs |
| `apps/web/package.json` | 57 | sí | Configuración: package.json |
| `apps/web/playwright.config.ts` | 45 | sí | Configuración: playwright.config.ts |
| `apps/web/playwright.dev.config.ts` | 19 | sí | Configuración: playwright.dev.config.ts |
| `apps/web/pruebas/e2e/accesibilidad.e2e.ts` | 39 | sí | E2E Playwright: accesibilidad |
| `apps/web/pruebas/e2e/catalogo.spec.ts` | 59 | sí | Pruebas unitarias/estructura de catalogo |
| `apps/web/pruebas/e2e/paginas.spec.ts` | 13 | sí | Pruebas unitarias/estructura de paginas |
| `apps/web/pruebas/e2e/web-seo.e2e.ts` | 66 | sí | E2E Playwright: web-seo |
| `apps/web/pruebas/e2e/web-verificacion.e2e.ts` | 72 | sí | E2E Playwright: web-verificacion |
| `apps/web/pruebas/preparar.ts` | 4 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | preparar.ts |
| `apps/web/pruebas/vitest-axe.d.ts` | 11 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | vitest-axe.d.ts |
| `apps/web/public/fuentes/OFL-Bricolage.txt` | 93 | no: licencia OFL estándar (texto legal de terceros) | OFL-Bricolage.txt |
| `apps/web/public/fuentes/OFL-Instrument.txt` | 93 | no: licencia OFL estándar (texto legal de terceros) | OFL-Instrument.txt |
| `apps/web/public/legal/estado-regulatorio.md` | 21 | sí | Texto público: Estado regulatorio |
| `apps/web/scripts/contenido.mjs` | 65 | sí | /*.md ANTES de `ng build`: produce src/generado/contenido.json (lo que |
| `apps/web/src/app/app.config.server.ts` | 10 | sí | Configuración: app.config.server.ts |
| `apps/web/src/app/app.config.ts` | 24 | sí | Configuración: app.config.ts |
| `apps/web/src/app/app.routes.server.ts` | 40 | sí | Estático por omisión** (ADR-041). Solo las rutas que dependen de un servicio en |
| `apps/web/src/app/app.routes.ts` | 185 | sí | `data.seo` es de `ServicioMeta` (F10, `apps/web/src/app/seo/`). Cada página le agrega |
| `apps/web/src/app/app.ts` | 35 | sí | El sitio: la barra y el pie de la landing alrededor de cada página. |
| `apps/web/src/app/enchufe-de-paginas.spec.ts` | 31 | sí | Pruebas unitarias/estructura de enchufe-de-paginas |
| `apps/web/src/app/geo/guia-de-redaccion.md` | 56 | sí | Doc: Guía de redacción para contenido legible por LLM (F11.4) |
| `apps/web/src/app/geo/llms.d.mts` | 17 | sí | llms.d.mts |
| `apps/web/src/app/geo/llms.mjs` | 85 | sí | /*.md. Plano y sin dependencias |
| `apps/web/src/app/geo/llms.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de llms |
| `apps/web/src/app/geo/llms.ts` | 2 | sí | Puente tipado sobre llms.mjs, que es lo que corre el script de build (Node, sin TS). |
| `apps/web/src/app/geo/robots.d.mts` | 4 | sí | robots.d.mts |
| `apps/web/src/app/geo/robots.mjs` | 16 | sí | La politica de rastreadores es decision de negocio (ADR-042) y por eso tiene prueba. |
| `apps/web/src/app/geo/robots.spec.ts` | 26 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de robots |
| `apps/web/src/app/geo/robots.ts` | 2 | sí | Puente tipado sobre robots.mjs, que es lo que corre el script de build (Node, sin TS). |
| `apps/web/src/app/layout/cabecera-de-pagina.ts` | 26 | sí | La banda de título de las páginas interiores: la misma voz visual que el hero de la |
| `apps/web/src/app/layout/cabecera.ts` | 42 | sí | La barra del sitio: la de la landing, con rutas reales en vez de anclas de una sola página. |
| `apps/web/src/app/layout/enlace-a-la-app.ts` | 35 | sí | «Abrir AportaYa en el navegador». La dirección la pone el servidor en |
| `apps/web/src/app/layout/icono.ts` | 44 | sí | Las formas se dibujan como elementos SVG de la plantilla, NO con `innerHTML`. El DOM |
| `apps/web/src/app/layout/marca.ts` | 29 | sí | El símbolo y la palabra, como en la landing y en la app. El aguayo naranja va por clase |
| `apps/web/src/app/layout/pie.ts` | 58 | sí | El pie de la landing, con las páginas legales que el sitio sí tiene. |
| `apps/web/src/app/layout/trazos.ts` | 37 | sí | Los trazos de los íconos del sitio, copiados de la landing (24×24, trazo redondeado). |
| `apps/web/src/app/nucleo/configuracion-invalida.a11y.spec.ts` | 24 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de configuracion-invalida |
| `apps/web/src/app/nucleo/configuracion-invalida.ts` | 30 | sí | Configuración: configuracion-invalida.ts |
| `apps/web/src/app/nucleo/errores.interceptor.ts` | 23 | sí | Todo error sale de acá ya traducido: la pantalla nunca ve el mensaje del backend ni |
| `apps/web/src/app/nucleo/errores.ts` | 28 | sí | Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso» |
| `apps/web/src/app/nucleo/gateway.spec.ts` | 73 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de gateway |
| `apps/web/src/app/nucleo/gateway.ts` | 74 | sí | Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. |
| `apps/web/src/app/nucleo/timeout.interceptor.spec.ts` | 36 | sí | Pruebas unitarias/estructura de timeout.interceptor |
| `apps/web/src/app/nucleo/timeout.interceptor.ts` | 20 | sí | H12.S1.M3 · el sitio solo LEE (verificadores y datos públicos): una lectura que no responde dejaba la pantalla en «carga… |
| `apps/web/src/app/nucleo/traza.interceptor.ts` | 11 | sí | `x-request-id` en cada petición: sin esto «me falló el reverso» no se puede seguir. |
| `apps/web/src/app/paginas/catalogo/catalogo.ts` | 30 | sí | El catálogo vivo de `@aportaya/ui`, prerenderizado y **sin indexar** (robots + meta): |
| `apps/web/src/app/paginas/como-funciona/como-funciona.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/contrato-de-adhesion/contrato-de-adhesion.ts` | 27 | sí | Contenido estático hoy — ver la nota técnica dentro del Markdown sobre por qué |
| `apps/web/src/app/paginas/descargar/descargar.ts` | 25 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/inicio/inicio.ts` | 27 | sí | El inicio del sitio: la estética y el recorrido de la landing, sobre los tokens y las |
| `apps/web/src/app/paginas/inicio/secciones/cierre.ts` | 74 | sí | Lo que hay debajo, las preguntas y el llamado final. |
| `apps/web/src/app/paginas/inicio/secciones/confianza.ts` | 77 | sí | Cuando algo sale mal, y la seguridad: por qué se puede confiar la plata. |
| `apps/web/src/app/paginas/inicio/secciones/hero.ts` | 58 | sí | hero.ts |
| `apps/web/src/app/paginas/inicio/secciones/producto.ts` | 78 | sí | Cómo funciona y la billetera: los dos bloques que explican el producto. |
| `apps/web/src/app/paginas/inicio/secciones/telefono.ts` | 75 | sí | La pantalla de inicio de la billetera, dibujada en HTML: la misma que muestra la app. |
| `apps/web/src/app/paginas/inicio/secciones/transparencia.ts` | 68 | sí | La transparencia verificable: qué se promete y cómo se ve un historial sellado. |
| `apps/web/src/app/paginas/legal-estado-regulatorio/legal-estado-regulatorio.ts` | 23 | sí | Contenido estático hoy — ver la nota técnica dentro del Markdown sobre el hueco de contrato. |
| `apps/web/src/app/paginas/plazos/plazos.ts` | 26 | sí | Ruta en servidor (consulta un servicio en vivo); el verificador se hidrata al verse. |
| `apps/web/src/app/paginas/preguntas/preguntas.a11y.spec.ts` | 18 | parcial: nombres de tests + grep | Prueba axe de accesibilidad de preguntas |
| `apps/web/src/app/paginas/preguntas/preguntas.spec.ts` | 24 | sí | Pruebas unitarias/estructura de preguntas |
| `apps/web/src/app/paginas/preguntas/preguntas.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/privacidad/privacidad.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/reclamos/reclamos.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/seguridad/seguridad.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/tarifas/tarifas.ts` | 41 | sí | CU-30/CU-34 — Tarifario vigente. `RenderMode.Server` (planes/14 F9.1): la página |
| `apps/web/src/app/paginas/transparencia/transparencia.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/verificar/verificar.ts` | 42 | sí | CU-75 — `GET /verificar/:codigo`, ruta pública sin sesión. **No indexable**: es |
| `apps/web/src/app/rutas-de-servidor.spec.ts` | 46 | sí | Pruebas unitarias/estructura de rutas-de-servidor |
| `apps/web/src/app/seo/json-ld.spec.ts` | 40 | sí | Pruebas unitarias/estructura de json-ld |
| `apps/web/src/app/seo/json-ld.ts` | 99 | sí | Constructores de JSON-LD (schema.org) para el sitio público. |
| `apps/web/src/app/seo/servicio-meta.spec.ts` | 13 | sí | Pruebas unitarias/estructura de servicio-meta |
| `apps/web/src/app/seo/servicio-meta.ts` | 126 | sí | El mismo patrón que `scripts/contenido.mjs` usa para escribir el espejo: `/` → `/index.md`, |
| `apps/web/src/app/seo/sitemap.d.mts` | 3 | sí | sitemap.d.mts |
| `apps/web/src/app/seo/sitemap.mjs` | 23 | sí | true si la ruta nunca puede entrar al sitemap, sin importar lo que diga `indexable`. |
| `apps/web/src/app/seo/sitemap.spec.ts` | 50 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de sitemap |
| `apps/web/src/app/seo/sitemap.ts` | 2 | sí | Puente tipado sobre sitemap.mjs, que es lo que corre scripts/contenido.mjs (Node, sin TS). |
| `apps/web/src/app/sin-acciones-ajenas.spec.ts` | 40 | sí | Pruebas unitarias/estructura de sin-acciones-ajenas |
| `apps/web/src/app/tutoriales/catalogo/catalogo.spec.ts` | 55 | sí | Pruebas unitarias/estructura de catalogo |
| `apps/web/src/app/tutoriales/catalogo/catalogo.ts` | 17 | sí | El catálogo del sitio público.** Agregar un tutorial es escribir su constante en el |
| `apps/web/src/app/tutoriales/catalogo/centro.ts` | 40 | sí | Cómo se usa el propio centro de tutoriales del sitio. |
| `apps/web/src/app/tutoriales/catalogo/herramientas.ts` | 105 | sí | El cotizador: lo que AportaYa cobra, antes de operar y sin cuenta. |
| `apps/web/src/app/tutoriales/catalogo/primeros-pasos.ts` | 77 | sí | Los tutoriales del **sitio público**: los hace alguien sin cuenta, que todavía está |
| `apps/web/src/app/tutoriales/guia-del-sitio.a11y.spec.ts` | 63 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de guia-del-sitio |
| `apps/web/src/app/tutoriales/guia-del-sitio.spec.ts` | 99 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de guia-del-sitio |
| `apps/web/src/app/tutoriales/proveer.ts` | 52 | sí | Las rutas del sitio contra las que se valida que ningún tutorial señale una página |
| `apps/web/src/app/tutoriales/tutoriales.routes.ts` | 13 | sí | La guía interactiva del sitio. Es una página más —estática, indexable— y el recorrido |
| `apps/web/src/index.html` | 15 | sí | index.html |
| `apps/web/src/main.server.ts` | 7 | sí | main.server.ts |
| `apps/web/src/main.ts` | 5 | sí | main.ts |
| `apps/web/src/server.ts` | 144 | sí | Example Express Rest API endpoints can be defined here. |
| `apps/web/src/sitio/fuentes.css` | 13 | sí | Estilos de fuentes |
| `apps/web/src/sitio/sitio.css` | 706 | parcial: cabecera, l.60-70 y 615-706 + grep de hex/rgba/motion/outline | Estilos de sitio |
| `apps/web/src/sitio/tokens-sitio.css` | 68 | sí | Estilos de tokens-sitio |
| `apps/web/src/styles.css` | 5 | sí | Estilos de styles |
| `apps/web/tsconfig.app.json` | 10 | sí | Configuración: tsconfig.app.json |
| `apps/web/tsconfig.json` | 52 | sí | Configuración: tsconfig.json |
| `apps/web/tsconfig.spec.json` | 14 | sí | Configuración: tsconfig.spec.json |
