/// Las entradas y los motivos de botón apagado de los pasos que hacen crecer un grupo:
/// organizarlo, invitar, pedir un lugar, unirse y aportar (pasada de marketing,
/// 2026-10-29: skills `copywriting`, `cro`, `onboarding`, `ux-writing-microcopy`).
///
/// Cada frase describe lo que la app hace hoy. Nada de avisos que nadie manda: ningún
/// servicio consume `grupos.ingreso_solicitado` ni `grupos.ingreso_aceptado`, así que la
/// app no dice «te avisamos» ni «le avisamos al organizador».
abstract final class TextosDeConversion {
  // Organizar un grupo (CU-20)
  static const crearTitulo = 'Armá el pasanaku con tu gente';
  static const crearExplicacion =
      'Elegí el monto y cada cuánto se aporta. Después compartís el enlace del grupo '
      'y vos decidís a quién aceptar.';
  static const crearBloqueado =
      'Escribí un nombre de 3 letras o más y el monto de cada aporte.';

  // Grupo recién creado: la cifra que importa y el paso que sigue.
  static const cadaTurnoJunta = 'Cada turno junta';
  static const loQueSigue =
      'Ahora sumá a tu gente: compartí el QR o el enlace del grupo y aceptá a quien pida unirse.';

  // Invitar con el enlace del grupo (CU-69)
  static const invitarTitulo = 'Sumá gente a tu pasanaku';
  static const invitarExplicacion =
      'Compartí el QR o el enlace. Quien lo abra te pide unirse y vos decidís a quién aceptar.';

  // Invitar a un número (CU-69)
  static const invitarNumeroTitulo = 'Invitá a una persona';
  static const invitarNumeroExplicacion =
      'La invitación sirve solo para ese celular. Se la mandás vos, por el canal que elijas.';
  static const telefonoBloqueado = 'Escribí los 8 dígitos del celular.';

  // Unirse con una invitación personal
  static const unirseBloqueado = 'Marcá que leíste el reglamento para unirte.';

  // Pedir unirse con el enlace del grupo
  static const alPedirTitulo = 'Qué pasa al pedir unirte';

  // Aportar (CU-21)
  static String aportarCon(String importe) => 'Aportar $importe';
  static const aportarBloqueado =
      'Escribí la referencia del pago para poder aportar.';
  static const referenciaAyuda = 'El número de operación de tu comprobante.';
}
