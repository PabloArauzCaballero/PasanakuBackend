/// No anuncia membresía hasta recibir los dos identificadores del contrato.
String grupoDeAceptacionConfirmada(Map<String, dynamic>? datos) {
  final grupo = datos?['grupoId'];
  final participante = datos?['participanteId'];
  final uuid = RegExp(
    r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$',
  );
  if (grupo is! String ||
      participante is! String ||
      !uuid.hasMatch(grupo) ||
      !uuid.hasMatch(participante)) {
    throw const FormatException('Aceptación sin confirmación verificable');
  }
  return grupo;
}
