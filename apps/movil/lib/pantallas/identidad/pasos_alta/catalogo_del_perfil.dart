/// Las listas cerradas del perfil transaccional declarado (CU-01, paso 7).
///
/// **Se elige, no se escribe.** Un campo libre acá produce «trabajo», «mi sueldo»,
/// «negocio» y «NEGOCIO» como cuatro respuestas distintas a la misma pregunta, y el
/// perfil transaccional existe justamente para poder compararlo después contra lo que
/// la persona opera (desvío de perfil, monitoreo LFT). Lo que no se puede agrupar no
/// se puede monitorear.
///
/// Los códigos de [origenesDeFondos] son **los mismos** que acepta la base:
/// `ck_declaracion_origen_fondos_origen` en
/// `sql/10_tablas/12_cumplimiento_asfi/declaracion_origen_fondos.sql`. Si acá
/// apareciera uno que la tabla no conoce, la declaración se rechazaría al guardarla.
///
/// Las actividades salen de las secciones de la CIIU Rev. 4 —la clasificación que usa
/// el INE y la que pide ASFI para el expediente— agrupadas en la cantidad de opciones
/// que alguien puede leer de un tirón en un celular. El código viaja junto al texto
/// para que `codigo_ciiu` (`perfil_transaccional`) se llene solo el día que el alta
/// mande el perfil al servidor.
library;

typedef OpcionDelPerfil = ({String codigo, String texto});

/// De dónde sale la plata que va a entrar a la billetera.
const List<OpcionDelPerfil> origenesDeFondos = [
  (codigo: 'SALARIO', texto: 'Sueldo o salario'),
  (
    codigo: 'NEGOCIO',
    texto: 'Ingresos de mi negocio o actividad independiente',
  ),
  (codigo: 'REMESA', texto: 'Remesas del exterior'),
  (codigo: 'PRESTAMO', texto: 'Préstamo o crédito'),
  (codigo: 'VENTA_BIEN', texto: 'Venta de un bien (vehículo, inmueble, otro)'),
  (codigo: 'HERENCIA', texto: 'Herencia o donación'),
  (codigo: 'OTRO', texto: 'Otro (lo detallo)'),
];

/// El código que obliga a escribir el detalle a mano.
const String origenOtro = 'OTRO';

/// A qué se dedica quien abre la cuenta. El código es la sección CIIU Rev. 4.
const List<OpcionDelPerfil> actividadesEconomicas = [
  (codigo: 'G', texto: 'Comercio (tienda, puesto, venta por mayor o menor)'),
  (codigo: 'T', texto: 'Empleo en relación de dependencia'),
  (codigo: 'M', texto: 'Servicios profesionales o técnicos independientes'),
  (codigo: 'A', texto: 'Agricultura, ganadería, pesca o silvicultura'),
  (codigo: 'C', texto: 'Industria manufacturera, artesanía o taller'),
  (codigo: 'F', texto: 'Construcción'),
  (codigo: 'H', texto: 'Transporte, carga o delivery'),
  (codigo: 'I', texto: 'Alojamiento, restaurante o comida'),
  (codigo: 'B', texto: 'Minería o hidrocarburos'),
  (codigo: 'K', texto: 'Actividades financieras o de seguros'),
  (codigo: 'P', texto: 'Educación'),
  (codigo: 'Q', texto: 'Salud o asistencia social'),
  (codigo: 'O', texto: 'Administración pública o defensa'),
  (codigo: 'S', texto: 'Otros servicios (peluquería, reparaciones, limpieza)'),
  (codigo: 'JUBILADO', texto: 'Jubilado o rentista'),
  (codigo: 'ESTUDIANTE', texto: 'Estudiante'),
  (codigo: 'HOGAR', texto: 'Trabajo del hogar no remunerado'),
  (codigo: 'SIN_ACTIVIDAD', texto: 'Sin actividad por ahora'),
];

/// El texto que corresponde a un código, o el propio código si no está en la lista
/// —que es lo que hay que mostrar antes que una pantalla en blanco.
String textoDelOrigen(String codigo) => _texto(origenesDeFondos, codigo);

String textoDeLaActividad(String codigo) =>
    _texto(actividadesEconomicas, codigo);

String _texto(List<OpcionDelPerfil> opciones, String codigo) {
  for (final o in opciones) {
    if (o.codigo == codigo) return o.texto;
  }
  return codigo;
}
