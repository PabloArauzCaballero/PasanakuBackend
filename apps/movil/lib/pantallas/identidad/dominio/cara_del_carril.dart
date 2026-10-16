/// Las cinco caras del expediente y sus valores exactos para el contrato.
enum CaraDelCarril {
  anverso('ANVERSO', esDocumento: true),
  reverso('REVERSO', esDocumento: true),
  selfie('SELFIE', esDocumento: false),
  perfilIzquierdo('PERFIL_IZQUIERDO', esDocumento: false),
  perfilDerecho('PERFIL_DERECHO', esDocumento: false);

  const CaraDelCarril(this.valorApi, {required this.esDocumento});

  final String valorApi;
  final bool esDocumento;
}
