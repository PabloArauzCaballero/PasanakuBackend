import '../../../dominio/tutoriales/modelo.dart';
import 'primeros_pasos.dart';
import 'tu_plata.dart';

/// **El catálogo completo de la app.** Agregar un tutorial es escribir su constante en
/// el archivo de su tema y sumarla a esta lista. Nada más: ni el motor, ni el registro,
/// ni el centro de ayuda saben que existe.
///
/// El orden es el orden en que se ofrecen cuando nada los separa: primero lo que le
/// sirve a cualquiera, después cada tema.
const List<TutorialDefinicion> catalogoDeTutoriales = [
  introALaApp,
  navegacionDeLaApp,
  usarElCentroDeAyuda,
  entenderTuSaldo,
  leerTusMovimientos,
  seguirTuPasanaku,
  cuidarTuCuenta,
];

/// Las rutas que la app sabe abrir, para validar que ningún tutorial apunte a una
/// pantalla que no existe. La prueba del catálogo falla si dejan de coincidir con
/// `navegacion/rutas.dart`.
const List<String> rutasConocidas = [
  '/billetera/inicio',
  '/billetera/extracto',
  '/pasanaku/mi-estado',
  '/identidad/perfil',
  '/notificaciones/bandeja',
  '/soporte/ayuda',
];
