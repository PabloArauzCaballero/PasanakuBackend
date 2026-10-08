//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'entrada_reverso.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class EntradaReverso {
  /// Returns a new [EntradaReverso] instance.
  EntradaReverso({

    required  this.transaccionOriginalId,

    required  this.tipo,

    required  this.motivo,

    required  this.autorizadaPor,
  });

  @JsonKey(
    
    name: r'transaccionOriginalId',
    required: true,
    includeIfNull: false,
  )


  final String transaccionOriginalId;



  @JsonKey(
    
    name: r'tipo',
    required: true,
    includeIfNull: false,
  )


  final EntradaReversoTipoEnum tipo;



  @JsonKey(
    
    name: r'motivo',
    required: true,
    includeIfNull: false,
  )


  final String motivo;



  @JsonKey(
    
    name: r'autorizadaPor',
    required: true,
    includeIfNull: false,
  )


  final String autorizadaPor;





    @override
    bool operator ==(Object other) => identical(this, other) || other is EntradaReverso &&
      other.transaccionOriginalId == transaccionOriginalId &&
      other.tipo == tipo &&
      other.motivo == motivo &&
      other.autorizadaPor == autorizadaPor;

    @override
    int get hashCode =>
        transaccionOriginalId.hashCode +
        tipo.hashCode +
        motivo.hashCode +
        autorizadaPor.hashCode;

  factory EntradaReverso.fromJson(Map<String, dynamic> json) => _$EntradaReversoFromJson(json);

  Map<String, dynamic> toJson() => _$EntradaReversoToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum EntradaReversoTipoEnum {
@JsonValue(r'ANULACION')
ANULACION(r'ANULACION'),
@JsonValue(r'CONTRACARGO')
CONTRACARGO(r'CONTRACARGO'),
@JsonValue(r'ERROR_OPERATIVO')
ERROR_OPERATIVO(r'ERROR_OPERATIVO'),
@JsonValue(r'ORDEN_AUTORIDAD')
ORDEN_AUTORIDAD(r'ORDEN_AUTORIDAD');

const EntradaReversoTipoEnum(this.value);

final String value;

@override
String toString() => value;
}


