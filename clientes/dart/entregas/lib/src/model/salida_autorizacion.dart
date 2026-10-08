//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_autorizacion.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAutorizacion {
  /// Returns a new [SalidaAutorizacion] instance.
  SalidaAutorizacion({

    required  this.entregaId,

    required  this.estado,

    required  this.autorizadaPor,
  });

  @JsonKey(
    
    name: r'entregaId',
    required: true,
    includeIfNull: false,
  )


  final String entregaId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaAutorizacionEstadoEnum estado;



  @JsonKey(
    
    name: r'autorizadaPor',
    required: true,
    includeIfNull: false,
  )


  final String autorizadaPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAutorizacion &&
      other.entregaId == entregaId &&
      other.estado == estado &&
      other.autorizadaPor == autorizadaPor;

    @override
    int get hashCode =>
        entregaId.hashCode +
        estado.hashCode +
        autorizadaPor.hashCode;

  factory SalidaAutorizacion.fromJson(Map<String, dynamic> json) => _$SalidaAutorizacionFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAutorizacionToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaAutorizacionEstadoEnum {
@JsonValue(r'AUTORIZADA')
AUTORIZADA(r'AUTORIZADA');

const SalidaAutorizacionEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


