# Lote A — packages/diseno_flutter (auditoría de solo lectura)

Rama origin/test @07fc697. 242 archivos en el lote: 213 de texto leídos completos (pubspec.lock muestreado), 29 binarios (23 PNG de goldens, 6 TTF) sólo inventariados. No se ejecutó nada (sin SDK de Flutter en la máquina): todo lo de abajo es peldaño DISCOVERED (lectura + grep), salvo donde se indica lo contrario.

## 1) Cobertura

| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| packages/diseno_flutter/.fvmrc | 3 | sí | Fija Flutter 3.44.8 para FVM. |
| packages/diseno_flutter/.gitignore | 7 | sí | Ignora build, tokens.dart generado y fallos de goldens. |
| packages/diseno_flutter/analysis_options.yaml | 21 | sí | Reglas de análisis (flutter_lints + 5 reglas). |
| packages/diseno_flutter/lib/atomos/aspecto_de_etapa.dart | 68 | sí | Color e ícono de cada etapa de reputación por orden. |
| packages/diseno_flutter/lib/atomos/avatar.dart | 53 | sí | Avatar circular con iniciales o imagen. |
| packages/diseno_flutter/lib/atomos/ayuda_de_hoja.dart | 30 | sí | Línea de ayuda con ícono de la hoja de selección. |
| packages/diseno_flutter/lib/atomos/barra_de_puntos.dart | 80 | sí | Indicador de láminas (pasivo o navegable). |
| packages/diseno_flutter/lib/atomos/borde_con_halo.dart | 58 | sí | OutlineInputBorder con anillo de foco. |
| packages/diseno_flutter/lib/atomos/boton.dart | 185 | sí | Botón principal con variantes, carga y motivo de bloqueo. |
| packages/diseno_flutter/lib/atomos/boton_flotante.dart | 43 | sí | FAB circular sobre SuperficieViva. |
| packages/diseno_flutter/lib/atomos/boton_icono.dart | 50 | sí | IconButton redondo de 48 dp con tooltip. |
| packages/diseno_flutter/lib/atomos/boton_tamano.dart | 1 | sí | Enum de tamaños de botón. |
| packages/diseno_flutter/lib/atomos/boton_variante.dart | 3 | sí | Enum de variantes de botón. |
| packages/diseno_flutter/lib/atomos/campo.dart | 145 | sí | Campo de texto base con etiqueta, ayuda, error y éxito. |
| packages/diseno_flutter/lib/atomos/campo_busqueda.dart | 52 | sí | Campo de búsqueda con borrar. |
| packages/diseno_flutter/lib/atomos/campo_codigo.dart | 36 | sí | Campo de código de invitación (5 chars, mayúsculas). |
| packages/diseno_flutter/lib/atomos/campo_contrasena.dart | 84 | sí | Campo de contraseña con ojo y medidor de fortaleza. |
| packages/diseno_flutter/lib/atomos/campo_de_seleccion.dart | 168 | sí | Select que abre hoja con buscador. |
| packages/diseno_flutter/lib/atomos/campo_monto.dart | 80 | sí | Campo de importe que emite cadena del contrato. |
| packages/diseno_flutter/lib/atomos/campo_monto_grande.dart | 163 | sí | Importe protagonista con montos sugeridos. |
| packages/diseno_flutter/lib/atomos/campo_o_t_p.dart | 152 | sí | Seis celdas OTP/PIN. |
| packages/diseno_flutter/lib/atomos/casilla.dart | 43 | sí | Checkbox con etiqueta tocable. |
| packages/diseno_flutter/lib/atomos/chip_elegible.dart | 96 | sí | Chip de elección/filtro. |
| packages/diseno_flutter/lib/atomos/chip_estado.dart | 64 | sí | Chip de estado con punto y tono. |
| packages/diseno_flutter/lib/atomos/codigo_q_r.dart | 108 | sí | Dibujo de QR y exportación PNG para WhatsApp. |
| packages/diseno_flutter/lib/atomos/colores_de_turno.dart | 36 | sí | Paleta cíclica de colores por turno. |
| packages/diseno_flutter/lib/atomos/cuenta_enmascarada.dart | 36 | sí | Número de cuenta enmascarado a 4 dígitos. |
| packages/diseno_flutter/lib/atomos/decoracion_de_campo.dart | 110 | sí | InputDecoration compartida de campos. |
| packages/diseno_flutter/lib/atomos/entrada_de_estado.dart | 20 | sí | Fundido de entrada para vacío/error. |
| packages/diseno_flutter/lib/atomos/esqueleto.dart | 35 | sí | Bloques placeholder de carga. |
| packages/diseno_flutter/lib/atomos/esqueleto_brillante.dart | 64 | sí | Destello sobre placeholders (respeta movimiento reducido). |
| packages/diseno_flutter/lib/atomos/estrellas_calificacion.dart | 37 | sí | Calificación 1–5 con estrellas. |
| packages/diseno_flutter/lib/atomos/fecha.dart | 55 | sí | Formatea instantes a hora La Paz (UTC-4 fijo) y días de calendario. |
| packages/diseno_flutter/lib/atomos/fila_de_seleccion.dart | 104 | sí | Fila de la hoja de selección con resaltado. |
| packages/diseno_flutter/lib/atomos/filtrar_opciones_de_hoja.dart | 16 | sí | Filtro de índices por texto normalizado. |
| packages/diseno_flutter/lib/atomos/girador.dart | 31 | sí | Spinner con etiqueta accesible. |
| packages/diseno_flutter/lib/atomos/grupo_radio.dart | 54 | sí | Grupo de radios (o texto si hay 1 opción). |
| packages/diseno_flutter/lib/atomos/hoja_de_seleccion.dart | 183 | sí | Bottom sheet de selección con buscador. |
| packages/diseno_flutter/lib/atomos/hundido_al_tocar.dart | 79 | sí | Escala 0,97 al presionar con resorte. |
| packages/diseno_flutter/lib/atomos/iconos_de_marca.dart | 79 | sí | Íconos Lucide por significado. |
| packages/diseno_flutter/lib/atomos/insignia_de_etapa.dart | 94 | sí | Insignia redonda de etapa (actual/alcanzada/bloqueada). |
| packages/diseno_flutter/lib/atomos/interruptor.dart | 40 | sí | Switch con etiqueta. |
| packages/diseno_flutter/lib/atomos/lista_con_buscador.dart | 20 | sí | Fachada de compatibilidad sobre la hoja de selección. |
| packages/diseno_flutter/lib/atomos/logotipo.dart | 77 | sí | Isotipo + palabra AportaYa apilados. |
| packages/diseno_flutter/lib/atomos/luz_de_boton.dart | 41 | sí | Painter del filo de luz/brillo del botón. |
| packages/diseno_flutter/lib/atomos/marca.dart | 54 | sí | Isotipo dibujado con CustomPaint. |
| packages/diseno_flutter/lib/atomos/monto.dart | 40 | sí | Único átomo que pinta dinero desde cadena del contrato. |
| packages/diseno_flutter/lib/atomos/monto_que_sube.dart | 128 | sí | Importe animado que cuenta hasta el saldo. |
| packages/diseno_flutter/lib/atomos/movimiento.dart | 125 | sí | Duraciones/curvas/resortes y helpers de movimiento reducido. |
| packages/diseno_flutter/lib/atomos/normalizar_para_buscar.dart | 11 | sí | Normaliza tildes y mayúsculas. |
| packages/diseno_flutter/lib/atomos/numero_que_sube.dart | 114 | sí | Entero animado (puntos). |
| packages/diseno_flutter/lib/atomos/palabra_de_marca.dart | 54 | sí | Palabra «AportaYa» con «Ya» en acento. |
| packages/diseno_flutter/lib/atomos/pintor_de_aura.dart | 116 | sí | Fondo de luces y viñeteo. |
| packages/diseno_flutter/lib/atomos/pintor_de_marca.dart | 104 | sí | Painter de los seis trazos del isotipo. |
| packages/diseno_flutter/lib/atomos/pintor_de_rueda.dart | 79 | sí | Painter de la rueda de turnos. |
| packages/diseno_flutter/lib/atomos/progreso.dart | 63 | sí | Barra/anillo de progreso con valor accesible. |
| packages/diseno_flutter/lib/atomos/punto.dart | 41 | sí | Punto de novedades con cuenta. |
| packages/diseno_flutter/lib/atomos/rueda.dart | 73 | sí | Rueda de turnos cobrados/pendientes. |
| packages/diseno_flutter/lib/atomos/selector_segmentado.dart | 109 | sí | Control segmentado. |
| packages/diseno_flutter/lib/atomos/superficie_viva.dart | 168 | sí | Piel de botón con hundido y brillo. |
| packages/diseno_flutter/lib/atomos/tono.dart | 17 | sí | Enum de tonos semánticos y sus colores. |
| packages/diseno_flutter/lib/catalogo/catalogo.dart | 87 | sí | Widgetbook claro/oscuro y página de catálogo para goldens. |
| packages/diseno_flutter/lib/catalogo/muestras.dart | 1180 | sí | Muestras de catálogo con datos ficticios (1180 líneas). |
| packages/diseno_flutter/lib/dinero/formatear.dart | 103 | sí | Formateo/aritmética de importes como cadena y centavos. |
| packages/diseno_flutter/lib/errores.dart | 8 | sí | Interfaz ErrorPresentable. |
| packages/diseno_flutter/lib/fuentes/Bricolage-600.ttf | binario (90864 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Bricolage-700.ttf | binario (90788 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Bricolage-800.ttf | binario (90836 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Instrument-400.ttf | binario (68712 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Instrument-500.ttf | binario (68932 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Instrument-600.ttf | binario (68900 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/OFL-Bricolage.txt | 93 | sí | Licencia OFL de Bricolage. |
| packages/diseno_flutter/lib/fuentes/OFL-Instrument.txt | 93 | sí | Licencia OFL de Instrument Sans. |
| packages/diseno_flutter/lib/ilustraciones/arte_de_maqueta.dart | 52 | sí | Base de painters del tour (lienzo 140). |
| packages/diseno_flutter/lib/ilustraciones/arte_en_custodia.dart | 55 | sí | Lámina «en custodia» con comprobante verificado. |
| packages/diseno_flutter/lib/ilustraciones/arte_fondo_de_garantia.dart | 53 | sí | Lámina paraguas del fondo de garantía cubriendo la cuota. |
| packages/diseno_flutter/lib/ilustraciones/arte_sin_cuaderno.dart | 59 | sí | Lámina «sin cuaderno». |
| packages/diseno_flutter/lib/ilustraciones/arte_sorteo_a_la_vista.dart | 60 | sí | Lámina del sorteo. |
| packages/diseno_flutter/lib/ilustraciones/lienzo.dart | 31 | sí | Envoltorio Ilustracion con semántica. |
| packages/diseno_flutter/lib/ilustraciones/pedestal.dart | 56 | sí | Orbe con sombra bajo un dibujo. |
| packages/diseno_flutter/lib/moleculas/acciones_rapidas.dart | 79 | sí | Accesos circulares de la billetera. |
| packages/diseno_flutter/lib/moleculas/acordeon.dart | 40 | sí | ExpansionTile estilizado. |
| packages/diseno_flutter/lib/moleculas/alerta.dart | 75 | sí | Aviso con tono, detalle y acción. |
| packages/diseno_flutter/lib/moleculas/aparicion_escalonada.dart | 124 | sí | Entrada escalonada de hijos. |
| packages/diseno_flutter/lib/moleculas/barra_de_accion.dart | 53 | sí | Acción principal anclada abajo. |
| packages/diseno_flutter/lib/moleculas/barra_de_etapa.dart | 179 | sí | Etapa actual con barra de avance en diezmilésimos. |
| packages/diseno_flutter/lib/moleculas/barra_de_pasos.dart | 51 | sí | Paso N de M segmentado. |
| packages/diseno_flutter/lib/moleculas/cabecera.dart | 188 | sí | Cabeceras de inicio/sección y BotonDeCabecera. |
| packages/diseno_flutter/lib/moleculas/campo_de_fecha.dart | 174 | sí | Fecha en tres selects con validación de día. |
| packages/diseno_flutter/lib/moleculas/chips_de_filtro.dart | 48 | sí | Chips de filtro en Wrap. |
| packages/diseno_flutter/lib/moleculas/desglose_de_cobro.dart | 80 | sí | Líneas bruto/comisión/descuento y neto. |
| packages/diseno_flutter/lib/moleculas/escalera_de_etapas.dart | 79 | sí | Etapas de cobranza con canal y tope. |
| packages/diseno_flutter/lib/moleculas/fila_de_cotejo.dart | 95 | sí | Declarado vs leído de un documento. |
| packages/diseno_flutter/lib/moleculas/fila_de_dato.dart | 41 | sí | Etiqueta + valor seleccionable. |
| packages/diseno_flutter/lib/moleculas/fila_de_movimiento.dart | 193 | sí | Fila de extracto con signo e importe. |
| packages/diseno_flutter/lib/moleculas/grilla_metricas_miembro.dart | 123 | sí | Grilla de KPIs de miembro con valores por defecto ficticios. |
| packages/diseno_flutter/lib/moleculas/grilla_metricas_miembro_partes.dart | 92 | sí | Tarjeta individual de la grilla de métricas. |
| packages/diseno_flutter/lib/moleculas/insignia_tile.dart | 151 | sí | Tarjeta de logro con tiers (estilo GymSheet). |
| packages/diseno_flutter/lib/moleculas/insignia_tile_partes.dart | 114 | sí | Encabezado de la tarjeta de logro. |
| packages/diseno_flutter/lib/moleculas/intro_de_pantalla.dart | 70 | sí | Ícono, título y explicación de pantalla interior. |
| packages/diseno_flutter/lib/moleculas/item_de_notificacion.dart | 89 | sí | Ítem de bandeja de avisos. |
| packages/diseno_flutter/lib/moleculas/item_pasanaku.dart | 58 | sí | Grupo en una línea con avance y estado. |
| packages/diseno_flutter/lib/moleculas/medidor_de_rango.dart | 69 | sí | Rango mínimo/estimado/máximo de un importe. |
| packages/diseno_flutter/lib/moleculas/opcion_con_costo.dart | 78 | sí | Tarjeta de opción con su costo. |
| packages/diseno_flutter/lib/moleculas/opcion_seleccionable.dart | 101 | sí | Opción excluyente como tarjeta. |
| packages/diseno_flutter/lib/moleculas/parte_de_fecha.dart | 74 | sí | Meses, diasDelMes y un select de fecha. |
| packages/diseno_flutter/lib/moleculas/pestanas.dart | 61 | sí | Pestañas de contenido. |
| packages/diseno_flutter/lib/moleculas/pregunta.dart | 139 | sí | Bloque pregunta/control/ayuda y Contador ±1. |
| packages/diseno_flutter/lib/moleculas/referencia_de_error.dart | 26 | sí | Muestra trazaId de soporte. |
| packages/diseno_flutter/lib/moleculas/reloj_de_plazo.dart | 86 | sí | Plazo guardado con lo que falta. |
| packages/diseno_flutter/lib/moleculas/resumen_de_periodo.dart | 72 | sí | Entró/salió y saldos de un período. |
| packages/diseno_flutter/lib/moleculas/riel_de_turnos.dart | 151 | sí | Riel de turnos con pulso del actual. |
| packages/diseno_flutter/lib/moleculas/seccion.dart | 100 | sí | TituloDeSeccion, Panel y FilasDePanel. |
| packages/diseno_flutter/lib/moleculas/selectores_de_fecha_responsivos.dart | 104 | sí | Día/Mes/Año en fila o apilados. |
| packages/diseno_flutter/lib/moleculas/tarjeta.dart | 47 | sí | Superficie base de tarjeta. |
| packages/diseno_flutter/lib/moleculas/tarjeta_de_racha.dart | 72 | sí | Racha de meses al día. |
| packages/diseno_flutter/lib/moleculas/tarjeta_k_p_i.dart | 63 | sí | Indicador con variación y provisorio. |
| packages/diseno_flutter/lib/moleculas/tarjeta_puntos_mesa.dart | 137 | sí | Puntos «en la mesa de juego» con rango (estilo GymSheet). |
| packages/diseno_flutter/lib/moleculas/tarjeta_puntos_mesa_partes.dart | 85 | sí | Cabecera de puntos con ícono de casino. |
| packages/diseno_flutter/lib/moleculas/tarjeta_riesgo_confianza.dart | 129 | sí | Calificación de riesgo con valores por defecto ficticios. |
| packages/diseno_flutter/lib/moleculas/tarjeta_riesgo_confianza_partes.dart | 175 | sí | Cabecera y métricas de la tarjeta de riesgo. |
| packages/diseno_flutter/lib/moleculas/tarjeta_saldo.dart | 92 | sí | Saldo disponible/retenido/en pasanakus. |
| packages/diseno_flutter/lib/moleculas/tipo_de_movimiento.dart | 45 | sí | Enum de tipos de movimiento con ícono y nombre. |
| packages/diseno_flutter/lib/moviles/acciones_del_globo.dart | 77 | sí | Botones del globo de tutorial. |
| packages/diseno_flutter/lib/moviles/alcance_haptica.dart | 28 | sí | InheritedWidget de política háptica. |
| packages/diseno_flutter/lib/moviles/anclas_de_tutorial.dart | 120 | sí | Registro de GlobalKeys para tutoriales. |
| packages/diseno_flutter/lib/moviles/apertura_de_marca.dart | 145 | sí | Splash animado de marca (1,4 s). |
| packages/diseno_flutter/lib/moviles/barra_pestanas.dart | 174 | sí | Barra de pestañas flotante con píldora y acción central. |
| packages/diseno_flutter/lib/moviles/barra_pestanas_destino.dart | 191 | sí | Destino de la barra y botón central. |
| packages/diseno_flutter/lib/moviles/barra_pestanas_pildora.dart | 52 | sí | Píldora deslizante de la barra. |
| packages/diseno_flutter/lib/moviles/celebracion_de_exito.dart | 115 | sí | Círculo con tilde de «Cuenta creada». |
| packages/diseno_flutter/lib/moviles/como_en_un_telefono.dart | 114 | sí | Enmarca la app en un teléfono en web ancha. |
| packages/diseno_flutter/lib/moviles/foco_de_tutorial.dart | 119 | sí | Velo con agujero y anillo del tutorial. |
| packages/diseno_flutter/lib/moviles/fondo_de_marca.dart | 48 | sí | Fondo con aura de marca. |
| packages/diseno_flutter/lib/moviles/globo_de_tutorial.dart | 162 | sí | Globo del tutorial con progreso. |
| packages/diseno_flutter/lib/moviles/hoja_de_confirmacion.dart | 109 | sí | Bottom sheet para confirmar operación de dinero. |
| packages/diseno_flutter/lib/moviles/mostrar_snackbar.dart | 30 | sí | Snackbar verde con acción. |
| packages/diseno_flutter/lib/moviles/notificacion_emergente.dart | 91 | sí | Aviso emergente encolado. |
| packages/diseno_flutter/lib/moviles/panel_de_marca.dart | 128 | sí | Panel verde del zoom de marca (retirado). |
| packages/diseno_flutter/lib/moviles/pantalla_de_onboarding.dart | 53 | sí | Pantalla de tour genérica. |
| packages/diseno_flutter/lib/moviles/teclado_numerico.dart | 86 | sí | Teclado 3×4 para montos/PIN. |
| packages/diseno_flutter/lib/moviles/tour_en_stages.dart | 193 | sí | Walkthrough por etapas en bottom sheet. |
| packages/diseno_flutter/lib/moviles/tour_en_stages_datos.dart | 61 | sí | Textos por defecto del tour gamificado. |
| packages/diseno_flutter/lib/moviles/tour_en_stages_pagina.dart | 133 | sí | Página de una etapa del tour. |
| packages/diseno_flutter/lib/moviles/tour_en_stages_pie.dart | 77 | sí | Puntos y botones del tour. |
| packages/diseno_flutter/lib/moviles/transicion_con_zoom.dart | 67 | sí | Transición de página con zoom (reemplazada). |
| packages/diseno_flutter/lib/moviles/transicion_de_eje.dart | 80 | sí | Transición lateral corta con fundido. |
| packages/diseno_flutter/lib/moviles/transicion_de_marca.dart | 59 | sí | Transición de portada (fundido y subida). |
| packages/diseno_flutter/lib/organismos/banner_de_pauta.dart | 89 | sí | Publicidad rotulada. |
| packages/diseno_flutter/lib/organismos/calendario_de_cuotas.dart | 191 | sí | Calendario mensual de cuotas con total exigible. |
| packages/diseno_flutter/lib/organismos/camino_de_etapas.dart | 195 | sí | Camino vertical de etapas de reputación. |
| packages/diseno_flutter/lib/organismos/celda_de_cuota.dart | 127 | sí | Día del calendario de cuotas. |
| packages/diseno_flutter/lib/organismos/celebracion_stage.dart | 182 | sí | Diálogo de celebración de logro (rayos, partículas). |
| packages/diseno_flutter/lib/organismos/celebracion_stage_piezas.dart | 115 | sí | Ícono con halo y contador de puntos. |
| packages/diseno_flutter/lib/organismos/celebracion_stage_pintores.dart | 118 | sí | Estampilla, rayos y partículas. |
| packages/diseno_flutter/lib/organismos/celebracion_stage_tarjeta.dart | 97 | sí | Tarjeta central de la celebración. |
| packages/diseno_flutter/lib/organismos/cuota.dart | 8 | sí | Typedef Cuota (fecha, grupo, monto, estado). |
| packages/diseno_flutter/lib/organismos/encabezado_de_saldo.dart | 198 | sí | Tarjeta héroe del saldo con ocultar. |
| packages/diseno_flutter/lib/organismos/encabezado_de_saldo_piezas.dart | 120 | sí | Botón ocultar, chip, frase de custodia, transición. |
| packages/diseno_flutter/lib/organismos/estado_de_cuota.dart | 2 | sí | Enum de estados de cuota. |
| packages/diseno_flutter/lib/organismos/estado_de_pantalla.dart | 107 | sí | Pinta AsyncValue en cargando/error/vacío/datos. |
| packages/diseno_flutter/lib/organismos/estado_de_tramite.dart | 117 | sí | Estado de un trámite con pasos y caso. |
| packages/diseno_flutter/lib/organismos/estado_de_tramite_pasos.dart | 110 | sí | Pasos del trámite. |
| packages/diseno_flutter/lib/organismos/estado_error.dart | 110 | sí | Error con mensaje, traza y reintento. |
| packages/diseno_flutter/lib/organismos/estado_vacio.dart | 83 | sí | Vacío que orienta. |
| packages/diseno_flutter/lib/organismos/fila_de_cuota.dart | 46 | sí | Cuota del día elegido. |
| packages/diseno_flutter/lib/organismos/leyenda_de_cuotas.dart | 79 | sí | Leyenda del calendario. |
| packages/diseno_flutter/lib/organismos/lista_de_movimientos.dart | 139 | sí | Extracto agrupado por día con neto. |
| packages/diseno_flutter/lib/organismos/lista_de_requisitos.dart | 87 | sí | Requisitos cumplidos/faltantes (habilitación). |
| packages/diseno_flutter/lib/organismos/motivo_vacio.dart | 3 | sí | Enum de motivos de vacío. |
| packages/diseno_flutter/lib/organismos/movimiento.dart | 11 | sí | Typedef Movimiento. |
| packages/diseno_flutter/lib/organismos/panel_bienvenida.dart | 169 | sí | Bienvenida con bono y pasos. |
| packages/diseno_flutter/lib/organismos/panel_de_factores.dart | 90 | sí | Veredicto aprobado/no con factores. |
| packages/diseno_flutter/lib/organismos/panel_sorteo.dart | 106 | sí | Pasos del sorteo con hash y reproducción. |
| packages/diseno_flutter/lib/organismos/pantalla_con_cabecera.dart | 199 | sí | Pantalla con cabecera que se compacta. |
| packages/diseno_flutter/lib/organismos/tarjeta_de_oferta.dart | 89 | sí | Oferta de permuta de turno. |
| packages/diseno_flutter/lib/organismos/tarjeta_de_solicitud.dart | 155 | sí | Solicitud de ingreso con aceptar/rechazar. |
| packages/diseno_flutter/lib/organismos/vale.dart | 117 | sí | Cupón de comercio. |
| packages/diseno_flutter/lib/tema.dart | 159 | sí | ThemeData desde tokens. |
| packages/diseno_flutter/lib/tema_de_campos.dart | 45 | sí | InputDecorationTheme. |
| packages/diseno_flutter/lib/tema_de_controles.dart | 93 | sí | Estilos de botones y controles. |
| packages/diseno_flutter/lib/tema_de_superficies.dart | 68 | sí | Tarjeta, AppBar, hoja, diálogo, snackbar. |
| packages/diseno_flutter/package.json | 17 | sí | Envoltorio turbo: build copia tokens.dart, typecheck, lint, tests. |
| packages/diseno_flutter/pubspec.lock | 434 | no (muestreado: dependencias directas y sdks) | Lockfile de dependencias Dart (muestreado: versiones directas y sdks). |
| packages/diseno_flutter/pubspec.yaml | 46 | sí | Paquete aportaya_diseno: deps (riverpod, qr_flutter, lucide, widgetbook) y fuentes. |
| packages/diseno_flutter/test/a11y/catalogo_a11y_test.dart | 32 | sí | Guías de a11y sobre el catálogo en claro/oscuro. |
| packages/diseno_flutter/test/capturas/catalogo_capturas_test.dart | 101 | sí | Capturas con fuentes reales hacia docs/. |
| packages/diseno_flutter/test/comun.dart | 38 | sí | Helpers conTema y pantallaChica. |
| packages/diseno_flutter/test/goldens/catalogo_golden_test.dart | 68 | sí | Goldens del catálogo (no en CI). |
| packages/diseno_flutter/test/goldens/ilustraciones_golden_test.dart | 68 | sí | Goldens de ilustraciones. |
| packages/diseno_flutter/test/goldens/imagenes/apertura_medio.png | binario (8545 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/apertura_tarde.png | binario (13647 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/apertura_temprano.png | binario (5541 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_botones_claro.png | binario (23527 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_botones_oscuro.png | binario (23460 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_campos_claro.png | binario (18255 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_campos_oscuro.png | binario (18521 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_indicadores_claro.png | binario (46015 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_indicadores_oscuro.png | binario (49147 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_seleccion_claro.png | binario (16297 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_seleccion_oscuro.png | binario (16343 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/ilustraciones_claro.png | binario (17993 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/ilustraciones_oscuro.png | binario (17702 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/marca_claro.png | binario (7035 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/marca_oscuro.png | binario (6999 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moleculas_claro.png | binario (40464 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moleculas_oscuro.png | binario (40902 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moviles_claro.png | binario (36205 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moviles_oscuro.png | binario (40329 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/organismos_claro.png | binario (23324 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/organismos_oscuro.png | binario (25090 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/reputacion_etapas_claro.png | binario (36204 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/reputacion_etapas_oscuro.png | binario (35832 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/marca_golden_test.dart | 66 | sí | Goldens de marca y apertura. |
| packages/diseno_flutter/test/unidad/aspecto_de_etapa_test.dart | 40 | sí | Contraste y unicidad de colores de etapa. |
| packages/diseno_flutter/test/unidad/fecha_test.dart | 21 | sí | Fecha de calendario y conversión a La Paz. |
| packages/diseno_flutter/test/unidad/formatear_monto_test.dart | 34 | sí | Vectores compartidos de formato de dinero. |
| packages/diseno_flutter/test/unidad/formatear_porcentaje_test.dart | 19 | sí | Formato de porcentaje. |
| packages/diseno_flutter/test/unidad/logica_pura_test.dart | 149 | sí | Elevación, CampoMonto, máscara, fecha, plazo, neto, exigible. |
| packages/diseno_flutter/test/unidad/movimiento_test.dart | 39 | sí | Parámetros de resortes y tope de 320 ms. |
| packages/diseno_flutter/test/widget/barra_pestanas_resorte_test.dart | 80 | sí | Píldora con resorte y movimiento reducido. |
| packages/diseno_flutter/test/widget/barra_pestanas_test.dart | 171 | sí | Semántica, toque y botón central. |
| packages/diseno_flutter/test/widget/campo_de_ejemplo_test.dart | 99 | sí | Ejemplo/hint de los campos. |
| packages/diseno_flutter/test/widget/campo_de_fecha_test.dart | 133 | sí | Selects de fecha y validación. |
| packages/diseno_flutter/test/widget/campo_de_seleccion_test.dart | 207 | sí | Hoja con buscador y foco. |
| packages/diseno_flutter/test/widget/campo_monto_grande_test.dart | 72 | sí | Contrato y sugeridos del monto grande. |
| packages/diseno_flutter/test/widget/campo_relleno_y_halo_test.dart | 39 | sí | Relleno y halo del campo. |
| packages/diseno_flutter/test/widget/comportamiento_test.dart | 573 | sí | Comportamientos varios (estados, OTP, monto, solicitud). |
| packages/diseno_flutter/test/widget/encabezado_de_saldo_desenfoque_test.dart | 73 | sí | Desenfoque al ocultar saldo. |
| packages/diseno_flutter/test/widget/encabezado_de_saldo_test.dart | 78 | sí | Semántica y ocultar del saldo. |
| packages/diseno_flutter/test/widget/estado_de_tramite_test.dart | 91 | sí | Pasos del trámite. |
| packages/diseno_flutter/test/widget/fila_de_cotejo_test.dart | 69 | sí | Cotejo pendiente/coincide/difiere. |
| packages/diseno_flutter/test/widget/fila_de_dato_test.dart | 28 | sí | Valor seleccionable. |
| packages/diseno_flutter/test/widget/fila_de_movimiento_test.dart | 96 | sí | Signo, semántica y apilado. |
| packages/diseno_flutter/test/widget/hoja_de_seleccion_lista_corta_con_teclado_test.dart | 54 | sí | Filas visibles con teclado. |
| packages/diseno_flutter/test/widget/hoja_de_seleccion_test.dart | 219 | sí | Hoja de selección: filtro, foco, semántica. |
| packages/diseno_flutter/test/widget/intro_de_pantalla_test.dart | 49 | sí | Encabezado y tono de peligro. |
| packages/diseno_flutter/test/widget/monto_que_sube_test.dart | 176 | sí | Reglas de animación del saldo. |
| packages/diseno_flutter/test/widget/movimiento_test.dart | 253 | sí | Botones vivos y transición sin zoom. |
| packages/diseno_flutter/test/widget/pantalla_con_cabecera_test.dart | 133 | sí | Cabecera compacta. |
| packages/diseno_flutter/test/widget/roles_de_color_accion_test.dart | 106 | sí | Colores por variante de botón. |
| packages/diseno_flutter/test/widget/roles_de_color_no_estado_test.dart | 400 | sí | Colores neutrales en varias piezas. |

## 2) Hallazgos candidatos

Convención: rutas relativas a `packages/diseno_flutter/` salvo que empiecen con `apps/`. "Uso en app" = `grep -rlw <Símbolo> apps` (solo `apps/movil` consume `package:aportaya_diseno`: 344 archivos).

### H01 — `CampoMonto.aCadenaDelContrato` borra el punto: «12.50» se convierte en 1250.00
- Clase: DINERO / DEFECTO · Severidad propuesta: **S1** (importe ×100 en recargar/retirar/transferir/crear grupo/puja/subasta).
- Ubicaciones: `lib/atomos/campo_monto.dart:31-49` (`final limpio = escrito.replaceAll('.', '')...`), `:72-73` (`TextInputType.numberWithOptions(decimal: true)` + `FilteringTextInputFormatter.allow(RegExp(r'[0-9.,]'))`); `lib/atomos/campo_monto_grande.dart:91-99` (mismo teclado, mismo filtro, misma función). Consumidores: `apps/movil/lib/pantallas/billetera/pantalla_retirar.dart:64`, `pantalla_recargar.dart:84`, `pantalla_transferir.dart:98`, `apps/movil/lib/pantallas/pasanaku/organismos_crear_grupo.dart:79`, `formulario_de_subasta.dart:93`, `formulario_de_puja.dart:68`.
- Cita: `final limpio = escrito.replaceAll('.', '').replaceAll(' ', '').trim();` → `'12.5'` da `'125.00'`, `'12.50'` da `'1250.00'`.
- Cómo: lectura de la función; el filtro deja pasar `.` y el teclado decimal de muchos Android sólo ofrece `.`. La prueba `test/unidad/logica_pura_test.dart:25-40` no cubre ningún caso con punto decimal; `test/widget/comportamiento_test.dart:382-397` tampoco.
- Impacto: la persona escribe el decimal "a la anglosajona" y el contrato recibe un importe 10× o 100× mayor, válido para el servidor. Sólo la hoja de confirmación (si existe en ese flujo) lo frena.
- Causa de fondo: el punto se asume siempre separador de miles; no hay regla para un único punto seguido de 1–2 dígitos. Sin ejecución (peldaño DISCOVERED); verificar en dispositivo.

### H02 — `CampoMonto(valorInicial:)` vuelve a parsear la cadena del contrato como si fuera texto de persona
- Clase: DINERO / DEFECTO latente · S3 (hoy nadie pasa `valorInicial`: `grep -rn valorInicial apps/movil/lib` = 0).
- Ubicaciones: `lib/atomos/campo_monto.dart:56-58` (`TextEditingController(text: widget.valorInicial)`), `:22` (doc: recibe la cadena del contrato `^\d+\.\d{2}$`).
- Impacto: `valorInicial: '1240.00'` se muestra «1240.00» y al editar emite `124000.00` (mismo bug de H01). Además `Campo.valorInicial` (`lib/atomos/campo.dart:14,44`) se declara y **no se usa**: la muestra `lib/catalogo/muestras.dart:191-195` (`valorInicial: '7000000'`) pinta un campo vacío y el golden congela eso.

### H03 — Fondo de garantía que "cubre la cuota" del que no paga (vocabulario de respaldo, en uso)
- Clase: RESPALDO · S2 (texto en producción contradice R-NUEVA-2 si el fondo lo pone la empresa; aunque lo pongan los miembros, es "cubrir faltante").
- Ubicaciones: `lib/ilustraciones/arte_fondo_de_garantia.dart:6-7` («Si alguien no aporta, el grupo no se frena»: el paraguas del fondo de garantía cubriendo la moneda de la cuota»); usado en `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:45-49` con `apps/movil/lib/pantallas/identidad/textos.dart:42-46` («Hay un fondo de garantía que cubre la cuota para que el turno se entregue igual…»). Catálogo: `lib/catalogo/muestras.dart:989-994` («Un paraguas cubriendo la moneda de la cuota»); golden `test/goldens/ilustraciones_golden_test.dart:35`.
- Otras apariciones RESPALDO: `lib/catalogo/muestras.dart:603-606` (OpcionConCosto «Mitad ahora, mitad en 15 días; el fondo cubre la diferencia.»), `:337-342` («Fondo consumido»), `lib/moleculas/tarjeta_riesgo_confianza.dart:13` («coberturas usadas»), `lib/moleculas/tarjeta_riesgo_confianza_partes.dart:128-129` («Fondo usado» / «$coberturasUsadas veces»), `lib/moviles/tour_en_stages_datos.dart:22` («sin garantías asfixiantes»), `lib/organismos/encabezado_de_saldo.dart:45-47` + `encabezado_de_saldo_piezas.dart:63` (clase `_Garantia` para la frase de custodia: nombre que mezcla custodia con garantía).
- Cómo: `grep -rniE "respaldo|cobertur|cubr|faltante|garant"` en el lote + grep de uso en `apps/movil`.

### H04 — Componentes de aprobación humana en el sistema de diseño (y pruebas que los fijan)
- Clase: APROB · S3 (ninguno se usa en `apps/movil`: 0 referencias; contradicen R-NUEVA-1 como catálogo/doc).
- Ubicaciones:
  - `lib/organismos/tarjeta_de_solicitud.dart:12-13` («El pedido de ingreso … las dos acciones; rechazar exige motivo»), `:125` «Confirmar rechazo», `:140` «Aceptar». Prueba: `test/widget/comportamiento_test.dart:544-572`, `test/widget/roles_de_color_no_estado_test.dart:64-85`. Muestra: `lib/catalogo/muestras.dart:853-863`.
  - `lib/organismos/panel_de_factores.dart:11,16,28-39` (`required this.aprobado`); muestra `muestras.dart:782-786` («La permuta no pasa la validación»).
  - `lib/organismos/tarjeta_de_oferta.dart:49-51` (`estado == 'EN_VALIDACION'`), `:79` «Aceptar la permuta».
  - `lib/organismos/estado_de_tramite.dart:9-10` («una verificación que revisa alguien, un pago que confirma el banco»), `:34` («En revisión»); muestras `muestras.dart:828-838` («Un especialista está verificando tu identidad.»), `:840-851` («Rechazado»); prueba `test/widget/estado_de_tramite_test.dart:17-18`.
  - `lib/organismos/lista_de_requisitos.dart:5-6` («Los 14 requisitos…») con códigos `HAB_APRENDIZ_*` en `muestras.dart:758-776` (habilitación).
  - `lib/catalogo/muestras.dart:680` («Lo aprobó el grupo por votación.»), `:288` (ChipEstado «En revisión»).
  - `lib/ilustraciones/arte_en_custodia.dart:6-7,31` y `arte_de_maqueta.dart:14` («comprobante verificado») — tilde de "comprobante" como señal de confianza, usado en `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:31`.
  - `lib/moviles/hoja_de_confirmacion.dart:8-9` (confirmación de operación de dinero: es confirmación de la propia persona, no aprobación de terceros; se lista por la palabra).

### H05 — Widgets con datos ficticios por defecto y reglas de negocio inventadas (gamificación "mesa de juego")
- Clase: HUECO / DATOS / DINERO · S2 (no están en uso hoy, pero son API pública del paquete: cualquier pantalla que los monte sin parámetros muestra cifras falsas como reales).
- Ubicaciones:
  - `lib/moleculas/grilla_metricas_miembro.dart:19-25` (`gruposActivos = 2`, `dineroDepositado = 'Bs 24.800'`, `turnosJugados = 14`…), `:79-82` («Puntualidad 100%»). Dinero como cadena preformateada, sin moneda ni centavos, saltando `Monto`/`formatearMonto` (invariante 5 declarada en `lib/dinero/formatear.dart:3-4`).
  - `lib/moleculas/tarjeta_riesgo_confianza.dart:17-22` (`score = 98`, `calificacion = 'Riesgo Bajo · A1'`, `puntualidadPorcentaje = 99.8`), `:89` (**«Nivel Excelente» fijo para cualquier score**), `:122` («Auditoría fáctica: calculado a partir de asientos contables reales en AportaYa.» — afirmación que el widget no puede garantizar).
  - `lib/moleculas/tarjeta_puntos_mesa.dart:10-12,17-21,90` («1 punto por cada Bs puesto en juego en la mesa», `progresoRango = 0.82`), `lib/moleculas/tarjeta_puntos_mesa_partes.dart:31` (`Icons.casino_outlined`), `:36` «PUNTOS EN JUEGO».
  - `lib/moviles/tour_en_stages_datos.dart:6-60` (reglas inventadas: «Bonos de +50 pts por anticipar tus aportes», «+500 pts al cerrar ciclos», «Acceso preferencial a grupos con montos elevados», «Certificaciones verificables ante terceros», «trofeos exclusivos tipo GymSheet»), `lib/moviles/tour_en_stages_pie.dart:66` («¡Entendido, a jugar!»).
  - `lib/moleculas/insignia_tile.dart:24` y `lib/organismos/celebracion_stage.dart:15-16` («filosofía de GymSheet»), `celebracion_stage_piezas.dart:103` «pts en mesa».
  - `lib/moviles/tour_en_stages_datos.dart:52` («consulta … aportes totales y días de demora de cada integrante») — promete exponer datos financieros de terceros (regla 90.2.8).
- Cómo: lectura + `git log` (agregados 2026-10-20, `e99570d2`, `c46d277a`) + grep de uso (0 en `apps`).
- Causa de fondo: piezas importadas de otro producto sin contrato del servidor; vocabulario de apuesta en una app de ahorro.

### H06 — `CelebracionDeExito`: la tilde se pinta del mismo color que el círculo (invisible)
- Clase: UX-A11Y / DEFECTO · S2 (en uso: 1 referencia en `apps/movil`).
- Ubicaciones: `lib/moviles/celebracion_de_exito.dart:74` (`color: t.brand`) y `:77` (`_TildeDeExito(_trazo.value, t.brandTexto)`).
- Cómo: `packages/tokens/tokens.json` → `roles.claro.brand = g600`, `roles.claro.brandTexto = g600`; `roles.oscuro.brand = g300`, `brandTexto = g300`. Mismo color en ambos temas.
- Impacto: «Cuenta creada» se ve como un círculo verde vacío. La prueba `test/widget/comportamiento_test.dart:275-286` sólo verifica que exista un `CustomPaint`; el `textContrastGuideline` no mide trazos pintados.

### H07 — Cuatro tratamientos de zona horaria distintos para la misma fecha
- Clase: DEFECTO / DUP-X divergente · S2 (un movimiento de madrugada aparece en días distintos según la pantalla).
- Ubicaciones:
  - `lib/atomos/fecha.dart:29-30` (`utc.subtract(const Duration(hours: 4))`, UTC-4 fijo, sin TZ database; `DateTime.parse` de una cadena sin `Z` la toma como hora local del teléfono).
  - `lib/organismos/lista_de_movimientos.dart:41` (`m.fechaIso.substring(0, 10)`: agrupa por **fecha UTC**), `:135-138` (`_diaLegible` parsea esa fecha).
  - `lib/organismos/fila_de_cuota.dart:21` (`cuota.fechaIso.substring(0, 10)` mostrado crudo «2026-09-10»).
  - `lib/organismos/calendario_de_cuotas.dart:79,82` (`startsWith(prefijo)` + `DateTime.parse(c.fechaIso).day`: zona del teléfono).
  - App: `apps/movil/lib/pantallas/billetera/lista_de_movimientos.dart:91` y `apps/movil/lib/pantallas/billetera/movimientos_del_periodo.dart:33` (`registradoEn.toLocal()`: zona del teléfono, sin «(La Paz)»).
- Impacto: `2026-09-09T02:00:00Z` es 8 sep en La Paz; `ListaDeMovimientos` del paquete lo agrupa el 9, `Fecha.formatear` dice 8, la app dice lo que diga el teléfono.

### H08 — Saldo y totales calculados en el cliente con tres parsers de centavos divergentes
- Clase: DINERO / DUP-INT divergente · S2 (regla 91.1.5; uno tiene bug de signo).
- Ubicaciones:
  - `lib/dinero/formatear.dart:28-36` (`centavosDe`, `int`, valida forma, maneja negativo).
  - `lib/organismos/calendario_de_cuotas.dart:56-68` (`totalExigible`, `BigInt`, **sin validar forma y con signo mal**: `'-12.50'` → `BigInt.parse('-12')*100 + 50 = -1150`).
  - `lib/organismos/lista_de_movimientos.dart:22-36` (`neto`, `BigInt`, signo bien) y `:56` (`saldoDelDia = lista.first.saldoCorrido`: depende del orden de entrada, no se ordena dentro del día).
- Impacto: «A pagar este mes» (`calendario_de_cuotas.dart:132-141`) y «Neto del día» son sumas ad-hoc en el cliente, no del mayor. Hoy sin uso en la app (0 refs a `CalendarioDeCuotas`; la app tiene su propio `ListaDeMovimientos`, ver H10), por eso S2 y no S1.
- Pruebas: `test/unidad/logica_pura_test.dart:87-148` no cubren negativos en `totalExigible`.

### H09 — Importes sin moneda en los modelos del paquete
- Clase: DINERO / CONTRATO · S3.
- Ubicaciones: `lib/organismos/cuota.dart:3-8` (`String monto` sin moneda), `lib/organismos/movimiento.dart:4-11` (`monto`, `saldoCorrido` sin moneda), `lib/moleculas/desglose_de_cobro.dart:17-19` (líneas sin moneda, una sola `moneda` para todas), `lib/organismos/calendario_de_cuotas.dart:30-32` (suma cuotas de varios grupos con una sola `moneda`).
- Impacto: si dos grupos tienen monedas distintas, se suman y se rotulan con la del parámetro (regla 91.1.2).

### H10 — Código muerto y reescrituras: ~35 piezas públicas sin uso y un homónimo divergente en la app
- Clase: REESCRITURA / DUP-X · S3.
- Sin referencias en `apps/` (grep `-rlw` = 0): `GrillaMetricasMiembro`, `InsigniaTile`, `TarjetaPuntosMesa`, `TarjetaRiesgoConfianza`, `CelebracionStage`, `TourEnStages`, `TransicionConZoom` (`lib/moviles/transicion_de_eje.dart:7` dice «Reemplaza al zoom»), `PanelDeMarca` (`lib/moviles/transicion_de_marca.dart:8-12` dice que el zoom de marca se quitó), `Logotipo`, `mostrarListaConBuscador` (`lib/atomos/lista_con_buscador.dart:7` «Fachada compatible mientras las pantallas migran»), `TarjetaDeSolicitud`, `PanelDeFactores`, `TarjetaDeOferta`, `DesgloseDeCobro`, `OpcionConCosto`, `MedidorDeRango`, `EscaleraDeEtapas`, `ListaDeRequisitos`, `BannerDePauta`, `Vale`, `CalendarioDeCuotas`, `TarjetaSaldo`, `TarjetaKPI`, `EstrellasCalificacion`, `TecladoNumerico`, `PantallaDeOnboarding`, `NotificacionEmergente`, `ItemPasanaku`, `ResumenDePeriodo`, `RelojDePlazo`, `Pestanas`, `CuentaEnmascarada`, `BotonFlotante`, `Contador` (`lib/moleculas/pregunta.dart:45`). `desformatearMonto` (`lib/dinero/formatear.dart:77-88`, «solo para la prueba de propiedad») vive en `lib/` y sólo lo usan pruebas de `apps/movil/test`.
- Homónimo divergente: `lib/organismos/lista_de_movimientos.dart:10` (agrupa por día, neto, saldo) vs `apps/movil/lib/pantallas/billetera/lista_de_movimientos.dart:13` (4 estados, sin agrupar, primeros 8 animados) — misma intención, mismo nombre, comportamiento distinto; la app además agrupa por día en `movimientos_del_periodo.dart:31-35`.

### H11 — Snackbar verde (color de éxito) usado para errores, y en contra del tema
- Clase: UX-A11Y / COHERENCIA · S3.
- Ubicaciones: `lib/moviles/mostrar_snackbar.dart:15-16` (`backgroundColor: t.verdeSolido`), contra `lib/tema_de_superficies.dart:60-64` (`backgroundColor: t.text` "tinta sobre fondo"). Uso con mensajes de error: `apps/movil/lib/pantallas/billetera/boton_de_certificado.dart:37` (`noSePudoCompartir`), `apps/movil/lib/pantallas/pasanaku/botones_de_compartir.dart:62` (`noSePudoAbrir`).
- Impacto: un fallo se comunica con el color de "listo" (regla 95.4.6 por color engañoso). `onAccion ?? () {}` (`:26`) acepta una acción sin efecto.

### H12 — Saldo oculto con «Bs» fijo y prefijo de moneda duplicado tres veces
- Clase: DINERO / DUP-INT · S3.
- Ubicaciones: `lib/organismos/encabezado_de_saldo.dart:121` (`Text('Bs ••••••')` aunque `moneda` sea USD); `lib/atomos/campo_monto.dart:71` y `lib/atomos/campo_monto_grande.dart:57` (`moneda == 'BOB' ? 'Bs' : moneda`) duplican `prefijoDe` de `lib/dinero/formatear.dart:12-14`.

### H13 — «Comisión» como tipo de movimiento y línea de desglose (choque con RN-18 sin aclarar de quién es)
- Clase: COHERENCIA / DINERO · S3 (declarar ambigüedad, no adivinar).
- Ubicaciones: `lib/moleculas/tipo_de_movimiento.dart:12,26,39`, `lib/atomos/iconos_de_marca.dart:46`, `lib/moleculas/desglose_de_cobro.dart:6,16`, muestras `lib/catalogo/muestras.dart:486-491` y `:652-659` («Comisión de entrega» −10.00, «manda el piso de Bs 10»).
- Impacto: la regla 91.5.1 prohíbe comisión del organizador; el sistema de diseño ofrece una "Comisión de entrega" sin decir quién la cobra. También `bono` (`tipo_de_movimiento.dart:15`) y `PanelBienvenida` «Te acreditamos … de bienvenida» (`lib/organismos/panel_bienvenida.dart:61-73`, en uso: 1 ref) implican dinero de la empresa acreditado al usuario: registrar quién lo financia.

### H14 — Tipos de movimiento y canales fuera del flujo QR
- Clase: FLUJO-QR · S3.
- Ubicaciones: `lib/moleculas/tipo_de_movimiento.dart:9,23,36` (`recargaTransferencia` «Recarga por transferencia»), `lib/atomos/iconos_de_marca.dart:43` (`ingresoDesdeBanco`), `:56-60` (canales `canalBilletera`, `canalBanco`, `canalTarjeta` — tarjeta además fuera de alcance PCI, regla 90.2.10). El enum `TipoDeMovimiento` no se usa en `apps/movil` (0 refs).

### H15 — `TarjetaDeOferta` decide tono con códigos del backend y los muestra crudos
- Clase: FRAGIL / UX-A11Y · S3.
- Ubicaciones: `lib/organismos/tarjeta_de_oferta.dart:47-53` (`estado == 'ACEPTADA' || estado == 'EJECUTADA'`, `'EN_VALIDACION'`), `:48` muestra `texto: estado` tal cual (muestras `muestras.dart:873` `'PUBLICADA'`, prueba `roles_de_color_no_estado_test.dart:258` `'NUEVA'`).
- Impacto: cadenas mágicas de otra capa en el sistema de diseño; la persona ve «EN_VALIDACION».

### H16 — `TarjetaDeSolicitud`: botón «Aceptar» queda cargando para siempre; «Confirmar rechazo» admite doble envío
- Clase: DEFECTO / UX-A11Y · S3 (sin uso en la app).
- Ubicaciones: `lib/organismos/tarjeta_de_solicitud.dart:143-146` (`setState(() => _enviando = true); widget.onAceptar();` sin vía para volver a `false`), `:124-129` (rechazo sin `cargando`), `:103-117` (`TextField` crudo con `OutlineInputBorder` propio en vez de `Campo`: no muestra error por campo).

### H17 — Literales de diseño y `Paleta.*` directos (saltan los roles del tema claro/oscuro)
- Clase: UX-A11Y / CLEAN · S3 (patrón).
- `Paleta.*` en lugar de roles `Tokens.of(context)`: `lib/moleculas/grilla_metricas_miembro.dart:45,52,59,66,73,81,88`, `lib/moleculas/grilla_metricas_miembro_partes.dart:37`, `lib/moleculas/insignia_tile.dart:13-17`, `lib/moleculas/tarjeta_puntos_mesa.dart:36`, `lib/moleculas/tarjeta_puntos_mesa_partes.dart:70,75`, `lib/organismos/celebracion_stage.dart:55`, `celebracion_stage_piezas.dart:48`, `celebracion_stage_pintores.dart:39`, `lib/moviles/tour_en_stages_datos.dart:12,26,40,54`, `lib/moviles/foco_de_tutorial.dart:9,43,55`. (Justificados por diseño: `lib/atomos/codigo_q_r.dart:31,40,44` QR siempre blanco/negro.)
- Números literales de tamaño/espacio/tipografía: `lib/tema.dart:119,130`, `lib/tema_de_superficies.dart:33,43`, `lib/atomos/boton.dart:125` (`Espacio.s1 + 2`), `lib/atomos/ayuda_de_hoja.dart:21`, `lib/atomos/boton_icono.dart:41`, `lib/moleculas/cabecera.dart:52,164-165`, `lib/moleculas/campo_de_fecha.dart:132`, `lib/moleculas/opcion_seleccionable.dart:88`, `lib/moleculas/pregunta.dart:130`, `lib/moleculas/seccion.dart:94`, `lib/moleculas/grilla_metricas_miembro_partes.dart:38-39,61-62,70,82,86`, `lib/moleculas/insignia_tile.dart:73,79-80`, `lib/moleculas/insignia_tile_partes.dart:26-27,35,44,68,79,82`, `lib/moleculas/tarjeta_puntos_mesa.dart:43,47-48,86-87`, `lib/moleculas/tarjeta_riesgo_confianza.dart:47,52-53`, `tarjeta_riesgo_confianza_partes.dart:24-25,32,56,66-67,73,125,133,170`, `lib/moviles/celebracion_de_exito.dart:71-72,76,106`, `lib/moviles/como_en_un_telefono.dart:88,93`, `lib/moviles/tour_en_stages.dart:141`, `tour_en_stages_pagina.dart:23-24,31-32,38,42,55,86,107`, `tour_en_stages_pie.dart:38-40`, `lib/organismos/celebracion_stage_*.dart` (múltiples: `piezas.dart:15-16,22,27-28,43,48,82,100`; `pintores.dart:12,41,48`; `tarjeta.dart:13,25,30-31,36-37,53`), `lib/organismos/estado_de_pantalla.dart:103`, `lib/ilustraciones/lienzo.dart:12`, `lib/atomos/logotipo.dart:19`, `lib/atomos/marca.dart:15`, `lib/atomos/rueda.dart:22`.
- Cómo: lectura completa; el propio `analysis_options.yaml:1-4` dice que la regla "sin literal de diseño" corre en `scripts/verificar_frontend.py` (fuera del lote) — no la detecta.

### H18 — Duraciones literales que violan el tope de 320 ms y la prueba que no las ve
- Clase: UX-A11Y (movimiento) / PRUEBAS · S3.
- Ubicaciones: `lib/atomos/numero_que_sube.dart:22` (650 ms), `lib/moleculas/riel_de_turnos.dart:113` (800 ms, en bucle), `lib/moleculas/aparicion_escalonada.dart:19-20` (45/280 ms; `Movimiento.pasoEscalonado` = 30 ms en `lib/atomos/movimiento.dart:112`, y la app sobreescribe 30/220 en `apps/movil/lib/pantallas/billetera/lista_de_movimientos.dart:61-62`), `lib/moviles/transicion_con_zoom.dart:25` (340 ms), `lib/moviles/tour_en_stages.dart:87,98` (320 ms), `tour_en_stages_pie.dart:37` (250 ms), `lib/moviles/apertura_de_marca.dart:19` (1400 ms), `lib/organismos/celebracion_stage.dart:86,91,96` (12 s, 2,5 s, 700 ms). La regla está en `lib/atomos/movimiento.dart:14` («Nada disparado por la persona pasa de 320 ms») y la prueba `test/unidad/movimiento_test.dart:29-37` sólo revisa 4 constantes.
- Movimiento reducido incompleto: `lib/organismos/celebracion_stage.dart:84-92` arranca `_rayosCtrl..repeat()` y `_particulasCtrl..repeat()` en `initState` aunque `reducirMovimiento` sea verdadero (sólo deja de pintarlos, `:141,157`); `tour_en_stages.dart:86-100` y `tour_en_stages_pie.dart:36-37` no consultan `reducirMovimiento`.

### H19 — Clones internos (misma intención, implementación distinta)
- Clase: DUP-INT · S3.
- Meses: `lib/atomos/fecha.dart:12-25`, `lib/organismos/calendario_de_cuotas.dart:40-53`, `lib/organismos/lista_de_movimientos.dart:121-134`, `lib/moleculas/parte_de_fecha.dart:6-19`.
- Botón de ícono circular de 48 dp con borde en oscuro: `lib/atomos/boton_icono.dart:25-48` (IconButton, `elevation: 1`, tooltip) vs `lib/moleculas/cabecera.dart:118-187` (`BotonDeCabecera`, Material + `Elevacion.fila`, `Semantics` sin tooltip).
- Palabra de marca: `lib/atomos/palabra_de_marca.dart:33-40` («Ya» en `t.accent`) vs `lib/moviles/apertura_de_marca.dart:123-145` (`_Nombre`, «Ya» en `t.accentTexto`).
- Contadores animados: `lib/atomos/monto_que_sube.dart` (curva `Movimiento.llega`, 220 ms) vs `lib/atomos/numero_que_sube.dart` (curva `easeOutCubic`, 650 ms) con el mismo ciclo de vida copiado.
- Esqueleto: `lib/atomos/esqueleto.dart:6-34` vs `_Esqueleto` en `lib/organismos/estado_de_pantalla.dart:84-107` (anchos literales 160/240/120).

### H20 — Comentarios que mienten
- Clase: DOC · S4.
- `lib/moviles/hoja_de_confirmacion.dart:9` («el botón de confirmar es el único naranja») vs `:87-89` (`BotonVariante.primario`, verde).
- `lib/moviles/apertura_de_marca.dart:40` («o el temporizador de respaldo») — no hay temporizador; si el ticker no avanza (TickerMode apagado) `alTerminar` nunca se llama, contra la promesa de `:13-14`.
- `lib/moviles/panel_de_marca.dart:5` describe un zoom que `transicion_de_marca.dart:8-12` dice retirado.
- `lib/organismos/lista_de_movimientos.dart:9` («Perezoso.») pero cada ítem construye todo el día en una `Column`.

### H21 — Texto con codificación rota (mojibake)
- Clase: DOC · S4. `lib/atomos/movimiento.dart:28` («ApariciÃ³n»), `:34` («ConfirmaciÃ³n»), `test/widget/comportamiento_test.dart:306` («vÃ¡lido» en el nombre del test).

### H22 — Accesibilidad de campos: etiqueta visible no vinculada al `TextField`
- Clase: UX-A11Y · S3 (hipótesis: no ejecutado con lector de pantalla).
- Ubicaciones: `lib/atomos/campo.dart:91-110` (la etiqueta es un `Text` hermano; `Semantics(label: etiquetaSemantica)` es `null` por defecto y `DecoracionDeCampo` no pone `labelText`, `lib/atomos/decoracion_de_campo.dart:36-108`). En `apps/movil/lib`: 24 usos de `Campo(` y 0 de `etiquetaSemantica`.
- Impacto: el lector anuncia el campo por su `hintText` (ejemplo) o sin nombre (regla 95.3.1/95.3.5). Igual patrón en `lib/moleculas/campo_de_fecha.dart:135-138` y `lib/atomos/campo_monto_grande.dart:62-70` (este sí envuelve con `Semantics(label:)`). El error de `CampoMontoGrande` (`:133-141`) es un `Text` suelto, no `errorText` ni `liveRegion`.

### H23 — Animaciones creadas en `build` / no liberadas
- Clase: RENDIMIENTO · S4 (hipótesis sin SDK local para confirmar la fuga de listeners de `CurvedAnimation`).
- `lib/moleculas/aparicion_escalonada.dart:97-104` (`CurvedAnimation` nueva por cada `build` de `_Hijo`, sin `dispose`), `lib/moviles/celebracion_de_exito.dart:24-31` (`CurvedAnimation` `late final` no liberadas en `dispose`, `:54-57`), `lib/organismos/celebracion_stage.dart:99-103` (ídem), `lib/moleculas/riel_de_turnos.dart:147` y `lib/organismos/estado_error.dart:105` (`Tween.animate` en `build`).

### H24 — Bordes no cubiertos
- Clase: DEFECTO · S4.
- `lib/atomos/rueda.dart:24` (`assert(turnos > 0)` sólo en debug) → `lib/atomos/pintor_de_rueda.dart:37-38` divide por `turnos`.
- `lib/atomos/punto.dart:36` texto siempre `sobreRojoSolido` aunque `tono` no sea error.
- `lib/atomos/fila_de_seleccion.dart:25-45` usa índices de `normalizarParaBuscar` (que hace `trim()`, `lib/atomos/normalizar_para_buscar.dart:10`) sobre el texto original: con espacios iniciales el resaltado se corre.
- `lib/moleculas/insignia_tile.dart:142` anuncia «Bloqueada.» también cuando está «En curso»; `:107` `toInt()` vs `Progreso` que redondea (`lib/atomos/progreso.dart:28`).
- `lib/organismos/panel_bienvenida.dart:53` «Bienvenida, $nombre» en femenino fijo.
- `test/unidad/formatear_monto_test.dart:13-15` bucle `while` hacia arriba sin tope: si no encuentra `settings.gradle.kts` no termina.

### H25 — Pruebas débiles, de plataforma o que congelan el flujo viejo
- Clase: PRUEBAS · S3.
- Goldens generados en macOS (`test/goldens/catalogo_golden_test.dart:1-12`, mismo bloque en `ilustraciones_golden_test.dart:1-12`, `marca_golden_test.dart:1-12`) y parcialmente regenerados en Linux (`git log`: `b26d2df7 … goldens de etapas regenerados en Linux`): bases mixtas, fuera de CI.
- `test/a11y/catalogo_a11y_test.dart:11-29` sólo recorre `catalogo`: las piezas que no están en `muestras.dart` (gamificación, `CampoMontoGrande`, `CampoDeFecha`, `EncabezadoDeSaldo`, `CelebracionDeExito`…) no pasan por las guías ni por ningún tema oscuro de a11y.
- `test/widget/monto_que_sube_test.dart:72-77` dice «a mitad» pero bombea 325 ms (>220 ms de duración) y sólo afirma `isNot('Bs 0,00')`; `:166-174` tolera 700 ms.
- `test/widget/campo_relleno_y_halo_test.dart:28-37` verifica el `focusedErrorBorder` del **tema**, no la decoración que usa `Campo`.
- `test/widget/roles_de_color_no_estado_test.dart:390-395` afirma que el **primer** `Container` del árbol es `accent` (frágil por estructura).
- `test/capturas/catalogo_capturas_test.dart:17-18,58` escribe en `docs/trabajo/2026-10-21-rediseno-bancario/...` desde una prueba (efecto lateral fuera del paquete).
- `test/widget/campo_de_seleccion_test.dart:112` `// ignore: avoid_print` sin `print`.
- Pruebas que fijan APROB: ver H04.

### H26 — Dependencias y acoplamiento del paquete
- Clase: CONFIG / ACOPLE · S4.
- `pubspec.yaml:17` `widgetbook` en `dependencies` (no `dev_dependencies`) y `lib/catalogo/catalogo.dart` dentro de `lib/`: el catálogo con sus datos ficticios se empaqueta con la app (`apps/movil` lo referencia: `CatalogoDeDiseno` 1 ref).
- `pubspec.yaml:13` + `lib/organismos/estado_de_pantalla.dart:2,32` atan el sistema de diseño a Riverpod (`AsyncValue`).
- `.gitignore:4-5` + `package.json:7`: `lib/tokens/tokens.dart` es generado y no versionado; el paquete no compila sin `build`.

## 3) Lo que está bien (preservar)

- `lib/dinero/formatear.dart`: dinero como cadena del contrato, aritmética en centavos enteros, falla ante formato inválido; probado contra 5.000+ vectores compartidos con Angular (`test/unidad/formatear_monto_test.dart`). `Monto` y `MontoQueSube` anuncian el valor final al lector y respetan movimiento reducido (`lib/atomos/monto_que_sube.dart:76-84`, con pruebas dedicadas).
- Helpers únicos de movimiento reducido (`lib/atomos/movimiento.dart:4-9`) usados por la mayoría de las piezas; resortes documentados y probados (`test/unidad/movimiento_test.dart:16-27`).
- `Boton`: `cargando` deshabilita y evita el doble efecto (`lib/atomos/boton.dart:45,138-146`; prueba `test/widget/comportamiento_test.dart:355-380`); motivo de bloqueo visible (`:165-183`).
- `EstadoDePantalla` / `EstadoVacio` / `EstadoError`: cuatro estados centralizados, vacío por motivo, error humano con traza y sin texto crudo del backend (`lib/organismos/estado_de_pantalla.dart`, `lib/errores.dart`).
- `DecoracionDeCampo` extraída para no divergir entre campo de texto y select (`lib/atomos/decoracion_de_campo.dart:7-11`); foco con borde + halo (no sólo color).
- `ChipEstado`, `CeldaDeCuota`, `OpcionSeleccionable`, `InsigniaDeEtapa` comunican estado con forma/ícono/texto además del color.
- Tema construido sólo desde tokens con claro y oscuro (`lib/tema.dart:16-136`), transiciones por plataforma con la nativa en iOS.
- Fuentes empaquetadas con licencia OFL (`lib/fuentes/OFL-*.txt`), íconos con versión fijada (`pubspec.yaml:15-16`).
- `CuentaEnmascarada` y `ReferenciaDeError` no exponen datos personales.

## 4) Cosas vistas fuera del lote

- `apps/movil/lib/dominio/validacion.dart:21-25` — `montoValido` usa `double.parse` (dice "valida forma"), duplica la regla de forma de `formatear.dart`.
- `apps/movil/lib/pantallas/identidad/textos.dart:42-46` — texto del tour «fondo de garantía que cubre la cuota» (RESPALDO, en uso).
- `apps/movil/lib/pantallas/billetera/lista_de_movimientos.dart:13,82-108` — `ListaDeMovimientos`/`FilaDeMovimiento` propios con el mismo nombre que las piezas del paquete; fecha con `toLocal()` sin zona.
- `apps/movil/lib/pantallas/billetera/movimientos_del_periodo.dart:31-35` — agrupación por día en zona del teléfono.
- `apps/movil/lib/pantallas/pasanaku/pantalla_solicitudes_del_grupo.dart:90-99` — pantalla de solicitudes del grupo (posible APROB en la app; no leída).
- `apps/movil/test/widget/cobro_de_recarga_test.dart:59` — usa `desformatearMonto` del paquete.
- `packages/tokens/tokens.json` — `brand` y `brandTexto` son el mismo color en ambos temas (origen de H06).
- `scripts/verificar_frontend.py` — barrido que debería detectar literales de diseño (no leído).
