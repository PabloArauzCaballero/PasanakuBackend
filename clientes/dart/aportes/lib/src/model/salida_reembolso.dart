//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:aportaya_cliente_aportes/src/model/dinero.dart';
import 'package:json_annotation/json_annotation.dart';

part 'salida_reembolso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaReembolso {
  /// Returns a new [SalidaReembolso] instance.
  SalidaReembolso({

    required  this.reembolsoId,

    required  this.estado,

    required  this.disponibleRestante,
  });

  @JsonKey(
    
    name: r'reembolsoId',
    required: true,
    includeIfNull: false,
  )


  final String reembolsoId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaReembolsoEstadoEnum estado;



  @JsonKey(
    
    name: r'disponibleRestante',
    required: true,
    includeIfNull: false,
  )


  final Dinero disponibleRestante;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaReembolso &&
      other.reembolsoId == reembolsoId &&
      other.estado == estado &&
      other.disponibleRestante == disponibleRestante;

    @override
    int get hashCode =>
        reembolsoId.hashCode +
        estado.hashCode +
        disponibleRestante.hashCode;

  factory SalidaReembolso.fromJson(Map<String, dynamic> json) => _$SalidaReembolsoFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaReembolsoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaReembolsoEstadoEnum {
@JsonValue(r'SOLICITADO')
SOLICITADO(r'SOLICITADO');

const SalidaReembolsoEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


