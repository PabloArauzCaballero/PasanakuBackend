//
// AUTO-GENERATED FILE, DO NOT MODIFY!
//

// ignore_for_file: unused_element
import 'package:json_annotation/json_annotation.dart';

part 'salida_anuncio.g.dart';


@JsonSerializable(
  checked: true,
  createToJson: true,
  disallowUnrecognizedKeys: false,
  explicitToJson: true,
)
class SalidaAnuncio {
  /// Returns a new [SalidaAnuncio] instance.
  SalidaAnuncio({

    required  this.anuncioId,

    required  this.estado,
  });

  @JsonKey(
    
    name: r'anuncioId',
    required: true,
    includeIfNull: false,
  )


  final String anuncioId;



  @JsonKey(
    
    name: r'estado',
    required: true,
    includeIfNull: false,
  )


  final SalidaAnuncioEstadoEnum estado;





    @override
    bool operator ==(Object other) => identical(this, other) || other is SalidaAnuncio &&
      other.anuncioId == anuncioId &&
      other.estado == estado;

    @override
    int get hashCode =>
        anuncioId.hashCode +
        estado.hashCode;

  factory SalidaAnuncio.fromJson(Map<String, dynamic> json) => _$SalidaAnuncioFromJson(json);

  Map<String, dynamic> toJson() => _$SalidaAnuncioToJson(this);

  @override
  String toString() {
    return toJson().toString();
  }

}


enum SalidaAnuncioEstadoEnum {
@JsonValue(r'PROGRAMADO')
PROGRAMADO(r'PROGRAMADO'),
@JsonValue(r'EN_ENTREGA')
EN_ENTREGA(r'EN_ENTREGA'),
@JsonValue(r'PAUSADO')
PAUSADO(r'PAUSADO'),
@JsonValue(r'FINALIZADO')
FINALIZADO(r'FINALIZADO'),
@JsonValue(r'RECHAZADO')
RECHAZADO(r'RECHAZADO');

const SalidaAnuncioEstadoEnum(this.value);

final String value;

@override
String toString() => value;
}


