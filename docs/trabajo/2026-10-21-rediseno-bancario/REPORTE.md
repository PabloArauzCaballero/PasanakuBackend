> **AVANCE: 41 / 126 — 32,5 %.**

# Reporte — Rediseño bancario de la app móvil (corte intermedio)

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/rediseno-bancario` (base `dev`)
- Peldaño de evidencia alcanzado (regla 30): **TESTED** en paquete y app; **prueba visual** de Inicio,
  Grupos, Mi estado y Perfil con fuentes reales (`evidencia/intermedio-h4`, `intermedio-h6`).
  **No VERIFIED**: no se recorrió ninguna pantalla nueva en runtime contra backend con consola y red.
- Este reporte es de un corte intermedio: el trabajo sigue. El plan es el estado durable.

## Lo que pidió la persona usuaria y cómo se entiende hoy
Pidió que la app se vea como una app bancaria (referencia: Meru y hermanas), con capturas de todo el
trabajo, motion design, Apple design y las mejores skills de animación, más skills de diseño
mobile UX/UI. Después marcó que **varias pantallas «seguían igual»**. La crítica
`evidencia/roast-pantallas-hoy.md` le da la razón: hasta H4 cambié color y piezas, no la composición.
Desde ahí se reordenó el plan (H6 antes de terminar H4/H5), pantalla por pantalla.

## Completado
Solo lo que tiene CA y DoD con salida pegada. Detalle y comandos por hito en `evidencia/*.txt`.

| ID | Qué se logró | Evidencia | Resultado |
|---|---|---|---|
| H0.S1.M1 | Instalar Flutter 3.44.8 en `/opt/flutter` | ver `evidencia/` de su hito | PASS |
| H0.S1.M2 | Generar los tokens | ver `evidencia/` de su hito | PASS |
| H0.S1.M3 | Resolver dependencias de la app y el paquete | ver `evidencia/` de su hito | PASS |
| H0.S1.M4 | Fijar la línea base de calidad | ver `evidencia/` de su hito | PASS |
| H0.S2.M1 | Cargador de fuentes reales para tests (`test/capturas/fuentes.dart`) | ver `evidencia/` de su hito | PASS |
| H0.S2.M2 | Matriz de viewport × tema (`test/capturas/matriz.dart`) | ver `evidencia/` de su hito | PASS |
| H0.S2.M3 | Datos sintéticos declarados por pantalla (`test/capturas/datos_sinteticos.dart`) | ver `evidencia/` de su hito | PASS |
| H0.S2.M4 | Inventario de pantallas del arnés (lista cerrada contra las rutas) | ver `evidencia/` de su hito | PASS |
| H0.S2.M5 | Recorrido web con Playwright (`herramientas/capturas-web/`) | ver `evidencia/` de su hito | PASS |
| H0.S2.M6 | Grabador de movimiento (video de Playwright más extracción de cuadros con ffmpeg) | ver `evidencia/` de su hito | PASS |
| H0.S2.M7 | Generador de galería `scripts/galeria_capturas.mjs` | ver `evidencia/` de su hito | PASS |
| H0.S3.M1 | Entrada: portada, tour (4), ingreso, MFA | ver `evidencia/` de su hito | PASS |
| H0.S3.M2 | Alta y KYC: registro (6 pasos), pautas, cámara, prueba de vida, subida, estado, contrato,  | ver `evidencia/` de su hito | PASS |
| H0.S3.M3 | Billetera: inicio, recargar, retirar, transferir, extracto | ver `evidencia/` de su hito | PASS |
| H0.S3.M4 | Grupos: mi estado, puntaje, nuevo, pedir cupo, invitar (+número), solicitudes, retiro, tra | ver `evidencia/` de su hito | PASS |
| H0.S3.M5 | Perfil: perfil, verificación, dispositivos, verificación profunda, contraseña, baja | ver `evidencia/` de su hito | PASS |
| H0.S3.M6 | Notificaciones, ayuda, candado, splash, pantallas de bloqueo y configuración inválida | ver `evidencia/` de su hito | PASS |
| H0.S3.M7 | Componentes: el catálogo Widgetbook completo por nivel | ver `evidencia/` de su hito | PASS |
| H0.S3.M8 | Revisión del «antes» con la rúbrica `ui-quality-review`, con la lista de problemas por pan | ver `evidencia/` de su hito | PASS |
| H1.S1.M1 | Contexto de producto (`.agents/product-marketing.md`, skill `product-marketing`): audienci | ver `evidencia/` de su hito | PASS |
| H1.S1.M2 | Guía de voz y microcopy (`copywriting` + `copy-editing` + `ux-writing-microcopy`): glosari | ver `evidencia/` de su hito | PASS |
| H1.S1.M3 | Mapa de psicología aplicada (`marketing-psychology`): confianza, transparencia del pozo, p | ver `evidencia/` de su hito | PASS |
| H1.S2.M1 | Lámina de dirección de arte: paleta, tipos, sombras, radios, íconos e Inicio en alta fidel | ver `evidencia/` de su hito | PASS |
| H1.S2.M2 | ADR-053 «Rediseño bancario»: decisiones de paleta, tipografía, íconos, navegación y efecto | ver `evidencia/` de su hito | PASS |
| H1.S2.M3 | Confirmación de A1–A7 con la persona | ver `evidencia/` de su hito | PASS |
| H2.S1.M2 | Roles nuevos: `heroInicio`, `heroFin`, `heroTexto`, `heroTexto2`, `halo`, `vidrio`, `vidri | ver `evidencia/` de su hito | PASS |
| H2.S1.M3 | Rol de acción según A2 | ver `evidencia/` de su hito | PASS |
| H2.S2.M1 | Elevación en 4 niveles con sombra en 2 capas (`e1` fila, `e2` tarjeta, `e3` héroe, `e4` ho | ver `evidencia/` de su hito | PASS |
| H2.S2.M2 | Radios: agregar `r2xl` 28 y la regla de anidado (interior = exterior − padding) | ver `evidencia/` de su hito | PASS |
| H2.S3.M1 | Tipografía: `Tipo.saldo` (Bricolage 44/700, tabular, −2 % de tracking), `tituloPantalla` ( | ver `evidencia/` de su hito | PASS |
| H2.S4.M1 | Movimiento: tokens de resorte (`resorteSuave`, `resorteRapido`, `resorteRebote`) y escalon | ver `evidencia/` de su hito | PASS |
| H2.S5.M1 | Efecto sobre la web (A3): capturar la web antes y después del cambio de tokens | ver `evidencia/` de su hito | PASS |
| H3.S1.M1 | `CardTheme`: sin borde en claro, elevación `e2`, radio `r2xl` | ver `evidencia/` de su hito | PASS |
| H3.S1.M2 | `AppBarTheme` → estilo cabecera grande, transparente y con desenfoque al desplazar | ver `evidencia/` de su hito | PASS |
| H4.S1.M1 | `Boton`: altura 56, radio píldora, variantes primario/secundario/fantasma/peligro/urgencia | ver `evidencia/` de su hito | PASS |
| H4.S1.M2 | `BotonIcono` circular de 44/56 con contenedor tintado | ver `evidencia/` de su hito | PASS |
| H4.S2.M4 | `FilaDeMovimiento`: avatar por tipo, monto con signo y color + ícono, y subtítulo con hora | ver `evidencia/` de su hito | PASS |
| H4.S3.M1 | `EncabezadoDeSaldo` vuelve a usar la tarjeta héroe | ver `evidencia/` de su hito | PASS |
| H5.S3.M4 | **Agregada en H0.S1.M4 (bug de la línea base):** el candado se resincroniza cuando «reduci | ver `evidencia/` de su hito | PASS |
| H6.S4.M7 | Shell: barra flotante y, si A6 = sí, pestaña central QR | ver `evidencia/` de su hito | PASS |
| H6.S4.M8 | **Agregada en H0.S2.M5 (bug real):** desde otra rama, la pestaña «Movimientos» llama a `go | ver `evidencia/` de su hito | PASS |

Además, fuera del conteo del plan: skills instaladas y leídas (marketing y redacción de
coreyhaines31, `mobile-app-ui-design`, `fintech-ux`, `roast-my-ui`), con procedencia en
`.claude/skills/README.md`; ADR-053; galería y lámina publicadas como artefactos.

## A medias
### H3.S1.M3 — `BottomSheetTheme`, `DialogTheme` y `SnackBarTheme` en el lenguaje nuevo
- (código y tests en verde; faltan las capturas de hoja y diálogo, que salen del catálogo en H4.S3.M8)

### H4.S1.M9 — Iconografía: agregar `lucide_icons_flutter: 3.1.20` (con ADR-053), mapa único `I
- (anda: dependencia fijada, `IconosDeMarca` con 22 sustantivos, fuente cargada en las capturas; falta: barrido de los 91 `Icons.` que quedan en `apps/movil/lib` y la lista de excepciones)

### H4.S2.M2 — `TarjetaSaldo` → tarjeta héroe (degradado, ruido, ojo, chips y brillo al tocar)
- (anda: la tarjeta héroe con degradado, brillo radial, ojo que oculta y chips, en `EncabezadoDeSaldo`; falta: ruido de textura, brillo al tocar con su MP4 y resolver A9 con `TarjetaSaldo`)

### H4.S2.M3 — `AccionesRapidas` → círculos de 56 dp con etiqueta y entrada escalonada
- (anda: círculos de 56 dp con la acción destacada en verde sólido, test de roles; falta: entrada escalonada y su MP4, que dependen de H5)

### H4.S2.M7 — `Alerta`, `Seccion`, `Pestanas`, `BarraDePasos`, `ChipsDeFiltro`, `TarjetaKPI` y
- (anda: BotonDeCabecera en círculo, píldora de pestañas; falta Alerta, Seccion, Pestanas, BarraDePasos, ChipsDeFiltro, TarjetaKPI y RielDeTurnos)

### H4.S3.M3 — `EstadoVacio`, `EstadoError` y `EstadoDePantalla` con ilustración, título, expli
- (anda: EstadoVacio con círculo de marca, título, ícono propio y acción, y EstadoDePantalla lo propaga; falta: EstadoError con el mismo tratamiento y las capturas de los tres estados × 2 temas)

### H4.S3.M4 — `BarraPestanas` flotante de vidrio con indicador en píldora con resorte (más pes
- (anda: cápsula flotante con píldora que crece, QR central levantado, etiquetas del mismo tamaño y test; falta: indicador que se desliza con resorte entre pestañas y su MP4 normal/reducido, que van con H5)

### H6.S1.M1 — `/billetera/inicio`
- (anda: cabecera con Ayuda y avisos en círculos, héroe, acciones, vacío de pasanakus con crear/entrar y últimos movimientos con la fila nueva; falta: tarjeta «próximo aporte» (A11) y carrusel de grupos)

### H6.S2.M1 — `/pasanaku/mi-estado`
- (anda: título grande, tarjeta de grupo con la cifra por período, tarjeta de aporte con cada cifra nombrada y restricción como alerta; falta: acción de pago en la alerta (A11) y el resumen de compromisos de todos los grupos)

### H6.S4.M1 — `/identidad/perfil`
- (anda: tarjeta de verificación con escudo y chip de estado con tono, correo, lista agrupada con íconos, cerrar sesión secundario y «Dar de baja» como fila roja al final; falta: avatar/nombre de la persona (el contrato solo trae el correo) y nivel KYC en escalera)

## Pendiente
Sin empezar (`TODO`): H3.S1.M4, H3.S1.M5, H4.S1.M3, H4.S1.M4, H4.S1.M5, H4.S1.M6, H4.S1.M7, H4.S1.M8, H4.S2.M1, H4.S2.M5, H4.S2.M6, H4.S3.M2, H4.S3.M5, H4.S3.M6, H4.S3.M7, H4.S3.M8, H5.S1.M1, H5.S1.M2, H5.S2.M1, H5.S2.M2, H5.S2.M3, H5.S2.M4, H5.S2.M5, H5.S2.M6, H5.S2.M7, H5.S2.M8, H5.S2.M9, H5.S3.M1, H5.S3.M2, H5.S3.M3, H6.S1.M2, H6.S1.M3, H6.S1.M4, H6.S1.M5, H6.S2.M2, H6.S2.M3, H6.S2.M4, H6.S2.M5, H6.S2.M6, H6.S2.M7, H6.S2.M8, H6.S2.M9, H6.S2.M10, H6.S3.M1, H6.S3.M2, H6.S3.M3, H6.S3.M4, H6.S3.M5, H6.S3.M6, H6.S4.M2, H6.S4.M3, H6.S4.M4, H6.S4.M5, H6.S4.M6, H7.S1.M1, H7.S1.M2, H7.S2.M1, H7.S2.M2, H7.S2.M3, H7.S2.M4, H7.S3.M1, H8.S1.M1, H8.S1.M2, H8.S1.M3, H8.S1.M4, H8.S2.M1, H8.S2.M2, H8.S2.M3, H8.S3.M1, H8.S3.M2, H9.S1.M1, H9.S1.M2, H9.S1.M3, H9.S1.M4.
Sin bloqueos externos. Lo abierto son decisiones de negocio (ver Decisiones) y el resto del plan.

## Evidencia
- `evidencia/h4-s2-s3-inicio.txt`, `h4-s2-m4-fila.txt`, `h4-s3-m4-h6-s4-m7-barra.txt`,
  `h6-s2-m1-grupos.txt`, `h6-s4-m1-perfil.txt`: comandos, salidas y clasificación de rojos.
- `evidencia/revision-goldens.md`: cada lote de goldens regenerado y mirado (más de 400 imágenes),
  con hallazgos G1–G6.
- `evidencia/roast-pantallas-hoy.md`: la crítica con 8 arreglos ordenados por impacto.
- `evidencia/antes/` (1344 capturas), `intermedio-h4/`, `intermedio-h6/`, `componentes/`.
- Última pirámide corrida (apps/movil): `dart analyze --fatal-infos` limpio; `flutter test test/unidad
  test/widget test/contrato test/identidad test/pasanaku test/a11y` → **+771 verde**; `test/goldens`
  → **+274 verde**. Paquete: **+146 verde** (unidad, widget, a11y, goldens). Tokens: vitest 15/15.
  `verificar_frontend.py movil` y `diseno`: TODO OK.
- Línea base: en `dev` (7752e1b) los 274 goldens de la app y los 18 del paquete pasaban en Linux; toda
  diferencia de píxeles posterior viene del rediseño.

## No cubierto
- Ninguna pantalla nueva se recorrió en runtime contra backend/mock con consola y red (peldaño 5).
- Lector de pantalla real y TalkBack; contraste calculado de las superficies nuevas fuera de las ya
  probadas por `roles-contraste`.
- Movimiento: no hay ningún MP4 de las animaciones nuevas (H5 sin empezar). La píldora de la barra
  crece, pero el indicador no se desliza con resorte.
- Pantallas sin rediseñar: portada, ingreso, MFA, alta (6 pasos), KYC, ayuda, verificación (5 estados),
  bienvenida, recargar/retirar/transferir/extracto, puntaje, crear grupo, invitar y el resto.
- Web y backoffice Angular: solo capturas de los tokens (H2.S5.M1); sin regresión completa.
- Los goldens se regeneraron en Linux, la plataforma del CI; los de macOS dependen de `goldens-macos.yml`.
- `dispositivos` e iOS/Android reales: no probados.

## Desvíos del plan
- Reorden: H6 pantalla por pantalla antes de terminar H4/H5 (pedido de la persona usuaria).
- `TarjetaSaldo` quedó solo en el catálogo; la tarjeta héroe vive en `EncabezadoDeSaldo` (A9).
- La fila de movimiento de la app dejó de tener su propia versión y usa la del paquete.
- Se agregó el token `tipo.pestana` a `tokens.json` y a su espejo CSS (el gate rechazó un literal).

## Riesgos residuales y deuda
- Muchos goldens cambiaron: dependen de mirarlos, no solo de pasar. Cada lote quedó revisado en
  `revision-goldens.md`, pero 400+ imágenes no se revisan a ojo con la misma calidad.
- Cuatro hallazgos preexistentes sin arreglar (G1, G2, G3, G5), asignados a su pantalla en H6.
- Incidente de proceso: un reemplazo mal armado inundó un archivo nuevo; se recuperó exacto y se verificó
  (ver `h4-s3-m4-h6-s4-m7-barra.txt`).

## Decisiones y ambigüedades
Resueltas por la persona usuaria (delegó en «lo más recomendado»): A1, A2, A3, A4, A6.
Abiertas, con supuesto tomado y a quién confirmarla:
- **A5** nombre visible «Aporteo» vs «AportaYa» → Pablo.
- **A7** bases de goldens en Linux; las de macOS por `goldens-macos.yml` → Pablo.
- **A8** «Transferir» no se expone entre las acciones de Inicio (toca la regla 91.5) → Pablo.
- **A9** `TarjetaSaldo` duplicada por `EncabezadoDeSaldo`; supuesto: retirarla → Pablo.
- **A10** el contrato de movimientos solo trae `sentido`, no el tipo; la fila no adivina → dueño de `nucleo-financiero`.
- **A11** pagar desde «Estado de tu aporte» necesita el id de la obligación, que el estado no trae → dueño de `aportes`.

## Procesos
Al cerrar este corte no queda ningún proceso en segundo plano: servidor mock (puerto 4010) y servidor
web (8080) detenidos y puertos verificados libres.
