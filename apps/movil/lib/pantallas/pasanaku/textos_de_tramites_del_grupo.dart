/// La entrada de los trámites del grupo (`IntroDePantalla`, ADR-053), el motivo de cada
/// botón apagado y las etiquetas legibles de los estados que devuelve el servidor (antes
/// se pintaban crudos: «DEUDORA», «Estado: PENDIENTE»).
abstract final class TextosDeTramitesDelGrupo {
  static const permutaTitulo = 'Proponé cambiar tu turno';
  static const permutaExplicacion =
      'La otra persona tiene que aceptar. Hasta entonces, tu turno no cambia.';
  static const motivoCorto = 'Contá por qué con al menos 10 caracteres.';

  static String estadoDePermuta(String estado) => switch (estado) {
    'PENDIENTE' => 'Pendiente',
    'ACEPTADA' => 'Aceptada',
    'RECHAZADA' => 'Rechazada',
    'EJECUTADA' => 'Hecha',
    _ => 'En proceso',
  };

  static const retiroTitulo = 'Pedir tu retiro del grupo';
  static const retiroExplicacion =
      'Al pedirlo se calcula cómo quedás con el grupo: a tu favor, con deuda o en cero.';

  static String posicionDeRetiro(String posicion) => switch (posicion) {
    'ACREEDORA' => 'A tu favor',
    'DEUDORA' => 'Con deuda',
    _ => 'En cero',
  };
}
