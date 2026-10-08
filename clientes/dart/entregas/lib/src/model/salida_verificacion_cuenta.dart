//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_verificacion_cuenta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaVerificacionCuenta {
  /// Returns a new [SalidaVerificacionCuenta] instance.
  SalidaVerificacionCuenta({

    required  this.cuentaId,

    required  this.estado,

     this.disponibleDesde,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'cuentaId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final String estado;



      /// el fin de la ventana de enfriamiento; se GUARDA, no se recalcula
  @JsonKey(
    
    name: r'disponibleDesde',
    required: false,
    includeIfNull: false,
  )


  final DateTime? disponibleDesde;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaVerificacionCuenta &&
      other.cuentaId == cuentaId &&
      other.estado == estado &&
      other.disponibleDesde == disponibleDesde &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        cuentaId.hashCode +
        estado.hashCode +
        (disponibleDesde == null ? 0 : disponibleDesde.hashCode) +
        esNueva.hashCode;

  factory SalidaVerificacionCuenta.fromJson(Map<String, dynamic> json) => _$SalidaVerificacionCuentaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaVerificacionCuentaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}

