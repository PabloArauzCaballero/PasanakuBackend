/// Los datos que la persona escribe en el alta, y lo que el proveedor de KYC lee de
/// su documento. Separados de `estado_alta.dart` por el barrido de 200 líneas.
class DatosPersonales {
  const DatosPersonales({
    this.nombres = '',
    this.apellidos = '',
    this.fechaNacimiento,
    this.telefono = '',
    this.tipoDocumento = 'CI',
    this.numeroDocumento = '',
  });
  final String nombres;
  final String apellidos;
  final DateTime? fechaNacimiento;
  final String telefono;
  final String tipoDocumento;
  final String numeroDocumento;

  DatosPersonales copiarCon({
    String? nombres,
    String? apellidos,
    DateTime? fechaNacimiento,
    String? telefono,
    String? tipoDocumento,
    String? numeroDocumento,
  }) => DatosPersonales(
    nombres: nombres ?? this.nombres,
    apellidos: apellidos ?? this.apellidos,
    fechaNacimiento: fechaNacimiento ?? this.fechaNacimiento,
    telefono: telefono ?? this.telefono,
    tipoDocumento: tipoDocumento ?? this.tipoDocumento,
    numeroDocumento: numeroDocumento ?? this.numeroDocumento,
  );
}

/// Lo leído del documento por el proveedor de KYC (CU-01 flujo 3). Sin cliente de
/// identidad generado (§ hueco H-CLIENTE del informe), esto queda como el shape que
/// consumirá `FilaDeCotejo`, poblado por el propio usuario mientras no haya OCR.
class DatosLeidosDelDocumento {
  const DatosLeidosDelDocumento({
    this.nombres = '',
    this.apellidos = '',
    this.numeroDocumento = '',
  });
  final String nombres;
  final String apellidos;
  final String numeroDocumento;
}
