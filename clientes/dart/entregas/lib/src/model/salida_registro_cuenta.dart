//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_registro_cuenta.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaRegistroCuenta {
  /// Returns a new [SalidaRegistroCuenta] instance.
  SalidaRegistroCuenta({

    required  this.cuentaId,

    required  this.numeroEnmascarado,

    required  this.estado,

    required  this.esNueva,
  });

  @JsonKey(
    
    name: r'cuentaId',
    required: true,
    includeIfNull: false,
  )


  final String cuentaId;



      /// lo unico que se muestra — los ultimos cuatro digitos
  @JsonKey(
    
    name: r'numeroEnmascarado',
    required: true,
    includeIfNull: false,
  )


  final String numeroEnmascarado;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaRegistroCuentaEstadoEnum estado;



  @JsonKey(
    
    name: r'esNueva',
    required: true,
    includeIfNull: false,
  )


  final bool esNueva;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaRegistroCuenta &&
      other.cuentaId == cuentaId &&
      other.numeroEnmascarado == numeroEnmascarado &&
      other.estado == estado &&
      other.esNueva == esNueva;

    @override
    int get hashCode =>
        cuentaId.hashCode +
        numeroEnmascarado.hashCode +
        estado.hashCode +
        esNueva.hashCode;

  factory SalidaRegistroCuenta.fromJson(Map<String, dynamic> json) => _$SalidaRegistroCuentaFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaRegistroCuentaToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaRegistroCuentaEstadoEnum {
@JsonValue(r'PENDIENTE')
PENDIENTE(r'PENDIENTE'),
@JsonValue(r'VERIFICADA')
VERIFICADA(r'VERIFICADA'),
@JsonValue(r'RECHAZADA')
RECHAZADA(r'RECHAZADA');

const SalidaRegistroCuentaEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


